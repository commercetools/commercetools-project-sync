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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
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

    final List<CompletableFuture<List<Category>>> transformReferencesToRunParallel =
        new ArrayList<>();
    transformReferencesToRunParallel.add(
        this.transformParentCategoryReference(categories).toCompletableFuture());
    transformReferencesToRunParallel.add(
        this.transformCustomTypeReference(categories).toCompletableFuture());
    transformReferencesToRunParallel.add(
        this.transformAssetsCustomTypeReference(categories).toCompletableFuture());

    return CompletableFuture.allOf(
            transformReferencesToRunParallel.toArray(new CompletableFuture[0]))
        .thenApply(
            ignore ->
                CategoryReferenceResolutionUtils.mapToCategoryDrafts(
                    categories, referenceIdToKeyCache.asMap()));
  }

  @Nonnull
  private CompletionStage<List<Category>> transformParentCategoryReference(
      @Nonnull final List<Category> categories) {

    final Set<String> parentCategoryIds =
        categories
            .stream()
            .map(Category::getParent)
            .filter(Objects::nonNull)
            .map(Reference::getId)
            .collect(Collectors.toSet());

    return fetchAndFillReferenceIdToKeyCache(
        categories, parentCategoryIds, GraphQlQueryResources.CATEGORIES);
  }

  @Nonnull
  private CompletionStage<List<Category>> transformCustomTypeReference(
      @Nonnull final List<Category> categories) {

    final Set<String> customTypeIds =
        categories
            .stream()
            .map(Category::getCustom)
            .filter(Objects::nonNull)
            .map(customFields -> customFields.getType().getId())
            .collect(Collectors.toSet());

    return fetchAndFillReferenceIdToKeyCache(
        categories, customTypeIds, GraphQlQueryResources.TYPES);
  }

  @Nonnull
  private CompletionStage<List<Category>> transformAssetsCustomTypeReference(
      @Nonnull final List<Category> categories) {

    final Set<String> typeIds =
        categories
            .stream()
            .map(category -> category.getAssets())
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
            .collect(toSet());

    return fetchAndFillReferenceIdToKeyCache(categories, typeIds, GraphQlQueryResources.TYPES);
  }
}
