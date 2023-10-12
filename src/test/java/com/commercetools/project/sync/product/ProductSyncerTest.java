package com.commercetools.project.sync.product;

import org.junit.jupiter.api.BeforeEach;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

// These tests aren't migrated
// TODO: Migrate tests
class ProductSyncerTest {

  private static final TestLogger testLogger = TestLoggerFactory.getTestLogger(ProductSyncer.class);

  @BeforeEach
  void tearDownTest() {
    testLogger.clearAll();
  }

  //  @Test
  //  void of_ShouldCreateProductSyncerInstance() {
  //    // test
  //    final ProductSyncer productSyncer =
  //        ProductSyncer.of(
  //            mock(SphereClient.class), mock(SphereClient.class), getMockedClock(), null);
  //
  //    // assertions
  //    assertThat(productSyncer).isNotNull();
  //    assertThat(productSyncer.getQuery()).isInstanceOf(ProductProjectionQuery.class);
  //    assertThat(productSyncer.getSync()).isExactlyInstanceOf(ProductSync.class);
  //  }
  //
  //  @Test
  //  void transform_WithAttributeReferences_ShouldReplaceProductReferenceIdsWithKeys() {
  //    // preparation
  //    final SphereClient sourceClient = mock(SphereClient.class);
  //    final ProductSyncer productSyncer =
  //        ProductSyncer.of(sourceClient, mock(SphereClient.class), getMockedClock(), null);
  //    final List<ProductProjection> productPage =
  //        Collections.singletonList(
  //            readObjectFromResource("product-key-4.json", Product.class)
  //                .toProjection(ProductProjectionType.STAGED));
  //
  //    String jsonStringProducts =
  //        "{\"results\":[{\"id\":\"53c4a8b4-754f-4b95-b6f2-3e1e70e3d0d2\",\"key\":\"prod1\"},"
  //            + "{\"id\":\"53c4a8b4-754f-4b95-b6f2-3e1e70e3d0d6\",\"key\":\"prod2\"}]}";
  //    final ResourceKeyIdGraphQlResult productsResult =
  //        SphereJsonUtils.readObject(jsonStringProducts, ResourceKeyIdGraphQlResult.class);
  //
  //    String jsonStringProductTypes =
  //        "{\"results\":[{\"id\":\"53c4a8b4-754f-4b95-b6f2-3e1e70e3d0d3\","
  //            + "\"key\":\"prodType1\"}]}";
  //    final ResourceKeyIdGraphQlResult productTypesResult =
  //        SphereJsonUtils.readObject(jsonStringProductTypes, ResourceKeyIdGraphQlResult.class);
  //
  //    String jsonStringCategories =
  //        "{\"results\":[{\"id\":\"53c4a8b4-754f-4b95-b6f2-3e1e70e3d0d4\",\"key\":\"cat1\"},"
  //            + "{\"id\":\"53c4a8b4-754f-4b95-b6f2-3e1e70e3d0d5\",\"key\":\"cat2\"}]}";
  //    final ResourceKeyIdGraphQlResult categoriesResult =
  //        SphereJsonUtils.readObject(jsonStringCategories, ResourceKeyIdGraphQlResult.class);
  //
  //    when(sourceClient.execute(any(ResourceIdsGraphQlRequest.class)))
  //        .thenReturn(CompletableFuture.completedFuture(productsResult))
  //        .thenReturn(CompletableFuture.completedFuture(productTypesResult))
  //        .thenReturn(CompletableFuture.completedFuture(categoriesResult));
  //
  //    // test
  //    final List<ProductDraft> draftsFromPageStage =
  //        productSyncer.transform(productPage).toCompletableFuture().join();
  //
  //    // assertions
  //
  //    final Optional<ProductDraft> productDraftKey1 =
  //        draftsFromPageStage.stream()
  //            .filter(productDraft -> "productKey4".equals(productDraft.getKey()))
  //            .findFirst();
  //
  //    assertThat(productDraftKey1)
  //        .hasValueSatisfying(
  //            productDraft ->
  //                assertThat(productDraft.getMasterVariant().getAttributes())
  //                    .anySatisfy(
  //                        attributeDraft -> {
  //                          assertThat(attributeDraft.getName()).isEqualTo("productReference");
  //                          final JsonNode referenceSet = attributeDraft.getValue();
  //                          assertThat(referenceSet)
  //                              .anySatisfy(
  //                                  reference ->
  //
  // assertThat(reference.get("id").asText()).isEqualTo("prod1"));
  //                          assertThat(referenceSet)
  //                              .anySatisfy(
  //                                  reference ->
  //
  // assertThat(reference.get("id").asText()).isEqualTo("prod2"));
  //                        }));
  //
  //    assertThat(productDraftKey1)
  //        .hasValueSatisfying(
  //            productDraft ->
  //                assertThat(productDraft.getMasterVariant().getAttributes())
  //                    .anySatisfy(
  //                        attributeDraft -> {
  //                          assertThat(attributeDraft.getName()).isEqualTo("categoryReference");
  //                          final JsonNode referenceSet = attributeDraft.getValue();
  //                          assertThat(referenceSet)
  //                              .anySatisfy(
  //                                  reference ->
  //
  // assertThat(reference.get("id").asText()).isEqualTo("cat1"));
  //                          assertThat(referenceSet)
  //                              .anySatisfy(
  //                                  reference ->
  //
  // assertThat(reference.get("id").asText()).isEqualTo("cat2"));
  //                        }));
  //
  //    assertThat(productDraftKey1)
  //        .hasValueSatisfying(
  //            productDraft ->
  //                assertThat(productDraft.getMasterVariant().getAttributes())
  //                    .anySatisfy(
  //                        attributeDraft -> {
  //
  // assertThat(attributeDraft.getName()).isEqualTo("productTypeReference");
  //                          assertThat(attributeDraft.getValue().get("id").asText())
  //                              .isEqualTo("prodType1");
  //                        }));
  //
  //    assertThat(testLogger.getAllLoggingEvents()).isEmpty();
  //  }
  //
  //  @Test
  //  void transform_WithDiscountedPrices_ShouldRemoveDiscountedPrices() {
  //    // preparation
  //    final SphereClient sourceClient = mock(SphereClient.class);
  //    final ProductSyncer productSyncer =
  //        ProductSyncer.of(sourceClient, mock(SphereClient.class), getMockedClock(), null);
  //    final List<ProductProjection> productPage =
  //        Collections.singletonList(
  //            readObjectFromResource("product-key-10.json", Product.class)
  //                .toProjection(ProductProjectionType.STAGED));
  //
  //    String jsonStringProductTypes =
  //        "{\"results\":[{\"id\":\"53c4a8b4-754f-4b95-b6f2-3e1e70e3d0d3\","
  //            + "\"key\":\"prodType1\"}]}";
  //    final ResourceKeyIdGraphQlResult productTypesResult =
  //        SphereJsonUtils.readObject(jsonStringProductTypes, ResourceKeyIdGraphQlResult.class);
  //
  //    when(sourceClient.execute(any(ResourceIdsGraphQlRequest.class)))
  //        .thenReturn(CompletableFuture.completedFuture(productTypesResult));
  //
  //    // test
  //    final List<ProductDraft> draftsFromPageStage =
  //        productSyncer.transform(productPage).toCompletableFuture().join();
  //
  //    final Optional<ProductDraft> productDraftKey1 =
  //        draftsFromPageStage.stream()
  //            .filter(productDraft -> "productKey10".equals(productDraft.getKey()))
  //            .findFirst();
  //
  //    assertThat(productDraftKey1)
  //        .hasValueSatisfying(
  //            productDraft ->
  //                assertThat(productDraft.getMasterVariant().getPrices())
  //                    .anySatisfy(priceDraft -> assertThat(priceDraft.getDiscounted()).isNull()));
  //    assertThat(testLogger.getAllLoggingEvents()).isEmpty();
  //  }
  //
  //  @Test
  //  void transform_WithErrorOnGraphQlRequest_ShouldContinueAndLogError() {
  //    // preparation
  //    final SphereClient sourceClient = mock(SphereClient.class);
  //    final ProductSyncer productSyncer =
  //        ProductSyncer.of(sourceClient, mock(SphereClient.class), getMockedClock(), null);
  //    final List<ProductProjection> productPage =
  //        asList(
  //            readObjectFromResource("product-key-1.json", Product.class)
  //                .toProjection(ProductProjectionType.STAGED),
  //            readObjectFromResource("product-key-2.json", Product.class)
  //                .toProjection(ProductProjectionType.STAGED));
  //
  //    final BadGatewayException badGatewayException =
  //        new BadGatewayException("Failed Graphql request");
  //    when(sourceClient.execute(any(ResourceIdsGraphQlRequest.class)))
  //        .thenReturn(CompletableFutureUtils.failed(badGatewayException));
  //
  //    // test
  //    final CompletionStage<List<ProductDraft>> draftsFromPageStage =
  //        productSyncer.transform(productPage);
  //
  //    // assertions
  //    assertThat(draftsFromPageStage).isCompletedWithValue(Collections.emptyList());
  //    assertThat(testLogger.getAllLoggingEvents())
  //        .anySatisfy(
  //            loggingEvent -> {
  //              assertThat(loggingEvent.getMessage())
  //                  .contains(
  //                      ReferenceTransformException.class.getCanonicalName()
  //                          + ": Failed to replace referenced resource ids with keys on the
  // attributes of the "
  //                          + "products in the current fetched page from the source project. "
  //                          + "This page will not be synced to the target project.");
  //              assertThat(loggingEvent.getThrowable().isPresent()).isTrue();
  //              assertThat(loggingEvent.getThrowable().get().getCause().getCause())
  //                  .isEqualTo(badGatewayException);
  //            });
  //  }
  //
  //  @Test
  //  void getQuery_ShouldBuildProductQueryWithoutAnyExpansionPaths() {
  //    // preparation
  //    final ProductSyncer productSyncer =
  //        ProductSyncer.of(
  //            mock(SphereClient.class), mock(SphereClient.class), getMockedClock(), null);
  //
  //    // test
  //    final ProductProjectionQuery query = productSyncer.getQuery();
  //
  //    // assertion
  //    assertThat(query.expansionPaths()).isEmpty();
  //  }
  //
  //  @Test
  //  void getQuery_ShouldBuildProductQueryWithCustomQueryAndLimitSize() {
  //    // preparation
  //    final Long limit = 100L;
  //    final String customQuery =
  //        "published=true AND masterData(masterVariant(attributes(name= \"abc\" AND value=123)))";
  //
  //    final ProductSyncCustomRequest productSyncCustomRequest = new ProductSyncCustomRequest();
  //    productSyncCustomRequest.setWhere(customQuery);
  //    productSyncCustomRequest.setLimit(limit);
  //
  //    final ProductSyncer productSyncer =
  //        ProductSyncer.of(
  //            mock(SphereClient.class),
  //            mock(SphereClient.class),
  //            getMockedClock(),
  //            productSyncCustomRequest);
  //
  //    // test
  //    final ProductProjectionQuery query = productSyncer.getQuery();
  //
  //    // assertion
  //    assertThat(query.limit()).isEqualTo(100);
  //    assertThat(query.predicates()).contains(QueryPredicate.of(customQuery));
  //  }
}
