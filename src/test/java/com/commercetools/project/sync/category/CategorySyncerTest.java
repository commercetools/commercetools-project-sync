package com.commercetools.project.sync.category;

import static com.commercetools.project.sync.util.TestUtils.getMockedClock;
import static com.commercetools.sync.categories.utils.CategoryReferenceResolutionUtils.buildCategoryQuery;
import static com.commercetools.sync.categories.utils.CategoryReferenceResolutionUtils.mapToCategoryDrafts;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.sync.categories.CategorySync;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.SphereClient;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CategorySyncerTest {
  @Test
  void of_ShouldCreateCategorySyncerInstance() {
    // test
    final CategorySyncer categorySyncer =
        CategorySyncer.of(mock(SphereClient.class), mock(SphereClient.class), getMockedClock());

    // assertions
    assertThat(categorySyncer).isNotNull();
    assertThat(categorySyncer.getQuery()).isEqualTo(buildCategoryQuery());
    assertThat(categorySyncer.getSync()).isExactlyInstanceOf(CategorySync.class);
  }

  @Test
  void transform_ShouldReplaceCategoryReferenceIdsWithKeys() {
    // preparation
    final CategorySyncer categorySyncer =
        CategorySyncer.of(mock(SphereClient.class), mock(SphereClient.class), getMockedClock());
    final List<Category> categoryPage =
        asList(
            readObjectFromResource("category-key-1.json", Category.class),
            readObjectFromResource("category-key-2.json", Category.class));
    final List<String> referenceIds =
        categoryPage
            .stream()
            .filter(category -> category.getCustom() != null)
            .map(category -> category.getCustom().getType().getId())
            .collect(Collectors.toList());

    // test
    final CompletionStage<List<CategoryDraft>> draftsFromPageStage =
        categorySyncer.transform(categoryPage);

    // assertions
    final List<CategoryDraft> expectedResult = mapToCategoryDrafts(categoryPage);
    final List<String> referenceKeys =
        expectedResult
            .stream()
            .filter(category -> category.getCustom() != null)
            .map(category -> category.getCustom().getType().getId())
            .collect(Collectors.toList());
    assertThat(referenceKeys).doesNotContainSequence(referenceIds);
    assertThat(draftsFromPageStage).isCompletedWithValue(expectedResult);
  }

  @Test
  void getQuery_ShouldBuildCategoryQuery() {
    // preparation
    final CategorySyncer categorySyncer =
        CategorySyncer.of(mock(SphereClient.class), mock(SphereClient.class), getMockedClock());

    // test
    final CategoryQuery query = categorySyncer.getQuery();

    // assertion
    assertThat(query).isEqualTo(buildCategoryQuery());
  }
}
