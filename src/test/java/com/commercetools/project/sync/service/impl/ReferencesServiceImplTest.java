package com.commercetools.project.sync.service.impl;

import static io.sphere.sdk.utils.SphereInternalUtils.asSet;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commercetools.project.sync.model.request.ResourceIdsGraphQlRequest;
import com.commercetools.project.sync.service.ReferencesService;
import com.commercetools.sync.commons.models.ResourceKeyId;
import com.commercetools.sync.commons.models.ResourceKeyIdGraphQlResult;
import com.fasterxml.jackson.databind.JsonNode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.sphere.sdk.client.SphereApiConfig;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

class ReferencesServiceImplTest {

  @Test
  void getIdToKeys_WithAllEmptyIds_ShouldReturnCompletedFutureOfCacheWithoutMakingCtpRequest() {
    // preparation
    final SphereClient ctpClient = mock(SphereClient.class);
    final ReferencesService referencesService = new ReferencesServiceImpl(ctpClient);

    // test
    final CompletionStage<Map<String, String>> idToKeysStage =
        referencesService.getIdToKeys(emptySet(), emptySet(), emptySet(), emptySet());

    // assertion
    assertThat(idToKeysStage).isCompletedWithValue(emptyMap());
    verify(ctpClient, never()).execute(any());
  }

