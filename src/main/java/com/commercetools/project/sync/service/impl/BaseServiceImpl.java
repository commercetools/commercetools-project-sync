package com.commercetools.project.sync.service.impl;

import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.project.sync.model.ResourceIdsGraphQlRequest;
import com.commercetools.sync.commons.models.GraphQlQueryResources;
import com.commercetools.sync.commons.models.ResourceKeyId;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.WithKey;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;

public class BaseServiceImpl {
  private final SphereClient ctpClient;
  protected final Cache<String, String> referenceIdToKeyCache;

  // TODO: This can be static instead of an instance-based cache.
  // Assume the CategoryReferenceTransformServiceImpl has cached almost all categories ids to keys,
  // and then ProductReferenceTransformServiceImpl will be initialized with a new cache which will
  // fetch all referenced category keys again.
  private long cacheSize = 10_000;

  protected BaseServiceImpl(@Nonnull final SphereClient ctpClient) {
    this.ctpClient = ctpClient;
    this.referenceIdToKeyCache =
        Caffeine.newBuilder().maximumSize(cacheSize).executor(Runnable::run).build();
  }

  protected SphereClient getCtpClient() {
    return ctpClient;
  }

  protected <T extends WithKey> CompletionStage<List<T>> fetchAndFillReferenceIdToKeyCache(
      @Nonnull final List<T> resourceList,
      final Set<String> ids,
      final GraphQlQueryResources requestType) {
    final Set<String> nonCachedReferenceIds = getNonCachedReferenceIds(ids);
    if (nonCachedReferenceIds.isEmpty()) {
      return CompletableFuture.completedFuture(resourceList);
    }

    return getCtpClient()
        .execute(new ResourceIdsGraphQlRequest(nonCachedReferenceIds, requestType))
        .toCompletableFuture()
        .thenApply(
            results -> {
              cacheProductTypeKeys(results.getResults());
              return resourceList;
            });
  }

  @Nonnull
  private Set<String> getNonCachedReferenceIds(@Nonnull final Set<String> referenceIds) {
    return referenceIds
        .stream()
        .filter(id -> null == referenceIdToKeyCache.getIfPresent(id))
        .collect(toSet());
  }

  private void cacheProductTypeKeys(final Set<ResourceKeyId> results) {
    results.forEach(
        resourceKeyId -> {
          final String key = resourceKeyId.getKey();
          final String id = resourceKeyId.getId();
          if (!isBlank(key)) {
            referenceIdToKeyCache.put(id, key);
          }
        });
  }
}
