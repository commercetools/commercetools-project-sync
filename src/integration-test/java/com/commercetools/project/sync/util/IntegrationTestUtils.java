package com.commercetools.project.sync.util;

import com.commercetools.project.sync.model.LastSyncCustomObject;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.commands.CustomObjectDeleteCommand;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.queries.PagedQueryResult;
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
  public static void deleteCustomObjects(@Nonnull final SphereClient ctpClient) {

    final CustomObjectQuery<LastSyncCustomObject> customObjectQuery =
        CustomObjectQuery.of(LastSyncCustomObject.class);

    final PagedQueryResult<CustomObject<LastSyncCustomObject>> queryResult =
        ctpClient.execute(customObjectQuery).toCompletableFuture().join();

    CompletableFuture.allOf(
            queryResult
                .getResults()
                .stream()
                .map(
                    customObject ->
                        ctpClient.execute(
                            CustomObjectDeleteCommand.of(customObject, LastSyncCustomObject.class)))
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new))
        .join();
  }

  private IntegrationTestUtils() {}
}
