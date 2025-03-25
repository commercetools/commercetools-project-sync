package com.commercetools.project.sync;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.project.sync.util.CtpClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.project.sync.util.CtpClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.project.sync.util.IntegrationTestUtils.assertProductTypeExists;
import static com.commercetools.project.sync.util.IntegrationTestUtils.cleanUpProjects;
import static com.commercetools.project.sync.util.IntegrationTestUtils.createITSyncerFactory;
import static com.commercetools.project.sync.util.IntegrationTestUtils.createReferenceOfType;
import static com.commercetools.project.sync.util.TestUtils.assertCartDiscountSyncerLoggingEvents;
import static com.commercetools.project.sync.util.TestUtils.assertCategorySyncerLoggingEvents;
import static com.commercetools.project.sync.util.TestUtils.assertCustomObjectSyncerLoggingEvents;
import static com.commercetools.project.sync.util.TestUtils.assertCustomerSyncerLoggingEvents;
import static com.commercetools.project.sync.util.TestUtils.assertInventoryEntrySyncerLoggingEvents;
import static com.commercetools.project.sync.util.TestUtils.assertProductSyncerLoggingEvents;
import static com.commercetools.project.sync.util.TestUtils.assertProductTypeSyncerLoggingEvents;
import static com.commercetools.project.sync.util.TestUtils.assertShoppingListSyncerLoggingEvents;
import static com.commercetools.project.sync.util.TestUtils.assertStateSyncerLoggingEvents;
import static com.commercetools.project.sync.util.TestUtils.assertTaxCategorySyncerLoggingEvents;
import static com.commercetools.project.sync.util.TestUtils.assertTypeSyncerLoggingEvents;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.client.QueryUtils;
import com.commercetools.api.models.common.ReferenceTypeId;
import com.commercetools.api.models.custom_object.CustomObjectPagedQueryResponse;
import com.commercetools.api.models.product.AttributeBuilder;
import com.commercetools.api.models.product.Product;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductDraftBuilder;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.product.ProductVariantDraftBuilder;
import com.commercetools.api.models.product_type.AttributeConstraintEnum;
import com.commercetools.api.models.product_type.AttributeDefinitionDraft;
import com.commercetools.api.models.product_type.AttributeDefinitionDraftBuilder;
import com.commercetools.api.models.product_type.AttributeReferenceTypeId;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypeDraftBuilder;
import com.commercetools.project.sync.cartdiscount.CartDiscountSyncer;
import com.commercetools.project.sync.category.CategorySyncer;
import com.commercetools.project.sync.customer.CustomerSyncer;
import com.commercetools.project.sync.customobject.CustomObjectSyncer;
import com.commercetools.project.sync.inventoryentry.InventoryEntrySyncer;
import com.commercetools.project.sync.product.ProductSyncer;
import com.commercetools.project.sync.producttype.ProductTypeSyncer;
import com.commercetools.project.sync.shoppinglist.ShoppingListSyncer;
import com.commercetools.project.sync.state.StateSyncer;
import com.commercetools.project.sync.taxcategory.TaxCategorySyncer;
import com.commercetools.project.sync.type.TypeSyncer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.valfirst.slf4jtest.TestLogger;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

// This will suppress MoreThanOneLogger warnings in this class
@SuppressWarnings("PMD.MoreThanOneLogger")
class ProductSyncWithReferencesIT {

