package com.commercetools.project.sync;

import static com.commercetools.api.client.QueryUtils.queryAll;
import static com.commercetools.project.sync.util.SyncUtils.getSyncModuleName;
import static java.lang.String.format;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.DomainResource;
import com.commercetools.api.models.PagedQueryResourceRequest;
import com.commercetools.api.models.ResourcePagedQueryResponse;
import com.commercetools.api.models.ResourceUpdateAction;
import com.commercetools.api.models.WithKey;
import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.common.BaseResource;
import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.project.sync.model.response.LastSyncCustomObject;
import com.commercetools.project.sync.service.CustomObjectService;
import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.logstash.logback.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This class compiles but not tested yet
// TODO: Test class and adjust logic if needed
/**
 * Base class of the syncer that handles syncing a resource from a source CTP project to a target
 * CTP project.
 *
 * @param <ResourceT>> The type of the resource to update (e.g. {@link ProductProjection}, {@link
 *     Category}, etc..)
 * @param <ResourceUpdateActionT>> The type of the resource to create
 * @param <ResourceDraftT>> The type of the resource draft (e.g. {@link ProductDraft}, {@link
 *     CategoryDraft}, etc..)
 * @param <SyncStatisticsT>> The type of the sync statistics resulting from the sync process (e.g.
 *     {@link com.commercetools.sync.products.helpers.ProductSyncStatistics}, {@link
 *     com.commercetools.sync.categories.helpers.CategorySyncStatistics}, etc..)
 * @param <SyncOptionsT>> The type of the sync options used for the sync (e.g. {@link
 *     com.commercetools.sync.products.ProductSyncOptions}, {@link
 *     com.commercetools.sync.categories.CategorySyncOptions}, etc..)
 * @param <PagedQueryT> The type of the query used to query for the source resources (e.g. {@link
 *     com.commercetools.api.client.ByProjectKeyProductProjectionsGet}, {@link
 *     com.commercetools.api.client.ByProjectKeyCategoriesGet}, etc..)
 * @param <BaseSyncT> The type of the sync instance used to execute the sync process (e.g. {@link
 *     com.commercetools.sync.products.ProductSync}, {@link
 *     com.commercetools.sync.categories.CategorySync}, etc..)
 */
