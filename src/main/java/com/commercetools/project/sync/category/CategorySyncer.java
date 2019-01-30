package com.commercetools.project.sync.category;

import com.commercetools.project.sync.Syncer;
import com.commercetools.project.sync.service.CustomObjectService;
import com.commercetools.project.sync.service.impl.CustomObjectServiceImpl;
import com.commercetools.sync.categories.CategorySync;
import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.categories.helpers.CategorySyncStatistics;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.SphereClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.List;

import static com.commercetools.sync.categories.utils.CategoryReferenceReplacementUtils.buildCategoryQuery;
import static com.commercetools.sync.categories.utils.CategoryReferenceReplacementUtils.replaceCategoriesReferenceIdsWithKeys;

public final class CategorySyncer
    extends Syncer<
        Category,
        CategoryDraft,
        CategorySyncStatistics,
        CategorySyncOptions,
        CategoryQuery,
        CategorySync> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CategorySyncer.class);

  /** Instantiates a {@link Syncer} instance. */
  private CategorySyncer(
      @Nonnull final CategorySync categorySync,
      @Nonnull final SphereClient sourceClient,
      @Nonnull final SphereClient targetClient,
      @Nonnull final CustomObjectService customObjectService) {
    super(categorySync, sourceClient, targetClient, customObjectService);
    // TODO: Instead of reference expansion, we could cache all keys and replace references
    // manually.
  }

  @Nonnull
  public static CategorySyncer of(
      @Nonnull final SphereClient sourceClient, @Nonnull final SphereClient targetClient) {
    final CategorySyncOptions syncOptions =
        CategorySyncOptionsBuilder.of(targetClient)
            .errorCallback(LOGGER::error)
            .warningCallback(LOGGER::warn)
            .build();

    final CategorySync categorySync = new CategorySync(syncOptions);

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(targetClient);

    return new CategorySyncer(categorySync, sourceClient, targetClient, customObjectService);
  }

  @Override
  @Nonnull
  protected List<CategoryDraft> transformResourcesToDrafts(@Nonnull final List<Category> page) {
    return replaceCategoriesReferenceIdsWithKeys(page);
  }

  @Nonnull
  @Override
  public CategoryQuery getQuery() {
    return buildCategoryQuery();
  }
}
