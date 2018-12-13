package com.commercetools.sync.category;

import static com.commercetools.sync.categories.utils.CategoryReferenceReplacementUtils.buildCategoryQuery;
import static com.commercetools.sync.categories.utils.CategoryReferenceReplacementUtils.replaceCategoriesReferenceIdsWithKeys;
import static com.commercetools.sync.utils.SphereClientUtils.CTP_TARGET_CLIENT;

import com.commercetools.sync.Syncer;
import com.commercetools.sync.categories.CategorySync;
import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.categories.helpers.CategorySyncStatistics;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.queries.CategoryQuery;
import java.util.List;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CategorySyncer
    extends Syncer<
        Category,
        CategoryDraft,
        CategorySyncStatistics,
        CategorySyncOptions,
        CategoryQuery,
        CategorySync> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CategorySyncer.class);

  /** Instantiates a {@link Syncer} instance. */
  public CategorySyncer() {
    super(
        new CategorySync(
            CategorySyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                .errorCallback(LOGGER::error)
                .warningCallback(LOGGER::warn)
                .build()),
        buildCategoryQuery());
    // TODO: Instead of reference expansion, we could cache all keys and replace references
    // manually.
  }

  @Override
  @Nonnull
  protected List<CategoryDraft> getDraftsFromPage(@Nonnull final List<Category> page) {
    return replaceCategoriesReferenceIdsWithKeys(page);
  }
}
