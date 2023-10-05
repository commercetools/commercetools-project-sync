package com.commercetools.project.sync.cartdiscount;

import static com.commercetools.project.sync.util.SyncUtils.logErrorCallback;
import static com.commercetools.project.sync.util.SyncUtils.logWarningCallback;
import static com.commercetools.sync.cartdiscounts.utils.CartDiscountTransformUtils.toCartDiscountDrafts;

import com.commercetools.api.client.ByProjectKeyCartDiscountsGet;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.cart_discount.CartDiscount;
import com.commercetools.api.models.cart_discount.CartDiscountDraft;
import com.commercetools.api.models.cart_discount.CartDiscountPagedQueryResponse;
import com.commercetools.api.models.cart_discount.CartDiscountUpdateAction;
import com.commercetools.project.sync.Syncer;
import com.commercetools.project.sync.service.CustomObjectService;
import com.commercetools.project.sync.service.impl.CustomObjectServiceImpl;
import com.commercetools.sync.cartdiscounts.CartDiscountSync;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptionsBuilder;
import com.commercetools.sync.cartdiscounts.helpers.CartDiscountSyncStatistics;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This class compiles but not tested yet
// TODO: Test class and adjust logic if needed
public final class CartDiscountSyncer
    extends Syncer<
        CartDiscount,
        CartDiscountUpdateAction,
        CartDiscountDraft,
        CartDiscountSyncStatistics,
        CartDiscountSyncOptions,
        ByProjectKeyCartDiscountsGet,
        CartDiscountPagedQueryResponse,
        CartDiscountSync> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CartDiscountSyncer.class);

  /** Instantiates a {@link Syncer} instance. */
  private CartDiscountSyncer(
      @Nonnull final CartDiscountSync cartDiscountSync,
      @Nonnull final ProjectApiRoot sourceClient,
      @Nonnull final ProjectApiRoot targetClient,
      @Nonnull final CustomObjectService customObjectService,
      @Nonnull final Clock clock) {
    super(cartDiscountSync, sourceClient, targetClient, customObjectService, clock);
  }

  @Nonnull
  public static CartDiscountSyncer of(
      @Nonnull final ProjectApiRoot sourceClient,
      @Nonnull final ProjectApiRoot targetClient,
      @Nonnull final Clock clock) {

    final QuadConsumer<
            SyncException,
            Optional<CartDiscountDraft>,
            Optional<CartDiscount>,
            List<CartDiscountUpdateAction>>
        logErrorCallback =
            (exception, newResourceDraft, oldResource, updateActions) ->
                logErrorCallback(LOGGER, "cart discount", exception, oldResource, updateActions);
    final TriConsumer<SyncException, Optional<CartDiscountDraft>, Optional<CartDiscount>>
        logWarningCallback =
            (exception, newResourceDraft, oldResource) ->
                logWarningCallback(LOGGER, "cart discount", exception, oldResource);
    final CartDiscountSyncOptions syncOptions =
        CartDiscountSyncOptionsBuilder.of(targetClient)
            .errorCallback(logErrorCallback)
            .warningCallback(logWarningCallback)
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
    return toCartDiscountDrafts(getSourceClient(), referenceIdToKeyCache, page);
  }

  @Nonnull
  @Override
  protected ByProjectKeyCartDiscountsGet getQuery() {
    return getSourceClient().cartDiscounts().get();
  }

  @Nonnull
  @Override
  protected Logger getLoggerInstance() {
    return LOGGER;
  }
}
