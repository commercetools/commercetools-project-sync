package com.commercetools.project.sync;

import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_CATEGORY_SYNC;
import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_INVENTORY_ENTRY_SYNC;
import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_LONG;
import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_PRODUCT_SYNC;
import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_PRODUCT_TYPE_SYNC;
import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_SHORT;
import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_TYPE_SYNC;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.project.sync.category.CategorySyncer;
import com.commercetools.project.sync.inventoryentry.InventoryEntrySyncer;
import com.commercetools.project.sync.product.ProductSyncer;
import com.commercetools.project.sync.producttype.ProductTypeSyncer;
import com.commercetools.project.sync.type.TypeSyncer;
import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.Resource;
import io.sphere.sdk.queries.QueryDsl;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SyncerFactory {

  static final String AVAILABLE_OPTIONS =
      format(
          "Please use any of the following options: \"%s\", \"%s\", \"%s\", \"%s\", \"%s\".",
          SYNC_MODULE_OPTION_TYPE_SYNC,
          SYNC_MODULE_OPTION_PRODUCT_TYPE_SYNC,
          SYNC_MODULE_OPTION_CATEGORY_SYNC,
          SYNC_MODULE_OPTION_PRODUCT_SYNC,
          SYNC_MODULE_OPTION_INVENTORY_ENTRY_SYNC);

  private static final Logger LOGGER = LoggerFactory.getLogger(CliRunner.class);

  private SphereClient targetClient;
  private SphereClient sourceClient;

  private SyncerFactory(
      @Nonnull final SphereClient sourceClient, @Nonnull final SphereClient targetClient) {
    this.targetClient = targetClient;
    this.sourceClient = sourceClient;
  }

  @Nonnull
  public static SyncerFactory of(
      @Nonnull final SphereClient sourceClient, @Nonnull final SphereClient targetClient) {
    return new SyncerFactory(sourceClient, targetClient);
  }

  @Nonnull
  public CompletableFuture<Void> syncAll() {

    final List<CompletableFuture<Void>> typeAndProductTypeSync =
        asList(
            ProductTypeSyncer.of(sourceClient, targetClient).sync().toCompletableFuture(),
            TypeSyncer.of(sourceClient, targetClient).sync().toCompletableFuture());

    return CompletableFuture.allOf(typeAndProductTypeSync.toArray(new CompletableFuture[0]))
        .thenCompose(ignored -> CategorySyncer.of(sourceClient, targetClient).sync())
        .thenCompose(ignored -> ProductSyncer.of(sourceClient, targetClient).sync())
        .thenCompose(ignored -> InventoryEntrySyncer.of(sourceClient, targetClient).sync())
        .whenComplete((syncResult, throwable) -> handleSyncCompletion(throwable));
  }

  private void handleSyncCompletion(@Nullable final Throwable throwable) {
    if (throwable != null) {
      final String errorMessage = "Failed to execute sync process";
      System.out.println(errorMessage); // NOPMD
      LOGGER.error(errorMessage, throwable);
    }
    sourceClient.close();
    targetClient.close();
  }

  @Nonnull
  public CompletionStage<Void> sync(@Nullable final String syncOptionValue) {

    if (isBlank(syncOptionValue)) {
      final String errorMessage =
          format(
              "Blank argument supplied to \"-%s\" or \"--%s\" option! %s",
              SYNC_MODULE_OPTION_SHORT, SYNC_MODULE_OPTION_LONG, AVAILABLE_OPTIONS);
      throw new IllegalArgumentException(errorMessage);
    }

    return buildSyncer(syncOptionValue)
        .sync()
        .whenComplete((syncResult, throwable) -> handleSyncCompletion(throwable));
  }

  /**
   * Builds an instance of {@link Syncer} corresponding to the passed option value. Acceptable
   * values are either "products" or "productTypes" or "categories" or "inventoryEntries" or
   * "types". Other cases, would cause an {@link IllegalArgumentException} to be thrown.
   *
   * @param syncOptionValue the string value passed to the sync option. Acceptable values are either
   *     "products" or "productTypes" or "categories" or "inventoryEntries" or "types". Other cases,
   *     would cause an {@link IllegalArgumentException} to be thrown.
   * @return The instance of the syncer corresponding to the passed option value.
   * @throws IllegalArgumentException if a wrong option value is passed to the sync option. (Wrong
   *     values are anything other than "types" or "products" or "categories" or "productTypes" or
   *     "inventoryEntries".
   */
  @Nonnull
  private Syncer<
          ? extends Resource,
          ?,
          ? extends BaseSyncStatistics,
          ? extends BaseSyncOptions<?, ?>,
          ? extends QueryDsl<?, ?>,
          ? extends BaseSync<?, ?, ?>>
      buildSyncer(@Nonnull final String syncOptionValue) {

    final String trimmedValue = syncOptionValue.trim();
    switch (trimmedValue) {
      case SYNC_MODULE_OPTION_PRODUCT_TYPE_SYNC:
        return ProductTypeSyncer.of(sourceClient, targetClient);
      case SYNC_MODULE_OPTION_CATEGORY_SYNC:
        return CategorySyncer.of(sourceClient, targetClient);
      case SYNC_MODULE_OPTION_PRODUCT_SYNC:
        return ProductSyncer.of(sourceClient, targetClient);
      case SYNC_MODULE_OPTION_INVENTORY_ENTRY_SYNC:
        return InventoryEntrySyncer.of(sourceClient, targetClient);
      case SYNC_MODULE_OPTION_TYPE_SYNC:
        return TypeSyncer.of(sourceClient, targetClient);
      default:
        final String errorMessage =
            format(
                "Unknown argument \"%s\" supplied to \"-%s\" or \"--%s\" option! %s",
                syncOptionValue,
                SYNC_MODULE_OPTION_SHORT,
                SYNC_MODULE_OPTION_LONG,
                AVAILABLE_OPTIONS);
        throw new IllegalArgumentException(errorMessage);
    }
  }
}
