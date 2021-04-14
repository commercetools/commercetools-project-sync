package com.commercetools.project.sync.producttype;

import static com.commercetools.project.sync.util.TestUtils.getMockedClock;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.producttypes.ProductTypeSync;
import io.sphere.sdk.client.SphereApiConfig;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

class ProductTypeSyncerTest {
  private final TestLogger syncerTestLogger =
      TestLoggerFactory.getTestLogger(ProductTypeSyncer.class);

  @BeforeEach
  void setup() {
    syncerTestLogger.clearAll();
  }

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
    // TODO: (ahmetoz) adapt changes
    //    assertThat(query).isEqualTo(ProductTypeReferenceResolutionUtils.buildProductTypeQuery(1));
  }

  @Test
  void syncWithError_WhenNoKeyIsProvided_ShouldCallErrorCallback() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    final SphereClient targetClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereApiConfig.of("source-project"));
    when(targetClient.getConfig()).thenReturn(SphereApiConfig.of("target-project"));

    final List<ProductType> productTypePage =
        asList(readObjectFromResource("product-type-without-key.json", ProductType.class));

    final PagedQueryResult<ProductType> pagedQueryResult = mock(PagedQueryResult.class);
    when(pagedQueryResult.getResults()).thenReturn(productTypePage);
    when(sourceClient.execute(any(ProductTypeQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(pagedQueryResult));

    // test
    final ProductTypeSyncer productTypeSyncer =
        ProductTypeSyncer.of(sourceClient, targetClient, mock(Clock.class));
    productTypeSyncer.sync(null, true).toCompletableFuture().join();

    // assertion
    final LoggingEvent errorLog = syncerTestLogger.getAllLoggingEvents().get(0);
    assertThat(errorLog.getMessage())
        .isEqualTo(
            "Error when trying to sync product type. Existing key: <<not present>>. Update actions: []");
    assertThat(errorLog.getThrowable().get().getMessage())
        .isEqualTo(
            "ProductTypeDraft with name: main doesn't have a key. Please make sure all productType drafts have keys.");
  }
}
