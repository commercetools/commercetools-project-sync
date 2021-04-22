package com.commercetools.project.sync.taxcategory;

import static com.commercetools.project.sync.util.TestUtils.getMockedClock;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.taxcategories.TaxCategorySync;
import io.sphere.sdk.client.SphereApiConfig;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import io.sphere.sdk.taxcategories.TaxCategoryDraftBuilder;
import io.sphere.sdk.taxcategories.TaxRateDraft;
import io.sphere.sdk.taxcategories.TaxRateDraftBuilder;
import io.sphere.sdk.taxcategories.queries.TaxCategoryQuery;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
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
    // test
    final TaxCategorySyncer taxCategorySyncer =
        TaxCategorySyncer.of(mock(SphereClient.class), mock(SphereClient.class), getMockedClock());

    // assertions
    assertThat(taxCategorySyncer).isNotNull();
    assertThat(taxCategorySyncer.getQuery()).isEqualTo(TaxCategoryQuery.of());
    assertThat(taxCategorySyncer.getSync()).isInstanceOf(TaxCategorySync.class);
  }

  @Test
  void transform_ShouldConvertResourcesToDrafts() {
    // preparation
    final TaxCategorySyncer taxCategorySyncer =
        TaxCategorySyncer.of(mock(SphereClient.class), mock(SphereClient.class), getMockedClock());
    final List<TaxCategory> taxCategoryPage =
        asList(
            readObjectFromResource("tax-category-key-1.json", TaxCategory.class),
            readObjectFromResource("tax-category-key-2.json", TaxCategory.class));

    // test
    final CompletionStage<List<TaxCategoryDraft>> draftsFromPageStage =
        taxCategorySyncer.transform(taxCategoryPage);

    // assertions
    assertThat(draftsFromPageStage)
        .isCompletedWithValue(
            taxCategoryPage
                .stream()
                .map(
                    taxCategory -> {
                      List<TaxRateDraft> taxRateDrafts =
                          taxCategory
                              .getTaxRates()
                              .stream()
                              .map(taxRate -> TaxRateDraftBuilder.of(taxRate).build())
                              .collect(Collectors.toList());
                      return TaxCategoryDraftBuilder.of(
                              taxCategory.getName(), taxRateDrafts, taxCategory.getDescription())
                          .key(taxCategory.getKey());
                    })
                .map(TaxCategoryDraftBuilder::build)
                .collect(toList()));
  }

  @Test
  void getQuery_ShouldBuildTaxCategoryQuery() {
    // preparation
    final TaxCategorySyncer taxCategorySyncer =
        TaxCategorySyncer.of(mock(SphereClient.class), mock(SphereClient.class), getMockedClock());

    // test
    final TaxCategoryQuery query = taxCategorySyncer.getQuery();

    // assertion
    assertThat(query).isEqualTo(TaxCategoryQuery.of());
  }

  @Test
  void syncWithError_WhenNoKeyIsProvided_ShouldCallErrorCallback() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    final SphereClient targetClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereApiConfig.of("source-project"));
    when(targetClient.getConfig()).thenReturn(SphereApiConfig.of("target-project"));

    final List<TaxCategory> taxCategoryPage =
        asList(readObjectFromResource("tax-category-without-key.json", TaxCategory.class));

    final PagedQueryResult<TaxCategory> pagedQueryResult = mock(PagedQueryResult.class);
    when(pagedQueryResult.getResults()).thenReturn(taxCategoryPage);
    when(sourceClient.execute(any(TaxCategoryQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(pagedQueryResult));

    // test
    final TaxCategorySyncer taxCategorySyncer =
        TaxCategorySyncer.of(sourceClient, targetClient, mock(Clock.class));
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
