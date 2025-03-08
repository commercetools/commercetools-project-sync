package com.commercetools.project.sync;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.project.sync.service.impl.CustomObjectServiceImpl.TIMESTAMP_GENERATOR_KEY;
import static com.commercetools.project.sync.util.CtpClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.project.sync.util.CtpClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.project.sync.util.IntegrationTestUtils.assertCategoryExists;
import static com.commercetools.project.sync.util.IntegrationTestUtils.assertProductExists;
import static com.commercetools.project.sync.util.IntegrationTestUtils.assertProductTypeExists;
import static com.commercetools.project.sync.util.IntegrationTestUtils.cleanUpProjects;
import static com.commercetools.project.sync.util.IntegrationTestUtils.createITSyncerFactory;
import static com.commercetools.project.sync.util.SyncUtils.APPLICATION_DEFAULT_NAME;
import static com.commercetools.project.sync.util.SyncUtils.DEFAULT_RUNNER_NAME;
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
import com.commercetools.api.models.cart_discount.CartDiscount;
import com.commercetools.api.models.cart_discount.CartDiscountDraft;
import com.commercetools.api.models.cart_discount.CartDiscountDraftBuilder;
import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.category.CategoryDraftBuilder;
import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.api.models.custom_object.CustomObjectDraft;
import com.commercetools.api.models.custom_object.CustomObjectDraftBuilder;
import com.commercetools.api.models.custom_object.CustomObjectPagedQueryResponse;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerDraft;
import com.commercetools.api.models.customer.CustomerDraftBuilder;
import com.commercetools.api.models.customer.CustomerSetFirstNameActionBuilder;
import com.commercetools.api.models.customer.CustomerSetLastNameActionBuilder;
import com.commercetools.api.models.customer.CustomerSignInResult;
import com.commercetools.api.models.inventory.InventoryEntry;
import com.commercetools.api.models.inventory.InventoryEntryDraft;
import com.commercetools.api.models.inventory.InventoryEntryDraftBuilder;
import com.commercetools.api.models.inventory.InventoryPagedQueryResponse;
import com.commercetools.api.models.product.Product;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductDraftBuilder;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypeDraftBuilder;
import com.commercetools.api.models.shopping_list.ShoppingList;
import com.commercetools.api.models.shopping_list.ShoppingListDraft;
import com.commercetools.api.models.shopping_list.ShoppingListDraftBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListLineItemDraft;
import com.commercetools.api.models.shopping_list.ShoppingListLineItemDraftBuilder;
import com.commercetools.api.models.state.State;
import com.commercetools.api.models.state.StateDraft;
import com.commercetools.api.models.state.StateDraftBuilder;
import com.commercetools.api.models.state.StateTypeEnum;
import com.commercetools.api.models.tax_category.TaxCategory;
import com.commercetools.api.models.tax_category.TaxCategoryDraft;
import com.commercetools.api.models.tax_category.TaxCategoryDraftBuilder;
import com.commercetools.api.models.tax_category.TaxRateDraft;
import com.commercetools.api.models.tax_category.TaxRateDraftBuilder;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.ResourceTypeId;
import com.commercetools.api.models.type.Type;
import com.commercetools.api.models.type.TypeDraft;
import com.commercetools.api.models.type.TypeDraftBuilder;
import com.commercetools.project.sync.cartdiscount.CartDiscountSyncer;
import com.commercetools.project.sync.category.CategorySyncer;
import com.commercetools.project.sync.customer.CustomerSyncer;
import com.commercetools.project.sync.customobject.CustomObjectSyncer;
import com.commercetools.project.sync.inventoryentry.InventoryEntrySyncer;
import com.commercetools.project.sync.model.response.LastSyncCustomObject;
import com.commercetools.project.sync.product.ProductSyncer;
import com.commercetools.project.sync.producttype.ProductTypeSyncer;
import com.commercetools.project.sync.shoppinglist.ShoppingListSyncer;
import com.commercetools.project.sync.state.StateSyncer;
import com.commercetools.project.sync.taxcategory.TaxCategorySyncer;
import com.commercetools.project.sync.type.TypeSyncer;
import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import com.commercetools.sync.producttypes.helpers.ProductTypeSyncStatistics;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.valfirst.slf4jtest.TestLogger;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.utils.json.JsonUtils;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.org.lidalia.slf4jext.Level;

// This will suppress MoreThanOneLogger warnings in this class
@SuppressWarnings("PMD.MoreThanOneLogger")
class CliRunnerIT {

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

  private static final String RESOURCE_KEY = "foo";
  private static final String PROJECT_SYNC_CONTAINER_NAME =
      "commercetools-project-sync.runnerName.ProductSync.timestampGenerator";

