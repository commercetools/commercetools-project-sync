package com.commercetools.project.sync.shoppinglist;

import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import org.junit.jupiter.api.BeforeEach;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

// These tests aren't migrated
// TODO: Migrate tests
class ShoppingListSyncerTest {
  private final TestLogger syncerTestLogger =
      TestLoggerFactory.getTestLogger(ShoppingListSyncer.class);
  private final ReferenceIdToKeyCache referenceIdToKeyCache =
      new CaffeineReferenceIdToKeyCacheImpl();

  @BeforeEach
  void setup() {
    syncerTestLogger.clearAll();
  }

  //  @Test
  //  void of_ShouldCreateShoppingListSyncerInstance() {
  //    // test
  //    final ShoppingListSyncer shoppingListSyncer =
  //        ShoppingListSyncer.of(
  //            mock(SphereClient.class), mock(SphereClient.class), mock(Clock.class));
  //
  //    // assertion
  //    assertThat(shoppingListSyncer).isNotNull();
  //    assertThat(shoppingListSyncer.getSync()).isInstanceOf(ShoppingListSync.class);
  //  }
  //
  //  @Test
  //  void transform_ShouldReplaceShoppingListReferenceIdsWithKeys() {
  //    // preparation
  //    final SphereClient sourceClient = mock(SphereClient.class);
  //    final ShoppingListSyncer shoppingListSyncer =
  //        ShoppingListSyncer.of(sourceClient, mock(SphereClient.class), mock(Clock.class));
  //    final List<ShoppingList> shoppingList =
  //        Collections.singletonList(readObjectFromResource("shopping-list.json",
  // ShoppingList.class));
  //
  //    mockResourceIdsGraphQlRequest(
  //        sourceClient, "5ebfa80e-f4aa-4c0b-be64-e348e09a855a", "customTypeKey");
  //
  //    // test
  //    final CompletionStage<List<ShoppingListDraft>> draftsFromPageStage =
  //        shoppingListSyncer.transform(shoppingList);
  //
  //    // assertion
  //    assertThat(draftsFromPageStage)
  //        .isCompletedWithValue(
  //            ShoppingListTransformUtils.toShoppingListDrafts(
  //                    sourceClient, referenceIdToKeyCache, shoppingList)
  //                .join());
  //  }
  //
  //  @Test
  //  void getQuery_ShouldBuildShoppingListQuery() {
  //    final ShoppingListSyncer shoppingListSyncer =
  //        ShoppingListSyncer.of(
  //            mock(SphereClient.class), mock(SphereClient.class), mock(Clock.class));
  //
  //    // assertion
  //    final ShoppingListQuery query = shoppingListSyncer.getQuery();
  //
  // assertThat(query.expansionPaths()).containsExactly(ExpansionPath.of("lineItems[*].variant"));
  //  }
  //
  //  @Test
  //  void syncWithError_ShouldCallErrorCallback() {
  //    // preparation: shoppingList with no key is synced
  //    final SphereClient sourceClient = mock(SphereClient.class);
  //    final SphereClient targetClient = mock(SphereClient.class);
  //    when(sourceClient.getConfig()).thenReturn(SphereApiConfig.of("source-project"));
  //    when(targetClient.getConfig()).thenReturn(SphereApiConfig.of("target-project"));
  //    final List<ShoppingList> shoppingLists =
  //        Collections.singletonList(
  //            readObjectFromResource("shopping-list-no-key.json", ShoppingList.class));
  //
  //    final PagedQueryResult<ShoppingList> pagedQueryResult = mock(PagedQueryResult.class);
  //    when(pagedQueryResult.getResults()).thenReturn(shoppingLists);
  //    when(sourceClient.execute(any(ShoppingListQuery.class)))
  //        .thenReturn(CompletableFuture.completedFuture(pagedQueryResult));
  //
  //    mockResourceIdsGraphQlRequest(
  //        sourceClient, "5ebfa80e-f4aa-4c0b-be64-e348e09a855a", "customTypeKey");
  //
  //    // test
  //    final ShoppingListSyncer shoppingListSyncer =
  //        ShoppingListSyncer.of(sourceClient, targetClient, mock(Clock.class));
  //    shoppingListSyncer.sync(null, true).toCompletableFuture().join();
  //
  //    // assertion
  //    final LoggingEvent errorLog = syncerTestLogger.getAllLoggingEvents().get(1);
  //    assertThat(errorLog.getMessage())
  //        .isEqualTo(
  //            "Error when trying to sync shoppingList. Existing key: <<not present>>. Update
  // actions: []");
  //    assertThat(errorLog.getThrowable().get().getMessage())
  //        .isEqualTo(
  //            "ShoppingListDraft with name: LocalizedString(en -> shoppingList-name-1) doesn't
  // have a key. Please make sure all shopping list drafts have keys.");
  //  }
}
