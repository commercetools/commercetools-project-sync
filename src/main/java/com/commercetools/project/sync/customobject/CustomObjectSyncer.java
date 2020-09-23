package com.commercetools.project.sync.customobject;

import static com.commercetools.project.sync.util.SyncUtils.logErrorCallback;
import static com.commercetools.project.sync.util.SyncUtils.logWarningCallback;

import com.commercetools.project.sync.Syncer;
import com.commercetools.project.sync.service.CustomObjectService;
import com.commercetools.project.sync.service.impl.CustomObjectServiceImpl;
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
import javax.annotation.Nonnull;
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
  private CustomObjectSyncer(
      @Nonnull CustomObjectSync sync,
      @Nonnull SphereClient sourceClient,
      @Nonnull SphereClient targetClient,
      @Nonnull CustomObjectService customObjectService,
      @Nonnull Clock clock) {
    super(sync, sourceClient, targetClient, customObjectService, clock);
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
    // todo: exclude custom objects created by the project-sync from syncing
    return CustomObjectQuery.ofJsonNode();
  }

  @Nonnull
  public static CustomObjectSyncer of(
      @Nonnull final SphereClient sourceClient,
      @Nonnull final SphereClient targetClient,
      @Nonnull final Clock clock) {
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
        customObjectSyncer, sourceClient, targetClient, customObjectService, clock);
  }

  private static String getCustomObjectResourceIdentifier(
      @Nonnull Optional<CustomObject<JsonNode>> oldResource) {
    return oldResource
        .map(CustomObjectCompositeIdentifier::of)
        .map(CustomObjectCompositeIdentifier::toString)
        .orElse("");
  }
}
