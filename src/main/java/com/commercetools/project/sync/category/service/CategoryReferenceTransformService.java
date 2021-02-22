package com.commercetools.project.sync.category.service;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.types.Type;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public interface CategoryReferenceTransformService {

  /**
   * Given a {@link List} of categories, this method will transform Categories to CategoryDrafts by
   * resolving the References parent Category, {@link Type}. If there exists a mapping for all the
   * ids in {@code referenceIdToKeyCache}, the method replaces id field with key. If there is at
   * least one missing mapping, it attempts to make a GraphQL request to CTP to fetch all ids and
   * keys of every missing type. For each fetched key/id pair, the method will insert it into the
   * {@code referenceIdToKeyCache} cache after the request is successful.
   *
   * @param categories the categories to find a key mapping for all the references and cache them.
   * @return categoryDrafts with the references replaced Id with keys.
   */
  @Nonnull
  CompletableFuture<List<CategoryDraft>> transformCategoryReferences(
      @Nonnull List<Category> categories);
}
