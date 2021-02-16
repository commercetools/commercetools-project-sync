package com.commercetools.project.sync.util.referenceresolution;

import static java.util.stream.Collectors.toList;

import com.commercetools.sync.commons.helpers.CategoryReferencePair;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.models.WithKey;
import io.sphere.sdk.products.CategoryOrderHints;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductData;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.attributes.Attribute;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.states.State;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.types.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Util class which provides utilities that can be used when syncing resources from a source
 * commercetools project to a target one.
 */
public final class ProductReferenceResolutionUtils {

  /**
   * Returns an {@link List}&lt;{@link ProductDraft}&gt; consisting of the results of applying the
   * mapping from {@link Product} to {@link ProductDraft} with considering reference resolution.
   *
   * <table summary="Mapping of Reference fields for the reference resolution">
   *   <thead>
   *     <tr>
   *       <th>Reference field</th>
   *       <th>from</th>
   *       <th>to</th>
   *     </tr>
   *   </thead>
   *   <tbody>
   *     <tr>
   *       <td>productType</td>
   *       <td>{@link Reference}&lt;{@link ProductType}&gt;</td>
   *       <td>{@link ResourceIdentifier}&lt;{@link ProductType}&gt;</td>
   *     </tr>
   *     <tr>
   *        <td>categories</td>
   *        <td>{@link Set}&lt;{@link Reference}&lt;{@link Category}&gt;&gt;</td>
   *        <td>{@link Set}&lt;{@link ResourceIdentifier}&lt;{@link Category}&gt;&gt;</td>
   *     </tr>
   *     <tr>
   *        <td>variants.prices.channel</td>
   *        <td>{@link Reference}&lt;{@link Channel}&gt;</td>
   *        <td>{@link ResourceIdentifier}&lt;{@link Channel}&gt;</td>
   *     </tr>
   *     <tr>
   *        <td>variants.prices.customerGroup *</td>
   *        <td>{@link Reference}&lt;{@link CustomerGroup}&gt;</td>
   *        <td>{@link Reference}&lt;{@link CustomerGroup}&gt; (with key replaced with id field)</td>
   *     </tr>
   *     <tr>
   *        <td>variants.prices.custom.type</td>
   *        <td>{@link Reference}&lt;{@link Type}&gt;</td>
   *        <td>{@link ResourceIdentifier}&lt;{@link Type}&gt;</td>
   *     </tr>
   *     <tr>
   *        <td>variants.assets.custom.type</td>
   *        <td>{@link Reference}&lt;{@link Type}&gt;</td>
   *        <td>{@link ResourceIdentifier}&lt;{@link Type}&gt;</td>
   *     </tr>
   *     <tr>
   *        <td>variants.attributes on {@link List}&lt;{@link Attribute} *</td>
   *        <td>{@link Reference}&lt;{@link ProductType}&gt; (example for ProductType)</td>
   *        <td>{@link Reference}&lt;{@link ProductType}&gt; (with key replaced with id field)</td>
   *     </tr>
   *     <tr>
   *        <td>taxCategory</td>
   *        <td>{@link Reference}&lt;{@link TaxCategory}&gt;</td>
   *        <td>{@link ResourceIdentifier}&lt;{@link TaxCategory}&gt;</td>
   *     </tr>
   *     <tr>
   *        <td>state *</td>
   *        <td>{@link Reference}&lt;{@link State}&gt;</td>
   *        <td>{@link ResourceIdentifier}&lt;{@link State}&gt;</td>
   *     </tr>
   *   </tbody>
   * </table>
   *
   * <p><b>Note:</b> The aforementioned references should be cached with a key. Any reference that
   * is not cached will have its id in place and not replaced by the key will be considered as
   * existing resources on the target commercetools project and the library will issues an
   * update/create API request without reference resolution.
   *
   * @param products the products with references resolution.
   * @param referenceIdToKeyMap the map containing the cached id to key values.
   * @return a {@link List} of {@link ProductDraft} built from the supplied {@link List} of {@link
   *     Product}.
   */
  @Nonnull
  public static List<ProductDraft> mapToProductDrafts(
      @Nonnull final List<Product> products, Map<String, String> referenceIdToKeyMap) {
    return products
        .stream()
        .filter(Objects::nonNull)
        .map(
            product -> {
              final ProductDraft productDraft = getDraftBuilderFromStagedProduct(product).build();

              final CategoryReferencePair categoryReferencePair =
                  mapToCategoryReferencePair(product, referenceIdToKeyMap);
              final Set<ResourceIdentifier<Category>> categoryResourceIdentifiers =
                  categoryReferencePair.getCategoryResourceIdentifiers();
              final CategoryOrderHints categoryOrderHintsWithKeys =
                  categoryReferencePair.getCategoryOrderHints();

              final List<ProductVariant> allVariants =
                  product.getMasterData().getStaged().getAllVariants();
              final List<ProductVariantDraft> variantDraftsWithKeys =
                  VariantReferenceResolutionUtils.mapToProductVariantDrafts(
                      allVariants, referenceIdToKeyMap);
              final ProductVariantDraft masterVariantDraftWithKeys =
                  variantDraftsWithKeys.remove(0);

              return ProductDraftBuilder.of(productDraft)
                  .masterVariant(masterVariantDraftWithKeys)
                  .variants(variantDraftsWithKeys)
                  .productType(
                      getResourceIdentifierWithKey(product.getProductType(), referenceIdToKeyMap))
                  .categories(categoryResourceIdentifiers)
                  .categoryOrderHints(categoryOrderHintsWithKeys)
                  .taxCategory(
                      getResourceIdentifierWithKey(product.getTaxCategory(), referenceIdToKeyMap))
                  .state(getResourceIdentifierWithKey(product.getState(), referenceIdToKeyMap))
                  .build();
            })
        .collect(Collectors.toList());
  }

