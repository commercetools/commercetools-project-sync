package com.commercetools.project.sync.product.service;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.states.State;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.types.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;

public interface ProductReferenceTransformService {

  @Nonnull
  CompletableFuture<List<ProductDraft>> transformProductReferences(@Nonnull List<Product> products);

  /**
   * Given 4 {@link Set}s of ids of products, categories, productTypes and custom objects, this
   * method first checks if there is a key mapping for each id in the {@code idToKey} cache. If
   * there exists a mapping for all the ids, the method returns a future containing the existing
   * {@code idToKey} cache as it is. If there is at least one missing mapping, it attempts to make a
   * GraphQL request (note: rest request for custom objects) to CTP to fetch all ids and keys of
   * every missing product, category, productType or custom object Id in a combined request. For
   * each fetched key/id pair, the method will insert it into the {@code idToKey} cache and then
   * return the cache in a {@link CompletableFuture} after the request is successful.
   *
   * @param productIds the product ids to find a key mapping for.
   * @param categoryIds the category ids to find a key mapping for.
   * @param productTypeIds the productType ids to find a key mapping for.
   * @param customObjectIds the custom object ids to find a key mapping for.
   * @return a map of id to key representing products, categories, productTypes and customObjects in
   *     the CTP project defined by the injected {@code ctpClient}.
   */
  @Nonnull
  CompletionStage<Map<String, String>> getIdToKeys(
      @Nonnull final Set<String> productIds,
      @Nonnull final Set<String> categoryIds,
      @Nonnull final Set<String> productTypeIds,
      @Nonnull final Set<String> customObjectIds);
}
