package com.commercetools.project.sync.product;


import static com.commercetools.sync.commons.utils.SyncUtils.getReferenceWithKeyReplaced;
import static com.commercetools.sync.commons.utils.SyncUtils.getResourceIdentifierWithKeyReplaced;
import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.commercetools.project.sync.Syncer;
import com.commercetools.project.sync.service.CustomObjectService;
import com.commercetools.project.sync.service.ReferencesService;
import com.commercetools.project.sync.service.impl.CustomObjectServiceImpl;
import com.commercetools.project.sync.service.impl.ReferencesServiceImpl;
import com.commercetools.sync.commons.helpers.CategoryReferencePair;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.products.utils.ProductReferenceReplacementUtils;
import com.commercetools.sync.products.utils.VariantReferenceReplacementUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.expansion.ExpansionPath;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.AttributeContainer;
import io.sphere.sdk.products.CategoryOrderHints;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.attributes.Attribute;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.products.expansion.ProductExpansionModel;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.queries.QueryPredicate;
import io.sphere.sdk.states.State;
import io.sphere.sdk.taxcategories.TaxCategory;

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
  private final ReferencesService referencesService;

  /** Instantiates a {@link Syncer} instance. */
  private ProductSyncer(
      @Nonnull final ProductSync productSync,
      @Nonnull final SphereClient sourceClient,
      @Nonnull final SphereClient targetClient,
      @Nonnull final CustomObjectService customObjectService,
      @Nonnull final ReferencesService referencesService,
      @Nonnull final Clock clock) {
    super(productSync, sourceClient, targetClient, customObjectService, clock);
    this.referencesService = referencesService;
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
            .beforeUpdateCallback(ProductSyncer::interceptUpdate)
			.beforeCreateCallback(ProductSyncer::interceptCreate).build();
           

    final ProductSync productSync = new ProductSync(syncOptions);

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(targetClient);

    final ReferencesService referencesService = new ReferencesServiceImpl(sourceClient);

    return new ProductSyncer(
        productSync, sourceClient, targetClient, customObjectService, referencesService, clock);
  }
  
  	private static AttributeDraft getAttribute(List<AttributeDraft> attributes, String string) {
		AttributeDraft attribute = attributes.stream().filter(x -> "lastmodifiedatProductSource".equals(x.getName()))
				.findAny().orElse(null);

		return attribute;
	}
  
  	@Nonnull
	static ProductDraft interceptCreate(ProductDraft newProductDraft) {
		Set<AttributeDraft> attributes = newProductDraft.getMasterVariant().getAttributes().stream()
				.filter(t -> !t.getName().equalsIgnoreCase("lastmodifiedatProductSource")).collect(Collectors.toSet());
		newProductDraft.getMasterVariant().getAttributes().clear();
		newProductDraft.getMasterVariant().getAttributes().addAll(attributes);
			return newProductDraft;
	}

	@Nonnull
	static List<UpdateAction<Product>> interceptUpdate(@Nonnull final List<UpdateAction<Product>> updateActions,
			@Nonnull final ProductDraft srcProductDraft, @Nonnull final Product targetProduct) {
		AttributeDraft lastmodifiedatProductSourceAttr = getAttribute(
				srcProductDraft.getMasterVariant().getAttributes(), "lastmodifiedatProductSource");
		ZonedDateTime lastmodifiedatProductTarget = targetProduct.getLastModifiedAt();
		if (null != lastmodifiedatProductSourceAttr) {
			String lastmodifiedatProductSource = lastmodifiedatProductSourceAttr.getValue().asText();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss a z");
			ZonedDateTime dateTimeSource = ZonedDateTime.parse(lastmodifiedatProductSource, formatter);
			if (lastmodifiedatProductTarget.isAfter(dateTimeSource)) {
				updateActions.clear();
				return updateActions;
			}
		}
		
		Set<AttributeDraft> attributes = srcProductDraft.getMasterVariant().getAttributes().stream()
				.filter(t -> !t.getName().equalsIgnoreCase("lastmodifiedatProductSource")).collect(Collectors.toSet());
		srcProductDraft.getMasterVariant().getAttributes().clear();
		srcProductDraft.getMasterVariant().getAttributes().addAll(attributes);
		return updateActions;
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
        .thenApply(this::replaceProductsReferenceIdsWithKeys);
  }
  
  static ResourceIdentifier<ProductType> replaceProductTypeReferenceIdWithKey(@Nonnull final Product product) {
		final Reference<ProductType> productType = product.getProductType();
		return getResourceIdentifierWithKeyReplaced(productType,
				() -> ResourceIdentifier.ofId(productType.getObj().getKey()));
	}
  
  static ResourceIdentifier<TaxCategory> replaceTaxCategoryReferenceIdWithKey(@Nonnull final Product product) {

		final Reference<TaxCategory> productTaxCategory = product.getTaxCategory();
		return getResourceIdentifierWithKeyReplaced(productTaxCategory,
				() -> ResourceIdentifier.ofId(productTaxCategory.getObj().getKey()));
	}
  
  static Reference<State> replaceProductStateReferenceIdWithKey(@Nonnull final Product product) {
		final Reference<State> productState = product.getState();
		return getReferenceWithKeyReplaced(productState, () -> State.referenceOfId("in_progress"));
	}
  
	static CategoryReferencePair replaceCategoryReferencesIdsWithKeys(@Nonnull final Product product) {
		final Set<Reference<Category>> categoryReferences = product.getMasterData().getStaged().getCategories();
		final Set<ResourceIdentifier<Category>> categoryResourceIdentifiers = new HashSet<>();

		final CategoryOrderHints categoryOrderHints = product.getMasterData().getStaged().getCategoryOrderHints();
		final Map<String, String> categoryOrderHintsMapWithKeys = new HashMap<>();

		categoryReferences.forEach(categoryReference -> categoryResourceIdentifiers
				.add(getResourceIdentifierWithKeyReplaced(categoryReference, () -> {
					final String categoryId = categoryReference.getId();
					@SuppressWarnings("ConstantConditions") // NPE is checked in replaceReferenceIdWithKey.
					final String categoryKey = categoryReference.getObj().getKey();

					if (categoryOrderHints != null) {
						final String categoryOrderHintValue = categoryOrderHints.get(categoryId);
						if (categoryOrderHintValue != null) {
							categoryOrderHintsMapWithKeys.put(categoryKey, categoryOrderHintValue);
						}
					}
					return ResourceIdentifier.ofId(categoryKey);
				})));

		final CategoryOrderHints categoryOrderHintsWithKeys = categoryOrderHintsMapWithKeys.isEmpty()
				? categoryOrderHints
				: CategoryOrderHints.of(categoryOrderHintsMapWithKeys);
		return CategoryReferencePair.of(categoryResourceIdentifiers, categoryOrderHintsWithKeys);
	}
	
	/**
	 * 
	 * @param descriptionKey
	 */
	private void refactorAttributeKeyModel(AttributeDraft descriptionKey) {
		ArrayNode o = (ArrayNode) descriptionKey.getValue();
		 Iterator<JsonNode> child = o.elements();
		 while (child.hasNext()) {
			 ArrayNode childNode = (ArrayNode) child.next();
			 Iterator<JsonNode> subChild = childNode.elements();
			 while (subChild.hasNext()) {
				 ObjectNode  subChildNode = (ObjectNode ) subChild.next();
				 if(null != subChildNode.get("name") && (subChildNode.get("name").asText().equalsIgnoreCase("key") || subChildNode.get("name").asText().equalsIgnoreCase("productFamily"))
						 && subChildNode.get("value").getNodeType() == JsonNodeType.OBJECT) {
					 subChildNode.put("value", subChildNode.get("value").get("key"));
					 break;
				 }
			 }
		 }
	}

  
  @Nonnull
	public List<ProductDraft> replaceProductsReferenceIdsWithKeys(@Nonnull final List<Product> products) {
		return products.stream().filter(Objects::nonNull).map(product -> {
			final ProductDraft productDraft = ProductReferenceReplacementUtils.getDraftBuilderFromStagedProduct(product)
					.build();
			final ResourceIdentifier<ProductType> productTypeReferenceWithKey = replaceProductTypeReferenceIdWithKey(
					product);
			final ResourceIdentifier<TaxCategory> taxCategoryReferenceWithKey = replaceTaxCategoryReferenceIdWithKey(
					product);
			
			final Reference<State> stateReferenceWithKey = replaceProductStateReferenceIdWithKey(product);

			final CategoryReferencePair categoryReferencePair = replaceCategoryReferencesIdsWithKeys(product);
			final Set<ResourceIdentifier<Category>> categoryResourceIdentifiers = categoryReferencePair
					.getCategoryResourceIdentifiers();
			final CategoryOrderHints categoryOrderHintsWithKeys = categoryReferencePair.getCategoryOrderHints();

			final List<ProductVariant> allVariants = product.getMasterData().getStaged().getAllVariants();
			
			final List<ProductVariantDraft> variantDraftsWithKeys = VariantReferenceReplacementUtils
					.replaceVariantsReferenceIdsWithKeys(allVariants);
			
			//TODO
			//final List<ProductVariantDraft> replacedInvalidKeys = replaceInvalidKeys(variantDraftsWithKeys);
			
			//final ProductVariantDraft masterVariantDraftWithKeys = replacedInvalidKeys.remove(0);
			
			final ProductVariantDraft masterVariantDraftWithKeys = variantDraftsWithKeys.remove(0);

			//TODO
			AttributeDraft e = AttributeDraft.of("lastmodifiedatProductSource", product.getLastModifiedAt());
			masterVariantDraftWithKeys.getAttributes().add(e);

			updateStructure(masterVariantDraftWithKeys);			
			updatedRequiredApprovals(masterVariantDraftWithKeys);
			
			return ProductDraftBuilder.of(productDraft).masterVariant(masterVariantDraftWithKeys)
					.variants(variantDraftsWithKeys).productType(productTypeReferenceWithKey)
					.categories(categoryResourceIdentifiers).categoryOrderHints(categoryOrderHintsWithKeys)
					.taxCategory(taxCategoryReferenceWithKey).state(stateReferenceWithKey).build();
		}).collect(Collectors.toList());
	}

