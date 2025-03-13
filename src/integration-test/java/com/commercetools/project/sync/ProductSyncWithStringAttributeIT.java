package com.commercetools.project.sync;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.project.sync.util.CtpClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.project.sync.util.CtpClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.project.sync.util.IntegrationTestUtils.cleanUpProjects;
import static com.commercetools.project.sync.util.IntegrationTestUtils.createITSyncerFactory;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.CentPrecisionMoneyBuilder;
import com.commercetools.api.models.common.TypedMoney;
import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.Product;
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
import io.vrap.rmf.base.client.ApiHttpResponse;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.org.lidalia.slf4jext.Level;

class ProductSyncWithStringAttributeIT {

  private static final TestLogger cliRunnerTestLogger =
      TestLoggerFactory.getTestLogger(CliRunner.class);

  private static final String MAIN_PRODUCT_TYPE_KEY = "main-product-type";
  private static final String MAIN_PRODUCT_MASTER_VARIANT_KEY = "main-product-master-variant-key";
  private static final String MAIN_PRODUCT_KEY = "product-with-attribute-as-string";
  private static final TypedMoney TEN_EUR =
      CentPrecisionMoneyBuilder.of()
          .centAmount(1000L)
          .currencyCode("EUR")
          .fractionDigits(2)
          .build();
  private Product sourceProduct;
  private Product targetProduct;

  @BeforeEach
  void setup() {
    cliRunnerTestLogger.clearAll();
    sourceProduct = setupProjectData(CTP_SOURCE_CLIENT);
    targetProduct = setupProjectData(CTP_TARGET_CLIENT);
  }

  static Product setupProjectData(@Nonnull final ProjectApiRoot ctpClient) {
    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of()
            .key(MAIN_PRODUCT_TYPE_KEY)
            .name(MAIN_PRODUCT_TYPE_KEY)
            .description("a productType for t-shirts")
            .addAttributes(
                attributeDefinitionDraftBuilder ->
                    attributeDefinitionDraftBuilder
                        .type(AttributeTypeBuilder::textBuilder)
                        .isRequired(false)
                        .attributeConstraint(AttributeConstraintEnum.SAME_FOR_ALL)
                        .name("modelo")
                        .label(
                            localizedStringBuilder ->
                                localizedStringBuilder.addValue("en", "modelo"))
                        .build())
            .build();

    final ProductType productType =
        ctpClient.productTypes().post(productTypeDraft).executeBlocking().getBody();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .key(MAIN_PRODUCT_MASTER_VARIANT_KEY)
            .sku(MAIN_PRODUCT_MASTER_VARIANT_KEY)
            .build();

    final ProductDraft draft =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish(MAIN_PRODUCT_KEY))
            .slug(ofEnglish(MAIN_PRODUCT_KEY))
            .masterVariant(masterVariant)
            .key(MAIN_PRODUCT_KEY)
            .build();

    return ctpClient.products().post(draft).execute().thenApply(ApiHttpResponse::getBody).join();
  }

  @AfterAll
  static void tearDownSuite() {
    cleanUpProjects(CTP_SOURCE_CLIENT, CTP_TARGET_CLIENT);
  }

  @Test
  void run_WhenTargetProductHasStringAttributeThatLooksLikeDate_ShouldNotParseIt() {
    sourceProduct =
        CTP_SOURCE_CLIENT
            .products()
            .withId(sourceProduct.getId())
            .post(
                productUpdateBuilder ->
                    productUpdateBuilder
                        .version(sourceProduct.getVersion())
                        .withActions(
                            productUpdateActionBuilder ->
                                productUpdateActionBuilder
                                    .setAttributeInAllVariantsBuilder()
                                    .name("modelo")
                                    .value("2281-22-90")))
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .join();

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
              final ProductVariant stagedMasterVariant =
                  product.getMasterData().getStaged().getMasterVariant();
              Attribute modeloAttribute =
                  stagedMasterVariant.getAttributes().stream()
                      .filter(attribute -> attribute.getName().equals("modelo"))
                      .findFirst()
                      .orElse(null);
              assertThat(modeloAttribute.getValue()).isEqualTo("2281-22-90");
            });
  }
}
