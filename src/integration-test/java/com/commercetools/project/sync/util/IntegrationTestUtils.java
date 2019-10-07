package com.commercetools.project.sync.util;

import static com.commercetools.project.sync.service.impl.CustomObjectServiceImpl.TIMESTAMP_GENERATOR_KEY;
import static com.commercetools.project.sync.util.QueryUtils.queryAndExecute;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.project.sync.model.response.LastSyncCustomObject;
import com.commercetools.sync.commons.utils.CtpQueryUtils;
import io.sphere.sdk.cartdiscounts.commands.CartDiscountDeleteCommand;
import io.sphere.sdk.cartdiscounts.queries.CartDiscountQuery;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.commands.CategoryDeleteCommand;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.commands.CustomObjectDeleteCommand;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.inventory.commands.InventoryEntryDeleteCommand;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.commands.ProductDeleteCommand;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.ProductTypeDeleteCommand;
import io.sphere.sdk.producttypes.commands.ProductTypeUpdateCommand;
import io.sphere.sdk.producttypes.commands.updateactions.RemoveAttributeDefinition;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.queries.QueryPredicate;
import io.sphere.sdk.types.commands.TypeDeleteCommand;
import io.sphere.sdk.types.queries.TypeQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
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

  public static void cleanUpProjects(
      @Nonnull final SphereClient sourceClient, @Nonnull final SphereClient targetClient) {

    deleteProjectData(sourceClient);
    deleteProjectData(targetClient);
    deleteLastSyncCustomObjects(targetClient, sourceClient.getConfig().getProjectKey());
  }

  private static void deleteProjectData(@Nonnull final SphereClient client) {
    queryAndExecute(client, CategoryQuery.of(), CategoryDeleteCommand::of);
    queryAndExecute(client, ProductQuery.of(), ProductDeleteCommand::of);
    queryAndExecute(client, TypeQuery.of(), TypeDeleteCommand::of);
    queryAndExecute(client, InventoryEntryQuery.of(), InventoryEntryDeleteCommand::of);
    queryAndExecute(client, CartDiscountQuery.of(), CartDiscountDeleteCommand::of);
    deleteProductTypes(client);
  }

  private static void deleteProductTypes(@Nonnull final SphereClient ctpClient) {
    deleteProductTypeAttributes(ctpClient);
    queryAndExecute(ctpClient, ProductTypeQuery.of(), ProductTypeDeleteCommand::of);
  }

  private static void deleteProductTypeAttributes(@Nonnull final SphereClient ctpClient) {
    final ConcurrentHashMap<ProductType, Set<UpdateAction<ProductType>>> productTypesToUpdate =
        new ConcurrentHashMap<>();

    CtpQueryUtils.queryAll(
            ctpClient,
            ProductTypeQuery.of(),
            page -> {
              page.forEach(
                  productType -> {
                    final Set<UpdateAction<ProductType>> removeActions =
                        productType
                            .getAttributes()
                            .stream()
                            .map(
                                attributeDefinition ->
                                    RemoveAttributeDefinition.of(attributeDefinition.getName()))
                            .collect(Collectors.toSet());
                    productTypesToUpdate.put(productType, removeActions);
                  });
            })
        .thenCompose(
            aVoid ->
                CompletableFuture.allOf(
                    productTypesToUpdate
                        .entrySet()
                        .stream()
                        .map(
                            entry ->
                                ctpClient.execute(
                                    ProductTypeUpdateCommand.of(
                                        entry.getKey(), new ArrayList<>(entry.getValue()))))
                        .toArray(CompletableFuture[]::new)))
        .toCompletableFuture()
        .join();
  }

  @Nonnull
  public static ProductType assertProductTypeExists(
      @Nonnull final SphereClient ctpClient, @Nonnull final String productTypeKey) {
    final PagedQueryResult<ProductType> productTypeQueryResult =
        ctpClient.execute(ProductTypeQuery.of().byKey(productTypeKey)).toCompletableFuture().join();

    assertThat(productTypeQueryResult.getResults())
        .hasSize(1)
        .hasOnlyOneElementSatisfying(
            productType -> assertThat(productType.getKey()).isEqualTo(productTypeKey));

    return productTypeQueryResult.getResults().get(0);
  }

  @Nonnull
  public static Category assertCategoryExists(
      @Nonnull final SphereClient ctpClient, @Nonnull final String key) {
    final String queryPredicate = format("key=\"%s\"", key);

    final PagedQueryResult<Category> categoryQueryResult =
        ctpClient
            .execute(CategoryQuery.of().withPredicates(QueryPredicate.of(queryPredicate)))
            .toCompletableFuture()
            .join();

    assertThat(categoryQueryResult.getResults())
        .hasSize(1)
        .hasOnlyOneElementSatisfying(category -> assertThat(category.getKey()).isEqualTo(key));

    return categoryQueryResult.getResults().get(0);
  }

  @Nonnull
  public static Product assertProductExists(
      @Nonnull final SphereClient targetClient,
      @Nonnull final String productKey,
      @Nonnull final String masterVariantKey,
      @Nonnull final String masterVariantSku) {
    final PagedQueryResult<Product> productQueryResult =
        targetClient.execute(ProductQuery.of()).toCompletableFuture().join();

    assertThat(productQueryResult.getResults())
        .hasSize(1)
        .hasOnlyOneElementSatisfying(
            product -> {
              assertThat(product.getKey()).isEqualTo(productKey);
              final ProductVariant stagedMasterVariant =
                  product.getMasterData().getStaged().getMasterVariant();
              assertThat(stagedMasterVariant.getKey()).isEqualTo(masterVariantKey);
              assertThat(stagedMasterVariant.getSku()).isEqualTo(masterVariantSku);
            });

    return productQueryResult.getResults().get(0);
  }

  private IntegrationTestUtils() {}
}
