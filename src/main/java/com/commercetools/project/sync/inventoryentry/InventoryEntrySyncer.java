package com.commercetools.project.sync.inventoryentry;

import static com.commercetools.sync.inventories.utils.InventoryReferenceResolutionUtils.mapToInventoryEntryDrafts;
import static java.lang.String.format;

import com.commercetools.project.sync.Syncer;
import com.commercetools.project.sync.service.CustomObjectService;
import com.commercetools.project.sync.service.impl.CustomObjectServiceImpl;
import com.commercetools.sync.inventories.InventorySync;
import com.commercetools.sync.inventories.InventorySyncOptions;
import com.commercetools.sync.inventories.InventorySyncOptionsBuilder;
import com.commercetools.sync.inventories.helpers.InventorySyncStatistics;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.expansion.ExpansionPath;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.expansion.InventoryEntryExpansionModel;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InventoryEntrySyncer
    extends Syncer<
        InventoryEntry,
        InventoryEntryDraft,
        InventorySyncStatistics,
        InventorySyncOptions,
        InventoryEntryQuery,
        InventorySync> {

  private static final Logger LOGGER = LoggerFactory.getLogger(InventoryEntrySyncer.class);

  /** Instantiates a {@link Syncer} instance. */
  private InventoryEntrySyncer(
      @Nonnull final InventorySync inventorySync,
      @Nonnull final SphereClient sourceClient,
      @Nonnull final SphereClient targetClient,
      @Nonnull final CustomObjectService customObjectService,
      @Nonnull final Clock clock) {
    super(inventorySync, sourceClient, targetClient, customObjectService, clock);
  }

  public static InventoryEntrySyncer of(
      @Nonnull final SphereClient sourceClient,
      @Nonnull final SphereClient targetClient,
      @Nonnull final Clock clock) {

    final InventorySyncOptions syncOptions =
        InventorySyncOptionsBuilder.of(targetClient)
            .errorCallback(
                (exception, newResourceDraft, oldResource, updateActions) -> {
                  LOGGER.error(
                      format(
                          "Error when trying to sync inventory entries. Existing inventory entry sku: %s. Update actions: %s",
                          oldResource.map(InventoryEntry::getSku).orElse(""),
                          updateActions
                              .stream()
                              .map(Object::toString)
                              .collect(Collectors.joining(","))),
                      exception);
                })
            .warningCallback(
                (exception, newResourceDraft, oldResource) -> {
                  LOGGER.warn(
                      format(
                          "Warning when trying to sync inventory entries. Existing inventory entry sku: %s",
                          oldResource.map(InventoryEntry::getSku).orElse("")),
                      exception);
                })
            .build();

    final InventorySync inventorySync = new InventorySync(syncOptions);

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(targetClient);

    return new InventoryEntrySyncer(
        inventorySync, sourceClient, targetClient, customObjectService, clock);
  }

  @Nonnull
  @Override
  protected CompletionStage<List<InventoryEntryDraft>> transform(
      @Nonnull final List<InventoryEntry> page) {
    return CompletableFuture.completedFuture(mapToInventoryEntryDrafts(page));
  }

  @Nonnull
  @Override
  protected InventoryEntryQuery getQuery() {
    return buildQuery();
  }

  /**
   * TODO: Should be added to the commercetools-sync library.
   *
   * @return an {@link InventoryEntryQuery} instance.
   */
  private static InventoryEntryQuery buildQuery() {
    return InventoryEntryQuery.of()
        .withExpansionPaths(InventoryEntryExpansionModel::supplyChannel)
        .plusExpansionPaths(ExpansionPath.of("custom.type"));
  }
}
