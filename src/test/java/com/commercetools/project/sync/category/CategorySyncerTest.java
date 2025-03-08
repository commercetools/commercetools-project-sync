package com.commercetools.project.sync.category;

import static com.commercetools.project.sync.util.TestUtils.*;
import static com.commercetools.sync.categories.utils.CategoryTransformUtils.toCategoryDrafts;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ByProjectKeyCategoriesGet;
import com.commercetools.api.client.ByProjectKeyCategoriesKeyByKeyGet;
import com.commercetools.api.client.ByProjectKeyCategoriesKeyByKeyRequestBuilder;
import com.commercetools.api.client.ByProjectKeyCategoriesRequestBuilder;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.category.CategoryPagedQueryResponse;
import com.commercetools.api.models.category.CategoryPagedQueryResponseBuilder;
import com.commercetools.sync.categories.CategorySync;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.valfirst.slf4jtest.LoggingEvent;
import com.github.valfirst.slf4jtest.TestLogger;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CategorySyncerTest {
  private final TestLogger syncerTestLogger = TestLoggerFactory.getTestLogger(CategorySyncer.class);

  private final ReferenceIdToKeyCache referenceIdToKeyCache =
      new CaffeineReferenceIdToKeyCacheImpl();

  @BeforeEach
  void setup() {
    syncerTestLogger.clearAll();
  }

  @Test
  void of_ShouldCreateCategorySyncerInstance() {
    // test
    final ProjectApiRoot projectApiRoot = mock(ProjectApiRoot.class);
    final ByProjectKeyCategoriesRequestBuilder byProjectKeyCategoriesRequestBuilder = mock();
    when(projectApiRoot.categories()).thenReturn(byProjectKeyCategoriesRequestBuilder);
    final ByProjectKeyCategoriesGet byProjectKeyCategoriesGet = mock();
    when(byProjectKeyCategoriesRequestBuilder.get()).thenReturn(byProjectKeyCategoriesGet);
    final CategorySyncer categorySyncer =
        CategorySyncer.of(projectApiRoot, projectApiRoot, getMockedClock());

    // assertions
    assertThat(categorySyncer).isNotNull();
    assertThat(categorySyncer.getQuery()).isEqualTo(byProjectKeyCategoriesGet);
    assertThat(categorySyncer.getSync()).isExactlyInstanceOf(CategorySync.class);
  }

  @Test
  void transform_ShouldReplaceCategoryReferenceIdsWithKeys() throws JsonProcessingException {
    // preparation
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final CategorySyncer categorySyncer =
        CategorySyncer.of(sourceClient, mock(ProjectApiRoot.class), getMockedClock());
    final List<Category> categoryPage =
        asList(
            readObjectFromResource("category-key-1.json", Category.class),
            readObjectFromResource("category-key-2.json", Category.class));
    final List<String> referenceIds =
        categoryPage.stream()
            .filter(category -> category.getCustom() != null)
            .map(category -> category.getCustom().getType().getId())
            .collect(Collectors.toList());

    mockResourceIdsGraphQlRequest(
        sourceClient, "typeDefinitions", "53c4a8b4-754f-4b95-b6f2-3e1e70e3d0c3", "cat1");

    // test
    final CompletionStage<List<CategoryDraft>> draftsFromPageStage =
        categorySyncer.transform(categoryPage);

    // assertions
    final List<CategoryDraft> expectedResult =
        toCategoryDrafts(sourceClient, referenceIdToKeyCache, categoryPage).join();
    final List<String> referenceKeys =
        expectedResult.stream()
            .filter(category -> category.getCustom() != null)
            .map(category -> category.getCustom().getType().getId())
            .collect(Collectors.toList());
    assertThat(referenceKeys).doesNotContainSequence(referenceIds);
    assertThat(draftsFromPageStage).isCompletedWithValue(expectedResult);
  }

  @Test
  void syncWithError_ShouldCallErrorCallback() {
    // preparation: category with no key is synced
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final ByProjectKeyCategoriesRequestBuilder byProjectKeyCategoriesRequestBuilder = mock();
    when(sourceClient.categories()).thenReturn(byProjectKeyCategoriesRequestBuilder);
    final ByProjectKeyCategoriesGet byProjectKeyCategoriesGet = mock();
    when(byProjectKeyCategoriesRequestBuilder.get()).thenReturn(byProjectKeyCategoriesGet);
    when(byProjectKeyCategoriesGet.withSort(anyString())).thenReturn(byProjectKeyCategoriesGet);
    when(byProjectKeyCategoriesGet.withLimit(anyInt())).thenReturn(byProjectKeyCategoriesGet);
    when(byProjectKeyCategoriesGet.withWithTotal(anyBoolean()))
        .thenReturn(byProjectKeyCategoriesGet);
    final ApiHttpResponse<CategoryPagedQueryResponse> response = mock(ApiHttpResponse.class);
    final List<Category> categories =
        Collections.singletonList(readObjectFromResource("category-no-key.json", Category.class));
    final CategoryPagedQueryResponse categoryPagedQueryResponse =
        CategoryPagedQueryResponseBuilder.of()
            .results(categories)
            .limit(20L)
            .offset(0L)
            .count(1L)
            .build();
    when(response.getBody()).thenReturn(categoryPagedQueryResponse);
    when(byProjectKeyCategoriesGet.execute())
        .thenReturn(CompletableFuture.completedFuture(response));

    // test
    final CategorySyncer categorySyncer =
        CategorySyncer.of(sourceClient, mock(ProjectApiRoot.class), mock(Clock.class));
    categorySyncer.sync(null, true).toCompletableFuture().join();

    // assertion
    final LoggingEvent errorLog = syncerTestLogger.getAllLoggingEvents().get(1);
    assertThat(errorLog.getMessage())
        .isEqualTo(
            "Error when trying to sync category. Existing key: <<not present>>. Update actions: []");
    assertThat(errorLog.getThrowable().get().getMessage())
        .isEqualTo(
            format(
                "CategoryDraft with name: %s doesn't have a key. Please make sure all category drafts have keys.",
                categories.get(0).getName().toString()));
  }

  @Test
  void syncWithWarning_ShouldCallWarningCallback() throws JsonProcessingException {
    // preparation: old category has category order hint,
    // new category does not have category order hint
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final ByProjectKeyCategoriesRequestBuilder byProjectKeyCategoriesRequestBuilder = mock();
    when(sourceClient.categories()).thenReturn(byProjectKeyCategoriesRequestBuilder);
    final ByProjectKeyCategoriesGet byProjectKeyCategoriesGetSource = mock();
    when(byProjectKeyCategoriesRequestBuilder.get()).thenReturn(byProjectKeyCategoriesGetSource);
    final ByProjectKeyCategoriesKeyByKeyRequestBuilder
        byProjectKeyCategoriesKeyByKeyRequestBuilder = mock();
    when(byProjectKeyCategoriesRequestBuilder.withKey(anyString()))
        .thenReturn(byProjectKeyCategoriesKeyByKeyRequestBuilder);
    final ByProjectKeyCategoriesKeyByKeyGet byProjectKeyCategoriesKeyByKeyGet = mock();
    when(byProjectKeyCategoriesKeyByKeyRequestBuilder.get())
        .thenReturn(byProjectKeyCategoriesKeyByKeyGet);
    when(byProjectKeyCategoriesGetSource.withSort(anyString()))
        .thenReturn(byProjectKeyCategoriesGetSource);
    when(byProjectKeyCategoriesGetSource.withLimit(anyInt()))
        .thenReturn(byProjectKeyCategoriesGetSource);
    when(byProjectKeyCategoriesGetSource.withWithTotal(anyBoolean()))
        .thenReturn(byProjectKeyCategoriesGetSource);
    final List<Category> sourceCategories =
        Collections.singletonList(
            readObjectFromResource("category-order-hint.json", Category.class));
    final ApiHttpResponse<CategoryPagedQueryResponse> sourceResponse = mock(ApiHttpResponse.class);
    final CategoryPagedQueryResponse categoryPagedQueryResponse =
        CategoryPagedQueryResponseBuilder.of()
            .results(sourceCategories)
            .limit(20L)
            .offset(0L)
            .count(1L)
            .build();
    when(sourceResponse.getBody()).thenReturn(categoryPagedQueryResponse);
    when(byProjectKeyCategoriesGetSource.execute())
        .thenReturn(CompletableFuture.completedFuture(sourceResponse));
    when(byProjectKeyCategoriesKeyByKeyGet.execute())
        .thenReturn(
            CompletableFuture.completedFuture(
                new ApiHttpResponse<Category>(200, null, sourceCategories.get(0))));

    final ProjectApiRoot targetClient = mock(ProjectApiRoot.class);
    final ByProjectKeyCategoriesGet byProjectKeyCategoriesGetTarget = mock();
    when(targetClient.categories()).thenReturn(byProjectKeyCategoriesRequestBuilder);
    when(byProjectKeyCategoriesRequestBuilder.get()).thenReturn(byProjectKeyCategoriesGetTarget);
    when(byProjectKeyCategoriesGetTarget.withWhere(anyString()))
        .thenReturn(byProjectKeyCategoriesGetTarget);
    when(byProjectKeyCategoriesGetTarget.withPredicateVar(anyString(), any()))
        .thenReturn(byProjectKeyCategoriesGetTarget);
    when(byProjectKeyCategoriesGetTarget.withLimit(anyInt()))
        .thenReturn(byProjectKeyCategoriesGetTarget);
    when(byProjectKeyCategoriesGetTarget.withWithTotal(anyBoolean()))
        .thenReturn(byProjectKeyCategoriesGetTarget);
    when(byProjectKeyCategoriesGetTarget.withSort(anyString()))
        .thenReturn(byProjectKeyCategoriesGetTarget);

    final List<Category> targetCategories =
        Collections.singletonList(readObjectFromResource("category-key-2.json", Category.class));
    final ApiHttpResponse<CategoryPagedQueryResponse> targetResponse = mock(ApiHttpResponse.class);
    final CategoryPagedQueryResponse categoryPagedQueryResponseTarget =
        CategoryPagedQueryResponseBuilder.of()
            .results(targetCategories)
            .limit(20L)
            .offset(0L)
            .count(1L)
            .build();
    when(targetResponse.getBody()).thenReturn(categoryPagedQueryResponseTarget);
    when(byProjectKeyCategoriesGetTarget.execute())
        .thenReturn(CompletableFuture.completedFuture(targetResponse));

    mockResourceIdsGraphQlRequest(
        targetClient, "categories", "ba81a6da-cf83-435b-a89e-2afab579846f", "categoryKey2");

    // test
    final CategorySyncer categorySyncer =
        CategorySyncer.of(sourceClient, targetClient, mock(Clock.class));
    categorySyncer.sync(null, true).toCompletableFuture().join();

    // assertion
    final LoggingEvent errorLog = syncerTestLogger.getAllLoggingEvents().get(1);
    assertThat(errorLog.getMessage())
        .isEqualTo("Warning when trying to sync category. Existing key: categoryKey2");
    assertThat(errorLog.getThrowable().get().getMessage())
        .isEqualTo(
            format(
                "Cannot unset 'orderHint' field of category with id '%s'.",
                sourceCategories.get(0).getId()));
  }
}
