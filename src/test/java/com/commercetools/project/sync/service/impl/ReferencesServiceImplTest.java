package com.commercetools.project.sync.service.impl;

import static io.sphere.sdk.utils.SphereInternalUtils.asSet;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
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
        referencesService.getIdToKeys(emptySet(), emptySet(), asSet("productTypeId"), emptySet());

    // assertion
    final HashMap<String, String> expectedCache = new HashMap<>();
    expectedCache.put("productTypeId", "productTypeKey");
    assertThat(idToKeysStage).isCompletedWithValue(expectedCache);
    verify(ctpClient, times(1)).execute(any(CombinedResourceKeysRequest.class));
  }

  @SuppressFBWarnings(
      "NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
  @Test
  void getIdToKeys_WithNullResponse_ShouldReturnCurrentCacheWithoutCachingNewIds() {
    // preparation
    final SphereClient ctpClient = mock(SphereClient.class);
    when(ctpClient.getConfig()).thenReturn(SphereApiConfig.of("test-project"));
    when(ctpClient.execute(any(CombinedResourceKeysRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(null));
    final ReferencesService referencesService = new ReferencesServiceImpl(ctpClient);

    // test
    final CompletionStage<Map<String, String>> idToKeysStage =
        referencesService.getIdToKeys(
            asSet("productId"), asSet("categoryId"), asSet("productTypeId"), emptySet());

    // assertion
    assertThat(idToKeysStage).isCompletedWithValue(new HashMap<>());
    verify(ctpClient, times(1)).execute(any(CombinedResourceKeysRequest.class));
  }

  @Test
  @SuppressWarnings("unchecked")
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
    verify(ctpClient, times(1)).execute(any(CombinedResourceKeysRequest.class));
    verify(ctpClient, times(1)).execute(any(CustomObjectQuery.class));
  }

  @Test
  @SuppressWarnings("unchecked")
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
    verify(ctpClient, times(2)).execute(any(CombinedResourceKeysRequest.class));
    verify(ctpClient, times(1)).execute(any(CustomObjectQuery.class));
  }

  @Test
  void getIdToKeys_WithSomeBlankKeysAndNonExistentIds_ShouldCacheNonBlankKeys() {
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
            asSet("productId", "productId2", "productId3"),
            asSet("categoryId"),
            asSet("productTypeId"),
            emptySet());

    // assertion
    final HashMap<String, String> expectedCache = new HashMap<>();
    expectedCache.put("productId", "productKey");
    assertThat(idToKeysStage).isCompletedWithValue(expectedCache);
    verify(ctpClient, times(1)).execute(any(CombinedResourceKeysRequest.class));
  }
}
