package com.commercetools.project.sync.producttype;

import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.sync.producttypes.ProductTypeSync;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProductTypeSyncerTest {
  @Test
  void of_ShouldCreateProductTypeSyncerInstance() {
    // test
    final ProductTypeSyncer productTypeSyncer =
        ProductTypeSyncer.of(mock(SphereClient.class), mock(SphereClient.class));

    // assertions
    assertThat(productTypeSyncer).isNotNull();
    assertThat(productTypeSyncer.getQuery()).isEqualTo(ProductTypeQuery.of());
    assertThat(productTypeSyncer.getSync()).isInstanceOf(ProductTypeSync.class);
  }

  @Test
  void transform_ShouldConvertResourcesToDrafts() {
    // preparation
    final ProductTypeSyncer productTypeSyncer =
        ProductTypeSyncer.of(mock(SphereClient.class), mock(SphereClient.class));
    final List<ProductType> productTypePage =
        asList(
            readObjectFromResource("product-type-key-1.json", ProductType.class),
            readObjectFromResource("product-type-key-2.json", ProductType.class));

    // test
    final List<ProductTypeDraft> draftsFromPage = productTypeSyncer.transform(productTypePage);

    // assertions
    assertThat(draftsFromPage)
        .isEqualTo(
            productTypePage
                .stream()
                .map(ProductTypeDraftBuilder::of)
                .map(ProductTypeDraftBuilder::build)
                .collect(toList()));
  }

  @Test
  void getQuery_ShouldBuildProductTypeQuery() {
    // preparation
    final ProductTypeSyncer productTypeSyncer =
        ProductTypeSyncer.of(mock(SphereClient.class), mock(SphereClient.class));

    // test
    final ProductTypeQuery query = productTypeSyncer.getQuery();

    // assertion
    assertThat(query).isEqualTo(ProductTypeQuery.of());
  }
}
