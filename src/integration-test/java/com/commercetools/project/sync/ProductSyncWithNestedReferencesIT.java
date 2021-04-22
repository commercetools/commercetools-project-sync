package com.commercetools.project.sync;

import static com.commercetools.project.sync.util.IntegrationTestUtils.assertCategoryExists;
import static com.commercetools.project.sync.util.IntegrationTestUtils.assertCustomerExists;
import static com.commercetools.project.sync.util.IntegrationTestUtils.assertProductTypeExists;
import static com.commercetools.project.sync.util.IntegrationTestUtils.assertStateExists;
import static com.commercetools.project.sync.util.IntegrationTestUtils.cleanUpProjects;
import static com.commercetools.project.sync.util.IntegrationTestUtils.createAttributeObject;
import static com.commercetools.project.sync.util.IntegrationTestUtils.createITSyncerFactory;
import static com.commercetools.project.sync.util.IntegrationTestUtils.createReferenceObject;
import static com.commercetools.project.sync.util.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.project.sync.util.SphereClientUtils.CTP_TARGET_CLIENT;
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
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.CustomerDraftBuilder;
import io.sphere.sdk.customers.CustomerSignInResult;
import io.sphere.sdk.customers.commands.CustomerCreateCommand;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;
import io.sphere.sdk.customobjects.commands.CustomObjectUpsertCommand;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.attributes.AttributeConstraint;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraftBuilder;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.products.attributes.NestedAttributeType;
import io.sphere.sdk.products.attributes.ReferenceAttributeType;
import io.sphere.sdk.products.attributes.SetAttributeType;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.StateDraftBuilder;
import io.sphere.sdk.states.StateType;
import io.sphere.sdk.states.commands.StateCreateCommand;
import java.util.Collections;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

class ProductSyncWithNestedReferencesIT {

  private static final TestLogger cliRunnerTestLogger =
      TestLoggerFactory.getTestLogger(CliRunner.class);
  private static final TestLogger productSyncerTestLogger =
      TestLoggerFactory.getTestLogger(ProductSyncer.class);
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

  private static final String INNER_PRODUCT_TYPE_KEY = "inner-product-type";
  private static final String MAIN_PRODUCT_TYPE_KEY = "sample-product-type";
  private static final String CATEGORY_KEY = "category-key";
  private static final String MAIN_PRODUCT_MASTER_VARIANT_KEY = "main-product-master-variant-key";
  private static final String MAIN_PRODUCT_KEY = "product-with-nested";
  private static final String NESTED_ATTRIBUTE_NAME = "nested-attribute";
  private static final String STATE_KEY = "state-key";
  private static final String CUSTOMER_KEY = "customer-key";

  @BeforeEach
  void setup() {
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
    cleanUpProjects(CTP_SOURCE_CLIENT, CTP_TARGET_CLIENT);
    setupSourceProjectData(CTP_SOURCE_CLIENT);
  }

