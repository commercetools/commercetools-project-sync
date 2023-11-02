package com.commercetools.project.sync.shoppinglist;

import static com.commercetools.project.sync.util.SyncUtils.IDENTIFIER_NOT_PRESENT;
import static com.commercetools.project.sync.util.SyncUtils.logErrorCallback;
import static com.commercetools.project.sync.util.SyncUtils.logWarningCallback;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListTransformUtils.toShoppingListDrafts;

import com.commercetools.api.client.ByProjectKeyShoppingListsGet;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.shopping_list.ShoppingList;
import com.commercetools.api.models.shopping_list.ShoppingListDraft;
import com.commercetools.api.models.shopping_list.ShoppingListPagedQueryResponse;
import com.commercetools.api.models.shopping_list.ShoppingListUpdateAction;
import com.commercetools.api.predicates.query.shopping_list.ShoppingListQueryBuilderDsl;
import com.commercetools.project.sync.Syncer;
import com.commercetools.project.sync.service.CustomObjectService;
import com.commercetools.project.sync.service.impl.CustomObjectServiceImpl;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.shoppinglists.ShoppingListSync;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptionsBuilder;
import com.commercetools.sync.shoppinglists.helpers.ShoppingListSyncStatistics;
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
        ShoppingListUpdateAction,
        ShoppingListDraft,
        ShoppingListQueryBuilderDsl,
        ShoppingListSyncStatistics,
        ShoppingListSyncOptions,
        ByProjectKeyShoppingListsGet,
        ShoppingListPagedQueryResponse,
        ShoppingListSync> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ShoppingListSyncer.class);

  private ShoppingListSyncer(
      @Nonnull final ShoppingListSync sync,
      @Nonnull final ProjectApiRoot sourceClient,
      @Nonnull final ProjectApiRoot targetClient,
      @Nonnull final CustomObjectService customObjectService,
      @Nonnull final Clock clock) {
    super(sync, sourceClient, targetClient, customObjectService, clock);
  }

  public static ShoppingListSyncer of(
      @Nonnull final ProjectApiRoot sourceClient,
      @Nonnull final ProjectApiRoot targetClient,
      @Nonnull final Clock clock) {

    final QuadConsumer<
            SyncException,
            Optional<ShoppingListDraft>,
            Optional<ShoppingList>,
            List<ShoppingListUpdateAction>>
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
    return toShoppingListDrafts(getSourceClient(), referenceIdToKeyCache, page);
  }

  @Nonnull
  @Override
  protected ByProjectKeyShoppingListsGet getQuery() {
    return getSourceClient().shoppingLists().get().addExpand("lineItems[*].variant");
  }

  @Nonnull
  @Override
  protected Logger getLoggerInstance() {
    return LOGGER;
  }
}
