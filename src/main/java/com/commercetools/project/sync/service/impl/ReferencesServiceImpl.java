package com.commercetools.project.sync.service.impl;

import com.commercetools.project.sync.model.request.CombinedReferenceKeysRequest;
import com.commercetools.project.sync.service.ReferencesService;
import io.sphere.sdk.client.SphereClient;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ReferencesServiceImpl extends BaseServiceImpl implements ReferencesService {
    private final Map<String, String> idToKey = new ConcurrentHashMap<>();

    public ReferencesServiceImpl(@Nonnull final SphereClient ctpClient) {
        super(ctpClient);
    }

    /**
     * TODO!!
     * @param productIds
     * @param categoryIds
     * @param productTypeIds
     * @return
     */
    @Nonnull
    @Override
    public CompletionStage<Map<String, String>> getReferenceKeys(
        @Nonnull final List<String> productIds,
        @Nonnull final List<String> categoryIds,
        @Nonnull final List<String> productTypeIds) {


        final List<String> nonCachedProductIds = getNonCachedIds(productIds);
        final List<String> nonCachedCategoryIds = getNonCachedIds(categoryIds);
        final List<String> nonCachedProductTypeIds = getNonCachedIds(productTypeIds);

        // if everything is cached, no need to make a request to CTP.
        if (nonCachedProductIds.isEmpty() && nonCachedCategoryIds.isEmpty() && nonCachedProductTypeIds.isEmpty()) {
            return CompletableFuture.completedFuture(idToKey);
        }

        // otherwise, make a combined request to CTP.
        final CombinedReferenceKeysRequest combinedReferenceKeysRequest =
            new CombinedReferenceKeysRequest(nonCachedProductIds, nonCachedCategoryIds, nonCachedProductTypeIds);

        return getCtpClient()
            .execute(combinedReferenceKeysRequest)
            .thenApply(combinedReferenceKeys -> {

                // TODO: Check if the key is empty/null
                combinedReferenceKeys
                    .getCategories()
                    .getResults()
                    .forEach(referenceKey -> idToKey.put(referenceKey.getId(), referenceKey.getKey()));

                combinedReferenceKeys
                    .getProducts()
                    .getResults()
                    .forEach(referenceKey -> idToKey.put(referenceKey.getId(), referenceKey.getKey()));

                combinedReferenceKeys
                    .getProductTypes()
                    .getResults()
                    .forEach(referenceKey -> idToKey.put(referenceKey.getId(), referenceKey.getKey()));
               return idToKey;
            });
        // TODO: Handle fetch exceptions
    }

    @Nonnull
    private List<String> getNonCachedIds(@Nonnull final List<String> ids) {
        return ids
            .stream()
            .filter(id -> !idToKey.containsKey(id))
            .collect(Collectors.toList());
    }
}
