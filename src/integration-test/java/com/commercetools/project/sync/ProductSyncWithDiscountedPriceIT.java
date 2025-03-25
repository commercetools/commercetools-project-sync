package com.commercetools.project.sync;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.project.sync.util.CtpClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.project.sync.util.CtpClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.project.sync.util.IntegrationTestUtils.cleanUpProjects;
import static com.commercetools.project.sync.util.IntegrationTestUtils.createITSyncerFactory;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.CentPrecisionMoneyBuilder;
import com.commercetools.api.models.common.DiscountedPriceDraft;
import com.commercetools.api.models.common.DiscountedPriceDraftBuilder;
import com.commercetools.api.models.common.Price;
import com.commercetools.api.models.common.PriceDraft;
import com.commercetools.api.models.common.PriceDraftBuilder;
import com.commercetools.api.models.common.TypedMoney;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductDraftBuilder;
import com.commercetools.api.models.product.ProductPagedQueryResponse;
import com.commercetools.api.models.product.ProductVariant;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.product.ProductVariantDraftBuilder;
import com.commercetools.api.models.product_discount.ProductDiscount;
import com.commercetools.api.models.product_discount.ProductDiscountDraft;
import com.commercetools.api.models.product_discount.ProductDiscountDraftBuilder;
import com.commercetools.api.models.product_discount.ProductDiscountValueExternalDraftBuilder;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypeDraftBuilder;
import com.github.valfirst.slf4jtest.TestLogger;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import java.time.ZonedDateTime;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

class ProductSyncWithDiscountedPriceIT {
  private static final TestLogger cliRunnerTestLogger =
      TestLoggerFactory.getTestLogger(CliRunner.class);

  private static final String MAIN_PRODUCT_TYPE_KEY = "main-product-type";
  private static final String MAIN_PRODUCT_MASTER_VARIANT_KEY = "main-product-master-variant-key";
  private static final String MAIN_PRODUCT_KEY = "product-with-references";
  private static final TypedMoney TEN_EUR =
      CentPrecisionMoneyBuilder.of()
          .centAmount(1000L)
          .currencyCode("EUR")
          .fractionDigits(2)
          .build();

  @BeforeEach
  void setup() {
    cliRunnerTestLogger.clearAll();
    ProductDiscountDraft productDiscountDraft =
        ProductDiscountDraftBuilder.of()
            .value(ProductDiscountValueExternalDraftBuilder.of().build())
            .name(ofEnglish("testProductDiscount"))
            .predicate("1=1")
            .sortOrder("0.9")
            .isActive(true)
            .build();
    ProductDiscount productDiscount =
        CTP_TARGET_CLIENT.productDiscounts().post(productDiscountDraft).executeBlocking().getBody();
    setupProjectData(CTP_SOURCE_CLIENT, null);
    setupProjectData(CTP_TARGET_CLIENT, productDiscount.getId());
  }

  static void setupProjectData(
      @Nonnull final ProjectApiRoot ctpClient, final String productDiscountId) {
    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of()
            .key(MAIN_PRODUCT_TYPE_KEY)
            .name(MAIN_PRODUCT_TYPE_KEY)
            .description("a productType for t-shirts")
            .build();

    final ProductType productType =
        ctpClient.productTypes().post(productTypeDraft).executeBlocking().getBody();

    final PriceDraft priceDraft =
        PriceDraftBuilder.of(getPriceDraft(22200L, "EUR", "DE", null, null, productDiscountId))
            .build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .key(MAIN_PRODUCT_MASTER_VARIANT_KEY)
            .sku(MAIN_PRODUCT_MASTER_VARIANT_KEY)
            .prices(priceDraft)
            .build();

    final ProductDraft draft =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish(MAIN_PRODUCT_KEY))
            .slug(ofEnglish(MAIN_PRODUCT_KEY))
            .masterVariant(masterVariant)
            .key(MAIN_PRODUCT_KEY)
            .build();

    ctpClient.products().post(draft).executeBlocking();
  }

  @AfterAll
  static void tearDownSuite() {
    cleanUpProjects(CTP_SOURCE_CLIENT, CTP_TARGET_CLIENT);
  }

  @Test
  void run_WhenTargetProductHasDiscountedPrice_ShouldNotRemoveIt() {
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
              assertThat(stagedMasterVariant.getPrices())
                  .satisfies(
                      prices -> {
                        final Price price = prices.get(0);
                        assertThat(price.getDiscounted()).isNotNull();
                        assertThat(price.getDiscounted().getValue()).isEqualTo(TEN_EUR);
                      });
            });
  }

  @Nonnull
  public static PriceDraft getPriceDraft(
      @Nonnull final Long value,
      @Nonnull final String currencyCode,
      @Nullable final String countryCode,
      @Nullable final ZonedDateTime validFrom,
      @Nullable final ZonedDateTime validUntil,
      @Nullable final String productDiscountReferenceId) {
    DiscountedPriceDraft discounted = null;
    if (productDiscountReferenceId != null) {
      discounted =
          DiscountedPriceDraftBuilder.of()
              .value(TEN_EUR)
              .discount(
                  productDiscountReferenceBuilder ->
                      productDiscountReferenceBuilder.id(productDiscountReferenceId))
              .build();
    }
    return PriceDraftBuilder.of()
        .value(moneyBuilder -> moneyBuilder.centAmount(value).currencyCode(currencyCode))
        .country(countryCode)
        .validFrom(validFrom)
        .validUntil(validUntil)
        .discounted(discounted)
        .build();
  }
}