  @BeforeEach
  void setup() throws ExecutionException, InterruptedException {
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

  static void setupSourceProjectData(@Nonnull final ProjectApiRoot sourceProjectClient)
      throws ExecutionException, InterruptedException {
    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of()
            .key(RESOURCE_KEY)
            .name("sample-product-type")
            .description("a productType for t-shirts")
            .build();

    final ProductType productType =
        sourceProjectClient.productTypes().post(productTypeDraft).executeBlocking().getBody();

    final TypeDraft typeDraft =
        TypeDraftBuilder.of()
            .key(RESOURCE_KEY)
            .name(ofEnglish("category-custom-type"))
            .resourceTypeIds(ResourceTypeId.CATEGORY, ResourceTypeId.SHOPPING_LIST)
            .build();

    final StateDraft stateDraft =
        StateDraftBuilder.of()
            .key(RESOURCE_KEY)
            .type(StateTypeEnum.PRODUCT_STATE)
            .roles(List.of())
            .description(ofEnglish("State 1"))
            .name(ofEnglish("State 1"))
            .initial(true)
            .transitions(List.of())
            .build();
    final State state = sourceProjectClient.states().post(stateDraft).executeBlocking().getBody();

    final CompletableFuture<Type> typeFuture =
        sourceProjectClient
            .types()
            .post(typeDraft)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture();

    final TaxRateDraft taxRateDraft =
        TaxRateDraftBuilder.of()
            .name("Tax-Rate-Name-1")
            .amount(0.3)
            .includedInPrice(false)
            .country("DE")
            .build();

    final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of()
            .name("Tax-Category-Name-1")
            .rates(taxRateDraft)
            .description("Tax-Category-Description-1")
            .key(RESOURCE_KEY)
            .build();

    final TaxCategory taxCategory =
        sourceProjectClient.taxCategories().post(taxCategoryDraft).executeBlocking().getBody();

    final ObjectNode customObjectValue = JsonNodeFactory.instance.objectNode().put("name", "value");
    final CustomObjectDraft customObjectDraft =
        CustomObjectDraftBuilder.of()
            .key(RESOURCE_KEY)
            .container(RESOURCE_KEY)
            .value(customObjectValue)
            .build();
    // following custom object should not be synced as it's created by the project-sync itself
    final CustomObjectDraft customObjectToIgnore =
        CustomObjectDraftBuilder.of()
            .container(PROJECT_SYNC_CONTAINER_NAME)
            .key("timestampGenerator")
            .value(customObjectValue)
            .build();

    final CompletableFuture<CustomObject> customObjectFuture1 =
        sourceProjectClient
            .customObjects()
            .post(customObjectDraft)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture();

    final CompletableFuture<CustomObject> customObjectFuture2 =
        sourceProjectClient
            .customObjects()
            .post(customObjectToIgnore)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture();

    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of()
            .name(ofEnglish("t-shirts"))
            .slug(ofEnglish("t-shirts"))
            .key(RESOURCE_KEY)
            .build();

    final CompletableFuture<Category> categoryFuture =
        sourceProjectClient
            .categories()
            .post(categoryDraft)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture();

    final CustomerDraft customerDraft =
        CustomerDraftBuilder.of()
            .email("test@email.com")
            .password("testPassword")
            .key(RESOURCE_KEY)
            .build();

    final CompletableFuture<CustomerSignInResult> customerFuture =
        sourceProjectClient
            .customers()
            .post(customerDraft)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture();

    CompletableFuture.allOf(
            typeFuture, customObjectFuture1, customObjectFuture2, categoryFuture, customerFuture)
        .join();

    final ProductDraft productDraft =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("V-neck Tee"))
            .slug(ofEnglish("v-neck-tee"))
            .masterVariant(
                productVariantDraftBuilder ->
                    productVariantDraftBuilder.key(RESOURCE_KEY).sku(RESOURCE_KEY))
            .state(state.toResourceIdentifier())
            .taxCategory(taxCategory.toResourceIdentifier())
            .key(RESOURCE_KEY)
            .publish(true)
            .build();

    final CompletableFuture<Product> productFuture =
        sourceProjectClient
            .products()
            .post(productDraft)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture();

    final InventoryEntryDraft inventoryEntryDraft =
        InventoryEntryDraftBuilder.of().sku(RESOURCE_KEY).quantityOnStock(1L).build();

    final CompletableFuture<InventoryEntry> inventoryFuture =
        sourceProjectClient
            .inventory()
            .post(inventoryEntryDraft)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture();

    final CartDiscountDraft cartDiscountDraft =
        CartDiscountDraftBuilder.of()
            .name(ofEnglish("my-cart-discount"))
            .cartPredicate("1 = 1")
            .value(
                cartDiscountValueDraftBuilder ->
                    cartDiscountValueDraftBuilder.relativeBuilder().permyriad(1L))
            .target(cartDiscountTargetBuilder -> cartDiscountTargetBuilder.shippingBuilder())
            .sortOrder("0.1")
            .isActive(true)
            .key(RESOURCE_KEY)
            .build();

    final CompletableFuture<CartDiscount> cartDiscountFuture =
        sourceProjectClient
            .cartDiscounts()
            .post(cartDiscountDraft)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture();
    CompletableFuture.allOf(productFuture, inventoryFuture, cartDiscountFuture);

    final CustomerSignInResult customerSignInResult = customerFuture.get();
    final Product product = productFuture.get();
    final ShoppingListLineItemDraft lineItemDraft =
        ShoppingListLineItemDraftBuilder.of().productId(product.getId()).build();
    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraftBuilder.of().type(typeFuture.get().toResourceIdentifier()).build();

    final ShoppingListDraft shoppingListDraft =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("shoppingList-name"))
            .key(RESOURCE_KEY)
            .customer(customerSignInResult.getCustomer().toResourceIdentifier())
            .lineItems(lineItemDraft)
            .custom(customFieldsDraft)
            .build();

