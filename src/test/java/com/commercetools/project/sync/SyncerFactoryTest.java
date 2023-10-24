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
import static com.commercetools.project.sync.util.TestUtils.createBadGatewayException;
import static com.commercetools.project.sync.util.TestUtils.getMockedClock;
import static com.commercetools.project.sync.util.TestUtils.mockLastSyncCustomObject;
import static com.commercetools.project.sync.util.TestUtils.readObjectFromResource;
import static com.commercetools.project.sync.util.TestUtils.readStringFromFile;
import static com.commercetools.project.sync.util.TestUtils.stubClientsCustomObjectService;
import static com.commercetools.project.sync.util.TestUtils.withTestClient;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ByProjectKeyCustomObjectsPost;
import com.commercetools.api.client.ByProjectKeyProductsPost;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.defaultconfig.ApiRootBuilder;
import com.commercetools.api.models.ResourcePagedQueryResponse;
import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.api.models.custom_object.CustomObjectDraft;
import com.commercetools.api.models.graph_ql.GraphQLRequest;
import com.commercetools.api.models.graph_ql.GraphQLRequestBuilder;
import com.commercetools.api.models.product.Product;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductMixin;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductProjectionPagedQueryResponse;
import com.commercetools.api.models.product.ProductProjectionPagedQueryResponseBuilder;
import com.commercetools.api.models.product.ProductProjectionType;
import com.commercetools.project.sync.cartdiscount.CartDiscountSyncer;
import com.commercetools.project.sync.category.CategorySyncer;
import com.commercetools.project.sync.customer.CustomerSyncer;
import com.commercetools.project.sync.customobject.CustomObjectSyncer;
import com.commercetools.project.sync.exception.CliException;
import com.commercetools.project.sync.inventoryentry.InventoryEntrySyncer;
import com.commercetools.project.sync.product.ProductSyncer;
import com.commercetools.project.sync.producttype.ProductTypeSyncer;
import com.commercetools.project.sync.shoppinglist.ShoppingListSyncer;
import com.commercetools.project.sync.state.StateSyncer;
import com.commercetools.project.sync.taxcategory.TaxCategorySyncer;
import com.commercetools.project.sync.type.TypeSyncer;
import com.commercetools.sync.commons.exceptions.ReferenceTransformException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vrap.rmf.base.client.ApiHttpMethod;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.BadGatewayException;
import io.vrap.rmf.base.client.utils.CompletableFutureUtils;
import io.vrap.rmf.base.client.utils.json.JsonUtils;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

