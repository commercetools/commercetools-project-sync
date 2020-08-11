package com.commercetools.project.sync;

import static com.commercetools.project.sync.util.ClientConfigurationUtils.createClient;
import static com.commercetools.project.sync.util.IntegrationTestUtils.assertProductTypeExists;
import static com.commercetools.project.sync.util.IntegrationTestUtils.cleanUpProjects;
import static com.commercetools.project.sync.util.IntegrationTestUtils.createReferenceObject;
import static com.commercetools.project.sync.util.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.project.sync.util.SphereClientUtils.CTP_SOURCE_CLIENT_CONFIG;
import static com.commercetools.project.sync.util.SphereClientUtils.CTP_TARGET_CLIENT_CONFIG;
import static com.commercetools.project.sync.util.TestUtils.assertCartDiscountSyncerLoggingEvents;
import static com.commercetools.project.sync.util.TestUtils.assertCategorySyncerLoggingEvents;
import static com.commercetools.project.sync.util.TestUtils.assertInventoryEntrySyncerLoggingEvents;
import static com.commercetools.project.sync.util.TestUtils.assertProductSyncerLoggingEvents;
import static com.commercetools.project.sync.util.TestUtils.assertProductTypeSyncerLoggingEvents;
import static com.commercetools.project.sync.util.TestUtils.assertStateSyncerLoggingEvents;
import static com.commercetools.project.sync.util.TestUtils.assertTaxCategorySyncerLoggingEvents;
import static com.commercetools.project.sync.util.TestUtils.assertTypeSyncerLoggingEvents;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.sync.commons.models.WaitingToBeResolved;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.attributes.AttributeConstraint;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraftBuilder;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.products.attributes.ReferenceAttributeType;
import io.sphere.sdk.products.attributes.SetAttributeType;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.queries.QueryExecutionUtils;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

class ProductSyncWithReferencesIT {

  private static final TestLogger syncerTestLogger = TestLoggerFactory.getTestLogger(Syncer.class);
  private static final TestLogger cliRunnerTestLogger =
      TestLoggerFactory.getTestLogger(CliRunner.class);

  private static final String MAIN_PRODUCT_TYPE_KEY = "main-product-type";
  private static final String MAIN_PRODUCT_MASTER_VARIANT_KEY = "main-product-master-variant-key";
  private static final String MAIN_PRODUCT_KEY = "product-with-references";

  @BeforeEach
  void setup() {
    syncerTestLogger.clearAll();
    cliRunnerTestLogger.clearAll();
    cleanUpProjects(createClient(CTP_SOURCE_CLIENT_CONFIG), createClient(CTP_TARGET_CLIENT_CONFIG));
    setupSourceProjectData(createClient(CTP_SOURCE_CLIENT_CONFIG));
  }

