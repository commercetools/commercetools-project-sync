package com.commercetools.project.sync.util;

import static com.commercetools.project.sync.service.impl.CustomObjectServiceImpl.TIMESTAMP_GENERATOR_KEY;
import static java.lang.String.format;

import com.commercetools.project.sync.model.LastSyncCustomObject;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.commands.CustomObjectDeleteCommand;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.queries.PagedResult;
import io.sphere.sdk.queries.QueryPredicate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public final class IntegrationTestUtils {

  /**
   * Since this method is expected to be used only by tests, it only works on projects with equal or
   * less than 20 custom objects. Otherwise, it won't delete all the custom objects in the project
   * of the client.
   *
   * @param ctpClient the client to delete the custom objects from.
   */
  public static void deleteLastSyncCustomObjects(
      @Nonnull final SphereClient ctpClient, @Nonnull final String targetProjectKey) {

    // 1. First query for the time generator custom object
    final QueryPredicate<CustomObject<String>> timeGeneratorPredicate =
        QueryPredicate.of(format("key=\"%s\"", TIMESTAMP_GENERATOR_KEY));

    final CustomObjectQuery<String> timeGeneratorCustomObjectQuery =
        CustomObjectQuery.of(String.class).plusPredicates(timeGeneratorPredicate);

    final List<CompletableFuture> deletionStages = new ArrayList<>();

    final CompletableFuture<CustomObject<String>> deleteTimeGeneratorCustomObjectFuture =
        ctpClient
            .execute(timeGeneratorCustomObjectQuery)
            .thenApply(PagedResult::head)
            .thenCompose(
                optionalCustomObject ->
                    optionalCustomObject
                        .map(
                            customObject ->
                                ctpClient.execute(
                                    CustomObjectDeleteCommand.of(customObject, String.class)))
                        .orElseGet(() -> CompletableFuture.completedFuture(null)))
            .toCompletableFuture();

    deletionStages.add(deleteTimeGeneratorCustomObjectFuture);

    // 2. Then query for the lastSync custom objects
    final QueryPredicate<CustomObject<LastSyncCustomObject>> lastSyncPredicate =
        QueryPredicate.of(format("key=\"%s\"", targetProjectKey));

    final CustomObjectQuery<LastSyncCustomObject> lastSyncCustomObjectQuery =
        CustomObjectQuery.of(LastSyncCustomObject.class).plusPredicates(lastSyncPredicate);

    final PagedQueryResult<CustomObject<LastSyncCustomObject>> queryResult =
        ctpClient.execute(lastSyncCustomObjectQuery).toCompletableFuture().join();

    final List<CompletableFuture<CustomObject<LastSyncCustomObject>>>
        lastSyncCustomObjectDeletionFutures =
            queryResult
                .getResults()
                .stream()
                .map(
                    customObject ->
                        ctpClient.execute(
                            CustomObjectDeleteCommand.of(customObject, LastSyncCustomObject.class)))
                .map(CompletionStage::toCompletableFuture)
                .collect(Collectors.toList());

    deletionStages.addAll(lastSyncCustomObjectDeletionFutures);

    // 3. Then delete all in parallel
    CompletableFuture.allOf(deletionStages.toArray(new CompletableFuture[0])).join();
  }

  private IntegrationTestUtils() {}
}
