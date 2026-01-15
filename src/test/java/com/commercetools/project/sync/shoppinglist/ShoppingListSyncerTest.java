package com.commercetools.project.sync.shoppinglist;

import static com.commercetools.project.sync.util.TestUtils.mockResourceIdsGraphQlRequest;
import static com.commercetools.project.sync.util.TestUtils.readObjectFromResource;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ByProjectKeyShoppingListsGet;
import com.commercetools.api.client.ByProjectKeyShoppingListsRequestBuilder;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.defaultconfig.ApiRootBuilder;
import com.commercetools.api.models.shopping_list.ShoppingList;
import com.commercetools.api.models.shopping_list.ShoppingListDraft;
import com.commercetools.api.models.shopping_list.ShoppingListPagedQueryResponse;
import com.commercetools.api.models.shopping_list.ShoppingListPagedQueryResponseBuilder;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.shoppinglists.ShoppingListSync;
import com.commercetools.sync.shoppinglists.utils.ShoppingListTransformUtils;
import com.github.valfirst.slf4jtest.LoggingEvent;
import com.github.valfirst.slf4jtest.TestLogger;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShoppingListSyncerTest {
  private final TestLogger syncerTestLogger =
      TestLoggerFactory.getTestLogger(ShoppingListSyncer.class);
  private final ReferenceIdToKeyCache referenceIdToKeyCache =
      new CaffeineReferenceIdToKeyCacheImpl();

  @BeforeEach
  void setup() {
    syncerTestLogger.clearAll();
  }

  @Test
  void of_ShouldCreateShoppingListSyncerInstance() {
    // test
    final ShoppingListSyncer shoppingListSyncer =
        ShoppingListSyncer.of(
            mock(ProjectApiRoot.class), mock(ProjectApiRoot.class), mock(Clock.class));

    // assertion
    assertThat(shoppingListSyncer).isNotNull();
    assertThat(shoppingListSyncer.getSync()).isInstanceOf(ShoppingListSync.class);
  }

  @Test
  void transform_ShouldReplaceShoppingListReferenceIdsWithKeys() {
    // preparation
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final ShoppingListSyncer shoppingListSyncer =
        ShoppingListSyncer.of(sourceClient, mock(ProjectApiRoot.class), mock(Clock.class));
    final List<ShoppingList> shoppingList =
        List.of(readObjectFromResource("shopping-list.json", ShoppingList.class));

    mockResourceIdsGraphQlRequest(
        sourceClient, "shoppingLists", "5ebfa80e-f4aa-4c0b-be64-e348e09a855a", "customTypeKey");

    // test
    final CompletionStage<List<ShoppingListDraft>> draftsFromPageStage =
        shoppingListSyncer.transform(shoppingList);

    // assertion
    assertThat(draftsFromPageStage)
        .isCompletedWithValue(
            ShoppingListTransformUtils.toShoppingListDrafts(
                    sourceClient, referenceIdToKeyCache, shoppingList)
                .join());
  }

  @Test
  void getQuery_ShouldBuildShoppingListQuery() {
    final ProjectApiRoot apiRoot =
        ApiRootBuilder.of().withApiBaseUrl("baseUrl").build("testProjectKey");
    final ShoppingListSyncer shoppingListSyncer =
        ShoppingListSyncer.of(apiRoot, apiRoot, mock(Clock.class));

    // assertion
    final ByProjectKeyShoppingListsGet shoppingListsGet = shoppingListSyncer.getQuery();

    assertThat(shoppingListsGet.getExpand()).containsExactly("lineItems[*].variant");
  }

  @Test
  void syncWithError_ShouldCallErrorCallback() {
    // preparation: shoppingList with no key is synced
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final List<ShoppingList> shoppingLists =
        List.of(readObjectFromResource("shopping-list-no-key.json", ShoppingList.class));
    final ByProjectKeyShoppingListsRequestBuilder projectKeyShoppingListsRequestBuilder = mock();
    when(sourceClient.shoppingLists()).thenReturn(projectKeyShoppingListsRequestBuilder);
    final ByProjectKeyShoppingListsGet shoppingListsGet = mock();
    when(shoppingListsGet.addExpand(anyString())).thenReturn(shoppingListsGet);
    when(shoppingListsGet.withLimit(anyInt())).thenReturn(shoppingListsGet);
    when(shoppingListsGet.withWithTotal(anyBoolean())).thenReturn(shoppingListsGet);
    when(shoppingListsGet.withSort(anyString())).thenReturn(shoppingListsGet);

    final ShoppingListPagedQueryResponse shoppingListPagedQueryResponse =
        ShoppingListPagedQueryResponseBuilder.of()
            .results(shoppingLists)
            .limit(20L)
            .offset(0L)
            .count(1L)
            .build();
    final ApiHttpResponse apiHttpResponse = mock(ApiHttpResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(shoppingListPagedQueryResponse);
    when(shoppingListsGet.execute()).thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(sourceClient.shoppingLists().get()).thenReturn(shoppingListsGet);

    mockResourceIdsGraphQlRequest(
        sourceClient, "shoppingLists", "5ebfa80e-f4aa-4c0b-be64-e348e09a855a", "customTypeKey");

    // test
    final ShoppingListSyncer shoppingListSyncer =
        ShoppingListSyncer.of(sourceClient, mock(ProjectApiRoot.class), mock(Clock.class));
    shoppingListSyncer.sync(null, true).toCompletableFuture().join();

    // assertion
    final LoggingEvent errorLog = syncerTestLogger.getAllLoggingEvents().get(1);
    assertThat(errorLog.getMessage())
        .isEqualTo(
            "Error when trying to sync shoppingList. Existing key: <<not present>>. Update actions: []");
    assertThat(errorLog.getThrowable().get().getMessage())
        .isEqualTo(
            format(
                "ShoppingListDraft with name: %s doesn't have a key. Please make sure all shopping list drafts have keys"
                    + ".",
                shoppingLists.get(0).getName().toString()));
  }

  @Test
  void transform_WithStoreReference_ShouldReplaceStoreReferenceIdsWithKeys() {
    // preparation
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final ShoppingListSyncer shoppingListSyncer =
        ShoppingListSyncer.of(sourceClient, mock(ProjectApiRoot.class), mock(Clock.class));
    final List<ShoppingList> shoppingLists =
        List.of(readObjectFromResource("shopping-list-with-store.json", ShoppingList.class));

    // Mock the store reference resolution
    mockResourceIdsGraphQlRequest(
        sourceClient, "stores", "store-id-456", "store-key-456");
    mockResourceIdsGraphQlRequest(
        sourceClient, "shoppingLists", "5ebfa80e-f4aa-4c0b-be64-e348e09a855a", "customTypeKey");

    // test
    final CompletionStage<List<ShoppingListDraft>> draftsFromPageStage =
        shoppingListSyncer.transform(shoppingLists);

    // assertion
    assertThat(draftsFromPageStage)
        .isCompletedWithValue(
            ShoppingListTransformUtils.toShoppingListDrafts(
                    sourceClient, referenceIdToKeyCache, shoppingLists)
                .join());
  }

  @Test
  void transform_WithMultipleStoreReferences_ShouldResolveAllStoreReferences() {
    // preparation
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final ShoppingListSyncer shoppingListSyncer =
        ShoppingListSyncer.of(sourceClient, mock(ProjectApiRoot.class), mock(Clock.class));
    final List<ShoppingList> shoppingLists =
        List.of(
            readObjectFromResource("shopping-list-with-multiple-stores.json", ShoppingList.class));

    // Mock multiple store reference resolutions
    mockResourceIdsGraphQlRequest(sourceClient, "stores", "store-id-1", "store-key-1");
    mockResourceIdsGraphQlRequest(sourceClient, "stores", "store-id-2", "store-key-2");
    mockResourceIdsGraphQlRequest(
        sourceClient, "shoppingLists", "5ebfa80e-f4aa-4c0b-be64-e348e09a855a", "customTypeKey");

    // test
    final CompletionStage<List<ShoppingListDraft>> draftsFromPageStage =
        shoppingListSyncer.transform(shoppingLists);

    // assertion
    final List<ShoppingListDraft> shoppingListDrafts =
        draftsFromPageStage.toCompletableFuture().join();
    assertThat(shoppingListDrafts).isNotEmpty();
    assertThat(shoppingListDrafts.get(0).getStore()).isNotNull();
  }
}
