package com.commercetools.project.sync.service.impl;

import com.commercetools.project.sync.model.request.ResourceIdsGraphQlRequest;
import com.commercetools.project.sync.service.ReferencesService;
import com.commercetools.project.sync.util.ChunkUtils;
import com.commercetools.sync.commons.models.GraphQlQueryResources;
import com.commercetools.sync.commons.models.ResourceKeyId;
import com.commercetools.sync.commons.utils.CollectionUtils;
import com.commercetools.sync.commons.utils.CtpQueryUtils;
import com.commercetools.sync.customobjects.helpers.CustomObjectCompositeIdentifier;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.queries.QueryPredicate;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class ReferencesServiceImpl extends BaseServiceImpl implements ReferencesService {
    private final Map<String, String> allResourcesIdToKey = new HashMap<>();

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
     * @param productIds      the product ids to find a key mapping for.
     * @param categoryIds     the category ids to find a key mapping for.
     * @param productTypeIds  the productType ids to find a key mapping for.
     * @param customObjectIds the custom object ids to find a key mapping for.
     * @return a map of id to key representing products, categories, productTypes and customObjects in
     * the CTP project defined by the injected {@code ctpClient}.
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

        /*
         * An id is a 36 characters long string. (i.e: 53c4a8b4-754f-4b95-b6f2-3e1e70e3d0c3) We
         * chunk them in 300 ids we will have around a query around 11.000 characters. Above this size it
         * could return - Error 413 (Request Entity Too Large)
         */
        final int CHUNK_SIZE = 300;        List<List<String>> productIdsChunk = ChunkUtils.chunk(nonCachedProductIds,
            CHUNK_SIZE);
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
                         .thenApply(results -> {
                             cacheKeys(results);
                             return allResourcesIdToKey;
                         })
                         .thenCompose(ignored -> fetchCustomObjectKeys(nonCachedCustomObjectIds));
    }

    @Nonnull
    private List<ResourceIdsGraphQlRequest> createResourceIdsGraphQlRequests(
        @Nonnull final List<List<String>> chunkedIds, @Nonnull final GraphQlQueryResources resourceType) {
        return chunkedIds.stream().map(chunk -> new ResourceIdsGraphQlRequest(new HashSet<>(chunk),
            resourceType)).collect(Collectors.toList());
    }

    @Nonnull
    private Set<String> getNonCachedIds(@Nonnull final Set<String> ids) {
        return ids.stream()
                  .filter(id -> !allResourcesIdToKey.containsKey(id))
                  .collect(Collectors.toSet());
    }

    @Nonnull
    private CompletionStage<Map<String, String>> fetchCustomObjectKeys(
        @Nonnull final Set<String> nonCachedCustomObjectIds) {

        if (nonCachedCustomObjectIds.isEmpty()) {
            return CompletableFuture.completedFuture(allResourcesIdToKey);
        }

        final Consumer<List<CustomObject<JsonNode>>> pageConsumer =
            page ->
                page.forEach(
                    resource ->
                        allResourcesIdToKey.put(
                            resource.getId(), CustomObjectCompositeIdentifier.of(resource).toString()));

        final CustomObjectQuery<JsonNode> jsonNodeCustomObjectQuery =
            CustomObjectQuery.ofJsonNode()
                             .withPredicates(buildCustomObjectIdsQueryPredicate(nonCachedCustomObjectIds));

        return CtpQueryUtils.queryAll(getCtpClient(), jsonNodeCustomObjectQuery, pageConsumer)
                            .thenApply(result -> allResourcesIdToKey);
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

    private void cacheKeys(final Set<ResourceKeyId> results) {
        results
            .forEach(
                resourceKeyId -> {
                    final String key = resourceKeyId.getKey();
                    final String id = resourceKeyId.getId();
                    if (!isBlank(key)) {
                        allResourcesIdToKey.put(id, key);
                    }
                });
    }
}
