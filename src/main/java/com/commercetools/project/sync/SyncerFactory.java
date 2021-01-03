package com.commercetools.project.sync;

import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_ALL;
import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_DESCRIPTION;
import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_LONG;
import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_SHORT;
import static io.sphere.sdk.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static java.lang.String.format;
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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.ResourceView;
import io.sphere.sdk.queries.QueryDsl;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
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

  @SuppressFBWarnings(
      "NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
  @Nonnull
  CompletableFuture<Void> sync(
      @Nonnull final String[] syncOptionValues,
      @Nullable final String runnerNameOptionValue,
      final boolean isFullSync,
      final boolean isSyncProjectSyncCustomObjects) {

    final List<SyncModuleOption> syncModuleOptions;
    try {
      syncModuleOptions = validateAndCollectSyncOptionValues(syncOptionValues);
    } catch (IllegalArgumentException exception) {
      return exceptionallyCompletedFuture(exception);
    }

    final Collection<List<SyncModuleOption>> groupedSyncModuleOptions =
        groupSyncModuleOptions(syncModuleOptions);
    CompletableFuture<Void> stagedSyncersToRunSequentially =
        CompletableFuture.completedFuture(null);

    for (List<SyncModuleOption> syncOptions : groupedSyncModuleOptions) {
      stagedSyncersToRunSequentially =
          stagedSyncersToRunSequentially.thenCompose(
              ignore ->
                  chainSyncExecution(
                      runnerNameOptionValue,
                      isFullSync,
                      isSyncProjectSyncCustomObjects,
                      syncOptions));
    }
    return stagedSyncersToRunSequentially.whenComplete((syncResult, throwable) -> closeClients());
  }

  @Nonnull
  private CompletableFuture<Void> chainSyncExecution(
      @Nullable final String runnerNameOptionValue,
      final boolean isFullSync,
      final boolean isSyncProjectSyncCustomObjects,
      final List<SyncModuleOption> syncOptions) {
    final List<CompletableFuture<Void>> syncersToRunParallel = new ArrayList<>();
    for (SyncModuleOption syncOptionValue : syncOptions) {
      Syncer<
              ? extends ResourceView,
              ?,
              ? extends BaseSyncStatistics,
              ? extends BaseSyncOptions<?, ?>,
              ? extends QueryDsl<?, ?>,
              ? extends BaseSync<?, ?, ?>>
          syncer =
              buildSyncer(syncOptionValue, runnerNameOptionValue, isSyncProjectSyncCustomObjects);
      syncersToRunParallel.add(
          syncer.sync(runnerNameOptionValue, isFullSync).toCompletableFuture());
    }
    return CompletableFuture.allOf(syncersToRunParallel.toArray(new CompletableFuture[0]));
  }

  @Nonnull
  private static List<SyncModuleOption> validateAndCollectSyncOptionValues(
      @Nonnull final String[] syncOptionValues) {

    if (null == syncOptionValues) {
      final String errorMessage =
          format(
              "Unknown argument \"%s\" supplied to \"-%s\" or \"--%s\" option! %s",
              null,
              SYNC_MODULE_OPTION_SHORT,
              SYNC_MODULE_OPTION_LONG,
              SYNC_MODULE_OPTION_DESCRIPTION);
      throw new IllegalArgumentException(errorMessage);
    }
    if (syncOptionValues.length == 1 && SYNC_MODULE_OPTION_ALL.equals(syncOptionValues[0])) {
      return Arrays.asList(SyncModuleOption.values());
    } else {
      return Arrays.stream(syncOptionValues)
          .map(
              syncOption -> {
                if (isBlank(syncOption)) {
                  final String errorMessage =
                      format(
                          "Blank argument supplied to \"-%s\" or \"--%s\" option! %s",
                          SYNC_MODULE_OPTION_SHORT,
                          SYNC_MODULE_OPTION_LONG,
                          SYNC_MODULE_OPTION_DESCRIPTION);

                  throw new IllegalArgumentException(errorMessage);
                }

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
  }

  private static Collection<List<SyncModuleOption>> groupSyncModuleOptions(
      List<SyncModuleOption> syncModuleOptions) {
    Map<SyncModuleOption, Integer> syncModuleOptionWithReferencesCount =
        syncModuleOptions
            .stream()
            .collect(
                Collectors.toMap(
                    key -> key,
                    option ->
                        (option.getReferences().isEmpty())
                            ? 0
                            : countReferences(option.getReferences(), syncModuleOptions)));

    Map<Integer, List<SyncModuleOption>> groupedSyncModuleOptions = new TreeMap<>();
    for (Map.Entry<SyncModuleOption, Integer> syncOptionWithReferenceCount :
        syncModuleOptionWithReferencesCount.entrySet()) {
      if (!groupedSyncModuleOptions.containsKey(syncOptionWithReferenceCount.getValue())) {
        List<SyncModuleOption> syncOptionsList = new ArrayList<>();
        for (Map.Entry<SyncModuleOption, Integer> syncModuleOptionIntegerEntry :
            syncModuleOptionWithReferencesCount.entrySet()) {
          if (0
              == syncOptionWithReferenceCount
                  .getValue()
                  .compareTo(syncModuleOptionIntegerEntry.getValue())) {
            syncOptionsList.add(syncModuleOptionIntegerEntry.getKey());
          }
        }
        groupedSyncModuleOptions.put(syncOptionWithReferenceCount.getValue(), syncOptionsList);
      }
    }
    return groupedSyncModuleOptions.values();
  }

  private static int countReferences(
      List<SyncModuleOption> syncOptionDependencies, List<SyncModuleOption> syncModuleOptions) {
    if (null == syncOptionDependencies || syncOptionDependencies.isEmpty()) {
      return 1;
    }
    int count = 0;
    for (SyncModuleOption value : syncOptionDependencies) {
      if (!syncModuleOptions.contains(value)) {
        continue;
      }
      count += countReferences(value.getReferences(), syncModuleOptions);
    }
    return count;
  }

  private void closeClients() {
    sourceClientSupplier.get().close();
    targetClientSupplier.get().close();
  }

  /**
   * Builds an instance of {@link Syncer} corresponding to the passed option value.
   *
   * @param syncModuleOption the string value passed to the sync option.
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
          @Nonnull final SyncModuleOption syncModuleOption,
          @Nonnull final String runnerNameOptionValue,
          final boolean syncProjectSyncCustomObjects) {

    switch (syncModuleOption) {
      case CART_DISCOUNT_SYNC:
        return CartDiscountSyncer.of(sourceClientSupplier.get(), targetClientSupplier.get(), clock);
      case PRODUCT_TYPE_SYNC:
        return ProductTypeSyncer.of(sourceClientSupplier.get(), targetClientSupplier.get(), clock);
      case CATEGORY_SYNC:
        return CategorySyncer.of(sourceClientSupplier.get(), targetClientSupplier.get(), clock);
      case PRODUCT_SYNC:
        return ProductSyncer.of(sourceClientSupplier.get(), targetClientSupplier.get(), clock);
      case INVENTORY_ENTRY_SYNC:
        return InventoryEntrySyncer.of(
            sourceClientSupplier.get(), targetClientSupplier.get(), clock);
      case TAX_CATEGORY_SYNC:
        return TaxCategorySyncer.of(sourceClientSupplier.get(), targetClientSupplier.get(), clock);
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
        return ShoppingListSyncer.of(sourceClientSupplier.get(), targetClientSupplier.get(), clock);
      default:
        throw new IllegalArgumentException();
    }
  }
}
