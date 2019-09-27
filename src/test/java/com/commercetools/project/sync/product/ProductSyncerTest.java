package com.commercetools.project.sync.product;

import static com.commercetools.project.sync.util.TestUtils.getMockedClock;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static io.sphere.sdk.utils.SphereInternalUtils.asSet;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.project.sync.model.request.CombinedResourceKeysRequest;
import com.commercetools.project.sync.model.response.CombinedResult;
import com.commercetools.project.sync.model.response.ReferenceIdKey;
import com.commercetools.project.sync.model.response.ResultingResourcesContainer;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.utils.ProductReferenceReplacementUtils;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.expansion.ExpansionPath;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductCatalogData;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.ChangeName;
import io.sphere.sdk.products.commands.updateactions.Publish;
import io.sphere.sdk.products.commands.updateactions.Unpublish;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.utils.CompletableFutureUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

class ProductSyncerTest {

  private static final TestLogger testLogger = TestLoggerFactory.getTestLogger(ProductSyncer.class);

  @Test
  void of_ShouldCreateProductSyncerInstance() {
    // test
    final ProductSyncer productSyncer =
        ProductSyncer.of(mock(SphereClient.class), mock(SphereClient.class), getMockedClock());

    // assertions
    assertThat(productSyncer).isNotNull();
    assertThat(productSyncer.getQuery()).isInstanceOf(ProductQuery.class);
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
    assertThat(draftsFromPageStage)
        .isCompletedWithValue(
            ProductReferenceReplacementUtils.replaceProductsReferenceIdsWithKeys(productPage));
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
    final ProductQuery query = productSyncer.getQuery();

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

  @Test
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
  }
}