  static void setupSourceProjectData(@Nonnull final SphereClient sourceProjectClient) {
    final AttributeDefinitionDraft setOfCategoriesAttributeDef =
        AttributeDefinitionDraftBuilder.of(
                SetAttributeType.of(ReferenceAttributeType.ofCategory()),
                "categories",
                ofEnglish("categories"),
                false)
            .searchable(false)
            .attributeConstraint(AttributeConstraint.NONE)
            .build();

    final AttributeDefinitionDraft setOfProductTypeAttributeDef =
        AttributeDefinitionDraftBuilder.of(
                SetAttributeType.of(ReferenceAttributeType.ofProductType()),
                "productTypes",
                ofEnglish("productTypes"),
                false)
            .searchable(false)
            .attributeConstraint(AttributeConstraint.NONE)
            .build();

    final AttributeDefinitionDraft setOfCustomObjectAttributeDef =
        AttributeDefinitionDraftBuilder.of(
                SetAttributeType.of(ReferenceAttributeType.of(CustomObject.referenceTypeId())),
                "customObjects",
                ofEnglish("customObjects"),
                false)
            .searchable(false)
            .attributeConstraint(AttributeConstraint.NONE)
            .build();

    final AttributeDefinitionDraft setOfStateAttributeDef =
        AttributeDefinitionDraftBuilder.of(
                SetAttributeType.of(ReferenceAttributeType.of(State.referenceTypeId())),
                "states",
                ofEnglish("states"),
                false)
            .searchable(false)
            .attributeConstraint(AttributeConstraint.NONE)
            .build();

    final AttributeDefinitionDraft setOfCustomerAttributeDef =
        AttributeDefinitionDraftBuilder.of(
                SetAttributeType.of(ReferenceAttributeType.of(Customer.referenceTypeId())),
                "customers",
                ofEnglish("customers"),
                false)
            .searchable(false)
            .attributeConstraint(AttributeConstraint.NONE)
            .build();

    final ProductTypeDraft nestedProductTypeDraft =
        ProductTypeDraftBuilder.of(
                INNER_PRODUCT_TYPE_KEY,
                INNER_PRODUCT_TYPE_KEY,
                "an inner productType for t-shirts",
                emptyList())
            .attributes(
                asList(
                    setOfCategoriesAttributeDef,
                    setOfProductTypeAttributeDef,
                    setOfCustomObjectAttributeDef,
                    setOfStateAttributeDef,
                    setOfCustomerAttributeDef))
            .build();

    final ProductType innerProductType =
        sourceProjectClient
            .execute(ProductTypeCreateCommand.of(nestedProductTypeDraft))
            .toCompletableFuture()
            .join();

    final AttributeDefinitionDraft nestedAttribute =
        AttributeDefinitionDraftBuilder.of(
                SetAttributeType.of(NestedAttributeType.of(innerProductType)),
                NESTED_ATTRIBUTE_NAME,
                ofEnglish("nested attribute"),
                false)
            .searchable(false)
            .attributeConstraint(AttributeConstraint.NONE)
            .build();

    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of(
                MAIN_PRODUCT_TYPE_KEY,
                MAIN_PRODUCT_TYPE_KEY,
                "a productType for t-shirts",
                emptyList())
            .attributes(singletonList(nestedAttribute))
            .build();

    final ProductType mainProductType =
        sourceProjectClient
            .execute(ProductTypeCreateCommand.of(productTypeDraft))
            .toCompletableFuture()
            .join();

    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of(ofEnglish("t-shirts"), ofEnglish("t-shirts"))
            .key(CATEGORY_KEY)
            .build();

    final Category category =
        sourceProjectClient
            .execute(CategoryCreateCommand.of(categoryDraft))
            .toCompletableFuture()
            .join();

    final StateDraft stateDraft =
        StateDraftBuilder.of(STATE_KEY, StateType.PRODUCT_STATE)
            .roles(Collections.emptySet())
            .description(ofEnglish("states"))
            .name(ofEnglish("states"))
            .initial(true)
            .transitions(Collections.emptySet())
            .build();

    final State state =
        sourceProjectClient.execute(StateCreateCommand.of(stateDraft)).toCompletableFuture().join();

    final CustomerDraft customerDraft =
        CustomerDraftBuilder.of("test@email.com", "testPassword").key(CUSTOMER_KEY).build();

    final CustomerSignInResult customerSignInResult =
        sourceProjectClient
            .execute(CustomerCreateCommand.of(customerDraft))
            .toCompletableFuture()
            .join();
    final Customer customer = customerSignInResult.getCustomer();

    final ObjectNode customObjectValue =
        JsonNodeFactory.instance.objectNode().put("field", "value1");

    final CustomObjectDraft<JsonNode> customObjectDraft =
        CustomObjectDraft.ofUnversionedUpsert("container", "key1", customObjectValue);

    final ObjectNode customObjectValue2 =
        JsonNodeFactory.instance.objectNode().put("field", "value2");

    final CustomObjectDraft<JsonNode> customObjectDraft2 =
        CustomObjectDraft.ofUnversionedUpsert("container", "key2", customObjectValue2);

    final CustomObject<JsonNode> customObject1 =
        sourceProjectClient
            .execute(CustomObjectUpsertCommand.of(customObjectDraft))
            .toCompletableFuture()
            .join();

    final CustomObject<JsonNode> customObject2 =
        sourceProjectClient
            .execute(CustomObjectUpsertCommand.of(customObjectDraft2))
            .toCompletableFuture()
            .join();

    final ArrayNode setAttributeValue = JsonNodeFactory.instance.arrayNode();

    final ArrayNode nestedAttributeValue = JsonNodeFactory.instance.arrayNode();

    final ArrayNode categoriesReferencesAttributeValue = JsonNodeFactory.instance.arrayNode();
    categoriesReferencesAttributeValue.add(
        createReferenceObject(Category.referenceTypeId(), category.getId()));

    final ArrayNode productTypesReferencesAttributeValue = JsonNodeFactory.instance.arrayNode();
    productTypesReferencesAttributeValue.add(
        createReferenceObject(ProductType.referenceTypeId(), mainProductType.getId()));

    final ArrayNode customObjectReferencesAttributeValue = JsonNodeFactory.instance.arrayNode();
    customObjectReferencesAttributeValue.add(
        createReferenceObject(CustomObject.referenceTypeId(), customObject1.getId()));
    customObjectReferencesAttributeValue.add(
        createReferenceObject(CustomObject.referenceTypeId(), customObject2.getId()));

    final ArrayNode stateReferenceAttributeValue = JsonNodeFactory.instance.arrayNode();
    stateReferenceAttributeValue.add(createReferenceObject(State.referenceTypeId(), state.getId()));

    final ArrayNode customerReferenceAttributeValue = JsonNodeFactory.instance.arrayNode();
    customerReferenceAttributeValue.add(
        createReferenceObject(Customer.referenceTypeId(), customer.getId()));

    nestedAttributeValue.add(
        createAttributeObject(
            setOfCategoriesAttributeDef.getName(), categoriesReferencesAttributeValue));
    nestedAttributeValue.add(
        createAttributeObject(
            setOfProductTypeAttributeDef.getName(), productTypesReferencesAttributeValue));
    nestedAttributeValue.add(
        createAttributeObject(
            setOfCustomObjectAttributeDef.getName(), customObjectReferencesAttributeValue));
    nestedAttributeValue.add(
        createAttributeObject(setOfStateAttributeDef.getName(), stateReferenceAttributeValue));
    nestedAttributeValue.add(
        createAttributeObject(
            setOfCustomerAttributeDef.getName(), customerReferenceAttributeValue));

    setAttributeValue.add(nestedAttributeValue);

    final AttributeDraft setOfNestedAttribute =
        AttributeDraft.of(nestedAttribute.getName(), setAttributeValue);

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .key(MAIN_PRODUCT_MASTER_VARIANT_KEY)
            .sku(MAIN_PRODUCT_MASTER_VARIANT_KEY)
            .attributes(setOfNestedAttribute)
            .build();

    final ProductDraft productDraftWithNestedAttribute =
        ProductDraftBuilder.of(
                mainProductType,
                ofEnglish(MAIN_PRODUCT_KEY),
                ofEnglish(MAIN_PRODUCT_KEY),
                masterVariant)
            .key(MAIN_PRODUCT_KEY)
            .build();

    sourceProjectClient
        .execute(ProductCreateCommand.of(productDraftWithNestedAttribute))
        .toCompletableFuture()
        .join();
  }

