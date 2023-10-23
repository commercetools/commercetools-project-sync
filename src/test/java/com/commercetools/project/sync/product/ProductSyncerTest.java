package com.commercetools.project.sync.product;

import static com.commercetools.project.sync.util.TestUtils.createBadGatewayException;
import static com.commercetools.project.sync.util.TestUtils.getMockedClock;
import static com.commercetools.project.sync.util.TestUtils.readObjectFromResource;
import static com.commercetools.project.sync.util.TestUtils.withTestClient;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ByProjectKeyGraphqlPost;
import com.commercetools.api.client.ByProjectKeyProductProjectionsGet;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.defaultconfig.ApiRootBuilder;
import com.commercetools.api.models.graph_ql.GraphQLRequest;
import com.commercetools.api.models.graph_ql.GraphQLResponse;
import com.commercetools.api.models.product.Product;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductMixin;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductProjectionType;
import com.commercetools.project.sync.model.ProductSyncCustomRequest;
import com.commercetools.sync.commons.exceptions.ReferenceTransformException;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.utils.AttributeUtils;
import com.fasterxml.jackson.databind.JsonNode;
import io.vrap.rmf.base.client.ApiHttpMethod;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.BadGatewayException;
import io.vrap.rmf.base.client.utils.CompletableFutureUtils;
import io.vrap.rmf.base.client.utils.json.JsonUtils;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

class ProductSyncerTest {

  private static final TestLogger testLogger = TestLoggerFactory.getTestLogger(ProductSyncer.class);

  @BeforeEach
  void tearDownTest() {
    testLogger.clearAll();
  }

  @Test
  void of_ShouldCreateProductSyncerInstance() {
    final ProjectApiRoot apiRoot = mock(ProjectApiRoot.class);
    when(apiRoot.productProjections()).thenReturn(mock());
    final ByProjectKeyProductProjectionsGet getMock = mock(ByProjectKeyProductProjectionsGet.class);
    when(getMock.addStaged(anyBoolean())).thenReturn(getMock);
    when(apiRoot.productProjections().get()).thenReturn(getMock);
    // test
    final ProductSyncer productSyncer = ProductSyncer.of(apiRoot, apiRoot, getMockedClock(), null);

    // assertions
    assertThat(productSyncer).isNotNull();
    assertThat(productSyncer.getQuery()).isEqualTo(getMock);
    assertThat(productSyncer.getSync()).isExactlyInstanceOf(ProductSync.class);
  }

