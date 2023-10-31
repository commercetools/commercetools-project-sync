package com.commercetools.project.sync.product;

import static com.commercetools.project.sync.util.SyncUtils.IDENTIFIER_NOT_PRESENT;
import static com.commercetools.project.sync.util.SyncUtils.getCompletionExceptionCause;
import static com.commercetools.project.sync.util.SyncUtils.logErrorCallback;
import static com.commercetools.project.sync.util.SyncUtils.logWarningCallback;
import static com.commercetools.sync.products.utils.ProductTransformUtils.toProductDrafts;

import com.commercetools.api.client.ByProjectKeyProductProjectionsGet;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.WithKey;
import com.commercetools.api.models.common.DiscountedPriceDraft;
import com.commercetools.api.models.common.PriceDraft;
import com.commercetools.api.models.common.PriceDraftBuilder;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductDraftBuilder;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductProjectionPagedQueryResponse;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.product.ProductVariantDraftBuilder;
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
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProductSyncer
    extends Syncer<
        ProductProjection,
        ProductUpdateAction,
        ProductDraft,
        ProductSyncStatistics,
        ProductSyncOptions,
        ByProjectKeyProductProjectionsGet,
        ProductProjectionPagedQueryResponse,
        ProductSync> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProductSyncer.class);

  private final ProductSyncCustomRequest productSyncCustomRequest;

  /** Instantiates a {@link Syncer} instance. */
  private ProductSyncer(
      @Nonnull final ProductSync productSync,
      @Nonnull final ProjectApiRoot sourceClient,
      @Nonnull final ProjectApiRoot targetClient,
      @Nonnull final CustomObjectService customObjectService,
      @Nonnull final Clock clock,
      @Nullable final ProductSyncCustomRequest productSyncCustomRequest) {
    super(productSync, sourceClient, targetClient, customObjectService, clock);
    this.productSyncCustomRequest = productSyncCustomRequest;
  }

  @Nonnull
  public static ProductSyncer of(
      @Nonnull final ProjectApiRoot sourceClient,
      @Nonnull final ProjectApiRoot targetClient,
      @Nonnull final Clock clock,
      @Nullable final ProductSyncCustomRequest productSyncCustomRequest) {

    final QuadConsumer<
            SyncException,
            Optional<ProductDraft>,
            Optional<ProductProjection>,
            List<ProductUpdateAction>>
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
  private List<ProductDraft> removeDiscountedFromPrices(
      @Nonnull final List<ProductDraft> productDrafts) {
    return productDrafts.stream()
        .map(
            productDraft -> {
              final List<ProductVariantDraft> productVariants =
                  productDraft.getVariants().stream()
                      .map(this::createProductVariantDraftWithoutDiscounted)
                      .collect(Collectors.toList());
              final ProductVariantDraft masterVariant = productDraft.getMasterVariant();
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

  private ProductVariantDraft createProductVariantDraftWithoutDiscounted(
      @Nonnull final ProductVariantDraft productVariantDraft) {
    final List<PriceDraft> prices = productVariantDraft.getPrices();
    List<PriceDraft> priceDrafts = null;
    if (prices != null) {
      priceDrafts =
          prices.stream()
              .map(
                  priceDraft ->
                      PriceDraftBuilder.of(priceDraft)
                          .discounted((DiscountedPriceDraft) null)
                          .build())
              .collect(Collectors.toList());
    }
    return ProductVariantDraftBuilder.of(productVariantDraft).prices(priceDrafts).build();
  }

  @Nonnull
  @Override
  protected ByProjectKeyProductProjectionsGet getQuery() {
    ByProjectKeyProductProjectionsGet productProjectionsGet =
        getSourceClient().productProjections().get().addStaged(true);
    if (productSyncCustomRequest == null) {
      return productProjectionsGet;
    }
    if (null != productSyncCustomRequest.getLimit()) {
      productProjectionsGet = productProjectionsGet.withLimit(productSyncCustomRequest.getLimit());
    }
    if (null != productSyncCustomRequest.getWhere()) {
      productProjectionsGet = productProjectionsGet.withWhere(productSyncCustomRequest.getWhere());
    }

    return productProjectionsGet;
  }

  @Nonnull
  @Override
  protected Logger getLoggerInstance() {
    return LOGGER;
  }
}
