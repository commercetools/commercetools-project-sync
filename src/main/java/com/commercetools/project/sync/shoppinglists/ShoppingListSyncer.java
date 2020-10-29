package com.commercetools.project.sync.shoppinglists;

import com.commercetools.project.sync.Syncer;
import com.commercetools.project.sync.service.CustomObjectService;
import com.commercetools.sync.shoppinglists.ShoppingListSync;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.shoppinglists.helpers.ShoppingListSyncStatistics;
import com.commercetools.sync.shoppinglists.utils.ShoppingListReferenceResolutionUtils;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.queries.ShoppingListQuery;

import javax.annotation.Nonnull;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class ShoppingListSyncer extends Syncer<
        ShoppingList,
        ShoppingListDraft,
        ShoppingListSyncStatistics,
        ShoppingListSyncOptions,
        ShoppingListQuery,
        ShoppingListSync> {

    public ShoppingListSyncer(@Nonnull ShoppingListSync sync,
                              @Nonnull SphereClient sourceClient,
                              @Nonnull SphereClient targetClient,
                              @Nonnull CustomObjectService customObjectService,
                              @Nonnull Clock clock) {

        super(sync, sourceClient, targetClient, customObjectService, clock);
    }

    @Nonnull
    @Override
    protected CompletionStage<List<ShoppingListDraft>> transform(@Nonnull List<ShoppingList> page) {
        return CompletableFuture.completedFuture(
                ShoppingListReferenceResolutionUtils.mapToShoppingListDrafts(page));
    }

    @Nonnull
    @Override
    protected ShoppingListQuery getQuery() {
        return ShoppingListReferenceResolutionUtils.buildShoppingListQuery();
    }
}
