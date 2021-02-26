package com.commercetools.project.sync.category.service.impl;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.commercetools.project.sync.category.service.CategoryReferenceTransformService;
import com.commercetools.project.sync.service.impl.BaseServiceImpl;
import com.commercetools.project.sync.util.referenceresolution.CategoryReferenceResolutionUtils;
import com.commercetools.sync.commons.models.GraphQlQueryResources;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.Reference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public class CategoryReferenceTransformServiceImpl extends BaseServiceImpl
    implements CategoryReferenceTransformService {

  public CategoryReferenceTransformServiceImpl(@Nonnull final SphereClient ctpClient) {
    super(ctpClient);
  }

  @Nonnull
  @Override
  public CompletableFuture<List<CategoryDraft>> transformCategoryReferences(
      @Nonnull final List<Category> categories) {

    final List<CompletableFuture<Void>> transformReferencesToRunParallel = new ArrayList<>();
    transformReferencesToRunParallel.add(this.transformParentCategoryReference(categories));
    transformReferencesToRunParallel.add(this.transformCustomTypeReference(categories));

    return CompletableFuture.allOf(
            transformReferencesToRunParallel.toArray(new CompletableFuture[0]))
        .thenApply(
            ignore ->
                CategoryReferenceResolutionUtils.mapToCategoryDrafts(
                    categories, referenceIdToKeyCache.asMap()));
  }

  @Nonnull
  private CompletableFuture<Void> transformParentCategoryReference(
      @Nonnull final List<Category> categories) {

    final Set<String> parentCategoryIds =
        categories
            .stream()
            .map(Category::getParent)
            .filter(Objects::nonNull)
            .map(Reference::getId)
            .collect(Collectors.toSet());

    /*
    TODO: Review comment:
     https://github.com/commercetools/commercetools-project-sync/pull/240/files#r580147994

    I wonder if we could fill already-fetched category keys (as it's a native field of category)
    it means we could fill the cache with category id to category key without an extra query (or
    at least a minimum amount)... because a category could be a parent to another category or a
    child of another parent category, this way we could optimize the query if the parent and
    child are in the same batch/page (which is highly probable).

    The downside of this using extra memory (hash map) for fast search O(1), to improve
    the linear search on a category list 0(n). To not complex it, we could simply iterate and
    fill all category keys with their ids, which means more keys stored in the cache, which might
    not be an issue as the internal cache is fast and not putting much memory overhead.
    */
    return fetchAndFillReferenceIdToKeyCache(parentCategoryIds, GraphQlQueryResources.CATEGORIES);
  }

  @Nonnull
  private CompletableFuture<Void> transformCustomTypeReference(
      @Nonnull final List<Category> categories) {

    final Set<String> setOfTypeIds = new HashSet<>();
    setOfTypeIds.addAll(
        categories
            .stream()
            .map(Category::getCustom)
            .filter(Objects::nonNull)
            .map(customFields -> customFields.getType().getId())
            .collect(Collectors.toSet()));

    setOfTypeIds.addAll(
        categories
            .stream()
            .map(Category::getAssets)
            .map(
                assets ->
                    assets
                        .stream()
                        .filter(Objects::nonNull)
                        .map(Asset::getCustom)
                        .filter(Objects::nonNull)
                        .map(customFields -> customFields.getType().getId())
                        .collect(toList()))
            .flatMap(Collection::stream)
            .collect(toSet()));

    return fetchAndFillReferenceIdToKeyCache(setOfTypeIds, GraphQlQueryResources.TYPES);
  }
}
