package com.commercetools.project.sync.category;

import static com.commercetools.project.sync.util.TestUtils.getMockedClock;
import static com.commercetools.project.sync.util.referenceresolution.CategoryReferenceResolutionUtils.mapToCategoryDrafts;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.categories.CategorySync;
import com.commercetools.sync.commons.helpers.ResourceKeyIdGraphQlRequest;
import com.commercetools.sync.commons.models.ResourceKeyId;
import com.commercetools.sync.commons.models.ResourceKeyIdGraphQlResult;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.SphereApiConfig;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.queries.PagedQueryResult;
import java.time.Clock;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

class CategorySyncerTest {
  private final TestLogger syncerTestLogger = TestLoggerFactory.getTestLogger(CategorySyncer.class);

  @BeforeEach
  void setup() {
    syncerTestLogger.clearAll();
  }

  @Test
  void of_ShouldCreateCategorySyncerInstance() {
    // test
    final CategorySyncer categorySyncer =
        CategorySyncer.of(mock(SphereClient.class), mock(SphereClient.class), getMockedClock());

    // assertions
    CategoryQuery expectedQuery = CategoryQuery.of();
    assertThat(categorySyncer).isNotNull();
    assertThat(categorySyncer.getQuery()).isEqualTo(expectedQuery);
    assertThat(categorySyncer.getSync()).isExactlyInstanceOf(CategorySync.class);
  }

  @Test
  void transform_ShouldReplaceCategoryReferenceIdsWithKeys() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    final CategorySyncer categorySyncer =
        CategorySyncer.of(sourceClient, mock(SphereClient.class), getMockedClock());
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

    String jsonStringCategories =
        "{\"results\":[{\"id\":\"53c4a8b4-754f-4b95-b6f2-3e1e70e3d0c3\",\"key\":\"cat1\"}]}";
    final ResourceKeyIdGraphQlResult categoriesResult =
        SphereJsonUtils.readObject(jsonStringCategories, ResourceKeyIdGraphQlResult.class);

    when(sourceClient.execute(any()))
        .thenReturn(CompletableFuture.completedFuture(categoriesResult));

    // test
    final CompletionStage<List<CategoryDraft>> draftsFromPageStage =
        categorySyncer.transform(categoryPage);

    Map<String, String> cache = new HashMap<>();
    cache.put("53c4a8b4-754f-4b95-b6f2-3e1e70e3d0c3", "cat1");

    // assertions
    final List<CategoryDraft> expectedResult = mapToCategoryDrafts(categoryPage, cache);
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
    CategoryQuery expectedQuery = CategoryQuery.of();
    assertThat(query).isEqualTo(expectedQuery);
  }

  @Test
  void syncWithError_ShouldCallErrorCallback() {
    // preparation: category with no key is synced
    final SphereClient sourceClient = mock(SphereClient.class);
    final SphereClient targetClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereApiConfig.of("source-project"));
    when(targetClient.getConfig()).thenReturn(SphereApiConfig.of("target-project"));
    final List<Category> categories =
        Collections.singletonList(readObjectFromResource("category-no-key.json", Category.class));

    final PagedQueryResult<Category> pagedQueryResult = mock(PagedQueryResult.class);
    when(pagedQueryResult.getResults()).thenReturn(categories);
    when(sourceClient.execute(any(CategoryQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(pagedQueryResult));

    // test
    final CategorySyncer categorySyncer =
        CategorySyncer.of(sourceClient, targetClient, mock(Clock.class));
    categorySyncer.sync(null, true).toCompletableFuture().join();

    // assertion
    final LoggingEvent errorLog = syncerTestLogger.getAllLoggingEvents().get(0);
    assertThat(errorLog.getMessage())
        .isEqualTo(
            "Error when trying to sync category. Existing key: <<not present>>. Update actions: []");
    assertThat(errorLog.getThrowable().get().getMessage())
        .isEqualTo(
            "CategoryDraft with name: LocalizedString(en -> category-name-1) doesn't have a key. Please make sure all category drafts have keys.");
  }

  @Test
  void syncWithWarning_ShouldCallWarningCallback() {
    // preparation: old category has category order hint,
    // new category does not have category order hint
    final SphereClient sourceClient = mock(SphereClient.class);
    final SphereClient targetClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereApiConfig.of("source-project"));
    when(targetClient.getConfig()).thenReturn(SphereApiConfig.of("target-project"));

    final List<Category> sourceCategories =
        Collections.singletonList(readObjectFromResource("category-key-2.json", Category.class));
    final List<Category> targetCategories =
        Collections.singletonList(
            readObjectFromResource("category-order-hint.json", Category.class));

    final PagedQueryResult<Category> sourcePagedQueryResult = mock(PagedQueryResult.class);
    when(sourcePagedQueryResult.getResults()).thenReturn(sourceCategories);
    when(sourceClient.execute(any(CategoryQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(sourcePagedQueryResult));

    final PagedQueryResult<Category> targetPagedQueryResult = mock(PagedQueryResult.class);
    when(targetPagedQueryResult.getResults()).thenReturn(targetCategories);
    when(targetClient.execute(any(CategoryQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(targetPagedQueryResult));

    final ResourceKeyIdGraphQlResult resourceKeyIdGraphQlResult =
        mock(ResourceKeyIdGraphQlResult.class);
    when(resourceKeyIdGraphQlResult.getResults())
        .thenReturn(
            singleton(new ResourceKeyId("categoryKey2", "ba81a6da-cf83-435b-a89e-2afab579846f")));
    when(targetClient.execute(any(ResourceKeyIdGraphQlRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(resourceKeyIdGraphQlResult));

    // test
    final CategorySyncer categorySyncer =
        CategorySyncer.of(sourceClient, targetClient, mock(Clock.class));
    categorySyncer.sync(null, true).toCompletableFuture().join();

    // assertion
    final LoggingEvent errorLog = syncerTestLogger.getAllLoggingEvents().get(0);
    assertThat(errorLog.getMessage())
        .isEqualTo("Warning when trying to sync category. Existing key: categoryKey2");
    assertThat(errorLog.getThrowable().get().getMessage())
        .isEqualTo(
            format(
                "Cannot unset 'orderHint' field of category with id '%s'.",
                sourceCategories.get(0).getId()));
  }
}
