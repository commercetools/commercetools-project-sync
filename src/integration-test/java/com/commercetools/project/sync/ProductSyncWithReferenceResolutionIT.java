package com.commercetools.project.sync;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.project.sync.ProductSyncWithDiscountedPriceIT.getPriceDraft;
import static com.commercetools.project.sync.util.CtpClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.project.sync.util.CtpClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.project.sync.util.IntegrationTestUtils.assertCategoryExists;
import static com.commercetools.project.sync.util.IntegrationTestUtils.assertProductExists;
import static com.commercetools.project.sync.util.IntegrationTestUtils.assertProductTypeExists;
import static com.commercetools.project.sync.util.IntegrationTestUtils.cleanUpProjects;
import static com.commercetools.project.sync.util.IntegrationTestUtils.createITSyncerFactory;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.category.CategoryDraftBuilder;
import com.commercetools.api.models.channel.ChannelDraft;
import com.commercetools.api.models.channel.ChannelDraftBuilder;
import com.commercetools.api.models.channel.ChannelRoleEnum;
import com.commercetools.api.models.common.AssetDraft;
import com.commercetools.api.models.common.AssetDraftBuilder;
import com.commercetools.api.models.common.AssetSourceBuilder;
import com.commercetools.api.models.common.PriceDraft;
import com.commercetools.api.models.common.PriceDraftBuilder;
import com.commercetools.api.models.customer_group.CustomerGroup;
import com.commercetools.api.models.customer_group.CustomerGroupDraft;
import com.commercetools.api.models.customer_group.CustomerGroupDraftBuilder;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductDraftBuilder;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.product.ProductVariantDraftBuilder;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypeDraftBuilder;
import com.commercetools.api.models.state.State;
import com.commercetools.api.models.state.StateDraft;
import com.commercetools.api.models.state.StateDraftBuilder;
import com.commercetools.api.models.state.StateTypeEnum;
import com.commercetools.api.models.tax_category.TaxCategory;
import com.commercetools.api.models.tax_category.TaxCategoryDraft;
import com.commercetools.api.models.tax_category.TaxCategoryDraftBuilder;
import com.commercetools.api.models.tax_category.TaxRateDraft;
import com.commercetools.api.models.tax_category.TaxRateDraftBuilder;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.FieldDefinition;
import com.commercetools.api.models.type.FieldDefinitionBuilder;
import com.commercetools.api.models.type.FieldTypeBuilder;
import com.commercetools.api.models.type.ResourceTypeId;
import com.commercetools.api.models.type.Type;
import com.commercetools.api.models.type.TypeDraft;
import com.commercetools.api.models.type.TypeDraftBuilder;
import com.commercetools.api.models.type.TypeTextInputHint;
import com.commercetools.project.sync.product.ProductSyncer;
import com.github.valfirst.slf4jtest.TestLogger;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.http.HttpStatusCode;
import java.util.List;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.org.lidalia.slf4jext.Level;

// This will suppress MoreThanOneLogger warnings in this class
@SuppressWarnings("PMD.MoreThanOneLogger")
public class ProductSyncWithReferenceResolutionIT {

  private static final TestLogger cliRunnerTestLogger =
      TestLoggerFactory.getTestLogger(CliRunner.class);
  private static final TestLogger productSyncerTestLogger =
      TestLoggerFactory.getTestLogger(ProductSyncer.class);
  private static final String RESOURCE_KEY = "foo";
  private static final String TYPE_KEY = "typeKey";

  @BeforeEach
  void setup() {
    cliRunnerTestLogger.clearAll();
    productSyncerTestLogger.clearAll();
    cleanUpProjects(CTP_SOURCE_CLIENT, CTP_TARGET_CLIENT);
    setupSourceProjectData(CTP_SOURCE_CLIENT);
  }

