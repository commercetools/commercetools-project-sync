package com.commercetools.project.sync;

import static com.commercetools.project.sync.service.impl.CustomObjectServiceImpl.TIMESTAMP_GENERATOR_CONTAINER_POSTFIX;
import static com.commercetools.project.sync.service.impl.CustomObjectServiceImpl.TIMESTAMP_GENERATOR_KEY;
import static com.commercetools.project.sync.service.impl.CustomObjectServiceImpl.TIMESTAMP_GENERATOR_VALUE;
import static com.commercetools.project.sync.util.ClientConfigurationUtils.createClient;
import static com.commercetools.project.sync.util.IntegrationTestUtils.deleteLastSyncCustomObjects;
import static com.commercetools.project.sync.util.QueryUtils.queryAndExecute;
import static com.commercetools.project.sync.util.SphereClientUtils.CTP_SOURCE_CLIENT_CONFIG;
import static com.commercetools.project.sync.util.SphereClientUtils.CTP_TARGET_CLIENT_CONFIG;
import static com.commercetools.project.sync.util.TestUtils.assertAllSyncersLoggingEvents;
import static com.commercetools.project.sync.util.TestUtils.assertSyncerLoggingEvents;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.project.sync.model.LastSyncCustomObject;
import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.commands.CategoryDeleteCommand;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import io.sphere.sdk.inventory.commands.InventoryEntryCreateCommand;
import io.sphere.sdk.inventory.commands.InventoryEntryDeleteCommand;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.ProductDeleteCommand;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
import io.sphere.sdk.producttypes.commands.ProductTypeDeleteCommand;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.queries.QueryPredicate;
import io.sphere.sdk.types.ResourceTypeIdsSetBuilder;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.TypeDraftBuilder;
import io.sphere.sdk.types.commands.TypeCreateCommand;
import io.sphere.sdk.types.commands.TypeDeleteCommand;
import io.sphere.sdk.types.queries.TypeQuery;
import java.time.Clock;
import java.time.ZonedDateTime;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

class MOCK_RUNNER_NAME {

  private static final TestLogger syncerTestLogger = TestLoggerFactory.getTestLogger(Syncer.class);
  private static final TestLogger cliRunnerTestLogger =
      TestLoggerFactory.getTestLogger(CliRunner.class);
  private static final String RESOURCE_KEY = "foo";
  static String defaultTestRunnerName = "testRunner";

  @BeforeAll
  static void setupSuite() {
    cleanUpProjects(createClient(CTP_SOURCE_CLIENT_CONFIG), createClient(CTP_TARGET_CLIENT_CONFIG));
  }

  static void setupSourceProjectData(@Nonnull final SphereClient sourceProjectClient) {
    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of(RESOURCE_KEY, "bar", "desc", emptyList()).build();

    final ProductType productType =
        sourceProjectClient
            .execute(ProductTypeCreateCommand.of(productTypeDraft))
            .toCompletableFuture()
            .join();

    final TypeDraft typeDraft =
        TypeDraftBuilder.of(
                RESOURCE_KEY, ofEnglish("bar"), ResourceTypeIdsSetBuilder.of().addCategories())
            .build();

    sourceProjectClient.execute(TypeCreateCommand.of(typeDraft)).toCompletableFuture().join();

    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of(ofEnglish("foo"), ofEnglish("bar")).key(RESOURCE_KEY).build();

    sourceProjectClient
        .execute(CategoryCreateCommand.of(categoryDraft))
        .toCompletableFuture()
        .join();

    final ProductDraft productDraft =
        ProductDraftBuilder.of(
                productType,
                ofEnglish("foo"),
                ofEnglish("bar"),
                ProductVariantDraftBuilder.of().key(RESOURCE_KEY).sku(RESOURCE_KEY).build())
            .key(RESOURCE_KEY)
            .build();

    sourceProjectClient.execute(ProductCreateCommand.of(productDraft)).toCompletableFuture().join();

    final InventoryEntryDraft inventoryEntryDraft =
        InventoryEntryDraftBuilder.of(RESOURCE_KEY, 1L).build();

    sourceProjectClient
        .execute(InventoryEntryCreateCommand.of(inventoryEntryDraft))
        .toCompletableFuture()
        .join();
  }

  @BeforeEach
  void setup() {
    setupSourceProjectData(createClient(CTP_SOURCE_CLIENT_CONFIG));
  }

  private static void cleanUpProjects(
      @Nonnull final SphereClient sourceClient, @Nonnull final SphereClient targetClient) {

    deleteProjectData(sourceClient);
    deleteProjectData(targetClient);
    deleteLastSyncCustomObjects(targetClient, sourceClient.getConfig().getProjectKey());
  }

  private static void deleteProjectData(@Nonnull final SphereClient client) {
    queryAndExecute(client, CategoryQuery.of(), CategoryDeleteCommand::of);
    queryAndExecute(client, ProductQuery.of(), ProductDeleteCommand::of);
    queryAndExecute(client, ProductTypeQuery.of(), ProductTypeDeleteCommand::of);
    queryAndExecute(client, TypeQuery.of(), TypeDeleteCommand::of);
    queryAndExecute(client, InventoryEntryQuery.of(), InventoryEntryDeleteCommand::of);
  }