public abstract class Syncer<
    ResourceT extends BaseResource & DomainResource<ResourceT> & WithKey,
    ResourceUpdateActionT extends ResourceUpdateAction<ResourceUpdateActionT>,
    ResourceDraftT,
    SyncStatisticsT extends BaseSyncStatistics,
    SyncOptionsT extends BaseSyncOptions<ResourceT, ResourceDraftT, ResourceUpdateActionT>,
    PagedQueryT extends PagedQueryResourceRequest<PagedQueryT, PagedQueryResponseT>,
    PagedQueryResponseT extends ResourcePagedQueryResponse<ResourceT>,
    BaseSyncT extends
        BaseSync<ResourceT, ResourceDraftT, ResourceUpdateActionT, SyncStatisticsT, SyncOptionsT>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(Syncer.class);

  /* Using default Caffeine cache implementation from sync-java library for caching reference
   * IdToKey values.
   */
  protected static final ReferenceIdToKeyCache referenceIdToKeyCache =
      new CaffeineReferenceIdToKeyCacheImpl();

  private final BaseSyncT sync;
  private final ProjectApiRoot sourceClient;
  private final ProjectApiRoot targetClient;
  private final CustomObjectService customObjectService;
  private final Clock clock;

  /**
   * Instantiates a {@link Syncer} which is used to sync resources from a source to a target
   * commercetools project.
   *
   * @param sync The sync module that is used for syncing the resource drafts to the target project,
   *     after being transformed from the resources fetched from the source project.
   * @param sourceClient the client used for querying data from the source commercetools project.
   * @param targetClient the client used for syncing the transformed drafts into the target
   *     commercetools project.
   * @param customObjectService service that is used for fetching and persisting the last sync
   *     timestamp for delta syncing.
   * @param clock the clock to record the time for calculating the sync duration.
   */
  public Syncer(
      @Nonnull final BaseSyncT sync,
      @Nonnull final ProjectApiRoot sourceClient,
      @Nonnull final ProjectApiRoot targetClient,
      @Nonnull final CustomObjectService customObjectService,
      @Nonnull final Clock clock) {
    this.sync = sync;
    this.sourceClient = sourceClient;
    this.targetClient = targetClient;
    this.customObjectService = customObjectService;
    this.clock = clock;
  }

  /**
   * Fetches the sourceClient's project resources of type {@code T} with all needed references
   * expanded and treats each page as a batch to the sync process. Then executes the sync process of
   * on every page fetched from the source project sequentially. It then returns a completion stage
   * containing a {@link Void} result after the execution of the sync process and logging the
   * result.
   *
   * <p>Note: If {@code isFullSync} is {@code false}, i.e. a delta sync is required, the method
   * checks if there was a last sync time stamp persisted as a custom object in the target project
   * for this specific source project and sync module. If there is, it will sync only the resources
   * which were modified after the last sync time stamp and before the start of this sync.
   *
   * @param runnerName the name of the sync runner.
   * @param isFullSync whether to run a delta sync (based on the last sync timestamp) or a full
   *     sync.
   * @return completion stage containing no result after the execution of the sync process and
   *     logging the result.
   */
  public CompletionStage<Void> sync(@Nullable final String runnerName, final boolean isFullSync) {

    final String sourceProjectKey = sourceClient.getProjectKey();
    final String syncModuleName = getSyncModuleName(sync.getClass());
    if (getLoggerInstance().isInfoEnabled()) {
      final String targetProjectKey = targetClient.getProjectKey();
      getLoggerInstance()
          .info(
              format(
                  "Starting %s from CTP project with key '%s' to project with key '%s'",
                  syncModuleName, sourceProjectKey, targetProjectKey));
    }

    final CompletionStage<Void> syncStage;
    if (isFullSync) {
      syncStage = sync(getQuery()).thenAccept(result -> {});
    } else {
      syncStage =
          customObjectService
              .getCurrentCtpTimestamp(runnerName, syncModuleName)
              .thenCompose(
                  currentCtpTimestamp ->
                      syncResourcesSinceLastSync(
                          sourceProjectKey, syncModuleName, runnerName, currentCtpTimestamp));
    }

    return syncStage.thenAccept(
        ignoredResult -> {
          if (getLoggerInstance().isInfoEnabled()) {
            getLoggerInstance()
                .info(
                    Markers.append("statistics", sync.getStatistics()),
                    sync.getStatistics().getReportMessage());
          }
        });
  }

  @Nonnull
  private CompletionStage<Void> syncResourcesSinceLastSync(
      @Nonnull final String sourceProjectKey,
      @Nonnull final String syncModuleName,
      @Nullable final String runnerName,
      @Nonnull final ZonedDateTime currentCtpTimestamp) {

    return getQueryOfResourcesSinceLastSync(
            sourceProjectKey, syncModuleName, runnerName, currentCtpTimestamp)
        .thenCompose(this::sync)
        .thenCompose(
            syncDurationInMillis ->
                createNewLastSyncCustomObject(
                    sourceProjectKey,
                    syncModuleName,
                    runnerName,
                    currentCtpTimestamp,
                    syncDurationInMillis))
        .thenAccept(result -> {});
  }

  @Nonnull
  private CompletionStage<PagedQueryT> getQueryOfResourcesSinceLastSync(
      @Nonnull final String sourceProjectKey,
      @Nonnull final String syncModuleName,
      @Nullable final String runnerName,
      @Nonnull final ZonedDateTime currentSyncStartTimestamp) {

    return customObjectService
        .getLastSyncCustomObject(sourceProjectKey, syncModuleName, runnerName)
        .thenApply(
            customObjectOptional ->
                customObjectOptional
                    .map(LastSyncCustomObject::getLastSyncTimestamp)
                    .map(
                        lastSyncTimestamp ->
                            getQueryWithTimeBoundedPredicate(
                                lastSyncTimestamp, currentSyncStartTimestamp))
                    // If there is no last sync custom object, use base query to get all resources
                    .orElseGet(this::getQuery));
  }

  @Nonnull
  private PagedQueryT getQueryWithTimeBoundedPredicate(
      @Nonnull final ZonedDateTime lowerBound, @Nonnull final ZonedDateTime upperBound) {

    return (PagedQueryT)
        getQuery()
            .withWhere("lastModifiedAt >= \":lower\" AND lastModifiedAt <= \":upper\"")
            .withPredicateVar("lower", lowerBound)
            .withPredicateVar("upper", upperBound);
  }

  @Nonnull
  private CompletionStage<Long> sync(@Nonnull final PagedQueryT queryResourcesSinceLastSync) {

    final long timeBeforeSync = clock.millis();
    return queryAll(queryResourcesSinceLastSync, this::syncPage)
        .thenApply(
            ignoredResult -> {
              final long timeAfterSync = clock.millis();
              return timeAfterSync - timeBeforeSync;
            });
  }

  @Nonnull
  private CompletableFuture<ApiHttpResponse<CustomObject>> createNewLastSyncCustomObject(
      @Nonnull final String sourceProjectKey,
      @Nonnull final String syncModuleName,
      @Nullable final String runnerName,
      @Nonnull final ZonedDateTime newLastSyncTimestamp,
      final long syncDurationInMillis) {

    /*
     * The 2 minutes is an arbitrary number chosen to account for any potential delays of
     * entries added to the CTP DB with older dates than now, since CTP timestamps are created on
     * distributed APIs.
     */
    final long bufferInMinutes = 2;
    final ZonedDateTime lastSyncTimestampMinusBuffer =
        newLastSyncTimestamp.minusMinutes(bufferInMinutes);

    final LastSyncCustomObject<SyncStatisticsT> lastSyncCustomObject =
        LastSyncCustomObject.of(
            lastSyncTimestampMinusBuffer, sync.getStatistics(), syncDurationInMillis);

    return customObjectService.createLastSyncCustomObject(
        sourceProjectKey, syncModuleName, runnerName, lastSyncCustomObject);
  }

  /**
   * Given a {@link List} representing a page of resources of type {@code T}, this method creates a
   * {@link CompletableFuture} of each sync process on the given page as a batch.
   */
  @Nonnull
  private SyncStatisticsT syncPage(@Nonnull final List<ResourceT> page) {
    return transform(page).thenCompose(sync::sync).toCompletableFuture().join();
  }

  /**
   * Given a {@link List} representing a page of resources of type {@code T}, this method creates a
   * a list of drafts of type {@link SyncStatisticsT} where reference ids of the references are
   * replaced with keys and are ready for reference resolution by the sync process.
   *
   * @return a {@link CompletionStage} containing a list of drafts of type {@link SyncStatisticsT}
   *     after being transformed from type {@link ResourceT}.
   */
  @Nonnull
  protected abstract CompletionStage<List<ResourceDraftT>> transform(
      @Nonnull final List<ResourceT> page);

  @Nonnull
  protected abstract PagedQueryT getQuery();

  public BaseSyncT getSync() {
    return sync;
  }

  @Nonnull
  protected abstract Logger getLoggerInstance();

  @Nonnull
  public ProjectApiRoot getSourceClient() {
    return sourceClient;
  }
}