  static void setupSourceProjectData(@Nonnull final SphereClient sourceProjectClient) {
    final AttributeDefinitionDraft setOfProductsAttributeDef =
        AttributeDefinitionDraftBuilder.of(
                SetAttributeType.of(ReferenceAttributeType.ofProduct()),
                "products",
                ofEnglish("products"),
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
            .attributes(singletonList(setOfProductsAttributeDef))
            .build();

    final ProductType productType =
        sourceProjectClient
            .execute(ProductTypeCreateCommand.of(productTypeDraft))
            .toCompletableFuture()
            .join();

    final ConcurrentHashMap.KeySetView<String, Boolean> productIds = ConcurrentHashMap.newKeySet();

    // create 500 products
    final CompletableFuture[] creationFutures =
        IntStream.range(0, 500)
            .mapToObj(
                index -> {
                  final ProductVariantDraft masterVariant =
                      ProductVariantDraftBuilder.of()
                          .key(format("%d-mv-key", index))
                          .sku(format("%d-mv-sku", index))
                          .build();

                  final ProductDraft draft =
                      ProductDraftBuilder.of(
                              productType,
                              ofEnglish(Integer.toString(index)),
                              ofEnglish(format("%d-slug", index)),
                              masterVariant)
                          .key(format("%d-key", index))
                          .build();
                  return CTP_SOURCE_CLIENT
                      .execute(ProductCreateCommand.of(draft))
                      .thenAccept(createdProduct -> productIds.add(createdProduct.getId()))
                      .toCompletableFuture();
                })
            .toArray(CompletableFuture[]::new);

    CompletableFuture.allOf(creationFutures).join();

    final ArrayNode setAttributeValue = JsonNodeFactory.instance.arrayNode();
    final Set<ObjectNode> productReferences =
        productIds
            .stream()
            .map(productId -> createReferenceObject(Product.referenceTypeId(), productId))
            .collect(Collectors.toSet());
    setAttributeValue.addAll(productReferences);

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .key(MAIN_PRODUCT_MASTER_VARIANT_KEY)
            .sku(MAIN_PRODUCT_MASTER_VARIANT_KEY)
            .attributes(AttributeDraft.of(setOfProductsAttributeDef.getName(), setAttributeValue))
            .build();

    final ProductDraft draft =
        ProductDraftBuilder.of(
                productType,
                ofEnglish(MAIN_PRODUCT_KEY),
                ofEnglish(MAIN_PRODUCT_KEY),
                masterVariant)
            .key(MAIN_PRODUCT_KEY)
            .build();

    sourceProjectClient.execute(ProductCreateCommand.of(draft)).toCompletableFuture().join();
  }

  @AfterAll
  static void tearDownSuite() {
    cleanUpProjects(createClient(CTP_SOURCE_CLIENT_CONFIG), createClient(CTP_TARGET_CLIENT_CONFIG));
  }

  @Test
  void run_WithAProductWith500DistinctReferences_ShouldSyncCorrectly() {
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
    try (final SphereClient postTargetClient = createClient(CTP_TARGET_CLIENT_CONFIG)) {

      // assertions
      assertThat(cliRunnerTestLogger.getAllLoggingEvents())
          .allMatch(loggingEvent -> !Level.ERROR.equals(loggingEvent.getLevel()));

      assertThat(syncerTestLogger.getAllLoggingEvents())
          .allMatch(loggingEvent -> !Level.ERROR.equals(loggingEvent.getLevel()));

      assertTypeSyncerLoggingEvents(syncerTestLogger, 0);
      assertProductTypeSyncerLoggingEvents(syncerTestLogger, 1);
      assertTaxCategorySyncerLoggingEvents(syncerTestLogger, 0);
      assertCategorySyncerLoggingEvents(syncerTestLogger, 0);
      assertProductSyncerLoggingEvents(syncerTestLogger, 501);
      assertInventoryEntrySyncerLoggingEvents(syncerTestLogger, 0);
      assertCartDiscountSyncerLoggingEvents(syncerTestLogger, 0);
      assertStateSyncerLoggingEvents(
          syncerTestLogger, 1); // 1 state is built-in and it cant be deleted

      // Every sync module is expected to have 2 logs (start and stats summary)
      assertThat(syncerTestLogger.getAllLoggingEvents()).hasSize(16);

      assertAllResourcesAreSyncedToTarget(postTargetClient);
    }
  }

  private static void assertAllResourcesAreSyncedToTarget(
      @Nonnull final SphereClient targetClient) {

    assertProductTypeExists(targetClient, MAIN_PRODUCT_TYPE_KEY);

    final List<Product> products =
        QueryExecutionUtils.queryAll(targetClient, ProductQuery.of()).toCompletableFuture().join();
    assertThat(products).hasSize(501);

    final CustomObjectQuery<WaitingToBeResolved> customObjectQuery =
        CustomObjectQuery.of(WaitingToBeResolved.class)
            .byContainer("commercetools-sync-java.UnresolvedReferencesService.productDrafts");

    final PagedQueryResult<CustomObject<WaitingToBeResolved>> queryResult =
        targetClient.execute(customObjectQuery).toCompletableFuture().join();

    assertThat(queryResult.getResults()).isEmpty();
  }
}
