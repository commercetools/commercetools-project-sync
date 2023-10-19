// package com.commercetools.project.sync;
//
// import static com.commercetools.project.sync.util.CtpClientUtils.CTP_SOURCE_CLIENT;
// import static com.commercetools.project.sync.util.CtpClientUtils.CTP_TARGET_CLIENT;
// import static com.commercetools.project.sync.util.IntegrationTestUtils.cleanUpProjects;
// import static com.commercetools.project.sync.util.IntegrationTestUtils.createITSyncerFactory;
// import static io.sphere.sdk.models.LocalizedString.ofEnglish;
// import static org.assertj.core.api.Assertions.assertThat;
//
// import io.sphere.sdk.client.SphereClient;
// import io.sphere.sdk.models.LocalizedString;
// import io.sphere.sdk.products.Product;
// import io.sphere.sdk.products.ProductDraft;
// import io.sphere.sdk.products.ProductDraftBuilder;
// import io.sphere.sdk.products.ProductVariant;
// import io.sphere.sdk.products.ProductVariantDraft;
// import io.sphere.sdk.products.ProductVariantDraftBuilder;
// import io.sphere.sdk.products.attributes.AttributeConstraint;
// import io.sphere.sdk.products.attributes.AttributeDefinitionDraftBuilder;
// import io.sphere.sdk.products.attributes.AttributeDraft;
// import io.sphere.sdk.products.attributes.StringAttributeType;
// import io.sphere.sdk.products.commands.ProductCreateCommand;
// import io.sphere.sdk.products.queries.ProductQuery;
// import io.sphere.sdk.producttypes.ProductType;
// import io.sphere.sdk.producttypes.ProductTypeDraft;
// import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
// import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
// import io.sphere.sdk.queries.PagedQueryResult;
// import java.util.Collections;
// import java.util.List;
// import javax.annotation.Nonnull;
// import org.junit.jupiter.api.AfterAll;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import uk.org.lidalia.slf4jext.Level;
// import uk.org.lidalia.slf4jtest.TestLogger;
// import uk.org.lidalia.slf4jtest.TestLoggerFactory;
//
//// This will suppress MoreThanOneLogger warnings in this class
// @SuppressWarnings("PMD.MoreThanOneLogger")
// class ProductSyncWithMasterVariantSwitchIT {
//  private static final TestLogger cliRunnerTestLogger =
//      TestLoggerFactory.getTestLogger(CliRunner.class);
//
//  private static final String MAIN_PRODUCT_TYPE_KEY = "main-product-type";
//  private static final String MAIN_PRODUCT_KEY = "product-with-references";
//
//  @BeforeEach
//  void setup() {
//    cliRunnerTestLogger.clearAll();
//
//    setupProjectData(CTP_SOURCE_CLIENT, "Source");
//    setupProjectData(CTP_TARGET_CLIENT, "Target");
//  }
//
//  static void setupProjectData(
//      @Nonnull final SphereClient sphereClient, @Nonnull final String variantSuffix) {
//    final ProductTypeDraft productTypeDraft =
//        ProductTypeDraftBuilder.of(
//                MAIN_PRODUCT_TYPE_KEY,
//                MAIN_PRODUCT_TYPE_KEY,
//                "a productType for t-shirts",
//                Collections.singletonList(
//                    AttributeDefinitionDraftBuilder.of(
//                            StringAttributeType.of(),
//                            "test",
//                            LocalizedString.ofEnglish("test"),
//                            false)
//                        .attributeConstraint(AttributeConstraint.SAME_FOR_ALL)
//                        .build()))
//            .build();
//
//    final ProductType productType =
//        sphereClient
//            .execute(ProductTypeCreateCommand.of(productTypeDraft))
//            .toCompletableFuture()
//            .join();
//
//    final ProductVariantDraft masterVariant =
//        ProductVariantDraftBuilder.of()
//            .key("key" + variantSuffix)
//            .sku("sku" + variantSuffix)
//            .attributes(AttributeDraft.of("test", "test" + variantSuffix))
//            .build();
//
//    final ProductVariantDraft variant1 =
//        ProductVariantDraftBuilder.of()
//            .key("key" + variantSuffix + "Variant1")
//            .sku("sku" + variantSuffix + "Variant1")
//            .attributes(AttributeDraft.of("test", "test" + variantSuffix))
//            .build();
//
//    final ProductDraft draft =
//        ProductDraftBuilder.of(
//                productType,
//                ofEnglish(MAIN_PRODUCT_KEY),
//                ofEnglish(MAIN_PRODUCT_KEY),
//                List.of(masterVariant, variant1))
//            .key(MAIN_PRODUCT_KEY)
//            .build();
//
//    sphereClient.execute(ProductCreateCommand.of(draft)).toCompletableFuture().join();
//  }
//
//  @AfterAll
//  static void tearDownSuite() {
//    cleanUpProjects(CTP_SOURCE_CLIENT, CTP_TARGET_CLIENT);
//  }
//
//  @Test
//  void run_WhenTargetProductDifferentSkusAndKeys_ShouldSyncTargetCorrectly() {
//    // test
//    CliRunner.of()
//        .run(new String[] {"-s", "products", "-r", "runnerName", "-f"}, createITSyncerFactory());
//
//    // assertions
//    assertThat(cliRunnerTestLogger.getAllLoggingEvents())
//        .allMatch(loggingEvent -> !Level.ERROR.equals(loggingEvent.getLevel()));
//
//    final PagedQueryResult<Product> productQueryResult =
//        CTP_TARGET_CLIENT.execute(ProductQuery.of()).toCompletableFuture().join();
//
//    assertThat(productQueryResult.getResults())
//        .hasSize(1)
//        .singleElement()
//        .satisfies(
//            product -> {
//              final ProductVariant targetMasterVariant =
//                  product.getMasterData().getStaged().getMasterVariant();
//              final ProductVariant targetVariant1 =
//                  product.getMasterData().getStaged().getVariants().get(0);
//              assertThat(targetMasterVariant.getSku()).isEqualTo("skuSource");
//              assertThat(targetMasterVariant.getKey()).isEqualTo("keySource");
//              assertThat(targetMasterVariant.getAttributes().get(0).getValueAsString())
//                  .isEqualTo("testSource");
//
//              assertThat(targetVariant1.getSku()).isEqualTo("skuSourceVariant1");
//              assertThat(targetVariant1.getKey()).isEqualTo("keySourceVariant1");
//              assertThat(targetVariant1.getAttributes().get(0).getValueAsString())
//                  .isEqualTo("testSource");
//            });
//  }
// }