// This will suppress MoreThanOneLogger warnings in this class
@SuppressWarnings("PMD.MoreThanOneLogger")
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

  private ProjectApiRoot sourceClient;
  private ProjectApiRoot targetClient;

  @BeforeEach
  void setupTest() {
    final ProjectApiRoot clientWithEmptyResourceResults =
        mockClientResourceRequests("testProjectKey");
    sourceClient = spy(clientWithEmptyResourceResults);
    targetClient = mock(ProjectApiRoot.class);
    when(targetClient.getProjectKey()).thenReturn("testTargetProjectKey");
  }

  @AfterEach
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

  private ProjectApiRoot mockClientResourceRequests(final String projectKey) {
    return withTestClient(
        projectKey,
        (uri, method, encodedRequetsBody) -> {
          final String responseString = "{\"results\":[]}";
          return CompletableFuture.completedFuture(
              new ApiHttpResponse<>(200, null, responseString.getBytes(StandardCharsets.UTF_8)));
        });
  }

  @Test
  void sync_WithNullOptionValue_ShouldCompleteExceptionallyWithIllegalArgumentException() {
    assertThat(
            SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock())
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
            SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock())
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
            SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock())
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
    final ZonedDateTime currentCtpTimestamp = ZonedDateTime.now();
    stubClientsCustomObjectService(targetClient, currentCtpTimestamp);

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    // test
    syncerFactory.sync(new String[] {"products"}, "myRunnerName", false, false, null);

    // assertions
    // verify product-projections are queried once
    verify(sourceClient, times(1)).productProjections();
    //    assertThat(verifyProductProjectionsGetCounter.get()).isEqualTo(1);
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(targetClient, "ProductSync", "myRunnerName");
    verifyLastSyncCustomObjectQuery(
        targetClient, "productSync", "myRunnerName", "testProjectKey", 1);
    // verify two custom object upserts : 1. current ctp timestamp and 2. last sync timestamp
    // creation)
    verify(targetClient.customObjects(), times(2)).post(any(CustomObjectDraft.class));
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
    final ZonedDateTime currentCtpTimestamp = ZonedDateTime.now();
    stubClientsCustomObjectService(targetClient, currentCtpTimestamp);

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    // test
    syncerFactory.sync(new String[] {"products"}, "myRunnerName", true, false, null);

    // assertions
    verify(sourceClient, times(1)).productProjections();
    //    assertThat(verifyProductProjectionsGetCounter.get()).isEqualTo(1);
    verifyTimestampGeneratorCustomObjectUpsertIsNotCalled(
        targetClient, "ProductSync", "myRunnerName");
    verifyLastSyncCustomObjectQuery(targetClient, "productSync", "myRunnerName", "foo", 0);
    verify(targetClient.customObjects(), times(0)).post(any(CustomObjectDraft.class));
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
    final ProductProjection product5 =
        ProductMixin.toProjection(
            readObjectFromResource("product-key-5.json", Product.class),
            ProductProjectionType.STAGED);
    final ProductProjection product6 =
        ProductMixin.toProjection(
            readObjectFromResource("product-key-6.json", Product.class),
            ProductProjectionType.STAGED);
    final ProductProjectionPagedQueryResponse twoProductResult =
        ProductProjectionPagedQueryResponseBuilder.of()
            .results(product5, product6)
            .limit(10L)
            .count(2L)
            .offset(0L)
            .total(2L)
            .build();

    final AtomicInteger verifyProductProjectionsGetCounter = new AtomicInteger(0);
    final BadGatewayException badGatewayException = createBadGatewayException();
    final ProjectApiRoot sourceClient =
        withTestClient(
            "testProjectKey",
            (uri, method, encodedRequetsBody) -> {
              if (uri.contains("graphql") && ApiHttpMethod.POST.equals(method)) {
                return CompletableFutureUtils.failed(badGatewayException);
              }
              if (uri.contains("product-projections") && ApiHttpMethod.GET.equals(method)) {
                if (verifyProductProjectionsGetCounter.get() == 0) {
                  verifyProductProjectionsGetCounter.incrementAndGet();
                  final String jsonString =
                      createJsonStringFromPagedQueryResponse(twoProductResult);
                  return CompletableFuture.completedFuture(
                      new ApiHttpResponse<>(
                          200, null, jsonString.getBytes(StandardCharsets.UTF_8)));
                }
              }
              return null;
            });

    stubClientsCustomObjectService(targetClient, ZonedDateTime.now());
    final ByProjectKeyProductsPost byProjectKeyProductsPost = mock(ByProjectKeyProductsPost.class);
    when(byProjectKeyProductsPost.execute())
        .thenReturn(CompletableFutureUtils.failed(badGatewayException));
    when(targetClient.products()).thenReturn(mock());
    when(targetClient.products().post(any(ProductDraft.class)))
        .thenReturn(byProjectKeyProductsPost);

    final ProjectApiRoot sourceSpy = spy(sourceClient);
    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceSpy, () -> targetClient, getMockedClock());

    // test
    syncerFactory.sync(new String[] {"products"}, "myRunnerName", true, false, null);

    // assertions
    verify(sourceSpy, times(1)).productProjections();
    verify(sourceSpy, times(3)).graphql();
    verifyInteractionsWithClientAfterSync(sourceSpy, 1);

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

  // TODO: Enable test when issue with java-sync (NPE) is solved.
  // See https://github.com/commercetools/commercetools-sync-java/issues/1101
  @Disabled
  @Test
  void
      sync_AsProductsFullSyncWithExceptionDuringAttributeReferenceReplacement_ShouldContinueWithPages() {
    // preparation
    final AtomicInteger verifyProductProjectionsGetCounter = new AtomicInteger(0);
    final AtomicInteger sourceGraphQLPostCounter = new AtomicInteger(0);
    final ProductProjection product1 =
        ProductMixin.toProjection(
            readObjectFromResource("product-key-7.json", Product.class),
            ProductProjectionType.STAGED);
    final ProductProjection product2 =
        ProductMixin.toProjection(
            readObjectFromResource("product-key-8.json", Product.class),
            ProductProjectionType.STAGED);
    final ProductProjection product3 =
        ProductMixin.toProjection(
            readObjectFromResource("product-key-9.json", Product.class),
            ProductProjectionType.STAGED);

    final List<ProductProjection> fullPageOfProducts =
        IntStream.range(0, 500).mapToObj(o -> product1).collect(Collectors.toList());

    final Long pageSize = Long.valueOf(fullPageOfProducts.size());

    final ProductProjectionPagedQueryResponse fullPageResponse =
        ProductProjectionPagedQueryResponseBuilder.of()
            .results(fullPageOfProducts)
            .limit(10L)
            .count(pageSize)
            .offset(0L)
            .total(pageSize)
            .build();
    final ProductProjectionPagedQueryResponse twoProductResult =
        ProductProjectionPagedQueryResponseBuilder.of()
            .results(product3, product2)
            .limit(10L)
            .count(2L)
            .offset(0L)
            .total(2L)
            .build();

    final ProjectApiRoot srcClient =
        withTestClient(
            "testProjectKey",
            (uri, method, encodedRequestBody) -> {
              final Charset charsetUTF8 = Charset.forName(StandardCharsets.UTF_8.name());
              if (uri.contains("graphql") && ApiHttpMethod.POST.equals(method)) {
                final ObjectMapper objectMapper = JsonUtils.getConfiguredObjectMapper();
                GraphQLRequest graphQLRequest;
                try {
                  graphQLRequest = objectMapper.readValue(encodedRequestBody, GraphQLRequest.class);
                } catch (JsonProcessingException e) {
                  graphQLRequest = GraphQLRequestBuilder.of().build();
                }
                final String graphQLRequestQuery = graphQLRequest.getQuery();
                final String bodyData = "{\"data\": %s}";
                String result = String.format(bodyData, "{}");
                if (graphQLRequestQuery != null) {
                  int graphQlPostCounter = sourceGraphQLPostCounter.getAndIncrement();
                  if (graphQlPostCounter < 3) {
                    return CompletableFutureUtils.failed(createBadGatewayException());
                  }
                  if (graphQLRequestQuery.contains("products")) {
                    final String jsonAsString =
                        "{\"results\":[{\"id\":\"53c4a8b4-865f-4b95-b6f2-3e1e70e3d0c1\",\"key\":\"productKey3\"}]}";
                    result = String.format(bodyData, jsonAsString);
                    return CompletableFuture.completedFuture(
                        new ApiHttpResponse<>(200, null, result.getBytes(charsetUTF8)));
                  } else if (graphQLRequestQuery.contains("productTypes")) {
                    final String jsonAsString =
                        "{\"results\":[{\"id\":\"53c4a8b4-865f-4b95-b6f2-3e1e70e3d0c2\",\"key\":\"prodType1\"}]}";
                    result = String.format(bodyData, jsonAsString);
                    return CompletableFuture.completedFuture(
                        new ApiHttpResponse<>(200, null, result.getBytes(charsetUTF8)));
                  } else if (graphQLRequestQuery.contains("categories")) {
                    final String jsonAsString =
                        "{\"results\":[{\"id\":\"53c4a8b4-865f-4b95-b6f2-3e1e70e3d0c3\",\"key\":\"cat1\"}]}";
                    result = String.format(bodyData, jsonAsString);
                    return CompletableFuture.completedFuture(
                        new ApiHttpResponse<>(200, null, result.getBytes(charsetUTF8)));
                  } else {
                    return CompletableFuture.completedFuture(
                        new ApiHttpResponse<>(200, null, result.getBytes(charsetUTF8)));
                  }
                }
              }
              if (uri.contains("product-projections") && ApiHttpMethod.GET.equals(method)) {
                int sourceGetCounter = verifyProductProjectionsGetCounter.getAndIncrement();
                if (sourceGetCounter == 0) {
                  final String fullPageResponseAsString =
                      createJsonStringFromPagedQueryResponse(fullPageResponse);
                  return CompletableFuture.completedFuture(
                      new ApiHttpResponse<>(
                          200, null, fullPageResponseAsString.getBytes(charsetUTF8)));
                } else if (sourceGetCounter == 1) {
                  final String twoResultsResponseAsString =
                      createJsonStringFromPagedQueryResponse(twoProductResult);
                  return CompletableFuture.completedFuture(
                      new ApiHttpResponse<>(
                          200, null, twoResultsResponseAsString.getBytes(charsetUTF8)));

                } else {
                  final String emptyResultsAsString = "{\"results\":[]}";
                  return CompletableFuture.completedFuture(
                      new ApiHttpResponse<>(200, null, emptyResultsAsString.getBytes(charsetUTF8)));
                }
              }
              return null;
            });

    final ProjectApiRoot trgClient =
        ApiRootBuilder.of(
                request -> {
                  final String uri = request.getUri() != null ? request.getUri().toString() : "";
                  final ApiHttpMethod method = request.getMethod();
                  final Charset charsetUTF8 = Charset.forName(StandardCharsets.UTF_8.name());
                  if (uri.contains("graphql") && ApiHttpMethod.POST.equals(method)) {
                    final String encodedRequestBody =
                        new String(request.getBody(), StandardCharsets.UTF_8);
                    ObjectMapper objectMapper = JsonUtils.getConfiguredObjectMapper();
                    GraphQLRequest graphQLRequest;
                    try {
                      graphQLRequest =
                          objectMapper.readValue(encodedRequestBody, GraphQLRequest.class);
                    } catch (JsonProcessingException e) {
                      graphQLRequest = GraphQLRequestBuilder.of().build();
                    }
                    final String graphQLRequestQuery = graphQLRequest.getQuery();
                    final String bodyData = "{\"data\": %s}";
                    String result;
                    if (graphQLRequestQuery != null) {
                      if (graphQLRequestQuery.contains("products")) {
                        final String jsonAsString =
                            "{\"results\":[{\"id\":\"53c4a8b4-865f-4b95-b6f2-3e1e70e3d0c1\",\"key\":\"productKey3\"}]}";
                        result = String.format(bodyData, jsonAsString);
                        return CompletableFuture.completedFuture(
                            new ApiHttpResponse<>(200, null, result.getBytes(charsetUTF8)));
                      } else {
                        result = String.format(bodyData, "{\"results\": []}");
                        return CompletableFuture.completedFuture(
                            new ApiHttpResponse<>(200, null, result.getBytes(charsetUTF8)));
                      }
                    }
                  }
                  if (uri.contains("products") && ApiHttpMethod.POST.equals(method)) {
                    final String productsResultAsString = readStringFromFile("product-key-8.json");
                    return CompletableFuture.completedFuture(
                        new ApiHttpResponse<>(
                            200, null, productsResultAsString.getBytes(charsetUTF8)));

                  } else {
                    final String emptyResultsAsString = "{\"results\":[]}";
                    return CompletableFuture.completedFuture(
                        new ApiHttpResponse<>(
                            200, null, emptyResultsAsString.getBytes(charsetUTF8)));
                  }
                })
            .withApiBaseUrl("testBaseUrl")
            .build("testProjectKey2");

    final ProjectApiRoot sourceClientSpy = spy(srcClient);
    final ProjectApiRoot targetClientSpy = spy(trgClient);

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClientSpy, () -> targetClientSpy, getMockedClock());

    // test
    syncerFactory.sync(new String[] {"products"}, "myRunnerName", true, false, null);

    // assertions
    assertThat(verifyProductProjectionsGetCounter.get()).isEqualTo(2);
    verify(sourceClientSpy, times(9)).graphql();
    verifyInteractionsWithClientAfterSync(sourceClientSpy, 1);

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
                  .isInstanceOf(BadGatewayException.class);
            });
  }

  private static void verifyTimestampGeneratorCustomObjectUpsertIsCalled(
      @Nonnull final ProjectApiRoot client,
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
      @Nonnull final ProjectApiRoot client,
      @Nonnull final String syncMethodName,
      @Nonnull final String syncRunnerName) {
    final CustomObjectDraft customObjectDraft =
        findTimestampGeneratorCustomObjectUpsert(client, syncMethodName, syncRunnerName);
    assertThat(customObjectDraft).isNull();
  }

  private static CustomObjectDraft findTimestampGeneratorCustomObjectUpsert(
      @Nonnull ProjectApiRoot client,
      @Nonnull String syncMethodName,
      @Nonnull String syncRunnerName) {
    final ArgumentCaptor<CustomObjectDraft> customObjectDraftArgumentCaptor =
        ArgumentCaptor.forClass(CustomObjectDraft.class);

    verify(client.customObjects(), atLeast(0)).post(customObjectDraftArgumentCaptor.capture());
    final List<CustomObjectDraft> allValues = customObjectDraftArgumentCaptor.getAllValues();
    final CustomObjectDraft customObjectDraft =
        allValues.stream()
            .filter(
                draft ->
                    draft
                            .getContainer()
                            .equals(
                                format(
                                    "%s.%s.%s.%s",
                                    getApplicationName(),
                                    syncRunnerName,
                                    syncMethodName,
                                    TIMESTAMP_GENERATOR_KEY))
                        && draft.getKey().equals(TIMESTAMP_GENERATOR_KEY))
            .findAny()
            .orElse(null);
    return customObjectDraft;
  }

  private static void verifyLastSyncCustomObjectQuery(
      @Nonnull final ProjectApiRoot client,
      @Nonnull final String syncModuleName,
      @Nonnull final String syncRunnerName,
      @Nonnull final String sourceProjectKey,
      final int expectedInvocations) {

    final String container =
        format("commercetools-project-sync.%s.%s", syncRunnerName, syncModuleName);

    if (expectedInvocations > 0) {
      verify(client.customObjects(), times(expectedInvocations))
          .withContainerAndKey(container, sourceProjectKey);
    } else {
      verifyNoInteractions(
          client.customObjects().withContainerAndKey(anyString(), anyString()).get());
    }
  }

  private static <T> String createJsonStringFromPagedQueryResponse(
      final ResourcePagedQueryResponse<T> pagedQueryResponse) {
    try {
      return JsonUtils.toJsonString(pagedQueryResponse);
    } catch (JsonProcessingException e) {
      return "{\"results\": []}";
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void sync_AsCategoriesDeltaSync_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    final ZonedDateTime currentCtpTimestamp = ZonedDateTime.now();
    stubClientsCustomObjectService(targetClient, currentCtpTimestamp);

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());
    // test
    syncerFactory.sync(new String[] {"categories"}, null, false, false, null);

    // assertions
    verify(sourceClient, times(1)).categories();
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(
        targetClient, "CategorySync", DEFAULT_RUNNER_NAME);
    verifyLastSyncCustomObjectQuery(
        targetClient, "categorySync", DEFAULT_RUNNER_NAME, "testProjectKey", 1);
    // verify two custom object upserts : 1. current ctp timestamp and 2. last sync timestamp
    verify(targetClient.customObjects(), times(2)).post(any(CustomObjectDraft.class));
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
    final ZonedDateTime currentCtpTimestamp = ZonedDateTime.now();
    stubClientsCustomObjectService(targetClient, currentCtpTimestamp);

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    // test
    syncerFactory.sync(new String[] {"productTypes"}, "", false, false, null);

    // assertions
    verify(sourceClient, times(1)).productTypes();
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(
        targetClient, "ProductTypeSync", DEFAULT_RUNNER_NAME);
    verifyLastSyncCustomObjectQuery(
        targetClient, "productTypeSync", DEFAULT_RUNNER_NAME, "testProjectKey", 1);
    // verify two custom object upserts : 1. current ctp timestamp and 2. last sync timestamp
    // creation)
    verify(targetClient.customObjects(), times(2)).post(any(CustomObjectDraft.class));
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
    final ZonedDateTime currentCtpTimestamp = ZonedDateTime.now();
    stubClientsCustomObjectService(targetClient, ZonedDateTime.now());

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    // test
    syncerFactory.sync(new String[] {"types"}, "foo", false, false, null);

    // assertions
    verify(sourceClient, times(1)).types();
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(targetClient, "TypeSync", "foo");
    verifyLastSyncCustomObjectQuery(targetClient, "typeSync", "foo", "testProjectKey", 1);
    // verify two custom object upserts : 1. current ctp timestamp and 2. last sync timestamp
    // creation)
    verify(targetClient.customObjects(), times(2)).post(any(CustomObjectDraft.class));
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
    stubClientsCustomObjectService(targetClient, ZonedDateTime.now());

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    // test
    syncerFactory.sync(new String[] {"inventoryEntries"}, null, false, false, null);

    // assertions
    verify(sourceClient, times(1)).inventory();
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(
        targetClient, "InventorySync", DEFAULT_RUNNER_NAME);
    verifyLastSyncCustomObjectQuery(
        targetClient, "inventorySync", DEFAULT_RUNNER_NAME, "testProjectKey", 1);
    // verify two custom object upserts : 1. current ctp timestamp and 2. last sync timestamp
    // creation)
    verify(targetClient.customObjects(), times(2)).post(any(CustomObjectDraft.class));
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
    final ProjectApiRoot mockSource =
        withTestClient(
            "testProjectKey",
            (uri, method, encodedRequestBody) -> {
              if (uri.contains("inventory") && ApiHttpMethod.GET.equals(method)) {
                return CompletableFutureUtils.exceptionallyCompletedFuture(
                    createBadGatewayException());
              }
              return null;
            });
    sourceClient = spy(mockSource);

    stubClientsCustomObjectService(targetClient, ZonedDateTime.now());

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    // test
    final CompletionStage<Void> result =
        syncerFactory.sync(new String[] {"inventoryEntries"}, null, false, false, null);

    // assertions
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(
        targetClient, "InventorySync", DEFAULT_RUNNER_NAME);
    verify(sourceClient, times(1)).inventory();
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
    final ByProjectKeyCustomObjectsPost customObjectsPost =
        mock(ByProjectKeyCustomObjectsPost.class);
    when(customObjectsPost.execute())
        .thenReturn(
            CompletableFutureUtils.exceptionallyCompletedFuture(createBadGatewayException()));
    when(targetClient.customObjects()).thenReturn(mock());
    when(targetClient.customObjects().post(any(CustomObjectDraft.class)))
        .thenReturn(customObjectsPost);

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    // test
    final CompletionStage<Void> result =
        syncerFactory.sync(new String[] {"inventoryEntries"}, "", false, false, null);

    // assertions
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(
        targetClient, "InventorySync", DEFAULT_RUNNER_NAME);
    verify(sourceClient, times(0)).inventory();
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
    final ByProjectKeyCustomObjectsPost customObjectsPost =
        mock(ByProjectKeyCustomObjectsPost.class);
    final CustomObject lastSyncCustomObjectCustomObject =
        mockLastSyncCustomObject(ZonedDateTime.now());
    final ApiHttpResponse apiHttpResponse = mock(ApiHttpResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(lastSyncCustomObjectCustomObject);
    when(customObjectsPost.execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(targetClient.customObjects()).thenReturn(mock());
    when(targetClient.customObjects().withContainerAndKey(anyString(), anyString()))
        .thenReturn(mock());
    when(targetClient.customObjects().withContainerAndKey(anyString(), anyString()).get())
        .thenReturn(mock());
    when(targetClient.customObjects().withContainerAndKey(anyString(), anyString()).get().execute())
        .thenReturn(
            CompletableFutureUtils.exceptionallyCompletedFuture(createBadGatewayException()));
    when(targetClient.customObjects().post(any(CustomObjectDraft.class)))
        .thenReturn(customObjectsPost);

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    // test
    final CompletionStage<Void> result =
        syncerFactory.sync(new String[] {"inventoryEntries"}, "bar", false, false, null);

    // assertions
    verifyTimestampGeneratorCustomObjectUpsertIsCalled(targetClient, "InventorySync", "bar");
    verifyLastSyncCustomObjectQuery(targetClient, "inventorySync", "bar", "testProjectKey", 1);
    verify(sourceClient, times(0)).inventory();
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
    stubClientsCustomObjectService(targetClient, ZonedDateTime.now());
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
    verify(targetClient.customObjects(), times(22)).post(any(CustomObjectDraft.class));
    verifyLastSyncCustomObjectQuery(
        targetClient, "inventorySync", DEFAULT_RUNNER_NAME, "testProjectKey", 1);
    verifyLastSyncCustomObjectQuery(
        targetClient, "productTypeSync", DEFAULT_RUNNER_NAME, "testProjectKey", 1);
    verifyLastSyncCustomObjectQuery(
        targetClient, "productSync", DEFAULT_RUNNER_NAME, "testProjectKey", 1);
    verifyLastSyncCustomObjectQuery(
        targetClient, "categorySync", DEFAULT_RUNNER_NAME, "testProjectKey", 1);
    verifyLastSyncCustomObjectQuery(
        targetClient, "typeSync", DEFAULT_RUNNER_NAME, "testProjectKey", 1);
    verifyLastSyncCustomObjectQuery(
        targetClient, "cartDiscountSync", DEFAULT_RUNNER_NAME, "testProjectKey", 1);
    verifyLastSyncCustomObjectQuery(
        targetClient, "stateSync", DEFAULT_RUNNER_NAME, "testProjectKey", 1);
    verifyLastSyncCustomObjectQuery(
        targetClient, "taxCategorySync", DEFAULT_RUNNER_NAME, "testProjectKey", 1);
    verifyLastSyncCustomObjectQuery(
        targetClient, "customObjectSync", DEFAULT_RUNNER_NAME, "testProjectKey", 1);
    verifyLastSyncCustomObjectQuery(
        targetClient, "customerSync", DEFAULT_RUNNER_NAME, "testProjectKey", 1);
    verifyLastSyncCustomObjectQuery(
        targetClient, "shoppingListSync", DEFAULT_RUNNER_NAME, "testProjectKey", 1);
    verify(sourceClient, times(1)).productTypes();
    verify(sourceClient, times(1)).types();
    verify(sourceClient, times(1)).categories();
    verify(sourceClient, times(1)).productProjections();
    verify(sourceClient, times(1)).inventory();
    verify(sourceClient, times(1)).cartDiscounts();
    verify(sourceClient, times(1)).states();
    verify(sourceClient, times(1)).taxCategories();
    verify(sourceClient, times(1)).customObjects();
    verify(sourceClient, times(1)).customers();
    verify(sourceClient, times(1)).shoppingLists();
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
    stubClientsCustomObjectService(targetClient, ZonedDateTime.now());
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
    verify(targetClient.customObjects(), times(8)).post(any(CustomObjectDraft.class));
    verifyLastSyncCustomObjectQuery(
        targetClient, "productTypeSync", DEFAULT_RUNNER_NAME, "testProjectKey", 1);
    verifyLastSyncCustomObjectQuery(
        targetClient, "productSync", DEFAULT_RUNNER_NAME, "testProjectKey", 1);
    verifyLastSyncCustomObjectQuery(
        targetClient, "customerSync", DEFAULT_RUNNER_NAME, "testProjectKey", 1);
    verifyLastSyncCustomObjectQuery(
        targetClient, "shoppingListSync", DEFAULT_RUNNER_NAME, "testProjectKey", 1);

    final InOrder inOrder = Mockito.inOrder(sourceClient);

    // According to sync algorithm, ProductType and Customer will run sync in parallel, Product and
    // ShoppingList sequentially.
    // Example: Given: ['productTypes', 'customers', 'products', 'shoppingLists']
    // From the given arguments, algorithm will group the resources as below,
    // [productTypes, customers] [products] [shoppingLists]
    inOrder.verify(sourceClient, times(1)).productTypes();
    verify(sourceClient, times(1)).customers();
    inOrder.verify(sourceClient, times(1)).productProjections();
    inOrder.verify(sourceClient, times(1)).shoppingLists();
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
    stubClientsCustomObjectService(targetClient, ZonedDateTime.now());
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
    verify(targetClient.customObjects(), times(6)).post(any(CustomObjectDraft.class));
    verifyLastSyncCustomObjectQuery(
        targetClient, "stateSync", DEFAULT_RUNNER_NAME, "testProjectKey", 1);
    verifyLastSyncCustomObjectQuery(
        targetClient, "inventorySync", DEFAULT_RUNNER_NAME, "testProjectKey", 1);
    verifyLastSyncCustomObjectQuery(
        targetClient, "customObjectSync", DEFAULT_RUNNER_NAME, "testProjectKey", 1);
    verify(sourceClient, times(1)).states();
    verify(sourceClient, times(1)).inventory();
    verify(sourceClient, times(1)).customObjects();

    verifyInteractionsWithClientAfterSync(sourceClient, 3);

    assertStateSyncerLoggingEvents(stateSyncerTestLogger, 0);
    assertInventoryEntrySyncerLoggingEvents(inventoryEntrySyncerTestLogger, 0);
    assertCustomObjectSyncerLoggingEvents(customObjectSyncerTestLogger, 0);
  }

  @Test
  @SuppressWarnings("unchecked")
  void syncTypesAndCategories_AsDelta_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    stubClientsCustomObjectService(targetClient, ZonedDateTime.now());

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
    verify(targetClient.customObjects(), times(4)).post(any(CustomObjectDraft.class));
    verifyLastSyncCustomObjectQuery(
        targetClient, "typeSync", DEFAULT_RUNNER_NAME, "testProjectKey", 1);
    verifyLastSyncCustomObjectQuery(
        targetClient, "categorySync", DEFAULT_RUNNER_NAME, "testProjectKey", 1);

    final InOrder inOrder = Mockito.inOrder(sourceClient);

    inOrder.verify(sourceClient, times(1)).types();
    inOrder.verify(sourceClient, times(1)).categories();
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
    stubClientsCustomObjectService(targetClient, ZonedDateTime.now());

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
    verify(targetClient.customObjects(), times(4)).post(any(CustomObjectDraft.class));
    verifyLastSyncCustomObjectQuery(
        targetClient, "productSync", DEFAULT_RUNNER_NAME, "testProjectKey", 1);
    verifyLastSyncCustomObjectQuery(
        targetClient, "shoppingListSync", DEFAULT_RUNNER_NAME, "testProjectKey", 1);
    verify(sourceClient, times(1)).productProjections();
    verify(sourceClient, times(1)).shoppingLists();
    verifyInteractionsWithClientAfterSync(sourceClient, 2);

    assertProductSyncerLoggingEvents(productSyncerTestLogger, 0);
    assertShoppingListSyncerLoggingEvents(shoppingListSyncerTestLogger, 0);
    assertThat(productSyncerTestLogger.getAllLoggingEvents()).hasSize(2);
    assertThat(shoppingListSyncerTestLogger.getAllLoggingEvents()).hasSize(2);
  }

  @Test
  void sync_AsDelta_WithOneUnmatchedSyncOptionValue_ShouldResultIllegalArgumentException() {
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

  private void verifyInteractionsWithClientAfterSync(
      @Nonnull final ProjectApiRoot client, final int numberOfGetConfigInvocations) {

    verify(client, times(1)).close();
    // Verify config is accessed for the success message after sync:
    // " example: Syncing products from CTP project with key 'x' to project with key 'y' is done","
    verify(client, times(numberOfGetConfigInvocations)).getProjectKey();
  }
}
