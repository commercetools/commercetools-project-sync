package com.commercetools.project.sync.producttype;

import static com.commercetools.project.sync.util.TestUtils.getMockedClock;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.sync.producttypes.ProductTypeSync;
import com.commercetools.sync.producttypes.utils.ProductTypeReferenceReplacementUtils;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import java.util.List;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

class ProductTypeSyncerTest {
  @Test
  void of_ShouldCreateProductTypeSyncerInstance() {
    // test
    final ProductTypeSyncer productTypeSyncer =
        ProductTypeSyncer.of(mock(SphereClient.class), mock(SphereClient.class), getMockedClock());

    // assertions
    assertThat(productTypeSyncer).isNotNull();
    assertThat(productTypeSyncer.getQuery()).isInstanceOf(ProductTypeQuery.class);
    assertThat(productTypeSyncer.getSync()).isExactlyInstanceOf(ProductTypeSync.class);
  }

  @Test
  void transform_ShouldConvertResourcesToDrafts() {
    // preparation
    final ProductTypeSyncer productTypeSyncer =
        ProductTypeSyncer.of(mock(SphereClient.class), mock(SphereClient.class), getMockedClock());
    final List<ProductType> productTypePage =
        asList(
            readObjectFromResource("product-type-key-1.json", ProductType.class),
            readObjectFromResource("product-type-key-2.json", ProductType.class));

    // test
    final CompletionStage<List<ProductTypeDraft>> draftsFromPageStage =
        productTypeSyncer.transform(productTypePage);

    // assertions
    assertThat(draftsFromPageStage)
        .isCompletedWithValue(
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
        ProductTypeSyncer.of(mock(SphereClient.class), mock(SphereClient.class), getMockedClock());

    // test
    final ProductTypeQuery query = productTypeSyncer.getQuery();

    // assertion
    assertThat(query).isEqualTo(ProductTypeReferenceReplacementUtils.buildProductTypeQuery(1));
  }
}
