package com.commercetools.project.sync.service.impl;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.project.sync.model.ResourceIdsGraphQlRequest;
import com.commercetools.sync.commons.models.GraphQlQueryResources;
import com.commercetools.sync.commons.models.ResourceKeyId;
import com.commercetools.sync.commons.utils.ChunkUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.sphere.sdk.client.SphereClient;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public class BaseTransformServiceImpl {
  /*
   * An id is a 36 characters long string. (i.e: 53c4a8b4-754f-4b95-b6f2-3e1e70e3d0c3) We
   * chunk them in 300 ids, we will have a query around 11.000 characters. Above this size it
   * could return - Error 413 (Request Entity Too Large)
   */
  public static final int CHUNK_SIZE = 300;
  protected static final Cache<String, String> referenceIdToKeyCache = initializeCache();

  private final SphereClient ctpClient;

  protected BaseTransformServiceImpl(@Nonnull final SphereClient ctpClient) {
    this.ctpClient = ctpClient;
  }

  protected SphereClient getCtpClient() {
    return ctpClient;
  }

  @Nonnull
  private static Cache<String, String> initializeCache() {
    return Caffeine.newBuilder().maximumSize(100_000).executor(Runnable::run).build();
  }

  protected CompletableFuture<Void> fetchAndFillReferenceIdToKeyCache(
      @Nonnull final Set<String> ids, @Nonnull final GraphQlQueryResources requestType) {

    final Set<String> nonCachedReferenceIds = getNonCachedReferenceIds(ids);
    if (nonCachedReferenceIds.isEmpty()) {
      return null;
    }

    final List<List<String>> chunkedIds = ChunkUtils.chunk(nonCachedReferenceIds, CHUNK_SIZE);

    List<ResourceIdsGraphQlRequest> collectedRequests =
        createResourceIdsGraphQlRequests(chunkedIds, requestType);

    return ChunkUtils.executeChunks(getCtpClient(), collectedRequests)
        .thenApply(ChunkUtils::flattenGraphQLBaseResults)
        .thenApply(
            results -> {
              cacheResourceReferenceKeys(results);
              return null;
            });
  }

  @Nonnull
  protected List<ResourceIdsGraphQlRequest> createResourceIdsGraphQlRequests(
      @Nonnull final List<List<String>> chunkedIds,
      @Nonnull final GraphQlQueryResources resourceType) {
    return chunkedIds
        .stream()
        .map(chunk -> new ResourceIdsGraphQlRequest(new HashSet<>(chunk), resourceType))
        .collect(toList());
  }

  @Nonnull
  protected Set<String> getNonCachedReferenceIds(@Nonnull final Set<String> referenceIds) {
    return referenceIds
        .stream()
        .filter(id -> null == referenceIdToKeyCache.getIfPresent(id))
        .collect(toSet());
  }

  protected void cacheResourceReferenceKeys(final Set<ResourceKeyId> results) {
    Optional.ofNullable(results)
        .orElseGet(Collections::emptySet)
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