/**
 * @param masterVariantDraftWithKeys
 */
private void updatedRequiredApprovals(final ProductVariantDraft masterVariantDraftWithKeys) {
	AttributeDraft approVal = masterVariantDraftWithKeys.getAttributes().stream()
			.filter(x -> Objects.nonNull(x) && "approvalStatus-ProductMarketing".equals(x.getName())).findAny().orElse(null);
	if(null == approVal) {
		masterVariantDraftWithKeys.getAttributes().add(AttributeDraft.of("approvalStatus-ProductMarketing", "pending"));
	}
	
	approVal = masterVariantDraftWithKeys.getAttributes().stream()
			.filter(x -> Objects.nonNull(x) && "approvalStatus-DMG".equals(x.getName())).findAny().orElse(null);
	if(null == approVal) {
		masterVariantDraftWithKeys.getAttributes().add(AttributeDraft.of("approvalStatus-DMG", "pending"));
	}
	
	approVal = masterVariantDraftWithKeys.getAttributes().stream()
	.filter(x -> Objects.nonNull(x) && "approvalStatus-BillerEvergent".equals(x.getName())).findAny().orElse(null);
	if(null == approVal) {
		masterVariantDraftWithKeys.getAttributes().add(AttributeDraft.of("approvalStatus-BillerEvergent", "pending"));
	}
	
	approVal = masterVariantDraftWithKeys.getAttributes().stream()
	.filter(x -> Objects.nonNull(x) && "approvalStatus-OTTProductMarketing".equals(x.getName())).findAny().orElse(null);
	if(null == approVal) {
		masterVariantDraftWithKeys.getAttributes().add(AttributeDraft.of("approvalStatus-OTTProductMarketing", "pending"));
	}

	 approVal = masterVariantDraftWithKeys.getAttributes().stream()
				.filter(x -> Objects.nonNull(x) && "approvalStatus-CPOP".equals(x.getName())).findAny().orElse(null);
	if(null == approVal) {
		masterVariantDraftWithKeys.getAttributes().add(AttributeDraft.of("approvalStatus-CPOP", "pending"));
	}		
	 approVal = masterVariantDraftWithKeys.getAttributes().stream()
				.filter(x -> Objects.nonNull(x) && "approvalStatus-OpusSales".equals(x.getName())).findAny().orElse(null);
	if(null == approVal) {
		masterVariantDraftWithKeys.getAttributes().add(AttributeDraft.of("approvalStatus-OpusSales", "pending"));
	}
	
	 approVal = masterVariantDraftWithKeys.getAttributes().stream()
				.filter(x -> Objects.nonNull(x) && "approvalStatus-OpusServices".equals(x.getName())).findAny().orElse(null);
	if(null == approVal) {
		masterVariantDraftWithKeys.getAttributes().add(AttributeDraft.of("approvalStatus-OpusServices", "pending"));
	}
	
	 approVal = masterVariantDraftWithKeys.getAttributes().stream()
				.filter(x -> Objects.nonNull(x) && "approvalStatus-OnlineSales".equals(x.getName())).findAny().orElse(null);
	if(null == approVal) {
		masterVariantDraftWithKeys.getAttributes().add(AttributeDraft.of("approvalStatus-OnlineSales", "pending"));
	}
	
	 approVal = masterVariantDraftWithKeys.getAttributes().stream()
				.filter(x -> Objects.nonNull(x) && "approvalStatus-OnlineServices".equals(x.getName())).findAny().orElse(null);
	if(null == approVal) {
		masterVariantDraftWithKeys.getAttributes().add(AttributeDraft.of("approvalStatus-OnlineServices", "pending"));
	}
	
	 approVal = masterVariantDraftWithKeys.getAttributes().stream()
				.filter(x -> Objects.nonNull(x) && "approvalStatus-Biller".equals(x.getName())).findAny().orElse(null);
	if(null == approVal) {
		masterVariantDraftWithKeys.getAttributes().add(AttributeDraft.of("approvalStatus-Biller", "pending"));
	}
	
	 approVal = masterVariantDraftWithKeys.getAttributes().stream()
				.filter(x -> Objects.nonNull(x) && "approvalStatus-MarketingOperations".equals(x.getName())).findAny().orElse(null);
	if(null == approVal) {
		masterVariantDraftWithKeys.getAttributes().add(AttributeDraft.of("approvalStatus-MarketingOperations", "pending"));
	}
	
	 approVal = masterVariantDraftWithKeys.getAttributes().stream()
				.filter(x -> Objects.nonNull(x) && "approvalStatus-ConsumerOnlineSales".equals(x.getName())).findAny().orElse(null);
	if(null == approVal) {
		masterVariantDraftWithKeys.getAttributes().add(AttributeDraft.of("approvalStatus-ConsumerOnlineSales", "pending"));
	}
	
	 approVal = masterVariantDraftWithKeys.getAttributes().stream()
				.filter(x -> Objects.nonNull(x) && "approvalComments-ProductMarketing".equals(x.getName())).findAny().orElse(null);
	if(null == approVal) {
		masterVariantDraftWithKeys.getAttributes().add(AttributeDraft.of("approvalComments-ProductMarketing", "pending"));
	}
	
	 approVal = masterVariantDraftWithKeys.getAttributes().stream()
				.filter(x -> Objects.nonNull(x) && "approvalComments-CPOP".equals(x.getName())).findAny().orElse(null);
	if(null == approVal) {
		masterVariantDraftWithKeys.getAttributes().add(AttributeDraft.of("approvalComments-CPOP", "pending"));
	}
	
	 approVal = masterVariantDraftWithKeys.getAttributes().stream()
				.filter(x -> Objects.nonNull(x) && "approvalComments-OpusServices".equals(x.getName())).findAny().orElse(null);
	if(null == approVal) {
		masterVariantDraftWithKeys.getAttributes().add(AttributeDraft.of("approvalComments-OpusServices", "pending"));
	}
	
	 approVal = masterVariantDraftWithKeys.getAttributes().stream()
				.filter(x -> Objects.nonNull(x) && "approvalComments-OnlineSales".equals(x.getName())).findAny().orElse(null);
	if(null == approVal) {
		masterVariantDraftWithKeys.getAttributes().add(AttributeDraft.of("approvalComments-OnlineSales", "pending"));
	}
	
	approVal = masterVariantDraftWithKeys.getAttributes().stream()
			.filter(x -> Objects.nonNull(x) && "approvalComments-OnlineServices".equals(x.getName())).findAny().orElse(null);
	if(null == approVal) {
		masterVariantDraftWithKeys.getAttributes().add(AttributeDraft.of("approvalComments-OnlineServices", "pending"));
	}
	
	approVal = masterVariantDraftWithKeys.getAttributes().stream()
			.filter(x -> Objects.nonNull(x) && "approvalComments-Biller".equals(x.getName())).findAny().orElse(null);
	if(null == approVal) {
		masterVariantDraftWithKeys.getAttributes().add(AttributeDraft.of("approvalComments-Biller", "pending"));
	}
	
	approVal = masterVariantDraftWithKeys.getAttributes().stream()
			.filter(x -> Objects.nonNull(x) && "approvalComments-MarketingOperations".equals(x.getName())).findAny().orElse(null);
	if(null == approVal) {
		masterVariantDraftWithKeys.getAttributes().add(AttributeDraft.of("approvalComments-MarketingOperations", "pending"));
	}
	
	approVal = masterVariantDraftWithKeys.getAttributes().stream()
			.filter(x -> Objects.nonNull(x) && "approvalComments-ConsumerOnlineSales".equals(x.getName())).findAny().orElse(null);
	if(null == approVal) {
		masterVariantDraftWithKeys.getAttributes().add(AttributeDraft.of("approvalComments-ConsumerOnlineSales", "pending"));
	}
	
	approVal = masterVariantDraftWithKeys.getAttributes().stream()
			.filter(x -> Objects.nonNull(x) && "approvalStatus-OTTLegal".equals(x.getName())).findAny().orElse(null);
	if(null == approVal) {
		masterVariantDraftWithKeys.getAttributes().add(AttributeDraft.of("approvalStatus-OTTLegal", "pending"));
	}
	
	approVal = masterVariantDraftWithKeys.getAttributes().stream()
			.filter(x -> Objects.nonNull(x) && "approvalComments-OTTLegal".equals(x.getName())).findAny().orElse(null);
	if(null == approVal) {
		masterVariantDraftWithKeys.getAttributes().add(AttributeDraft.of("approvalComments-OTTLegal", "pending"));
	}
	
	approVal = masterVariantDraftWithKeys.getAttributes().stream()
			.filter(x -> Objects.nonNull(x) && "approvalComments-DMG".equals(x.getName())).findAny().orElse(null);
	if(null == approVal) {
		masterVariantDraftWithKeys.getAttributes().add(AttributeDraft.of("approvalComments-DMG", "pending"));
	}
	
	approVal = masterVariantDraftWithKeys.getAttributes().stream()
			.filter(x -> Objects.nonNull(x) && "approvalStatus-OTTProductManagement".equals(x.getName())).findAny().orElse(null);
	if(null == approVal) {
		masterVariantDraftWithKeys.getAttributes().add(AttributeDraft.of("approvalStatus-OTTProductManagement", "pending"));
	}
	
	approVal = masterVariantDraftWithKeys.getAttributes().stream()
			.filter(x -> Objects.nonNull(x) && "approvalComments-OTTProductManagement".equals(x.getName())).findAny().orElse(null);
	if(null == approVal) {
		masterVariantDraftWithKeys.getAttributes().add(AttributeDraft.of("approvalComments-OTTProductManagement", "pending"));
	}
	
	approVal = masterVariantDraftWithKeys.getAttributes().stream()
			.filter(x -> Objects.nonNull(x) && "approvalStatus-Finance".equals(x.getName())).findAny().orElse(null);
	if(null == approVal) {
		masterVariantDraftWithKeys.getAttributes().add(AttributeDraft.of("approvalStatus-Finance", "pending"));
	}
	
	approVal = masterVariantDraftWithKeys.getAttributes().stream()
			.filter(x -> Objects.nonNull(x) && "approvalComments-Finance".equals(x.getName())).findAny().orElse(null);
	if(null == approVal) {
		masterVariantDraftWithKeys.getAttributes().add(AttributeDraft.of("approvalComments-Finance", "pending"));
	}
	
	approVal = masterVariantDraftWithKeys.getAttributes().stream()
			.filter(x -> Objects.nonNull(x) && "approvalStatus-SuppliersPayments".equals(x.getName())).findAny().orElse(null);
	if(null == approVal) {
		masterVariantDraftWithKeys.getAttributes().add(AttributeDraft.of("approvalStatus-SuppliersPayments", "pending"));
	}
	
	approVal = masterVariantDraftWithKeys.getAttributes().stream()
			.filter(x -> Objects.nonNull(x) && "approvalComments-SuppliersPayments".equals(x.getName())).findAny().orElse(null);
	if(null == approVal) {
		masterVariantDraftWithKeys.getAttributes().add(AttributeDraft.of("approvalComments-SuppliersPayments", "pending"));
	}
	
	approVal = masterVariantDraftWithKeys.getAttributes().stream()
			.filter(x -> Objects.nonNull(x) && "approvalStatus-OTTAcqMkt".equals(x.getName())).findAny().orElse(null);
	if(null == approVal) {
		masterVariantDraftWithKeys.getAttributes().add(AttributeDraft.of("approvalStatus-OTTAcqMkt", "pending"));
	}
	
	approVal = masterVariantDraftWithKeys.getAttributes().stream()
			.filter(x -> Objects.nonNull(x) && "approvalComments-OTTAcqMkt".equals(x.getName())).findAny().orElse(null);
	if(null == approVal) {
		masterVariantDraftWithKeys.getAttributes().add(AttributeDraft.of("approvalComments-OTTAcqMkt", "pending"));
	}
	
	approVal = masterVariantDraftWithKeys.getAttributes().stream()
			.filter(x -> Objects.nonNull(x) && "approvalStatus-OTTRetentionMkt".equals(x.getName())).findAny().orElse(null);
	if(null == approVal) {
		masterVariantDraftWithKeys.getAttributes().add(AttributeDraft.of("approvalStatus-OTTRetentionMkt", "pending"));
	}
	
	approVal = masterVariantDraftWithKeys.getAttributes().stream()
			.filter(x -> Objects.nonNull(x) && "approvalComments-OTTRetentionMkt".equals(x.getName())).findAny().orElse(null);
	if(null == approVal) {
		masterVariantDraftWithKeys.getAttributes().add(AttributeDraft.of("approvalComments-OTTRetentionMkt", "pending"));
	}
}

