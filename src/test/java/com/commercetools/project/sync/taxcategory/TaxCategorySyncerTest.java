package com.commercetools.project.sync.taxcategory;

import static com.commercetools.project.sync.util.TestUtils.getMockedClock;
import static com.commercetools.project.sync.util.TestUtils.readObjectFromResource;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ByProjectKeyTaxCategoriesGet;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.tax_category.TaxCategory;
import com.commercetools.api.models.tax_category.TaxCategoryDraft;
import com.commercetools.api.models.tax_category.TaxCategoryDraftBuilder;
import com.commercetools.api.models.tax_category.TaxCategoryPagedQueryResponse;
import com.commercetools.api.models.tax_category.TaxCategoryPagedQueryResponseBuilder;
import com.commercetools.api.models.tax_category.TaxRate;
import com.commercetools.api.models.tax_category.TaxRateDraft;
import com.commercetools.api.models.tax_category.TaxRateDraftBuilder;
import com.commercetools.sync.taxcategories.TaxCategorySync;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

class TaxCategorySyncerTest {
  private final TestLogger syncerTestLogger =
      TestLoggerFactory.getTestLogger(TaxCategorySyncer.class);

  @BeforeEach
  void setup() {
    syncerTestLogger.clearAll();
  }

  @Test
  void of_ShouldCreateTaxCategorySyncerInstance() {
    // preparation
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    when(sourceClient.taxCategories()).thenReturn(mock());
    when(sourceClient.taxCategories().get()).thenReturn(mock());

    // test
    final TaxCategorySyncer taxCategorySyncer =
        TaxCategorySyncer.of(sourceClient, mock(ProjectApiRoot.class), getMockedClock());

    // assertions
    assertThat(taxCategorySyncer).isNotNull();
    assertThat(taxCategorySyncer.getQuery()).isInstanceOf(ByProjectKeyTaxCategoriesGet.class);
    assertThat(taxCategorySyncer.getSync()).isInstanceOf(TaxCategorySync.class);
  }

  @Test
  void transform_ShouldConvertResourcesToDrafts() {
    // preparation
    final TaxCategorySyncer taxCategorySyncer =
        TaxCategorySyncer.of(
            mock(ProjectApiRoot.class), mock(ProjectApiRoot.class), getMockedClock());
    final List<TaxCategory> taxCategoryPage =
        List.of(
            readObjectFromResource("tax-category-key-1.json", TaxCategory.class),
            readObjectFromResource("tax-category-key-2.json", TaxCategory.class));

    // test
    final CompletionStage<List<TaxCategoryDraft>> draftsFromPageStage =
        taxCategorySyncer.transform(taxCategoryPage);

    // assertions
    assertThat(draftsFromPageStage)
        .isCompletedWithValue(
            taxCategoryPage.stream()
                .map(
                    taxCategory -> {
                      final List<TaxRateDraft> taxRateDrafts =
                          convertTaxRateToTaxRateDraft(taxCategory.getRates());
                      return TaxCategoryDraftBuilder.of()
                          .name(taxCategory.getName())
                          .rates(taxRateDrafts)
                          .description(taxCategory.getDescription())
                          .key(taxCategory.getKey());
                    })
                .map(TaxCategoryDraftBuilder::build)
                .collect(toList()));
  }

  private List<TaxRateDraft> convertTaxRateToTaxRateDraft(@Nonnull final List<TaxRate> taxRates) {

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

  @Test
  void syncWithError_WhenNoKeyIsProvided_ShouldCallErrorCallback() {
    // preparation
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final List<TaxCategory> taxCategoryPage =
        List.of(readObjectFromResource("tax-category-without-key.json", TaxCategory.class));

    final TaxCategoryPagedQueryResponse queryResponse =
        TaxCategoryPagedQueryResponseBuilder.of()
            .results(taxCategoryPage)
            .limit(20L)
            .offset(0L)
            .count(1L)
            .build();

    final ApiHttpResponse apiHttpResponse = mock(ApiHttpResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(queryResponse);
    final ByProjectKeyTaxCategoriesGet byProjectKeyTaxCategoriesGet =
        mock(ByProjectKeyTaxCategoriesGet.class);
    when(sourceClient.taxCategories()).thenReturn(mock());
    when(sourceClient.taxCategories().get()).thenReturn(byProjectKeyTaxCategoriesGet);
    when(byProjectKeyTaxCategoriesGet.withLimit(anyInt())).thenReturn(byProjectKeyTaxCategoriesGet);
    when(byProjectKeyTaxCategoriesGet.withWithTotal(anyBoolean()))
        .thenReturn(byProjectKeyTaxCategoriesGet);
    when(byProjectKeyTaxCategoriesGet.withSort(anyString()))
        .thenReturn(byProjectKeyTaxCategoriesGet);
    when(byProjectKeyTaxCategoriesGet.execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    // test
    final TaxCategorySyncer taxCategorySyncer =
        TaxCategorySyncer.of(sourceClient, mock(ProjectApiRoot.class), getMockedClock());
    taxCategorySyncer.sync(null, true).toCompletableFuture().join();

    // assertion
    final LoggingEvent errorLog = syncerTestLogger.getAllLoggingEvents().get(1);
    assertThat(errorLog.getMessage())
        .isEqualTo(
            "Error when trying to sync tax category. Existing key: <<not present>>. Update actions: []");
    assertThat(errorLog.getThrowable().get().getMessage())
        .isEqualTo(
            "TaxCategoryDraft with name: Standard tax category doesn't have a key. "
                + "Please make sure all tax category drafts have keys.");
  }
}
