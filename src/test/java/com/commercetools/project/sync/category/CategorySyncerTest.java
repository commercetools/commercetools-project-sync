package com.commercetools.project.sync.category;

import static com.commercetools.sync.categories.utils.CategoryReferenceReplacementUtils.buildCategoryQuery;
import static com.commercetools.sync.categories.utils.CategoryReferenceReplacementUtils.replaceCategoriesReferenceIdsWithKeys;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.sync.categories.CategorySync;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.client.SphereClient;
import java.util.List;
import org.junit.jupiter.api.Test;

class CategorySyncerTest {
  @Test
  void of_ShouldCreateCategorySyncerInstance() {
    // test
    final CategorySyncer categorySyncer =
        CategorySyncer.of(mock(SphereClient.class), mock(SphereClient.class));

    // assertions
    assertThat(categorySyncer).isNotNull();
    assertThat(categorySyncer.getQuery()).isEqualTo(buildCategoryQuery());
    assertThat(categorySyncer.getSync()).isInstanceOf(CategorySync.class);
  }

  @Test
  void transformResourcesToDrafts_ShouldReplaceCategoryReferenceIdsWithKeys() {
    // preparation
    final CategorySyncer categorySyncer =
        CategorySyncer.of(mock(SphereClient.class), mock(SphereClient.class));
    final List<Category> categoryPage =
        asList(
            readObjectFromResource("category-key-1.json", Category.class),
            readObjectFromResource("category-key-2.json", Category.class));

    // test
    final List<CategoryDraft> draftsFromPage =
        categorySyncer.transformResourcesToDrafts(categoryPage);

    // assertions
    final List<CategoryDraft> expectedResult = replaceCategoriesReferenceIdsWithKeys(categoryPage);
    assertThat(draftsFromPage).isEqualTo(expectedResult);
  }

  @Test
  void getQuery_ShouldBuildCategoryQuery() {
    // preparation
    final CategorySyncer categorySyncer =
        CategorySyncer.of(mock(SphereClient.class), mock(SphereClient.class));

    // test
    final CategoryQuery query = categorySyncer.getQuery();

    // assertion
    assertThat(query).isEqualTo(buildCategoryQuery());
  }
}
