package com.commercetools.project.sync.cartdiscount;

import static com.commercetools.project.sync.util.TestUtils.getMockedClock;
import static com.commercetools.sync.cartdiscounts.utils.CartDiscountReferenceResolutionUtils.buildCartDiscountQuery;
import static com.commercetools.sync.cartdiscounts.utils.CartDiscountReferenceResolutionUtils.mapToCartDiscountDrafts;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.cartdiscounts.CartDiscountSync;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.queries.CartDiscountQuery;
import io.sphere.sdk.client.SphereApiConfig;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.queries.PagedQueryResult;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

class CartDiscountSyncerTest {
  @Test
  void of_ShouldCreateCartDiscountSyncerInstance() {
    // test
    final CartDiscountSyncer cartDiscountSyncer =
        CartDiscountSyncer.of(mock(SphereClient.class), mock(SphereClient.class), getMockedClock());

    // assertions
    assertThat(cartDiscountSyncer).isNotNull();
    assertThat(cartDiscountSyncer.getQuery()).isEqualTo(buildCartDiscountQuery());
    assertThat(cartDiscountSyncer.getSync()).isExactlyInstanceOf(CartDiscountSync.class);
  }

  @Test
  void transform_ShouldReplaceCartDiscountReferenceIdsWithKeys() {
    // preparation
    final CartDiscountSyncer cartDiscountSyncer =
        CartDiscountSyncer.of(mock(SphereClient.class), mock(SphereClient.class), getMockedClock());
    final List<CartDiscount> cartDiscountPage =
        asList(
            readObjectFromResource("cart-discount-key-1.json", CartDiscount.class),
            readObjectFromResource("cart-discount-key-2.json", CartDiscount.class));
    final List<String> referenceIds =
        cartDiscountPage
            .stream()
            .filter(cartDiscount -> cartDiscount.getCustom() != null)
            .map(cartDiscount -> cartDiscount.getCustom().getType().getId())
            .collect(Collectors.toList());

    // test
    final CompletionStage<List<CartDiscountDraft>> draftsFromPageStage =
        cartDiscountSyncer.transform(cartDiscountPage);

    // assertions
    final List<CartDiscountDraft> expectedResult = mapToCartDiscountDrafts(cartDiscountPage);
    final List<String> referenceKeys =
        expectedResult
            .stream()
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
    assertThat(query).isEqualTo(buildCartDiscountQuery());
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
    final LoggingEvent errorLog = syncerTestLogger.getAllLoggingEvents().get(0);
    assertThat(errorLog.getMessage())
        .isEqualTo(
            "Error when trying to sync cart discount. Existing key: <<not present>>. Update actions: []");
    assertThat(errorLog.getThrowable().get().getMessage())
        .isEqualTo(
            "CartDiscountDraft with name: LocalizedString(en -> 1-month prepay(Go Big)) doesn't have a key. Please make sure all cart discount drafts have keys.");
  }
}
