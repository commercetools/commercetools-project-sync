package com.commercetools.project.sync;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.project.sync.util.CtpClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.project.sync.util.CtpClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.project.sync.util.IntegrationTestUtils.cleanUpProjects;
import static com.commercetools.project.sync.util.IntegrationTestUtils.createITSyncerFactory;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.Image;
import com.commercetools.api.models.common.ImageBuilder;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductDraftBuilder;
import com.commercetools.api.models.product.ProductPagedQueryResponse;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.product.ProductVariantDraftBuilder;
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

class ProductSyncWithImagesSyncIT {
  private static final TestLogger cliRunnerTestLogger =
      TestLoggerFactory.getTestLogger(CliRunner.class);

  private static final String MAIN_PRODUCT_TYPE_KEY = "main-product-type";
  private static final String MAIN_PRODUCT_MASTER_VARIANT_KEY = "main-product-master-variant-key";
  private static final String MAIN_PRODUCT_KEY = "product-images";

  @BeforeEach
  void setup() {
    cliRunnerTestLogger.clearAll();
    final Image image =
        new ImageBuilder()
            .label("image")
            .dimensions(imageDimensionsBuilder -> imageDimensionsBuilder.w(100).h(100))
            .url("http://image.com")
            .build();
    setupProjectData(CTP_SOURCE_CLIENT, image);
    setupProjectData(CTP_TARGET_CLIENT, null);
  }

  static void setupProjectData(@Nonnull final ProjectApiRoot ctpClient, final Image image) {
    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of()
            .key(MAIN_PRODUCT_TYPE_KEY)
            .name(MAIN_PRODUCT_TYPE_KEY)
            .description("a productType for t-shirts")
            .build();

    final ProductType productType =
        ctpClient.productTypes().post(productTypeDraft).executeBlocking().getBody();

    ProductVariantDraftBuilder productVariantDraftBuilder =
        ProductVariantDraftBuilder.of()
            .key(MAIN_PRODUCT_MASTER_VARIANT_KEY)
            .sku(MAIN_PRODUCT_MASTER_VARIANT_KEY);
    if (image != null) {
      productVariantDraftBuilder = productVariantDraftBuilder.images(image);
    }

    final ProductVariantDraft masterVariant = productVariantDraftBuilder.build();

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
  void run_WhenTargetProductHasNoImage_ShouldGetImageFromSource() {
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
              assertThat(product.getMasterData().getStaged().getMasterVariant().getImages())
                  .hasSize(1)
                  .singleElement()
                  .satisfies(
                      img -> {
                        assertThat(img.getUrl()).isEqualTo("http://image.com");
                        assertThat(img.getLabel()).isEqualTo("image");
                      });
            });
  }
}
