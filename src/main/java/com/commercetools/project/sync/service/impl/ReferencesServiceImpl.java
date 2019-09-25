package com.commercetools.project.sync.service.impl;

import com.commercetools.project.sync.model.request.CombinedResourceKeysRequest;
import com.commercetools.project.sync.model.response.CombinedResult;
import com.commercetools.project.sync.model.response.ResultingResourcesContainer;
import com.commercetools.project.sync.service.ReferencesService;
import io.sphere.sdk.client.SphereClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class ReferencesServiceImpl extends BaseServiceImpl implements ReferencesService {
  private final Map<String, String> idToKey = new HashMap<>();
  private static final Logger LOGGER = LoggerFactory.getLogger(ReferencesServiceImpl.class);

  public ReferencesServiceImpl(@Nonnull final SphereClient ctpClient) {
    super(ctpClient);
  }

  /**
   * Given 3 {@link Set}s of ids of products, categories and productTypes, this method first checks
   * if there is a key mapping for each id in the {@code idToKey} cache. If there exists a mapping
   * for all the ids, the method returns a future containing the existing {@code idToKey} cache as
   * it is. If there is at least one missing mapping, it attempts to make a GraphQL request to CTP
   * to fetch all ids and keys of every missing product, category or productType Id in a combined request. For each
   * fetched key/id pair, the method will insert it into the {@code idToKey} cache and then return
   * the cache in a {@link CompletableFuture} after the request is successful.
   *
   * @param productIds the product ids to find a key mapping for.
   * @param categoryIds the category ids to find a key mapping for.
   * @param productTypeIds the productType ids to find a key mapping for.
   * @return a map of id to key representing products, categories and productTypes in the CTP
   *     project defined by the injected {@code ctpClient}.
   */
  @Nonnull
  @Override
  public CompletionStage<Map<String, String>> getIdToKeys(
      @Nonnull final Set<String> productIds,
      @Nonnull final Set<String> categoryIds,
      @Nonnull final Set<String> productTypeIds) {

    final Set<String> nonCachedProductIds = getNonCachedIds(productIds);
    final Set<String> nonCachedCategoryIds = getNonCachedIds(categoryIds);
    final Set<String> nonCachedProductTypeIds = getNonCachedIds(productTypeIds);

    // if everything is cached, no need to make a request to CTP.
    if (nonCachedProductIds.isEmpty()
        && nonCachedCategoryIds.isEmpty()
        && nonCachedProductTypeIds.isEmpty()) {
      return CompletableFuture.completedFuture(idToKey);
    }

    // TODO: Make sure each nonCached set has less than 500 resources, otherwise batch requests
    // https://github.com/commercetools/commercetools-project-sync/issues/42
    final CombinedResourceKeysRequest combinedResourceKeysRequest =
        new CombinedResourceKeysRequest(
            nonCachedProductIds, nonCachedCategoryIds, nonCachedProductTypeIds);

    return getCtpClient()
        .execute(combinedResourceKeysRequest)
        .thenApply(
            combinedResult -> {
              cacheKeys(combinedResult);
              return idToKey;
            });
  }

  @Nonnull
  private Set<String> getNonCachedIds(@Nonnull final Set<String> ids) {
    return ids.stream().filter(id -> !idToKey.containsKey(id)).collect(Collectors.toSet());
  }

  private void cacheKeys(@Nonnull final CombinedResult combinedResult) {
    cacheCategoryKeys(combinedResult);
    cacheProductKeys(combinedResult);
    cacheProductTypeKeys(combinedResult);
  }

  private void cacheProductTypeKeys(@Nonnull final CombinedResult combinedResult) {
    final ResultingResourcesContainer productTypeResults = combinedResult.getProductTypes();
    if (productTypeResults != null) {
      productTypeResults
          .getResults()
          .forEach(
              referenceIdKey -> {
                final String key = referenceIdKey.getKey();
                final String id = referenceIdKey.getId();
                if (isBlank(key)) {
                  LOGGER.error(
                      format(
                          "The key for the productTypes with id '%s' is blank. Please make sure all"
                              + " productTypes, in the source project with key '%s', have non-blank"
                              + "(i.e. non-null and non-empty) keys.",
                          id, getCtpClient().getConfig().getProjectKey()));
                } else {
                  idToKey.put(id, key);
                }
              });
    }
  }

  private void cacheProductKeys(@Nonnull final CombinedResult combinedResult) {
    final ResultingResourcesContainer productResults = combinedResult.getProducts();
    if (productResults != null) {
      productResults
          .getResults()
          .forEach(
              referenceIdKey -> {
                final String key = referenceIdKey.getKey();
                final String id = referenceIdKey.getId();
                if (isBlank(key)) {
                  LOGGER.error(
                      format(
                          "The key for the product with id '%s' is blank. Please make sure all "
                              + "products, in the source project with key '%s', have non-blank"
                              + "(i.e. non-null and non-empty) keys.",
                          id, getCtpClient().getConfig().getProjectKey()));
                } else {
                  idToKey.put(id, key);
                }
              });
    }
  }

  private void cacheCategoryKeys(@Nonnull final CombinedResult combinedResult) {
    final ResultingResourcesContainer categoryResults = combinedResult.getCategories();
    if (categoryResults != null) {
      categoryResults
          .getResults()
          .forEach(
              referenceIdKey -> {
                final String key = referenceIdKey.getKey();
                final String id = referenceIdKey.getId();
                if (isBlank(key)) {
                  LOGGER.error(
                      format(
                          "The key for the category with id '%s' is blank. Please make sure all "
                              + "categories, in the source project with key '%s', have non-blank"
                              + "(i.e. non-null and non-empty) keys.",
                          id, getCtpClient().getConfig().getProjectKey()));
                } else {
                  idToKey.put(id, key);
                }
              });
    }
  }
}
