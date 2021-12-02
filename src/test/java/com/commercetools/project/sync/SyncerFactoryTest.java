package com.commercetools.project.sync;

import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_DESCRIPTION;
import static com.commercetools.project.sync.service.impl.CustomObjectServiceImpl.TIMESTAMP_GENERATOR_KEY;
import static com.commercetools.project.sync.util.SyncUtils.DEFAULT_RUNNER_NAME;
import static com.commercetools.project.sync.util.SyncUtils.getApplicationName;
import static com.commercetools.project.sync.util.TestUtils.assertCartDiscountSyncerLoggingEvents;
import static com.commercetools.project.sync.util.TestUtils.assertCategorySyncerLoggingEvents;
import static com.commercetools.project.sync.util.TestUtils.assertCustomObjectSyncerLoggingEvents;
import static com.commercetools.project.sync.util.TestUtils.assertCustomerSyncerLoggingEvents;
import static com.commercetools.project.sync.util.TestUtils.assertInventoryEntrySyncerLoggingEvents;
import static com.commercetools.project.sync.util.TestUtils.assertProductSyncerLoggingEvents;
import static com.commercetools.project.sync.util.TestUtils.assertProductTypeSyncerLoggingEvents;
import static com.commercetools.project.sync.util.TestUtils.assertShoppingListSyncerLoggingEvents;
import static com.commercetools.project.sync.util.TestUtils.assertStateSyncerLoggingEvents;
import static com.commercetools.project.sync.util.TestUtils.assertTaxCategorySyncerLoggingEvents;
import static com.commercetools.project.sync.util.TestUtils.assertTypeSyncerLoggingEvents;
import static com.commercetools.project.sync.util.TestUtils.getMockedClock;
import static com.commercetools.project.sync.util.TestUtils.mockLastSyncCustomObject;
import static com.commercetools.project.sync.util.TestUtils.stubClientsCustomObjectService;
import static com.commercetools.project.sync.util.TestUtils.verifyInteractionsWithClientAfterSync;
import static io.sphere.sdk.products.ProductProjectionType.STAGED;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commercetools.project.sync.cartdiscount.CartDiscountSyncer;
import com.commercetools.project.sync.category.CategorySyncer;
import com.commercetools.project.sync.customer.CustomerSyncer;
import com.commercetools.project.sync.customobject.CustomObjectSyncer;
import com.commercetools.project.sync.exception.CliException;
import com.commercetools.project.sync.inventoryentry.InventoryEntrySyncer;
import com.commercetools.project.sync.model.response.LastSyncCustomObject;
import com.commercetools.project.sync.product.ProductSyncer;
import com.commercetools.project.sync.producttype.ProductTypeSyncer;
import com.commercetools.project.sync.shoppinglist.ShoppingListSyncer;
import com.commercetools.project.sync.state.StateSyncer;
import com.commercetools.project.sync.taxcategory.TaxCategorySyncer;
import com.commercetools.project.sync.type.TypeSyncer;
import com.commercetools.project.sync.util.MockPagedQueryResult;
import com.commercetools.sync.commons.exceptions.ReferenceTransformException;
import com.commercetools.sync.commons.helpers.ResourceKeyIdGraphQlRequest;
import com.commercetools.sync.commons.models.ResourceIdsGraphQlRequest;
import com.commercetools.sync.commons.models.ResourceKeyId;
import com.commercetools.sync.commons.models.ResourceKeyIdGraphQlResult;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import io.sphere.sdk.cartdiscounts.queries.CartDiscountQuery;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.client.SphereRequest;
import io.sphere.sdk.customers.queries.CustomerQuery;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;
import io.sphere.sdk.customobjects.commands.CustomObjectUpsertCommand;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.queries.ProductProjectionQuery;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.queries.QueryPredicate;
import io.sphere.sdk.shoppinglists.queries.ShoppingListQuery;
import io.sphere.sdk.states.queries.StateQuery;
import io.sphere.sdk.taxcategories.queries.TaxCategoryQuery;
import io.sphere.sdk.types.queries.TypeQuery;
import io.sphere.sdk.utils.CompletableFutureUtils;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

class SyncerFactoryTest {
  private static final TestLogger productSyncerTestLogger =
      TestLoggerFactory.getTestLogger(ProductSyncer.class);
  private static final TestLogger cliRunnerTestLogger =
      TestLoggerFactory.getTestLogger(CliRunner.class);
  private static final TestLogger productTypeSyncerTestLogger =
      TestLoggerFactory.getTestLogger(ProductTypeSyncer.class);
  private static final TestLogger customerSyncerTestLogger =
      TestLoggerFactory.getTestLogger(CustomerSyncer.class);
  private static final TestLogger shoppingListSyncerTestLogger =
      TestLoggerFactory.getTestLogger(ShoppingListSyncer.class);
  private static final TestLogger stateSyncerTestLogger =
      TestLoggerFactory.getTestLogger(StateSyncer.class);
  private static final TestLogger inventoryEntrySyncerTestLogger =
      TestLoggerFactory.getTestLogger(InventoryEntrySyncer.class);
  private static final TestLogger customObjectSyncerTestLogger =
      TestLoggerFactory.getTestLogger(CustomObjectSyncer.class);
  private static final TestLogger typeSyncerTestLogger =
      TestLoggerFactory.getTestLogger(TypeSyncer.class);
  private static final TestLogger categorySyncerTestLogger =
      TestLoggerFactory.getTestLogger(CategorySyncer.class);
  private static final TestLogger cartDiscountSyncerTestLogger =
      TestLoggerFactory.getTestLogger(CartDiscountSyncer.class);
  private static final TestLogger taxCategorySyncerTestLogger =
      TestLoggerFactory.getTestLogger(TaxCategorySyncer.class);

  @BeforeEach
  void tearDownTest() {
    cliRunnerTestLogger.clearAll();
    productSyncerTestLogger.clearAll();
    productTypeSyncerTestLogger.clearAll();
    customerSyncerTestLogger.clearAll();
    shoppingListSyncerTestLogger.clearAll();
    stateSyncerTestLogger.clearAll();
    inventoryEntrySyncerTestLogger.clearAll();
    customObjectSyncerTestLogger.clearAll();
    typeSyncerTestLogger.clearAll();
    categorySyncerTestLogger.clearAll();
    cartDiscountSyncerTestLogger.clearAll();
    taxCategorySyncerTestLogger.clearAll();
  }

