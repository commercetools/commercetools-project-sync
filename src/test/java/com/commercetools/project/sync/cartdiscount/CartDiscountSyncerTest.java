package com.commercetools.project.sync.cartdiscount;

import static com.commercetools.project.sync.util.TestUtils.getMockedClock;
import static com.commercetools.sync.cartdiscounts.utils.CartDiscountReferenceResolutionUtils.buildCartDiscountQuery;
import static com.commercetools.sync.cartdiscounts.utils.CartDiscountReferenceResolutionUtils.mapToCartDiscountDrafts;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.sync.cartdiscounts.CartDiscountSync;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.queries.CartDiscountQuery;
import io.sphere.sdk.client.SphereClient;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

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
}
