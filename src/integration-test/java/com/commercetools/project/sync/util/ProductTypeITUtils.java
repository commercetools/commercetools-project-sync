package com.commercetools.project.sync.util;

import com.commercetools.sync.commons.utils.CtpQueryUtils;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereRequest;
import io.sphere.sdk.models.Versioned;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.ProductTypeDeleteCommand;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import javax.annotation.Nonnull;

public final class ProductTypeITUtils {
  /**
   * Deletes all product types from the CTP project defined by the {@code ctpClient}.
   *
   * @param ctpClient defines the CTP project to delete the product types from.
   */
  public static void deleteProductTypes(@Nonnull final SphereClient ctpClient) {
    deleteProductTypesWithRetry(ctpClient);
  }

  private static void deleteProductTypesWithRetry(@Nonnull final SphereClient ctpClient) {
    final Consumer<List<ProductType>> pageConsumer =
        pageElements ->
            CompletableFuture.allOf(
                    pageElements
                        .stream()
                        .map(productType -> deleteProductTypeWithRetry(ctpClient, productType))
                        .map(CompletionStage::toCompletableFuture)
                        .toArray(CompletableFuture[]::new))
                .join();

    CtpQueryUtils.queryAll(ctpClient, ProductTypeQuery.of(), pageConsumer)
        .toCompletableFuture()
        .join();
  }

  private static CompletionStage<ProductType> deleteProductTypeWithRetry(
      @Nonnull final SphereClient ctpClient, @Nonnull final ProductType productType) {
    return ctpClient
        .execute(ProductTypeDeleteCommand.of(productType))
        .handle(
            (result, throwable) -> {
              if (throwable instanceof ConcurrentModificationException) {
                Long currentVersion =
                    ((ConcurrentModificationException) throwable).getCurrentVersion();
                SphereRequest<ProductType> retry =
                    ProductTypeDeleteCommand.of(Versioned.of(productType.getId(), currentVersion));
                ctpClient.execute(retry);
              }
              return result;
            });
  }
}