  @Test
  void sync_WithNullOptionValue_ShouldCompleteExceptionallyWithIllegalArgumentException() {
    assertThat(
            SyncerFactory.of(
                    () -> mock(SphereClient.class),
                    () -> mock(SphereClient.class),
                    getMockedClock())
                .sync(new String[] {null}, "myRunnerName", false, false, null))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(CliException.class)
        .withMessageContaining(
            format(
                "Blank argument supplied to \"-s\" or \"--sync\" option! %s",
                SYNC_MODULE_OPTION_DESCRIPTION));
  }

  @Test
  void sync_WithEmptyOptionValue_ShouldCompleteExceptionallyWithIllegalArgumentException() {
    assertThat(
            SyncerFactory.of(
                    () -> mock(SphereClient.class),
                    () -> mock(SphereClient.class),
                    getMockedClock())
                .sync(new String[] {""}, "myRunnerName", false, false, null))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(CliException.class)
        .withMessageContaining(
            format(
                "Blank argument supplied to \"-s\" or \"--sync\" option! %s",
                SYNC_MODULE_OPTION_DESCRIPTION));
  }

  @Test
  void sync_WithUnknownOptionValue_ShouldCompleteExceptionallyWithIllegalArgumentException() {
    final String[] unknownOptionValue = {"anyOption"};

    assertThat(
            SyncerFactory.of(
                    () -> mock(SphereClient.class),
                    () -> mock(SphereClient.class),
                    getMockedClock())
                .sync(unknownOptionValue, "myRunnerName", false, false, null))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(CliException.class)
        .withMessageContaining(
            format(
                "Unknown argument \"%s\" supplied to \"-s\" or \"--sync\" option! %s",
                unknownOptionValue[0], SYNC_MODULE_OPTION_DESCRIPTION));
  }

  @Test
  @SuppressWarnings("unchecked")
  void sync_AsProductsDeltaSync_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    when(sourceClient.execute(any(ProductProjectionQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    final ZonedDateTime currentCtpTimestamp = ZonedDateTime.now();
    stubClientsCustomObjectService(targetClient, currentCtpTimestamp);

    // test
    syncerFactory.sync(new String[] {"products"}, "myRunnerName", false, false, null);

    // assertions
    verify(sourceClient, times(1)).execute(any(ProductProjectionQuery.class));

    verifyTimestampGeneratorCustomObjectUpsertIsCalled(targetClient, "ProductSync", "myRunnerName");
    verifyLastSyncCustomObjectQuery(targetClient, "productSync", "myRunnerName", "foo", 1);
    // verify two custom object upserts : 1. current ctp timestamp and 2. last sync timestamp
    // creation)
    verify(targetClient, times(2)).execute(any(CustomObjectUpsertCommand.class));
    // TODO: Assert on actual last sync timestamp creation in detail after Statistics classes in
    // java-sync library
    // TODO: override #equals method:
    // https://github.com/commercetools/commercetools-sync-java/issues/376
    // TODO: e.g. verifyNewLastSyncCustomObjectCreation(targetClient,
    // currentCtpTimestamp.minusMinutes(2), any(ProductSyncStatistics.class), 0L, "productSync",
    // "foo");
    verifyInteractionsWithClientAfterSync(sourceClient, 1);

    final Condition<LoggingEvent> startLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent.getMessage().contains("Starting ProductSync"),
            "start log");

    final Condition<LoggingEvent> statisticsLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent
                        .getMessage()
                        .contains(
                            "Summary: 0 product(s) were processed in total (0 created, 0 updated, "
                                + "0 failed to sync and 0 product(s) with missing reference(s))."),
            "statistics log");

    assertThat(productSyncerTestLogger.getAllLoggingEvents())
        .hasSize(2)
        .haveExactly(1, startLog)
        .haveExactly(1, statisticsLog);
  }

  @Test
  @SuppressWarnings("unchecked")
  void sync_AsProductsFullSync_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    when(sourceClient.execute(any(ProductProjectionQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    final ZonedDateTime currentCtpTimestamp = ZonedDateTime.now();
    stubClientsCustomObjectService(targetClient, currentCtpTimestamp);

    // test
    syncerFactory.sync(new String[] {"products"}, "myRunnerName", true, false, null);

    // assertions
    verify(sourceClient, times(1)).execute(any(ProductProjectionQuery.class));

    verifyTimestampGeneratorCustomObjectUpsertIsNotCalled(
        targetClient, "ProductSync", "myRunnerName");
    verifyLastSyncCustomObjectQuery(targetClient, "productSync", "myRunnerName", "foo", 0);
    verify(targetClient, times(0)).execute(any(CustomObjectUpsertCommand.class));
    verifyInteractionsWithClientAfterSync(sourceClient, 1);

    final Condition<LoggingEvent> startLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent.getMessage().contains("Starting ProductSync"),
            "start log");

    final Condition<LoggingEvent> statisticsLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent
                        .getMessage()
                        .contains(
                            "Summary: 0 product(s) were processed in total (0 created, 0 updated, "
                                + "0 failed to sync and 0 product(s) with missing reference(s))."),
            "statistics log");

    assertThat(productSyncerTestLogger.getAllLoggingEvents())
        .hasSize(2)
        .haveExactly(1, startLog)
        .haveExactly(1, statisticsLog);
  }

  @Test
  void
      sync_AsProductsFullSyncWithExceptionDuringAttributeReferenceReplacement_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    final ProductProjection product5 =
        SphereJsonUtils.readObjectFromResource("product-key-5.json", Product.class)
            .toProjection(STAGED);
    final ProductProjection product6 =
        SphereJsonUtils.readObjectFromResource("product-key-6.json", Product.class)
            .toProjection(STAGED);
    final PagedQueryResult<ProductProjection> twoProductResult =
        MockPagedQueryResult.of(asList(product5, product6));

