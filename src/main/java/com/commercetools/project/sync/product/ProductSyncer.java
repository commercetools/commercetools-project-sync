package com.commercetools.project.sync.product;

import com.commercetools.project.sync.Syncer;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.Publish;
import io.sphere.sdk.products.commands.updateactions.Unpublish;
import io.sphere.sdk.products.queries.ProductQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.List;

import static com.commercetools.sync.products.utils.ProductReferenceReplacementUtils.buildProductQuery;
import static com.commercetools.sync.products.utils.ProductReferenceReplacementUtils.replaceProductsReferenceIdsWithKeys;

public final class ProductSyncer
    extends Syncer<
        Product,
        ProductDraft,
        ProductSyncStatistics,
        ProductSyncOptions,
        ProductQuery,
        ProductSync> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProductSyncer.class);

  /** Instantiates a {@link Syncer} instance. */
  private ProductSyncer(
      @Nonnull final ProductSync productSync,
      @Nonnull final ProductQuery productQuery,
      @Nonnull final SphereClient sourceClient,
      @Nonnull final SphereClient targetClient) {
    super(productSync, productQuery, sourceClient, targetClient);
  }

  @Nonnull
  public static ProductSyncer of(
      @Nonnull final SphereClient sourceClient, @Nonnull final SphereClient targetClient) {

    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(targetClient)
            .errorCallback(LOGGER::error)
            .warningCallback(LOGGER::warn)
            .build();

    final ProductSync productSync = new ProductSync(syncOptions);

    return new ProductSyncer(productSync, buildProductQuery(), sourceClient, targetClient);
    // TODO: Instead of reference expansion, we could cache all keys and replace references
    // manually.
  }

  @Override
  @Nonnull
  protected List<ProductDraft> transformResourcesToDrafts(@Nonnull final List<Product> page) {
    return replaceProductsReferenceIdsWithKeys(page);
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

    final Publish publishAction = Publish.of();
    final Unpublish unpublishAction = Unpublish.of();

    // Only if there are new updates and the target product is already published
    if (!updateActions.isEmpty() && targetProduct.getMasterData().isPublished()) {

      // Only if there is no Publish and Unpublish action in those updates
      if (updateActions
          .stream()
          .noneMatch(
              action ->
                  publishAction.getAction().equals(action.getAction())
                      || unpublishAction.getAction().equals(action.getAction()))) {

        updateActions.add(publishAction);
      }
    }
    return updateActions;
  }
}
