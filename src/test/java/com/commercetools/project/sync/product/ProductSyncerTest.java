package com.commercetools.project.sync.product;

import static com.commercetools.sync.products.utils.ProductReferenceReplacementUtils.buildProductQuery;
import static com.commercetools.sync.products.utils.ProductReferenceReplacementUtils.replaceProductsReferenceIdsWithKeys;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.products.ProductSync;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductCatalogData;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.ChangeName;
import io.sphere.sdk.products.commands.updateactions.Publish;
import io.sphere.sdk.products.commands.updateactions.Unpublish;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProductSyncerTest {

  @Test
  void of_ShouldCreateProductSyncerInstance() {
    // test
    final ProductSyncer productSyncer =
        ProductSyncer.of(mock(SphereClient.class), mock(SphereClient.class));

    // assertions
    assertThat(productSyncer).isNotNull();
    assertThat(productSyncer.getQuery()).isEqualTo(buildProductQuery());
    assertThat(productSyncer.getSync()).isInstanceOf(ProductSync.class);
  }

  @Test
  void transformResourcesToDrafts_ShouldReplaceProductReferenceIdsWithKeys() {
    // preparation
    final ProductSyncer productSyncer =
        ProductSyncer.of(mock(SphereClient.class), mock(SphereClient.class));
    final List<Product> productPage =
        asList(
            readObjectFromResource("product-key-1.json", Product.class),
            readObjectFromResource("product-key-2.json", Product.class));

    // test
    final List<ProductDraft> draftsFromPage = productSyncer.transformResourcesToDrafts(productPage);

    // assertions
    final List<ProductDraft> expectedResult = replaceProductsReferenceIdsWithKeys(productPage);
    assertThat(draftsFromPage).isEqualTo(expectedResult);
  }

  @Test
  void getQuery_ShouldBuildProductQuery() {
    // preparation
    final ProductSyncer productSyncer =
        ProductSyncer.of(mock(SphereClient.class), mock(SphereClient.class));

    // test
    final ProductQuery query = productSyncer.getQuery();

    // assertion
    assertThat(query).isEqualTo(buildProductQuery());
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