  /**
   * Given a reference to a resource of type {@code T}, this method checks if the reference is found
   * in cache. If it is, then it return the resource identifier with key. Otherwise, it returns the
   * resource identifier with id. Since, the reference could be {@code null}, this method could also
   * return null if the reference was not expanded and cached.
   *
   * @param reference the reference of the resource to check if it's expanded.
   * @param <T> the type of the resource.
   * @param referenceIdToKeyMap the map containing the cached id to key values.
   * @return returns the resource identifier with key if the {@code reference} was found in cache.
   *     Otherwise, it returns the resource identifier with id.
   */
  @Nullable
  public static <T extends WithKey> ResourceIdentifier<T> getResourceIdentifierWithKey(
      @Nullable final Reference<T> reference, Map<String, String> referenceIdToKeyMap) {

    if (reference != null) {
      final String id = reference.getId();
      if (referenceIdToKeyMap.containsKey(id)) {
        return ResourceIdentifier.ofKey(referenceIdToKeyMap.get(id));
      }
      return ResourceIdentifier.ofId(id);
    }

    return null;
  }

  /**
   * Given a {@link Product} this method creates a {@link ProductDraftBuilder} based on the staged
   * projection values of the supplied product.
   *
   * @param product the product to create a {@link ProductDraftBuilder} based on it's staged data.
   * @return a {@link ProductDraftBuilder} based on the staged projection values of the supplied
   *     product.
   */
  @Nonnull
  public static ProductDraftBuilder getDraftBuilderFromStagedProduct(
      @Nonnull final Product product) {
    final ProductData productData = product.getMasterData().getStaged();
    final List<ProductVariantDraft> allVariants =
        productData
            .getAllVariants()
            .stream()
            .map(productVariant -> ProductVariantDraftBuilder.of(productVariant).build())
            .collect(toList());
    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of(product.getMasterData().getStaged().getMasterVariant())
            .build();

    return ProductDraftBuilder.of(
            product.getProductType(), productData.getName(), productData.getSlug(), allVariants)
        .masterVariant(masterVariant)
        .metaDescription(productData.getMetaDescription())
        .metaKeywords(productData.getMetaKeywords())
        .metaTitle(productData.getMetaTitle())
        .description(productData.getDescription())
        .searchKeywords(productData.getSearchKeywords())
        .taxCategory(product.getTaxCategory())
        .state(product.getState())
        .key(product.getKey())
        .publish(product.getMasterData().isPublished())
        .categories(new ArrayList<>(productData.getCategories()))
        .categoryOrderHints(productData.getCategoryOrderHints());
  }

  @Nonnull
  static CategoryReferencePair mapToCategoryReferencePair(
      @Nonnull final Product product, Map<String, String> referenceIdToKeyMap) {
    final Set<Reference<Category>> categoryReferences =
        product.getMasterData().getStaged().getCategories();
    final Set<ResourceIdentifier<Category>> categoryResourceIdentifiers = new HashSet<>();

    final CategoryOrderHints categoryOrderHints =
        product.getMasterData().getStaged().getCategoryOrderHints();
    final Map<String, String> categoryOrderHintsMapWithKeys = new HashMap<>();

    categoryReferences.forEach(
        categoryReference -> {
          if (categoryReference != null) {
            final String categoryId = categoryReference.getId();
            if (referenceIdToKeyMap.containsKey(categoryId)) {
              final String categoryKey = referenceIdToKeyMap.get(categoryId);

              if (categoryOrderHints != null) {
                final String categoryOrderHintValue = categoryOrderHints.get(categoryId);
                if (categoryOrderHintValue != null) {
                  categoryOrderHintsMapWithKeys.put(categoryKey, categoryOrderHintValue);
                }
              }
              categoryResourceIdentifiers.add(ResourceIdentifier.ofKey(categoryKey));
            } else {
              categoryResourceIdentifiers.add(ResourceIdentifier.ofId(categoryReference.getId()));
            }
          }
        });

    final CategoryOrderHints categoryOrderHintsWithKeys =
        categoryOrderHintsMapWithKeys.isEmpty()
            ? categoryOrderHints
            : CategoryOrderHints.of(categoryOrderHintsMapWithKeys);
    return CategoryReferencePair.of(categoryResourceIdentifiers, categoryOrderHintsWithKeys);
  }

  private ProductReferenceResolutionUtils() {}
}