  private void setupSourceProjectData(final ProjectApiRoot sourceProjectClient) {
    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of()
            .key(RESOURCE_KEY)
            .name("sample-product-type")
            .description("a productType for t-shirts")
            .build();

    final ProductType productType =
        sourceProjectClient.productTypes().post(productTypeDraft).executeBlocking().getBody();

    final FieldDefinition FIELD_DEFINITION_1 =
        FieldDefinitionBuilder.of()
            .type(FieldTypeBuilder::stringBuilder)
            .name("field_name_1")
            .label(ofEnglish("label_1"))
            .required(false)
            .inputHint(TypeTextInputHint.SINGLE_LINE)
            .build();

    final TypeDraft typeDraft =
        TypeDraftBuilder.of()
            .key(TYPE_KEY)
            .name(ofEnglish("name_1"))
            .resourceTypeIds(
                ResourceTypeId.CATEGORY, ResourceTypeId.PRODUCT_PRICE, ResourceTypeId.ASSET)
            .description(ofEnglish("description_1"))
            .fieldDefinitions(FIELD_DEFINITION_1)
            .build();

    final Type type = sourceProjectClient.types().post(typeDraft).executeBlocking().getBody();

    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of()
            .name(ofEnglish("t-shirts"))
            .slug(ofEnglish("t-shirts"))
            .key(RESOURCE_KEY)
            .build();

    sourceProjectClient.categories().post(categoryDraft).executeBlocking();

    final StateDraft stateDraft =
        StateDraftBuilder.of()
            .key(RESOURCE_KEY)
            .type(StateTypeEnum.PRODUCT_STATE)
            .roles(List.of())
            .description(ofEnglish("State 1"))
            .name(ofEnglish("State 1"))
            .initial(true)
            .transitions(List.of())
            .build();
    final State state = sourceProjectClient.states().post(stateDraft).executeBlocking().getBody();

    final TaxRateDraft taxRateDraft =
        TaxRateDraftBuilder.of()
            .name("Tax-Rate-Name-1")
            .amount(0.3)
            .includedInPrice(false)
            .country("DE")
            .build();

    final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of()
            .name("Tax-Category-Name-1")
            .rates(taxRateDraft)
            .description("Tax-Category-Description-1")
            .key(RESOURCE_KEY)
            .build();

    final TaxCategory taxCategory =
        sourceProjectClient.taxCategories().post(taxCategoryDraft).executeBlocking().getBody();

    final CustomerGroupDraft customerGroupDraft =
        CustomerGroupDraftBuilder.of()
            .groupName("customerGroupName")
            .key("customerGroupKey")
            .build();

    final CustomerGroup customerGroup =
        sourceProjectClient.customerGroups().post(customerGroupDraft).executeBlocking().getBody();

    CTP_TARGET_CLIENT.customerGroups().post(customerGroupDraft).executeBlocking();

    CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraftBuilder.of().type(type.toResourceIdentifier()).build();

    final ChannelDraft draft =
        ChannelDraftBuilder.of().key("channelKey").roles(ChannelRoleEnum.INVENTORY_SUPPLY).build();
    sourceProjectClient.channels().post(draft).executeBlocking();
    CTP_TARGET_CLIENT.channels().post(draft).executeBlocking();

    final PriceDraft priceDraft =
        PriceDraftBuilder.of(getPriceDraft(22200L, "EUR", "DE", null, null, null))
            .customerGroup(customerGroup.toResourceIdentifier())
            .custom(customFieldsDraft)
            .channel(
                channelResourceIdentifierBuilder ->
                    channelResourceIdentifierBuilder.key("channelKey"))
            .build();

    final AssetDraft assetDraft =
        AssetDraftBuilder.of()
            .name(ofEnglish("assetName"))
            .key("assetKey")
            .sources(AssetSourceBuilder.of().uri("sourceUri").build())
            .custom(customFieldsDraft)
            .build();

    final ProductVariantDraft variantDraft1 =
        ProductVariantDraftBuilder.of()
            .key("variantKey")
            .sku("sku1")
            .prices(priceDraft)
            .assets(assetDraft)
            .build();

    final ProductDraft productDraft =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("V-Neck Tee"))
            .slug(ofEnglish("v-neck-tee"))
            .masterVariant(
                ProductVariantDraftBuilder.of().key(RESOURCE_KEY).sku(RESOURCE_KEY).build())
            .state(state.toResourceIdentifier())
            .taxCategory(taxCategory.toResourceIdentifier())
            .productType(productType.toResourceIdentifier())
            .variants(asList(variantDraft1))
            .key(RESOURCE_KEY)
            .publish(true)
            .build();

    sourceProjectClient.products().post(productDraft).executeBlocking();
  }

  @AfterAll
  static void tearDownSuite() {
    cleanUpProjects(CTP_SOURCE_CLIENT, CTP_TARGET_CLIENT);
  }

  @Test
  void run_WithSyncAsArgumentWithAllArgAsFullSync_ShouldResolveReferencesAndExecuteSyncers() {
    // test
    CliRunner.of().run(new String[] {"-s", "all", "-f"}, createITSyncerFactory());
    // assertions
    assertThat(cliRunnerTestLogger.getAllLoggingEvents())
        .allMatch(loggingEvent -> !Level.ERROR.equals(loggingEvent.getLevel()));

    assertThat(productSyncerTestLogger.getAllLoggingEvents())
        .allMatch(loggingEvent -> !Level.ERROR.equals(loggingEvent.getLevel()));

    assertAllResourcesAreSyncedToTarget(CTP_TARGET_CLIENT);
  }

  private static void assertAllResourcesAreSyncedToTarget(
      @Nonnull final ProjectApiRoot targetClient) {

    assertProductTypeExists(targetClient, RESOURCE_KEY);
    assertCategoryExists(targetClient, RESOURCE_KEY);
    assertProductExists(targetClient, RESOURCE_KEY, RESOURCE_KEY, RESOURCE_KEY);

    final ApiHttpResponse<TaxCategory> taxCategoryApiHttpResponse =
        targetClient
            .taxCategories()
            .withKey(RESOURCE_KEY)
            .get()
            .execute()
            .toCompletableFuture()
            .join();

    assertThat(taxCategoryApiHttpResponse.getStatusCode()).isEqualTo(HttpStatusCode.OK_200);
    assertThat(taxCategoryApiHttpResponse.getBody()).isNotNull();
    assertThat(taxCategoryApiHttpResponse.getBody().getKey()).isEqualTo(RESOURCE_KEY);
  }
}