  @Test
  void getIdToKeys_WithOnlyProductTypeIds_ShouldFetchOnceAndCacheIds() {
    // preparation
    final SphereClient ctpClient = mock(SphereClient.class);
    ResourceKeyIdGraphQlResult mockResult = mock(ResourceKeyIdGraphQlResult.class);
    ResourceKeyId resourceKeyId = new ResourceKeyId("productTypeKey", "productTypeId");
    when(mockResult.getResults()).thenReturn(singleton(resourceKeyId));
    when(ctpClient.execute(any(ResourceIdsGraphQlRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(mockResult));
    final ReferencesService referencesService = new ReferencesServiceImpl(ctpClient);

    // test
    final CompletionStage<Map<String, String>> idToKeysStage =
        referencesService.getIdToKeys(emptySet(), emptySet(), asSet("productTypeId"), emptySet());

    // assertion
    final HashMap<String, String> expectedCache = new HashMap<>();
    expectedCache.put("productTypeId", "productTypeKey");
    assertThat(idToKeysStage).isCompletedWithValue(expectedCache);
    verify(ctpClient, times(1)).execute(any(ResourceIdsGraphQlRequest.class));
  }

  @SuppressFBWarnings(
      "NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
  @Test
  void getIdToKeys_WithNullResponse_ShouldReturnCurrentCacheWithoutCachingNewIds() {
    // preparation
    final SphereClient ctpClient = mock(SphereClient.class);
    when(ctpClient.getConfig()).thenReturn(SphereApiConfig.of("test-project"));
    when(ctpClient.execute(any(ResourceIdsGraphQlRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(null));
    final ReferencesService referencesService = new ReferencesServiceImpl(ctpClient);

    // test
    final CompletionStage<Map<String, String>> idToKeysStage =
        referencesService.getIdToKeys(
            asSet("productId"), asSet("categoryId"), asSet("productTypeId"), emptySet());

    // assertion
    assertThat(idToKeysStage).isCompletedWithValue(new HashMap<>());
    verify(ctpClient, times(3)).execute(any(ResourceIdsGraphQlRequest.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  void getIdToKeys_WithNonCachedIds_ShouldFetchOnceAndCacheIds() {
    // preparation
    final SphereClient ctpClient = mock(SphereClient.class);
    ResourceKeyIdGraphQlResult mockResultProducts = mock(ResourceKeyIdGraphQlResult.class);
    ResourceKeyId productKeyId = new ResourceKeyId("productKey", "productId");
    when(mockResultProducts.getResults()).thenReturn(singleton(productKeyId));
    ResourceKeyIdGraphQlResult mockResultCategories = mock(ResourceKeyIdGraphQlResult.class);
    ResourceKeyId categoryKeyId = new ResourceKeyId("categoryKey", "categoryId");
    when(mockResultCategories.getResults()).thenReturn(singleton(categoryKeyId));
    ResourceKeyIdGraphQlResult mockResultProductTypes = mock(ResourceKeyIdGraphQlResult.class);
    ResourceKeyId productTypeKeyId = new ResourceKeyId("productTypeKey", "productTypeId");
    when(mockResultProductTypes.getResults()).thenReturn(singleton(productTypeKeyId));

    when(ctpClient.execute(any(ResourceIdsGraphQlRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(mockResultProducts))
        .thenReturn(CompletableFuture.completedFuture(mockResultCategories))
        .thenReturn(CompletableFuture.completedFuture(mockResultProductTypes));

    final CustomObject<JsonNode> mockCustomObject = mock(CustomObject.class);
    when(mockCustomObject.getId()).thenReturn("customObjectId");
    when(mockCustomObject.getKey()).thenReturn("customObjectKey");
    when(mockCustomObject.getContainer()).thenReturn("customObjectContainer");
    final PagedQueryResult<CustomObject<JsonNode>> result = mock(PagedQueryResult.class);
    when(result.getResults()).thenReturn(singletonList(mockCustomObject));

    when(result.getResults()).thenReturn(asList(mockCustomObject));
    when(ctpClient.execute(any(CustomObjectQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(result));

    final ReferencesService referencesService = new ReferencesServiceImpl(ctpClient);

    // test
    final CompletionStage<Map<String, String>> idToKeysStage =
        referencesService.getIdToKeys(
            asSet("productId"), asSet("categoryId"), asSet("productTypeId"), emptySet());
    // get again to test second fetch doesnt make request to ctp
    referencesService.getIdToKeys(
        asSet("productId"), asSet("categoryId"), asSet("productTypeId"), asSet("customObjectId"));

    // assertion
    final HashMap<String, String> expectedCache = new HashMap<>();
    expectedCache.put("productId", "productKey");
    expectedCache.put("categoryId", "categoryKey");
    expectedCache.put("productTypeId", "productTypeKey");
    expectedCache.put("customObjectId", "customObjectContainer|customObjectKey");

    assertThat(idToKeysStage).isCompletedWithValue(expectedCache);
    verify(ctpClient, times(3)).execute(any(ResourceIdsGraphQlRequest.class));
    verify(ctpClient, times(1)).execute(any(CustomObjectQuery.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  void getIdToKeys_WithSomeNonCachedIds_ShouldFetchTwiceAndCacheIds() {
    // preparation
    final SphereClient ctpClient = mock(SphereClient.class);

    ResourceKeyIdGraphQlResult mockResultProducts = mock(ResourceKeyIdGraphQlResult.class);
    ResourceKeyId productKeyId = new ResourceKeyId("productKey", "productId");
    when(mockResultProducts.getResults()).thenReturn(singleton(productKeyId));
    ResourceKeyIdGraphQlResult mockResultCategories = mock(ResourceKeyIdGraphQlResult.class);
    ResourceKeyId categoryKeyId = new ResourceKeyId("categoryKey", "categoryId");
    when(mockResultCategories.getResults()).thenReturn(singleton(categoryKeyId));
    ResourceKeyIdGraphQlResult mockResultProductTypes = mock(ResourceKeyIdGraphQlResult.class);
    ResourceKeyId productTypeKeyId = new ResourceKeyId("productTypeKey", "productTypeId");
    when(mockResultProductTypes.getResults()).thenReturn(singleton(productTypeKeyId));

    when(ctpClient.execute(any(ResourceIdsGraphQlRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(mockResultProducts))
        .thenReturn(CompletableFuture.completedFuture(mockResultCategories))
        .thenReturn(CompletableFuture.completedFuture(mockResultProductTypes));

    final CustomObject<JsonNode> mockCustomObject = mock(CustomObject.class);
    when(mockCustomObject.getId()).thenReturn("customObjectId");
    when(mockCustomObject.getKey()).thenReturn("customObjectKey");
    when(mockCustomObject.getContainer()).thenReturn("customObjectContainer");
    final PagedQueryResult<CustomObject<JsonNode>> result = mock(PagedQueryResult.class);
    when(result.getResults()).thenReturn(singletonList(mockCustomObject));

    final CustomObject<JsonNode> mockCustomObject2 = mock(CustomObject.class);
    when(mockCustomObject2.getId()).thenReturn("customObjectId2");
    when(mockCustomObject2.getKey()).thenReturn("customObjectKey2");
    when(mockCustomObject2.getContainer()).thenReturn("customObjectContainer2");

    when(result.getResults()).thenReturn(asList(mockCustomObject, mockCustomObject2));
    when(ctpClient.execute(any(CustomObjectQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(result));

    final ReferencesService referencesService = new ReferencesServiceImpl(ctpClient);

    // test
    final CompletionStage<Map<String, String>> idToKeysStage =
        referencesService.getIdToKeys(
            asSet("productId"),
            asSet("categoryId"),
            asSet("productTypeId"),
            asSet("customObjectId", "customObjectId2"));

    // get again to test second fetch doesnt make request to ctp
    referencesService.getIdToKeys(
        asSet("newProductId"),
        asSet("categoryId"),
        asSet("productTypeId"),
        asSet("customObjectId", "customObjectId2"));

    // assertion
    final HashMap<String, String> expectedCache = new HashMap<>();
    expectedCache.put("productId", "productKey");
    expectedCache.put("categoryId", "categoryKey");
    expectedCache.put("productTypeId", "productTypeKey");
    expectedCache.put("customObjectId", "customObjectContainer|customObjectKey");
    expectedCache.put("customObjectId2", "customObjectContainer2|customObjectKey2");
    assertThat(idToKeysStage).isCompletedWithValue(expectedCache);

    //  second fetch doesnt make request to ctp
    verify(ctpClient, times(4)).execute(any(ResourceIdsGraphQlRequest.class));
    verify(ctpClient, times(1)).execute(any(CustomObjectQuery.class));
  }

  @Test
  void getIdToKeys_WithSomeBlankKeysAndNonExistentIds_ShouldCacheNonBlankKeys() {
    // preparation
    final SphereClient ctpClient = mock(SphereClient.class);
    when(ctpClient.getConfig()).thenReturn(SphereApiConfig.of("test-project"));

    ResourceKeyIdGraphQlResult mockResultProducts = mock(ResourceKeyIdGraphQlResult.class);
    ResourceKeyId productKeyId1 = new ResourceKeyId("productKey", "productId");
    ResourceKeyId productKeyId2 = new ResourceKeyId("", "productId2");
    when(mockResultProducts.getResults()).thenReturn(asSet(productKeyId1, productKeyId2));
    ResourceKeyIdGraphQlResult mockResultCategories = mock(ResourceKeyIdGraphQlResult.class);
    ResourceKeyId categoryKeyId = new ResourceKeyId("", "categoryId");
    when(mockResultCategories.getResults()).thenReturn(singleton(categoryKeyId));
    ResourceKeyIdGraphQlResult mockResultProductTypes = mock(ResourceKeyIdGraphQlResult.class);
    ResourceKeyId productTypeKeyId = new ResourceKeyId("", "productTypeId");
    when(mockResultProductTypes.getResults()).thenReturn(singleton(productTypeKeyId));

    when(ctpClient.execute(any(ResourceIdsGraphQlRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(mockResultProducts))
        .thenReturn(CompletableFuture.completedFuture(mockResultCategories))
        .thenReturn(CompletableFuture.completedFuture(mockResultProductTypes));
    final ReferencesService referencesService = new ReferencesServiceImpl(ctpClient);

    // test
    final CompletionStage<Map<String, String>> idToKeysStage =
        referencesService.getIdToKeys(
            asSet("productId", "productId2", "productId3"),
            asSet("categoryId"),
            asSet("productTypeId"),
            emptySet());

    // assertion
    final HashMap<String, String> expectedCache = new HashMap<>();
    expectedCache.put("productId", "productKey");
    assertThat(idToKeysStage).isCompletedWithValue(expectedCache);
    verify(ctpClient, times(3)).execute(any(ResourceIdsGraphQlRequest.class));
  }
}
