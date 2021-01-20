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
import com.commercetools.project.sync.exception.CLIException;
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
    } catch (CLIException exception) {
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
                  throw new CLIException(errorMessage);
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

      throw new CLIException(errorMessage);
    }
  }

  private static void isSyncOptionValueBlank(String syncOptionValue) {
    if (isBlank(syncOptionValue)) {
      final String errorMessage =
          format(
              "Blank argument supplied to \"-%s\" or \"--%s\" option! %s",
              SYNC_MODULE_OPTION_SHORT, SYNC_MODULE_OPTION_LONG, SYNC_MODULE_OPTION_DESCRIPTION);

      throw new CLIException(errorMessage);
    }
  }

  /**
   * Algorithm to group SyncModuleOptions based on their EssentialSyncOptions count and run sync
   * sequentially for each group in count order(Ascending). This grouping will help the program to
   * run efficiently with proper order and better performance.
   *
   * <p>Example: When the given arguments for sync are [types,productTypes,products,shoppingLists].
   * This will return as [types, productTypes] [products] [shoppingLists]
   *
   * <p>Order is important here to make sure the resources run after their EssentialSyncOptions sync
   * is completed. In the above example, Products sync is run after the group [types, productTypes]
   * which can be referenced in Products. ShoppingLists sync can have Product references, So it will
   * run after Products sync.
   *
   * @param syncModuleOptions list of SyncModuleOption values passed as arguments.
   * @return The Collection of SyncModuleOptions grouped accordingly with their essentialSyncOptions
   *     count.
   */
  private static Collection<List<SyncModuleOption>> groupSyncModuleOptions(
      List<SyncModuleOption> syncModuleOptions) {
    Map<SyncModuleOption, Integer> syncModuleOptionWithEssentialSyncOptionsCount =
        syncModuleOptions
            .stream()
            .collect(
                Collectors.toMap(
                    Function.identity(),
                    syncModuleOption ->
                        (syncModuleOption.getEssentialSyncOptions().isEmpty())
                            ? 1
                            : countEssentialSyncOptions(
                                syncModuleOption.getEssentialSyncOptions(), syncModuleOptions)));

    Map<Integer, List<SyncModuleOption>> groupedSyncModuleOptions = new TreeMap<>();

    for (Map.Entry<SyncModuleOption, Integer> pivotEntry :
        syncModuleOptionWithEssentialSyncOptionsCount.entrySet()) {
      if (!groupedSyncModuleOptions.containsKey(pivotEntry.getValue())) {
        List<SyncModuleOption> syncOptionsList = new ArrayList<>();

        for (Map.Entry<SyncModuleOption, Integer> iteratingEntry :
            syncModuleOptionWithEssentialSyncOptionsCount.entrySet()) {
          if (pivotEntry.getValue().equals(iteratingEntry.getValue())) {
            syncOptionsList.add(iteratingEntry.getKey());
          }
        }

        groupedSyncModuleOptions.put(pivotEntry.getValue(), syncOptionsList);
      }
    }
    return groupedSyncModuleOptions.values();
  }

  /**
   * Recursive function to count the total number of references for a given resource and its
   * references.
   *
   * @param essentialSyncOptions list of references of a given SyncModuleOption.
   * @param syncModuleOptions list of SyncModuleOption values passed as arguments.
   * @return The number of references to the passed essentialSyncOptions.
   */
  private static int countEssentialSyncOptions(
      List<SyncModuleOption> essentialSyncOptions, List<SyncModuleOption> syncModuleOptions) {
    if (null == essentialSyncOptions || essentialSyncOptions.isEmpty()) {
      return 1;
    }
    int count = 1;
    for (SyncModuleOption value : essentialSyncOptions) {
      if (syncModuleOptions.contains(value)) {
        count += countEssentialSyncOptions(value.getEssentialSyncOptions(), syncModuleOptions);
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
