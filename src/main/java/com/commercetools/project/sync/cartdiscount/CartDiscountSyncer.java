package com.commercetools.project.sync.cartdiscount;

import static com.commercetools.sync.cartdiscounts.utils.CartDiscountReferenceReplacementUtils.buildCartDiscountQuery;
import static com.commercetools.sync.cartdiscounts.utils.CartDiscountReferenceReplacementUtils.replaceCartDiscountsReferenceIdsWithKeys;

import com.commercetools.project.sync.Syncer;
import com.commercetools.project.sync.service.CustomObjectService;
import com.commercetools.project.sync.service.impl.CustomObjectServiceImpl;
import com.commercetools.sync.cartdiscounts.CartDiscountSync;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptionsBuilder;
import com.commercetools.sync.cartdiscounts.helpers.CartDiscountSyncStatistics;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.queries.CartDiscountQuery;
import io.sphere.sdk.client.SphereClient;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CartDiscountSyncer
    extends Syncer<
        CartDiscount,
        CartDiscountDraft,
        CartDiscountSyncStatistics,
        CartDiscountSyncOptions,
        CartDiscountQuery,
        CartDiscountSync> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CartDiscountSyncer.class);

  /** Instantiates a {@link Syncer} instance. */
  private CartDiscountSyncer(
      @Nonnull final CartDiscountSync cartDiscountSync,
      @Nonnull final SphereClient sourceClient,
      @Nonnull final SphereClient targetClient,
      @Nonnull final CustomObjectService customObjectService,
      @Nonnull final Clock clock) {
    super(cartDiscountSync, sourceClient, targetClient, customObjectService, clock);
  }

  @Nonnull
  public static CartDiscountSyncer of(
      @Nonnull final SphereClient sourceClient,
      @Nonnull final SphereClient targetClient,
      @Nonnull final Clock clock) {

    final CartDiscountSyncOptions syncOptions =
        CartDiscountSyncOptionsBuilder.of(targetClient)
            .errorCallback(LOGGER::error)
            .warningCallback(LOGGER::warn)
            .build();

    final CartDiscountSync cartDiscountSync = new CartDiscountSync(syncOptions);

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(targetClient);

    return new CartDiscountSyncer(
        cartDiscountSync, sourceClient, targetClient, customObjectService, clock);
  }

  @Override
  @Nonnull
  protected CompletionStage<List<CartDiscountDraft>> transform(
      @Nonnull final List<CartDiscount> page) {
    return CompletableFuture.completedFuture(replaceCartDiscountsReferenceIdsWithKeys(page));
  }

  @Nonnull
  @Override
  protected CartDiscountQuery getQuery(String queryString) {
    return buildCartDiscountQuery();
  }
}
