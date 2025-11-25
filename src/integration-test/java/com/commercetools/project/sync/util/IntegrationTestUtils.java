package com.commercetools.project.sync.util;

import static com.commercetools.project.sync.service.impl.CustomObjectServiceImpl.TIMESTAMP_GENERATOR_KEY;
import static com.commercetools.project.sync.util.CtpClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.project.sync.util.CtpClientUtils.CTP_TARGET_CLIENT;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.client.ByProjectKeyCustomObjectsGet;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.client.QueryUtils;
import com.commercetools.api.client.error.ConcurrentModificationException;
import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.product.Product;
import com.commercetools.api.models.product.ProductUnpublishActionBuilder;
import com.commercetools.api.models.product.ProductVariant;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeRemoveAttributeDefinitionActionBuilder;
import com.commercetools.api.models.state.State;
import com.commercetools.project.sync.SyncerFactory;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.http.HttpStatusCode;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;

public final class IntegrationTestUtils {

  private static final int MAX_RETRY = 10;

  public static SyncerFactory createITSyncerFactory() {
    return SyncerFactory.of(
        () -> CTP_SOURCE_CLIENT, () -> CTP_TARGET_CLIENT, Clock.systemDefaultZone(), false);
  }

  /**
   * Since this method is expected to be used only by tests, it only works on projects with equal or
   * less than 20 custom objects. Otherwise, it won't delete all the custom objects in the project
   * of the client.
   *
   * @param ctpClient the client to delete the custom objects from.
   */
  public static void deleteLastSyncCustomObjects(
      @Nonnull final ProjectApiRoot ctpClient, @Nonnull final String sourceProjectKey) {

    // 1. First query for the time generator custom object
    final ByProjectKeyCustomObjectsGet timeGeneratorCustomObjectQuery =
        ctpClient.customObjects().get().withWhere(format("key=\"%s\"", TIMESTAMP_GENERATOR_KEY));

    final List<CompletableFuture> deletionStages = new ArrayList<>();

    final CompletableFuture<Void> timeGeneratorDeletionsStage =
        timeGeneratorCustomObjectQuery
            .execute()
            .thenApply(
                customObjectPagedQueryResponseApiHttpResponse ->
                    customObjectPagedQueryResponseApiHttpResponse.getBody().getResults())
            .thenCompose(
                customObjects ->
                    CompletableFuture.allOf(
                        customObjects.stream()
                            .map(
                                customObject ->
                                    ctpClient
                                        .customObjects()
                                        .withContainerAndKey(
                                            customObject.getContainer(), customObject.getKey())
                                        .delete()
                                        .execute())
                            .toArray(CompletableFuture[]::new)))
            .toCompletableFuture();

    deletionStages.add(timeGeneratorDeletionsStage);

    // 2. Then query for the lastSync custom objects
    final ByProjectKeyCustomObjectsGet lastSyncCustomObjectQuery =
        ctpClient.customObjects().get().withWhere(format("key=\"%s\"", sourceProjectKey));

    final CompletableFuture<Void> lastSyncCustomObjectDeletionFutures =
        lastSyncCustomObjectQuery
            .execute()
            .thenApply(
                customObjectPagedQueryResponseApiHttpResponse ->
                    customObjectPagedQueryResponseApiHttpResponse.getBody().getResults())
            .thenCompose(
                customObjects ->
                    CompletableFuture.allOf(
                        customObjects.stream()
                            .map(
                                customObject ->
                                    ctpClient
                                        .customObjects()
                                        .withContainerAndKey(
                                            customObject.getContainer(), customObject.getKey())
                                        .delete()
                                        .execute())
                            .map(CompletionStage::toCompletableFuture)
                            .toArray(CompletableFuture[]::new)))
            .toCompletableFuture();

    deletionStages.add(lastSyncCustomObjectDeletionFutures);

    // 3. Then delete all in parallel
    CompletableFuture.allOf(deletionStages.toArray(new CompletableFuture[0])).join();
  }

