package com.commercetools.project.sync;

import static com.commercetools.project.sync.util.SyncUtils.getSyncModuleName;
import static com.commercetools.sync.commons.utils.CtpQueryUtils.queryAll;
import static java.lang.String.format;

import com.commercetools.project.sync.model.response.LastSyncCustomObject;
import com.commercetools.project.sync.service.CustomObjectService;
import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.models.ResourceView;
import io.sphere.sdk.queries.QueryDsl;
import io.sphere.sdk.queries.QueryPredicate;
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

/**
 * Base class of the syncer that handles syncing a resource from a source CTP project to a target
 * CTP project.
 *
 * @param <RU> The type of the resource to update (e.g. {@link
 *     io.sphere.sdk.products.ProductProjection}, {@link io.sphere.sdk.categories.CategoryDraft},
 *     etc..)
 * @param <RC> The type of the resource to create (e.g. {@link io.sphere.sdk.products.Product},
 *     {@link io.sphere.sdk.categories.Category}, etc..)
 * @param <RD> The type of the resource draft (e.g. {@link io.sphere.sdk.products.ProductDraft},
 *     {@link io.sphere.sdk.categories.CategoryDraft}, etc..)
 * @param <S> The type of the sync statistics resulting from the sync process (e.g. {@link
 *     com.commercetools.sync.products.helpers.ProductSyncStatistics}, {@link
 *     com.commercetools.sync.categories.helpers.CategorySyncStatistics}, etc..)
 * @param <O> The type of the sync options used for the sync (e.g. {@link
 *     com.commercetools.sync.products.ProductSyncOptions}, {@link
 *     com.commercetools.sync.categories.CategorySyncOptions}, etc..)
 * @param <Q> The type of the query used to query for the source resources (e.g. {@link
 *     io.sphere.sdk.products.queries.ProductProjectionQuery}, {@link
 *     io.sphere.sdk.categories.queries.CategoryQuery}, etc..)
 * @param <B> The type of the sync instance used to execute the sync process (e.g. {@link
 *     com.commercetools.sync.products.ProductSync}, {@link
 *     com.commercetools.sync.categories.CategorySync}, etc..)
 */
public abstract class Syncer<
    RU extends ResourceView,
    RC extends ResourceView<RC, RC>,
    RD,
    S extends BaseSyncStatistics,
    O extends BaseSyncOptions<RU, RD, RC>,
    Q extends QueryDsl<RU, Q>,
    B extends BaseSync<RD, S, O>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(Syncer.class);

  /* Using default Caffeine cache implementation from sync-java library for caching reference
   * IdToKey values.
   */
  protected static final ReferenceIdToKeyCache referenceIdToKeyCache =
      new CaffeineReferenceIdToKeyCacheImpl();

  private final B sync;
  private final SphereClient sourceClient;
  private final SphereClient targetClient;
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
      @Nonnull final B sync,
      @Nonnull final SphereClient sourceClient,
      @Nonnull final SphereClient targetClient,
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

    final String sourceProjectKey = sourceClient.getConfig().getProjectKey();
    final String syncModuleName = getSyncModuleName(sync.getClass());
    if (getLoggerInstance().isInfoEnabled()) {
      final String targetProjectKey = targetClient.getConfig().getProjectKey();
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
  private CompletionStage<Q> getQueryOfResourcesSinceLastSync(
      @Nonnull final String sourceProjectKey,
      @Nonnull final String syncModuleName,
      @Nullable final String runnerName,
      @Nonnull final ZonedDateTime currentSyncStartTimestamp) {

    return customObjectService
        .getLastSyncCustomObject(sourceProjectKey, syncModuleName, runnerName)
        .thenApply(
            customObjectOptional ->
                customObjectOptional
                    .map(CustomObject::getValue)
                    .map(LastSyncCustomObject::getLastSyncTimestamp)
                    .map(
                        lastSyncTimestamp ->
                            getQueryWithTimeBoundedPredicate(
                                lastSyncTimestamp, currentSyncStartTimestamp))
                    // If there is no last sync custom object, use base query to get all resources
                    .orElseGet(this::getQuery));
  }

  @Nonnull
  private Q getQueryWithTimeBoundedPredicate(
      @Nonnull final ZonedDateTime lowerBound, @Nonnull final ZonedDateTime upperBound) {

    final QueryPredicate<RU> queryPredicate =
        QueryPredicate.of(
            format(
                "lastModifiedAt >= \"%s\" AND lastModifiedAt <= \"%s\"", lowerBound, upperBound));
    return getQuery().plusPredicates(queryPredicate);
  }

  @Nonnull
  private CompletionStage<Long> sync(@Nonnull final Q queryResourcesSinceLastSync) {

    final long timeBeforeSync = clock.millis();
    return queryAll(sourceClient, queryResourcesSinceLastSync, this::syncPage)
        .thenApply(
            ignoredResult -> {
              final long timeAfterSync = clock.millis();
              return timeAfterSync - timeBeforeSync;
            });
  }

  @Nonnull
  private CompletionStage<CustomObject<LastSyncCustomObject>> createNewLastSyncCustomObject(
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

    final LastSyncCustomObject<S> lastSyncCustomObject =
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
  private S syncPage(@Nonnull final List<RU> page) {
    return transform(page).thenCompose(sync::sync).toCompletableFuture().join();
  }

  /**
   * Given a {@link List} representing a page of resources of type {@code T}, this method creates a
   * a list of drafts of type {@link S} where reference ids of the references are replaced with keys
   * and are ready for reference resolution by the sync process.
   *
   * @return a {@link CompletionStage} containing a list of drafts of type {@link S} after being
   *     transformed from type {@link RU}.
   */
  @Nonnull
  protected abstract CompletionStage<List<RD>> transform(@Nonnull final List<RU> page);

  @Nonnull
  protected abstract Q getQuery();

  public B getSync() {
    return sync;
  }

  @Nonnull
  protected abstract Logger getLoggerInstance();

  @Nonnull
  public SphereClient getSourceClient() {
    return sourceClient;
  }
}