    when(sourceClient.execute(any(ProductProjectionQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(twoProductResult));

    when(targetClient.execute(any())).thenReturn(CompletableFuture.completedFuture(null));
    final BadGatewayException badGatewayException = new BadGatewayException("Error!");
    when(targetClient.execute(any(ProductCreateCommand.class)))
        .thenReturn(CompletableFutureUtils.failed(badGatewayException));
    when(sourceClient.execute(any(ResourceIdsGraphQlRequest.class)))
        .thenReturn(CompletableFutureUtils.failed(badGatewayException));

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    // test
    syncerFactory.sync(new String[] {"products"}, "myRunnerName", true, false, null);

    // assertions
    verify(sourceClient, times(1)).execute(any(ProductProjectionQuery.class));
    verify(sourceClient, times(3)).execute(any(ResourceIdsGraphQlRequest.class));
    verifyInteractionsWithClientAfterSync(sourceClient, 1);

    final Condition<LoggingEvent> startLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent.getMessage().contains("Starting ProductSync"),
            "start log");

    final Condition<LoggingEvent> statisticsLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent
                        .getMessage()
                        .contains(
                            "Summary: 0 product(s) were processed in total (0 created, 0 updated, "
                                + "0 failed to sync and 0 product(s) with missing reference(s))."),
            "statistics log");

    assertThat(productSyncerTestLogger.getAllLoggingEvents())
        .hasSize(3)
        .haveExactly(1, startLog)
        .haveExactly(1, statisticsLog);