  public static void cleanUpProjects(
      @Nonnull final ProjectApiRoot sourceClient, @Nonnull final ProjectApiRoot targetClient) {
    deleteProjectData(sourceClient);
    deleteProjectData(targetClient);
    deleteLastSyncCustomObjects(targetClient, sourceClient.getProjectKey());
  }

  private static void deleteProjectData(@Nonnull final ProjectApiRoot client) {
    QueryUtils.queryAll(
            client.customers().get(),
            customers -> {
              CompletableFuture.allOf(
                      customers.stream()
                          .map(
                              customer ->
                                  client
                                      .customers()
                                      .delete(customer)
                                      .execute()
                                      .thenApply(ApiHttpResponse::getBody))
                          .map(CompletionStage::toCompletableFuture)
                          .toArray(CompletableFuture[]::new))
                  .join();
            })
        .toCompletableFuture()
        .join();
    QueryUtils.queryAll(
            client.categories().get(),
            categories -> {
              CompletableFuture.allOf(
                      categories.stream()
                          .map(
                              category ->
                                  client
                                      .categories()
                                      .delete(category)
                                      .execute()
                                      .thenApply(ApiHttpResponse::getBody))
                          .map(CompletionStage::toCompletableFuture)
                          .toArray(CompletableFuture[]::new))
                  .join();
            })
        .toCompletableFuture()
        .join();
    QueryUtils.queryAll(
            client.products().get(),
            products -> {
              CompletableFuture.allOf(
                      products.stream()
                          .map(
                              product ->
                                  client
                                      .products()
                                      .update(product)
                                      .with(
                                          actionBuilder ->
                                              actionBuilder.plus(
                                                  ProductUnpublishActionBuilder.of()))
                                      .execute()
                                      .thenApply(ApiHttpResponse::getBody))
                          .map(CompletionStage::toCompletableFuture)
                          .toArray(CompletableFuture[]::new))
                  .join();
            })
        .toCompletableFuture()
        .join();

    final CompletableFuture<Void> deleteProduct =
        QueryUtils.queryAll(
                client.products().get(),
                products -> {
                  CompletableFuture.allOf(
                          products.stream()
                              .map(
                                  product ->
                                      client
                                          .products()
                                          .delete(product)
                                          .execute()
                                          .thenApply(ApiHttpResponse::getBody))
                              .map(CompletionStage::toCompletableFuture)
                              .toArray(CompletableFuture[]::new))
                      .join();
                })
            .toCompletableFuture();

    final CompletableFuture<Void> deleteInventory =
        QueryUtils.queryAll(
                client.inventory().get(),
                inventoryEntries -> {
                  CompletableFuture.allOf(
                          inventoryEntries.stream()
                              .map(
                                  inventoryEntry ->
                                      client
                                          .inventory()
                                          .delete(inventoryEntry)
                                          .execute()
                                          .thenApply(ApiHttpResponse::getBody))
                              .map(CompletionStage::toCompletableFuture)
                              .toArray(CompletableFuture[]::new))
                      .join();
                })
            .toCompletableFuture();

    final CompletableFuture<Void> deleteCartDiscount =
        QueryUtils.queryAll(
                client.cartDiscounts().get(),
                cartDiscounts -> {
                  CompletableFuture.allOf(
                          cartDiscounts.stream()
                              .map(
                                  cartDiscount ->
                                      client
                                          .cartDiscounts()
                                          .delete(cartDiscount)
                                          .execute()
                                          .thenApply(ApiHttpResponse::getBody))
                              .map(CompletionStage::toCompletableFuture)
                              .toArray(CompletableFuture[]::new))
                      .join();
                })
            .toCompletableFuture();

    final CompletableFuture<Void> deleteCustomObject =
        QueryUtils.queryAll(
                client.customObjects().get(),
                customObjects -> {
                  CompletableFuture.allOf(
                          customObjects.stream()
                              .map(
                                  customObject ->
                                      client
                                          .customObjects()
                                          .withContainerAndKey(
                                              customObject.getContainer(), customObject.getKey())
                                          .delete()
                                          .execute()
                                          .thenApply(ApiHttpResponse::getBody))
                              .map(CompletionStage::toCompletableFuture)
                              .toArray(CompletableFuture[]::new))
                      .join();
                })
            .toCompletableFuture();

    final CompletableFuture<Void> deleteProductDiscount =
        QueryUtils.queryAll(
                client.productDiscounts().get(),
                productDiscounts -> {
                  CompletableFuture.allOf(
                          productDiscounts.stream()
                              .map(
                                  productDiscount ->
                                      client
                                          .productDiscounts()
                                          .delete(productDiscount)
                                          .execute()
                                          .thenApply(ApiHttpResponse::getBody))
                              .map(CompletionStage::toCompletableFuture)
                              .toArray(CompletableFuture[]::new))
                      .join();
                })
            .toCompletableFuture();

    CompletableFuture.allOf(
            deleteProduct,
            deleteInventory,
            deleteCartDiscount,
            deleteCustomObject,
            deleteProductDiscount)
        .join();

    QueryUtils.queryAll(
            client.states().get().withWhere("builtIn=\"false\""),
            states -> {
              CompletableFuture.allOf(
                      states.stream()
                          .map(
                              state ->
                                  client
                                      .states()
                                      .delete(state)
                                      .execute()
                                      .thenApply(ApiHttpResponse::getBody))
                          .map(CompletionStage::toCompletableFuture)
                          .toArray(CompletableFuture[]::new))
                  .join();
            })
        .toCompletableFuture()
        .join();

    QueryUtils.queryAll(
            client.shoppingLists().get(),
            shoppingLists -> {
              CompletableFuture.allOf(
                      shoppingLists.stream()
                          .map(
                              shoppingList ->
                                  client
                                      .shoppingLists()
                                      .delete(shoppingList)
                                      .execute()
                                      .thenApply(ApiHttpResponse::getBody))
                          .map(CompletionStage::toCompletableFuture)
                          .toArray(CompletableFuture[]::new))
                  .join();
            })
        .toCompletableFuture()
        .join();

    final CompletableFuture<Void> deleteType =
        QueryUtils.queryAll(
                client.types().get(),
                types -> {
                  CompletableFuture.allOf(
                          types.stream()
                              .map(
                                  type ->
                                      client
                                          .types()
                                          .delete(type)
                                          .execute()
                                          .thenApply(ApiHttpResponse::getBody))
                              .map(CompletionStage::toCompletableFuture)
                              .toArray(CompletableFuture[]::new))
                      .join();
                })
            .toCompletableFuture();

    final CompletableFuture<Void> deleteShippingMethod =
        QueryUtils.queryAll(
                client.shippingMethods().get(),
                shippingMethods -> {
                  CompletableFuture.allOf(
                          shippingMethods.stream()
                              .map(
                                  shippingMethod ->
                                      client
                                          .shippingMethods()
                                          .delete(shippingMethod)
                                          .execute()
                                          .thenApply(ApiHttpResponse::getBody))
                              .map(CompletionStage::toCompletableFuture)
                              .toArray(CompletableFuture[]::new))
                      .join();
                })
            .toCompletableFuture();

    final CompletableFuture<Void> deleteTaxCategory =
        QueryUtils.queryAll(
                client.taxCategories().get(),
                taxCategories -> {
                  CompletableFuture.allOf(
                          taxCategories.stream()
                              .map(
                                  taxCategory ->
                                      client
                                          .taxCategories()
                                          .delete(taxCategory)
                                          .execute()
                                          .thenApply(ApiHttpResponse::getBody))
                              .map(CompletionStage::toCompletableFuture)
                              .toArray(CompletableFuture[]::new))
                      .join();
                })
            .toCompletableFuture();

    final CompletableFuture<Void> deleteCustomer =
        QueryUtils.queryAll(
                client.customers().get(),
                customers -> {
                  CompletableFuture.allOf(
                          customers.stream()
                              .map(
                                  customer ->
                                      client
                                          .customers()
                                          .delete(customer)
                                          .execute()
                                          .thenApply(ApiHttpResponse::getBody))
                              .map(CompletionStage::toCompletableFuture)
                              .toArray(CompletableFuture[]::new))
                      .join();
                })
            .toCompletableFuture();

    final CompletableFuture<Void> deleteCustomerGroup =
        QueryUtils.queryAll(
                client.customerGroups().get(),
                customerGroups -> {
                  CompletableFuture.allOf(
                          customerGroups.stream()
                              .map(
                                  customerGroup ->
                                      client
                                          .customerGroups()
                                          .delete(customerGroup)
                                          .execute()
                                          .thenApply(ApiHttpResponse::getBody))
                              .map(CompletionStage::toCompletableFuture)
                              .toArray(CompletableFuture[]::new))
                      .join();
                })
            .toCompletableFuture();

    final CompletableFuture<Void> deleteChannel =
        QueryUtils.queryAll(
                client.channels().get(),
                channels -> {
                  CompletableFuture.allOf(
                          channels.stream()
                              .map(
                                  channel ->
                                      client
                                          .channels()
                                          .delete(channel)
                                          .execute()
                                          .thenApply(ApiHttpResponse::getBody))
                              .map(CompletionStage::toCompletableFuture)
                              .toArray(CompletableFuture[]::new))
                      .join();
                })
            .toCompletableFuture();

    CompletableFuture.allOf(
            deleteType,
            deleteShippingMethod,
            deleteTaxCategory,
            deleteCustomer,
            deleteCustomerGroup,
            deleteChannel)
        .join();
    deleteProductTypes(client);
  }

