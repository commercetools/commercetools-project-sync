package com.commercetools.project.sync;

import static com.commercetools.project.sync.util.IntegrationTestUtils.cleanUpProjects;
import static com.commercetools.project.sync.util.IntegrationTestUtils.createITSyncerFactory;
import static com.commercetools.project.sync.util.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.project.sync.util.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.neovisionaries.i18n.CountryCode.DE;
import static io.sphere.sdk.models.DefaultCurrencyUnits.EUR;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.productdiscounts.*;
import io.sphere.sdk.productdiscounts.commands.ProductDiscountCreateCommand;
import io.sphere.sdk.products.*;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
import io.sphere.sdk.queries.PagedQueryResult;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.money.CurrencyUnit;
import org.javamoney.moneta.FastMoney;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

// This will suppress MoreThanOneLogger warnings in this class
@SuppressWarnings("PMD.MoreThanOneLogger")
class ProductSyncWithDiscountedPrice {
  private static final TestLogger cliRunnerTestLogger =
      TestLoggerFactory.getTestLogger(CliRunner.class);

  private static final String MAIN_PRODUCT_TYPE_KEY = "main-product-type";
  private static final String MAIN_PRODUCT_MASTER_VARIANT_KEY = "main-product-master-variant-key";
  private static final String MAIN_PRODUCT_KEY = "product-with-references";
  private static final FastMoney TEN_EUR = FastMoney.of(10, EUR);

  @BeforeEach
  void setup() {
    cliRunnerTestLogger.clearAll();

    cleanUpProjects(CTP_SOURCE_CLIENT, CTP_TARGET_CLIENT);

    ProductDiscountDraft productDiscountDraft =
        ProductDiscountDraftBuilder.of()
            .value(ProductDiscountValue.ofExternal())
            .name(ofEnglish("testProductDiscount"))
            .predicate("1=1")
            .sortOrder("0.9")
            .isActive(true)
            .build();
    ProductDiscount productDiscount =
        CTP_TARGET_CLIENT
            .execute(ProductDiscountCreateCommand.of(productDiscountDraft))
            .toCompletableFuture()
            .join();
    setupProjectData(CTP_SOURCE_CLIENT, null);
    setupProjectData(CTP_TARGET_CLIENT, productDiscount.getId());
  }

  static void setupProjectData(@Nonnull final SphereClient sphereClient, String productDiscountId) {
    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of(
                MAIN_PRODUCT_TYPE_KEY,
                MAIN_PRODUCT_TYPE_KEY,
                "a productType for t-shirts",
                emptyList())
            .build();

    final ProductType productType =
        sphereClient
            .execute(ProductTypeCreateCommand.of(productTypeDraft))
            .toCompletableFuture()
            .join();

    final PriceDraft priceDraft =
        PriceDraftBuilder.of(
                getPriceDraft(BigDecimal.valueOf(222), EUR, DE, null, null, productDiscountId))
            .build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .key(MAIN_PRODUCT_MASTER_VARIANT_KEY)
            .sku(MAIN_PRODUCT_MASTER_VARIANT_KEY)
            .prices(priceDraft)
            .build();

    final ProductDraft draft =
        ProductDraftBuilder.of(
                productType,
                ofEnglish(MAIN_PRODUCT_KEY),
                ofEnglish(MAIN_PRODUCT_KEY),
                masterVariant)
            .key(MAIN_PRODUCT_KEY)
            .build();

    sphereClient.execute(ProductCreateCommand.of(draft)).toCompletableFuture().join();
  }

  @AfterAll
  static void tearDownSuite() {
    cleanUpProjects(CTP_SOURCE_CLIENT, CTP_TARGET_CLIENT);
  }

  @Test
  void run_WhenTargetProductHasDiscountedPrice_ShouldNotRemoveIt() {
    // test
    CliRunner.of()
        .run(new String[] {"-s", "all", "-r", "runnerName", "-f"}, createITSyncerFactory());

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
              final ProductVariant stagedMasterVariant =
                  product.getMasterData().getStaged().getMasterVariant();
              assertThat(stagedMasterVariant.getPrices())
                  .satisfies(
                      prices -> {
                        Price price = prices.get(0);
                        assertThat(price.getDiscounted()).isNotNull();
                        assertThat(price.getDiscounted().getValue()).isEqualTo(TEN_EUR);
                      });
            });
  }

  @Nonnull
  public static PriceDraft getPriceDraft(
      @Nonnull final BigDecimal value,
      @Nonnull final CurrencyUnit currencyUnits,
      @Nullable final CountryCode countryCode,
      @Nullable final ZonedDateTime validFrom,
      @Nullable final ZonedDateTime validUntil,
      @Nullable final String productDiscountReferenceId) {
    DiscountedPrice discounted = null;
    if (productDiscountReferenceId != null) {
      discounted =
          DiscountedPrice.of(TEN_EUR, Reference.of("product-discount", productDiscountReferenceId));
    }
    return PriceDraftBuilder.of(Price.of(value, currencyUnits))
        .country(countryCode)
        .validFrom(validFrom)
        .validUntil(validUntil)
        .discounted(discounted)
        .build();
  }
}
