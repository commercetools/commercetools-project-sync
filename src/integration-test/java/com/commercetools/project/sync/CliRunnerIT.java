package com.commercetools.project.sync;

import static com.commercetools.project.sync.service.impl.CustomObjectServiceImpl.DEFAULT_RUNNER_NAME;
import static com.commercetools.project.sync.service.impl.CustomObjectServiceImpl.TIMESTAMP_GENERATOR_KEY;
import static com.commercetools.project.sync.service.impl.CustomObjectServiceImpl.TIMESTAMP_GENERATOR_VALUE;
import static com.commercetools.project.sync.util.ClientConfigurationUtils.createClient;
import static com.commercetools.project.sync.util.IntegrationTestUtils.assertCategoryExists;
import static com.commercetools.project.sync.util.IntegrationTestUtils.assertProductExists;
import static com.commercetools.project.sync.util.IntegrationTestUtils.assertProductTypeExists;
import static com.commercetools.project.sync.util.IntegrationTestUtils.cleanUpProjects;
import static com.commercetools.project.sync.util.SphereClientUtils.CTP_SOURCE_CLIENT_CONFIG;
import static com.commercetools.project.sync.util.SphereClientUtils.CTP_TARGET_CLIENT_CONFIG;
import static com.commercetools.project.sync.util.SyncUtils.APPLICATION_DEFAULT_NAME;
import static com.commercetools.project.sync.util.TestUtils.assertAllSyncersLoggingEvents;
import static com.commercetools.project.sync.util.TestUtils.assertSyncerLoggingEvents;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.project.sync.model.response.LastSyncCustomObject;
import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.CartDiscountDraftBuilder;
import io.sphere.sdk.cartdiscounts.CartDiscountValue;
import io.sphere.sdk.cartdiscounts.ShippingCostTarget;
import io.sphere.sdk.cartdiscounts.commands.CartDiscountCreateCommand;
import io.sphere.sdk.cartdiscounts.queries.CartDiscountQuery;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import io.sphere.sdk.inventory.commands.InventoryEntryCreateCommand;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.queries.QueryPredicate;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.StateDraftBuilder;
import io.sphere.sdk.states.StateType;
import io.sphere.sdk.states.commands.StateCreateCommand;
import io.sphere.sdk.states.queries.StateQuery;
import io.sphere.sdk.types.ResourceTypeIdsSetBuilder;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.TypeDraftBuilder;
import io.sphere.sdk.types.commands.TypeCreateCommand;
import io.sphere.sdk.types.queries.TypeQuery;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Collections;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

class CliRunnerIT {

  private static final TestLogger syncerTestLogger = TestLoggerFactory.getTestLogger(Syncer.class);
  private static final TestLogger cliRunnerTestLogger =
      TestLoggerFactory.getTestLogger(CliRunner.class);
  private static final String RESOURCE_KEY = "foo";

  @BeforeEach
  void setup() {
    syncerTestLogger.clearAll();
    cliRunnerTestLogger.clearAll();
    cleanUpProjects(createClient(CTP_SOURCE_CLIENT_CONFIG), createClient(CTP_TARGET_CLIENT_CONFIG));
    setupSourceProjectData(createClient(CTP_SOURCE_CLIENT_CONFIG));
  }

  static void setupSourceProjectData(@Nonnull final SphereClient sourceProjectClient) {
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
                ResourceTypeIdsSetBuilder.of().addCategories())
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

    sourceProjectClient.execute(TypeCreateCommand.of(typeDraft)).toCompletableFuture().join();

    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of(ofEnglish("t-shirts"), ofEnglish("t-shirts"))
            .key(RESOURCE_KEY)
            .build();

    sourceProjectClient
        .execute(CategoryCreateCommand.of(categoryDraft))
        .toCompletableFuture()
        .join();

    final ProductDraft productDraft =
        ProductDraftBuilder.of(
                productType,
                ofEnglish("V-neck Tee"),
                ofEnglish("v-neck-tee"),
                ProductVariantDraftBuilder.of().key(RESOURCE_KEY).sku(RESOURCE_KEY).build())
            .state(state)
            .key(RESOURCE_KEY)
            .build();

    sourceProjectClient.execute(ProductCreateCommand.of(productDraft)).toCompletableFuture().join();

