package com.commercetools.project.sync.service.impl;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.project.sync.model.request.CombinedResourceKeysRequest;
import com.commercetools.project.sync.model.response.CombinedResult;
import com.commercetools.project.sync.model.response.ResultingResourcesContainer;
import com.commercetools.project.sync.service.ReferencesService;
import com.commercetools.sync.commons.utils.CtpQueryUtils;
import com.commercetools.sync.customobjects.helpers.CustomObjectCompositeIdentifier;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.queries.QueryPredicate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

public class ReferencesServiceImpl extends BaseServiceImpl implements ReferencesService {
  private final Map<String, String> idToKey = new HashMap<>();

  public ReferencesServiceImpl(@Nonnull final SphereClient ctpClient) {
    super(ctpClient);
  }

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
            })
        .thenCompose(ignored -> fetchCustomObjectKeys(nonCachedCustomObjectIds));
  }

  @Nonnull
  private Set<String> getNonCachedIds(@Nonnull final Set<String> ids) {
    return ids.stream().filter(id -> !idToKey.containsKey(id)).collect(Collectors.toSet());
  }

  private void cacheKeys(@Nullable final CombinedResult combinedResult) {
    if (combinedResult != null) {
      cacheKeys(combinedResult.getProducts());
      cacheKeys(combinedResult.getCategories());
      cacheKeys(combinedResult.getProductTypes());
    }
  }

  private void cacheKeys(@Nullable final ResultingResourcesContainer resultsContainer) {
    if (resultsContainer != null) {
      resultsContainer
          .getResults()
          .forEach(
              referenceIdKey -> {
                final String key = referenceIdKey.getKey();
                final String id = referenceIdKey.getId();
                if (!isBlank(key)) {
                  idToKey.put(id, key);
                }
              });
    }
  }

  @Nonnull
  private CompletionStage<Map<String, String>> fetchCustomObjectKeys(
      @Nonnull final Set<String> nonCachedCustomObjectIds) {

    if (nonCachedCustomObjectIds.isEmpty()) {
      return CompletableFuture.completedFuture(idToKey);
    }

    final Consumer<List<CustomObject<JsonNode>>> pageConsumer =
        page ->
            page.forEach(
                resource ->
                    idToKey.put(
                        resource.getId(), CustomObjectCompositeIdentifier.of(resource).toString()));

    final CustomObjectQuery<JsonNode> jsonNodeCustomObjectQuery =
        CustomObjectQuery.ofJsonNode()
            .withPredicates(buildCustomObjectIdsQueryPredicate(nonCachedCustomObjectIds));

    return CtpQueryUtils.queryAll(getCtpClient(), jsonNodeCustomObjectQuery, pageConsumer)
        .thenApply(result -> idToKey);
  }

  private QueryPredicate<CustomObject<JsonNode>> buildCustomObjectIdsQueryPredicate(
      @Nonnull final Set<String> customObjectIds) {
    final List<String> idsSurroundedWithDoubleQuotes =
        customObjectIds
            .stream()
            .filter(StringUtils::isNotBlank)
            .map(customObjectId -> format("\"%s\"", customObjectId))
            .collect(Collectors.toList());
    String idsQueryString = idsSurroundedWithDoubleQuotes.toString();
    // Strip square brackets from list string. For example: ["id1", "id2"] -> "id1", "id2"
    idsQueryString = idsQueryString.substring(1, idsQueryString.length() - 1);
    return QueryPredicate.of(format("id in (%s)", idsQueryString));
  }
}
