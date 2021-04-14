package com.commercetools.project.sync.shoppinglist;

import static com.commercetools.project.sync.util.SyncUtils.IDENTIFIER_NOT_PRESENT;
import static com.commercetools.project.sync.util.SyncUtils.logErrorCallback;
import static com.commercetools.project.sync.util.SyncUtils.logWarningCallback;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListReferenceResolutionUtils.buildShoppingListQuery;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListTransformUtils.toShoppingListDrafts;

import com.commercetools.project.sync.Syncer;
import com.commercetools.project.sync.service.CustomObjectService;
import com.commercetools.project.sync.service.impl.CustomObjectServiceImpl;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.shoppinglists.ShoppingListSync;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptionsBuilder;
import com.commercetools.sync.shoppinglists.helpers.ShoppingListSyncStatistics;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.queries.ShoppingListQuery;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ShoppingListSyncer
    extends Syncer<
        ShoppingList,
        ShoppingList,
        ShoppingListDraft,
        ShoppingListSyncStatistics,
        ShoppingListSyncOptions,
        ShoppingListQuery,
        ShoppingListSync> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ShoppingListSyncer.class);

  private ShoppingListSyncer(
      @Nonnull final ShoppingListSync sync,
      @Nonnull final SphereClient sourceClient,
      @Nonnull final SphereClient targetClient,
      @Nonnull final CustomObjectService customObjectService,
      @Nonnull final Clock clock) {
    super(sync, sourceClient, targetClient, customObjectService, clock);
  }

  public static ShoppingListSyncer of(
      @Nonnull final SphereClient sourceClient,
      @Nonnull final SphereClient targetClient,
      @Nonnull final Clock clock) {

    final QuadConsumer<
            SyncException,
            Optional<ShoppingListDraft>,
            Optional<ShoppingList>,
            List<UpdateAction<ShoppingList>>>
        logErrorCallback =
            (exception, newResourceDraft, oldResource, updateActions) ->
                logErrorCallback(
                    LOGGER,
                    "shoppingList",
                    exception,
                    oldResource.map(ShoppingList::getKey).orElse(IDENTIFIER_NOT_PRESENT),
                    updateActions);

    final TriConsumer<SyncException, Optional<ShoppingListDraft>, Optional<ShoppingList>>
        logWarningCallback =
            (exception, newResourceDraft, oldResource) ->
                logWarningCallback(
                    LOGGER,
                    "shoppingList",
                    exception,
                    oldResource.map(ShoppingList::getKey).orElse(IDENTIFIER_NOT_PRESENT));

    final ShoppingListSyncOptions shoppingListSyncOptions =
        ShoppingListSyncOptionsBuilder.of(targetClient)
            .errorCallback(logErrorCallback)
            .warningCallback(logWarningCallback)
            .build();

    final ShoppingListSync shoppingListSync = new ShoppingListSync(shoppingListSyncOptions);

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(targetClient);

    return new ShoppingListSyncer(
        shoppingListSync, sourceClient, targetClient, customObjectService, clock);
  }

  @Nonnull
  @Override
  protected CompletionStage<List<ShoppingListDraft>> transform(@Nonnull List<ShoppingList> page) {
    final ReferenceIdToKeyCache referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();
    return toShoppingListDrafts(this.getSourceClient(), referenceIdToKeyCache, page);
  }

  @Nonnull
  @Override
  protected ShoppingListQuery getQuery() {
    return buildShoppingListQuery();
  }
}
