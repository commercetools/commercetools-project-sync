package com.commercetools.project.sync.service.impl;

import static io.sphere.sdk.utils.SphereInternalUtils.asSet;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commercetools.project.sync.model.request.CombinedResourceKeysRequest;
import com.commercetools.project.sync.model.response.CombinedResult;
import com.commercetools.project.sync.model.response.ReferenceIdKey;
import com.commercetools.project.sync.model.response.ResultingResourcesContainer;
import com.commercetools.project.sync.service.ReferencesService;
import io.sphere.sdk.client.SphereApiConfig;
import io.sphere.sdk.client.SphereClient;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

class ReferencesServiceImplTest {

  private static final TestLogger testLogger =
      TestLoggerFactory.getTestLogger(ReferencesServiceImpl.class);

  @Test
  void getIdToKeys_WithAllEmptyIds_ShouldReturnCompletedFutureOfCacheWithoutMakingCtpRequest() {
    // preparation
    final SphereClient ctpClient = mock(SphereClient.class);
    final ReferencesService referencesService = new ReferencesServiceImpl(ctpClient);

    // test
    final CompletionStage<Map<String, String>> idToKeysStage =
        referencesService.getIdToKeys(emptySet(), emptySet(), emptySet());

    // assertion
    assertThat(idToKeysStage).isCompletedWithValue(emptyMap());
    verify(ctpClient, never()).execute(any());
  }

