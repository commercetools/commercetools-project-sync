package com.commercetools.project.sync.inventoryentry;

import static com.commercetools.sync.inventories.utils.InventoryReferenceReplacementUtils.replaceInventoriesReferenceIdsWithKeys;

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
import java.util.List;
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
      @Nonnull final CustomObjectService customObjectService) {
    super(inventorySync, sourceClient, targetClient, customObjectService);
  }

  public static InventoryEntrySyncer of(
      @Nonnull final SphereClient sourceClient, @Nonnull final SphereClient targetClient) {

    final InventorySyncOptions syncOptions =
        InventorySyncOptionsBuilder.of(targetClient)
            .errorCallback(LOGGER::error)
            .warningCallback(LOGGER::warn)
            .build();

    final InventorySync inventorySync = new InventorySync(syncOptions);

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(targetClient);

    return new InventoryEntrySyncer(inventorySync, sourceClient, targetClient, customObjectService);
  }

  @Nonnull
  @Override
  protected List<InventoryEntryDraft> transformResourcesToDrafts(
      @Nonnull final List<InventoryEntry> page) {
    return replaceInventoriesReferenceIdsWithKeys(page);
  }

  @Nonnull
  @Override
  public InventoryEntryQuery getQuery() {
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
