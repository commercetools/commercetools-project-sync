package com.commercetools.project.sync;

import static com.commercetools.project.sync.util.IntegrationTestUtils.cleanUpProjects;
import static com.commercetools.project.sync.util.IntegrationTestUtils.createITSyncerFactory;
import static com.commercetools.project.sync.util.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.project.sync.util.SphereClientUtils.CTP_TARGET_CLIENT;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static org.assertj.core.api.Assertions.assertThat;

import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.attributes.AttributeConstraint;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraftBuilder;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.products.attributes.StringAttributeType;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
import io.sphere.sdk.queries.PagedQueryResult;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

// This will suppress MoreThanOneLogger warnings in this class
@SuppressWarnings("PMD.MoreThanOneLogger")
class ProductSyncIssue447IT {
  private static final TestLogger cliRunnerTestLogger =
      TestLoggerFactory.getTestLogger(CliRunner.class);

  private static final String MAIN_PRODUCT_TYPE_KEY = "main-product-type";

  @BeforeEach
  void setup() {
    cliRunnerTestLogger.clearAll();

    setupTargetData();
    setupSourceData();
  }

  static void setupTargetData() {
    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of(
                MAIN_PRODUCT_TYPE_KEY,
                MAIN_PRODUCT_TYPE_KEY,
                "a productType for t-shirts",
                Collections.singletonList(
                    AttributeDefinitionDraftBuilder.of(
                            StringAttributeType.of(),
                            "test",
                            LocalizedString.ofEnglish("test"),
                            false)
                        .attributeConstraint(AttributeConstraint.SAME_FOR_ALL)
                        .build()))
            .build();

    final ProductType productType =
        CTP_TARGET_CLIENT
            .execute(ProductTypeCreateCommand.of(productTypeDraft))
            .toCompletableFuture()
            .join();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .key("6056101576893-37426863276221")
            .sku("10-90-10076")
            .attributes(AttributeDraft.of("test", "testTarget"))
            .build();

    final ProductVariantDraft variant1 =
        ProductVariantDraftBuilder.of()
            .sku("10-90-10073")
            .key("6056101576893-37426863341757")
            .attributes(AttributeDraft.of("test", "testTarget"))
            .build();

    final ProductVariantDraft variant2 =
        ProductVariantDraftBuilder.of()
            .sku("10-90-10077")
            .key("6056101576893-37426863374525")
            .attributes(AttributeDraft.of("test", "testTarget"))
            .build();
    final ProductVariantDraft variant3 =
        ProductVariantDraftBuilder.of()
            .sku("10-90-10078")
            .key("6056101576893-37426863472829")
            .attributes(AttributeDraft.of("test", "testTarget"))
            .build();
    final ProductVariantDraft variant4 =
        ProductVariantDraftBuilder.of()
            .sku("10-90-10083")
            .key("6056101576893-37426863571133")
            .attributes(AttributeDraft.of("test", "testTarget"))
            .build();
    final ProductVariantDraft variant5 =
        ProductVariantDraftBuilder.of()
            .sku("10-90-10079")
            .key("6056101576893-37426863603901")
            .attributes(AttributeDraft.of("test", "testTarget"))
            .build();
    final ProductVariantDraft variant6 =
        ProductVariantDraftBuilder.of()
            .sku("10-90-10084")
            .key("6056101576893-37426863669437")
            .attributes(AttributeDraft.of("test", "testTarget"))
            .build();
    final ProductVariantDraft variant7 =
        ProductVariantDraftBuilder.of()
            .sku("10-90-10085")
            .key("6056101576893-37426863767741")
            .attributes(AttributeDraft.of("test", "testTarget"))
            .build();
    final ProductVariantDraft variant8 =
        ProductVariantDraftBuilder.of()
            .sku("10-90-10086")
            .key("6056101576893-37426863866045")
            .attributes(AttributeDraft.of("test", "testTarget"))
            .build();

    final ProductDraft draft =
        ProductDraftBuilder.of(
                productType,
                ofEnglish("Sleepy Jones + Purple Pajamas"),
                ofEnglish("pajamas"),
                List.of(
                    masterVariant,
                    variant1,
                    variant2,
                    variant3,
                    variant4,
                    variant5,
                    variant6,
                    variant7,
                    variant8))
            .key("7f0d004f-51da-4497-b5fb-32432eee1208")
            .build();

    CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(draft)).toCompletableFuture().join();
  }

  static void setupSourceData() {
    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of(
                MAIN_PRODUCT_TYPE_KEY,
                MAIN_PRODUCT_TYPE_KEY,
                "a productType for t-shirts",
                Collections.singletonList(
                    AttributeDefinitionDraftBuilder.of(
                            StringAttributeType.of(),
                            "test",
                            LocalizedString.ofEnglish("test"),
                            false)
                        .attributeConstraint(AttributeConstraint.SAME_FOR_ALL)
                        .build()))
            .build();

    final ProductType productType =
        CTP_SOURCE_CLIENT
            .execute(ProductTypeCreateCommand.of(productTypeDraft))
            .toCompletableFuture()
            .join();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .key("6056101576893-37426863276221")
            .sku("10-90-10076")
            .attributes(AttributeDraft.of("test", "testSource"))
            .build();

    final ProductVariantDraft variant1 =
        ProductVariantDraftBuilder.of()
            .sku("10-90-10073")
            .key("6056101576893-37426863341757")
            .attributes(AttributeDraft.of("test", "testSource"))
            .build();

    final ProductVariantDraft variant2 =
        ProductVariantDraftBuilder.of()
            .sku("10-90-10077")
            .key("6056101576893-37426863374525")
            .attributes(AttributeDraft.of("test", "testSource"))
            .build();
    final ProductVariantDraft variant3 =
        ProductVariantDraftBuilder.of()
            .sku("10-90-10078")
            .key("6056101576893-37426863472829")
            .attributes(AttributeDraft.of("test", "testSource"))
            .build();
    final ProductVariantDraft variant4 =
        ProductVariantDraftBuilder.of()
            .sku("10-90-10083")
            .key("6056101576893-37426863571133")
            .attributes(AttributeDraft.of("test", "testSource"))
            .build();
    final ProductVariantDraft variant5 =
        ProductVariantDraftBuilder.of()
            .sku("10-90-10084")
            .key("6056101576893-37426863669437")
            .attributes(AttributeDraft.of("test", "testSource"))
            .build();
    final ProductVariantDraft variant6 =
        ProductVariantDraftBuilder.of()
            .sku("10-90-10085")
            .key("6056101576893-37426863767741")
            .attributes(AttributeDraft.of("test", "testSource"))
            .build();
    final ProductVariantDraft variant7 =
        ProductVariantDraftBuilder.of()
            .sku("10-90-10086")
            .key("6056101576893-37426863866045")
            .attributes(AttributeDraft.of("test", "testSource"))
            .build();
    final ProductVariantDraft variant8 =
        ProductVariantDraftBuilder.of()
            .sku("10-90-10072")
            .key("6056101576893-37426863243453")
            .attributes(AttributeDraft.of("test", "testSource"))
            .build();

    final ProductVariantDraft variant9 =
        ProductVariantDraftBuilder.of()
            .sku("10-90-10080")
            .key("6056101576893-37426863702205")
            .attributes(AttributeDraft.of("test", "testSource"))
            .build();
    final ProductVariantDraft variant10 =
        ProductVariantDraftBuilder.of()
            .sku("10-90-10081")
            .key("6056101576893-37426863800509")
            .attributes(AttributeDraft.of("test", "testSource"))
            .build();
    final ProductVariantDraft variant11 =
        ProductVariantDraftBuilder.of()
            .sku("10-90-10082")
            .key("6056101576893-37426863898813")
            .attributes(AttributeDraft.of("test", "testSource"))
            .build();
    final ProductVariantDraft variant12 =
        ProductVariantDraftBuilder.of()
            .sku("10-90-10071")
            .key("6056101576893-37426863145149")
            .attributes(AttributeDraft.of("test", "testSource"))
            .build();
    final ProductVariantDraft variant13 =
        ProductVariantDraftBuilder.of()
            .sku("10-90-10075")
            .key("6056101576893-37426863177917")
            .attributes(AttributeDraft.of("test", "testSource"))
            .build();
    final ProductVariantDraft variant14 =
        ProductVariantDraftBuilder.of()
            .sku("10-90-10074")
            .key("6056101576893-37426863440061")
            .attributes(AttributeDraft.of("test", "testSource"))
            .build();
    final ProductVariantDraft variant15 =
        ProductVariantDraftBuilder.of()
            .sku("10-90-10079")
            .key("6056101576893-37426863603901")
            .attributes(AttributeDraft.of("test", "testSource"))
            .build();

    final ProductDraft draft =
        ProductDraftBuilder.of(
                productType,
                ofEnglish("Sleepy Jones + Purple Pajamas"),
                ofEnglish("pajamas"),
                List.of(
                    masterVariant,
                    variant1,
                    variant2,
                    variant3,
                    variant4,
                    variant5,
                    variant6,
                    variant7,
                    variant8,
                    variant9,
                    variant10,
                    variant11,
                    variant12,
                    variant13,
                    variant14,
                    variant15))
            .description(
                ofEnglish(
                    "Love our SoftStretch Sheets? Now you can wear them – and look as cool as they feel."))
            .metaDescription(
                ofEnglish(
                    "Love our SoftStretch Sheets? Now you can wear them – and look as cool as they feel."))
            .key("7f0d004f-51da-4497-b5fb-32432eee1208")
            .build();

    CTP_SOURCE_CLIENT.execute(ProductCreateCommand.of(draft)).toCompletableFuture().join();
  }

  @AfterAll
  static void tearDownSuite() {
    cleanUpProjects(CTP_SOURCE_CLIENT, CTP_TARGET_CLIENT);
  }

  @Test
  void run_WhenTargetProductDifferentSkusAndKeys_ShouldSyncTargetCorrectly() {
    // test
    CliRunner.of()
        .run(new String[] {"-s", "products", "-r", "runnerName", "-f"}, createITSyncerFactory());

    // assertions
    assertThat(cliRunnerTestLogger.getAllLoggingEvents())
        .allMatch(loggingEvent -> !Level.ERROR.equals(loggingEvent.getLevel()));

    final PagedQueryResult<Product> productQueryResult =
        CTP_TARGET_CLIENT.execute(ProductQuery.of()).toCompletableFuture().join();

    assertThat(productQueryResult.getResults())
        .hasSize(1)
        .singleElement()
        .satisfies(
            product -> {
              final ProductVariant targetMasterVariant =
                  product.getMasterData().getStaged().getMasterVariant();
              final ProductVariant targetVariant1 =
                  product.getMasterData().getStaged().getVariants().get(0);
              assertThat(targetMasterVariant.getSku()).isEqualTo("10-90-10076");
              assertThat(targetMasterVariant.getKey()).isEqualTo("6056101576893-37426863276221");
              assertThat(targetMasterVariant.getAttributes().get(0).getValueAsString())
                  .isEqualTo("testSource");

              assertThat(targetVariant1.getSku()).isEqualTo("10-90-10073");
              assertThat(targetVariant1.getKey()).isEqualTo("6056101576893-37426863341757");
              assertThat(targetVariant1.getAttributes().get(0).getValueAsString())
                  .isEqualTo("testSource");
            });
  }
}
