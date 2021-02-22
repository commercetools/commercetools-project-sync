package com.commercetools.project.sync.category;

import static com.commercetools.project.sync.util.SyncUtils.logErrorCallback;
import static com.commercetools.project.sync.util.SyncUtils.logWarningCallback;

import com.commercetools.project.sync.Syncer;
import com.commercetools.project.sync.category.service.CategoryReferenceTransformService;
import com.commercetools.project.sync.category.service.impl.CategoryReferenceTransformServiceImpl;
import com.commercetools.project.sync.service.CustomObjectService;
import com.commercetools.project.sync.service.impl.CustomObjectServiceImpl;
import com.commercetools.sync.categories.CategorySync;
import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.categories.helpers.CategorySyncStatistics;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
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

  private final CategoryReferenceTransformService referencesService;

  /** Instantiates a {@link Syncer} instance. */
  private CategorySyncer(
      @Nonnull final CategorySync categorySync,
      @Nonnull final SphereClient sourceClient,
      @Nonnull final SphereClient targetClient,
      @Nonnull final CustomObjectService customObjectService,
      @Nonnull final CategoryReferenceTransformService referencesService,
      @Nonnull final Clock clock) {
    super(categorySync, sourceClient, targetClient, customObjectService, clock);
    this.referencesService = referencesService;
  }

  @Nonnull
  public static CategorySyncer of(
      @Nonnull final SphereClient sourceClient,
      @Nonnull final SphereClient targetClient,
      @Nonnull final Clock clock) {
    final QuadConsumer<
            SyncException,
            Optional<CategoryDraft>,
            Optional<Category>,
            List<UpdateAction<Category>>>
        logErrorCallback =
            (exception, newResourceDraft, oldResource, updateActions) ->
                logErrorCallback(LOGGER, "category", exception, oldResource, updateActions);
    final TriConsumer<SyncException, Optional<CategoryDraft>, Optional<Category>>
        logWarningCallback =
            (exception, newResourceDraft, oldResource) ->
                logWarningCallback(LOGGER, "category", exception, oldResource);
    final CategorySyncOptions syncOptions =
        CategorySyncOptionsBuilder.of(targetClient)
            .errorCallback(logErrorCallback)
            .warningCallback(logWarningCallback)
            .build();

    final CategorySync categorySync = new CategorySync(syncOptions);

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(targetClient);

    final CategoryReferenceTransformService referenceTransformService =
        new CategoryReferenceTransformServiceImpl(sourceClient);

    return new CategorySyncer(
        categorySync,
        sourceClient,
        targetClient,
        customObjectService,
        referenceTransformService,
        clock);
  }

  @Override
  @Nonnull
  protected CompletionStage<List<CategoryDraft>> transform(@Nonnull final List<Category> page) {
    return this.referencesService.transformCategoryReferences(page);
  }

  @Nonnull
  @Override
  protected CategoryQuery getQuery() {
    return CategoryQuery.of();
  }
}
