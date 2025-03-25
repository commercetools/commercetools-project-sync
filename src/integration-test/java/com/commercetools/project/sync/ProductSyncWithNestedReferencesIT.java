package com.commercetools.project.sync;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.project.sync.util.CtpClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.project.sync.util.CtpClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.project.sync.util.IntegrationTestUtils.assertCategoryExists;
import static com.commercetools.project.sync.util.IntegrationTestUtils.assertCustomerExists;
import static com.commercetools.project.sync.util.IntegrationTestUtils.assertProductExists;
import static com.commercetools.project.sync.util.IntegrationTestUtils.assertProductTypeExists;
import static com.commercetools.project.sync.util.IntegrationTestUtils.assertStateExists;
import static com.commercetools.project.sync.util.IntegrationTestUtils.cleanUpProjects;
import static com.commercetools.project.sync.util.IntegrationTestUtils.createAttributeObject;
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
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.category.CategoryDraftBuilder;
import com.commercetools.api.models.common.Reference;
import com.commercetools.api.models.common.ReferenceTypeId;
import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.api.models.custom_object.CustomObjectDraft;
import com.commercetools.api.models.custom_object.CustomObjectDraftBuilder;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerDraft;
import com.commercetools.api.models.customer.CustomerDraftBuilder;
import com.commercetools.api.models.customer.CustomerSignInResult;
import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.AttributeAccessor;
import com.commercetools.api.models.product.AttributeBuilder;
import com.commercetools.api.models.product.Product;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductDraftBuilder;
import com.commercetools.api.models.product.ProductVariant;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.product.ProductVariantDraftBuilder;
import com.commercetools.api.models.product_type.AttributeConstraintEnum;
import com.commercetools.api.models.product_type.AttributeDefinitionDraft;
import com.commercetools.api.models.product_type.AttributeDefinitionDraftBuilder;
import com.commercetools.api.models.product_type.AttributeReferenceTypeId;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypeDraftBuilder;
import com.commercetools.api.models.state.State;
import com.commercetools.api.models.state.StateDraft;
import com.commercetools.api.models.state.StateDraftBuilder;
import com.commercetools.api.models.state.StateTypeEnum;
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
import com.commercetools.project.sync.util.TestUtils;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.valfirst.slf4jtest.TestLogger;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

