package com.commercetools.project.sync.util.referenceresolution;

import static com.commercetools.project.sync.util.SyncUtils.getResourceIdentifierWithKey;
import static com.commercetools.project.sync.util.referenceresolution.AssetReferenceResolutionUtils.mapToAssetDrafts;
import static com.commercetools.project.sync.util.referenceresolution.CustomTypeReferenceResolutionUtils.mapToCustomFieldsDraft;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.types.Type;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

/**
 * Util class which provides utilities that can be used to resolve category reference when syncing
 * resources from a source commercetools project to a target one.
 */
public final class CategoryReferenceResolutionUtils {

  private CategoryReferenceResolutionUtils() {}

  /**
   * Returns an {@link List}&lt;{@link CategoryDraft}&gt; consisting of the results of applying the
   * mapping from {@link Category} to {@link CategoryDraft} with considering reference resolution.
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
   *       <td>parent</td>
   *       <td>{@link Reference}&lt;{@link Category}&gt;</td>
   *       <td>{@link ResourceIdentifier}&lt;{@link Category}&gt;</td>
   *     </tr>
   *     <tr>
   *        <td>custom.type</td>
   *        <td>{@link Reference}&lt;{@link Type}&gt;</td>
   *        <td>{@link ResourceIdentifier}&lt;{@link Type}&gt;</td>
   *     </tr>
   *     <tr>
   *        <td>asset.custom.type</td>
   *        <td>{@link Reference}&lt;{@link Type}&gt;</td>
   *        <td>{@link ResourceIdentifier}&lt;{@link Type}&gt;</td>
   *     </tr>
   *   </tbody>
   * </table>
   *
   * <p><b>Note:</b> The {@link Category} and {@link Type} references should be cached with a key.
   * Any reference that is not cached will have its id in place and not replaced by the key will be
   * considered as existing resources on the target commercetools project and the library will
   * issues an update/create API request without reference resolution.
   *
   * @param categories the categories with expanded references.
   * @return a {@link List} of {@link CategoryDraft} built from the supplied {@link List} of {@link
   *     Category}.
   */
  @Nonnull
  public static List<CategoryDraft> mapToCategoryDrafts(
      @Nonnull final List<Category> categories,
      @Nonnull final Map<String, String> referenceIdToKeyMap) {
    return categories
        .stream()
        .map(category -> mapToCategoryDraft(category, referenceIdToKeyMap))
        .collect(Collectors.toList());
  }

  @Nonnull
  private static CategoryDraft mapToCategoryDraft(
      @Nonnull final Category category, @Nonnull final Map<String, String> referenceIdToKeyMap) {
    return CategoryDraftBuilder.of(category)
        .custom(mapToCustomFieldsDraft(category, referenceIdToKeyMap))
        .assets(mapToAssetDrafts(category.getAssets(), referenceIdToKeyMap))
        .parent(getResourceIdentifierWithKey(category.getParent(), referenceIdToKeyMap))
        .build();
  }
}
