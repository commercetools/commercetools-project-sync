package com.commercetools.project.sync.producttype;

import static com.commercetools.project.sync.util.SyncUtils.logErrorCallback;
import static com.commercetools.project.sync.util.SyncUtils.logWarningCallback;

import com.commercetools.api.client.ByProjectKeyProductTypesGet;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypePagedQueryResponse;
import com.commercetools.api.models.product_type.ProductTypeUpdateAction;
import com.commercetools.project.sync.Syncer;
import com.commercetools.project.sync.service.CustomObjectService;
import com.commercetools.project.sync.service.impl.CustomObjectServiceImpl;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.producttypes.ProductTypeSync;
import com.commercetools.sync.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.producttypes.ProductTypeSyncOptionsBuilder;
import com.commercetools.sync.producttypes.helpers.ProductTypeSyncStatistics;
import com.commercetools.sync.producttypes.utils.ProductTypeTransformUtils;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This class compiles but not tested yet
// TODO: Test class and adjust logic if needed
public final class ProductTypeSyncer
    extends Syncer<
        ProductType,
        ProductTypeUpdateAction,
        ProductTypeDraft,
        ProductTypeSyncStatistics,
        ProductTypeSyncOptions,
        ByProjectKeyProductTypesGet,
        ProductTypePagedQueryResponse,
        ProductTypeSync> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProductTypeSyncer.class);

  /** Instantiates a {@link Syncer} instance. */
  private ProductTypeSyncer(
      @Nonnull final ProductTypeSync productTypeSync,
      @Nonnull final ProjectApiRoot sourceClient,
      @Nonnull final ProjectApiRoot targetClient,
      @Nonnull final CustomObjectService customObjectService,
      @Nonnull final Clock clock) {
    super(productTypeSync, sourceClient, targetClient, customObjectService, clock);
  }

  @Nonnull
  public static ProductTypeSyncer of(
      @Nonnull final ProjectApiRoot sourceClient,
      @Nonnull final ProjectApiRoot targetClient,
      @Nonnull final Clock clock) {

    final QuadConsumer<
            SyncException,
            Optional<ProductTypeDraft>,
            Optional<ProductType>,
            List<ProductTypeUpdateAction>>
        logErrorCallback =
            (exception, newResourceDraft, oldResource, updateActions) ->
                logErrorCallback(LOGGER, "product type", exception, oldResource, updateActions);
    final TriConsumer<SyncException, Optional<ProductTypeDraft>, Optional<ProductType>>
        logWarningCallback =
            (exception, newResourceDraft, oldResource) ->
                logWarningCallback(LOGGER, "product type", exception, oldResource);
    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(targetClient)
            .errorCallback(logErrorCallback)
            .warningCallback(logWarningCallback)
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
    return ProductTypeTransformUtils.toProductTypeDrafts(
        getSourceClient(), referenceIdToKeyCache, page);
  }

  @Nonnull
  @Override
  protected ByProjectKeyProductTypesGet getQuery() {
    return getSourceClient().productTypes().get();
  }

  @Nonnull
  @Override
  protected Logger getLoggerInstance() {
    return LOGGER;
  }
}