  @Test
  void getIdToKeys_WithOnlyProductTypeIds_ShouldFetchOnceAndCacheIds() {
    // preparation
    final SphereClient ctpClient = mock(SphereClient.class);
    final CombinedResult mockResult =
        new CombinedResult(
            null,
            null,
            new ResultingResourcesContainer(
                asSet(new ReferenceIdKey("productTypeId", "productTypeKey"))));
    when(ctpClient.execute(any(CombinedResourceKeysRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(mockResult));
    final ReferencesService referencesService = new ReferencesServiceImpl(ctpClient);

    // test
    final CompletionStage<Map<String, String>> idToKeysStage =
        referencesService.getIdToKeys(emptySet(), emptySet(), asSet("productTypeId"));

    // assertion
    final HashMap<String, String> expectedCache = new HashMap<>();
    expectedCache.put("productTypeId", "productTypeKey");
    assertThat(idToKeysStage).isCompletedWithValue(expectedCache);
    verify(ctpClient, times(1)).execute(any(CombinedResourceKeysRequest.class));
  }

  @Test
  void getIdToKeys_WithNullResponse_ShouldReturnCurrentCacheWithoutCachingNewIds() {
    // preparation
    final SphereClient ctpClient = mock(SphereClient.class);
    when(ctpClient.execute(any(CombinedResourceKeysRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(null));
    final ReferencesService referencesService = new ReferencesServiceImpl(ctpClient);

    // test
    final CompletionStage<Map<String, String>> idToKeysStage =
        referencesService.getIdToKeys(
            asSet("productId"), asSet("categoryId"), asSet("productTypeId"));

    // assertion
    assertThat(idToKeysStage).isCompletedWithValue(new HashMap<>());
    verify(ctpClient, times(1)).execute(any(CombinedResourceKeysRequest.class));
  }

  @Test
  void getIdToKeys_WithNonCachedIds_ShouldFetchOnceAndCacheIds() {
    // preparation
    final SphereClient ctpClient = mock(SphereClient.class);
    final CombinedResult mockResult =
        new CombinedResult(
            new ResultingResourcesContainer(asSet(new ReferenceIdKey("productId", "productKey"))),
            new ResultingResourcesContainer(asSet(new ReferenceIdKey("categoryId", "categoryKey"))),
            new ResultingResourcesContainer(
                asSet(new ReferenceIdKey("productTypeId", "productTypeKey"))));
    when(ctpClient.execute(any(CombinedResourceKeysRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(mockResult));
    final ReferencesService referencesService = new ReferencesServiceImpl(ctpClient);

    // test
    final CompletionStage<Map<String, String>> idToKeysStage =
        referencesService.getIdToKeys(
            asSet("productId"), asSet("categoryId"), asSet("productTypeId"));
    // get again to test second fetch doesnt make request to ctp
    referencesService.getIdToKeys(asSet("productId"), asSet("categoryId"), asSet("productTypeId"));

    // assertion
    final HashMap<String, String> expectedCache = new HashMap<>();
    expectedCache.put("productId", "productKey");
    expectedCache.put("categoryId", "categoryKey");
    expectedCache.put("productTypeId", "productTypeKey");
    assertThat(idToKeysStage).isCompletedWithValue(expectedCache);
    verify(ctpClient, times(1)).execute(any(CombinedResourceKeysRequest.class));
  }

  @Test
  void getIdToKeys_WithSomeNonCachedIds_ShouldFetchTwiceAndCacheIds() {
    // preparation
    final SphereClient ctpClient = mock(SphereClient.class);
    final CombinedResult mockResult =
        new CombinedResult(
            new ResultingResourcesContainer(asSet(new ReferenceIdKey("productId", "productKey"))),
            new ResultingResourcesContainer(asSet(new ReferenceIdKey("categoryId", "categoryKey"))),
            new ResultingResourcesContainer(
                asSet(new ReferenceIdKey("productTypeId", "productTypeKey"))));
    when(ctpClient.execute(any(CombinedResourceKeysRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(mockResult));
    final ReferencesService referencesService = new ReferencesServiceImpl(ctpClient);

    // test
    final CompletionStage<Map<String, String>> idToKeysStage =
        referencesService.getIdToKeys(
            asSet("productId"), asSet("categoryId"), asSet("productTypeId"));
    // get again to test second fetch doesnt make request to ctp
    referencesService.getIdToKeys(
        asSet("newProductId"), asSet("categoryId"), asSet("productTypeId"));

    // assertion
    final HashMap<String, String> expectedCache = new HashMap<>();
    expectedCache.put("productId", "productKey");
    expectedCache.put("categoryId", "categoryKey");
    expectedCache.put("productTypeId", "productTypeKey");
    assertThat(idToKeysStage).isCompletedWithValue(expectedCache);
    verify(ctpClient, times(2)).execute(any(CombinedResourceKeysRequest.class));
  }

  @Test
  void getIdToKeys_WithSomeBlankKeys_ShouldCacheNonBlankKeysAndLogErrors() {
    // preparation
    final SphereClient ctpClient = mock(SphereClient.class);
    when(ctpClient.getConfig()).thenReturn(SphereApiConfig.of("test-project"));
    final CombinedResult mockResult =
        new CombinedResult(
            new ResultingResourcesContainer(
                asSet(
                    new ReferenceIdKey("productId", "productKey"),
                    new ReferenceIdKey("productId2", ""))),
            new ResultingResourcesContainer(asSet(new ReferenceIdKey("categoryId", ""))),
            new ResultingResourcesContainer(asSet(new ReferenceIdKey("productTypeId", null))));
    when(ctpClient.execute(any(CombinedResourceKeysRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(mockResult));
    final ReferencesService referencesService = new ReferencesServiceImpl(ctpClient);

    // test
    final CompletionStage<Map<String, String>> idToKeysStage =
        referencesService.getIdToKeys(
            asSet("productId"), asSet("categoryId"), asSet("productTypeId"));

    // assertion
    final HashMap<String, String> expectedCache = new HashMap<>();
    expectedCache.put("productId", "productKey");
    assertThat(idToKeysStage).isCompletedWithValue(expectedCache);
    verify(ctpClient, times(1)).execute(any(CombinedResourceKeysRequest.class));
    assertThat(testLogger.getAllLoggingEvents())
        .containsExactlyInAnyOrder(
            LoggingEvent.error(
                "The key for the category with id 'categoryId' is blank. Please make sure all "
                    + "categories, in the source project with key 'test-project', have non-blank "
                    + "(i.e. non-null and non-empty) keys."),
            LoggingEvent.error(
                "The key for the product with id 'productId2' is blank. Please make sure all "
                    + "products, in the source project with key 'test-project', have non-blank "
                    + "(i.e. non-null and non-empty) keys."),
            LoggingEvent.error(
                "The key for the productType with id 'productTypeId' is blank. Please make sure all "
                    + "productTypes, in the source project with key 'test-project', have non-blank "
                    + "(i.e. non-null and non-empty) keys."));
  }
}
