package com.commercetools.project.sync.product.service.impl;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.project.sync.model.ResourceIdsGraphQlRequest;
import com.commercetools.project.sync.product.service.ProductReferenceTransformService;
import com.commercetools.project.sync.service.impl.BaseServiceImpl;
import com.commercetools.project.sync.util.referenceresolution.ProductReferenceResolutionUtils;
import com.commercetools.sync.commons.models.GraphQlQueryResources;
import com.commercetools.sync.commons.models.ResourceKeyId;
import com.commercetools.sync.commons.utils.ChunkUtils;
import com.commercetools.sync.customobjects.helpers.CustomObjectCompositeIdentifier;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductLike;
import io.sphere.sdk.products.ProductVariant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public class ProductReferenceTransformServiceImpl extends BaseServiceImpl
    implements ProductReferenceTransformService {
  private final Map<String, String> allResourcesIdToKey = new HashMap<>();
  /*
   * An id is a 36 characters long string. (i.e: 53c4a8b4-754f-4b95-b6f2-3e1e70e3d0c3) We
   * chunk them in 300 ids, we will have a query around 11.000 characters. Above this size it
   * could return - Error 413 (Request Entity Too Large)
   */
  public static final int CHUNK_SIZE = 300;

  public ProductReferenceTransformServiceImpl(@Nonnull final SphereClient ctpClient) {
    super(ctpClient);
  }

  @Nonnull
  @Override
  public CompletableFuture<List<ProductDraft>> transformProductReferences(
      @Nonnull final List<Product> products) {

    final List<CompletableFuture<List<Product>>> transformReferencesToRunParallel =
        new ArrayList<>();
    transformReferencesToRunParallel.add(
        this.transformProductTypeReference(products).toCompletableFuture());
    transformReferencesToRunParallel.add(
        this.transformTaxCategoryReference(products).toCompletableFuture());
    transformReferencesToRunParallel.add(
        this.transformStateReference(products).toCompletableFuture());
    transformReferencesToRunParallel.add(
        this.transformCategoryReference(products).toCompletableFuture());
    transformReferencesToRunParallel.add(
        this.transformPricesChannelReference(products).toCompletableFuture());
    transformReferencesToRunParallel.add(
        this.transformMasterVariantPricesCustomTypeReference(products).toCompletableFuture());
    transformReferencesToRunParallel.add(
        this.transformMasterVariantAssetsCustomTypeReference(products).toCompletableFuture());
    transformReferencesToRunParallel.add(
        this.transformPricesCustomTypeReference(products).toCompletableFuture());
    transformReferencesToRunParallel.add(
        this.transformAssetsCustomTypeReference(products).toCompletableFuture());
    transformReferencesToRunParallel.add(
        this.transformPricesCustomerGroupReference(products).toCompletableFuture());

    return CompletableFuture.allOf(
            transformReferencesToRunParallel.toArray(new CompletableFuture[0]))
        .thenApply(
            ignore ->
                ProductReferenceResolutionUtils.mapToProductDrafts(
                    products, referenceIdToKeyCache.asMap()));
  }

  @Nonnull
  private CompletionStage<List<Product>> transformProductTypeReference(
      @Nonnull final List<Product> products) {

    Set<String> productTypeIds =
        products.stream().map(ProductLike::getProductType).map(Reference::getId).collect(toSet());

    return fetchAndFillReferenceIdToKeyCache(
        products, productTypeIds, GraphQlQueryResources.PRODUCT_TYPES);
  }

  @Nonnull
  private CompletionStage<List<Product>> transformTaxCategoryReference(
      @Nonnull final List<Product> products) {

    Set<String> taxCategoryIds =
        products
            .stream()
            .map(ProductLike::getTaxCategory)
            .filter(Objects::nonNull)
            .map(Reference::getId)
            .collect(toSet());

    return fetchAndFillReferenceIdToKeyCache(
        products, taxCategoryIds, GraphQlQueryResources.TAX_CATEGORIES);
  }

  @Nonnull
  private CompletionStage<List<Product>> transformStateReference(
      @Nonnull final List<Product> products) {

    Set<String> stateIds =
        products
            .stream()
            .map(Product::getState)
            .filter(Objects::nonNull)
            .map(Reference::getId)
            .collect(toSet());

    return fetchAndFillReferenceIdToKeyCache(products, stateIds, GraphQlQueryResources.STATES);
  }

  @Nonnull
  private CompletionStage<List<Product>> transformCategoryReference(
      @Nonnull final List<Product> products) {

    Set<String> categoryIds =
        products
            .stream()
            .map(product -> product.getMasterData().getStaged().getCategories())
            .filter(Objects::nonNull)
            .map(
                categories ->
                    categories.stream().map(Reference::getId).collect(Collectors.toList()))
            .flatMap(Collection::stream)
            .collect(toSet());

    return fetchAndFillReferenceIdToKeyCache(products, categoryIds, GraphQlQueryResources.CATEGORIES);
  }

  @Nonnull
  private CompletionStage<List<Product>> transformPricesChannelReference(
      @Nonnull final List<Product> products) {

    Set<String> channelIds =
        products
            .stream()
            .map(product -> product.getMasterData().getStaged().getAllVariants())
            .map(
                productVariants ->
                    productVariants
                        .stream()
                        .filter(Objects::nonNull)
                        .map(
                            productVariant ->
                                productVariant
                                    .getPrices()
                                    .stream()
                                    .map(Price::getChannel)
                                    .filter(Objects::nonNull)
                                    .map(Reference::getId)
                                    .collect(toList()))
                        .flatMap(Collection::stream)
                        .collect(toList()))
            .flatMap(Collection::stream)
            .collect(toSet());

    return fetchAndFillReferenceIdToKeyCache(products, channelIds, GraphQlQueryResources.CHANNELS);
  }

  @Nonnull
  private CompletionStage<List<Product>> transformAssetsCustomTypeReference(
      @Nonnull final List<Product> products) {

    Set<String> typeIds =
        products
            .stream()
            .map(product -> product.getMasterData().getStaged().getAllVariants())
            .map(
                productVariants ->
                    productVariants
                        .stream()
                        .filter(Objects::nonNull)
                        .map(
                            productVariant ->
                                productVariant
                                    .getAssets()
                                    .stream()
                                    .map(asset -> asset.getCustom().getType())
                                    .filter(Objects::nonNull)
                                    .map(Reference::getId)
                                    .collect(toList()))
                        .flatMap(Collection::stream)
                        .collect(toList()))
            .flatMap(Collection::stream)
            .collect(toSet());

    return fetchAndFillReferenceIdToKeyCache(products, typeIds, GraphQlQueryResources.TYPES);
  }

  @Nonnull
  private CompletionStage<List<Product>> transformPricesCustomTypeReference(
      @Nonnull final List<Product> products) {

    Set<String> typeIds =
        products
            .stream()
            .map(product -> product.getMasterData().getStaged().getAllVariants())
            .map(
                productVariants ->
                    productVariants
                        .stream()
                        .filter(Objects::nonNull)
                        .map(
                            productVariant ->
                                productVariant
                                    .getPrices()
                                    .stream()
                                    .map(asset -> asset.getCustom().getType())
                                    .filter(Objects::nonNull)
                                    .map(Reference::getId)
                                    .collect(toList()))
                        .flatMap(Collection::stream)
                        .collect(toList()))
            .flatMap(Collection::stream)
            .collect(toSet());

    return fetchAndFillReferenceIdToKeyCache(products, typeIds, GraphQlQueryResources.TYPES);
  }

  @Nonnull
  private CompletionStage<List<Product>> transformMasterVariantPricesCustomTypeReference(
      @Nonnull final List<Product> products) {

    Set<String> typeIds =
        products
            .stream()
            .map(product -> product.getMasterData().getStaged().getMasterVariant())
            .filter(Objects::nonNull)
            .map(ProductVariant::getPrices)
            .filter(Objects::nonNull)
            .map(
                prices ->
                    prices
                        .stream()
                        .map(price -> price.getCustom())
                        .filter(Objects::nonNull)
                        .map(customType -> customType.getType().getId())
                        .collect(Collectors.toList()))
            .flatMap(Collection::stream)
            .collect(toSet());

    return fetchAndFillReferenceIdToKeyCache(products, typeIds, GraphQlQueryResources.TYPES);
  }

  @Nonnull
  private CompletionStage<List<Product>> transformMasterVariantAssetsCustomTypeReference(
      @Nonnull final List<Product> products) {

    Set<String> customTypeIds =
        products
            .stream()
            .map(product -> product.getMasterData().getStaged().getMasterVariant())
            .filter(Objects::nonNull)
            .map(ProductVariant::getAssets)
            .filter(Objects::nonNull)
            .map(
                prices ->
                    prices
                        .stream()
                        .map(asset -> asset.getCustom())
                        .filter(Objects::nonNull)
                        .map(customType -> customType.getType().getId())
                        .collect(Collectors.toList()))
            .flatMap(Collection::stream)
            .collect(toSet());

    return fetchAndFillReferenceIdToKeyCache(products, customTypeIds, GraphQlQueryResources.TYPES);
  }

  @Nonnull
  private CompletionStage<List<Product>> transformPricesCustomerGroupReference(
      @Nonnull final List<Product> products) {

    Set<String> customerGroupIds =
        products
            .stream()
            .map(product -> product.getMasterData().getStaged().getAllVariants())
            .map(
                productVariants ->
                    productVariants
                        .stream()
                        .filter(Objects::nonNull)
                        .map(
                            productVariant ->
                                productVariant
                                    .getPrices()
                                    .stream()
                                    .map(price -> price.getCustomerGroup())
                                    .filter(Objects::nonNull)
                                    .map(Reference::getId)
                                    .collect(toList()))
                        .flatMap(Collection::stream)
                        .collect(toList()))
            .flatMap(Collection::stream)
            .collect(toSet());

    return fetchAndFillReferenceIdToKeyCache(
        products, customerGroupIds, GraphQlQueryResources.CUSTOMER_GROUPS);
  }

  private CompletionStage<List<Product>> fetchAndFillReferenceIdToKeyCache(
      @Nonnull final List<Product> products,
      final Set<String> ids,
      final GraphQlQueryResources requestType) {
    final Set<String> nonCachedReferenceIds = getNonCachedReferenceIds(ids);
    if (nonCachedReferenceIds.isEmpty()) {
      return CompletableFuture.completedFuture(products);
    }

    return getCtpClient()
        .execute(new ResourceIdsGraphQlRequest(nonCachedReferenceIds, requestType))
        .toCompletableFuture()
        .thenApply(
            results -> {
              cacheProductTypeKeys(results.getResults());
              return products;
            });
  }

  @Nonnull
  @Override
  public CompletionStage<Map<String, String>> getIdToKeys(
      @Nonnull final Set<String> productIds,
      @Nonnull final Set<String> categoryIds,
      @Nonnull final Set<String> productTypeIds,
      @Nonnull final Set<String> customObjectIds) {

    final Set<String> nonCachedProductIds = getNonCachedIds(productIds);
    final Set<String> nonCachedCategoryIds = getNonCachedIds(categoryIds);
    final Set<String> nonCachedProductTypeIds = getNonCachedIds(productTypeIds);
    final Set<String> nonCachedCustomObjectIds = getNonCachedIds(customObjectIds);

    if (nonCachedProductIds.isEmpty()
        && nonCachedCategoryIds.isEmpty()
        && nonCachedProductTypeIds.isEmpty()) {
      return fetchCustomObjectKeys(customObjectIds);
    }

    List<List<String>> productIdsChunk = ChunkUtils.chunk(nonCachedProductIds, CHUNK_SIZE);
    List<List<String>> categoryIdsChunk = ChunkUtils.chunk(nonCachedCategoryIds, CHUNK_SIZE);
    List<List<String>> productTypeIdsChunk = ChunkUtils.chunk(nonCachedProductTypeIds, CHUNK_SIZE);

    List<ResourceIdsGraphQlRequest> collectedRequests = new ArrayList<>();

    collectedRequests.addAll(
        createResourceIdsGraphQlRequests(productIdsChunk, GraphQlQueryResources.PRODUCTS));
    collectedRequests.addAll(
        createResourceIdsGraphQlRequests(categoryIdsChunk, GraphQlQueryResources.CATEGORIES));
    collectedRequests.addAll(
        createResourceIdsGraphQlRequests(productTypeIdsChunk, GraphQlQueryResources.PRODUCT_TYPES));

    // TODO: Adapt to run grapqhl and rest call in parallel.
    return ChunkUtils.executeChunks(getCtpClient(), collectedRequests)
        .thenApply(ChunkUtils::flattenGraphQLBaseResults)
        .thenApply(
            results -> {
              cacheKeys(results);
              return allResourcesIdToKey;
            })
        .thenCompose(ignored -> fetchCustomObjectKeys(nonCachedCustomObjectIds));
  }

  @Nonnull
  private List<ResourceIdsGraphQlRequest> createResourceIdsGraphQlRequests(
      @Nonnull final List<List<String>> chunkedIds,
      @Nonnull final GraphQlQueryResources resourceType) {
    return chunkedIds
        .stream()
        .map(chunk -> new ResourceIdsGraphQlRequest(new HashSet<>(chunk), resourceType))
        .collect(toList());
  }

  @Nonnull
  private Set<String> getNonCachedIds(@Nonnull final Set<String> ids) {
    return ids.stream().filter(id -> !allResourcesIdToKey.containsKey(id)).collect(toSet());
  }

  @Nonnull
  private Set<String> getNonCachedReferenceIds(@Nonnull final Set<String> referenceIds) {
    return referenceIds
        .stream()
        .filter(id -> null == referenceIdToKeyCache.getIfPresent(id))
        .collect(toSet());
  }

  @Nonnull
  private CompletionStage<Map<String, String>> fetchCustomObjectKeys(
      @Nonnull final Set<String> nonCachedCustomObjectIds) {

    if (nonCachedCustomObjectIds.isEmpty()) {
      return CompletableFuture.completedFuture(allResourcesIdToKey);
    }

    final List<List<String>> chunkedIds = ChunkUtils.chunk(nonCachedCustomObjectIds, CHUNK_SIZE);

    final List<CustomObjectQuery<JsonNode>> chunkedRequests =
        chunkedIds
            .stream()
            .map(ids -> CustomObjectQuery.ofJsonNode().plusPredicates(p -> p.id().isIn(ids)))
            .collect(toList());

    return ChunkUtils.executeChunks(getCtpClient(), chunkedRequests)
        .thenApply(ChunkUtils::flattenPagedQueryResults)
        .thenApply(
            customObjects -> {
              customObjects
                  .stream()
                  .forEach(
                      customObject -> {
                        allResourcesIdToKey.put(
                            customObject.getId(),
                            CustomObjectCompositeIdentifier.of(customObject).toString());
                      });
              return allResourcesIdToKey;
            });
  }

  private void cacheKeys(final Set<ResourceKeyId> results) {
    results.forEach(
        resourceKeyId -> {
          final String key = resourceKeyId.getKey();
          final String id = resourceKeyId.getId();
          if (!isBlank(key)) {
            allResourcesIdToKey.put(id, key);
          }
        });
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
