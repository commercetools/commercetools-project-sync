package com.commercetools.project.sync;

import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_DESCRIPTION;
import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_LONG;
import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_SHORT;
import static com.commercetools.project.sync.SyncModuleOption.CART_DISCOUNT_SYNC;
import static com.commercetools.project.sync.SyncModuleOption.CATEGORY_SYNC;
import static com.commercetools.project.sync.SyncModuleOption.CUSTOMER_SYNC;
import static com.commercetools.project.sync.SyncModuleOption.CUSTOM_OBJECT_SYNC;
import static com.commercetools.project.sync.SyncModuleOption.INVENTORY_ENTRY_SYNC;
import static com.commercetools.project.sync.SyncModuleOption.PRODUCT_SYNC;
import static com.commercetools.project.sync.SyncModuleOption.PRODUCT_TYPE_SYNC;
import static com.commercetools.project.sync.SyncModuleOption.SHOPPING_LIST_SYNC;
import static com.commercetools.project.sync.SyncModuleOption.STATE_SYNC;
import static com.commercetools.project.sync.SyncModuleOption.TAX_CATEGORY_SYNC;
import static com.commercetools.project.sync.SyncModuleOption.TYPE_SYNC;
import static io.sphere.sdk.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.project.sync.cartdiscount.CartDiscountSyncer;
import com.commercetools.project.sync.category.CategorySyncer;
import com.commercetools.project.sync.customer.CustomerSyncer;
import com.commercetools.project.sync.customobject.CustomObjectSyncer;
import com.commercetools.project.sync.inventoryentry.InventoryEntrySyncer;
import com.commercetools.project.sync.product.ProductSyncer;
import com.commercetools.project.sync.producttype.ProductTypeSyncer;
import com.commercetools.project.sync.shoppinglist.ShoppingListSyncer;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import java.util.stream.Collectors;
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
      @Nullable final String runnerNameOptionValue,
      final boolean isFullSync,
      final boolean isSyncProjectSyncCustomObjects) {

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
                CustomObjectSyncer.of(
                        sourceClient,
                        targetClient,
                        clock,
                        runnerNameOptionValue,
                        isSyncProjectSyncCustomObjects)
                    .sync(runnerNameOptionValue, isFullSync)
                    .toCompletableFuture());

    return CompletableFuture.allOf(
            typeAndProductTypeAndStateAndTaxCategoryAndCustomObjectSync.toArray(
                new CompletableFuture[0]))
        .thenCompose(
            ignored -> {
              final List<CompletableFuture<Void>> categoryAndInventoryAndCartDiscountAndCustomer =
                  asList(
                      CategorySyncer.of(sourceClient, targetClient, clock)
                          .sync(runnerNameOptionValue, isFullSync)
                          .toCompletableFuture(),
                      CartDiscountSyncer.of(sourceClient, targetClient, clock)
                          .sync(runnerNameOptionValue, isFullSync)
                          .toCompletableFuture(),
                      InventoryEntrySyncer.of(sourceClient, targetClient, clock)
                          .sync(runnerNameOptionValue, isFullSync)
                          .toCompletableFuture(),
                      CustomerSyncer.of(sourceClient, targetClient, clock)
                          .sync(runnerNameOptionValue, isFullSync)
                          .toCompletableFuture());
              return CompletableFuture.allOf(
                  categoryAndInventoryAndCartDiscountAndCustomer.toArray(new CompletableFuture[0]));
            })
        .thenCompose(
            ignored ->
                ProductSyncer.of(sourceClient, targetClient, clock)
                    .sync(runnerNameOptionValue, isFullSync))
        .thenCompose(
            ignored ->
                ShoppingListSyncer.of(sourceClient, targetClient, clock)
                    .sync(runnerNameOptionValue, isFullSync))
        .whenComplete((syncResult, throwable) -> closeClients());
  }

  @Nonnull
  CompletableFuture<Void> syncMultipleResources(
      @Nonnull final String[] syncOptionValues,
      @Nullable final String runnerNameOptionValue,
      final boolean isFullSync,
      final boolean isSyncProjectSyncCustomObjects) {

    final SphereClient sourceClient = sourceClientSupplier.get();
    final SphereClient targetClient = targetClientSupplier.get();

    final List<SyncModuleOption> syncOptions;
    try {
      syncOptions = collectSyncOptionValues(syncOptionValues);
    } catch (IllegalArgumentException exception) {
      return exceptionallyCompletedFuture(exception);
    }

    final List<CompletableFuture<Void>>
        typeAndProductTypeAndStateAndTaxCategoryAndCustomObjectSync =
            buildTypeAndProductTypeAndStateAndTaxCategoryAndCustomObject(
                syncOptions,
                runnerNameOptionValue,
                isFullSync,
                isSyncProjectSyncCustomObjects,
                sourceClient,
                targetClient);

    final List<CompletableFuture<Void>> categoryAndInventoryAndCartDiscountAndCustomer =
        buildCategoryAndInventoryAndCartDiscountAndCustomer(
            syncOptions, runnerNameOptionValue, isFullSync, sourceClient, targetClient);

    CompletableFuture<Void> completableFuture =
        CompletableFuture.allOf(
            typeAndProductTypeAndStateAndTaxCategoryAndCustomObjectSync.toArray(
                new CompletableFuture[0]));

    CompletableFuture<Void> completableFuture2 =
        categoryAndInventoryAndCartDiscountAndCustomer.isEmpty()
            ? completableFuture
            : buildCategoryAndInventoryAndCartDiscountAndCustomerSync(
                categoryAndInventoryAndCartDiscountAndCustomer, completableFuture);

    CompletableFuture<Void> completableFuture3 =
        syncOptions.contains(PRODUCT_SYNC)
            ? buildProductSync(
                runnerNameOptionValue, isFullSync, sourceClient, targetClient, completableFuture2)
            : completableFuture2;

    CompletableFuture<Void> completableFuture4 =
        syncOptions.contains(SHOPPING_LIST_SYNC)
            ? buildShoppingListsSync(
                runnerNameOptionValue, isFullSync, sourceClient, targetClient, completableFuture3)
            : completableFuture3;

    return completableFuture4.whenComplete((syncResult, throwable) -> closeClients());
  }

  @Nonnull
  private List<SyncModuleOption> collectSyncOptionValues(@Nonnull final String[] syncOptionValues) {

    return Arrays.stream(syncOptionValues)
        .map(
            syncOption -> {
              final String trimmedValue = syncOption.trim();
              try {
                return SyncModuleOption.getSyncModuleOptionBySyncOptionValue(trimmedValue);
              } catch (IllegalArgumentException e) {
                final String errorMessage =
                    format(
                        "Unknown argument \"%s\" supplied to \"-%s\" or \"--%s\" option! %s",
                        trimmedValue,
                        SYNC_MODULE_OPTION_SHORT,
                        SYNC_MODULE_OPTION_LONG,
                        SYNC_MODULE_OPTION_DESCRIPTION);
                throw new IllegalArgumentException(errorMessage);
              }
            })
        .collect(Collectors.toList());
  }

  private CompletableFuture<Void> buildCategoryAndInventoryAndCartDiscountAndCustomerSync(
      final List<CompletableFuture<Void>> categoryAndInventoryAndCartDiscountAndCustomer,
      final CompletableFuture<Void> completableFuture) {

    return completableFuture.thenCompose(
        ignored ->
            CompletableFuture.allOf(
                categoryAndInventoryAndCartDiscountAndCustomer.toArray(new CompletableFuture[0])));
  }

  private CompletableFuture<Void> buildShoppingListsSync(
      final @Nullable String runnerNameOptionValue,
      final boolean isFullSync,
      final SphereClient sourceClient,
      final SphereClient targetClient,
      final CompletableFuture<Void> completableFuture3) {

    return completableFuture3.thenCompose(
        ignored ->
            ShoppingListSyncer.of(sourceClient, targetClient, clock)
                .sync(runnerNameOptionValue, isFullSync));
  }

  private CompletableFuture<Void> buildProductSync(
      @Nullable final String runnerNameOptionValue,
      final boolean isFullSync,
      final SphereClient sourceClient,
      final SphereClient targetClient,
      final CompletableFuture<Void> completableFuture2) {

    return completableFuture2.thenCompose(
        ignored ->
            ProductSyncer.of(sourceClient, targetClient, clock)
                .sync(runnerNameOptionValue, isFullSync));
  }

  private List<CompletableFuture<Void>>
      buildTypeAndProductTypeAndStateAndTaxCategoryAndCustomObject(
          @Nonnull List<SyncModuleOption> syncOptions,
          @Nullable final String runnerNameOptionValue,
          final boolean isFullSync,
          final boolean isSyncProjectSyncCustomObjects,
          final SphereClient sourceClient,
          final SphereClient targetClient) {

    List<CompletableFuture<Void>> typeAndProductTypeAndStateAndTaxCategoryAndCustomObjectSync =
        new ArrayList<>();

    if (syncOptions.contains(PRODUCT_TYPE_SYNC)) {
      typeAndProductTypeAndStateAndTaxCategoryAndCustomObjectSync.add(
          ProductTypeSyncer.of(sourceClient, targetClient, clock)
              .sync(runnerNameOptionValue, isFullSync)
              .toCompletableFuture());
    }
    if (syncOptions.contains(TYPE_SYNC)) {
      typeAndProductTypeAndStateAndTaxCategoryAndCustomObjectSync.add(
          TypeSyncer.of(sourceClient, targetClient, clock)
              .sync(runnerNameOptionValue, isFullSync)
              .toCompletableFuture());
    }
    if (syncOptions.contains(STATE_SYNC)) {
      typeAndProductTypeAndStateAndTaxCategoryAndCustomObjectSync.add(
          StateSyncer.of(sourceClient, targetClient, clock)
              .sync(runnerNameOptionValue, isFullSync)
              .toCompletableFuture());
    }
    if (syncOptions.contains(TAX_CATEGORY_SYNC)) {
      typeAndProductTypeAndStateAndTaxCategoryAndCustomObjectSync.add(
          TaxCategorySyncer.of(sourceClient, targetClient, clock)
              .sync(runnerNameOptionValue, isFullSync)
              .toCompletableFuture());
    }
    if (syncOptions.contains(CUSTOM_OBJECT_SYNC)) {
      typeAndProductTypeAndStateAndTaxCategoryAndCustomObjectSync.add(
          CustomObjectSyncer.of(
                  sourceClient,
                  targetClient,
                  clock,
                  runnerNameOptionValue,
                  isSyncProjectSyncCustomObjects)
              .sync(runnerNameOptionValue, isFullSync)
              .toCompletableFuture());
    }

    return typeAndProductTypeAndStateAndTaxCategoryAndCustomObjectSync;
  }

  private List<CompletableFuture<Void>> buildCategoryAndInventoryAndCartDiscountAndCustomer(
      @Nonnull List<SyncModuleOption> syncOptions,
      @Nullable final String runnerNameOptionValue,
      final boolean isFullSync,
      final SphereClient sourceClient,
      final SphereClient targetClient) {
    List<CompletableFuture<Void>> categoryAndInventoryAndCartDiscountAndCustomer =
        new ArrayList<>();

    if (syncOptions.contains(CATEGORY_SYNC)) {
      categoryAndInventoryAndCartDiscountAndCustomer.add(
          CategorySyncer.of(sourceClient, targetClient, clock)
              .sync(runnerNameOptionValue, isFullSync)
              .toCompletableFuture());
    }
    if (syncOptions.contains(CART_DISCOUNT_SYNC)) {
      categoryAndInventoryAndCartDiscountAndCustomer.add(
          CartDiscountSyncer.of(sourceClient, targetClient, clock)
              .sync(runnerNameOptionValue, isFullSync)
              .toCompletableFuture());
    }
    if (syncOptions.contains(INVENTORY_ENTRY_SYNC)) {
      categoryAndInventoryAndCartDiscountAndCustomer.add(
          InventoryEntrySyncer.of(sourceClient, targetClient, clock)
              .sync(runnerNameOptionValue, isFullSync)
              .toCompletableFuture());
    }
    if (syncOptions.contains(CUSTOMER_SYNC)) {
      categoryAndInventoryAndCartDiscountAndCustomer.add(
          CustomerSyncer.of(sourceClient, targetClient, clock)
              .sync(runnerNameOptionValue, isFullSync)
              .toCompletableFuture());
    }

    return categoryAndInventoryAndCartDiscountAndCustomer;
  }

  private void closeClients() {
    sourceClientSupplier.get().close();
    targetClientSupplier.get().close();
  }

  @Nonnull
  CompletionStage<Void> sync(
      @Nullable final String syncOptionValue,
      @Nullable final String runnerNameOptionValue,
      final boolean isFullSync,
      final boolean isSyncProjectSyncCustomObjects) {

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
      syncer = buildSyncer(syncOptionValue, runnerNameOptionValue, isSyncProjectSyncCustomObjects);
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
      buildSyncer(
          @Nonnull final String syncOptionValue,
          @Nonnull final String runnerNameOptionValue,
          final boolean syncProjectSyncCustomObjects) {

    final String trimmedValue = syncOptionValue.trim();
    try {
      final SyncModuleOption syncModuleOption =
          SyncModuleOption.getSyncModuleOptionBySyncOptionValue(trimmedValue);
      switch (syncModuleOption) {
        case CART_DISCOUNT_SYNC:
          return CartDiscountSyncer.of(
              sourceClientSupplier.get(), targetClientSupplier.get(), clock);
        case PRODUCT_TYPE_SYNC:
          return ProductTypeSyncer.of(
              sourceClientSupplier.get(), targetClientSupplier.get(), clock);
        case CATEGORY_SYNC:
          return CategorySyncer.of(sourceClientSupplier.get(), targetClientSupplier.get(), clock);
        case PRODUCT_SYNC:
          return ProductSyncer.of(sourceClientSupplier.get(), targetClientSupplier.get(), clock);
        case INVENTORY_ENTRY_SYNC:
          return InventoryEntrySyncer.of(
              sourceClientSupplier.get(), targetClientSupplier.get(), clock);
        case TAX_CATEGORY_SYNC:
          return TaxCategorySyncer.of(
              sourceClientSupplier.get(), targetClientSupplier.get(), clock);
        case TYPE_SYNC:
          return TypeSyncer.of(sourceClientSupplier.get(), targetClientSupplier.get(), clock);
        case STATE_SYNC:
          return StateSyncer.of(sourceClientSupplier.get(), targetClientSupplier.get(), clock);
        case CUSTOM_OBJECT_SYNC:
          return CustomObjectSyncer.of(
              sourceClientSupplier.get(),
              targetClientSupplier.get(),
              clock,
              runnerNameOptionValue,
              syncProjectSyncCustomObjects);
        case CUSTOMER_SYNC:
          return CustomerSyncer.of(sourceClientSupplier.get(), targetClientSupplier.get(), clock);
        case SHOPPING_LIST_SYNC:
          return ShoppingListSyncer.of(
              sourceClientSupplier.get(), targetClientSupplier.get(), clock);
        default:
          throw new IllegalArgumentException();
      }
    } catch (IllegalArgumentException e) {
      final String errorMessage =
          format(
              "Unknown argument \"%s\" supplied to \"-%s\" or \"--%s\" option! %s",
              trimmedValue,
              SYNC_MODULE_OPTION_SHORT,
              SYNC_MODULE_OPTION_LONG,
              SYNC_MODULE_OPTION_DESCRIPTION);
      throw new IllegalArgumentException(errorMessage);
    }
  }
}
