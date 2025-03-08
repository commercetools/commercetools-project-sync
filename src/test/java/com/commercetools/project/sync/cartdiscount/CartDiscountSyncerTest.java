package com.commercetools.project.sync.cartdiscount;

import static com.commercetools.project.sync.util.TestUtils.*;
import static com.commercetools.sync.cartdiscounts.utils.CartDiscountTransformUtils.toCartDiscountDrafts;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ByProjectKeyCartDiscountsGet;
import com.commercetools.api.client.ByProjectKeyCartDiscountsRequestBuilder;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.cart_discount.CartDiscount;
import com.commercetools.api.models.cart_discount.CartDiscountDraft;
import com.commercetools.api.models.cart_discount.CartDiscountPagedQueryResponse;
import com.commercetools.sync.cartdiscounts.CartDiscountSync;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.github.valfirst.slf4jtest.LoggingEvent;
import com.github.valfirst.slf4jtest.TestLogger;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CartDiscountSyncerTest {

  final ReferenceIdToKeyCache referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();

  @Test
  void of_ShouldCreateCartDiscountSyncerInstance() {
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final ByProjectKeyCartDiscountsRequestBuilder byProjectKeyCartDiscountsRequestBuilder = mock();
    when(sourceClient.cartDiscounts()).thenReturn(byProjectKeyCartDiscountsRequestBuilder);
    final ByProjectKeyCartDiscountsGet byProjectKeyCartDiscountsGet = mock();
    when(byProjectKeyCartDiscountsRequestBuilder.get()).thenReturn(byProjectKeyCartDiscountsGet);

    // test
    final CartDiscountSyncer cartDiscountSyncer =
        CartDiscountSyncer.of(sourceClient, mock(ProjectApiRoot.class), getMockedClock());

    // assertions
    assertThat(cartDiscountSyncer).isNotNull();
    assertThat(cartDiscountSyncer.getQuery()).isEqualTo(byProjectKeyCartDiscountsGet);
    assertThat(cartDiscountSyncer.getSync()).isExactlyInstanceOf(CartDiscountSync.class);
  }

  @Test
  void transform_ShouldReplaceCartDiscountReferenceIdsWithKeys() {
    // preparation
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final CartDiscountSyncer cartDiscountSyncer =
        CartDiscountSyncer.of(sourceClient, mock(ProjectApiRoot.class), getMockedClock());
    final List<CartDiscount> cartDiscountPage =
        asList(
            readObjectFromResource("cart-discount-key-1.json", CartDiscount.class),
            readObjectFromResource("cart-discount-key-2.json", CartDiscount.class));

    final List<String> referenceIds =
        cartDiscountPage.stream()
            .map(cartDiscount -> cartDiscount.getCustom().getType().getId())
            .collect(Collectors.toList());
    mockResourceIdsGraphQlRequest(
        sourceClient,
        "typeDefinitions",
        "4db98ea6-38dc-4ccb-b20f-466e1566fd03",
        "test cart discount custom type");

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
  void syncWithError_ShouldCallErrorCallback() {
    final TestLogger syncerTestLogger = TestLoggerFactory.getTestLogger(CartDiscountSyncer.class);
    // preparation: cart discount with no key is synced
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final ProjectApiRoot targetClient = mock(ProjectApiRoot.class);
    final List<CartDiscount> cartDiscounts =
        Collections.singletonList(
            readObjectFromResource("cart-discount-no-key.json", CartDiscount.class));

    final ApiHttpResponse<CartDiscountPagedQueryResponse> apiHttpResponse =
        mock(ApiHttpResponse.class);
    final CartDiscountPagedQueryResponse cartDiscountPagedQueryResponse =
        mock(CartDiscountPagedQueryResponse.class);
    when(cartDiscountPagedQueryResponse.getResults()).thenReturn(cartDiscounts);
    when(apiHttpResponse.getBody()).thenReturn(cartDiscountPagedQueryResponse);

    final ByProjectKeyCartDiscountsRequestBuilder byProjectKeyCartDiscountsRequestBuilder = mock();
    when(sourceClient.cartDiscounts()).thenReturn(byProjectKeyCartDiscountsRequestBuilder);
    final ByProjectKeyCartDiscountsGet byProjectKeyCartDiscountsGet = mock();
    when(byProjectKeyCartDiscountsRequestBuilder.get()).thenReturn(byProjectKeyCartDiscountsGet);
    when(byProjectKeyCartDiscountsGet.withLimit(anyInt())).thenReturn(byProjectKeyCartDiscountsGet);
    when(byProjectKeyCartDiscountsGet.withSort(anyString()))
        .thenReturn(byProjectKeyCartDiscountsGet);
    when(byProjectKeyCartDiscountsGet.withWithTotal(anyBoolean()))
        .thenReturn(byProjectKeyCartDiscountsGet);
    when(byProjectKeyCartDiscountsGet.execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

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
            format(
                "CartDiscountDraft with name: %s doesn't have a key. Please make sure all cart discount drafts have keys.",
                cartDiscounts.get(0).getName().toString()));
  }
}
