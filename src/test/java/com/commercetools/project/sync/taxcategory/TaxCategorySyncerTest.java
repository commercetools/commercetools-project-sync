package com.commercetools.project.sync.taxcategory;

import static com.commercetools.project.sync.util.TestUtils.getMockedClock;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.sync.taxcategories.TaxCategorySync;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import io.sphere.sdk.taxcategories.TaxCategoryDraftBuilder;
import io.sphere.sdk.taxcategories.TaxRateDraft;
import io.sphere.sdk.taxcategories.TaxRateDraftBuilder;
import io.sphere.sdk.taxcategories.queries.TaxCategoryQuery;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class TaxCategorySyncerTest {
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
                          taxCategory.getName(), taxRateDrafts, taxCategory.getDescription());
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
}
