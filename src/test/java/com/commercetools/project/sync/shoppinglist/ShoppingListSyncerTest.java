package com.commercetools.project.sync.shoppinglist;

import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.shoppinglists.ShoppingListSync;
import com.commercetools.sync.shoppinglists.utils.ShoppingListReferenceResolutionUtils;
import io.sphere.sdk.client.SphereApiConfig;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.expansion.ExpansionPath;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.expansion.ShoppingListExpansionModel;
import io.sphere.sdk.shoppinglists.queries.ShoppingListQuery;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

class ShoppingListSyncerTest {
  private final TestLogger syncerTestLogger =
      TestLoggerFactory.getTestLogger(ShoppingListSyncer.class);

  @BeforeEach
  void setup() {
    syncerTestLogger.clearAll();
  }

  @Test
  void of_ShouldCreateShoppingListSyncerInstance() {
    // test
    final ShoppingListSyncer shoppingListSyncer =
        ShoppingListSyncer.of(
            mock(SphereClient.class), mock(SphereClient.class), mock(Clock.class));

    // assertion
    assertThat(shoppingListSyncer).isNotNull();
    assertThat(shoppingListSyncer.getSync()).isInstanceOf(ShoppingListSync.class);
  }

  @Test
  void transform_ShouldReplaceShoppingListReferenceIdsWithKeys() {
    // preparation
    final ShoppingListSyncer shoppingListSyncer =
        ShoppingListSyncer.of(
            mock(SphereClient.class), mock(SphereClient.class), mock(Clock.class));
    final List<ShoppingList> shoppingList =
        Collections.singletonList(readObjectFromResource("shopping-list.json", ShoppingList.class));

    // test
    final CompletionStage<List<ShoppingListDraft>> draftsFromPageStage =
        shoppingListSyncer.transform(shoppingList);

    // assertion
    assertThat(draftsFromPageStage)
        .isCompletedWithValue(
            ShoppingListReferenceResolutionUtils.mapToShoppingListDrafts(shoppingList));
  }

  @Test
  void getQuery_ShouldBuildShoppingListQuery() {
    // test
    ShoppingListQuery expectedQuery = buildShoppingListQuery();
    final ShoppingListSyncer shoppingListSyncer =
        ShoppingListSyncer.of(
            mock(SphereClient.class), mock(SphereClient.class), mock(Clock.class));

    // assertion
    final ShoppingListQuery query = shoppingListSyncer.getQuery();
    assertThat(query).isEqualTo(expectedQuery);
  }

  @Test
  void syncWithError_ShouldCallErrorCallback() {
    // preparation: shoppingList with no key is synced
    final SphereClient sourceClient = mock(SphereClient.class);
    final SphereClient targetClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereApiConfig.of("source-project"));
    when(targetClient.getConfig()).thenReturn(SphereApiConfig.of("target-project"));
    final List<ShoppingList> shoppingLists =
        Collections.singletonList(
            readObjectFromResource("shopping-list-no-key.json", ShoppingList.class));

    final PagedQueryResult<ShoppingList> pagedQueryResult = mock(PagedQueryResult.class);
    when(pagedQueryResult.getResults()).thenReturn(shoppingLists);
    when(sourceClient.execute(any(ShoppingListQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(pagedQueryResult));

    // test
    final ShoppingListSyncer shoppingListSyncer =
        ShoppingListSyncer.of(sourceClient, targetClient, mock(Clock.class));
    shoppingListSyncer.sync(null, true).toCompletableFuture().join();

    // assertion
    final LoggingEvent errorLog = syncerTestLogger.getAllLoggingEvents().get(0);
    assertThat(errorLog.getMessage())
        .isEqualTo(
            "Error when trying to sync shoppingList. Existing key: <<not present>>. Update actions: []");
    assertThat(errorLog.getThrowable().get().getMessage())
        .isEqualTo(
            "ShoppingListDraft with name: LocalizedString(en -> shoppingList-name-1) doesn't have a key. Please make sure all shopping list drafts have keys.");
  }

  private ShoppingListQuery buildShoppingListQuery() {
    return (ShoppingListQuery)((ShoppingListQuery)((ShoppingListQuery)((ShoppingListQuery)ShoppingListQuery.of().withExpansionPaths(ShoppingListExpansionModel::customer).plusExpansionPaths(ExpansionPath.of("custom.type"))).plusExpansionPaths(ExpansionPath.of("lineItems[*].variant"))).plusExpansionPaths(ExpansionPath.of("lineItems[*].custom.type"))).plusExpansionPaths(ExpansionPath.of("textLineItems[*].custom.type"));
  }
}
