package com.commercetools.project.sync;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.project.sync.util.CtpClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.project.sync.util.CtpClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.project.sync.util.IntegrationTestUtils.cleanUpProjects;
import static com.commercetools.project.sync.util.IntegrationTestUtils.createITSyncerFactory;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductDraftBuilder;
import com.commercetools.api.models.product.ProductPagedQueryResponse;
import com.commercetools.api.models.product.ProductVariant;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.product.ProductVariantDraftBuilder;
import com.commercetools.api.models.product_type.AttributeConstraintEnum;
import com.commercetools.api.models.product_type.AttributeTypeBuilder;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypeDraftBuilder;
import com.github.valfirst.slf4jtest.TestLogger;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.org.lidalia.slf4jext.Level;

class ProductSyncWithMasterVariantSwitchIT {
  private static final TestLogger cliRunnerTestLogger =
      TestLoggerFactory.getTestLogger(CliRunner.class);

  private static final String MAIN_PRODUCT_TYPE_KEY = "main-product-type";
  private static final String MAIN_PRODUCT_KEY = "product-with-references";

  @BeforeEach
  void setup() {
    cliRunnerTestLogger.clearAll();

    setupProjectData(CTP_SOURCE_CLIENT, "Source");
    setupProjectData(CTP_TARGET_CLIENT, "Target");
  }

  static void setupProjectData(
      @Nonnull final ProjectApiRoot ctpClient, @Nonnull final String variantSuffix) {
    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of()
            .name(MAIN_PRODUCT_TYPE_KEY)
            .key(MAIN_PRODUCT_TYPE_KEY)
            .description("a productType for t-shirts")
            .addAttributes(
                attributeDefinitionDraftBuilder ->
                    attributeDefinitionDraftBuilder
                        .type(AttributeTypeBuilder::textBuilder)
                        .name("test")
                        .label(ofEnglish("test"))
                        .isSearchable(false)
                        .isRequired(false)
                        .attributeConstraint(AttributeConstraintEnum.SAME_FOR_ALL)
                        .build())
            .build();

    final ProductType productType =
        ctpClient.productTypes().post(productTypeDraft).executeBlocking().getBody();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .key("key" + variantSuffix)
            .sku("sku" + variantSuffix)
            .addAttributes(
                attributeBuilder ->
                    attributeBuilder.name("test").value("test" + variantSuffix).build())
            .build();

    final ProductVariantDraft variant1 =
        ProductVariantDraftBuilder.of()
            .key("key" + variantSuffix + "Variant1")
            .sku("sku" + variantSuffix + "Variant1")
            .addAttributes(
                attributeBuilder ->
                    attributeBuilder.name("test").value("test" + variantSuffix).build())
            .build();

    final ProductDraft draft =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish(MAIN_PRODUCT_KEY))
            .slug(ofEnglish(MAIN_PRODUCT_KEY))
            .masterVariant(masterVariant)
            .variants(variant1)
            .key(MAIN_PRODUCT_KEY)
            .build();

    ctpClient.products().post(draft).executeBlocking();
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

    final ProductPagedQueryResponse productPagedQueryResponse =
        CTP_TARGET_CLIENT.products().get().execute().toCompletableFuture().join().getBody();

    assertThat(productPagedQueryResponse.getResults())
        .hasSize(1)
        .singleElement()
        .satisfies(
            product -> {
              final ProductVariant targetMasterVariant =
                  product.getMasterData().getStaged().getMasterVariant();
              final ProductVariant targetVariant1 =
                  product.getMasterData().getStaged().getVariants().get(0);
              assertThat(targetMasterVariant.getSku()).isEqualTo("skuSource");
              assertThat(targetMasterVariant.getKey()).isEqualTo("keySource");
              assertThat(targetMasterVariant.getAttributes().get(0).getValue())
                  .isEqualTo("testSource");

              assertThat(targetVariant1.getSku()).isEqualTo("skuSourceVariant1");
              assertThat(targetVariant1.getKey()).isEqualTo("keySourceVariant1");
              assertThat(targetVariant1.getAttributes().get(0).getValue()).isEqualTo("testSource");
            });
  }
}