    assertThat(productSyncerTestLogger.getAllLoggingEvents())
        .anySatisfy(
            loggingEvent -> {
              assertThat(loggingEvent.getMessage())
                  .contains(
                      ReferenceTransformException.class.getCanonicalName()
                          + ": Failed to replace referenced resource ids with keys on the attributes of the "
                          + "products in the current fetched page from the source project. "
                          + "This page will not be synced to the target project.");
              assertThat(loggingEvent.getThrowable().isPresent()).isTrue();
              assertThat(loggingEvent.getThrowable().get().getCause().getCause())
                  .isEqualTo(badGatewayException);
            });
  }

  @Test
  void
      sync_AsProductsFullSyncWithExceptionDuringAttributeReferenceReplacement_ShouldContinueWithPages() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    final ProductProjection product1 =
        SphereJsonUtils.readObjectFromResource("product-key-7.json", Product.class)
            .toProjection(STAGED);
    final ProductProjection product2 =
        SphereJsonUtils.readObjectFromResource("product-key-8.json", Product.class)
            .toProjection(STAGED);
    final ProductProjection product3 =
        SphereJsonUtils.readObjectFromResource("product-key-9.json", Product.class)
            .toProjection(STAGED);

    final List<ProductProjection> fullPageOfProducts =
        IntStream.range(0, 500).mapToObj(o -> product1).collect(Collectors.toList());

    when(sourceClient.execute(any(ProductProjectionQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(MockPagedQueryResult.of(fullPageOfProducts)))
        .thenReturn(
            CompletableFuture.completedFuture(MockPagedQueryResult.of(asList(product3, product2))));

    when(targetClient.execute(any(ProductProjectionQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(MockPagedQueryResult.of(emptyList())));
    when(targetClient.execute(any(ProductTypeQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(MockPagedQueryResult.of(emptyList())));
    when(targetClient.execute(any(CategoryQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(MockPagedQueryResult.of(emptyList())));
    when(targetClient.execute(any(TaxCategoryQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(MockPagedQueryResult.of(emptyList())));
    when(targetClient.execute(any(StateQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(MockPagedQueryResult.of(emptyList())));
    when(targetClient.execute(any(CustomObjectQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(MockPagedQueryResult.of(emptyList())));

    final Product product4 =
        SphereJsonUtils.readObjectFromResource("product-key-8.json", Product.class);

    when(targetClient.execute(any(ProductCreateCommand.class)))
        .thenReturn(CompletableFuture.completedFuture(product4));

    String jsonAsString =
        "{\"results\":[{\"id\":\"53c4a8b4-865f-4b95-b6f2-3e1e70e3d0c1\",\"key\":\"productKey3\"}]}";
    final ResourceKeyIdGraphQlResult productsResult =
        SphereJsonUtils.readObject(jsonAsString, ResourceKeyIdGraphQlResult.class);

    String jsonStringProductTypes =
        "{\"results\":[{\"id\":\"53c4a8b4-865f-4b95-b6f2-3e1e70e3d0c2\",\"key\":\"prodType1\"}]}";
    final ResourceKeyIdGraphQlResult productTypesResult =
        SphereJsonUtils.readObject(jsonStringProductTypes, ResourceKeyIdGraphQlResult.class);

    String jsonStringCategories =
        "{\"results\":[{\"id\":\"53c4a8b4-865f-4b95-b6f2-3e1e70e3d0c3\",\"key\":\"cat1\"}]}";
    final ResourceKeyIdGraphQlResult categoriesResult =
        SphereJsonUtils.readObject(jsonStringCategories, ResourceKeyIdGraphQlResult.class);

    final BadGatewayException badGatewayException = new BadGatewayException("Error!");
    when(sourceClient.execute(any(ResourceIdsGraphQlRequest.class)))
        .thenReturn(CompletableFutureUtils.failed(badGatewayException))
        .thenReturn(CompletableFutureUtils.failed(badGatewayException))
        .thenReturn(CompletableFutureUtils.failed(badGatewayException))
        .thenReturn(CompletableFuture.completedFuture(productsResult))
        .thenReturn(CompletableFuture.completedFuture(categoriesResult))
        .thenReturn(CompletableFuture.completedFuture(productTypesResult));

    final ResourceKeyIdGraphQlResult resourceKeyIdGraphQlResult =
        mock(ResourceKeyIdGraphQlResult.class);
    when(resourceKeyIdGraphQlResult.getResults())
        .thenReturn(
            singleton(new ResourceKeyId("productKey3", "53c4a8b4-865f-4b95-b6f2-3e1e70e3d0c1")));
    when(targetClient.execute(any(ResourceKeyIdGraphQlRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(resourceKeyIdGraphQlResult));

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    // test
    syncerFactory.sync(new String[] {"products"}, "myRunnerName", true, false, null);

    // assertions
    verify(sourceClient, times(2)).execute(any(ProductProjectionQuery.class));
    verify(sourceClient, times(9)).execute(any(ResourceIdsGraphQlRequest.class));
    verifyInteractionsWithClientAfterSync(sourceClient, 1);

    final Condition<LoggingEvent> startLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent.getMessage().contains("Starting ProductSync"),
            "start log");

    final Condition<LoggingEvent> statisticsLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent
                        .getMessage()
                        .contains(
                            "Summary: 2 product(s) were processed in total (2 created, 0 updated, "
                                + "0 failed to sync and 0 product(s) with missing reference(s))."),
            "statistics log");

    assertThat(productSyncerTestLogger.getAllLoggingEvents())
        .hasSize(3)
        .haveExactly(1, startLog)
        .haveExactly(1, statisticsLog);

    assertThat(productSyncerTestLogger.getAllLoggingEvents())
        .anySatisfy(
            loggingEvent -> {
              assertThat(loggingEvent.getMessage())
                  .contains(
                      ReferenceTransformException.class.getCanonicalName()
                          + ": Failed to replace referenced resource ids with keys on the attributes of the "
                          + "products in the current fetched page from the source project. "
                          + "This page will not be synced to the target project.");
              assertThat(loggingEvent.getThrowable().isPresent()).isTrue();
              assertThat(loggingEvent.getThrowable().get().getCause().getCause())
                  .isEqualTo(badGatewayException);
            });
  }

  private static void verifyTimestampGeneratorCustomObjectUpsertIsCalled(
      @Nonnull final SphereClient client,
      @Nonnull final String syncMethodName,
      @Nonnull final String syncRunnerName) {
    final CustomObjectDraft customObjectDraft =
        findTimestampGeneratorCustomObjectUpsert(client, syncMethodName, syncRunnerName);
    assertThat(customObjectDraft).isNotNull();
    assertThat((String) customObjectDraft.getValue())
        .matches(
            "[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}");
  }

  private static void verifyTimestampGeneratorCustomObjectUpsertIsNotCalled(
      @Nonnull final SphereClient client,
      @Nonnull final String syncMethodName,
      @Nonnull final String syncRunnerName) {
    final CustomObjectDraft customObjectDraft =
        findTimestampGeneratorCustomObjectUpsert(client, syncMethodName, syncRunnerName);
    assertThat(customObjectDraft).isNull();
  }

  private static CustomObjectDraft findTimestampGeneratorCustomObjectUpsert(
      @Nonnull SphereClient client,
      @Nonnull String syncMethodName,
      @Nonnull String syncRunnerName) {
    // fact: SphereRequest is a very broad interface and we actually wanted to capture only
    // CustomObjectUpsertCommand.
    // I tried it but argumentcaptor captures also CustomObjectQueryImpl classes, because we call
    // both query and upsert in the mocked SphereClient.
    // This situation throws runtime NPE error later in the method as query doesnt contain a draft.
    // I guess generics doesnt work here as type is not know on compile time.
    // That's why we need to filter instanceof CustomObjectUpsertCommand in the streams.
    final ArgumentCaptor<SphereRequest> sphereClientArgumentCaptor =
        ArgumentCaptor.forClass(CustomObjectUpsertCommand.class);

    verify(client, atLeast(0)).execute(sphereClientArgumentCaptor.capture());
    final List<SphereRequest> allValues = sphereClientArgumentCaptor.getAllValues();
    final CustomObjectDraft customObjectDraft =
        allValues
            .stream()
            .filter(sphereRequest -> sphereRequest instanceof CustomObjectUpsertCommand)
            .map(sphereRequest -> (CustomObjectUpsertCommand) sphereRequest)
            .map(command -> (CustomObjectDraft) command.getDraft())
            .filter(
                draft -> {
                  return draft
                          .getContainer()
                          .equals(
                              format(
                                  "%s.%s.%s.%s",
                                  getApplicationName(),
                                  syncRunnerName,
                                  syncMethodName,
                                  TIMESTAMP_GENERATOR_KEY))
                      && draft.getKey().equals(TIMESTAMP_GENERATOR_KEY);
                })
            .findAny()
            .orElse(null);
    return customObjectDraft;
  }

  private static void verifyLastSyncCustomObjectQuery(
      @Nonnull final SphereClient client,
      @Nonnull final String syncModuleName,
      @Nonnull final String syncRunnerName,
      @Nonnull final String sourceProjectKey,
      final int expectedInvocations) {

    final QueryPredicate<CustomObject<LastSyncCustomObject>> queryPredicate =
        QueryPredicate.of(
            format(
                "container=\"commercetools-project-sync.%s.%s\" AND key=\"%s\"",
                syncRunnerName, syncModuleName, sourceProjectKey));

    verify(client, times(expectedInvocations))
        .execute(CustomObjectQuery.of(LastSyncCustomObject.class).plusPredicates(queryPredicate));
  }

  @Test
  @SuppressWarnings("unchecked")
  void sync_AsCategoriesDeltaSync_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    when(sourceClient.execute(any(CategoryQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    final ZonedDateTime currentCtpTimestamp = ZonedDateTime.now();
    stubClientsCustomObjectService(targetClient, currentCtpTimestamp);

    // test
    syncerFactory.sync(new String[] {"categories"}, null, false, false, null);

    // assertions
    verify(sourceClient, times(1)).execute(any(CategoryQuery.class));
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(
        targetClient, "CategorySync", DEFAULT_RUNNER_NAME);
    verifyLastSyncCustomObjectQuery(targetClient, "categorySync", DEFAULT_RUNNER_NAME, "foo", 1);
    // verify two custom object upserts : 1. current ctp timestamp and 2. last sync timestamp
    // creation)
    verify(targetClient, times(2)).execute(any(CustomObjectUpsertCommand.class));
    // TODO: Assert on actual last sync timestamp creation in detail after Statistics classes in
    // java-sync library
    // TODO: override #equals method:
    // https://github.com/commercetools/commercetools-sync-java/issues/376
    // TODO: e.g. verifyNewLastSyncCustomObjectCreation(targetClient,
    // currentCtpTimestamp.minusMinutes(2), any(ProductSyncStatistics.class), 0L, "productSync",
    // "foo");
    verifyInteractionsWithClientAfterSync(sourceClient, 1);

    final Condition<LoggingEvent> startLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent.getMessage().contains("Starting CategorySync"),
            "start log");

    final Condition<LoggingEvent> statisticsLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent
                        .getMessage()
                        .contains(
                            "Summary: 0 categories were processed in total (0 created, 0 updated, "
                                + "0 failed to sync and 0 categories with a missing parent)."),
            "statistics log");

    assertThat(categorySyncerTestLogger.getAllLoggingEvents())
        .hasSize(2)
        .haveExactly(1, startLog)
        .haveExactly(1, statisticsLog);
  }

  @Test
  @SuppressWarnings("unchecked")
  void sync_AsProductTypesDeltaSync_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    when(sourceClient.execute(any(ProductTypeQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    final ZonedDateTime currentCtpTimestamp = ZonedDateTime.now();
    stubClientsCustomObjectService(targetClient, currentCtpTimestamp);

    // test
    syncerFactory.sync(new String[] {"productTypes"}, "", false, false, null);

    // assertions
    verify(sourceClient, times(1)).execute(any(ProductTypeQuery.class));
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(
        targetClient, "ProductTypeSync", DEFAULT_RUNNER_NAME);
    verifyLastSyncCustomObjectQuery(targetClient, "productTypeSync", DEFAULT_RUNNER_NAME, "foo", 1);
    // verify two custom object upserts : 1. current ctp timestamp and 2. last sync timestamp
    // creation)
    verify(targetClient, times(2)).execute(any(CustomObjectUpsertCommand.class));
    // TODO: Assert on actual last sync timestamp creation in detail after Statistics classes in
    // java-sync library
    // TODO: override #equals method:
    // https://github.com/commercetools/commercetools-sync-java/issues/376
    // TODO: e.g. verifyNewLastSyncCustomObjectCreation(targetClient,
    // currentCtpTimestamp.minusMinutes(2), any(ProductSyncStatistics.class), 0L, "productSync",
    // "foo");
    verifyInteractionsWithClientAfterSync(sourceClient, 1);

    final Condition<LoggingEvent> startLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent.getMessage().contains("Starting ProductTypeSync"),
            "start log");

    final Condition<LoggingEvent> statisticsLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent
                        .getMessage()
                        .contains(
                            "Summary: 0 product types were processed in total (0 created, 0 updated, 0 failed to sync"
                                + " and 0 product types with at least one NestedType or a Set of NestedType attribute"
                                + " definition(s) referencing a missing product type)."),
            "statistics log");

    assertThat(productTypeSyncerTestLogger.getAllLoggingEvents())
        .hasSize(2)
        .haveExactly(1, startLog)
        .haveExactly(1, statisticsLog);
  }

  @Test
  @SuppressWarnings("unchecked")
  void sync_AsTypesDeltaSync_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    when(sourceClient.execute(any(TypeQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));

    final ZonedDateTime currentCtpTimestamp = ZonedDateTime.now();
    stubClientsCustomObjectService(targetClient, currentCtpTimestamp);

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    // test
    syncerFactory.sync(new String[] {"types"}, "foo", false, false, null);

    // assertions
    verify(sourceClient, times(1)).execute(any(TypeQuery.class));
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(targetClient, "TypeSync", "foo");
    verifyLastSyncCustomObjectQuery(targetClient, "typeSync", "foo", "foo", 1);
    // verify two custom object upserts : 1. current ctp timestamp and 2. last sync timestamp
    // creation)
    verify(targetClient, times(2)).execute(any(CustomObjectUpsertCommand.class));
    // TODO: Assert on actual last sync timestamp creation in detail after Statistics classes in
    // java-sync library
    // TODO: override #equals method:
    // https://github.com/commercetools/commercetools-sync-java/issues/376
    // TODO: e.g. verifyNewLastSyncCustomObjectCreation(targetClient,
    // currentCtpTimestamp.minusMinutes(2), any(ProductSyncStatistics.class), 0L, "productSync",
    // "foo");
    verifyInteractionsWithClientAfterSync(sourceClient, 1);

    final Condition<LoggingEvent> startLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent.getMessage().contains("Starting TypeSync"),
            "start log");

    final Condition<LoggingEvent> statisticsLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent
                        .getMessage()
                        .contains(
                            "Summary: 0 types were processed in total (0 created, 0 updated "
                                + "and 0 failed to sync)."),
            "statistics log");

    assertThat(typeSyncerTestLogger.getAllLoggingEvents())
        .hasSize(2)
        .haveExactly(1, startLog)
        .haveExactly(1, statisticsLog);
  }

  @Test
  @SuppressWarnings("unchecked")
  void sync_AsInventoryEntriesDeltaSync_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    when(sourceClient.execute(any(InventoryEntryQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));

    final ZonedDateTime currentCtpTimestamp = ZonedDateTime.now();
    stubClientsCustomObjectService(targetClient, currentCtpTimestamp);

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    // test
    syncerFactory.sync(new String[] {"inventoryEntries"}, null, false, false, null);

    // assertions
    verify(sourceClient, times(1)).execute(any(InventoryEntryQuery.class));
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(
        targetClient, "InventorySync", DEFAULT_RUNNER_NAME);
    verifyLastSyncCustomObjectQuery(targetClient, "inventorySync", DEFAULT_RUNNER_NAME, "foo", 1);
    // verify two custom object upserts : 1. current ctp timestamp and 2. last sync timestamp
    // creation)
    verify(targetClient, times(2)).execute(any(CustomObjectUpsertCommand.class));
    // TODO: Assert on actual last sync timestamp creation in detail after Statistics classes in
    // java-sync library
    // TODO: override #equals method:
    // https://github.com/commercetools/commercetools-sync-java/issues/376
    // TODO: e.g. verifyNewLastSyncCustomObjectCreation(targetClient,
    // currentCtpTimestamp.minusMinutes(2), any(ProductSyncStatistics.class), 0L, "productSync",
    // "foo");
    verifyInteractionsWithClientAfterSync(sourceClient, 1);

    final Condition<LoggingEvent> startLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent.getMessage().contains("Starting InventorySync"),
            "start log");

    final Condition<LoggingEvent> statisticsLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent
                        .getMessage()
                        .contains(
                            "Summary: 0 inventory entries were processed in total (0 created, 0 updated "
                                + "and 0 failed to sync)."),
            "statistics log");

    assertThat(inventoryEntrySyncerTestLogger.getAllLoggingEvents())
        .hasSize(2)
        .haveExactly(1, startLog)
        .haveExactly(1, statisticsLog);
  }

  @Test
  void sync_WithErrorOnFetch_ShouldCloseClientAndCompleteExceptionally() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    final BadGatewayException badGatewayException = new BadGatewayException();
    when(sourceClient.execute(any(InventoryEntryQuery.class)))
        .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(badGatewayException));

    final ZonedDateTime currentCtpTimestamp = ZonedDateTime.now();
    stubClientsCustomObjectService(targetClient, currentCtpTimestamp);

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    // test
    final CompletionStage<Void> result =
        syncerFactory.sync(new String[] {"inventoryEntries"}, null, false, false, null);

    // assertions
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(
        targetClient, "InventorySync", DEFAULT_RUNNER_NAME);
    verify(sourceClient, times(1)).execute(any(InventoryEntryQuery.class));
    verifyInteractionsWithClientAfterSync(sourceClient, 1);
    assertThat(result)
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(BadGatewayException.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  void
      sync_WithErrorOnCurrentCtpTimestampUpsert_ShouldCloseClientAndCompleteExceptionallyWithoutSyncing() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    final BadGatewayException badGatewayException = new BadGatewayException();
    when(targetClient.execute(any(CustomObjectUpsertCommand.class)))
        .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(badGatewayException));

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    // test
    final CompletionStage<Void> result =
        syncerFactory.sync(new String[] {"inventoryEntries"}, "", false, false, null);

    // assertions
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(
        targetClient, "InventorySync", DEFAULT_RUNNER_NAME);
    verify(sourceClient, times(0)).execute(any(InventoryEntryQuery.class));
    verifyInteractionsWithClientAfterSync(sourceClient, 1);
    assertThat(result)
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(BadGatewayException.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  void
      sync_WithErrorOnQueryLastSyncTimestamp_ShouldCloseClientAndCompleteExceptionallyWithoutSyncing() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    final BadGatewayException badGatewayException = new BadGatewayException();
    when(targetClient.execute(any(CustomObjectQuery.class)))
        .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(badGatewayException));

    final CustomObject<LastSyncCustomObject<ProductSyncStatistics>>
        lastSyncCustomObjectCustomObject = mockLastSyncCustomObject(ZonedDateTime.now());
    when(targetClient.execute(any(CustomObjectUpsertCommand.class)))
        .thenReturn(CompletableFuture.completedFuture(lastSyncCustomObjectCustomObject));

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    // test
    final CompletionStage<Void> result =
        syncerFactory.sync(new String[] {"inventoryEntries"}, "bar", false, false, null);

    // assertions
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(targetClient, "InventorySync", "bar");
    verifyLastSyncCustomObjectQuery(targetClient, "inventorySync", "bar", "foo", 1);
    verify(sourceClient, times(0)).execute(any(InventoryEntryQuery.class));
    verifyInteractionsWithClientAfterSync(sourceClient, 1);
    assertThat(result)
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(BadGatewayException.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  void syncAll_AsDelta_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    when(sourceClient.execute(any(ProductTypeQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));
    when(sourceClient.execute(any(TypeQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));
    when(sourceClient.execute(any(StateQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));
    when(sourceClient.execute(any(TaxCategoryQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));
    when(sourceClient.execute(any(CategoryQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));
    when(sourceClient.execute(any(InventoryEntryQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));
    when(sourceClient.execute(any(ProductProjectionQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));
    when(sourceClient.execute(any(CartDiscountQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));
    when(sourceClient.execute(any(CustomObjectQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));
    when(sourceClient.execute(any(CustomerQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));
    when(sourceClient.execute(any(ShoppingListQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));

    final ZonedDateTime currentCtpTimestamp = ZonedDateTime.now();
    stubClientsCustomObjectService(targetClient, currentCtpTimestamp);

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    // test
    syncerFactory.sync(new String[] {"all"}, null, false, false, null).join();

    // assertions
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(
        targetClient, "ProductTypeSync", DEFAULT_RUNNER_NAME);
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(
        targetClient, "ProductSync", DEFAULT_RUNNER_NAME);
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(
        targetClient, "CategorySync", DEFAULT_RUNNER_NAME);
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(
        targetClient, "TypeSync", DEFAULT_RUNNER_NAME);
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(
        targetClient, "InventorySync", DEFAULT_RUNNER_NAME);
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(
        targetClient, "CartDiscountSync", DEFAULT_RUNNER_NAME);
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(
        targetClient, "StateSync", DEFAULT_RUNNER_NAME);
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(
        targetClient, "TaxCategorySync", DEFAULT_RUNNER_NAME);
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(
        targetClient, "CustomObjectSync", DEFAULT_RUNNER_NAME);
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(
        targetClient, "CustomerSync", DEFAULT_RUNNER_NAME);
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(
        targetClient, "ShoppingListSync", DEFAULT_RUNNER_NAME);
    verify(targetClient, times(22)).execute(any(CustomObjectUpsertCommand.class));
    verifyLastSyncCustomObjectQuery(targetClient, "inventorySync", DEFAULT_RUNNER_NAME, "foo", 1);
    verifyLastSyncCustomObjectQuery(targetClient, "productTypeSync", DEFAULT_RUNNER_NAME, "foo", 1);
    verifyLastSyncCustomObjectQuery(targetClient, "productSync", DEFAULT_RUNNER_NAME, "foo", 1);
    verifyLastSyncCustomObjectQuery(targetClient, "categorySync", DEFAULT_RUNNER_NAME, "foo", 1);
    verifyLastSyncCustomObjectQuery(targetClient, "typeSync", DEFAULT_RUNNER_NAME, "foo", 1);
    verifyLastSyncCustomObjectQuery(
        targetClient, "cartDiscountSync", DEFAULT_RUNNER_NAME, "foo", 1);
    verifyLastSyncCustomObjectQuery(targetClient, "stateSync", DEFAULT_RUNNER_NAME, "foo", 1);
    verifyLastSyncCustomObjectQuery(targetClient, "taxCategorySync", DEFAULT_RUNNER_NAME, "foo", 1);
    verifyLastSyncCustomObjectQuery(
        targetClient, "customObjectSync", DEFAULT_RUNNER_NAME, "foo", 1);
    verifyLastSyncCustomObjectQuery(targetClient, "customerSync", DEFAULT_RUNNER_NAME, "foo", 1);
    verifyLastSyncCustomObjectQuery(
        targetClient, "shoppingListSync", DEFAULT_RUNNER_NAME, "foo", 1);
    verify(sourceClient, times(1)).execute(any(ProductTypeQuery.class));
    verify(sourceClient, times(1)).execute(any(TypeQuery.class));
    verify(sourceClient, times(1)).execute(any(CategoryQuery.class));
    verify(sourceClient, times(1)).execute(any(ProductProjectionQuery.class));
    verify(sourceClient, times(1)).execute(any(InventoryEntryQuery.class));
    verify(sourceClient, times(1)).execute(any(CartDiscountQuery.class));
    verify(sourceClient, times(1)).execute(any(StateQuery.class));
    verify(sourceClient, times(1)).execute(any(TaxCategoryQuery.class));
    verify(sourceClient, times(1)).execute(any(CustomObjectQuery.class));
    verify(sourceClient, times(1)).execute(any(CustomerQuery.class));
    verify(sourceClient, times(1)).execute(any(ShoppingListQuery.class));
    verifyInteractionsWithClientAfterSync(sourceClient, 11);

    assertThat(cliRunnerTestLogger.getAllLoggingEvents())
        .allMatch(loggingEvent -> !Level.ERROR.equals(loggingEvent.getLevel()));

    assertThat(productSyncerTestLogger.getAllLoggingEvents())
        .allMatch(loggingEvent -> !Level.ERROR.equals(loggingEvent.getLevel()));

    assertTypeSyncerLoggingEvents(typeSyncerTestLogger, 0);
    assertProductTypeSyncerLoggingEvents(productTypeSyncerTestLogger, 0);
    assertCategorySyncerLoggingEvents(categorySyncerTestLogger, 0);
    assertProductSyncerLoggingEvents(productSyncerTestLogger, 0);
    assertInventoryEntrySyncerLoggingEvents(inventoryEntrySyncerTestLogger, 0);
    assertCartDiscountSyncerLoggingEvents(cartDiscountSyncerTestLogger, 0);
    // +1 state is a built-in state and it cant be deleted
    assertStateSyncerLoggingEvents(stateSyncerTestLogger, 0);
    assertTaxCategorySyncerLoggingEvents(taxCategorySyncerTestLogger, 0);
    assertCustomObjectSyncerLoggingEvents(customObjectSyncerTestLogger, 0);
    assertCustomerSyncerLoggingEvents(customerSyncerTestLogger, 0);
    assertShoppingListSyncerLoggingEvents(shoppingListSyncerTestLogger, 0);

    // Every sync module is expected to have 2 logs (start and stats summary)
    assertThat(typeSyncerTestLogger.getAllLoggingEvents()).hasSize(2);
    assertThat(productTypeSyncerTestLogger.getAllLoggingEvents()).hasSize(2);
    assertThat(categorySyncerTestLogger.getAllLoggingEvents()).hasSize(2);
    assertThat(productSyncerTestLogger.getAllLoggingEvents()).hasSize(2);
    assertThat(inventoryEntrySyncerTestLogger.getAllLoggingEvents()).hasSize(2);
    assertThat(cartDiscountSyncerTestLogger.getAllLoggingEvents()).hasSize(2);
    assertThat(stateSyncerTestLogger.getAllLoggingEvents()).hasSize(2);
    assertThat(taxCategorySyncerTestLogger.getAllLoggingEvents()).hasSize(2);
    assertThat(customObjectSyncerTestLogger.getAllLoggingEvents()).hasSize(2);
    assertThat(customerSyncerTestLogger.getAllLoggingEvents()).hasSize(2);
    assertThat(shoppingListSyncerTestLogger.getAllLoggingEvents()).hasSize(2);
  }

  @Test
  @SuppressWarnings("unchecked")
  void syncProductTypesProductsCustomersAndShoppingLists_AsDelta_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    when(sourceClient.execute(any(ProductTypeQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));
    when(sourceClient.execute(any(ProductProjectionQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));
    when(sourceClient.execute(any(CustomerQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));
    when(sourceClient.execute(any(ShoppingListQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));

    final ZonedDateTime currentCtpTimestamp = ZonedDateTime.now();
    stubClientsCustomObjectService(targetClient, currentCtpTimestamp);

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    // test
    String[] syncModuleOptions = {"productTypes", "products", "customers", "shoppingLists"};
    syncerFactory.sync(syncModuleOptions, null, false, false, null).join();

    // assertions
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(
        targetClient, "ProductTypeSync", DEFAULT_RUNNER_NAME);
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(
        targetClient, "ProductSync", DEFAULT_RUNNER_NAME);
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(
        targetClient, "CustomerSync", DEFAULT_RUNNER_NAME);
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(
        targetClient, "ShoppingListSync", DEFAULT_RUNNER_NAME);
    verify(targetClient, times(8)).execute(any(CustomObjectUpsertCommand.class));
    verifyLastSyncCustomObjectQuery(targetClient, "productTypeSync", DEFAULT_RUNNER_NAME, "foo", 1);
    verifyLastSyncCustomObjectQuery(targetClient, "productSync", DEFAULT_RUNNER_NAME, "foo", 1);
    verifyLastSyncCustomObjectQuery(targetClient, "customerSync", DEFAULT_RUNNER_NAME, "foo", 1);
    verifyLastSyncCustomObjectQuery(
        targetClient, "shoppingListSync", DEFAULT_RUNNER_NAME, "foo", 1);

    final InOrder inOrder = Mockito.inOrder(sourceClient);

    // According to sync algorithm, ProductType and Customer will run sync in parallel, Product and
    // ShoppingList sequentially.
    // Example: Given: ['productTypes', 'customers', 'products', 'shoppingLists']
    // From the given arguments, algorithm will group the resources as below,
    // [productTypes, customers] [products] [shoppingLists]
    inOrder.verify(sourceClient, times(1)).execute(any(ProductTypeQuery.class));
    verify(sourceClient, times(1)).execute(any(CustomerQuery.class));

    inOrder.verify(sourceClient, times(1)).execute(any(ProductProjectionQuery.class));

    inOrder.verify(sourceClient, times(1)).execute(any(ShoppingListQuery.class));
    verifyInteractionsWithClientAfterSync(sourceClient, 4);

    assertProductTypeSyncerLoggingEvents(productTypeSyncerTestLogger, 0);
    assertProductSyncerLoggingEvents(productSyncerTestLogger, 0);
    assertCustomerSyncerLoggingEvents(customerSyncerTestLogger, 0);
    assertShoppingListSyncerLoggingEvents(shoppingListSyncerTestLogger, 0);
  }

  @Test
  @SuppressWarnings("unchecked")
  void syncStatesInventoryEntriesAndCustomObjects_AsDelta_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    when(sourceClient.execute(any(StateQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));
    when(sourceClient.execute(any(InventoryEntryQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));
    when(sourceClient.execute(any(CustomObjectQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));

    final ZonedDateTime currentCtpTimestamp = ZonedDateTime.now();
    stubClientsCustomObjectService(targetClient, currentCtpTimestamp);

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    // test
    String[] syncModuleOptions = {"states", "inventoryEntries", "customObjects"};
    syncerFactory.sync(syncModuleOptions, null, false, false, null).join();

    // assertions
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(
        targetClient, "StateSync", DEFAULT_RUNNER_NAME);
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(
        targetClient, "InventorySync", DEFAULT_RUNNER_NAME);
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(
        targetClient, "CustomObjectSync", DEFAULT_RUNNER_NAME);
    verify(targetClient, times(6)).execute(any(CustomObjectUpsertCommand.class));
    verifyLastSyncCustomObjectQuery(targetClient, "stateSync", DEFAULT_RUNNER_NAME, "foo", 1);
    verifyLastSyncCustomObjectQuery(targetClient, "inventorySync", DEFAULT_RUNNER_NAME, "foo", 1);
    verifyLastSyncCustomObjectQuery(
        targetClient, "customObjectSync", DEFAULT_RUNNER_NAME, "foo", 1);
    verify(sourceClient, times(1)).execute(any(StateQuery.class));
    verify(sourceClient, times(1)).execute(any(InventoryEntryQuery.class));
    verify(sourceClient, times(1)).execute(any(CustomObjectQuery.class));
    verifyInteractionsWithClientAfterSync(sourceClient, 3);

    assertStateSyncerLoggingEvents(stateSyncerTestLogger, 0);
    assertInventoryEntrySyncerLoggingEvents(inventoryEntrySyncerTestLogger, 0);
    assertCustomObjectSyncerLoggingEvents(customObjectSyncerTestLogger, 0);
  }

  @Test
  @SuppressWarnings("unchecked")
  void syncTypesAndCategories_AsDelta_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    when(sourceClient.execute(any(TypeQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));
    when(sourceClient.execute(any(CategoryQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));

    final ZonedDateTime currentCtpTimestamp = ZonedDateTime.now();
    stubClientsCustomObjectService(targetClient, currentCtpTimestamp);

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    // test
    String[] syncModuleOptions = {"types", "categories"};
    syncerFactory.sync(syncModuleOptions, null, false, false, null).join();

    // assertions
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(
        targetClient, "TypeSync", DEFAULT_RUNNER_NAME);
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(
        targetClient, "CategorySync", DEFAULT_RUNNER_NAME);
    verify(targetClient, times(4)).execute(any(CustomObjectUpsertCommand.class));
    verifyLastSyncCustomObjectQuery(targetClient, "typeSync", DEFAULT_RUNNER_NAME, "foo", 1);
    verifyLastSyncCustomObjectQuery(targetClient, "categorySync", DEFAULT_RUNNER_NAME, "foo", 1);

    final InOrder inOrder = Mockito.inOrder(sourceClient);

    inOrder.verify(sourceClient, times(1)).execute(any(TypeQuery.class));
    inOrder.verify(sourceClient, times(1)).execute(any(CategoryQuery.class));
    verifyInteractionsWithClientAfterSync(sourceClient, 2);

    assertTypeSyncerLoggingEvents(typeSyncerTestLogger, 0);
    assertCategorySyncerLoggingEvents(categorySyncerTestLogger, 0);
    assertThat(typeSyncerTestLogger.getAllLoggingEvents()).hasSize(2);
    assertThat(categorySyncerTestLogger.getAllLoggingEvents()).hasSize(2);
  }

  @Test
  @SuppressWarnings("unchecked")
  void syncProductsAndShoppingLists_AsDelta_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    when(sourceClient.execute(any(ProductProjectionQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));
    when(sourceClient.execute(any(ShoppingListQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));

    final ZonedDateTime currentCtpTimestamp = ZonedDateTime.now();
    stubClientsCustomObjectService(targetClient, currentCtpTimestamp);

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    // test
    String[] syncModuleOptions = {"products", "shoppingLists"};
    syncerFactory.sync(syncModuleOptions, null, false, false, null).join();

    // assertions
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(
        targetClient, "ProductSync", DEFAULT_RUNNER_NAME);
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(
        targetClient, "ShoppingListSync", DEFAULT_RUNNER_NAME);
    verify(targetClient, times(4)).execute(any(CustomObjectUpsertCommand.class));
    verifyLastSyncCustomObjectQuery(targetClient, "productSync", DEFAULT_RUNNER_NAME, "foo", 1);
    verifyLastSyncCustomObjectQuery(
        targetClient, "shoppingListSync", DEFAULT_RUNNER_NAME, "foo", 1);
    verify(sourceClient, times(1)).execute(any(ProductProjectionQuery.class));
    verify(sourceClient, times(1)).execute(any(ShoppingListQuery.class));
    verifyInteractionsWithClientAfterSync(sourceClient, 2);

    assertProductSyncerLoggingEvents(productSyncerTestLogger, 0);
    assertShoppingListSyncerLoggingEvents(shoppingListSyncerTestLogger, 0);
    assertThat(productSyncerTestLogger.getAllLoggingEvents()).hasSize(2);
    assertThat(shoppingListSyncerTestLogger.getAllLoggingEvents()).hasSize(2);
  }

  @Test
  void sync_AsDelta_WithOneUnmatchedSyncOptionValue_ShouldResultIllegalArgumentException() {
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    // test
    String[] syncResources = {"productTypes", "unknown", "shoppingLists"};
    CompletionStage<Void> result = syncerFactory.sync(syncResources, null, false, false, null);

    String errorMessage =
        format(
            "Unknown argument \"%s\" supplied to \"-s\" or \"--sync\" option! %s",
            syncResources[1], SYNC_MODULE_OPTION_DESCRIPTION);

    assertThat(result)
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(CliException.class)
        .withMessageContaining(errorMessage);
  }

  @Test
  void sync_AsDelta_WithSyncOptionValuesAndAll_ShouldResultIllegalArgumentException() {
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    // test
    String[] syncResources = {"productTypes", "all", "shoppingLists"};
    CompletionStage<Void> result = syncerFactory.sync(syncResources, null, false, false, null);

    String errorMessage =
        format(
            "Wrong arguments supplied to \"-s\" or \"--sync\" option! "
                + "'all' option cannot be passed along with other arguments.\" %s",
            SYNC_MODULE_OPTION_DESCRIPTION);

    assertThat(result)
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(CliException.class)
        .withMessageContaining(errorMessage);
  }
}
