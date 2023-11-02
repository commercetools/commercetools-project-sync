package com.commercetools.project.sync.taxcategory;

import static com.commercetools.project.sync.util.SyncUtils.logErrorCallback;
import static com.commercetools.project.sync.util.SyncUtils.logWarningCallback;

import com.commercetools.api.client.ByProjectKeyTaxCategoriesGet;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.tax_category.TaxCategory;
import com.commercetools.api.models.tax_category.TaxCategoryDraft;
import com.commercetools.api.models.tax_category.TaxCategoryDraftBuilder;
import com.commercetools.api.models.tax_category.TaxCategoryPagedQueryResponse;
import com.commercetools.api.models.tax_category.TaxCategoryUpdateAction;
import com.commercetools.api.models.tax_category.TaxRate;
import com.commercetools.api.models.tax_category.TaxRateDraft;
import com.commercetools.api.models.tax_category.TaxRateDraftBuilder;
import com.commercetools.api.predicates.query.tax_category.TaxCategoryQueryBuilderDsl;
import com.commercetools.project.sync.Syncer;
import com.commercetools.project.sync.service.CustomObjectService;
import com.commercetools.project.sync.service.impl.CustomObjectServiceImpl;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.taxcategories.TaxCategorySync;
import com.commercetools.sync.taxcategories.TaxCategorySyncOptions;
import com.commercetools.sync.taxcategories.TaxCategorySyncOptionsBuilder;
import com.commercetools.sync.taxcategories.helpers.TaxCategorySyncStatistics;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TaxCategorySyncer
    extends Syncer<
        TaxCategory,
        TaxCategoryUpdateAction,
        TaxCategoryDraft,
        TaxCategoryQueryBuilderDsl,
        TaxCategorySyncStatistics,
        TaxCategorySyncOptions,
        ByProjectKeyTaxCategoriesGet,
        TaxCategoryPagedQueryResponse,
        TaxCategorySync> {

  private static final Logger LOGGER = LoggerFactory.getLogger(TaxCategorySyncer.class);

  /** Instantiates a {@link Syncer} instance. */
  private TaxCategorySyncer(
      @Nonnull final TaxCategorySync taxCategorySync,
      @Nonnull final ProjectApiRoot sourceClient,
      @Nonnull final ProjectApiRoot targetClient,
      @Nonnull final CustomObjectService customObjectService,
      @Nonnull final Clock clock) {
    super(taxCategorySync, sourceClient, targetClient, customObjectService, clock);
  }

  @Nonnull
  public static TaxCategorySyncer of(
      @Nonnull final ProjectApiRoot sourceClient,
      @Nonnull final ProjectApiRoot targetClient,
      @Nonnull final Clock clock) {
    final QuadConsumer<
            SyncException,
            Optional<TaxCategoryDraft>,
            Optional<TaxCategory>,
            List<TaxCategoryUpdateAction>>
        logErrorCallback =
            (exception, newResourceDraft, oldResource, updateActions) ->
                logErrorCallback(LOGGER, "tax category", exception, oldResource, updateActions);
    final TriConsumer<SyncException, Optional<TaxCategoryDraft>, Optional<TaxCategory>>
        logWarningCallback =
            (exception, newResourceDraft, oldResource) ->
                logWarningCallback(LOGGER, "tax category", exception, oldResource);
    final TaxCategorySyncOptions syncOptions =
        TaxCategorySyncOptionsBuilder.of(targetClient)
            .errorCallback(logErrorCallback)
            .warningCallback(logWarningCallback)
            .build();

    final TaxCategorySync taxCategorySync = new TaxCategorySync(syncOptions);
    final CustomObjectService customObjectService = new CustomObjectServiceImpl(targetClient);
    return new TaxCategorySyncer(
        taxCategorySync, sourceClient, targetClient, customObjectService, clock);
  }

  @Override
  @Nonnull
  protected CompletionStage<List<TaxCategoryDraft>> transform(
      @Nonnull final List<TaxCategory> page) {
    return CompletableFuture.completedFuture(
        page.stream()
            .map(TaxCategorySyncer::convertTaxCategoryToTaxCategoryDraft)
            .collect(Collectors.toList()));
  }

  @Nonnull
  private static TaxCategoryDraft convertTaxCategoryToTaxCategoryDraft(
      @Nonnull final TaxCategory taxCategory) {
    List<TaxRateDraft> taxRateDrafts = convertTaxRateToTaxRateDraft(taxCategory.getRates());
    return TaxCategoryDraftBuilder.of()
        .name(taxCategory.getName())
        .rates(taxRateDrafts)
        .description(taxCategory.getDescription())
        .key(taxCategory.getKey())
        .build();
  }

  @Nonnull
  private static List<TaxRateDraft> convertTaxRateToTaxRateDraft(
      @Nonnull final List<TaxRate> taxRates) {

    return taxRates.stream()
        .map(
            taxRate ->
                TaxRateDraftBuilder.of()
                    .name(taxRate.getName())
                    .country(taxRate.getCountry())
                    .state(taxRate.getState())
                    .includedInPrice(taxRate.getIncludedInPrice())
                    .amount(taxRate.getAmount())
                    .key(taxRate.getKey())
                    .subRates(taxRate.getSubRates())
                    .build())
        .collect(Collectors.toList());
  }

  @Nonnull
  @Override
  protected ByProjectKeyTaxCategoriesGet getQuery() {
    return getSourceClient().taxCategories().get();
  }

  @Nonnull
  @Override
  protected Logger getLoggerInstance() {
    return LOGGER;
  }
}
