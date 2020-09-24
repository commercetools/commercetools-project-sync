package com.commercetools.project.sync;

import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_CART_DISCOUNT_SYNC;
import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_CATEGORY_SYNC;
import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_CUSTOM_OBJECT_SYNC;
import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_DESCRIPTION;
import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_INVENTORY_ENTRY_SYNC;
import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_LONG;
import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_PRODUCT_SYNC;
import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_PRODUCT_TYPE_SYNC;
import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_SHORT;
import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_STATE_SYNC;
import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_TAX_CATEGORY_SYNC;
import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_TYPE_SYNC;
import static io.sphere.sdk.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.project.sync.cartdiscount.CartDiscountSyncer;
import com.commercetools.project.sync.category.CategorySyncer;
import com.commercetools.project.sync.customobject.CustomObjectSyncer;
import com.commercetools.project.sync.inventoryentry.InventoryEntrySyncer;
import com.commercetools.project.sync.product.ProductSyncer;
import com.commercetools.project.sync.producttype.ProductTypeSyncer;
import com.commercetools.project.sync.state.StateSyncer;
import com.commercetools.project.sync.taxcategory.TaxCategorySyncer;
import com.commercetools.project.sync.type.TypeSyncer;
import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.ResourceView;
import io.sphere.sdk.queries.QueryDsl;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class SyncerFactory {
  private Supplier<SphereClient> targetClientSupplier;
  private Supplier<SphereClient> sourceClientSupplier;
  private Clock clock;

  private SyncerFactory(
      @Nonnull final Supplier<SphereClient> sourceClient,
      @Nonnull final Supplier<SphereClient> targetClient,
      @Nonnull final Clock clock) {
    this.targetClientSupplier = targetClient;
    this.sourceClientSupplier = sourceClient;
    this.clock = clock;
  }

  @Nonnull
  public static SyncerFactory of(
      @Nonnull final Supplier<SphereClient> sourceClient,
      @Nonnull final Supplier<SphereClient> targetClient,
      @Nonnull final Clock clock) {
    return new SyncerFactory(sourceClient, targetClient, clock);
  }

  @Nonnull
  CompletableFuture<Void> syncAll(
      @Nullable final String runnerNameOptionValue, final boolean isFullSync) {

    final SphereClient sourceClient = sourceClientSupplier.get();
    final SphereClient targetClient = targetClientSupplier.get();

    final List<CompletableFuture<Void>>
        typeAndProductTypeAndStateAndTaxCategoryAndCustomObjectSync =
            asList(
                ProductTypeSyncer.of(sourceClient, targetClient, clock)
                    .sync(runnerNameOptionValue, isFullSync)
                    .toCompletableFuture(),
                TypeSyncer.of(sourceClient, targetClient, clock)
                    .sync(runnerNameOptionValue, isFullSync)
                    .toCompletableFuture(),
                StateSyncer.of(sourceClient, targetClient, clock)
                    .sync(runnerNameOptionValue, isFullSync)
                    .toCompletableFuture(),
                TaxCategorySyncer.of(sourceClient, targetClient, clock)
                    .sync(runnerNameOptionValue, isFullSync)
                    .toCompletableFuture(),
                CustomObjectSyncer.of(sourceClient, targetClient, clock)
                    .sync(runnerNameOptionValue, isFullSync)
                    .toCompletableFuture());

    return CompletableFuture.allOf(
            typeAndProductTypeAndStateAndTaxCategoryAndCustomObjectSync.toArray(
                new CompletableFuture[0]))
        .thenCompose(
            ignored -> {
              final List<CompletableFuture<Void>> categoryAndInventoryAndCartDiscountSync =
                  asList(
                      CategorySyncer.of(sourceClient, targetClient, clock)
                          .sync(runnerNameOptionValue, isFullSync)
                          .toCompletableFuture(),
                      CartDiscountSyncer.of(sourceClient, targetClient, clock)
                          .sync(runnerNameOptionValue, isFullSync)
                          .toCompletableFuture(),
                      InventoryEntrySyncer.of(sourceClient, targetClient, clock)
                          .sync(runnerNameOptionValue, isFullSync)
                          .toCompletableFuture());
              return CompletableFuture.allOf(
                  categoryAndInventoryAndCartDiscountSync.toArray(new CompletableFuture[0]));
            })
        .thenCompose(
            ignored ->
                ProductSyncer.of(sourceClient, targetClient, clock)
                    .sync(runnerNameOptionValue, isFullSync))
        .whenComplete((syncResult, throwable) -> closeClients());
  }

  private void closeClients() {
    sourceClientSupplier.get().close();
    targetClientSupplier.get().close();
  }

  @Nonnull
  CompletionStage<Void> sync(
      @Nullable final String syncOptionValue,
      @Nullable final String runnerNameOptionValue,
      final boolean isFullSync) {

    if (isBlank(syncOptionValue)) {
      final String errorMessage =
          format(
              "Blank argument supplied to \"-%s\" or \"--%s\" option! %s",
              SYNC_MODULE_OPTION_SHORT, SYNC_MODULE_OPTION_LONG, SYNC_MODULE_OPTION_DESCRIPTION);

      return exceptionallyCompletedFuture(new IllegalArgumentException(errorMessage));
    }

    Syncer<
            ? extends ResourceView,
            ?,
            ? extends BaseSyncStatistics,
            ? extends BaseSyncOptions<?, ?>,
            ? extends QueryDsl<?, ?>,
            ? extends BaseSync<?, ?, ?>>
        syncer;

    try {
      syncer = buildSyncer(syncOptionValue);
    } catch (IllegalArgumentException exception) {

      return exceptionallyCompletedFuture(exception);
    }

    return syncer
        .sync(runnerNameOptionValue, isFullSync)
        .whenComplete((syncResult, throwable) -> closeClients());
  }

  /**
   * Builds an instance of {@link Syncer} corresponding to the passed option value.
   *
   * @param syncOptionValue the string value passed to the sync option.
   * @return The instance of the syncer corresponding to the passed option value.
   * @throws IllegalArgumentException if a wrong option value is passed to the sync option.
   */
  @Nonnull
  private Syncer<
          ? extends ResourceView,
          ?,
          ? extends BaseSyncStatistics,
          ? extends BaseSyncOptions<?, ?>,
          ? extends QueryDsl<?, ?>,
          ? extends BaseSync<?, ?, ?>>
      buildSyncer(@Nonnull final String syncOptionValue) {

    final String trimmedValue = syncOptionValue.trim();
    switch (trimmedValue) {
      case SYNC_MODULE_OPTION_CART_DISCOUNT_SYNC:
        return CartDiscountSyncer.of(sourceClientSupplier.get(), targetClientSupplier.get(), clock);
      case SYNC_MODULE_OPTION_PRODUCT_TYPE_SYNC:
        return ProductTypeSyncer.of(sourceClientSupplier.get(), targetClientSupplier.get(), clock);
      case SYNC_MODULE_OPTION_CATEGORY_SYNC:
        return CategorySyncer.of(sourceClientSupplier.get(), targetClientSupplier.get(), clock);
      case SYNC_MODULE_OPTION_PRODUCT_SYNC:
        return ProductSyncer.of(sourceClientSupplier.get(), targetClientSupplier.get(), clock);
      case SYNC_MODULE_OPTION_INVENTORY_ENTRY_SYNC:
        return InventoryEntrySyncer.of(
            sourceClientSupplier.get(), targetClientSupplier.get(), clock);
      case SYNC_MODULE_OPTION_TAX_CATEGORY_SYNC:
        return TaxCategorySyncer.of(sourceClientSupplier.get(), targetClientSupplier.get(), clock);
      case SYNC_MODULE_OPTION_TYPE_SYNC:
        return TypeSyncer.of(sourceClientSupplier.get(), targetClientSupplier.get(), clock);
      case SYNC_MODULE_OPTION_STATE_SYNC:
        return StateSyncer.of(sourceClientSupplier.get(), targetClientSupplier.get(), clock);
      case SYNC_MODULE_OPTION_CUSTOM_OBJECT_SYNC:
        return CustomObjectSyncer.of(sourceClientSupplier.get(), targetClientSupplier.get(), clock);
      default:
        final String errorMessage =
            format(
                "Unknown argument \"%s\" supplied to \"-%s\" or \"--%s\" option! %s",
                syncOptionValue,
                SYNC_MODULE_OPTION_SHORT,
                SYNC_MODULE_OPTION_LONG,
                SYNC_MODULE_OPTION_DESCRIPTION);
        throw new IllegalArgumentException(errorMessage);
    }
  }
}
