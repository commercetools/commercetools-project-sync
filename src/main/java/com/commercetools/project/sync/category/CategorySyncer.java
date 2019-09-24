package com.commercetools.project.sync.category;

import static com.commercetools.sync.categories.utils.CategoryReferenceReplacementUtils.buildCategoryQuery;
import static com.commercetools.sync.categories.utils.CategoryReferenceReplacementUtils.replaceCategoriesReferenceIdsWithKeys;

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
import java.time.Clock;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
      @Nonnull final CustomObjectService customObjectService,
      @Nonnull final Clock clock) {
    super(categorySync, sourceClient, targetClient, customObjectService, clock);
    // TODO: Instead of reference expansion, we could cache all keys and replace references
    // manually.
  }

  @Nonnull
  public static CategorySyncer of(
      @Nonnull final SphereClient sourceClient,
      @Nonnull final SphereClient targetClient,
      @Nonnull final Clock clock) {
    final CategorySyncOptions syncOptions =
        CategorySyncOptionsBuilder.of(targetClient)
            .errorCallback(LOGGER::error)
            .warningCallback(LOGGER::warn)
            .build();

    final CategorySync categorySync = new CategorySync(syncOptions);

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(targetClient);

    return new CategorySyncer(categorySync, sourceClient, targetClient, customObjectService, clock);
  }

  @Override
  @Nonnull
  protected CompletionStage<List<CategoryDraft>> transform(@Nonnull final List<Category> page) {
    return CompletableFuture.completedFuture(replaceCategoriesReferenceIdsWithKeys(page));
  }

  @Nonnull
  @Override
  protected CategoryQuery getQuery() {
    return buildCategoryQuery();
  }
}
