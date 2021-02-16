package com.commercetools.project.sync.category.service;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import java.util.List;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;

public interface CategoryReferenceTransformService {

  @Nonnull
  CompletionStage<List<CategoryDraft>> transformCategoryReferences(
      @Nonnull List<Category> categories);
}