  private static void deleteProductTypes(@Nonnull final ProjectApiRoot ctpClient) {
    deleteProductTypeAttributes(ctpClient);
    deleteProductTypesWithRetry(ctpClient);
  }

  private static void deleteProductTypesWithRetry(@Nonnull final ProjectApiRoot ctpClient) {
    withRetry(
        () ->
            QueryUtils.queryAll(
                    ctpClient.productTypes().get(),
                    productTypes -> {
                      CompletableFuture.allOf(
                              productTypes.stream()
                                  .map(
                                      productType ->
                                          ctpClient
                                              .productTypes()
                                              .delete(productType)
                                              .execute()
                                              .thenApply(ApiHttpResponse::getBody))
                                  .map(CompletionStage::toCompletableFuture)
                                  .toArray(CompletableFuture[]::new))
                          .join();
                    })
                .toCompletableFuture()
                .join());
  }

  private static void deleteProductTypeAttributes(@Nonnull final ProjectApiRoot ctpClient) {
    withRetry(
        () ->
            QueryUtils.queryAll(
                    ctpClient.productTypes().get(),
                    productTypes -> {
                      CompletableFuture.allOf(
                              productTypes.stream()
                                  .map(
                                      productType ->
                                          ctpClient
                                              .productTypes()
                                              .update(productType)
                                              .with(
                                                  builder -> {
                                                    productType
                                                        .getAttributes()
                                                        .forEach(
                                                            attributeDefinition ->
                                                                builder.plus(
                                                                    ProductTypeRemoveAttributeDefinitionActionBuilder
                                                                        .of()
                                                                        .name(
                                                                            attributeDefinition
                                                                                .getName())));
                                                    return builder;
                                                  })
                                              .execute()
                                              .thenApply(ApiHttpResponse::getBody))
                                  .map(CompletionStage::toCompletableFuture)
                                  .toArray(CompletableFuture[]::new))
                          .join();
                    })
                .toCompletableFuture()
                .join());
  }

