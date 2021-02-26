package com.commercetools.project.sync.product;

import static com.commercetools.project.sync.util.SyncUtils.logErrorCallback;
import static com.commercetools.project.sync.util.SyncUtils.logWarningCallback;
import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;

import com.commercetools.project.sync.Syncer;
import com.commercetools.project.sync.product.service.ProductReferenceTransformService;
import com.commercetools.project.sync.product.service.impl.ProductReferenceTransformTransformServiceImpl;
import com.commercetools.project.sync.service.CustomObjectService;
import com.commercetools.project.sync.service.impl.CustomObjectServiceImpl;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.products.AttributeContainer;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.attributes.Attribute;
import io.sphere.sdk.products.commands.updateactions.Publish;
import io.sphere.sdk.products.commands.updateactions.Unpublish;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.producttypes.ProductType;
import java.time.Clock;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
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

  /** Instantiates a {@link Syncer} instance. */
  private ProductSyncer(
      @Nonnull final ProductSync productSync,
      @Nonnull final SphereClient sourceClient,
      @Nonnull final SphereClient targetClient,
      @Nonnull final CustomObjectService customObjectService,
      @Nonnull final ProductReferenceTransformService referencesService,
      @Nonnull final Clock clock) {
    super(productSync, sourceClient, targetClient, customObjectService, clock);
    this.referencesService = referencesService;
  }

  @Nonnull
  public static ProductSyncer of(
      @Nonnull final SphereClient sourceClient,
      @Nonnull final SphereClient targetClient,
      @Nonnull final Clock clock) {

    final QuadConsumer<
            SyncException, Optional<ProductDraft>, Optional<Product>, List<UpdateAction<Product>>>
        logErrorCallback =
            (exception, newResourceDraft, oldResource, updateActions) ->
                logErrorCallback(LOGGER, "product", exception, oldResource, updateActions);
    final TriConsumer<SyncException, Optional<ProductDraft>, Optional<Product>> logWarningCallback =
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
        new ProductReferenceTransformTransformServiceImpl(sourceClient);

    return new ProductSyncer(
        productSync, sourceClient, targetClient, customObjectService, referencesService, clock);
  }

  @Override
  @Nonnull
  protected CompletionStage<List<ProductDraft>> transform(@Nonnull final List<Product> page) {
    return replaceAttributeReferenceIdsWithKeys(page)
        .handle(
            (products, throwable) -> {
              if (throwable != null) {
                LOGGER.warn(
                    "Failed to replace referenced resource ids with keys on the attributes of the products in "
                        + "the current fetched page from the source project. This page will not be synced to the target "
                        + "project.",
                    getCompletionExceptionCause(throwable));
                return Collections.<Product>emptyList();
              }
              return products;
            })
        .thenCompose(this.referencesService::transformProductReferences);
  }

  @Nonnull
  private static Throwable getCompletionExceptionCause(@Nonnull final Throwable exception) {
    if (exception instanceof CompletionException) {
      return getCompletionExceptionCause(exception.getCause());
    }
    return exception;
  }

  /**
   * Replaces the ids on attribute references with keys. If a product has at least one irresolvable
   * reference, it will be filtered out and not returned in the new list.
   *
   * <p>Note: this method mutates the products passed by changing the reference keys with ids.
   *
   * @param products the products to replace the reference attributes ids with keys on.
   * @return a new list which contains only products which have all their attributes references
   *     resolvable and already replaced with keys.
   */
  @Nonnull
  private CompletionStage<List<Product>> replaceAttributeReferenceIdsWithKeys(
      @Nonnull final List<Product> products) {

    // TODO (CTPI-432): Those calls below should be part of the mapTo methods in java-sync later.
    final List<JsonNode> allAttributeReferences = getAllReferences(products);

    final List<JsonNode> allProductReferences =
        getReferencesByTypeId(allAttributeReferences, Product.referenceTypeId());

    final List<JsonNode> allCategoryReferences =
        getReferencesByTypeId(allAttributeReferences, Category.referenceTypeId());

    final List<JsonNode> allProductTypeReferences =
        getReferencesByTypeId(allAttributeReferences, ProductType.referenceTypeId());

    final List<JsonNode> allCustomObjectReferences =
        getReferencesByTypeId(allAttributeReferences, CustomObject.referenceTypeId());

    return this.referencesService
        .getIdToKeys(
            getIds(allProductReferences),
            getIds(allCategoryReferences),
            getIds(allProductTypeReferences),
            getIds(allCustomObjectReferences))
        .thenApply(
            idToKey -> {
              final List<Product> validProducts =
                  filterOutWithIrresolvableReferences(products, idToKey);
              replaceReferences(getAllReferences(validProducts), idToKey);
              return validProducts;
            });
  }

  @Nonnull
  private List<JsonNode> getAllReferences(@Nonnull final List<Product> products) {
    return products
        .stream()
        .map(this::getAllReferences)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  @Nonnull
  private List<JsonNode> getAllReferences(@Nonnull final Product product) {
    final List<ProductVariant> allVariants = product.getMasterData().getStaged().getAllVariants();
    return getAttributeReferences(allVariants);
  }

  @Nonnull
  private static List<JsonNode> getAttributeReferences(
      @Nonnull final List<ProductVariant> variants) {

    return variants
        .stream()
        .map(AttributeContainer::getAttributes)
        .flatMap(Collection::stream)
        .map(Attribute::getValueAsJsonNode)
        .map(ProductSyncer::getReferences)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  @Nonnull
  private static List<JsonNode> getReferences(@Nonnull final JsonNode attributeValue) {
    // This will only work if the reference is not expanded, otherwise behaviour is not guaranteed.
    return attributeValue.findParents(REFERENCE_TYPE_ID_FIELD);
  }

  @Nonnull
  private List<JsonNode> getReferencesByTypeId(
      @Nonnull final List<JsonNode> references, @Nonnull final String typeId) {
    return references
        .stream()
        .filter(reference -> typeId.equals(reference.get(REFERENCE_TYPE_ID_FIELD).asText()))
        .collect(Collectors.toList());
  }

  @Nonnull
  private static Set<String> getIds(@Nonnull final List<JsonNode> references) {
    return references.stream().map(ProductSyncer::getId).collect(toSet());
  }

  @Nonnull
  private static String getId(@Nonnull final JsonNode ref) {
    return ref.get(REFERENCE_ID_FIELD).asText();
  }

  @Nonnull
  private List<Product> filterOutWithIrresolvableReferences(
      @Nonnull final List<Product> products, @Nonnull final Map<String, String> idToKey) {

    return products
        .stream()
        .filter(
            product -> {
              final Set<JsonNode> irresolvableReferences =
                  getIrresolvableReferences(product, idToKey);
              final boolean hasIrresolvableReferences = !irresolvableReferences.isEmpty();
              if (hasIrresolvableReferences) {
                LOGGER.warn(
                    format(
                        WITH_IRRESOLVABLE_REFS_ERROR_MSG,
                        product.getId(),
                        getSourceClient().getConfig().getProjectKey(),
                        irresolvableReferences));
              }
              return !hasIrresolvableReferences;
            })
        .collect(Collectors.toList());
  }

  private Set<JsonNode> getIrresolvableReferences(
      @Nonnull final Product product, @Nonnull final Map<String, String> idToKey) {

    return getAllReferences(product)
        .stream()
        .filter(reference -> !idToKey.containsKey(getId(reference)))
        .collect(toSet());
  }

  private static void replaceReferences(
      @Nonnull final List<JsonNode> references, @Nonnull final Map<String, String> idToKey) {

    references.forEach(
        reference -> {
          final String id = reference.get(REFERENCE_ID_FIELD).asText();
          final String key = idToKey.get(id);
          ((ObjectNode) reference).put(REFERENCE_ID_FIELD, key);
        });
  }

  @Nonnull
  @Override
  protected ProductQuery getQuery() {
    return ProductQuery.of();
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