    sourceProjectClient.shoppingLists().post(shoppingListDraft).executeBlocking();
  }

  @AfterAll
  static void tearDownSuite() {
    cleanUpProjects(CTP_SOURCE_CLIENT, CTP_TARGET_CLIENT);
  }

  @Test
  void run_WithSyncAsArgumentWithAllArg_ShouldExecuteAllSyncers() {
    // test
    CliRunner.of().run(new String[] {"-s", "all"}, createITSyncerFactory());
    // assertions
    assertAllSyncersLoggingEvents(1);
    assertAllResourcesAreSyncedToTarget(CTP_TARGET_CLIENT);
  }

  @Test
  void run_WithSyncAsArgumentWhenAllArgumentsPassed_ShouldExecuteAllSyncers() {
    // test
    CliRunner.of()
        .run(
            new String[] {
              "-s",
              "types",
              "productTypes",
              "customers",
              "customObjects",
              "taxCategories",
              "categories",
              "inventoryEntries",
              "cartDiscounts",
              "states",
              "products",
              "shoppingLists"
            },
            createITSyncerFactory());

    // assertions
    assertAllSyncersLoggingEvents(1);

    assertAllResourcesAreSyncedToTarget(CTP_TARGET_CLIENT);
  }

  @Test
  void run_WithSyncAsArgumentWithCustomersAndShoppingLists_ShouldExecuteGivenSyncers() {
    prepareDataForShoppingListSync(CTP_SOURCE_CLIENT);

    // test
    CliRunner.of().run(new String[] {"-s", "customers", "shoppingLists"}, createITSyncerFactory());

    // assertions
    assertCustomerSyncerLoggingEvents(customerSyncerTestLogger, 1);
    assertShoppingListSyncerLoggingEvents(shoppingListSyncerTestLogger, 1);

    assertCustomersAreSyncedCorrectly(CTP_TARGET_CLIENT);
    assertShoppingListsAreSyncedCorrectly(CTP_TARGET_CLIENT);

    final String sourceProjectKey = CTP_SOURCE_CLIENT.getProjectKey();

    assertLastSyncCustomObjectExists(
        CTP_TARGET_CLIENT, sourceProjectKey, "customerSync", "runnerName");
    assertLastSyncCustomObjectExists(
        CTP_TARGET_CLIENT, sourceProjectKey, "shoppingListSync", "runnerName");
  }

  @Test
  void
      run_WithSyncAsArgumentWithCategoriesTaxCategoriesAndCartDiscounts_ShouldExecuteGivenSyncers() {
    // test
    CliRunner.of()
        .run(
            new String[] {"-s", "taxCategories", "categories", "cartDiscounts"},
            createITSyncerFactory());
    // assertions
    assertCategorySyncerLoggingEvents(categorySyncerTestLogger, 1);
    assertTaxCategorySyncerLoggingEvents(taxCategorySyncerTestLogger, 1);
    assertCartDiscountSyncerLoggingEvents(cartDiscountSyncerTestLogger, 1);

    assertCategoriesAreSyncedCorrectly(CTP_TARGET_CLIENT);
    assertTaxCategoriesAreSyncedCorrectly(CTP_TARGET_CLIENT);
    assertCartDiscountsAreSyncedCorrectly(CTP_TARGET_CLIENT);

    final String sourceProjectKey = CTP_SOURCE_CLIENT.getProjectKey();

    assertLastSyncCustomObjectExists(
        CTP_TARGET_CLIENT, sourceProjectKey, "categorySync", "runnerName");
    assertLastSyncCustomObjectExists(
        CTP_TARGET_CLIENT, sourceProjectKey, "taxCategorySync", "runnerName");
    assertLastSyncCustomObjectExists(
        CTP_TARGET_CLIENT, sourceProjectKey, "cartDiscountSync", "runnerName");
  }

  @Test
  void
      run_WithSyncAsArgumentWithProductTypesCategoriesAndShoppingLists_ShouldExecuteGivenSyncers() {
    // preparation
    prepareDataForShoppingListSync(CTP_SOURCE_CLIENT);

    // test
    CliRunner.of()
        .run(
            new String[] {"-s", "productTypes", "categories", "shoppingLists"},
            createITSyncerFactory());
    // assertions
    assertProductTypeSyncerLoggingEvents(productTypeSyncerTestLogger, 1);
    assertCategorySyncerLoggingEvents(categorySyncerTestLogger, 1);
    assertShoppingListSyncerLoggingEvents(shoppingListSyncerTestLogger, 1);

    assertProductTypesAreSyncedCorrectly(CTP_TARGET_CLIENT);
    assertCategoriesAreSyncedCorrectly(CTP_TARGET_CLIENT);
    assertShoppingListsAreSyncedCorrectly(CTP_TARGET_CLIENT);

    final String sourceProjectKey = CTP_SOURCE_CLIENT.getProjectKey();

    assertLastSyncCustomObjectExists(
        CTP_TARGET_CLIENT, sourceProjectKey, "categorySync", "runnerName");
    assertLastSyncCustomObjectExists(
        CTP_TARGET_CLIENT, sourceProjectKey, "productTypeSync", "runnerName");
    assertLastSyncCustomObjectExists(
        CTP_TARGET_CLIENT, sourceProjectKey, "shoppingListSync", "runnerName");
  }

  @Test
  void
      run_WithSyncAsArgumentWithProductsAndShoppingListsAlongWithTheirReferencedResources_ShouldExecuteGivenSyncers() {
    // test
    CliRunner.of()
        .run(
            new String[] {
              "-s",
              "types",
              "productTypes",
              "states",
              "taxCategories",
              "products",
              "customers",
              "shoppingLists"
            },
            createITSyncerFactory());
    // assertions
    assertProductSyncerLoggingEvents(productSyncerTestLogger, 1);

    assertTypesAreSyncedCorrectly(CTP_TARGET_CLIENT);
    assertProductsAreSyncedCorrectly(CTP_TARGET_CLIENT);
    assertProductTypesAreSyncedCorrectly(CTP_TARGET_CLIENT);
    assertStatesAreSyncedCorrectly(CTP_TARGET_CLIENT);
    assertTaxCategoriesAreSyncedCorrectly(CTP_TARGET_CLIENT);
    assertCustomersAreSyncedCorrectly(CTP_TARGET_CLIENT);
    assertShoppingListsAreSyncedCorrectly(CTP_TARGET_CLIENT);

    final String sourceProjectKey = CTP_SOURCE_CLIENT.getProjectKey();

    assertLastSyncCustomObjectExists(CTP_TARGET_CLIENT, sourceProjectKey, "typeSync", "runnerName");
    assertLastSyncCustomObjectExists(
        CTP_TARGET_CLIENT, sourceProjectKey, "productSync", "runnerName");
    assertLastSyncCustomObjectExists(
        CTP_TARGET_CLIENT, sourceProjectKey, "productTypeSync", "runnerName");
    assertLastSyncCustomObjectExists(
        CTP_TARGET_CLIENT, sourceProjectKey, "stateSync", "runnerName");
    assertLastSyncCustomObjectExists(
        CTP_TARGET_CLIENT, sourceProjectKey, "taxCategorySync", "runnerName");
    assertLastSyncCustomObjectExists(
        CTP_TARGET_CLIENT, sourceProjectKey, "customerSync", "runnerName");
    assertLastSyncCustomObjectExists(
        CTP_TARGET_CLIENT, sourceProjectKey, "shoppingListSync", "runnerName");
  }

  @Test
  void
      run_WithCustomerSyncAsArgument_ShouldSyncCustomersAndStoreLastSyncTimestampsAsCustomObject() {
    // test
    CliRunner.of().run(new String[] {"-s", "customers"}, createITSyncerFactory());
    // assertions
    assertThat(customerSyncerTestLogger.getAllLoggingEvents()).hasSize(2);

    assertCustomerSyncerLoggingEvents(customerSyncerTestLogger, 1);

    assertCustomersAreSyncedCorrectly(CTP_TARGET_CLIENT);

    final String sourceProjectKey = CTP_SOURCE_CLIENT.getProjectKey();

    assertLastSyncCustomObjectExists(
        CTP_TARGET_CLIENT, sourceProjectKey, "customerSync", "runnerName");
  }

  @Test
  void run_WithUpdatedCustomer_ShouldSyncCustomersAndStoreLastSyncTimestampsAsCustomObject() {
    ZonedDateTime lastModifiedTime = null;
    // preparation
    final SyncerFactory syncerFactory = createITSyncerFactory();

    // test
    CliRunner.of().run(new String[] {"-s", "customers"}, syncerFactory);

    lastModifiedTime =
        getCustomObjectLastModifiedTime(CTP_TARGET_CLIENT, "customerSync", "runnerName");
    updateCustomerSourceObject(CTP_SOURCE_CLIENT);

    // test
    CliRunner.of().run(new String[] {"-s", "customers"}, syncerFactory);

    // assertions
    assertUpdatedCustomersAreSyncedCorrectly(CTP_SOURCE_CLIENT, CTP_TARGET_CLIENT);

    assertUpdatedCustomObjectTimestampAfterSync(
        CTP_TARGET_CLIENT, "customerSync", "runnerName", lastModifiedTime);
  }

  private void assertUpdatedCustomObjectTimestampAfterSync(
      @Nonnull final ProjectApiRoot targetClient,
      @Nonnull final String syncModuleName,
      @Nonnull final String runnerName,
      @Nonnull final ZonedDateTime lastModifiedTime) {

    CustomObjectPagedQueryResponse lastSyncCustomObject =
        fetchLastSyncCustomObject(targetClient, syncModuleName, runnerName);

    assertThat(lastSyncCustomObject.getResults()).isNotEmpty();
    assertThat(lastModifiedTime)
        .isBefore(lastSyncCustomObject.getResults().get(0).getLastModifiedAt());
  }

  private ZonedDateTime getCustomObjectLastModifiedTime(
      @Nonnull final ProjectApiRoot targetClient,
      @Nonnull final String syncModuleName,
      @Nonnull final String runnerName) {
    final CustomObjectPagedQueryResponse lastSyncResult =
        fetchLastSyncCustomObject(targetClient, syncModuleName, runnerName);

    return lastSyncResult.getResults().isEmpty()
        ? null
        : lastSyncResult.getResults().get(0).getLastModifiedAt();
  }

  private void updateCustomerSourceObject(@Nonnull final ProjectApiRoot sourceProjectClient) {
    final Customer customer =
        sourceProjectClient.customers().withKey(RESOURCE_KEY).get().executeBlocking().getBody();

    sourceProjectClient
        .customers()
        .withKey(RESOURCE_KEY)
        .post(
            customerUpdateBuilder ->
                customerUpdateBuilder
                    .actions(
                        CustomerSetFirstNameActionBuilder.of().firstName("testFirstName").build(),
                        CustomerSetLastNameActionBuilder.of().lastName("testLastName").build())
                    .version(customer.getVersion()))
        .executeBlocking();
  }

  private void assertUpdatedCustomersAreSyncedCorrectly(
      @Nonnull final ProjectApiRoot sourceClient, @Nonnull final ProjectApiRoot targetClient) {
    final Customer sourceCustomer =
        sourceClient
            .customers()
            .withKey(RESOURCE_KEY)
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    final Customer targetCustomer =
        targetClient
            .customers()
            .withKey(RESOURCE_KEY)
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    assertThat(sourceCustomer.getFirstName()).isEqualTo(targetCustomer.getFirstName());
    assertThat(sourceCustomer.getLastName()).isEqualTo(targetCustomer.getLastName());
  }

  @Test
  void
      run_WithShoppingListSyncAsArgument_ShouldSyncShoppingListsAndStoreLastSyncTimestampsAsCustomObject() {
    // preparation
    // Shopping List contains Product, Customer and Type references. So, here it is recreating
    // ShoppingList without any references.
    prepareDataForShoppingListSync(CTP_SOURCE_CLIENT);

    // test
    CliRunner.of().run(new String[] {"-s", "shoppingLists"}, createITSyncerFactory());

    // create clients again (for assertions and cleanup), since the run method closes the clients
    // after execution is done.
    // assertions
    assertThat(shoppingListSyncerTestLogger.getAllLoggingEvents()).hasSize(2);

    assertShoppingListSyncerLoggingEvents(shoppingListSyncerTestLogger, 1);

    assertShoppingListsAreSyncedCorrectly(CTP_TARGET_CLIENT);

    final String sourceProjectKey = CTP_SOURCE_CLIENT.getProjectKey();

    assertLastSyncCustomObjectExists(
        CTP_TARGET_CLIENT, sourceProjectKey, "shoppingListSync", "runnerName");
  }

  private void prepareDataForShoppingListSync(ProjectApiRoot sourceClient) {
    QueryUtils.queryAll(
            sourceClient.shoppingLists().get(),
            shoppingLists -> {
              CompletableFuture.allOf(
                      shoppingLists.stream()
                          .map(
                              shoppingList ->
                                  sourceClient
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

    final ShoppingListDraft shoppingListDraft =
        ShoppingListDraftBuilder.of()
            .name(ofEnglish("shoppingList-name"))
            .key(RESOURCE_KEY)
            .build();

    sourceClient.shoppingLists().post(shoppingListDraft).executeBlocking();
  }

  @Test
  void
      run_WithCustomObjectSyncAsArgument_ShouldSyncCustomObjectsWithoutProjectSyncGeneratedCustomObjects() {
    // test
    CliRunner.of().run(new String[] {"-s", "customObjects"}, createITSyncerFactory());

    // assertions
    assertThat(customObjectSyncerTestLogger.getAllLoggingEvents()).hasSize(2);
    assertCustomObjectSyncerLoggingEvents(customObjectSyncerTestLogger, 1);
  }

  @Test
  void
      run_WithProductTypeSyncAsArgument_ShouldExecuteProductTypeSyncerAndStoreLastSyncTimestampsAsCustomObject() {
    // test
    CliRunner.of().run(new String[] {"-s", "productTypes"}, createITSyncerFactory());
    // assertions
    assertThat(productTypeSyncerTestLogger.getAllLoggingEvents()).hasSize(2);

    assertProductTypeSyncerLoggingEvents(productTypeSyncerTestLogger, 1);

    assertProductTypesAreSyncedCorrectly(CTP_TARGET_CLIENT);

    final ZonedDateTime lastSyncTimestamp =
        assertCurrentCtpTimestampGeneratorAndGetLastModifiedAt(
            CTP_TARGET_CLIENT, DEFAULT_RUNNER_NAME, "ProductTypeSync");

    final String sourceProjectKey = CTP_SOURCE_CLIENT.getProjectKey();

    assertLastSyncCustomObjectIsCorrect(
        CTP_TARGET_CLIENT,
        sourceProjectKey,
        "productTypeSync",
        DEFAULT_RUNNER_NAME,
        ProductTypeSyncStatistics.class,
        lastSyncTimestamp);
  }

  private static void assertTypesAreSyncedCorrectly(@Nonnull final ProjectApiRoot ctpClient) {
    final Type type =
        ctpClient
            .types()
            .withKey(RESOURCE_KEY)
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    assertThat(type.getKey()).isEqualTo(RESOURCE_KEY);
  }

  private static void assertProductTypesAreSyncedCorrectly(
      @Nonnull final ProjectApiRoot ctpClient) {
    final ProductType productType =
        ctpClient
            .productTypes()
            .withKey(RESOURCE_KEY)
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    assertThat(productType.getKey()).isEqualTo(RESOURCE_KEY);
  }

  private static void assertProductsAreSyncedCorrectly(@Nonnull final ProjectApiRoot ctpClient) {
    final Product product =
        ctpClient
            .products()
            .withKey(RESOURCE_KEY)
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    assertThat(product.getKey()).isEqualTo(RESOURCE_KEY);
  }

  private static void assertCustomersAreSyncedCorrectly(@Nonnull final ProjectApiRoot ctpClient) {
    final Customer customer =
        ctpClient
            .customers()
            .withKey(RESOURCE_KEY)
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    assertThat(customer.getKey()).isEqualTo(RESOURCE_KEY);
  }

  private static void assertShoppingListsAreSyncedCorrectly(
      @Nonnull final ProjectApiRoot ctpClient) {
    final ShoppingList shoppingList =
        ctpClient
            .shoppingLists()
            .withKey(RESOURCE_KEY)
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    assertThat(shoppingList.getKey()).isEqualTo(RESOURCE_KEY);
  }

  private static void assertCategoriesAreSyncedCorrectly(@Nonnull final ProjectApiRoot ctpClient) {
    final Category category =
        ctpClient
            .categories()
            .withKey(RESOURCE_KEY)
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    assertThat(category.getKey()).isEqualTo(RESOURCE_KEY);
  }

  private static void assertStatesAreSyncedCorrectly(@Nonnull final ProjectApiRoot ctpClient) {
    final State state =
        ctpClient
            .states()
            .withKey(RESOURCE_KEY)
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    assertThat(state.getKey()).isEqualTo(RESOURCE_KEY);
  }

  private static void assertTaxCategoriesAreSyncedCorrectly(
      @Nonnull final ProjectApiRoot ctpClient) {
    final TaxCategory taxCategory =
        ctpClient
            .taxCategories()
            .withKey(RESOURCE_KEY)
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    assertThat(taxCategory.getKey()).isEqualTo(RESOURCE_KEY);
  }

  private static void assertCartDiscountsAreSyncedCorrectly(
      @Nonnull final ProjectApiRoot ctpClient) {
    final CartDiscount cartDiscount =
        ctpClient
            .cartDiscounts()
            .withKey(RESOURCE_KEY)
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    assertThat(cartDiscount.getKey()).isEqualTo(RESOURCE_KEY);
  }

  private void assertLastSyncCustomObjectIsCorrect(
      @Nonnull final ProjectApiRoot targetClient,
      @Nonnull final String sourceProjectKey,
      @Nonnull final String syncModuleName,
      @Nonnull final String syncRunnerName,
      @Nonnull final Class<? extends BaseSyncStatistics> statisticsClass,
      @Nonnull final ZonedDateTime lastSyncTimestamp) {

    final LastSyncCustomObject lastSyncCustomObject =
        assertLastSyncCustomObjectExists(
            targetClient, sourceProjectKey, syncModuleName, syncRunnerName);

    assertThat(lastSyncCustomObject.getLastSyncStatistics()).isInstanceOf(statisticsClass);
    assertThat(lastSyncCustomObject.getLastSyncStatistics().getProcessed().get()).isEqualTo(1);
    assertThat(lastSyncCustomObject.getLastSyncTimestamp()).isBeforeOrEqualTo(lastSyncTimestamp);
  }

  @Nonnull
  private ZonedDateTime assertCurrentCtpTimestampGeneratorAndGetLastModifiedAt(
      @Nonnull final ProjectApiRoot targetClient,
      @Nonnull final String runnerName,
      @Nonnull final String syncModuleName) {

    final String container =
        format(
            "%s.%s.%s.%s",
            APPLICATION_DEFAULT_NAME, runnerName, syncModuleName, TIMESTAMP_GENERATOR_KEY);

    final CustomObjectPagedQueryResponse currentCtpTimestampGeneratorResults =
        targetClient
            .customObjects()
            .withContainer(container)
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    assertThat(currentCtpTimestampGeneratorResults.getResults())
        .hasSize(1)
        .singleElement()
        .satisfies(
            currentCtpTimestamp -> {
              assertThat(currentCtpTimestamp.getKey()).isEqualTo(TIMESTAMP_GENERATOR_KEY);
              assertThat((String) currentCtpTimestamp.getValue())
                  .matches(
                      "[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}");
            });

    return currentCtpTimestampGeneratorResults.getResults().get(0).getLastModifiedAt();
  }

  @Test
  void
      run_WithSyncAsArgumentWithAllArgAsDeltaSync_ShouldExecuteAllSyncersAndStoreLastSyncTimestampsAsCustomObjects() {
    // test
    CliRunner.of().run(new String[] {"-s", "all", "-r", "runnerName"}, createITSyncerFactory());
    assertAllSyncersLoggingEvents(1);

    assertAllResourcesAreSyncedToTarget(CTP_TARGET_CLIENT);
    assertCurrentCtpTimestampGeneratorAndGetLastModifiedAt(
        CTP_TARGET_CLIENT, "runnerName", "ProductTypeSync");

    final String sourceProjectKey = CTP_SOURCE_CLIENT.getProjectKey();

    assertLastSyncCustomObjectExists(
        CTP_TARGET_CLIENT, sourceProjectKey, "productSync", "runnerName");

    assertLastSyncCustomObjectExists(
        CTP_TARGET_CLIENT, sourceProjectKey, "categorySync", "runnerName");

    assertLastSyncCustomObjectExists(
        CTP_TARGET_CLIENT, sourceProjectKey, "productTypeSync", "runnerName");

    assertLastSyncCustomObjectExists(CTP_TARGET_CLIENT, sourceProjectKey, "typeSync", "runnerName");

    assertLastSyncCustomObjectExists(
        CTP_TARGET_CLIENT, sourceProjectKey, "inventorySync", "runnerName");

    assertLastSyncCustomObjectExists(
        CTP_TARGET_CLIENT, sourceProjectKey, "cartDiscountSync", "runnerName");

    assertLastSyncCustomObjectExists(
        CTP_TARGET_CLIENT, sourceProjectKey, "stateSync", "runnerName");

    assertLastSyncCustomObjectExists(
        CTP_TARGET_CLIENT, sourceProjectKey, "taxCategorySync", "runnerName");

    assertLastSyncCustomObjectExists(
        CTP_TARGET_CLIENT, sourceProjectKey, "customObjectSync", "runnerName");

    assertLastSyncCustomObjectExists(
        CTP_TARGET_CLIENT, sourceProjectKey, "customerSync", "runnerName");
  }

  @Test
  void run_WithSyncAsArgumentWithAllArgAsFullSync_ShouldExecuteAllSyncers() {
    // test
    CliRunner.of()
        .run(new String[] {"-s", "all", "-r", "runnerName", "-f"}, createITSyncerFactory());
    // assertions
    assertAllSyncersLoggingEvents(1);

    assertAllResourcesAreSyncedToTarget(CTP_TARGET_CLIENT);
    assertCurrentCtpTimestampGeneratorDoesntExist(
        CTP_TARGET_CLIENT, "runnerName", "ProductTypeSync");
    assertNoProjectSyncCustomObjectExists(CTP_TARGET_CLIENT);
  }

  @Test
  void
      run_WithSyncAsArgumentWithAllArgAsFullSyncAndWithCustomQueryAndLimitForProducts_ShouldExecuteAllSyncers() {
    final Long limit = 100L;
    final String customQuery = "\"published=true AND masterVariant(key= \\\"foo\\\")\"";
    final String productQueryParametersValue =
        "{\"limit\": " + limit + ", \"where\": " + customQuery + "}";

    // test
    CliRunner.of()
        .run(
            new String[] {
              "-s",
              "all",
              "-r",
              "runnerName",
              "-f",
              "-productQueryParameters",
              productQueryParametersValue
            },
            createITSyncerFactory());
    // assertions
    assertAllSyncersLoggingEvents(1);

    assertAllResourcesAreSyncedToTarget(CTP_TARGET_CLIENT);
    assertCurrentCtpTimestampGeneratorDoesntExist(
        CTP_TARGET_CLIENT, "runnerName", "ProductTypeSync");
    assertNoProjectSyncCustomObjectExists(CTP_TARGET_CLIENT);
  }

  private void assertNoProjectSyncCustomObjectExists(@Nonnull final ProjectApiRoot targetClient) {
    final CustomObjectPagedQueryResponse lastSyncResult =
        targetClient
            .customObjects()
            .withContainer(PROJECT_SYNC_CONTAINER_NAME)
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();
    assertThat(lastSyncResult.getResults()).isEmpty();
  }

  private void assertCurrentCtpTimestampGeneratorDoesntExist(
      @Nonnull final ProjectApiRoot targetClient,
      @Nonnull final String runnerName,
      @Nonnull final String syncModuleName) {
    final String container =
        format(
            "commercetools-project-sync.%s.%s.%s",
            runnerName, syncModuleName, TIMESTAMP_GENERATOR_KEY);

    final CustomObjectPagedQueryResponse currentCtpTimestampGeneratorResults =
        targetClient
            .customObjects()
            .withContainer(container)
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    assertThat(currentCtpTimestampGeneratorResults.getResults()).isEmpty();
  }

  private LastSyncCustomObject assertLastSyncCustomObjectExists(
      @Nonnull final ProjectApiRoot targetClient,
      @Nonnull final String sourceProjectKey,
      @Nonnull final String syncModuleName,
      @Nonnull final String runnerName) {

    final CustomObjectPagedQueryResponse lastSyncResult =
        fetchLastSyncCustomObject(targetClient, syncModuleName, runnerName);

    assertThat(lastSyncResult.getResults())
        .hasSize(1)
        .singleElement()
        .satisfies(
            customObject -> {
              assertThat(customObject.getKey()).isEqualTo(sourceProjectKey);
              assertThat(customObject.getValue()).isNotNull();
            });
    final CustomObject customObject = lastSyncResult.getResults().get(0);
    final ObjectMapper objectMapper = JsonUtils.getConfiguredObjectMapper();
    final LastSyncCustomObject lastSyncCustomObject =
        objectMapper.convertValue(customObject.getValue(), LastSyncCustomObject.class);

    assertThat(lastSyncCustomObject.getLastSyncTimestamp()).isNotNull();
    assertThat(lastSyncCustomObject.getLastSyncDurationInMillis()).isNotNull();
    assertThat(lastSyncCustomObject.getApplicationVersion()).isNotNull();
    return lastSyncCustomObject;
  }

  private CustomObjectPagedQueryResponse fetchLastSyncCustomObject(
      @Nonnull ProjectApiRoot targetClient,
      @Nonnull String syncModuleName,
      @Nonnull String runnerName) {
    final String container =
        format("%s.%s.%s", APPLICATION_DEFAULT_NAME, runnerName, syncModuleName);
    return targetClient
        .customObjects()
        .withContainer(container)
        .get()
        .execute()
        .thenApply(ApiHttpResponse::getBody)
        .toCompletableFuture()
        .join();
  }

  private static void assertAllResourcesAreSyncedToTarget(
      @Nonnull final ProjectApiRoot targetClient) {

    assertProductTypeExists(targetClient, RESOURCE_KEY);
    assertCategoryExists(targetClient, RESOURCE_KEY);
    assertProductExists(targetClient, RESOURCE_KEY, RESOURCE_KEY, RESOURCE_KEY);
    assertTypesAreSyncedCorrectly(targetClient);
    assertTaxCategoriesAreSyncedCorrectly(targetClient);

    final InventoryPagedQueryResponse inventoryEntryQueryResult =
        targetClient
            .inventory()
            .get()
            .withWhere(format("sku=\"%s\"", RESOURCE_KEY))
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    assertThat(inventoryEntryQueryResult.getResults())
        .hasSize(1)
        .singleElement()
        .satisfies(inventoryEntry -> assertThat(inventoryEntry.getSku()).isEqualTo(RESOURCE_KEY));

    assertCartDiscountsAreSyncedCorrectly(targetClient);
    assertStatesAreSyncedCorrectly(targetClient);

    final CustomObject customObject =
        targetClient
            .customObjects()
            .withContainerAndKey(RESOURCE_KEY, RESOURCE_KEY)
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    assertThat(customObject.getKey()).isEqualTo(RESOURCE_KEY);

    assertCustomersAreSyncedCorrectly(targetClient);
    assertShoppingListsAreSyncedCorrectly(targetClient);
  }

  public static void assertAllSyncersLoggingEvents(final int numberOfResources) {

    assertThat(cliRunnerTestLogger.getAllLoggingEvents())
        .allMatch(loggingEvent -> !Level.ERROR.equals(loggingEvent.getLevel()));

    assertTypeSyncerLoggingEvents(typeSyncerTestLogger, numberOfResources);
    assertProductTypeSyncerLoggingEvents(productTypeSyncerTestLogger, numberOfResources);
    assertCategorySyncerLoggingEvents(categorySyncerTestLogger, numberOfResources);
    assertProductSyncerLoggingEvents(productSyncerTestLogger, numberOfResources);
    assertInventoryEntrySyncerLoggingEvents(inventoryEntrySyncerTestLogger, numberOfResources);
    assertCartDiscountSyncerLoggingEvents(cartDiscountSyncerTestLogger, numberOfResources);
    // +1 state is a built-in state and it cant be deleted
    assertStateSyncerLoggingEvents(stateSyncerTestLogger, numberOfResources + 1);
    assertTaxCategorySyncerLoggingEvents(taxCategorySyncerTestLogger, numberOfResources);
    assertCustomerSyncerLoggingEvents(customerSyncerTestLogger, numberOfResources);
    assertShoppingListSyncerLoggingEvents(shoppingListSyncerTestLogger, numberOfResources);

    // Every sync module is expected to have 2 logs (start and stats summary)
    assertThat(typeSyncerTestLogger.getAllLoggingEvents()).hasSize(2);
    assertThat(productTypeSyncerTestLogger.getAllLoggingEvents()).hasSize(2);
    assertThat(categorySyncerTestLogger.getAllLoggingEvents()).hasSize(2);
    assertThat(productSyncerTestLogger.getAllLoggingEvents()).hasSize(2);
    assertThat(inventoryEntrySyncerTestLogger.getAllLoggingEvents()).hasSize(2);
    assertThat(cartDiscountSyncerTestLogger.getAllLoggingEvents()).hasSize(2);
    assertThat(stateSyncerTestLogger.getAllLoggingEvents()).hasSize(2);
    assertThat(taxCategorySyncerTestLogger.getAllLoggingEvents()).hasSize(2);
    assertThat(customObjectSyncerTestLogger.getAllLoggingEvents()).hasSize(2);
    assertThat(customerSyncerTestLogger.getAllLoggingEvents()).hasSize(2);
    assertThat(shoppingListSyncerTestLogger.getAllLoggingEvents()).hasSize(2);
  }
}
