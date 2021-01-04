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
import java.util.function.Function;
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

    if (syncOptionValues.length == 1 && SYNC_MODULE_OPTION_ALL.equals(syncOptionValues[0])) {
      isSyncOptionValueBlank(syncOptionValues[0]);
      return Arrays.asList(SyncModuleOption.values());
    } else {
      return Arrays.stream(syncOptionValues)
          .map(
              syncOption -> {
                isSyncOptionValueBlank(syncOption);
                final String trimmedValue = syncOption.trim();
                isSyncOptionValueAll(syncOption);
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

  private static void isSyncOptionValueAll(String syncOptionValue) {
    if (("all").equalsIgnoreCase(syncOptionValue)) {
      final String errorMessage =
          format(
              "Wrong arguments supplied to \"-%s\" or \"--%s\" option! "
                  + "'all' option cannot be passed along with other arguments.\" %s",
              SYNC_MODULE_OPTION_SHORT, SYNC_MODULE_OPTION_LONG, SYNC_MODULE_OPTION_DESCRIPTION);

      throw new IllegalArgumentException(errorMessage);
    }
  }

  private static void isSyncOptionValueBlank(String syncOptionValue) {
    if (isBlank(syncOptionValue)) {
      final String errorMessage =
          format(
              "Blank argument supplied to \"-%s\" or \"--%s\" option! %s",
              SYNC_MODULE_OPTION_SHORT, SYNC_MODULE_OPTION_LONG, SYNC_MODULE_OPTION_DESCRIPTION);

      throw new IllegalArgumentException(errorMessage);
    }
  }

  private static Collection<List<SyncModuleOption>> groupSyncModuleOptions(
      List<SyncModuleOption> syncModuleOptions) {
    Map<SyncModuleOption, Integer> syncModuleOptionWithReferencesCount =
        syncModuleOptions
            .stream()
            .collect(
                Collectors.toMap(
                    Function.identity(),
                    syncModuleOption ->
                        (syncModuleOption.getReferences().isEmpty())
                            ? 1
                            : countReferences(
                                syncModuleOption.getReferences(), syncModuleOptions)));

    Map<Integer, List<SyncModuleOption>> groupedSyncModuleOptions = new TreeMap<>();
    for (Map.Entry<SyncModuleOption, Integer> syncOptionWithReferenceCount :
        syncModuleOptionWithReferencesCount.entrySet()) {
      if (!groupedSyncModuleOptions.containsKey(syncOptionWithReferenceCount.getValue())) {
        List<SyncModuleOption> syncOptionsList = new ArrayList<>();
        for (Map.Entry<SyncModuleOption, Integer> syncModuleOptionIntegerEntry :
            syncModuleOptionWithReferencesCount.entrySet()) {
          if (syncOptionWithReferenceCount
              .getValue()
              .equals(syncModuleOptionIntegerEntry.getValue())) {
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
    int count = 1;
    for (SyncModuleOption value : syncOptionDependencies) {
      if (syncModuleOptions.contains(value)) {
        count += countReferences(value.getReferences(), syncModuleOptions);
      }
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

    Syncer<
            ? extends ResourceView,
            ?,
            ? extends BaseSyncStatistics,
            ? extends BaseSyncOptions<?, ?>,
            ? extends QueryDsl<?, ?>,
            ? extends BaseSync<?, ?, ?>>
        syncer = null;

    switch (syncModuleOption) {
      case CART_DISCOUNT_SYNC:
        syncer =
            CartDiscountSyncer.of(sourceClientSupplier.get(), targetClientSupplier.get(), clock);
        break;
      case PRODUCT_TYPE_SYNC:
        syncer =
            ProductTypeSyncer.of(sourceClientSupplier.get(), targetClientSupplier.get(), clock);
        break;
      case CATEGORY_SYNC:
        syncer = CategorySyncer.of(sourceClientSupplier.get(), targetClientSupplier.get(), clock);
        break;
      case PRODUCT_SYNC:
        syncer = ProductSyncer.of(sourceClientSupplier.get(), targetClientSupplier.get(), clock);
        break;
      case INVENTORY_ENTRY_SYNC:
        syncer =
            InventoryEntrySyncer.of(sourceClientSupplier.get(), targetClientSupplier.get(), clock);
        break;
      case TAX_CATEGORY_SYNC:
        syncer =
            TaxCategorySyncer.of(sourceClientSupplier.get(), targetClientSupplier.get(), clock);
        break;
      case TYPE_SYNC:
        syncer = TypeSyncer.of(sourceClientSupplier.get(), targetClientSupplier.get(), clock);
        break;
      case STATE_SYNC:
        syncer = StateSyncer.of(sourceClientSupplier.get(), targetClientSupplier.get(), clock);
        break;
      case CUSTOM_OBJECT_SYNC:
        syncer =
            CustomObjectSyncer.of(
                sourceClientSupplier.get(),
                targetClientSupplier.get(),
                clock,
                runnerNameOptionValue,
                syncProjectSyncCustomObjects);
        break;
      case CUSTOMER_SYNC:
        syncer = CustomerSyncer.of(sourceClientSupplier.get(), targetClientSupplier.get(), clock);
        break;
      case SHOPPING_LIST_SYNC:
        syncer =
            ShoppingListSyncer.of(sourceClientSupplier.get(), targetClientSupplier.get(), clock);
        break;
    }
    return syncer;
  }
}