  @AfterAll
  static void tearDownSuite() {
    cleanUpProjects(CTP_SOURCE_CLIENT, CTP_TARGET_CLIENT);
  }

  @Test
  void run_WithSyncAsArgumentWithAllArgAsFullSync_ShouldExecuteAllSyncers() {
    // test
    CliRunner.of()
        .run(new String[] {"-s", "all", "-r", "runnerName", "-f"}, createITSyncerFactory());

    assertThat(cliRunnerTestLogger.getAllLoggingEvents())
        .allMatch(loggingEvent -> !Level.ERROR.equals(loggingEvent.getLevel()));

    assertTypeSyncerLoggingEvents(typeSyncerTestLogger, 0);
    assertProductTypeSyncerLoggingEvents(productTypeSyncerTestLogger, 2);
    assertTaxCategorySyncerLoggingEvents(taxCategorySyncerTestLogger, 0);
    assertCategorySyncerLoggingEvents(categorySyncerTestLogger, 1);
    assertProductSyncerLoggingEvents(productSyncerTestLogger, 1);
    assertInventoryEntrySyncerLoggingEvents(inventoryEntrySyncerTestLogger, 0);
    assertCartDiscountSyncerLoggingEvents(cartDiscountSyncerTestLogger, 0);
    assertStateSyncerLoggingEvents(stateSyncerTestLogger, 2);
    assertCustomObjectSyncerLoggingEvents(customObjectSyncerTestLogger, 2);
    assertCustomerSyncerLoggingEvents(customerSyncerTestLogger, 1);
    assertShoppingListSyncerLoggingEvents(shoppingListSyncerTestLogger, 0);

    assertAllResourcesAreSyncedToTarget(CTP_TARGET_CLIENT);
  }

