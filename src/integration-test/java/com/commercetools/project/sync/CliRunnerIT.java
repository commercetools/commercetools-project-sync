package com.commercetools.project.sync;

import static com.commercetools.project.sync.service.impl.CustomObjectServiceImpl.TIMESTAMP_GENERATOR_KEY;
import static com.commercetools.project.sync.util.IntegrationTestUtils.assertCategoryExists;
import static com.commercetools.project.sync.util.IntegrationTestUtils.assertProductExists;
import static com.commercetools.project.sync.util.IntegrationTestUtils.assertProductTypeExists;
import static com.commercetools.project.sync.util.IntegrationTestUtils.cleanUpProjects;
import static com.commercetools.project.sync.util.IntegrationTestUtils.createITSyncerFactory;
import static com.commercetools.project.sync.util.QueryUtils.queryAndExecute;
import static com.commercetools.project.sync.util.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.project.sync.util.SphereClientUtils.CTP_TARGET_CLIENT;
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
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

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
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.CartDiscountDraftBuilder;
import io.sphere.sdk.cartdiscounts.CartDiscountValue;
import io.sphere.sdk.cartdiscounts.ShippingCostTarget;
import io.sphere.sdk.cartdiscounts.commands.CartDiscountCreateCommand;
import io.sphere.sdk.cartdiscounts.queries.CartDiscountQuery;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.CustomerDraftBuilder;
import io.sphere.sdk.customers.CustomerName;
import io.sphere.sdk.customers.CustomerSignInResult;
import io.sphere.sdk.customers.commands.CustomerCreateCommand;
import io.sphere.sdk.customers.commands.CustomerUpdateCommand;
import io.sphere.sdk.customers.commands.updateactions.ChangeName;
import io.sphere.sdk.customers.queries.CustomerQuery;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;
import io.sphere.sdk.customobjects.commands.CustomObjectUpsertCommand;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import io.sphere.sdk.inventory.commands.InventoryEntryCreateCommand;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.queries.QueryPredicate;
import io.sphere.sdk.shoppinglists.LineItemDraft;
import io.sphere.sdk.shoppinglists.LineItemDraftBuilder;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.ShoppingListDraftBuilder;
import io.sphere.sdk.shoppinglists.commands.ShoppingListCreateCommand;
import io.sphere.sdk.shoppinglists.commands.ShoppingListDeleteCommand;
import io.sphere.sdk.shoppinglists.queries.ShoppingListQuery;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.StateDraftBuilder;
import io.sphere.sdk.states.StateType;
import io.sphere.sdk.states.commands.StateCreateCommand;
import io.sphere.sdk.states.queries.StateQuery;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import io.sphere.sdk.taxcategories.TaxCategoryDraftBuilder;
import io.sphere.sdk.taxcategories.TaxRateDraft;
import io.sphere.sdk.taxcategories.TaxRateDraftBuilder;
import io.sphere.sdk.taxcategories.commands.TaxCategoryCreateCommand;
import io.sphere.sdk.taxcategories.queries.TaxCategoryQuery;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.CustomFieldsDraftBuilder;
import io.sphere.sdk.types.ResourceTypeIdsSetBuilder;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.TypeDraftBuilder;
import io.sphere.sdk.types.commands.TypeCreateCommand;
import io.sphere.sdk.types.queries.TypeQuery;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

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

  static void setupSourceProjectData(@Nonnull final SphereClient sourceProjectClient)
      throws ExecutionException, InterruptedException {
    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of(
                RESOURCE_KEY, "sample-product-type", "a productType for t-shirts", emptyList())
            .build();

    final ProductType productType =
        sourceProjectClient
            .execute(ProductTypeCreateCommand.of(productTypeDraft))
            .toCompletableFuture()
            .join();

    final TypeDraft typeDraft =
        TypeDraftBuilder.of(
                RESOURCE_KEY,
                ofEnglish("category-custom-type"),
                ResourceTypeIdsSetBuilder.of().addCategories().add("shopping-list"))
            .build();

    final StateDraft stateDraft =
        StateDraftBuilder.of(RESOURCE_KEY, StateType.PRODUCT_STATE)
            .roles(Collections.emptySet())
            .description(ofEnglish("State 1"))
            .name(ofEnglish("State 1"))
            .initial(true)
            .transitions(Collections.emptySet())
            .build();
    final State state =
        sourceProjectClient.execute(StateCreateCommand.of(stateDraft)).toCompletableFuture().join();

    final CompletableFuture<Type> typeFuture =
        sourceProjectClient.execute(TypeCreateCommand.of(typeDraft)).toCompletableFuture();

    final TaxRateDraft taxRateDraft =
        TaxRateDraftBuilder.of("Tax-Rate-Name-1", 0.3, false, CountryCode.DE).build();

    final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of(
                "Tax-Category-Name-1", singletonList(taxRateDraft), "Tax-Category-Description-1")
            .key(RESOURCE_KEY)
            .build();

    final TaxCategory taxCategory =
        sourceProjectClient
            .execute(TaxCategoryCreateCommand.of(taxCategoryDraft))
            .toCompletableFuture()
            .join();

    final ObjectNode customObjectValue = JsonNodeFactory.instance.objectNode().put("name", "value");
    final CustomObjectDraft<JsonNode> customObjectDraft =
        CustomObjectDraft.ofUnversionedUpsert(RESOURCE_KEY, RESOURCE_KEY, customObjectValue);
    // following custom object should not be synced as it's created by the project-sync itself
    final CustomObjectDraft<JsonNode> customObjectToIgnore =
        CustomObjectDraft.ofUnversionedUpsert(
            PROJECT_SYNC_CONTAINER_NAME, "timestampGenerator", customObjectValue);

    final CompletableFuture<CustomObject<JsonNode>> customObjectFuture1 =
        sourceProjectClient
            .execute(CustomObjectUpsertCommand.of(customObjectDraft))
            .toCompletableFuture();

    final CompletableFuture<CustomObject<JsonNode>> customObjectFuture2 =
        sourceProjectClient
            .execute(CustomObjectUpsertCommand.of(customObjectToIgnore))
            .toCompletableFuture();

    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of(ofEnglish("t-shirts"), ofEnglish("t-shirts"))
            .key(RESOURCE_KEY)
            .build();

    final CompletableFuture<Category> categoryFuture =
        sourceProjectClient.execute(CategoryCreateCommand.of(categoryDraft)).toCompletableFuture();

    final CustomerDraft customerDraft =
        CustomerDraftBuilder.of("test@email.com", "testPassword").key(RESOURCE_KEY).build();

    final CompletableFuture<CustomerSignInResult> customerFuture =
        sourceProjectClient.execute(CustomerCreateCommand.of(customerDraft)).toCompletableFuture();

    CompletableFuture.allOf(
            typeFuture, customObjectFuture1, customObjectFuture2, categoryFuture, customerFuture)
        .join();

    final ProductDraft productDraft =
        ProductDraftBuilder.of(
                productType,
                ofEnglish("V-neck Tee"),
                ofEnglish("v-neck-tee"),
                ProductVariantDraftBuilder.of().key(RESOURCE_KEY).sku(RESOURCE_KEY).build())
            .state(state)
            .taxCategory(taxCategory)
            .key(RESOURCE_KEY)
            .publish(true)
            .build();

    final CompletableFuture<Product> productFuture =
        sourceProjectClient.execute(ProductCreateCommand.of(productDraft)).toCompletableFuture();

    final InventoryEntryDraft inventoryEntryDraft =
        InventoryEntryDraftBuilder.of(RESOURCE_KEY, 1L).build();

    final CompletableFuture<InventoryEntry> inventoryFuture =
        sourceProjectClient
            .execute(InventoryEntryCreateCommand.of(inventoryEntryDraft))
            .toCompletableFuture();

    final CartDiscountDraft cartDiscountDraft =
        CartDiscountDraftBuilder.of(
                ofEnglish("my-cart-discount"),
                "1 = 1",
                CartDiscountValue.ofRelative(1),
                ShippingCostTarget.of(),
                "0.1",
                true)
            .key(RESOURCE_KEY)
            .build();

    final CompletableFuture<CartDiscount> cartDiscountFuture =
        sourceProjectClient
            .execute(CartDiscountCreateCommand.of(cartDiscountDraft))
            .toCompletableFuture();
    CompletableFuture.allOf(productFuture, inventoryFuture, cartDiscountFuture);

    final CustomerSignInResult customerSignInResult = customerFuture.get();
    final Product product = productFuture.get();
    final LineItemDraft lineItemDraft = LineItemDraftBuilder.of(product.getId()).build();
    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraftBuilder.ofTypeId(typeFuture.get().getId()).build();

    final ShoppingListDraft shoppingListDraft =
        ShoppingListDraftBuilder.of(ofEnglish("shoppingList-name"))
            .key(RESOURCE_KEY)
            .customer(ResourceIdentifier.ofId(customerSignInResult.getCustomer().getId()))
            .lineItems(Collections.singletonList(lineItemDraft))
            .custom(customFieldsDraft)
            .build();

    sourceProjectClient
        .execute(ShoppingListCreateCommand.of(shoppingListDraft))
        .toCompletableFuture()
        .join();
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

    final String sourceProjectKey = CTP_SOURCE_CLIENT.getConfig().getProjectKey();

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

    final String sourceProjectKey = CTP_SOURCE_CLIENT.getConfig().getProjectKey();

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

    final String sourceProjectKey = CTP_SOURCE_CLIENT.getConfig().getProjectKey();

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

    final String sourceProjectKey = CTP_SOURCE_CLIENT.getConfig().getProjectKey();

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

    final String sourceProjectKey = CTP_SOURCE_CLIENT.getConfig().getProjectKey();

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

    assertUpdatedCustomersAreSyncedCorrectly(CTP_SOURCE_CLIENT, CTP_TARGET_CLIENT);

    assertUpdatedCustomObjectTimestampAfterSync(
        CTP_TARGET_CLIENT, "customerSync", "runnerName", lastModifiedTime);
  }

  private void assertUpdatedCustomObjectTimestampAfterSync(
      @Nonnull final SphereClient targetClient,
      @Nonnull final String syncModuleName,
      @Nonnull final String runnerName,
      @Nonnull final ZonedDateTime lastModifiedTime) {
    final PagedQueryResult<CustomObject<LastSyncCustomObject>> lastSyncResult =
        fetchLastSyncCustomObject(targetClient, syncModuleName, runnerName);

    assertThat(lastModifiedTime)
        .isBefore(lastSyncResult.getResults().get(0).getValue().getLastSyncTimestamp());
  }

  private ZonedDateTime getCustomObjectLastModifiedTime(
      @Nonnull final SphereClient targetClient,
      @Nonnull final String syncModuleName,
      @Nonnull final String runnerName) {
    final PagedQueryResult<CustomObject<LastSyncCustomObject>> lastSyncResult =
        fetchLastSyncCustomObject(targetClient, syncModuleName, runnerName);

    return lastSyncResult.getResults().get(0).getValue().getLastSyncTimestamp();
  }

  private void updateCustomerSourceObject(@Nonnull final SphereClient sourceProjectClient) {
    final PagedQueryResult<Customer> customerPagedQueryResult =
        sourceProjectClient
            .execute(
                CustomerQuery.of()
                    .withPredicates(QueryPredicate.of(format("key=\"%s\"", RESOURCE_KEY))))
            .toCompletableFuture()
            .join();
    final Customer customer = customerPagedQueryResult.getResults().get(0);

    sourceProjectClient
        .execute(
            CustomerUpdateCommand.of(
                customer,
                ChangeName.of(CustomerName.ofFirstAndLastName("testFirstName", "testLastName"))))
        .toCompletableFuture()
        .join();
  }

  private void assertUpdatedCustomersAreSyncedCorrectly(
      @Nonnull final SphereClient cspClient, @Nonnull final SphereClient ctpClient) {
    final PagedQueryResult<Customer> sourceCustomerPagedQueryResult =
        cspClient
            .execute(
                CustomerQuery.of()
                    .withPredicates(QueryPredicate.of(format("key=\"%s\"", RESOURCE_KEY))))
            .toCompletableFuture()
            .join();
    Customer sourceCustomer = sourceCustomerPagedQueryResult.getResults().get(0);

    final PagedQueryResult<Customer> targetCustomerPagedQueryResult =
        ctpClient
            .execute(
                CustomerQuery.of()
                    .withPredicates(QueryPredicate.of(format("key=\"%s\"", RESOURCE_KEY))))
            .toCompletableFuture()
            .join();
    Customer targetCustomer = targetCustomerPagedQueryResult.getResults().get(0);

    assertThat(sourceCustomer.getName()).isEqualTo(targetCustomer.getName());
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

    final String sourceProjectKey = CTP_SOURCE_CLIENT.getConfig().getProjectKey();

    assertLastSyncCustomObjectExists(
        CTP_TARGET_CLIENT, sourceProjectKey, "shoppingListSync", "runnerName");
  }

  private void prepareDataForShoppingListSync(SphereClient sourceClient) {
    queryAndExecute(sourceClient, ShoppingListQuery.of(), ShoppingListDeleteCommand::of).join();
    final ShoppingListDraft shoppingListDraft =
        ShoppingListDraftBuilder.of(ofEnglish("shoppingList-name")).key(RESOURCE_KEY).build();

    sourceClient
        .execute(ShoppingListCreateCommand.of(shoppingListDraft))
        .toCompletableFuture()
        .join();
  }

  @Test
  void
      run_WithCustomObjectSyncAsArgument_ShouldSyncCustomObjectsWithoutProjectSyncGeneratedCustomObjects() {
    // test
    CliRunner.of().run(new String[] {"-s", "customObjects"}, createITSyncerFactory());
    // assertions
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

    final String sourceProjectKey = CTP_SOURCE_CLIENT.getConfig().getProjectKey();

    assertLastSyncCustomObjectIsCorrect(
        CTP_TARGET_CLIENT,
        sourceProjectKey,
        "productTypeSync",
        DEFAULT_RUNNER_NAME,
        ProductSyncStatistics.class,
        lastSyncTimestamp);
  }

  private static void assertTypesAreSyncedCorrectly(@Nonnull final SphereClient ctpClient) {
    final String queryPredicate = format("key=\"%s\"", RESOURCE_KEY);

    final PagedQueryResult<Type> typeQueryResult =
        ctpClient
            .execute(TypeQuery.of().withPredicates(QueryPredicate.of(queryPredicate)))
            .toCompletableFuture()
            .join();

    assertThat(typeQueryResult.getResults())
        .hasSize(1)
        .singleElement()
        .satisfies(productType -> assertThat(productType.getKey()).isEqualTo(RESOURCE_KEY));
  }

  private static void assertProductTypesAreSyncedCorrectly(@Nonnull final SphereClient ctpClient) {

    final PagedQueryResult<ProductType> productTypeQueryResult =
        ctpClient.execute(ProductTypeQuery.of().byKey(RESOURCE_KEY)).toCompletableFuture().join();

    assertThat(productTypeQueryResult.getResults())
        .hasSize(1)
        .singleElement()
        .satisfies(productType -> assertThat(productType.getKey()).isEqualTo(RESOURCE_KEY));
  }

  private static void assertProductsAreSyncedCorrectly(@Nonnull final SphereClient ctpClient) {

    final PagedQueryResult<Product> productQueryResult =
        ctpClient.execute(ProductQuery.of()).toCompletableFuture().join();

    assertThat(productQueryResult.getResults())
        .hasSize(1)
        .singleElement()
        .satisfies(product -> assertThat(product.getKey()).isEqualTo(RESOURCE_KEY));
  }

  private static void assertCustomersAreSyncedCorrectly(@Nonnull final SphereClient ctpClient) {
    final PagedQueryResult<Customer> customerPagedQueryResult =
        ctpClient
            .execute(
                CustomerQuery.of()
                    .withPredicates(QueryPredicate.of(format("key=\"%s\"", RESOURCE_KEY))))
            .toCompletableFuture()
            .join();
    assertThat(customerPagedQueryResult.getResults()).hasSize(1);
  }

  private static void assertShoppingListsAreSyncedCorrectly(@Nonnull final SphereClient ctpClient) {
    final PagedQueryResult<ShoppingList> shoppingListPagedQueryResult =
        ctpClient
            .execute(
                ShoppingListQuery.of()
                    .withPredicates(QueryPredicate.of(format("key=\"%s\"", RESOURCE_KEY))))
            .toCompletableFuture()
            .join();
    assertThat(shoppingListPagedQueryResult.getResults()).hasSize(1);
  }

  private static void assertCategoriesAreSyncedCorrectly(@Nonnull final SphereClient ctpClient) {
    final PagedQueryResult<Category> categoryPagedQueryResult =
        ctpClient
            .execute(
                CategoryQuery.of()
                    .withPredicates(QueryPredicate.of(format("key=\"%s\"", RESOURCE_KEY))))
            .toCompletableFuture()
            .join();
    assertThat(categoryPagedQueryResult.getResults()).hasSize(1);
  }

  private static void assertStatesAreSyncedCorrectly(@Nonnull final SphereClient ctpClient) {
    final PagedQueryResult<State> statePagedQueryResult =
        ctpClient
            .execute(
                StateQuery.of()
                    .withPredicates(QueryPredicate.of(format("key=\"%s\"", RESOURCE_KEY))))
            .toCompletableFuture()
            .join();
    assertThat(statePagedQueryResult.getResults()).hasSize(1);
  }

  private static void assertTaxCategoriesAreSyncedCorrectly(@Nonnull final SphereClient ctpClient) {
    final PagedQueryResult<TaxCategory> taxCategoryPagedQueryResult =
        ctpClient
            .execute(
                TaxCategoryQuery.of()
                    .withPredicates(QueryPredicate.of(format("key=\"%s\"", RESOURCE_KEY))))
            .toCompletableFuture()
            .join();
    assertThat(taxCategoryPagedQueryResult.getResults()).hasSize(1);
  }

  private static void assertCartDiscountsAreSyncedCorrectly(@Nonnull final SphereClient ctpClient) {
    final PagedQueryResult<CartDiscount> cartDiscountPagedQueryResult =
        ctpClient
            .execute(
                CartDiscountQuery.of()
                    .withPredicates(QueryPredicate.of(format("key=\"%s\"", RESOURCE_KEY))))
            .toCompletableFuture()
            .join();
    assertThat(cartDiscountPagedQueryResult.getResults()).hasSize(1);
  }

  private void assertLastSyncCustomObjectIsCorrect(
      @Nonnull final SphereClient targetClient,
      @Nonnull final String sourceProjectKey,
      @Nonnull final String syncModuleName,
      @Nonnull final String syncRunnerName,
      @Nonnull final Class<? extends BaseSyncStatistics> statisticsClass,
      @Nonnull final ZonedDateTime lastSyncTimestamp) {

    final PagedQueryResult<CustomObject<LastSyncCustomObject>> lastSyncResult =
        fetchLastSyncCustomObject(targetClient, syncModuleName, syncRunnerName);

    assertThat(lastSyncResult.getResults())
        .hasSize(1)
        .singleElement()
        .satisfies(
            lastSyncCustomObject -> {
              assertThat(lastSyncCustomObject.getKey()).isEqualTo(sourceProjectKey);
              assertThat(lastSyncCustomObject.getValue())
                  .satisfies(
                      value -> {
                        // Excluding til BaseStatisticsDeserializer works with correct type.
                        // assertThat(value.getLastSyncStatistics()).isInstanceOf(statisticsClass);
                        assertThat(value.getLastSyncStatistics().getProcessed().get()).isEqualTo(1);
                        assertThat(value.getLastSyncTimestamp())
                            .isBeforeOrEqualTo(lastSyncTimestamp);
                      });
            });
  }

  @Nonnull
  private ZonedDateTime assertCurrentCtpTimestampGeneratorAndGetLastModifiedAt(
      @Nonnull final SphereClient targetClient,
      @Nonnull final String runnerName,
      @Nonnull final String syncModuleName) {

    final CustomObjectQuery<String> timestampGeneratorQuery =
        CustomObjectQuery.of(String.class)
            .byContainer(
                format(
                    "%s.%s.%s.%s",
                    APPLICATION_DEFAULT_NAME, runnerName, syncModuleName, TIMESTAMP_GENERATOR_KEY));

    final PagedQueryResult<CustomObject<String>> currentCtpTimestampGeneratorResults =
        targetClient.execute(timestampGeneratorQuery).toCompletableFuture().join();

    assertThat(currentCtpTimestampGeneratorResults.getResults())
        .hasSize(1)
        .singleElement()
        .satisfies(
            currentCtpTimestamp -> {
              assertThat(currentCtpTimestamp.getKey()).isEqualTo(TIMESTAMP_GENERATOR_KEY);
              assertThat(currentCtpTimestamp.getValue())
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

    final String sourceProjectKey = CTP_SOURCE_CLIENT.getConfig().getProjectKey();

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

  private void assertNoProjectSyncCustomObjectExists(@Nonnull final SphereClient targetClient) {
    final CustomObjectQuery<LastSyncCustomObject> lastSyncQuery =
        CustomObjectQuery.of(LastSyncCustomObject.class)
            .plusPredicates(
                customObjectQueryModel ->
                    customObjectQueryModel.container().is(PROJECT_SYNC_CONTAINER_NAME));
    final PagedQueryResult<CustomObject<LastSyncCustomObject>> lastSyncResult =
        targetClient.execute(lastSyncQuery).toCompletableFuture().join();
    assertThat(lastSyncResult.getResults()).isEmpty();
  }

  private void assertCurrentCtpTimestampGeneratorDoesntExist(
      @Nonnull final SphereClient targetClient,
      @Nonnull final String runnerName,
      @Nonnull final String syncModuleName) {

    final CustomObjectQuery<String> timestampGeneratorQuery =
        CustomObjectQuery.of(String.class)
            .byContainer(
                format(
                    "commercetools-project-sync.%s.%s.%s",
                    runnerName, syncModuleName, TIMESTAMP_GENERATOR_KEY));

    final PagedQueryResult<CustomObject<String>> currentCtpTimestampGeneratorResults =
        targetClient.execute(timestampGeneratorQuery).toCompletableFuture().join();

    assertThat(currentCtpTimestampGeneratorResults.getResults()).isEmpty();
  }

  private void assertLastSyncCustomObjectExists(
      @Nonnull final SphereClient targetClient,
      @Nonnull final String sourceProjectKey,
      @Nonnull final String syncModuleName,
      @Nonnull final String runnerName) {

    final PagedQueryResult<CustomObject<LastSyncCustomObject>> lastSyncResult =
        fetchLastSyncCustomObject(targetClient, syncModuleName, runnerName);

    assertThat(lastSyncResult.getResults())
        .hasSize(1)
        .singleElement()
        .satisfies(
            lastSyncCustomObject -> {
              assertThat(lastSyncCustomObject.getKey()).isEqualTo(sourceProjectKey);
              assertThat(lastSyncCustomObject.getValue())
                  .satisfies(
                      value -> {
                        assertThat(value.getLastSyncTimestamp()).isNotNull();
                        assertThat(value.getLastSyncDurationInMillis()).isNotNull();
                        assertThat(value.getApplicationVersion()).isNotNull();
                      });
            });
  }

  private PagedQueryResult<CustomObject<LastSyncCustomObject>> fetchLastSyncCustomObject(
      @Nonnull SphereClient targetClient,
      @Nonnull String syncModuleName,
      @Nonnull String runnerName) {
    final CustomObjectQuery<LastSyncCustomObject> lastSyncQuery =
        CustomObjectQuery.of(LastSyncCustomObject.class)
            .byContainer(format("%s.%s.%s", APPLICATION_DEFAULT_NAME, runnerName, syncModuleName));

    return targetClient.execute(lastSyncQuery).toCompletableFuture().join();
  }

  private static void assertAllResourcesAreSyncedToTarget(
      @Nonnull final SphereClient targetClient) {

    assertProductTypeExists(targetClient, RESOURCE_KEY);
    assertCategoryExists(targetClient, RESOURCE_KEY);
    assertProductExists(targetClient, RESOURCE_KEY, RESOURCE_KEY, RESOURCE_KEY);

    final String queryPredicate = format("key=\"%s\"", RESOURCE_KEY);

    final PagedQueryResult<Type> typeQueryResult =
        targetClient
            .execute(TypeQuery.of().withPredicates(QueryPredicate.of(queryPredicate)))
            .toCompletableFuture()
            .join();

    assertThat(typeQueryResult.getResults())
        .hasSize(1)
        .singleElement()
        .satisfies(type -> assertThat(type.getKey()).isEqualTo(RESOURCE_KEY));

    final PagedQueryResult<TaxCategory> taxCategoryQueryResult =
        targetClient
            .execute(TaxCategoryQuery.of().withPredicates(QueryPredicate.of(queryPredicate)))
            .toCompletableFuture()
            .join();

    assertThat(taxCategoryQueryResult.getResults())
        .hasSize(1)
        .singleElement()
        .satisfies(taxCategory -> assertThat(taxCategory.getKey()).isEqualTo(RESOURCE_KEY));

    final PagedQueryResult<InventoryEntry> inventoryEntryQueryResult =
        targetClient
            .execute(
                InventoryEntryQuery.of()
                    .withPredicates(QueryPredicate.of(format("sku=\"%s\"", RESOURCE_KEY))))
            .toCompletableFuture()
            .join();

    assertThat(inventoryEntryQueryResult.getResults())
        .hasSize(1)
        .singleElement()
        .satisfies(inventoryEntry -> assertThat(inventoryEntry.getSku()).isEqualTo(RESOURCE_KEY));

    final PagedQueryResult<CartDiscount> cartDiscountPagedQueryResult =
        targetClient
            .execute(
                CartDiscountQuery.of()
                    .withPredicates(queryModel -> queryModel.key().is(RESOURCE_KEY)))
            .toCompletableFuture()
            .join();

    assertThat(cartDiscountPagedQueryResult.getResults())
        .hasSize(1)
        .singleElement()
        .satisfies(cartDiscount -> assertThat(cartDiscount.getKey()).isEqualTo(RESOURCE_KEY));

    final PagedQueryResult<State> statePagedQueryResult =
        targetClient
            .execute(
                StateQuery.of().withPredicates(queryModel -> queryModel.key().is(RESOURCE_KEY)))
            .toCompletableFuture()
            .join();

    assertThat(statePagedQueryResult.getResults())
        .hasSize(1)
        .singleElement()
        .satisfies(state -> assertThat(state.getKey()).isEqualTo(RESOURCE_KEY));

    final PagedQueryResult<CustomObject<JsonNode>> customObjectPagedQueryResult =
        targetClient
            .execute(
                CustomObjectQuery.ofJsonNode()
                    .withPredicates(
                        queryModel ->
                            queryModel
                                .key()
                                .is(RESOURCE_KEY)
                                .and(queryModel.container().is(RESOURCE_KEY))))
            .toCompletableFuture()
            .join();

    assertThat(customObjectPagedQueryResult.getResults())
        .hasSize(1)
        .singleElement()
        .satisfies(customObject -> assertThat(customObject.getKey()).isEqualTo(RESOURCE_KEY));

    final PagedQueryResult<Customer> customerPagedQueryResult =
        targetClient
            .execute(
                CustomerQuery.of().withPredicates(queryModel -> queryModel.key().is(RESOURCE_KEY)))
            .toCompletableFuture()
            .join();
    assertThat(customerPagedQueryResult.getResults()).hasSize(1);

    final PagedQueryResult<ShoppingList> shoppingListPagedQueryResult =
        targetClient
            .execute(
                ShoppingListQuery.of()
                    .withPredicates(queryModel -> queryModel.key().is(RESOURCE_KEY)))
            .toCompletableFuture()
            .join();
    assertThat(shoppingListPagedQueryResult.getResults()).hasSize(1);
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
