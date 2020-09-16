package com.commercetools.project.sync.producttype;

import static java.lang.String.format;

import com.commercetools.project.sync.Syncer;
import com.commercetools.project.sync.service.CustomObjectService;
import com.commercetools.project.sync.service.impl.CustomObjectServiceImpl;
import com.commercetools.sync.producttypes.ProductTypeSync;
import com.commercetools.sync.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.producttypes.ProductTypeSyncOptionsBuilder;
import com.commercetools.sync.producttypes.helpers.ProductTypeSyncStatistics;
import com.commercetools.sync.producttypes.utils.ProductTypeReferenceResolutionUtils;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProductTypeSyncer
    extends Syncer<
        ProductType,
        ProductTypeDraft,
        ProductTypeSyncStatistics,
        ProductTypeSyncOptions,
        ProductTypeQuery,
        ProductTypeSync> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProductTypeSyncer.class);

  /** Instantiates a {@link Syncer} instance. */
  private ProductTypeSyncer(
      @Nonnull final ProductTypeSync productTypeSync,
      @Nonnull final SphereClient sourceClient,
      @Nonnull final SphereClient targetClient,
      @Nonnull final CustomObjectService customObjectService,
      @Nonnull final Clock clock) {
    super(productTypeSync, sourceClient, targetClient, customObjectService, clock);
  }

  @Nonnull
  public static ProductTypeSyncer of(
      @Nonnull final SphereClient sourceClient,
      @Nonnull final SphereClient targetClient,
      @Nonnull final Clock clock) {

    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(targetClient)
            .errorCallback(
                (exception, newResourceDraft, oldResource, updateActions) -> {
                  LOGGER.error(
                      format(
                          "Error when trying to sync product types. Existing product type key: %s. Update actions: %s",
                          oldResource.map(ProductType::getKey).orElse(""),
                          updateActions
                              .stream()
                              .map(Object::toString)
                              .collect(Collectors.joining(","))),
                      exception);
                })
            .warningCallback(
                (exception, newResourceDraft, oldResource) -> {
                  LOGGER.warn(
                      format(
                          "Warning when trying to sync product types. Existing product type key: %s",
                          oldResource.map(ProductType::getKey).orElse("")),
                      exception);
                })
            .build();

    final ProductTypeSync productTypeSync = new ProductTypeSync(syncOptions);

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(targetClient);

    return new ProductTypeSyncer(
        productTypeSync, sourceClient, targetClient, customObjectService, clock);
  }

  @Nonnull
  @Override
  protected CompletionStage<List<ProductTypeDraft>> transform(
      @Nonnull final List<ProductType> page) {
    return CompletableFuture.completedFuture(
        ProductTypeReferenceResolutionUtils.mapToProductTypeDrafts(page));
  }

  @Nonnull
  @Override
  protected ProductTypeQuery getQuery() {
    // TODO: Set depth need to be configurable.
    // https://github.com/commercetools/commercetools-project-sync/issues/44
    return ProductTypeReferenceResolutionUtils.buildProductTypeQuery(1);
  }
}
