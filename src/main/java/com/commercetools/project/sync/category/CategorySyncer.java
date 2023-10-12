package com.commercetools.project.sync.category;

import static com.commercetools.project.sync.util.SyncUtils.logErrorCallback;
import static com.commercetools.project.sync.util.SyncUtils.logWarningCallback;
import static com.commercetools.sync.categories.utils.CategoryTransformUtils.toCategoryDrafts;

import com.commercetools.api.client.ByProjectKeyCategoriesGet;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.category.CategoryPagedQueryResponse;
import com.commercetools.api.models.category.CategoryUpdateAction;
import com.commercetools.project.sync.Syncer;
import com.commercetools.project.sync.service.CustomObjectService;
import com.commercetools.project.sync.service.impl.CustomObjectServiceImpl;
import com.commercetools.sync.categories.CategorySync;
import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.categories.helpers.CategorySyncStatistics;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This class compiles but not tested yet
// TODO: Test class and adjust logic if needed
public final class CategorySyncer
    extends Syncer<
        Category,
        CategoryUpdateAction,
        CategoryDraft,
        CategorySyncStatistics,
        CategorySyncOptions,
        ByProjectKeyCategoriesGet,
        CategoryPagedQueryResponse,
        CategorySync> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CategorySyncer.class);

  /** Instantiates a {@link Syncer} instance. */
  private CategorySyncer(
      @Nonnull final CategorySync categorySync,
      @Nonnull final ProjectApiRoot sourceClient,
      @Nonnull final ProjectApiRoot targetClient,
      @Nonnull final CustomObjectService customObjectService,
      @Nonnull final Clock clock) {
    super(categorySync, sourceClient, targetClient, customObjectService, clock);
  }

  @Nonnull
  public static CategorySyncer of(
      @Nonnull final ProjectApiRoot sourceClient,
      @Nonnull final ProjectApiRoot targetClient,
      @Nonnull final Clock clock) {
    final QuadConsumer<
            SyncException, Optional<CategoryDraft>, Optional<Category>, List<CategoryUpdateAction>>
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

    return new CategorySyncer(categorySync, sourceClient, targetClient, customObjectService, clock);
  }

  @Override
  @Nonnull
  protected CompletionStage<List<CategoryDraft>> transform(@Nonnull final List<Category> page) {
    return toCategoryDrafts(getSourceClient(), referenceIdToKeyCache, page);
  }

  @Nonnull
  @Override
  protected ByProjectKeyCategoriesGet getQuery() {
    return getSourceClient().categories().get();
  }

  @Nonnull
  @Override
  protected Logger getLoggerInstance() {
    return LOGGER;
  }
}
