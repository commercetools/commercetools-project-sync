package com.commercetools.project.sync.category;

import static com.commercetools.project.sync.util.TestUtils.getMockedClock;
import static com.commercetools.sync.categories.utils.CategoryReferenceReplacementUtils.buildCategoryQuery;
import static com.commercetools.sync.categories.utils.CategoryReferenceReplacementUtils.replaceCategoriesReferenceIdsWithKeys;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.concurrent.CompletionStage;

import org.junit.jupiter.api.Test;

import com.commercetools.sync.categories.CategorySync;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.SphereClient;

class CategorySyncerTest {
  @Test
  void of_ShouldCreateCategorySyncerInstance() {
    // test
    final CategorySyncer categorySyncer =
        CategorySyncer.of(mock(SphereClient.class), mock(SphereClient.class), getMockedClock());

    // assertions
    assertThat(categorySyncer).isNotNull();
    assertThat(categorySyncer.getQuery(null)).isEqualTo(buildCategoryQuery());
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

    // test
    final CompletionStage<List<CategoryDraft>> draftsFromPageStage =
        categorySyncer.transform(categoryPage);

    // assertions
    final List<CategoryDraft> expectedResult = replaceCategoriesReferenceIdsWithKeys(categoryPage);
    assertThat(draftsFromPageStage).isCompletedWithValue(expectedResult);
  }

  @Test
  void getQuery_ShouldBuildCategoryQuery() {
    // preparation
    final CategorySyncer categorySyncer =
        CategorySyncer.of(mock(SphereClient.class), mock(SphereClient.class), getMockedClock());

    // test
    final CategoryQuery query = categorySyncer.getQuery(null);

    // assertion
    assertThat(query).isEqualTo(buildCategoryQuery());
  }
}
