package com.commercetools.project.sync.product;

import static com.commercetools.project.sync.util.SyncUtils.IDENTIFIER_NOT_PRESENT;
import static com.commercetools.project.sync.util.SyncUtils.logErrorCallback;
import static com.commercetools.project.sync.util.SyncUtils.logWarningCallback;
import static com.commercetools.sync.products.utils.ProductTransformUtils.toProductDrafts;

import com.commercetools.project.sync.Syncer;
import com.commercetools.project.sync.model.ProductSyncCustomRequest;
import com.commercetools.project.sync.service.CustomObjectService;
import com.commercetools.project.sync.service.impl.CustomObjectServiceImpl;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.WithKey;
import io.sphere.sdk.products.*;
import io.sphere.sdk.products.commands.updateactions.SetDiscountedPrice;
import io.sphere.sdk.products.queries.ProductProjectionQuery;
import io.sphere.sdk.queries.QueryPredicate;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProductSyncer
    extends Syncer<
        ProductProjection,
        Product,
        ProductDraft,
        ProductSyncStatistics,
        ProductSyncOptions,
        ProductProjectionQuery,
        ProductSync> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProductSyncer.class);

  private final ProductSyncCustomRequest productSyncCustomRequest;

  /** Instantiates a {@link Syncer} instance. */
  private ProductSyncer(
      @Nonnull final ProductSync productSync,
      @Nonnull final SphereClient sourceClient,
      @Nonnull final SphereClient targetClient,
      @Nonnull final CustomObjectService customObjectService,
      @Nonnull final Clock clock,
      @Nullable final ProductSyncCustomRequest productSyncCustomRequest) {
    super(productSync, sourceClient, targetClient, customObjectService, clock);
    this.productSyncCustomRequest = productSyncCustomRequest;
  }

  @Nonnull
  public static ProductSyncer of(
      @Nonnull final SphereClient sourceClient,
      @Nonnull final SphereClient targetClient,
      @Nonnull final Clock clock,
      @Nullable final ProductSyncCustomRequest productSyncCustomRequest) {

    final QuadConsumer<
            SyncException,
            Optional<ProductDraft>,
            Optional<ProductProjection>,
            List<UpdateAction<Product>>>
        logErrorCallback =
            (exception, newResourceDraft, oldResource, updateActions) -> {
              final String resourceKey =
                  oldResource.map(WithKey::getKey).orElse(IDENTIFIER_NOT_PRESENT);
              logErrorCallback(LOGGER, "product", exception, resourceKey, updateActions);
            };

    final TriConsumer<SyncException, Optional<ProductDraft>, Optional<ProductProjection>>
        logWarningCallback =
            (exception, newResourceDraft, oldResource) ->
                logWarningCallback(LOGGER, "product", exception, oldResource);
    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(targetClient)
            .beforeUpdateCallback(
                (updateActions, newProductDraft, oldProduct) -> {
                  if (updateActions != null) {
                    return updateActions
                        .stream()
                        .filter(updateAction -> !(updateAction instanceof SetDiscountedPrice))
                        .collect(Collectors.toList());
                  }
                  return null;
                })
            .errorCallback(logErrorCallback)
            .warningCallback(logWarningCallback)
            .build();

    final ProductSync productSync = new ProductSync(syncOptions);

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(targetClient);

    return new ProductSyncer(
        productSync,
        sourceClient,
        targetClient,
        customObjectService,
        clock,
        productSyncCustomRequest);
  }

  @Nonnull
  @Override
  protected CompletionStage<List<ProductDraft>> transform(@Nonnull List<ProductProjection> page) {
    return toProductDrafts(getSourceClient(), referenceIdToKeyCache, page)
        .handle(
            (productDrafts, throwable) -> {
              if (throwable != null) {
                if (LOGGER.isWarnEnabled()) {
                  LOGGER.warn(throwable.getMessage(), getCompletionExceptionCause(throwable));
                }
                return Collections.emptyList();
              }
              return removeDiscountedFromPrices(productDrafts);
            });
  }

  /**
   * Currently java-sync does not support discounted price sync. This workaround is to remove
   * discounted prices from syncing.
   *
   * <p>Issue: https://github.com/commercetools/commercetools-project-sync/issues/363
   */
  private List<ProductDraft> removeDiscountedFromPrices(List<ProductDraft> productDrafts) {
    return productDrafts
        .stream()
        .map(
            productDraft -> {
              List<ProductVariantDraft> productVariants =
                  productDraft
                      .getVariants()
                      .stream()
                      .map(this::createProductVariantDraftWithoutDiscounted)
                      .collect(Collectors.toList());
              ProductVariantDraft masterVariant = productDraft.getMasterVariant();
              ProductVariantDraft masterVariantDraft = null;
              if (masterVariant != null) {
                masterVariantDraft = createProductVariantDraftWithoutDiscounted(masterVariant);
              }
              return ProductDraftBuilder.of(productDraft)
                  .masterVariant(masterVariantDraft)
                  .variants(productVariants)
                  .build();
            })
        .collect(Collectors.toList());
  }

  private ProductVariantDraftDsl createProductVariantDraftWithoutDiscounted(
      ProductVariantDraft productVariantDraft) {
    List<PriceDraft> prices = productVariantDraft.getPrices();
    List<PriceDraft> priceDrafts = null;
    if (prices != null) {
      priceDrafts =
          prices
              .stream()
              .map(priceDraft -> PriceDraftBuilder.of(priceDraft).discounted(null).build())
              .collect(Collectors.toList());
    }
    return ProductVariantDraftBuilder.of(productVariantDraft).prices(priceDrafts).build();
  }

  @Nonnull
  private static Throwable getCompletionExceptionCause(@Nonnull final Throwable exception) {
    if (exception instanceof CompletionException) {
      return getCompletionExceptionCause(exception.getCause());
    }
    return exception;
  }

  @Nonnull
  @Override
  protected ProductProjectionQuery getQuery() {
    ProductProjectionQuery productQuery = ProductProjectionQuery.ofStaged();
    if (productSyncCustomRequest == null) {
      return productQuery;
    }
    if (null != productSyncCustomRequest.getLimit()) {
      productQuery = productQuery.withLimit(productSyncCustomRequest.getLimit());
    }
    if (null != productSyncCustomRequest.getWhere()) {
      productQuery =
          productQuery.withPredicates(QueryPredicate.of(productSyncCustomRequest.getWhere()));
    }

    return productQuery;
  }

  @Nonnull
  @Override
  protected Logger getLoggerInstance() {
    return LOGGER;
  }
}