  @AfterEach
  void tearDownTest() {
    syncerTestLogger.clearAll();
    cliRunnerTestLogger.clearAll();
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

        cleanUpProjects(postSourceClient, postTargetClient);
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
                postTargetClient, "ProductTypeSync");

        final String sourceProjectKey = postSourceClient.getConfig().getProjectKey();

        assertLastSyncCustomObjectIsCorrect(
            postTargetClient,
            sourceProjectKey,
            "productTypeSync",
            ProductSyncStatistics.class,
            lastSyncTimestamp);

        cleanUpProjects(postSourceClient, postTargetClient);
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
      @Nonnull final String syncModule,
      @Nonnull final Class<? extends BaseSyncStatistics> statisticsClass,
      @Nonnull final ZonedDateTime lastSyncTimestamp) {

    final CustomObjectQuery<LastSyncCustomObject> lastSyncQuery =
        CustomObjectQuery.of(LastSyncCustomObject.class)
            .byContainer(
                format("commercetools-project-sync.%s.%s", defaultTestRunnerName, syncModule));

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
      @Nonnull final SphereClient targetClient, String syncMethodName) {

    final CustomObjectQuery<String> timestampGeneratorQuery =
        CustomObjectQuery.of(String.class)
            .byContainer(
                format(
                    "commercetools-project-sync.%s.%s.%s",
                    defaultTestRunnerName, syncMethodName, TIMESTAMP_GENERATOR_CONTAINER_POSTFIX));

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
      run_WithSyncAsArgumentWithAllArg_ShouldExecuteAllSyncersAndStoreLastSyncTimestampsAsCustomObjects() {
    // preparation
    try (final SphereClient targetClient = createClient(CTP_TARGET_CLIENT_CONFIG)) {
      try (final SphereClient sourceClient = createClient(CTP_SOURCE_CLIENT_CONFIG)) {

        final SyncerFactory syncerFactory =
            SyncerFactory.of(() -> sourceClient, () -> targetClient, Clock.systemDefaultZone());

        // test
        CliRunner.of().run(new String[] {"-s", "all", "-r", defaultTestRunnerName}, syncerFactory);
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
        assertCurrentCtpTimestampGeneratorAndGetLastModifiedAt(postTargetClient, "ProductTypeSync");

        final String sourceProjectKey = postSourceClient.getConfig().getProjectKey();

        assertLastSyncCustomObjectExists(postTargetClient, sourceProjectKey, "productSync");

        assertLastSyncCustomObjectExists(postTargetClient, sourceProjectKey, "categorySync");

        assertLastSyncCustomObjectExists(postTargetClient, sourceProjectKey, "productTypeSync");

        assertLastSyncCustomObjectExists(postTargetClient, sourceProjectKey, "typeSync");

        assertLastSyncCustomObjectExists(postTargetClient, sourceProjectKey, "inventorySync");

        cleanUpProjects(postSourceClient, postTargetClient);
      }
    }
  }

  private void assertLastSyncCustomObjectExists(
      @Nonnull final SphereClient targetClient,
      @Nonnull final String sourceProjectKey,
      @Nonnull final String syncModule) {

    final CustomObjectQuery<LastSyncCustomObject> lastSyncQuery =
        CustomObjectQuery.of(LastSyncCustomObject.class)
            .byContainer(
                format("commercetools-project-sync.%s.%s", defaultTestRunnerName, syncModule));

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

    final PagedQueryResult<ProductType> productTypeQueryResult =
        targetClient
            .execute(ProductTypeQuery.of().byKey(RESOURCE_KEY))
            .toCompletableFuture()
            .join();

    assertThat(productTypeQueryResult.getResults())
        .hasSize(1)
        .hasOnlyOneElementSatisfying(
            productType -> assertThat(productType.getKey()).isEqualTo(RESOURCE_KEY));

    final String queryPredicate = format("key=\"%s\"", RESOURCE_KEY);

    final PagedQueryResult<Type> typeQueryResult =
        targetClient
            .execute(TypeQuery.of().withPredicates(QueryPredicate.of(queryPredicate)))
            .toCompletableFuture()
            .join();

    assertThat(typeQueryResult.getResults())
        .hasSize(1)
        .hasOnlyOneElementSatisfying(type -> assertThat(type.getKey()).isEqualTo(RESOURCE_KEY));

    final PagedQueryResult<Category> categoryQueryResult =
        targetClient
            .execute(CategoryQuery.of().withPredicates(QueryPredicate.of(queryPredicate)))
            .toCompletableFuture()
            .join();

    assertThat(categoryQueryResult.getResults())
        .hasSize(1)
        .hasOnlyOneElementSatisfying(
            category -> assertThat(category.getKey()).isEqualTo(RESOURCE_KEY));

    final PagedQueryResult<Product> productQueryResult =
        targetClient.execute(ProductQuery.of()).toCompletableFuture().join();

    assertThat(productQueryResult.getResults())
        .hasSize(1)
        .hasOnlyOneElementSatisfying(
            product -> {
              assertThat(product.getKey()).isEqualTo(RESOURCE_KEY);
              final ProductVariant stagedMasterVariant =
                  product.getMasterData().getStaged().getMasterVariant();
              assertThat(stagedMasterVariant.getKey()).isEqualTo(RESOURCE_KEY);
              assertThat(stagedMasterVariant.getSku()).isEqualTo(RESOURCE_KEY);
            });

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
  }
}