    final InventoryEntryDraft inventoryEntryDraft =
        InventoryEntryDraftBuilder.of(RESOURCE_KEY, 1L).build();

    sourceProjectClient
        .execute(InventoryEntryCreateCommand.of(inventoryEntryDraft))
        .toCompletableFuture()
        .join();

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

    sourceProjectClient
        .execute(CartDiscountCreateCommand.of(cartDiscountDraft))
        .toCompletableFuture()
        .join();
  }

  @AfterAll
  static void tearDownSuite() {
    cleanUpProjects(createClient(CTP_SOURCE_CLIENT_CONFIG), createClient(CTP_TARGET_CLIENT_CONFIG));
  }

  @Test
  void run_WithSyncAsArgumentWithAllArg_ShouldExecuteAllSyncers() {
    // preparation
    try (final SphereClient targetClient = createClient(CTP_TARGET_CLIENT_CONFIG)) {
      try (final SphereClient sourceClient = createClient(CTP_SOURCE_CLIENT_CONFIG)) {

        final SyncerFactory syncerFactory =
            SyncerFactory.of(() -> sourceClient, () -> targetClient, Clock.systemDefaultZone());

        // test
        CliRunner.of().run(new String[] {"-s", "all"}, syncerFactory);
      }
    }

    // create clients again (for assertions and cleanup), since the run method closes the clients
    // after execution
    // is done.
    try (final SphereClient postSourceClient = createClient(CTP_SOURCE_CLIENT_CONFIG)) {
      try (final SphereClient postTargetClient = createClient(CTP_TARGET_CLIENT_CONFIG)) {
        // assertions
        assertAllSyncersLoggingEvents(syncerTestLogger, cliRunnerTestLogger, 1);

        assertAllResourcesAreSyncedToTarget(postTargetClient);
      }
    }
  }

  @Disabled(
      "Should be enabled after resolution of https://github.com/commercetools/commercetools-project-sync/issues/27")
  @Test
  void
      run_WithProductTypeSyncAsArgument_ShouldExecuteProductTypeSyncerAndStoreLastSyncTimestampsAsCustomObject() {
    // preparation
    try (final SphereClient targetClient = createClient(CTP_TARGET_CLIENT_CONFIG)) {
      try (final SphereClient sourceClient = createClient(CTP_SOURCE_CLIENT_CONFIG)) {

        final SyncerFactory syncerFactory =
            SyncerFactory.of(() -> sourceClient, () -> targetClient, Clock.systemDefaultZone());

        // test
        CliRunner.of().run(new String[] {"-s", "productTypes"}, syncerFactory);
      }
    }

    // create clients again (for assertions and cleanup), since the run method closes the clients
    // after execution
    // is done.
    try (final SphereClient postSourceClient = createClient(CTP_SOURCE_CLIENT_CONFIG)) {
      try (final SphereClient postTargetClient = createClient(CTP_TARGET_CLIENT_CONFIG)) {

        // assertions
        assertThat(syncerTestLogger.getAllLoggingEvents()).hasSize(2);

        assertSyncerLoggingEvents(
            syncerTestLogger,
            "ProductTypeSync",
            "Summary: 1 product types were processed in total (1 created, 0 updated "
                + "and 0 failed to sync).");

        assertProductTypesAreSyncedCorrectly(postTargetClient);

        final ZonedDateTime lastSyncTimestamp =
            assertCurrentCtpTimestampGeneratorAndGetLastModifiedAt(
                postTargetClient, DEFAULT_RUNNER_NAME, "ProductTypeSync");

        final String sourceProjectKey = postSourceClient.getConfig().getProjectKey();

        assertLastSyncCustomObjectIsCorrect(
            postTargetClient,
            sourceProjectKey,
            "productTypeSync",
            DEFAULT_RUNNER_NAME,
            ProductSyncStatistics.class,
            lastSyncTimestamp);
      }
    }
  }

  private static void assertProductTypesAreSyncedCorrectly(@Nonnull final SphereClient ctpClient) {

    final PagedQueryResult<ProductType> productTypeQueryResult =
        ctpClient.execute(ProductTypeQuery.of().byKey(RESOURCE_KEY)).toCompletableFuture().join();

    assertThat(productTypeQueryResult.getResults())
        .hasSize(1)
        .hasOnlyOneElementSatisfying(
            productType -> assertThat(productType.getKey()).isEqualTo(RESOURCE_KEY));
  }

  private void assertLastSyncCustomObjectIsCorrect(
      @Nonnull final SphereClient targetClient,
      @Nonnull final String sourceProjectKey,
      @Nonnull final String syncModuleName,
      @Nonnull final String syncRunnerName,
      @Nonnull final Class<? extends BaseSyncStatistics> statisticsClass,
      @Nonnull final ZonedDateTime lastSyncTimestamp) {

    final CustomObjectQuery<LastSyncCustomObject> lastSyncQuery =
        CustomObjectQuery.of(LastSyncCustomObject.class)
            .byContainer(
                format("%s.%s.%s", APPLICATION_DEFAULT_NAME, syncRunnerName, syncModuleName));

    final PagedQueryResult<CustomObject<LastSyncCustomObject>> lastSyncResult =
        targetClient.execute(lastSyncQuery).toCompletableFuture().join();

    assertThat(lastSyncResult.getResults())
        .hasSize(1)
        .hasOnlyOneElementSatisfying(
            lastSyncCustomObject -> {
              assertThat(lastSyncCustomObject.getKey()).isEqualTo(sourceProjectKey);
              assertThat(lastSyncCustomObject.getValue())
                  .satisfies(
                      value -> {
                        assertThat(value.getLastSyncStatistics()).isInstanceOf(statisticsClass);
                        assertThat(value.getLastSyncStatistics().getProcessed()).isEqualTo(1);
                        assertThat(value.getLastSyncTimestamp()).isEqualTo(lastSyncTimestamp);
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
        .hasOnlyOneElementSatisfying(
            currentCtpTimestamp -> {
              assertThat(currentCtpTimestamp.getKey()).isEqualTo(TIMESTAMP_GENERATOR_KEY);
              assertThat(currentCtpTimestamp.getValue()).isEqualTo(TIMESTAMP_GENERATOR_VALUE);
            });

    return currentCtpTimestampGeneratorResults.getResults().get(0).getLastModifiedAt();
  }

  @Test
  void
      run_WithSyncAsArgumentWithAllArgAsDeltaSync_ShouldExecuteAllSyncersAndStoreLastSyncTimestampsAsCustomObjects() {
    // preparation
    try (final SphereClient targetClient = createClient(CTP_TARGET_CLIENT_CONFIG)) {
      try (final SphereClient sourceClient = createClient(CTP_SOURCE_CLIENT_CONFIG)) {

        final SyncerFactory syncerFactory =
            SyncerFactory.of(() -> sourceClient, () -> targetClient, Clock.systemDefaultZone());

        // test
        CliRunner.of().run(new String[] {"-s", "all", "-r", "runnerName"}, syncerFactory);
      }
    }

    // create clients again (for assertions and cleanup), since the run method closes the clients
    // after execution
    // is done.
    try (final SphereClient postSourceClient = createClient(CTP_SOURCE_CLIENT_CONFIG)) {
      try (final SphereClient postTargetClient = createClient(CTP_TARGET_CLIENT_CONFIG)) {
        // assertions
        assertAllSyncersLoggingEvents(syncerTestLogger, cliRunnerTestLogger, 1);

        assertAllResourcesAreSyncedToTarget(postTargetClient);
        assertCurrentCtpTimestampGeneratorAndGetLastModifiedAt(
            postTargetClient, "runnerName", "ProductTypeSync");

        final String sourceProjectKey = postSourceClient.getConfig().getProjectKey();

        assertLastSyncCustomObjectExists(
            postTargetClient, sourceProjectKey, "productSync", "runnerName");

        assertLastSyncCustomObjectExists(
            postTargetClient, sourceProjectKey, "categorySync", "runnerName");

        assertLastSyncCustomObjectExists(
            postTargetClient, sourceProjectKey, "productTypeSync", "runnerName");

        assertLastSyncCustomObjectExists(
            postTargetClient, sourceProjectKey, "typeSync", "runnerName");

        assertLastSyncCustomObjectExists(
            postTargetClient, sourceProjectKey, "inventorySync", "runnerName");

        assertLastSyncCustomObjectExists(
            postTargetClient, sourceProjectKey, "cartDiscountSync", "runnerName");

        assertLastSyncCustomObjectExists(
                postTargetClient, sourceProjectKey, "stateSync", "runnerName");
      }
    }
  }

  @Test
  void run_WithSyncAsArgumentWithAllArgAsFullSync_ShouldExecuteAllSyncers() {
    // preparation
    try (final SphereClient targetClient = createClient(CTP_TARGET_CLIENT_CONFIG)) {
      try (final SphereClient sourceClient = createClient(CTP_SOURCE_CLIENT_CONFIG)) {

        final SyncerFactory syncerFactory =
            SyncerFactory.of(() -> sourceClient, () -> targetClient, Clock.systemDefaultZone());

        // test
        CliRunner.of().run(new String[] {"-s", "all", "-r", "runnerName", "-f"}, syncerFactory);
      }
    }

    // create clients again (for assertions and cleanup), since the run method closes the clients
    // after execution
    // is done.
    try (final SphereClient postSourceClient = createClient(CTP_SOURCE_CLIENT_CONFIG)) {
      try (final SphereClient postTargetClient = createClient(CTP_TARGET_CLIENT_CONFIG)) {
        // assertions
        assertAllSyncersLoggingEvents(syncerTestLogger, cliRunnerTestLogger, 1);

        assertAllResourcesAreSyncedToTarget(postTargetClient);
        assertCurrentCtpTimestampGeneratorDoesntExist(
            postTargetClient, "runnerName", "ProductTypeSync");
        assertNoCustomObjectExists(postTargetClient);
      }
    }
  }

  private void assertNoCustomObjectExists(@Nonnull final SphereClient targetClient) {

    final CustomObjectQuery<LastSyncCustomObject> lastSyncQuery =
        CustomObjectQuery.of(LastSyncCustomObject.class);
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

    final CustomObjectQuery<LastSyncCustomObject> lastSyncQuery =
        CustomObjectQuery.of(LastSyncCustomObject.class)
            .byContainer(format("%s.%s.%s", APPLICATION_DEFAULT_NAME, runnerName, syncModuleName));

    final PagedQueryResult<CustomObject<LastSyncCustomObject>> lastSyncResult =
        targetClient.execute(lastSyncQuery).toCompletableFuture().join();

    assertThat(lastSyncResult.getResults())
        .hasSize(1)
        .hasOnlyOneElementSatisfying(
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
        .hasOnlyOneElementSatisfying(type -> assertThat(type.getKey()).isEqualTo(RESOURCE_KEY));

    final PagedQueryResult<InventoryEntry> inventoryEntryQueryResult =
        targetClient
            .execute(
                InventoryEntryQuery.of()
                    .withPredicates(QueryPredicate.of(format("sku=\"%s\"", RESOURCE_KEY))))
            .toCompletableFuture()
            .join();

    assertThat(inventoryEntryQueryResult.getResults())
        .hasSize(1)
        .hasOnlyOneElementSatisfying(
            inventoryEntry -> assertThat(inventoryEntry.getSku()).isEqualTo(RESOURCE_KEY));

    final PagedQueryResult<CartDiscount> cartDiscountPagedQueryResult =
        targetClient
            .execute(
                CartDiscountQuery.of()
                    .withPredicates(queryModel -> queryModel.key().is(RESOURCE_KEY)))
            .toCompletableFuture()
            .join();

    assertThat(cartDiscountPagedQueryResult.getResults())
        .hasSize(1)
        .hasOnlyOneElementSatisfying(
            cartDiscount -> assertThat(cartDiscount.getKey()).isEqualTo(RESOURCE_KEY));

    final PagedQueryResult<State> statePagedQueryResult =
            targetClient
                    .execute(
                            StateQuery.of()
                                      .withPredicates(queryModel -> queryModel.key().is(RESOURCE_KEY)))
                    .toCompletableFuture()
                    .join();

    assertThat(statePagedQueryResult.getResults())
            .hasSize(1)
            .hasOnlyOneElementSatisfying(
                    state -> assertThat(state.getKey()).isEqualTo(RESOURCE_KEY));
  }
}
