package com.commercetools.project.sync.product.service;

import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;

public interface ProductReferenceTransformService {

  @Nonnull
  CompletionStage<List<ProductDraft>> transformProductReferences(@Nonnull List<Product> products);

  @Nonnull
  CompletionStage<Map<String, String>> getIdToKeys(
      @Nonnull final Set<String> productIds,
      @Nonnull final Set<String> categoryIds,
      @Nonnull final Set<String> productTypeIds,
      @Nonnull final Set<String> customObjectIds);
}