  private static void withRetry(final Runnable action) {
    for (int i = 0; i < MAX_RETRY; i++) {
      try {
        action.run();
      } catch (Exception e) {
        if (!e.getCause().getClass().equals(ConcurrentModificationException.class)) {
          throw e;
        }
      }
    }
  }

  @Nonnull
  public static ProductType assertProductTypeExists(
      @Nonnull final ProjectApiRoot ctpClient, @Nonnull final String key) {
    final ApiHttpResponse<ProductType> productTypeApiHttpResponse =
        ctpClient.productTypes().withKey(key).get().execute().toCompletableFuture().join();

    assertThat(productTypeApiHttpResponse.getStatusCode()).isEqualTo(HttpStatusCode.OK_200);
    assertThat(productTypeApiHttpResponse.getBody()).isNotNull();
    assertThat(productTypeApiHttpResponse.getBody().getKey()).isEqualTo(key);

    return productTypeApiHttpResponse.getBody();
  }

  @Nonnull
  public static Category assertCategoryExists(
      @Nonnull final ProjectApiRoot ctpClient, @Nonnull final String key) {
    final ApiHttpResponse<Category> categoryApiHttpResponse =
        ctpClient.categories().withKey(key).get().execute().toCompletableFuture().join();

    assertThat(categoryApiHttpResponse.getStatusCode()).isEqualTo(HttpStatusCode.OK_200);
    assertThat(categoryApiHttpResponse.getBody()).isNotNull();
    assertThat(categoryApiHttpResponse.getBody().getKey()).isEqualTo(key);

    return categoryApiHttpResponse.getBody();
  }

