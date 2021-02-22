package com.commercetools.project.sync.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.sphere.sdk.client.SphereClient;
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
}
