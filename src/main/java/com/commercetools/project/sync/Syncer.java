package com.commercetools.project.sync;

import static com.commercetools.project.sync.util.StatisticsUtils.logStatistics;
import static com.commercetools.project.sync.util.SyncUtils.getSyncModuleName;
import static com.commercetools.sync.commons.utils.CtpQueryUtils.queryAll;
import static java.lang.String.format;

import com.commercetools.project.sync.model.LastSyncCustomObject;
import com.commercetools.project.sync.service.CustomObjectService;
import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.models.Resource;
import io.sphere.sdk.queries.QueryDsl;
import io.sphere.sdk.queries.QueryPredicate;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class of the syncer that handles syncing a resource from a source CTP project to a target
 * CTP project.
 *
 * @param <T> The type of the resource (e.g. {@link io.sphere.sdk.products.Product}, {@link
 *     io.sphere.sdk.categories.Category}, etc..)
 * @param <S> The type of the resource draft (e.g. {@link io.sphere.sdk.products.ProductDraft},
 *     {@link io.sphere.sdk.categories.CategoryDraft}, etc..)
 * @param <U> The type of the sync statistics resulting from the sync process (e.g. {@link
 *     com.commercetools.sync.products.helpers.ProductSyncStatistics}, {@link
 *     com.commercetools.sync.categories.helpers.CategorySyncStatistics}, etc..)
 * @param <V> The type of the sync options used for the sync (e.g. {@link
 *     com.commercetools.sync.products.ProductSyncOptions}, {@link
 *     com.commercetools.sync.categories.CategorySyncOptions}, etc..)
 * @param <C> The type of the query used to query for the source resources (e.g. {@link
 *     io.sphere.sdk.products.queries.ProductQuery}, {@link
 *     io.sphere.sdk.categories.queries.CategoryQuery}, etc..)
 * @param <B> The type of the sync instance used to execute the sync process (e.g. {@link
 *     com.commercetools.sync.products.ProductSync}, {@link
 *     com.commercetools.sync.categories.CategorySync}, etc..)
 */
public abstract class Syncer<
    T extends Resource,
    S,
    U extends BaseSyncStatistics,
    V extends BaseSyncOptions<T, S>,
    C extends QueryDsl<T, C>,
    B extends BaseSync<S, U, V>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(Syncer.class);

  private final B sync;
  private final SphereClient sourceClient;
  private final SphereClient targetClient;
  private final CustomObjectService customObjectService;
  private final Clock clock;

  /**
   * Instantiates a {@link Syncer} which is used to sync resources from a source to a target
   * commercetools project.
   *
   * @param sync The sync module that is used for syncing the transformed drafts to the target
   *     project.
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
   * all pages in parallel. It then returns a completion stage containing no result after the
   * execution of the sync process and logging the result.
   *
   * <p>Note: The method checks if there was a last sync time stamp persisted as a custom object in
   * the target project for this specific source project and sync module. If there is, it will sync
   * only the resources which were modified after the last sync time stamp and before the start of
   * this sync.
   *
   * @param runnerName the name of the sync runner.
   * @return completion stage containing no result after the execution of the sync process and
   *     logging the result.
   */
  public CompletionStage<Void> sync(@Nullable final String runnerName) {

    final String sourceProjectKey = sourceClient.getConfig().getProjectKey();
    final String syncModuleName = getSyncModuleName(sync.getClass());
    if (LOGGER.isInfoEnabled()) {
      final String targetProjectKey = targetClient.getConfig().getProjectKey();
      LOGGER.info(
          format(
              "Starting %s from CTP project with key '%s' to project with key '%s'",
              syncModuleName, sourceProjectKey, targetProjectKey));
    }

    return customObjectService
        .getCurrentCtpTimestamp(runnerName, syncModuleName)
        .thenCompose(
            currentCtpTimestamp ->
                syncResourcesSinceLastSync(
                    sourceProjectKey, syncModuleName, runnerName, currentCtpTimestamp))
        .thenAccept(
            ignoredResult -> {
              if (LOGGER.isInfoEnabled()) {
                logStatistics(sync.getStatistics(), LOGGER);
              }
            });
  }

  @Nonnull
  private CompletionStage<CustomObject<LastSyncCustomObject>> syncResourcesSinceLastSync(
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
                    syncDurationInMillis));
  }

  @Nonnull
  private CompletionStage<C> getQueryOfResourcesSinceLastSync(
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
  private C getQueryWithTimeBoundedPredicate(
      @Nonnull final ZonedDateTime lowerBound, @Nonnull final ZonedDateTime upperBound) {

    final QueryPredicate<T> queryPredicate =
        QueryPredicate.of(
            format(
                "lastModifiedAt >= \"%s\" AND lastModifiedAt <= \"%s\"", lowerBound, upperBound));
    return getQuery().plusPredicates(queryPredicate);
  }

  @Nonnull
  private CompletionStage<Long> sync(@Nonnull final C queryResourcesSinceLastSync) {

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
      @Nonnull final String runnerName,
      @Nonnull final ZonedDateTime newLastSyncTimestamp,
      final long syncDurationInMillis) {

    final LastSyncCustomObject<U> lastSyncCustomObject =
        LastSyncCustomObject.of(newLastSyncTimestamp, sync.getStatistics(), syncDurationInMillis);

    return customObjectService.createLastSyncCustomObject(
        sourceProjectKey, syncModuleName, runnerName, lastSyncCustomObject);
  }

  /**
   * Given a {@link List} representing a page of resources of type {@code T}, this method creates a
   * {@link CompletableFuture} of each sync process on the given page as a batch.
   */
  @Nonnull
  private U syncPage(@Nonnull final List<T> page) {

    final List<S> draftsWithKeysInReferences = transform(page);
    return sync.sync(draftsWithKeysInReferences).toCompletableFuture().join();
  }

  /**
   * Given a {@link List} representing a page of resources of type {@code T}, this method creates a
   * a list of drafts of type {@link S} where reference ids of the references are replaced with keys
   * and are ready for reference resolution by the sync process.
   *
   * @return list of drafts of type {@link S}.
   */
  @Nonnull
  protected abstract List<S> transform(@Nonnull final List<T> page);

  @Nonnull
  protected abstract C getQuery();

  public B getSync() {
    return sync;
  }
}
