package com.commercetools.project.sync.service.impl;

import com.commercetools.project.sync.product.CombinedReferenceKeysRequest;
import com.commercetools.project.sync.service.ReferencesService;
import io.sphere.sdk.client.SphereClient;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ReferencesServiceImpl implements ReferencesService {

    // Are you uuid unique across resources?
    final Map<String, String> idToKey = new ConcurrentHashMap<>();
    final SphereClient ctpClient;

    public ReferencesServiceImpl(@Nonnull final SphereClient ctpClient) {
        this.ctpClient = ctpClient;
    }

    @Nonnull
    @Override
    public CompletionStage<Map<String, String>> getReferenceKeys(
        @Nonnull final List<String> productIds,
        @Nonnull final List<String> categoryIds,
        @Nonnull final List<String> productTypeIds) {


        final List<String> nonCachedProductIds = productIds
            .stream()
            .filter(id -> !idToKey.containsKey(id))
            .collect(Collectors.toList());

        final List<String> nonCachedCategoryIds = categoryIds
            .stream()
            .filter(id -> !idToKey.containsKey(id))
            .collect(Collectors.toList());

        final List<String> nonCachedProductTypeIds = productTypeIds
            .stream()
            .filter(id -> !idToKey.containsKey(id))
            .collect(Collectors.toList());

        if (nonCachedCategoryIds.isEmpty() && nonCachedProductIds.isEmpty() && nonCachedProductTypeIds.isEmpty()) {
            return CompletableFuture.completedFuture(idToKey);
        }

        final CombinedReferenceKeysRequest combinedReferenceKeysRequest =
            new CombinedReferenceKeysRequest(nonCachedProductIds, nonCachedCategoryIds, nonCachedProductTypeIds);

        return ctpClient
            .execute(combinedReferenceKeysRequest)
            .thenApply(combinedReferenceKeys -> {

                // TODO: Check if the key is empty.
                combinedReferenceKeys
                    .getCategoryKeys()
                    .getReferenceKeys()
                    .forEach(referenceKey -> idToKey.put(referenceKey.getId(), referenceKey.getKey()));

                combinedReferenceKeys
                    .getProductKeys()
                    .getReferenceKeys()
                    .forEach(referenceKey -> idToKey.put(referenceKey.getId(), referenceKey.getKey()));

                combinedReferenceKeys
                    .getProductTypeKeys()
                    .getReferenceKeys()
                    .forEach(referenceKey -> idToKey.put(referenceKey.getId(), referenceKey.getKey()));


               return idToKey;
            });
    }
}
