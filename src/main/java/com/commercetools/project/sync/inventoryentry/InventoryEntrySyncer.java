package com.commercetools.project.sync.inventoryentry;

import static com.commercetools.project.sync.util.SyncUtils.IDENTIFIER_NOT_PRESENT;
import static com.commercetools.project.sync.util.SyncUtils.logErrorCallback;
import static com.commercetools.project.sync.util.SyncUtils.logWarningCallback;
import static com.commercetools.sync.inventories.utils.InventoryTransformUtils.toInventoryEntryDrafts;

import com.commercetools.project.sync.Syncer;
import com.commercetools.project.sync.service.CustomObjectService;
import com.commercetools.project.sync.service.impl.CustomObjectServiceImpl;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.inventories.InventorySync;
import com.commercetools.sync.inventories.InventorySyncOptions;
import com.commercetools.sync.inventories.InventorySyncOptionsBuilder;
import com.commercetools.sync.inventories.helpers.InventorySyncStatistics;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InventoryEntrySyncer
    extends Syncer<
        InventoryEntry,
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

    final QuadConsumer<
            SyncException,
            Optional<InventoryEntryDraft>,
            Optional<InventoryEntry>,
            List<UpdateAction<InventoryEntry>>>
        logErrorCallback =
            (exception, newResourceDraft, oldResource, updateActions) ->
                logErrorCallback(
                    LOGGER,
                    "inventory entry",
                    exception,
                    oldResource.map(InventoryEntry::getSku).orElse(IDENTIFIER_NOT_PRESENT),
                    updateActions);
    final TriConsumer<SyncException, Optional<InventoryEntryDraft>, Optional<InventoryEntry>>
        logWarningCallback =
            (exception, newResourceDraft, oldResource) ->
                logWarningCallback(
                    LOGGER,
                    "inventory entry",
                    exception,
                    oldResource.map(InventoryEntry::getSku).orElse(IDENTIFIER_NOT_PRESENT));
    final InventorySyncOptions syncOptions =
        InventorySyncOptionsBuilder.of(targetClient)
            .errorCallback(logErrorCallback)
            .warningCallback(logWarningCallback)
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
    final ReferenceIdToKeyCache referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();
    return toInventoryEntryDrafts(getSourceClient(), referenceIdToKeyCache, page);
  }

  @Nonnull
  @Override
  protected InventoryEntryQuery getQuery() {
    return InventoryEntryQuery.of();
  }
}
