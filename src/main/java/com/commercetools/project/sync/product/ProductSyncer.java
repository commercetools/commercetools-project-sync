package com.commercetools.project.sync.product;

import static com.commercetools.sync.products.utils.ProductReferenceReplacementUtils.replaceProductsReferenceIdsWithKeys;

import com.commercetools.project.sync.Syncer;
import com.commercetools.project.sync.service.CustomObjectService;
import com.commercetools.project.sync.service.ReferencesService;
import com.commercetools.project.sync.service.impl.CustomObjectServiceImpl;
import com.commercetools.project.sync.service.impl.ReferencesServiceImpl;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.expansion.ExpansionPath;
import io.sphere.sdk.products.AttributeContainer;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductCatalogData;
import io.sphere.sdk.products.ProductData;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.attributes.Attribute;
import io.sphere.sdk.products.commands.updateactions.Publish;
import io.sphere.sdk.products.commands.updateactions.Unpublish;
import io.sphere.sdk.products.expansion.ProductExpansionModel;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.producttypes.ProductType;
import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  /** Instantiates a {@link Syncer} instance. */
  private ProductSyncer(
      @Nonnull final ProductSync productSync,
      @Nonnull final SphereClient sourceClient,
      @Nonnull final SphereClient targetClient,
      @Nonnull final CustomObjectService customObjectService,
      @Nonnull final Clock clock) {
    super(productSync, sourceClient, targetClient, customObjectService, clock);
  }

  @Nonnull
  public static ProductSyncer of(
      @Nonnull final SphereClient sourceClient,
      @Nonnull final SphereClient targetClient,
      @Nonnull final Clock clock) {

    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(targetClient)
            .errorCallback(LOGGER::error)
            .warningCallback(LOGGER::warn)
            .beforeUpdateCallback(ProductSyncer::appendPublishIfPublished)
            .build();

    final ProductSync productSync = new ProductSync(syncOptions);

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(targetClient);

    return new ProductSyncer(productSync, sourceClient, targetClient, customObjectService, clock);
  }

  @Override
  @Nonnull
  protected CompletionStage<List<ProductDraft>> transform(@Nonnull final List<Product> page) {
    return replaceAttributeReferenceIdsWithKeys(page)
        .thenApply(aVoid -> replaceProductsReferenceIdsWithKeys(page));
  }

  @Nonnull
  private CompletionStage<Void> replaceAttributeReferenceIdsWithKeys(
      @Nonnull final List<Product> page) {

    final List<JsonNode> allAttributeReferences =
        page.stream()
            .map(Product::getMasterData)
            .map(ProductCatalogData::getStaged)
            .map(ProductData::getAllVariants)
            .map(ProductSyncer::getAttributeReferences)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

    final List<JsonNode> allProductReferences =
        getReferencesByTypeId(allAttributeReferences, Product.referenceTypeId());

    final List<JsonNode> allCategoryReferences =
        getReferencesByTypeId(allAttributeReferences, Category.referenceTypeId());

    final List<JsonNode> allProductTypeReferences =
        getReferencesByTypeId(allAttributeReferences, ProductType.referenceTypeId());

    final ReferencesService referencesService = new ReferencesServiceImpl(getSourceClient());

    return referencesService
        .getIdToKeys(
            getIds(allProductReferences),
            getIds(allCategoryReferences),
            getIds(allProductTypeReferences))
        .thenAccept(
            idToKey -> {
              replaceReferences(allProductReferences, idToKey);
              replaceReferences(allCategoryReferences, idToKey);
              replaceReferences(allProductTypeReferences, idToKey);
            });
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
  private List<JsonNode> getReferencesByTypeId(
      @Nonnull final List<JsonNode> references, @Nonnull final String typeId) {
    return references
        .stream()
        .filter(reference -> typeId.equals(reference.get(REFERENCE_TYPE_ID_FIELD).asText()))
        .collect(Collectors.toList());
  }

  @Nonnull
  private static Set<String> getIds(@Nonnull final List<JsonNode> references) {
    return references
        .stream()
        .map(ref -> ref.get(REFERENCE_ID_FIELD).asText())
        .collect(Collectors.toSet());
  }

  @Nonnull
  @Override
  protected ProductQuery getQuery() {
    // TODO: Eventually don't expand all references and cache references for replacement.
    return ProductQuery.of()
        .withExpansionPaths(ProductExpansionModel::productType)
        .plusExpansionPaths(ProductExpansionModel::taxCategory)
        .plusExpansionPaths(ExpansionPath.of("state"))
        .plusExpansionPaths(expansionModel -> expansionModel.masterData().staged().categories())
        .plusExpansionPaths(
            expansionModel -> expansionModel.masterData().staged().allVariants().prices().channel())
        .plusExpansionPaths(
            ExpansionPath.of("masterData.staged.masterVariant.prices[*].custom.type"))
        .plusExpansionPaths(ExpansionPath.of("masterData.staged.variants[*].prices[*].custom.type"))
        .plusExpansionPaths(
            ExpansionPath.of("masterData.staged.masterVariant.assets[*].custom.type"))
        .plusExpansionPaths(
            ExpansionPath.of("masterData.staged.variants[*].assets[*].custom.type"));
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