  @Test
  void transform_WithAttributeReferences_ShouldReplaceProductReferenceIdsWithKeys() {
    // preparation
    final ProductProjection productProjection =
        ProductMixin.toProjection(
            readObjectFromResource("product-key-4.json", Product.class),
            ProductProjectionType.STAGED);

    final String jsonStringProducts =
        "{\"data\":{\"products\":{\"results\":[{\"id\":\"53c4a8b4-754f-4b95-b6f2-3e1e70e3d0d2\","
            + "\"key\":\"prod1\"},"
            + "{\"id\":\"53c4a8b4-754f-4b95-b6f2-3e1e70e3d0d6\",\"key\":\"prod2\"}]}}}";

    final String jsonStringProductTypes =
        "{\"data\":{\"productTypes\":{\"results\":[{\"id\":\"53c4a8b4-754f-4b95-b6f2-3e1e70e3d0d3\","
            + "\"key\":\"prodType1\"}]}}}";

    final String jsonStringCategories =
        "{\"data\":{\"categories\":{\"results\":[{\"id\":\"53c4a8b4-754f-4b95-b6f2-3e1e70e3d0d4\",\"key\":\"cat1\"},"
            + "{\"id\":\"53c4a8b4-754f-4b95-b6f2-3e1e70e3d0d5\",\"key\":\"cat2\"}]}}}";

    final ProjectApiRoot sourceClient =
        withTestClient(
            "testProjectKey",
            (uri, method, requestBody) -> {
              if (uri.contains("graphql") && ApiHttpMethod.POST.equals(method)) {
                final GraphQLRequest graphQLRequest =
                    JsonUtils.fromJsonString(requestBody, GraphQLRequest.class);
                final String requestQuery = graphQLRequest.getQuery();
                if (requestQuery.contains("products")) {
                  return CompletableFuture.completedFuture(
                      new ApiHttpResponse<>(
                          200, null, jsonStringProducts.getBytes(StandardCharsets.UTF_8)));
                }
                if (requestQuery.contains("productTypes")) {
                  return CompletableFuture.completedFuture(
                      new ApiHttpResponse<>(
                          200, null, jsonStringProductTypes.getBytes(StandardCharsets.UTF_8)));
                }
                if (requestQuery.contains("categories")) {
                  return CompletableFuture.completedFuture(
                      new ApiHttpResponse<>(
                          200, null, jsonStringCategories.getBytes(StandardCharsets.UTF_8)));
                }
                final String result = "{\"data\":{}}";
                return CompletableFuture.completedFuture(
                    new ApiHttpResponse<>(200, null, result.getBytes(StandardCharsets.UTF_8)));
              }
              return null;
            });

    final ProductSyncer productSyncer =
        ProductSyncer.of(sourceClient, mock(ProjectApiRoot.class), getMockedClock(), null);
    // test
    final List<ProductDraft> draftsFromPageStage =
        productSyncer.transform(singletonList(productProjection)).toCompletableFuture().join();

    // assertions

    final Optional<ProductDraft> productDraftKey1 =
        draftsFromPageStage.stream()
            .filter(productDraft -> "productKey4".equals(productDraft.getKey()))
            .findFirst();

    assertThat(productDraftKey1)
        .hasValueSatisfying(
            productDraft ->
                assertThat(productDraft.getMasterVariant().getAttributes())
                    .anySatisfy(
                        attributeDraft -> {
                          assertThat(attributeDraft.getName()).isEqualTo("productReference");
                          final JsonNode attributeValue =
                              AttributeUtils.replaceAttributeValueWithJsonAndReturnValue(
                                  attributeDraft);
                          final List<JsonNode> referenceSet =
                              AttributeUtils.getAttributeReferences(attributeValue);
                          assertThat(referenceSet)
                              .anySatisfy(
                                  reference ->
                                      assertThat(reference.get("id").asText()).isEqualTo("prod1"));
                          assertThat(referenceSet)
                              .anySatisfy(
                                  reference ->
                                      assertThat(reference.get("id").asText()).isEqualTo("prod2"));
                        }));

    assertThat(productDraftKey1)
        .hasValueSatisfying(
            productDraft ->
                assertThat(productDraft.getMasterVariant().getAttributes())
                    .anySatisfy(
                        attributeDraft -> {
                          assertThat(attributeDraft.getName()).isEqualTo("categoryReference");
                          final JsonNode attributeValue =
                              AttributeUtils.replaceAttributeValueWithJsonAndReturnValue(
                                  attributeDraft);
                          final List<JsonNode> referenceSet =
                              AttributeUtils.getAttributeReferences(attributeValue);
                          assertThat(referenceSet)
                              .anySatisfy(
                                  reference ->
                                      assertThat(reference.get("id").asText()).isEqualTo("cat1"));
                          assertThat(referenceSet)
                              .anySatisfy(
                                  reference ->
                                      assertThat(reference.get("id").asText()).isEqualTo("cat2"));
                        }));

    assertThat(productDraftKey1)
        .hasValueSatisfying(
            productDraft ->
                assertThat(productDraft.getMasterVariant().getAttributes())
                    .anySatisfy(
                        attributeDraft -> {
                          assertThat(attributeDraft.getName()).isEqualTo("productTypeReference");
                          final JsonNode attributeValue =
                              AttributeUtils.replaceAttributeValueWithJsonAndReturnValue(
                                  attributeDraft);
                          final List<JsonNode> referenceSet =
                              AttributeUtils.getAttributeReferences(attributeValue);
                          assertThat(referenceSet.get(0).get("id").asText()).isEqualTo("prodType1");
                        }));

    assertThat(testLogger.getAllLoggingEvents()).isEmpty();
  }

