package com.commercetools.project.sync.product;

import static com.commercetools.project.sync.util.TestUtils.getMockedClock;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static io.sphere.sdk.utils.SphereInternalUtils.asSet;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.commercetools.project.sync.model.request.CombinedResourceKeysRequest;
import com.commercetools.project.sync.model.response.CombinedResult;
import com.commercetools.project.sync.model.response.ReferenceIdKey;
import com.commercetools.project.sync.model.response.ResultingResourcesContainer;
import com.commercetools.sync.products.ProductSync;
import com.fasterxml.jackson.databind.JsonNode;

import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.SphereApiConfig;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.expansion.ExpansionPath;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.utils.CompletableFutureUtils;
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
    // test
    final ProductSyncer productSyncer =
        ProductSyncer.of(mock(SphereClient.class), mock(SphereClient.class), getMockedClock());

    // assertions
    assertThat(productSyncer).isNotNull();
    assertThat(productSyncer.getQuery(null)).isInstanceOf(ProductQuery.class);
    assertThat(productSyncer.getSync()).isExactlyInstanceOf(ProductSync.class);
  }

  @Test
  void transform_WithAttributeReferences_ShouldReplaceProductReferenceIdsWithKeys() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    final ProductSyncer productSyncer =
        ProductSyncer.of(sourceClient, mock(SphereClient.class), getMockedClock());
    final List<Product> productPage =
        asList(
            readObjectFromResource("product-key-1.json", Product.class),
            readObjectFromResource("product-key-2.json", Product.class));

    final ResultingResourcesContainer productsResult =
        new ResultingResourcesContainer(
            asSet(
                new ReferenceIdKey("53c4a8b4-754f-4b95-b6f2-3e1e70e3d0c1", "prod1"),
                new ReferenceIdKey("53c4a8b4-754f-4b95-b6f2-3e1e70e3d0c5", "prod2")));
    final ResultingResourcesContainer productTypesResult =
        new ResultingResourcesContainer(
            asSet(new ReferenceIdKey("53c4a8b4-754f-4b95-b6f2-3e1e70e3d0c2", "prodType1")));
    final ResultingResourcesContainer categoriesResult =
        new ResultingResourcesContainer(
            asSet(
                new ReferenceIdKey("53c4a8b4-754f-4b95-b6f2-3e1e70e3d0c3", "cat1"),
                new ReferenceIdKey("53c4a8b4-754f-4b95-b6f2-3e1e70e3d0c4", "cat2")));

    when(sourceClient.execute(any(CombinedResourceKeysRequest.class)))
        .thenReturn(
            CompletableFuture.completedFuture(
                new CombinedResult(productsResult, categoriesResult, productTypesResult)));

    // test
    final List<ProductDraft> draftsFromPageStage =
        productSyncer.transform(productPage).toCompletableFuture().join();

    // assertions

    final Optional<ProductDraft> productDraftKey1 =
        draftsFromPageStage
            .stream()
            .filter(productDraft -> "productKey1".equals(productDraft.getKey()))
            .findFirst();

    assertThat(productDraftKey1)
        .hasValueSatisfying(
            productDraft ->
                assertThat(productDraft.getMasterVariant().getAttributes())
                    .anySatisfy(
                        attributeDraft -> {
                          assertThat(attributeDraft.getName()).isEqualTo("productReference");
                          final JsonNode referenceSet = attributeDraft.getValue();
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
                          final JsonNode referenceSet = attributeDraft.getValue();
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
                          assertThat(attributeDraft.getValue().get("id").asText())
                              .isEqualTo("prodType1");
                        }));

    final Optional<ProductDraft> productDraftKey2 =
        draftsFromPageStage
            .stream()
            .filter(productDraft -> "productKey2".equals(productDraft.getKey()))
            .findFirst();

    assertThat(productDraftKey2)
        .hasValueSatisfying(
            productDraft ->
                assertThat(productDraft.getMasterVariant().getAttributes())
                    .anySatisfy(
                        attributeDraft -> {
                          assertThat(attributeDraft.getName()).isEqualTo("productReference");
                          assertThat(attributeDraft.getValue().get("id").asText())
                              .isEqualTo("prod1");
                        }));

    assertThat(productDraftKey2)
        .hasValueSatisfying(
            productDraft ->
                assertThat(productDraft.getMasterVariant().getAttributes())
                    .anySatisfy(
                        attributeDraft -> {
                          assertThat(attributeDraft.getName()).isEqualTo("categoryReference");
                          assertThat(attributeDraft.getValue().get("id").asText())
                              .isEqualTo("cat1");
                        }));

    assertThat(productDraftKey2)
        .hasValueSatisfying(
            productDraft ->
                assertThat(productDraft.getMasterVariant().getAttributes())
                    .anySatisfy(
                        attributeDraft -> {
                          assertThat(attributeDraft.getName()).isEqualTo("productTypeReference");
                          assertThat(attributeDraft.getValue().get("id").asText())
                              .isEqualTo("prodType1");
                        }));

    assertThat(testLogger.getAllLoggingEvents()).isEmpty();
  }

  @Test
  void
      transform_WithIrresolvableAttributeReferences_ShouldSkipProductsWithIrresolvableReferences() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereApiConfig.of("test-project"));
    final ProductSyncer productSyncer =
        ProductSyncer.of(sourceClient, mock(SphereClient.class), getMockedClock());
    final List<Product> productPage =
        asList(
            readObjectFromResource("product-key-1.json", Product.class),
            readObjectFromResource("product-key-2.json", Product.class));

    final ResultingResourcesContainer productsResult =
        new ResultingResourcesContainer(
            asSet(new ReferenceIdKey("53c4a8b4-754f-4b95-b6f2-3e1e70e3d0c1", "prod1")));
    final ResultingResourcesContainer productTypesResult =
        new ResultingResourcesContainer(
            asSet(new ReferenceIdKey("53c4a8b4-754f-4b95-b6f2-3e1e70e3d0c2", "prodType1")));
    final ResultingResourcesContainer categoriesResult =
        new ResultingResourcesContainer(
            asSet(new ReferenceIdKey("53c4a8b4-754f-4b95-b6f2-3e1e70e3d0c3", "cat1")));

    when(sourceClient.execute(any(CombinedResourceKeysRequest.class)))
        .thenReturn(
            CompletableFuture.completedFuture(
                new CombinedResult(productsResult, categoriesResult, productTypesResult)));

    // test
    final List<ProductDraft> draftsFromPageStage =
        productSyncer.transform(productPage).toCompletableFuture().join();

    // assertions
    assertThat(draftsFromPageStage).hasSize(1);
    assertThat(draftsFromPageStage)
        .anySatisfy(
            productDraft ->
                assertThat(productDraft.getMasterVariant().getAttributes())
                    .anySatisfy(
                        attributeDraft -> {
                          assertThat(attributeDraft.getName()).isEqualTo("productReference");
                          assertThat(attributeDraft.getValue().get("id").asText())
                              .isEqualTo("prod1");
                        }));

    assertThat(draftsFromPageStage)
        .anySatisfy(
            productDraft ->
                assertThat(productDraft.getMasterVariant().getAttributes())
                    .anySatisfy(
                        attributeDraft -> {
                          assertThat(attributeDraft.getName()).isEqualTo("categoryReference");
                          assertThat(attributeDraft.getValue().get("id").asText())
                              .isEqualTo("cat1");
                        }));

    assertThat(draftsFromPageStage)
        .anySatisfy(
            productDraft ->
                assertThat(productDraft.getMasterVariant().getAttributes())
                    .anySatisfy(
                        attributeDraft -> {
                          assertThat(attributeDraft.getName()).isEqualTo("productTypeReference");
                          assertThat(attributeDraft.getValue().get("id").asText())
                              .isEqualTo("prodType1");
                        }));

    assertThat(testLogger.getAllLoggingEvents())
        .anySatisfy(
            loggingEvent ->
                assertThat(loggingEvent.getMessage())
                    .contains(
                        "The product with id 'ba81a6da-cf83-435b-a89e-2afab579846f' on the source project ('test-project') "
                            + "will not be synced because it has the following reference attribute(s): \n"
                            + "[{\"id\":\"53c4a8b4-754f-4b95-b6f2-3e1e70e3d0c5\",\"typeId\":\"product\"}, "
                            + "{\"id\":\"53c4a8b4-754f-4b95-b6f2-3e1e70e3d0c4\",\"typeId\":\"category\"}].\n"
                            + "These references are either pointing to a non-existent resource or to an existing one but with a blank key. "
                            + "Please make sure these referenced resources are existing and have non-blank (i.e. non-null and non-empty) keys."));
  }

  @Test
  void transform_WithErrorOnGraphQlRequest_ShouldContinueAndLogError() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    final ProductSyncer productSyncer =
        ProductSyncer.of(sourceClient, mock(SphereClient.class), getMockedClock());
    final List<Product> productPage =
        asList(
            readObjectFromResource("product-key-1.json", Product.class),
            readObjectFromResource("product-key-2.json", Product.class));

    final BadGatewayException badGatewayException =
        new BadGatewayException("Failed Graphql request");
    when(sourceClient.execute(any(CombinedResourceKeysRequest.class)))
        .thenReturn(CompletableFutureUtils.failed(badGatewayException));

    // test
    final CompletionStage<List<ProductDraft>> draftsFromPageStage =
        productSyncer.transform(productPage);

    // assertions
    assertThat(draftsFromPageStage).isCompletedWithValue(Collections.emptyList());
    assertThat(testLogger.getAllLoggingEvents())
        .anySatisfy(
            loggingEvent -> {
              assertThat(loggingEvent.getMessage())
                  .contains(
                      "Failed to replace referenced resource ids with keys on the"
                          + " attributes of the products in the current fetched page from the source project.");
              assertThat(loggingEvent.getThrowable().isPresent()).isTrue();
              assertThat(loggingEvent.getThrowable().get()).isEqualTo(badGatewayException);
            });
  }

  @Test
  void getQuery_ShouldBuildProductQuery() {
    // preparation
    final ProductSyncer productSyncer =
        ProductSyncer.of(mock(SphereClient.class), mock(SphereClient.class), getMockedClock());

    // test
    final ProductQuery query = productSyncer.getQuery(null);

    // assertion
    assertThat(query.expansionPaths())
        .containsExactly(
            ExpansionPath.of("productType"),
            ExpansionPath.of("taxCategory"),
            ExpansionPath.of("state"),
            ExpansionPath.of("masterData.staged.categories[*]"),
            ExpansionPath.of("masterData.staged.masterVariant.prices[*].channel"),
            ExpansionPath.of("masterData.staged.variants[*].prices[*].channel"),
            ExpansionPath.of("masterData.staged.masterVariant.prices[*].custom.type"),
            ExpansionPath.of("masterData.staged.variants[*].prices[*].custom.type"),
            ExpansionPath.of("masterData.staged.masterVariant.assets[*].custom.type"),
            ExpansionPath.of("masterData.staged.variants[*].assets[*].custom.type"));
  }

 /* @Test
  void appendPublishIfPublished_WithPublishedProductAndEmptyActions_ShouldNotAppendPublish() {
    final ProductCatalogData masterData = mock(ProductCatalogData.class);
    when(masterData.isPublished()).thenReturn(true);

    final Product product = mock(Product.class);
    when(product.getMasterData()).thenReturn(masterData);

    final List<UpdateAction<Product>> newUpdateActions =
        ProductSyncer.appendPublishIfPublished(
            new ArrayList<>(), mock(ProductDraft.class), product);

    assertThat(newUpdateActions).isEmpty();
  }
  
  @Test
  void appendPublishIfPublished_WithPublishedProductAndNonEmptyActions_ShouldAppendPublish() {
    final ProductCatalogData masterData = mock(ProductCatalogData.class);
    when(masterData.isPublished()).thenReturn(true);

    final Product product = mock(Product.class);
    when(product.getMasterData()).thenReturn(masterData);

    final ArrayList<UpdateAction<Product>> updateActions = new ArrayList<>();
    updateActions.add(ChangeName.of(ofEnglish("foo")));

    final List<UpdateAction<Product>> newUpdateActions =
        ProductSyncer.appendPublishIfPublished(updateActions, mock(ProductDraft.class), product);

    assertThat(newUpdateActions).hasSize(2);
    assertThat(newUpdateActions.get(1)).isEqualTo(Publish.of());
  }

  @Test
  void appendPublishIfPublished_WithUnPublishedProductAndEmptyActions_ShouldNotAppendPublish() {
    final ProductCatalogData masterData = mock(ProductCatalogData.class);
    when(masterData.isPublished()).thenReturn(false);

    final Product product = mock(Product.class);
    when(product.getMasterData()).thenReturn(masterData);

    final List<UpdateAction<Product>> newUpdateActions =
        ProductSyncer.appendPublishIfPublished(
            new ArrayList<>(), mock(ProductDraft.class), product);

    assertThat(newUpdateActions).isEmpty();
  }

  @Test
  void appendPublishIfPublished_WithUnPublishedProductAndNonEmptyActions_ShouldNotAppendPublish() {
    final ProductCatalogData masterData = mock(ProductCatalogData.class);
    when(masterData.isPublished()).thenReturn(false);

    final Product product = mock(Product.class);
    when(product.getMasterData()).thenReturn(masterData);

    final ArrayList<UpdateAction<Product>> updateActions = new ArrayList<>();
    updateActions.add(ChangeName.of(ofEnglish("foo")));

    final List<UpdateAction<Product>> newUpdateActions =
        ProductSyncer.appendPublishIfPublished(updateActions, mock(ProductDraft.class), product);

    assertThat(newUpdateActions).hasSize(1);
    assertThat(newUpdateActions.get(0)).isEqualTo(ChangeName.of(ofEnglish("foo")));
  }

  @Test
  void appendPublishIfPublished_WithPublishedProductAndOnePublish_ShouldNotAppendPublish() {
    final ProductCatalogData masterData = mock(ProductCatalogData.class);
    when(masterData.isPublished()).thenReturn(true);

    final Product product = mock(Product.class);
    when(product.getMasterData()).thenReturn(masterData);

    final ArrayList<UpdateAction<Product>> updateActions = new ArrayList<>();
    updateActions.add(Publish.of());

    final List<UpdateAction<Product>> newUpdateActions =
        ProductSyncer.appendPublishIfPublished(updateActions, mock(ProductDraft.class), product);

    assertThat(newUpdateActions).hasSize(1);
    assertThat(newUpdateActions.get(0)).isEqualTo(Publish.of());
  }

  @Test
  void appendPublishIfPublished_WithUnPublishedProductAndOnePublishAction_ShouldNotAppendPublish() {
    final ProductCatalogData masterData = mock(ProductCatalogData.class);
    when(masterData.isPublished()).thenReturn(false);

    final Product product = mock(Product.class);
    when(product.getMasterData()).thenReturn(masterData);

    final ArrayList<UpdateAction<Product>> updateActions = new ArrayList<>();
    updateActions.add(Publish.of());

    final List<UpdateAction<Product>> newUpdateActions =
        ProductSyncer.appendPublishIfPublished(updateActions, mock(ProductDraft.class), product);

    assertThat(newUpdateActions).hasSize(1);
    assertThat(newUpdateActions.get(0)).isEqualTo(Publish.of());
  }

  @Test
  void appendPublishIfPublished_WithPublishedProductAndOneUnPublishAction_ShouldNotAppendPublish() {
    final ProductCatalogData masterData = mock(ProductCatalogData.class);
    when(masterData.isPublished()).thenReturn(true);

    final Product product = mock(Product.class);
    when(product.getMasterData()).thenReturn(masterData);

    final ArrayList<UpdateAction<Product>> updateActions = new ArrayList<>();
    updateActions.add(Unpublish.of());

    final List<UpdateAction<Product>> newUpdateActions =
        ProductSyncer.appendPublishIfPublished(updateActions, mock(ProductDraft.class), product);

    assertThat(newUpdateActions).hasSize(1);
    assertThat(newUpdateActions.get(0)).isEqualTo(Unpublish.of());
  }*/
}
