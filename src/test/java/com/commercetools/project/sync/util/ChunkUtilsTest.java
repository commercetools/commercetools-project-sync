package com.commercetools.project.sync.util;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.commons.helpers.ResourceKeyIdGraphQlRequest;
import com.commercetools.sync.commons.models.GraphQlQueryResources;
import com.commercetools.sync.commons.models.ResourceKeyId;
import com.commercetools.sync.commons.models.ResourceKeyIdGraphQlResult;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.queries.CategoryQueryBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.queries.PagedQueryResult;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class ChunkUtilsTest {

  @Test
  void chunk_WithEmptyList_ShouldNotChunkItems() {
    final List<List<Object>> chunk = ChunkUtils.chunk(emptyList(), 5);

    assertThat(chunk).isEmpty();
  }

  @Test
  void chunk_WithList_ShouldChunkItemsIntoMultipleLists() {
    final List<List<String>> chunks =
        ChunkUtils.chunk(asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"), 3);

    assertThat(chunks).hasSize(4);
    assertThat(chunks)
        .isEqualTo(
            asList(
                asList("1", "2", "3"),
                asList("4", "5", "6"),
                asList("7", "8", "9"),
                singletonList("10")));
  }

  @Test
  void executeChunks_withEmptyRequestList_ShouldReturnEmptyList() {
    final List<Object> results =
        ChunkUtils.executeChunks(mock(SphereClient.class), emptyList()).join();

    assertThat(results).isEmpty();
  }

  @Test
  void executeChunks_withQueryBuilderRequests_ShouldReturnResults() {
    final SphereClient client = mock(SphereClient.class);

    final PagedQueryResult<Category> pagedQueryResult = mock(PagedQueryResult.class);
    when(pagedQueryResult.getResults())
        .thenReturn(
            Arrays.asList(mock(Category.class), mock(Category.class), mock(Category.class)));

    when(client.execute(any())).thenReturn(completedFuture(pagedQueryResult));
    when(client.execute(any())).thenReturn(completedFuture(pagedQueryResult));

    final List<PagedQueryResult<Category>> results =
        ChunkUtils.executeChunks(
                client,
                asList(
                    CategoryQueryBuilder.of()
                        .plusPredicates(queryModel -> queryModel.key().isIn(asList("1", "2", "3")))
                        .build(),
                    CategoryQueryBuilder.of()
                        .plusPredicates(queryModel -> queryModel.key().isIn(asList("4", "5", "6")))
                        .build()))
            .join();

    assertThat(results).hasSize(2);

    final List<Category> categories = ChunkUtils.flattenPagedQueryResults(results);
    assertThat(categories).hasSize(6);
  }

  @Test
  void executeChunks_withGraphqlRequests_ShouldReturnResults() {
    final SphereClient client = mock(SphereClient.class);

    final ResourceKeyIdGraphQlResult resourceKeyIdGraphQlResult =
        mock(ResourceKeyIdGraphQlResult.class);
    when(resourceKeyIdGraphQlResult.getResults())
        .thenReturn(
            new HashSet<>(
                Arrays.asList(
                    new ResourceKeyId("coKey1", "coId1"), new ResourceKeyId("coKey2", "coId2"))));

    when(client.execute(any(ResourceKeyIdGraphQlRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(resourceKeyIdGraphQlResult));

    final ResourceKeyIdGraphQlRequest request =
        new ResourceKeyIdGraphQlRequest(singleton("key-1"), GraphQlQueryResources.CATEGORIES);

    final List<ResourceKeyIdGraphQlResult> results =
        ChunkUtils.executeChunks(client, asList(request, request, request)).join();

    assertThat(results).hasSize(3);

    final Set<ResourceKeyId> resourceKeyIds = ChunkUtils.flattenGraphQLBaseResults(results);
    assertThat(resourceKeyIds).hasSize(2);
  }
}
