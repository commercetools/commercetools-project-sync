package com.commercetools.project.sync.taxcategory;

import com.commercetools.project.sync.Syncer;
import com.commercetools.project.sync.service.CustomObjectService;
import com.commercetools.project.sync.service.impl.CustomObjectServiceImpl;
import com.commercetools.sync.taxcategories.TaxCategorySync;
import com.commercetools.sync.taxcategories.TaxCategorySyncOptions;
import com.commercetools.sync.taxcategories.TaxCategorySyncOptionsBuilder;
import com.commercetools.sync.taxcategories.helpers.TaxCategorySyncStatistics;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import io.sphere.sdk.taxcategories.TaxCategoryDraftBuilder;
import io.sphere.sdk.taxcategories.TaxRate;
import io.sphere.sdk.taxcategories.TaxRateDraft;
import io.sphere.sdk.taxcategories.TaxRateDraftBuilder;
import io.sphere.sdk.taxcategories.queries.TaxCategoryQuery;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TaxCategorySyncer
    extends Syncer<
        TaxCategory,
        TaxCategoryDraft,
        TaxCategorySyncStatistics,
        TaxCategorySyncOptions,
        TaxCategoryQuery,
        TaxCategorySync> {

  private static final Logger LOGGER = LoggerFactory.getLogger(TaxCategorySyncer.class);

  /** Instantiates a {@link Syncer} instance. */
  private TaxCategorySyncer(
      @Nonnull final TaxCategorySync taxCategorySync,
      @Nonnull final SphereClient sourceClient,
      @Nonnull final SphereClient targetClient,
      @Nonnull final CustomObjectService customObjectService,
      @Nonnull final Clock clock) {
    super(taxCategorySync, sourceClient, targetClient, customObjectService, clock);
  }

  @Nonnull
  public static TaxCategorySyncer of(
      @Nonnull final SphereClient sourceClient,
      @Nonnull final SphereClient targetClient,
      @Nonnull final Clock clock) {
    final TaxCategorySyncOptions syncOptions =
        TaxCategorySyncOptionsBuilder.of(targetClient)
            .errorCallback(LOGGER::error)
            .warningCallback(LOGGER::warn)
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
    List<TaxRateDraft> taxRateDrafts = convertTaxRateToTaxRateDraft(taxCategory.getTaxRates());
    return TaxCategoryDraftBuilder.of(
            taxCategory.getName(), taxRateDrafts, taxCategory.getDescription())
        .key(taxCategory.getKey())
        .build();
  }

  @Nonnull
  private static List<TaxRateDraft> convertTaxRateToTaxRateDraft(
      @Nonnull final List<TaxRate> taxRates) {
    return taxRates
        .stream()
        .map(taxRate -> TaxRateDraftBuilder.of(taxRate).build())
        .collect(Collectors.toList());
  }

  @Nonnull
  @Override
  protected TaxCategoryQuery getQuery() {
    return TaxCategoryQuery.of();
  }
}
