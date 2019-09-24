package com.commercetools.project.sync.util;

import static com.commercetools.project.sync.service.impl.CustomObjectServiceImpl.TIMESTAMP_GENERATOR_KEY;
import static java.lang.String.format;

import com.commercetools.project.sync.model.response.LastSyncCustomObject;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.commands.CustomObjectDeleteCommand;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.queries.QueryPredicate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
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
      @Nonnull final SphereClient ctpClient, @Nonnull final String sourceProjectKey) {

    // 1. First query for the time generator custom object
    final QueryPredicate<CustomObject<String>> timeGeneratorPredicate =
        QueryPredicate.of(format("key=\"%s\"", TIMESTAMP_GENERATOR_KEY));

    final CustomObjectQuery<String> timeGeneratorCustomObjectQuery =
        CustomObjectQuery.of(String.class).plusPredicates(timeGeneratorPredicate);

    final List<CompletableFuture> deletionStages = new ArrayList<>();

    final CompletableFuture<Void> timeGeneratorDeletionsStage =
        ctpClient
            .execute(timeGeneratorCustomObjectQuery)
            .thenApply(PagedQueryResult::getResults)
            .thenCompose(
                customObjects ->
                    CompletableFuture.allOf(
                        customObjects
                            .stream()
                            .map(
                                customObject ->
                                    ctpClient.execute(
                                        CustomObjectDeleteCommand.of(customObject, String.class)))
                            .map(CompletionStage::toCompletableFuture)
                            .toArray(CompletableFuture[]::new)))
            .toCompletableFuture();

    deletionStages.add(timeGeneratorDeletionsStage);

    // 2. Then query for the lastSync custom objects
    final QueryPredicate<CustomObject<LastSyncCustomObject>> lastSyncPredicate =
        QueryPredicate.of(format("key=\"%s\"", sourceProjectKey));

    final CustomObjectQuery<LastSyncCustomObject> lastSyncCustomObjectQuery =
        CustomObjectQuery.of(LastSyncCustomObject.class).plusPredicates(lastSyncPredicate);

    final CompletableFuture<Void> lastSyncCustomObjectDeletionFutures =
        ctpClient
            .execute(lastSyncCustomObjectQuery)
            .thenApply(PagedQueryResult::getResults)
            .thenCompose(
                customObjects ->
                    CompletableFuture.allOf(
                        customObjects
                            .stream()
                            .map(
                                customObject ->
                                    ctpClient.execute(
                                        CustomObjectDeleteCommand.of(
                                            customObject, LastSyncCustomObject.class)))
                            .map(CompletionStage::toCompletableFuture)
                            .toArray(CompletableFuture[]::new)))
            .toCompletableFuture();

    deletionStages.add(lastSyncCustomObjectDeletionFutures);

    // 3. Then delete all in parallel
    CompletableFuture.allOf(deletionStages.toArray(new CompletableFuture[0])).join();
  }

  private IntegrationTestUtils() {}
}