  private static final TestLogger productSyncerTestLogger =
      TestLoggerFactory.getTestLogger(ProductSyncer.class);
  private static final TestLogger cliRunnerTestLogger =
      TestLoggerFactory.getTestLogger(CliRunner.class);
  private static final TestLogger productTypeSyncerTestLogger =
      TestLoggerFactory.getTestLogger(ProductTypeSyncer.class);
  private static final TestLogger customerSyncerTestLogger =
      TestLoggerFactory.getTestLogger(CustomerSyncer.class);
  private static final TestLogger shoppingListSyncerTestLogger =
      TestLoggerFactory.getTestLogger(ShoppingListSyncer.class);
  private static final TestLogger stateSyncerTestLogger =
      TestLoggerFactory.getTestLogger(StateSyncer.class);
  private static final TestLogger inventoryEntrySyncerTestLogger =
      TestLoggerFactory.getTestLogger(InventoryEntrySyncer.class);
  private static final TestLogger customObjectSyncerTestLogger =
      TestLoggerFactory.getTestLogger(CustomObjectSyncer.class);
  private static final TestLogger typeSyncerTestLogger =
      TestLoggerFactory.getTestLogger(TypeSyncer.class);
  private static final TestLogger categorySyncerTestLogger =
      TestLoggerFactory.getTestLogger(CategorySyncer.class);
  private static final TestLogger cartDiscountSyncerTestLogger =
      TestLoggerFactory.getTestLogger(CartDiscountSyncer.class);
  private static final TestLogger taxCategorySyncerTestLogger =
      TestLoggerFactory.getTestLogger(TaxCategorySyncer.class);

  private static final String MAIN_PRODUCT_TYPE_KEY = "main-product-type";
  private static final String MAIN_PRODUCT_MASTER_VARIANT_KEY = "main-product-master-variant-key";
  private static final String MAIN_PRODUCT_KEY = "product-with-references";

  @BeforeEach
  void setup() {
    cleanUpProjects(CTP_SOURCE_CLIENT, CTP_TARGET_CLIENT);
    cliRunnerTestLogger.clearAll();
    productSyncerTestLogger.clearAll();
    productTypeSyncerTestLogger.clearAll();
    customerSyncerTestLogger.clearAll();
    shoppingListSyncerTestLogger.clearAll();
    stateSyncerTestLogger.clearAll();
    inventoryEntrySyncerTestLogger.clearAll();
    customObjectSyncerTestLogger.clearAll();
    typeSyncerTestLogger.clearAll();
    categorySyncerTestLogger.clearAll();
    cartDiscountSyncerTestLogger.clearAll();
    taxCategorySyncerTestLogger.clearAll();
    setupSourceProjectData(CTP_SOURCE_CLIENT);
  }

