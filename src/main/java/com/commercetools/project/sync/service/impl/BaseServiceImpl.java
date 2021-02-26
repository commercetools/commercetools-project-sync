package com.commercetools.project.sync.service.impl;

import com.commercetools.project.sync.model.ResourceIdsGraphQlRequest;
import com.commercetools.sync.commons.models.GraphQlQueryResources;
import com.commercetools.sync.commons.models.ResourceKeyId;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.WithKey;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class BaseServiceImpl {
  private final SphereClient ctpClient;
  private static long cacheSize = 10_000;
  protected static final Cache<String, String> referenceIdToKeyCache = initializeCache();

  protected BaseServiceImpl(@Nonnull final SphereClient ctpClient) {
    this.ctpClient = ctpClient;
  }

  protected SphereClient getCtpClient() {
    return ctpClient;
  }

  @Nonnull
  private static Cache<String, String> initializeCache() {
    return Caffeine.newBuilder().maximumSize(cacheSize).executor(Runnable::run).build();
  }

  protected <T extends WithKey> CompletableFuture<List<T>> fetchAndFillReferenceIdToKeyCache(
      @Nonnull final List<T> resourceList,
      @Nonnull final Set<String> ids,
      @Nonnull final GraphQlQueryResources requestType) {
    final Set<String> nonCachedReferenceIds = getNonCachedReferenceIds(ids);
    if (nonCachedReferenceIds.isEmpty()) {
      return CompletableFuture.completedFuture(resourceList);
    }

    return getCtpClient()
        .execute(new ResourceIdsGraphQlRequest(nonCachedReferenceIds, requestType))
        .toCompletableFuture()
        .thenApply(
            results -> {
              cacheResourceReferenceKeys(results.getResults());
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

  private void cacheResourceReferenceKeys(final Set<ResourceKeyId> results) {
    Optional.ofNullable(results)
        .orElseGet(Collections::emptySet)
        .stream()
        .forEach(
            resourceKeyId -> {
              final String key = resourceKeyId.getKey();
              final String id = resourceKeyId.getId();
              if (!isBlank(key)) {
                referenceIdToKeyCache.put(id, key);
              }
            });
  }
}
