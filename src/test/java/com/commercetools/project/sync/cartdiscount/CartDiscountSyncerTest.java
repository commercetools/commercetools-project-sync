package com.commercetools.project.sync.cartdiscount;

import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;

// These tests aren't migrated
// TODO: Migrate tests
class CartDiscountSyncerTest {

  final ReferenceIdToKeyCache referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();

  /*  @Test
  void of_ShouldCreateCartDiscountSyncerInstance() {
    // test
    final CartDiscountSyncer cartDiscountSyncer =
        CartDiscountSyncer.of(mock(SphereClient.class), mock(SphereClient.class), getMockedClock());

    // assertions
    assertThat(cartDiscountSyncer).isNotNull();
    assertThat(cartDiscountSyncer.getQuery()).isEqualTo(CartDiscountQuery.of());
    assertThat(cartDiscountSyncer.getSync()).isExactlyInstanceOf(CartDiscountSync.class);
  }

  @Test
  void transform_ShouldReplaceCartDiscountReferenceIdsWithKeys() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    final CartDiscountSyncer cartDiscountSyncer =
        CartDiscountSyncer.of(sourceClient, mock(SphereClient.class), getMockedClock());
    final List<CartDiscount> cartDiscountPage =
        asList(
            readObjectFromResource("cart-discount-key-1.json", CartDiscount.class),
            readObjectFromResource("cart-discount-key-2.json", CartDiscount.class));

    final List<String> referenceIds =
        cartDiscountPage.stream()
            .map(cartDiscount -> cartDiscount.getCustom().getType().getId())
            .collect(Collectors.toList());
    mockResourceIdsGraphQlRequest(
        sourceClient, "4db98ea6-38dc-4ccb-b20f-466e1566fd03", "test cart discount custom type");

    // test
    final CompletionStage<List<CartDiscountDraft>> draftsFromPageStage =
        cartDiscountSyncer.transform(cartDiscountPage);

    // assertions
    final List<CartDiscountDraft> expectedResult =
        toCartDiscountDrafts(sourceClient, referenceIdToKeyCache, cartDiscountPage).join();
    final List<String> referenceKeys =
        expectedResult.stream()
            .map(cartDiscount -> cartDiscount.getCustom().getType().getId())
            .collect(Collectors.toList());
    assertThat(referenceKeys).doesNotContainSequence(referenceIds);
    assertThat(draftsFromPageStage).isCompletedWithValue(expectedResult);
  }

  @Test
  void getQuery_ShouldBuildCartDiscountQuery() {
    // preparation
    final CartDiscountSyncer cartDiscountSyncer =
        CartDiscountSyncer.of(mock(SphereClient.class), mock(SphereClient.class), getMockedClock());

    // test
    final CartDiscountQuery query = cartDiscountSyncer.getQuery();

    // assertion
    assertThat(query).isEqualTo(CartDiscountQuery.of());
  }

  @Test
  void syncWithError_ShouldCallErrorCallback() {
    final TestLogger syncerTestLogger = TestLoggerFactory.getTestLogger(CartDiscountSyncer.class);
    // preparation: cart discount with no key is synced
    final SphereClient sourceClient = mock(SphereClient.class);
    final SphereClient targetClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereApiConfig.of("source-project"));
    when(targetClient.getConfig()).thenReturn(SphereApiConfig.of("target-project"));
    final List<CartDiscount> cartDiscounts =
        Collections.singletonList(
            readObjectFromResource("cart-discount-no-key.json", CartDiscount.class));

    final PagedQueryResult<CartDiscount> pagedQueryResult = mock(PagedQueryResult.class);
    when(pagedQueryResult.getResults()).thenReturn(cartDiscounts);
    when(sourceClient.execute(any(CartDiscountQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(pagedQueryResult));

    // test
    final CartDiscountSyncer cartDiscountSyncer =
        CartDiscountSyncer.of(sourceClient, targetClient, mock(Clock.class));
    cartDiscountSyncer.sync(null, true).toCompletableFuture().join();

    // assertion
    final LoggingEvent errorLog = syncerTestLogger.getAllLoggingEvents().get(1);
    assertThat(errorLog.getMessage())
        .isEqualTo(
            "Error when trying to sync cart discount. Existing key: <<not present>>. Update actions: []");
    assertThat(errorLog.getThrowable().get().getMessage())
        .isEqualTo(
            "CartDiscountDraft with name: LocalizedString(en -> 1-month prepay(Go Big)) doesn't have a key. Please make sure all cart discount drafts have keys.");
  }*/
}