// This will suppress MoreThanOneLogger warnings in this class
@SuppressWarnings("PMD.MoreThanOneLogger")
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

  static void setupSourceProjectData(@Nonnull final ProjectApiRoot sourceProjectClient) {
    final List<AttributeReferenceTypeId> attributeList =
        List.of(
            AttributeReferenceTypeId.PRODUCT_TYPE,
            AttributeReferenceTypeId.CATEGORY,
            AttributeReferenceTypeId.KEY_VALUE_DOCUMENT,
            AttributeReferenceTypeId.STATE,
            AttributeReferenceTypeId.CUSTOMER);
    final Map<String, AttributeDefinitionDraft> attributeDefinitionDrafts =
        attributeList.stream()
            .map(
                typeId ->
                    AttributeDefinitionDraftBuilder.of()
                        .type(
                            attributeTypeBuilder ->
                                attributeTypeBuilder
                                    .setBuilder()
                                    .elementType(
                                        builder ->
                                            builder.referenceBuilder().referenceTypeId(typeId)))
                        .name(typeId.getJsonName())
                        .label(ofEnglish(typeId.getJsonName()))
                        .isRequired(false)
                        .isSearchable(false)
                        .attributeConstraint(AttributeConstraintEnum.NONE)
                        .build())
            .collect(
                Collectors.toMap(
                    attributeDefinitionDraft -> attributeDefinitionDraft.getName(),
                    attributeDefinitionDraft -> attributeDefinitionDraft));

    final ProductTypeDraft nestedProductTypeDraft =
        ProductTypeDraftBuilder.of()
            .key(INNER_PRODUCT_TYPE_KEY)
            .name(INNER_PRODUCT_TYPE_KEY)
            .description("an inner productType for t-shirts")
            .attributes(List.copyOf(attributeDefinitionDrafts.values()))
            .build();

    final ProductType innerProductType =
        sourceProjectClient.productTypes().post(nestedProductTypeDraft).executeBlocking().getBody();

    final AttributeDefinitionDraft nestedAttribute =
        AttributeDefinitionDraftBuilder.of()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            nestedBuilder ->
                                nestedBuilder
                                    .nestedBuilder()
                                    .typeReference(innerProductType.toReference())))
            .name(NESTED_ATTRIBUTE_NAME)
            .label(ofEnglish("nested attribute"))
            .isRequired(false)
            .isSearchable(false)
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .build();

    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of()
            .key(MAIN_PRODUCT_TYPE_KEY)
            .name(MAIN_PRODUCT_TYPE_KEY)
            .description("a productType for t-shirts")
            .attributes(nestedAttribute)
            .build();

    final ProductType mainProductType =
        sourceProjectClient.productTypes().post(productTypeDraft).executeBlocking().getBody();

    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of()
            .name(ofEnglish("t-shirts"))
            .slug(ofEnglish("t-shirts"))
            .key(CATEGORY_KEY)
            .build();

    final Category category =
        sourceProjectClient.categories().post(categoryDraft).executeBlocking().getBody();

    final StateDraft stateDraft =
        StateDraftBuilder.of()
            .key(STATE_KEY)
            .type(StateTypeEnum.PRODUCT_STATE)
            .roles(Collections.emptyList())
            .description(ofEnglish("states"))
            .name(ofEnglish("states"))
            .initial(true)
            .transitions(Collections.emptyList())
            .build();

    final State state = sourceProjectClient.states().post(stateDraft).executeBlocking().getBody();

    final CustomerDraft customerDraft =
        CustomerDraftBuilder.of()
            .email("test@email.com")
            .password("testPassword")
            .key(CUSTOMER_KEY)
            .build();

    final CustomerSignInResult customerSignInResult =
        sourceProjectClient.customers().post(customerDraft).executeBlocking().getBody();

    final Customer customer = customerSignInResult.getCustomer();

    final ObjectNode customObjectValue =
        JsonNodeFactory.instance.objectNode().put("field", "value1");

    final CustomObjectDraft customObjectDraft =
        CustomObjectDraftBuilder.of()
            .container("container")
            .key("key1")
            .value(customObjectValue)
            .build();

    final CustomObjectDraft customObjectDraft2 =
        CustomObjectDraftBuilder.of()
            .container("container")
            .key("key2")
            .value("{\"field\": \"value2\"}")
            .build();

    final CustomObject customObject1 =
        sourceProjectClient.customObjects().post(customObjectDraft).executeBlocking().getBody();

    final CustomObject customObject2 =
        sourceProjectClient.customObjects().post(customObjectDraft2).executeBlocking().getBody();

    final ArrayNode setAttributeValue = JsonNodeFactory.instance.arrayNode();

    final ArrayNode nestedAttributeValue = JsonNodeFactory.instance.arrayNode();

    final ArrayNode categoriesReferencesAttributeValue = JsonNodeFactory.instance.arrayNode();
    categoriesReferencesAttributeValue.add(
        createReferenceOfType(AttributeReferenceTypeId.CATEGORY.getJsonName(), category.getId()));

    final ArrayNode productTypesReferencesAttributeValue = JsonNodeFactory.instance.arrayNode();
    productTypesReferencesAttributeValue.add(
        createReferenceOfType(
            AttributeReferenceTypeId.PRODUCT_TYPE.getJsonName(), mainProductType.getId()));

    final ArrayNode customObjectReferencesAttributeValue = JsonNodeFactory.instance.arrayNode();
    customObjectReferencesAttributeValue.add(
        createReferenceOfType(
            AttributeReferenceTypeId.KEY_VALUE_DOCUMENT.getJsonName(), customObject1.getId()));
    customObjectReferencesAttributeValue.add(
        createReferenceOfType(
            AttributeReferenceTypeId.KEY_VALUE_DOCUMENT.getJsonName(), customObject2.getId()));

    final ArrayNode stateReferenceAttributeValue = JsonNodeFactory.instance.arrayNode();
    stateReferenceAttributeValue.add(
        createReferenceOfType(AttributeReferenceTypeId.STATE.getJsonName(), state.getId()));

    final ArrayNode customerReferenceAttributeValue = JsonNodeFactory.instance.arrayNode();
    customerReferenceAttributeValue.add(
        createReferenceOfType(AttributeReferenceTypeId.CUSTOMER.getJsonName(), customer.getId()));

    final AttributeDefinitionDraft categoriesAttributeDef =
        attributeDefinitionDrafts.get(AttributeReferenceTypeId.CATEGORY.getJsonName());
    nestedAttributeValue.add(
        createAttributeObject(
            categoriesAttributeDef.getName(), categoriesReferencesAttributeValue));
    final AttributeDefinitionDraft setOfProductTypeAttributeDef =
        attributeDefinitionDrafts.get(ReferenceTypeId.PRODUCT_TYPE.getJsonName());
    nestedAttributeValue.add(
        createAttributeObject(
            setOfProductTypeAttributeDef.getName(), productTypesReferencesAttributeValue));
    final AttributeDefinitionDraft setOfCustomObjectAttributeDef =
        attributeDefinitionDrafts.get(ReferenceTypeId.KEY_VALUE_DOCUMENT.getJsonName());
    nestedAttributeValue.add(
        createAttributeObject(
            setOfCustomObjectAttributeDef.getName(), customObjectReferencesAttributeValue));
    final AttributeDefinitionDraft setOfStateAttributeDef =
        attributeDefinitionDrafts.get(ReferenceTypeId.STATE.getJsonName());
    nestedAttributeValue.add(
        createAttributeObject(setOfStateAttributeDef.getName(), stateReferenceAttributeValue));
    final AttributeDefinitionDraft setOfCustomerAttributeDef =
        attributeDefinitionDrafts.get(ReferenceTypeId.CUSTOMER.getJsonName());
    nestedAttributeValue.add(
        createAttributeObject(
            setOfCustomerAttributeDef.getName(), customerReferenceAttributeValue));

    setAttributeValue.add(nestedAttributeValue);

    final Attribute setOfNestedAttribute =
        AttributeBuilder.of().name(nestedAttribute.getName()).value(setAttributeValue).build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .key(MAIN_PRODUCT_MASTER_VARIANT_KEY)
            .sku(MAIN_PRODUCT_MASTER_VARIANT_KEY)
            .attributes(setOfNestedAttribute)
            .build();

    final ProductDraft productDraftWithNestedAttribute =
        ProductDraftBuilder.of()
            .productType(mainProductType.toResourceIdentifier())
            .name(ofEnglish(MAIN_PRODUCT_KEY))
            .slug(ofEnglish(MAIN_PRODUCT_KEY))
            .masterVariant(masterVariant)
            .key(MAIN_PRODUCT_KEY)
            .build();

    sourceProjectClient.products().post(productDraftWithNestedAttribute).executeBlocking();
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

    TestUtils.assertTypeSyncerLoggingEvents(typeSyncerTestLogger, 0);
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
      @Nonnull final ProjectApiRoot targetClient) {

    assertProductTypeExists(targetClient, INNER_PRODUCT_TYPE_KEY);
    final ProductType productType = assertProductTypeExists(targetClient, MAIN_PRODUCT_TYPE_KEY);
    final Category category = assertCategoryExists(targetClient, CATEGORY_KEY);
    final State state = assertStateExists(targetClient, STATE_KEY);
    final Customer customer = assertCustomerExists(targetClient, CUSTOMER_KEY);

    final Product mainProduct =
        assertProductExists(
            targetClient,
            MAIN_PRODUCT_KEY,
            MAIN_PRODUCT_MASTER_VARIANT_KEY,
            MAIN_PRODUCT_MASTER_VARIANT_KEY);

    assertThat(mainProduct.getKey()).isEqualTo(MAIN_PRODUCT_KEY);
    final ProductVariant stagedMasterVariant =
        mainProduct.getMasterData().getStaged().getMasterVariant();
    assertThat(stagedMasterVariant.getKey()).isEqualTo(MAIN_PRODUCT_MASTER_VARIANT_KEY);
    assertThat(stagedMasterVariant.getAttributes()).hasSize(1);
    assertThat(stagedMasterVariant.getAttribute(NESTED_ATTRIBUTE_NAME))
        .satisfies(
            attribute -> {
              final List<List<Attribute>> attributeAsSetNested =
                  AttributeAccessor.asSetNested(attribute);
              assertThat(attributeAsSetNested).isExactlyInstanceOf(ArrayList.class);
              assertThat(attributeAsSetNested).hasSize(1);

              final List<Attribute> nestedAttributeElement = attributeAsSetNested.get(0);
              assertThat(nestedAttributeElement).isExactlyInstanceOf(ArrayList.class);
              assertThat(nestedAttributeElement).hasSize(5);

              assertThat(nestedAttributeElement.get(0).getName())
                  .isEqualTo(AttributeReferenceTypeId.CATEGORY.getJsonName());
              assertThat(nestedAttributeElement.get(0).getValue())
                  .isExactlyInstanceOf(ArrayList.class);
              final List<Reference> categoryReferences =
                  AttributeAccessor.asSetReference(nestedAttributeElement.get(0));
              assertThat(categoryReferences)
                  .singleElement()
                  .satisfies(
                      categoryReference ->
                          assertThat(categoryReference.getId()).isEqualTo(category.getId()));

              assertThat(nestedAttributeElement.get(1).getName())
                  .isEqualTo(AttributeReferenceTypeId.PRODUCT_TYPE.getJsonName());
              assertThat(nestedAttributeElement.get(1).getValue())
                  .isExactlyInstanceOf(ArrayList.class);
              final List<Reference> productTypeReferences =
                  AttributeAccessor.asSetReference(nestedAttributeElement.get(1));
              assertThat(productTypeReferences)
                  .singleElement()
                  .satisfies(
                      productTypeReference ->
                          assertThat(productTypeReference.getId()).isEqualTo(productType.getId()));

              assertThat(nestedAttributeElement.get(2).getName())
                  .isEqualTo(AttributeReferenceTypeId.KEY_VALUE_DOCUMENT.getJsonName());
              assertThat(nestedAttributeElement.get(2).getValue())
                  .isExactlyInstanceOf(ArrayList.class);
              final List<Reference> customObjectReferences =
                  AttributeAccessor.asSetReference(nestedAttributeElement.get(2));
              assertThat(customObjectReferences).hasSize(2);

              assertThat(nestedAttributeElement.get(3).getName())
                  .isEqualTo(AttributeReferenceTypeId.STATE.getJsonName());
              assertThat(nestedAttributeElement.get(3).getValue())
                  .isExactlyInstanceOf(ArrayList.class);
              final List<Reference> stateReferences =
                  AttributeAccessor.asSetReference(nestedAttributeElement.get(3));
              assertThat(stateReferences)
                  .singleElement()
                  .satisfies(
                      stateReference ->
                          assertThat(stateReference.getId()).isEqualTo(state.getId()));

              assertThat(nestedAttributeElement.get(4).getName())
                  .isEqualTo(AttributeReferenceTypeId.CUSTOMER.getJsonName());
              assertThat(nestedAttributeElement.get(4).getValue())
                  .isExactlyInstanceOf(ArrayList.class);
              final List<Reference> customerReferences =
                  AttributeAccessor.asSetReference(nestedAttributeElement.get(4));
              assertThat(customerReferences)
                  .singleElement()
                  .satisfies(
                      customerReference ->
                          assertThat(customerReference.getId()).isEqualTo(customer.getId()));
            });
  }
}