  private static void assertAllResourcesAreSyncedToTarget(
      @Nonnull final SphereClient targetClient) {

    assertProductTypeExists(targetClient, INNER_PRODUCT_TYPE_KEY);
    final ProductType productType = assertProductTypeExists(targetClient, MAIN_PRODUCT_TYPE_KEY);
    final Category category = assertCategoryExists(targetClient, CATEGORY_KEY);
    final State state = assertStateExists(targetClient, STATE_KEY);
    final Customer customer = assertCustomerExists(targetClient, CUSTOMER_KEY);

    final PagedQueryResult<Product> productQueryResult =
        targetClient.execute(ProductQuery.of()).toCompletableFuture().join();

    assertThat(productQueryResult.getResults())
        .hasSize(1)
        .singleElement()
        .satisfies(
            product -> {
              assertThat(product.getKey()).isEqualTo(MAIN_PRODUCT_KEY);
              final ProductVariant stagedMasterVariant =
                  product.getMasterData().getStaged().getMasterVariant();
              assertThat(stagedMasterVariant.getKey()).isEqualTo(MAIN_PRODUCT_MASTER_VARIANT_KEY);
              assertThat(stagedMasterVariant.getAttributes()).hasSize(1);
              assertThat(stagedMasterVariant.getAttribute(NESTED_ATTRIBUTE_NAME))
                  .satisfies(
                      attribute -> {
                        final JsonNode attributeValue = attribute.getValueAsJsonNode();
                        assertThat(attributeValue).isExactlyInstanceOf(ArrayNode.class);
                        final ArrayNode attributeValueAsArray = (ArrayNode) attributeValue;
                        assertThat(attributeValueAsArray).hasSize(1);

                        final JsonNode nestedAttributeElement = attributeValueAsArray.get(0);
                        assertThat(nestedAttributeElement).isExactlyInstanceOf(ArrayNode.class);
                        final ArrayNode nestedTypeAttributes = (ArrayNode) nestedAttributeElement;
                        assertThat(nestedTypeAttributes).hasSize(5);

                        assertThat(nestedTypeAttributes.get(0).get("name").asText())
                            .isEqualTo("categories");
                        assertThat(nestedTypeAttributes.get(0).get("value"))
                            .isExactlyInstanceOf(ArrayNode.class);
                        final ArrayNode categoryReferences =
                            (ArrayNode) (nestedTypeAttributes.get(0).get("value"));
                        assertThat(categoryReferences)
                            .singleElement()
                            .satisfies(
                                categoryReference ->
                                    assertThat(categoryReference.get("id").asText())
                                        .isEqualTo(category.getId()));

                        assertThat(nestedTypeAttributes.get(1).get("name").asText())
                            .isEqualTo("productTypes");
                        assertThat(nestedTypeAttributes.get(1).get("value"))
                            .isExactlyInstanceOf(ArrayNode.class);
                        final ArrayNode productTypeReferences =
                            (ArrayNode) (nestedTypeAttributes.get(1).get("value"));
                        assertThat(productTypeReferences)
                            .singleElement()
                            .satisfies(
                                productTypeReference ->
                                    assertThat(productTypeReference.get("id").asText())
                                        .isEqualTo(productType.getId()));

                        assertThat(nestedTypeAttributes.get(2).get("name").asText())
                            .isEqualTo("customObjects");
                        assertThat(nestedTypeAttributes.get(2).get("value"))
                            .isExactlyInstanceOf(ArrayNode.class);
                        final ArrayNode customObjectReferences =
                            (ArrayNode) (nestedTypeAttributes.get(2).get("value"));
                        assertThat(customObjectReferences).hasSize(2);

                        assertThat(nestedTypeAttributes.get(3).get("name").asText())
                            .isEqualTo("states");
                        assertThat(nestedTypeAttributes.get(3).get("value"))
                            .isExactlyInstanceOf(ArrayNode.class);
                        final ArrayNode stateReferences =
                            (ArrayNode) (nestedTypeAttributes.get(3).get("value"));
                        assertThat(stateReferences)
                            .singleElement()
                            .satisfies(
                                stateReference ->
                                    assertThat(stateReference.get("id").asText())
                                        .isEqualTo(state.getId()));

                        assertThat(nestedTypeAttributes.get(4).get("name").asText())
                            .isEqualTo("customers");
                        assertThat(nestedTypeAttributes.get(4).get("value"))
                            .isExactlyInstanceOf(ArrayNode.class);
                        final ArrayNode customerReferences =
                            (ArrayNode) (nestedTypeAttributes.get(4).get("value"));
                        assertThat(customerReferences)
                            .singleElement()
                            .satisfies(
                                customerReference ->
                                    assertThat(customerReference.get("id").asText())
                                        .isEqualTo(customer.getId()));
                      });
            });
  }
}