/**
 * @param masterVariantDraftWithKeys
 */
private void updateStructure(final ProductVariantDraft masterVariantDraftWithKeys) {
	AttributeDraft descriptionKey = masterVariantDraftWithKeys.getAttributes().stream()
			.filter(x -> "descriptionsByKey".equals(x.getName())).findAny().orElse(null);

	AttributeDraft displayNameKey = masterVariantDraftWithKeys.getAttributes().stream()
			.filter(x -> "displayNamesByKey".equals(x.getName())).findAny().orElse(null);
	
	AttributeDraft associatedProducts = masterVariantDraftWithKeys.getAttributes().stream()
			.filter(x -> "associatedProducts".equals(x.getName())).findAny().orElse(null);

	Set<AttributeDraft> attributes = masterVariantDraftWithKeys.getAttributes().stream()
			.filter(t -> !t.getName().equalsIgnoreCase("descriptionsByKey")).collect(Collectors.toSet());
	attributes = attributes.stream()
			.filter(t -> !t.getName().equalsIgnoreCase("displayNamesByKey")).collect(Collectors.toSet());
	attributes = attributes.stream()
			.filter(t -> !t.getName().equalsIgnoreCase("associatedProducts")).collect(Collectors.toSet());
	masterVariantDraftWithKeys.getAttributes().clear();
	masterVariantDraftWithKeys.getAttributes().addAll(attributes);

	if(null != descriptionKey && null != descriptionKey.getValue()) {
		refactorAttributeKeyModel(descriptionKey);
	}
	if(null != displayNameKey && null != displayNameKey.getValue()) { 
		refactorAttributeKeyModel(displayNameKey);
	}
	if(null != associatedProducts && null != associatedProducts.getValue()) { 
		refactorAttributeKeyModel(associatedProducts);
	}
	if(null != descriptionKey) {
		masterVariantDraftWithKeys.getAttributes().add(descriptionKey);
	}
	if(null != displayNameKey) {
		masterVariantDraftWithKeys.getAttributes().add(displayNameKey);
	}
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

    final List<JsonNode> allAttributeReferences = getAllReferences(products);

    final List<JsonNode> allProductReferences =
        getReferencesByTypeId(allAttributeReferences, Product.referenceTypeId());

    final List<JsonNode> allCategoryReferences =
        getReferencesByTypeId(allAttributeReferences, Category.referenceTypeId());

    final List<JsonNode> allProductTypeReferences =
        getReferencesByTypeId(allAttributeReferences, ProductType.referenceTypeId());

    return this.referencesService
        .getIdToKeys(
            getIds(allProductReferences),
            getIds(allCategoryReferences),
            getIds(allProductTypeReferences))
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
  protected ProductQuery getQuery(String queryString) {
    // TODO: Eventually don't expand all references and cache references for replacement.
    // https://github.com/commercetools/commercetools-project-sync/issues/49
	  
	  if (null != queryString) {
			// final String queryPredicateStr = "masterData(current(name(en in (\"" +
			// queryString + "\"))))";
			final String queryPredicateStr = "key = \"" + queryString + "\"";
			final QueryPredicate<Product> queryPredicate = QueryPredicate.of(queryPredicateStr);
			return ProductQuery.of().withExpansionPaths(ProductExpansionModel::productType)
					.plusExpansionPaths(ProductExpansionModel::taxCategory)
					.plusExpansionPaths(ExpansionPath.of("state"))
					.plusExpansionPaths(expansionModel -> expansionModel.masterData().staged().categories())
					.plusExpansionPaths(
							expansionModel -> expansionModel.masterData().staged().allVariants().prices().channel())
					.plusExpansionPaths(ExpansionPath.of("masterData.staged.masterVariant.prices[*].custom.type"))
					.plusExpansionPaths(ExpansionPath.of("masterData.staged.variants[*].prices[*].custom.type"))
					.plusExpansionPaths(ExpansionPath.of("masterData.staged.masterVariant.assets[*].custom.type"))
					.plusExpansionPaths(ExpansionPath.of("masterData.staged.variants[*].assets[*].custom.type"))
					.plusPredicates(queryPredicate);

		} else {
			return ProductQuery.of().withExpansionPaths(ProductExpansionModel::productType)
					.plusExpansionPaths(ProductExpansionModel::taxCategory)
					.plusExpansionPaths(ExpansionPath.of("state"))
					.plusExpansionPaths(expansionModel -> expansionModel.masterData().staged().categories())
					.plusExpansionPaths(
							expansionModel -> expansionModel.masterData().staged().allVariants().prices().channel())
					.plusExpansionPaths(ExpansionPath.of("masterData.staged.masterVariant.prices[*].custom.type"))
					.plusExpansionPaths(ExpansionPath.of("masterData.staged.variants[*].prices[*].custom.type"))
					.plusExpansionPaths(ExpansionPath.of("masterData.staged.masterVariant.assets[*].custom.type"))
					.plusExpansionPaths(ExpansionPath.of("masterData.staged.variants[*].assets[*].custom.type"));

		}
	  
    /*return ProductQuery.of()
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
            ExpansionPath.of("masterData.staged.variants[*].assets[*].custom.type"));*/
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
  /*
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
  }*/
}