  static void setupSourceProjectData(@Nonnull final ProjectApiRoot sourceProjectClient) {
    final AttributeDefinitionDraft setOfProductsAttributeDef =
        AttributeDefinitionDraftBuilder.of()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            elementBuilder ->
                                elementBuilder
                                    .referenceBuilder()
                                    .referenceTypeId(AttributeReferenceTypeId.PRODUCT)))
            .name("products")
            .label(ofEnglish("products"))
            .isRequired(false)
            .isSearchable(false)
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .build();

    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of()
            .key(MAIN_PRODUCT_TYPE_KEY)
            .name(MAIN_PRODUCT_TYPE_KEY)
            .description("an inner productType for t-shirts")
            .attributes(setOfProductsAttributeDef)
            .build();

    final ProductType productType =
        sourceProjectClient.productTypes().post(productTypeDraft).executeBlocking().getBody();

    final ConcurrentHashMap.KeySetView<String, Boolean> productIds = ConcurrentHashMap.newKeySet();

    CompletableFuture.allOf(
            IntStream.range(0, 500)
                .mapToObj(value -> createProduct(sourceProjectClient, productType, value))
                .toArray(CompletableFuture[]::new))
        .join();

    final ArrayNode setAttributeValue = JsonNodeFactory.instance.arrayNode();
    final Set<ObjectNode> productReferences =
        productIds.stream()
            .map(
                productId ->
                    createReferenceOfType(ReferenceTypeId.PRODUCT.getJsonName(), productId))
            .collect(Collectors.toSet());
    setAttributeValue.addAll(productReferences);

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .key(MAIN_PRODUCT_MASTER_VARIANT_KEY)
            .sku(MAIN_PRODUCT_MASTER_VARIANT_KEY)
            .attributes(
                AttributeBuilder.of()
                    .name(setOfProductsAttributeDef.getName())
                    .value(setAttributeValue)
                    .build())
            .build();

    final ProductDraft draft =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish(MAIN_PRODUCT_KEY))
            .slug(ofEnglish(MAIN_PRODUCT_KEY))
            .masterVariant(masterVariant)
            .key(MAIN_PRODUCT_KEY)
            .build();

    sourceProjectClient.products().post(draft).executeBlocking();
  }

  private static CompletableFuture<Product> createProduct(
      @Nonnull final ProjectApiRoot sourceProjectClient,
      @Nonnull final ProductType productType,
      int index) {
    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .key(format("%d-mv-key", index))
            .sku(format("%d-mv-sku", index))
            .build();
    final ProductDraft draft =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish(Integer.toString(index)))
            .slug(ofEnglish(format("%d-slug", index)))
            .masterVariant(masterVariant)
            .key(format("%d-key", index))
            .build();
    return sourceProjectClient
        .products()
        .create(draft)
        .execute()
        .thenApply(ApiHttpResponse::getBody)
        .toCompletableFuture();
  }

  @AfterAll
  static void tearDownSuite() {
    cleanUpProjects(CTP_SOURCE_CLIENT, CTP_TARGET_CLIENT);
  }

  @Test
  void run_WithAProductWith500DistinctReferences_ShouldSyncCorrectly() {
    // test
    CliRunner.of()
        .run(new String[] {"-s", "all", "-r", "runnerName", "-f"}, createITSyncerFactory());

    // assertions
    assertThat(cliRunnerTestLogger.getAllLoggingEvents())
        .allMatch(loggingEvent -> !Level.ERROR.equals(loggingEvent.getLevel()));

    assertTypeSyncerLoggingEvents(typeSyncerTestLogger, 0);
    assertProductTypeSyncerLoggingEvents(productTypeSyncerTestLogger, 1);
    assertTaxCategorySyncerLoggingEvents(taxCategorySyncerTestLogger, 0);
    assertCategorySyncerLoggingEvents(categorySyncerTestLogger, 0);
    assertProductSyncerLoggingEvents(productSyncerTestLogger, 501);
    assertInventoryEntrySyncerLoggingEvents(inventoryEntrySyncerTestLogger, 0);
    assertCartDiscountSyncerLoggingEvents(cartDiscountSyncerTestLogger, 0);
    assertCustomerSyncerLoggingEvents(customerSyncerTestLogger, 0);
    assertShoppingListSyncerLoggingEvents(shoppingListSyncerTestLogger, 0);
    assertStateSyncerLoggingEvents(
        stateSyncerTestLogger, 1); // 1 state is built-in and it cant be deleted
    assertCustomObjectSyncerLoggingEvents(customObjectSyncerTestLogger, 0);

    assertAllResourcesAreSyncedToTarget(CTP_TARGET_CLIENT);
  }

  private static void assertAllResourcesAreSyncedToTarget(
      @Nonnull final ProjectApiRoot targetClient) {

    assertProductTypeExists(targetClient, MAIN_PRODUCT_TYPE_KEY);

    final List<Product> products =
        QueryUtils.queryAll(targetClient.products().get(), Function.identity())
            .thenApply(
                fetchedProducts ->
                    fetchedProducts.stream().flatMap(List::stream).collect(Collectors.toList()))
            .toCompletableFuture()
            .join();
    assertThat(products).hasSize(501);

    final CustomObjectPagedQueryResponse queryResult =
        targetClient
            .customObjects()
            .withContainer("commercetools-sync-java.UnresolvedReferencesService" + ".productDrafts")
            .get()
            .executeBlocking()
            .getBody();

    assertThat(queryResult.getResults()).isEmpty();
  }
}