  @Nonnull
  public static State assertStateExists(
      @Nonnull final ProjectApiRoot ctpClient, @Nonnull final String key) {
    final ApiHttpResponse<State> stateApiHttpResponse =
        ctpClient.states().withKey(key).get().execute().toCompletableFuture().join();

    assertThat(stateApiHttpResponse.getStatusCode()).isEqualTo(HttpStatusCode.OK_200);
    assertThat(stateApiHttpResponse.getBody()).isNotNull();
    assertThat(stateApiHttpResponse.getBody().getKey()).isEqualTo(key);

    return stateApiHttpResponse.getBody();
  }

  @Nonnull
  public static Customer assertCustomerExists(
      @Nonnull final ProjectApiRoot ctpClient, @Nonnull final String key) {
    final ApiHttpResponse<Customer> customerApiHttpResponse =
        ctpClient.customers().withKey(key).get().execute().toCompletableFuture().join();

    assertThat(customerApiHttpResponse.getStatusCode()).isEqualTo(HttpStatusCode.OK_200);
    assertThat(customerApiHttpResponse.getBody()).isNotNull();
    assertThat(customerApiHttpResponse.getBody().getKey()).isEqualTo(key);

    return customerApiHttpResponse.getBody();
  }

  @Nonnull
  public static Product assertProductExists(
      @Nonnull final ProjectApiRoot ctpClient,
      @Nonnull final String productKey,
      @Nonnull final String masterVariantKey,
      @Nonnull final String masterVariantSku) {
    final ApiHttpResponse<Product> productApiHttpResponse =
        ctpClient.products().withKey(productKey).get().execute().toCompletableFuture().join();

    assertThat(productApiHttpResponse.getStatusCode()).isEqualTo(HttpStatusCode.OK_200);
    assertThat(productApiHttpResponse.getBody()).isNotNull();
    assertThat(productApiHttpResponse.getBody())
        .satisfies(
            product -> {
              assertThat(product.getKey()).isEqualTo(productKey);
              final ProductVariant stagedMasterVariant =
                  product.getMasterData().getStaged().getMasterVariant();
              assertThat(stagedMasterVariant.getKey()).isEqualTo(masterVariantKey);
              assertThat(stagedMasterVariant.getSku()).isEqualTo(masterVariantSku);
            });

    return productApiHttpResponse.getBody();
  }

  @Nonnull
  public static ObjectNode createReferenceOfType(
      @Nonnull final String typeId, @Nonnull final String id) {
    final ObjectNode referenceObject = JsonNodeFactory.instance.objectNode();
    referenceObject.set("typeId", JsonNodeFactory.instance.textNode(typeId));
    referenceObject.set("id", JsonNodeFactory.instance.textNode(id));
    return referenceObject;
  }

  @Nonnull
  public static ObjectNode createAttributeObject(
      @Nonnull final String name, @Nonnull final ArrayNode value) {
    final ObjectNode attributeObject = JsonNodeFactory.instance.objectNode();
    attributeObject.set("name", JsonNodeFactory.instance.textNode(name));
    attributeObject.set("value", value);
    return attributeObject;
  }

  private IntegrationTestUtils() {}
}
