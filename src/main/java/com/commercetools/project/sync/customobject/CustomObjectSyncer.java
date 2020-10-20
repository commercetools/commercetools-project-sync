package com.commercetools.project.sync.customobject;

import static com.commercetools.project.sync.util.SyncUtils.logErrorCallback;
import static com.commercetools.project.sync.util.SyncUtils.logWarningCallback;

import com.commercetools.project.sync.SyncModuleOption;
import com.commercetools.project.sync.Syncer;
import com.commercetools.project.sync.service.CustomObjectService;
import com.commercetools.project.sync.service.impl.CustomObjectServiceImpl;
import com.commercetools.project.sync.util.SyncUtils;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.customobjects.CustomObjectSync;
import com.commercetools.sync.customobjects.CustomObjectSyncOptions;
import com.commercetools.sync.customobjects.CustomObjectSyncOptionsBuilder;
import com.commercetools.sync.customobjects.helpers.CustomObjectCompositeIdentifier;
import com.commercetools.sync.customobjects.helpers.CustomObjectSyncStatistics;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CustomObjectSyncer
    extends Syncer<
        CustomObject<JsonNode>,
        CustomObjectDraft<JsonNode>,
        CustomObjectSyncStatistics,
        CustomObjectSyncOptions,
        CustomObjectQuery<JsonNode>,
        CustomObjectSync> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CustomObjectSyncer.class);
  private final String runnerName;

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
   * @param runnerName name for the running sync instance. It is passed as a CLI param from user.
   *                   It is used here to generate names of custom objects created by this application
   *                   so we can exclude/include those custom objects as requested.
   */
  private CustomObjectSyncer(
      @Nonnull CustomObjectSync sync,
      @Nonnull SphereClient sourceClient,
      @Nonnull SphereClient targetClient,
      @Nonnull CustomObjectService customObjectService,
      @Nonnull Clock clock,
      @Nullable String runnerName) {
    super(sync, sourceClient, targetClient, customObjectService, clock);
    this.runnerName = runnerName;
  }

  @Nonnull
  @Override
  protected CompletionStage<List<CustomObjectDraft<JsonNode>>> transform(
      @Nonnull List<CustomObject<JsonNode>> page) {
    final List<CustomObjectDraft<JsonNode>> collect =
        page.stream()
            .map(
                customObject ->
                    CustomObjectDraft.ofUnversionedUpsert(
                        customObject.getContainer(),
                        customObject.getKey(),
                        customObject.getValue()))
            .collect(Collectors.toList());
    return CompletableFuture.completedFuture(collect);
  }

  @Nonnull
  @Override
  protected CustomObjectQuery<JsonNode> getQuery() {
    final List<String> excludedContainerNames = getExcludedContainerNames();
    return CustomObjectQuery.ofJsonNode()
        .plusPredicates(
            customObjectQueryModel ->
                customObjectQueryModel.container().isNotIn(excludedContainerNames));
  }

  @Nonnull
  public static CustomObjectSyncer of(
      @Nonnull final SphereClient sourceClient,
      @Nonnull final SphereClient targetClient,
      @Nonnull final Clock clock,
      @Nullable final String runnerName) {
    final QuadConsumer<
            SyncException,
            Optional<CustomObjectDraft<JsonNode>>,
            Optional<CustomObject<JsonNode>>,
            List<UpdateAction<CustomObject<JsonNode>>>>
        logErrorCallback =
            (exception, newResourceDraft, oldResource, updateActions) -> {
              final String resourceIdentifier = getCustomObjectResourceIdentifier(oldResource);
              updateActions = updateActions == null ? Collections.emptyList() : updateActions;
              logErrorCallback(
                  LOGGER, "customObject", exception, resourceIdentifier, updateActions);
            };
    final TriConsumer<
            SyncException, Optional<CustomObjectDraft<JsonNode>>, Optional<CustomObject<JsonNode>>>
        logWarningCallback =
            (exception, newResourceDraft, oldResource) -> {
              final String resourceIdentifier = getCustomObjectResourceIdentifier(oldResource);
              logWarningCallback(LOGGER, "customObject", exception, resourceIdentifier);
            };
    final CustomObjectSyncOptions syncOptions =
        CustomObjectSyncOptionsBuilder.of(targetClient)
            .errorCallback(logErrorCallback)
            .warningCallback(logWarningCallback)
            .build();

    final CustomObjectSync customObjectSyncer = new CustomObjectSync(syncOptions);

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(targetClient);

    return new CustomObjectSyncer(
        customObjectSyncer, sourceClient, targetClient, customObjectService, clock, runnerName);
  }

  private static String getCustomObjectResourceIdentifier(
      @Nonnull Optional<CustomObject<JsonNode>> oldResource) {
    return oldResource
        .map(CustomObjectCompositeIdentifier::of)
        .map(CustomObjectCompositeIdentifier::toString)
        .orElse("");
  }

  /**
   * This method build container names of all custom objects generated by the project-sync. They're
   * not included in the fetch query for custom objects and thus will not be synced to the target
   * projects.
   */
  private List<String> getExcludedContainerNames() {
    final List<String> lastSyncTimestampContainerNames =
        Stream.of(SyncModuleOption.values())
            .map(
                syncModuleOption -> {
                  final String moduleName = syncModuleOption.getSyncModuleName();
                  return SyncUtils.buildLastSyncTimestampContainerName(moduleName, this.runnerName);
                })
            .collect(Collectors.toList());
    final List<String> currentCtpTimestampContainerNames =
        Stream.of(SyncModuleOption.values())
            .map(
                syncModuleOption -> {
                  final String moduleName = syncModuleOption.getSyncModuleName();
                  return SyncUtils.buildCurrentCtpTimestampContainerName(
                      moduleName, this.runnerName);
                })
            .collect(Collectors.toList());
    final List<String> excludedContainerNames =
        Stream.concat(
                lastSyncTimestampContainerNames.stream(),
                currentCtpTimestampContainerNames.stream())
            .collect(Collectors.toList());
    return excludedContainerNames;
  }
}
