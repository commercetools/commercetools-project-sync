package com.commercetools.project.sync.producttype;

import static com.commercetools.project.sync.util.TestUtils.getMockedClock;
import static com.commercetools.project.sync.util.TestUtils.readObjectFromResource;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ByProjectKeyProductTypesGet;
import com.commercetools.api.client.ByProjectKeyProductTypesRequestBuilder;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.product_type.AttributeDefinitionDraftBuilder;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypeDraftBuilder;
import com.commercetools.api.models.product_type.ProductTypePagedQueryResponse;
import com.commercetools.sync.producttypes.ProductTypeSync;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
    final ProjectApiRoot projectApiRoot = mock(ProjectApiRoot.class);
    final ByProjectKeyProductTypesRequestBuilder byProjectKeyProductTypesRequestBuilder = mock();
    when(projectApiRoot.productTypes()).thenReturn(byProjectKeyProductTypesRequestBuilder);
    final ByProjectKeyProductTypesGet byProjectKeyProductTypesGet = mock();
    when(byProjectKeyProductTypesRequestBuilder.get()).thenReturn(byProjectKeyProductTypesGet);

    // test
    final ProductTypeSyncer productTypeSyncer =
        ProductTypeSyncer.of(projectApiRoot, projectApiRoot, getMockedClock());

    // assertions
    assertThat(productTypeSyncer).isNotNull();
    assertThat(productTypeSyncer.getQuery()).isEqualTo(byProjectKeyProductTypesGet);
    assertThat(productTypeSyncer.getSync()).isExactlyInstanceOf(ProductTypeSync.class);
  }

  @Test
  void transform_ShouldConvertResourcesToDrafts() {
    // preparation
    final ProductTypeSyncer productTypeSyncer =
        ProductTypeSyncer.of(
            mock(ProjectApiRoot.class), mock(ProjectApiRoot.class), getMockedClock());
    final List<ProductType> productTypes =
        asList(
            readObjectFromResource("product-type-key-1.json", ProductType.class),
            readObjectFromResource("product-type-key-2.json", ProductType.class));

    // test
    final List<ProductTypeDraft> productDrafts =
        productTypeSyncer.transform(productTypes).toCompletableFuture().join();

    // assertions
    final List<ProductTypeDraft> expectedProductTypeDraft =
        productTypes.stream()
            .map(
                (p) ->
                    ProductTypeDraftBuilder.of()
                        .key(p.getKey())
                        .name(p.getName())
                        .description(p.getDescription())
                        .attributes(
                            p.getAttributes().stream()
                                .map(
                                    attributeDefinition ->
                                        AttributeDefinitionDraftBuilder.of()
                                            .name(attributeDefinition.getName())
                                            .label(attributeDefinition.getLabel())
                                            .type(attributeDefinition.getType())
                                            .isRequired(attributeDefinition.getIsRequired())
                                            .inputHint(attributeDefinition.getInputHint())
                                            .attributeConstraint(
                                                attributeDefinition.getAttributeConstraint())
                                            .isSearchable(attributeDefinition.getIsSearchable())
                                            .inputTip(attributeDefinition.getInputTip())
                                            .build())
                                .collect(toList()))
                        .build())
            .collect(toList());

    assertProductTypes(productDrafts.get(0), expectedProductTypeDraft.get(0));
    assertProductTypes(productDrafts.get(1), expectedProductTypeDraft.get(1));
  }

  private static void assertProductTypes(
      final ProductTypeDraft productTypeDraft, final ProductTypeDraft expectedProductTypeDraft) {
    assertThat(productTypeDraft.getKey()).isEqualTo(expectedProductTypeDraft.getKey());
    assertThat(productTypeDraft.getName()).isEqualTo(expectedProductTypeDraft.getName());
    assertThat(productTypeDraft.getDescription())
        .isEqualTo(expectedProductTypeDraft.getDescription());
    assertThat(productTypeDraft.getAttributes().size())
        .isEqualTo(expectedProductTypeDraft.getAttributes().size());
  }

  @Test
  void syncWithError_WhenNoKeyIsProvided_ShouldCallErrorCallback() {
    // preparation
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final ProjectApiRoot targetClient = mock(ProjectApiRoot.class);

    final List<ProductType> productTypes =
        List.of(readObjectFromResource("product-type-without-key.json", ProductType.class));

    final ByProjectKeyProductTypesRequestBuilder byProjectKeyProductTypesRequestBuilder = mock();
    when(sourceClient.productTypes()).thenReturn(byProjectKeyProductTypesRequestBuilder);
    final ByProjectKeyProductTypesGet byProjectKeyProductTypesGet = mock();
    when(byProjectKeyProductTypesRequestBuilder.get()).thenReturn(byProjectKeyProductTypesGet);
    when(byProjectKeyProductTypesGet.withSort(anyString())).thenReturn(byProjectKeyProductTypesGet);
    when(byProjectKeyProductTypesGet.withLimit(anyInt())).thenReturn(byProjectKeyProductTypesGet);
    when(byProjectKeyProductTypesGet.withWithTotal(anyBoolean()))
        .thenReturn(byProjectKeyProductTypesGet);

    final ApiHttpResponse<ProductTypePagedQueryResponse> apiHttpResponse =
        mock(ApiHttpResponse.class);
    final ProductTypePagedQueryResponse productTypePagedQueryResponse =
        mock(ProductTypePagedQueryResponse.class);
    when(productTypePagedQueryResponse.getResults()).thenReturn(productTypes);
    when(apiHttpResponse.getBody()).thenReturn(productTypePagedQueryResponse);

    when(byProjectKeyProductTypesGet.execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

    // test
    final ProductTypeSyncer productTypeSyncer =
        ProductTypeSyncer.of(sourceClient, targetClient, mock(Clock.class));
    productTypeSyncer.sync(null, true).toCompletableFuture().join();

    // assertion
    final LoggingEvent errorLog = syncerTestLogger.getAllLoggingEvents().get(1);
    assertThat(errorLog.getMessage())
        .isEqualTo(
            "Error when trying to sync product type. Existing key: <<not present>>. Update actions: []");
    assertThat(errorLog.getThrowable().get().getMessage())
        .isEqualTo(
            "ProductTypeDraft with name: main doesn't have a key. Please make sure all productType drafts have keys.");
  }
}
