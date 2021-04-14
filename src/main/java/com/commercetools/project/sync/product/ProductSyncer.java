package com.commercetools.project.sync.product;

import static com.commercetools.project.sync.util.SyncUtils.logErrorCallback;
import static com.commercetools.project.sync.util.SyncUtils.logWarningCallback;
import static com.commercetools.sync.products.utils.ProductTransformUtils.toProductDrafts;

import com.commercetools.project.sync.Syncer;
import com.commercetools.project.sync.model.ProductSyncCustomRequest;
import com.commercetools.project.sync.product.service.ProductReferenceTransformService;
import com.commercetools.project.sync.product.service.impl.ProductReferenceTransformServiceImpl;
import com.commercetools.project.sync.service.CustomObjectService;
import com.commercetools.project.sync.service.impl.CustomObjectServiceImpl;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.commands.updateactions.Publish;
import io.sphere.sdk.products.commands.updateactions.Unpublish;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.queries.QueryPredicate;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProductSyncer
    extends Syncer<
        Product,
        ProductDraft,
        ProductSyncStatistics,
        ProductSyncOptions,
        ProductQuery,
        ProductSync> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProductSyncer.class);
  private static final String REFERENCE_TYPE_ID_FIELD = "typeId";
  private static final String REFERENCE_ID_FIELD = "id";
  private static final String WITH_IRRESOLVABLE_REFS_ERROR_MSG =
      "The product with id '%s' on the source project ('%s') will "
          + "not be synced because it has the following reference attribute(s): \n"
          + "%s.\nThese references are either pointing to a non-existent resource or to an existing one but with a blank key. "
          + "Please make sure these referenced resources are existing and have non-blank (i.e. non-null and non-empty) keys.";
  private final ProductReferenceTransformService referencesService;

  private final ProductSyncCustomRequest productSyncCustomRequest;

  /** Instantiates a {@link Syncer} instance. */
  private ProductSyncer(
      @Nonnull final ProductSync productSync,
      @Nonnull final SphereClient sourceClient,
      @Nonnull final SphereClient targetClient,
      @Nonnull final CustomObjectService customObjectService,
      @Nonnull final ProductReferenceTransformService referencesService,
      @Nonnull final Clock clock,
      @Nullable final ProductSyncCustomRequest productSyncCustomRequest) {
    super(productSync, sourceClient, targetClient, customObjectService, clock);
    this.referencesService = referencesService;
    this.productSyncCustomRequest = productSyncCustomRequest;
  }

  @Nonnull
  public static ProductSyncer of(
      @Nonnull final SphereClient sourceClient,
      @Nonnull final SphereClient targetClient,
      @Nonnull final Clock clock,
      @Nullable final ProductSyncCustomRequest productSyncCustomRequest) {

    final QuadConsumer<
            SyncException, Optional<ProductDraft>, Optional<Product>, List<UpdateAction<Product>>>
        logErrorCallback =
            (exception, newResourceDraft, oldResource, updateActions) ->
                logErrorCallback(LOGGER, "product", exception, oldResource, updateActions);
    final TriConsumer<SyncException, Optional<ProductDraft>, Optional<ProductProjection>>
        logWarningCallback =
            (exception, newResourceDraft, oldResource) ->
                logWarningCallback(LOGGER, "product", exception, oldResource);
    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(targetClient)
            .errorCallback(logErrorCallback)
            .warningCallback(logWarningCallback)
            .beforeUpdateCallback(ProductSyncer::appendPublishIfPublished)
            .build();

    final ProductSync productSync = new ProductSync(syncOptions);

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(targetClient);

    final ProductReferenceTransformService referencesService =
        new ProductReferenceTransformServiceImpl(sourceClient);

    return new ProductSyncer(
        productSync,
        sourceClient,
        targetClient,
        customObjectService,
        referencesService,
        clock,
        productSyncCustomRequest);
  }

  @Override
  @Nonnull
  protected CompletionStage<List<ProductDraft>> transform(@Nonnull final List<Product> page) {
    final ReferenceIdToKeyCache referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();
    return toProductDrafts(this.getSourceClient(), referenceIdToKeyCache, page);
  }

  @Nonnull
  @Override
  protected ProductQuery getQuery() {
    ProductSyncCustomRequest customRequest = this.productSyncCustomRequest;
    ProductQuery productQuery = ProductQuery.of();
    if (customRequest == null) {
      return productQuery;
    }
    if (null != customRequest.getLimit()) {
      productQuery = productQuery.withLimit(customRequest.getLimit());
    }
    if (null != customRequest.getWhere()) {
      productQuery = productQuery.withPredicates(QueryPredicate.of(customRequest.getWhere()));
    }

    return productQuery;
  }

  /**
   * Used for the beforeUpdateCallback of the sync. When an {@code targetProduct} is updated, this
   * method will add a {@link Publish} update action to the list of update actions, only if the
   * {@code targetProduct} has the published field set to true and has new update actions (not
   * containing a publish action nor an unpublish action). Which means that it will publish the
   * staged changes caused by the {@code updateActions} if it was already published.
   *
   * @param updateActions update actions needed to sync {@code srcProductDraft} to {@code
   *     targetProduct}.
   * @param srcProductDraft the source product draft with the changes.
   * @param targetProduct the target product to be updated.
   * @return the same list of update actions with a publish update action added, if there are staged
   *     changes that should be published.
   */
  @Nonnull
  static List<UpdateAction<Product>> appendPublishIfPublished(
      @Nonnull final List<UpdateAction<Product>> updateActions,
      @Nonnull final ProductDraft srcProductDraft,
      @Nonnull final Product targetProduct) {

    if (!updateActions.isEmpty()
        && targetProduct.getMasterData().isPublished()
        && doesNotContainPublishOrUnPublishActions(updateActions)) {

      updateActions.add(Publish.of());
    }

    return updateActions;
  }

  private static boolean doesNotContainPublishOrUnPublishActions(
      @Nonnull final List<UpdateAction<Product>> updateActions) {

    final Publish publishAction = Publish.of();
    final Unpublish unpublishAction = Unpublish.of();

    return updateActions
        .stream()
        .noneMatch(
            action ->
                publishAction.getAction().equals(action.getAction())
                    || unpublishAction.getAction().equals(action.getAction()));
  }
}