  @Test
  void transform_WithDiscountedPrices_ShouldRemoveDiscountedPrices() {
    // preparation
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final ProductSyncer productSyncer =
        ProductSyncer.of(sourceClient, mock(ProjectApiRoot.class), getMockedClock(), null);
    final ProductProjection productProjection =
        ProductMixin.toProjection(
            readObjectFromResource("product-key-10.json", Product.class),
            ProductProjectionType.STAGED);

    final String jsonStringProductTypes =
        "{\"data\":{\"productTypes\":{\"results\":[{\"id\":\"53c4a8b4-754f-4b95-b6f2-3e1e70e3d0d3\","
            + "\"key\":\"prodType1\"}]}}}";
    final GraphQLResponse graphQLResponse =
        JsonUtils.fromJsonString(jsonStringProductTypes, GraphQLResponse.class);
    final ApiHttpResponse apiHttpResponse = mock(ApiHttpResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(graphQLResponse);

    final ByProjectKeyGraphqlPost byProjectKeyGraphqlPost = mock(ByProjectKeyGraphqlPost.class);
    when(byProjectKeyGraphqlPost.execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

    when(sourceClient.graphql()).thenReturn(mock());
    when(sourceClient.graphql().post(any(GraphQLRequest.class)))
        .thenReturn(byProjectKeyGraphqlPost);

    // test
    final List<ProductDraft> draftsFromPageStage =
        productSyncer.transform(singletonList(productProjection)).toCompletableFuture().join();

    final Optional<ProductDraft> productDraftKey1 =
        draftsFromPageStage.stream()
            .filter(productDraft -> "productKey10".equals(productDraft.getKey()))
            .findFirst();

    assertThat(productDraftKey1)
        .hasValueSatisfying(
            productDraft ->
                assertThat(productDraft.getMasterVariant().getPrices())
                    .anySatisfy(priceDraft -> assertThat(priceDraft.getDiscounted()).isNull()));
    assertThat(testLogger.getAllLoggingEvents()).isEmpty();
  }

  @Test
  void transform_WithErrorOnGraphQlRequest_ShouldContinueAndLogError() {
    // preparation
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final ProductSyncer productSyncer =
        ProductSyncer.of(sourceClient, mock(ProjectApiRoot.class), getMockedClock(), null);
    final ProductProjection productProjection1 =
        ProductMixin.toProjection(
            readObjectFromResource("product-key-1.json", Product.class),
            ProductProjectionType.STAGED);
    final ProductProjection productProjection2 =
        ProductMixin.toProjection(
            readObjectFromResource("product-key-2.json", Product.class),
            ProductProjectionType.STAGED);

    final ByProjectKeyGraphqlPost byProjectKeyGraphqlPost = mock(ByProjectKeyGraphqlPost.class);
    final BadGatewayException badGatewayException = createBadGatewayException();
    when(sourceClient.graphql()).thenReturn(mock());
    when(sourceClient.graphql().post(any(GraphQLRequest.class)))
        .thenReturn(byProjectKeyGraphqlPost);
    when(byProjectKeyGraphqlPost.execute())
        .thenReturn(CompletableFutureUtils.failed(badGatewayException));

    // test
    final CompletionStage<List<ProductDraft>> draftsFromPageStage =
        productSyncer.transform(List.of(productProjection1, productProjection2));

    // assertions
    assertThat(draftsFromPageStage).isCompletedWithValue(Collections.emptyList());
    assertThat(testLogger.getAllLoggingEvents())
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
  void getQuery_ShouldBuildProductQueryWithoutAnyExpansionPaths() {
    // preparation
    final ProductSyncer productSyncer =
        ProductSyncer.of(
            mock(ProjectApiRoot.class), mock(ProjectApiRoot.class), getMockedClock(), null);

    // test
    final ByProjectKeyProductProjectionsGet query = productSyncer.getQuery();

    // assertion
    assertThat(query.getExpand()).isEmpty();
  }

  @Test
  void getQuery_ShouldBuildProductQueryWithCustomQueryAndLimitSize() {
    // preparation
    final Long limit = 100L;
    final String customQuery =
        "published=true AND masterData(masterVariant(attributes(name= \"abc\" AND value=123)))";

    final ProjectApiRoot apiRoot =
        ApiRootBuilder.of().withApiBaseUrl("apiBaseUrl").build("projectKey");

    final ProductSyncCustomRequest productSyncCustomRequest = new ProductSyncCustomRequest();
    productSyncCustomRequest.setLimit(limit);
    productSyncCustomRequest.setWhere(customQuery);

    final ProductSyncer productSyncer =
        ProductSyncer.of(apiRoot, apiRoot, getMockedClock(), productSyncCustomRequest);

    // test
    final ByProjectKeyProductProjectionsGet query = productSyncer.getQuery();

    // assertion
    assertThat(query.getLimit().get(0)).isEqualTo("100");
    assertThat(query.getWhere()).contains(customQuery);
  }
}
