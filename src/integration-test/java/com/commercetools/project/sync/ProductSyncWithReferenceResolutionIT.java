package com.commercetools.project.sync;

import static com.commercetools.project.sync.util.IntegrationTestUtils.assertCategoryExists;
import static com.commercetools.project.sync.util.IntegrationTestUtils.assertProductExists;
import static com.commercetools.project.sync.util.IntegrationTestUtils.assertProductTypeExists;
import static com.commercetools.project.sync.util.IntegrationTestUtils.cleanUpProjects;
import static com.commercetools.project.sync.util.IntegrationTestUtils.createITSyncerFactory;
import static com.commercetools.project.sync.util.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.project.sync.util.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.neovisionaries.i18n.CountryCode.DE;
import static io.sphere.sdk.models.DefaultCurrencyUnits.EUR;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.project.sync.product.ProductSyncer;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelDraft;
import io.sphere.sdk.channels.ChannelDraftBuilder;
import io.sphere.sdk.channels.ChannelRole;
import io.sphere.sdk.channels.commands.ChannelCreateCommand;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.customergroups.CustomerGroupDraft;
import io.sphere.sdk.customergroups.CustomerGroupDraftBuilder;
import io.sphere.sdk.customergroups.commands.CustomerGroupCreateCommand;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.AssetDraftBuilder;
import io.sphere.sdk.models.AssetSourceBuilder;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.models.TextInputHint;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.PriceDraftBuilder;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.queries.QueryPredicate;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.StateDraftBuilder;
import io.sphere.sdk.states.StateType;
import io.sphere.sdk.states.commands.StateCreateCommand;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import io.sphere.sdk.taxcategories.TaxCategoryDraftBuilder;
import io.sphere.sdk.taxcategories.TaxRateDraft;
import io.sphere.sdk.taxcategories.TaxRateDraftBuilder;
import io.sphere.sdk.taxcategories.commands.TaxCategoryCreateCommand;
import io.sphere.sdk.taxcategories.queries.TaxCategoryQuery;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.FieldDefinition;
import io.sphere.sdk.types.ResourceTypeIdsSetBuilder;
import io.sphere.sdk.types.StringFieldType;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.TypeDraftBuilder;
import io.sphere.sdk.types.commands.TypeCreateCommand;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.money.CurrencyUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

public class ProductSyncWithReferenceResolutionIT {

  private static final TestLogger syncerTestLogger = TestLoggerFactory.getTestLogger(Syncer.class);
  private static final TestLogger cliRunnerTestLogger =
      TestLoggerFactory.getTestLogger(CliRunner.class);
  private static final TestLogger productSyncerTestLogger =
      TestLoggerFactory.getTestLogger(ProductSyncer.class);
  private static final String RESOURCE_KEY = "foo";
  private static final String TYPE_KEY = "typeKey";

  @BeforeEach
  void setup() {
    syncerTestLogger.clearAll();
    cliRunnerTestLogger.clearAll();
    productSyncerTestLogger.clearAll();
    cleanUpProjects(CTP_SOURCE_CLIENT, CTP_TARGET_CLIENT);
    setupSourceProjectData(CTP_SOURCE_CLIENT);
  }

  private void setupSourceProjectData(SphereClient sourceProjectClient) {
    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of(
                RESOURCE_KEY, "sample-product-type", "a productType for t-shirts", emptyList())
            .build();

    final ProductType productType =
        sourceProjectClient
            .execute(ProductTypeCreateCommand.of(productTypeDraft))
            .toCompletableFuture()
            .join();

    final FieldDefinition FIELD_DEFINITION_1 =
        FieldDefinition.of(
            StringFieldType.of(),
            "field_name_1",
            LocalizedString.ofEnglish("label_1"),
            false,
            TextInputHint.SINGLE_LINE);

    final TypeDraft typeDraft =
        TypeDraftBuilder.of(
                TYPE_KEY,
                LocalizedString.ofEnglish("name_1"),
                ResourceTypeIdsSetBuilder.of().addCategories().addPrices().addAssets().build())
            .description(LocalizedString.ofEnglish("description_1"))
            .fieldDefinitions(Arrays.asList(FIELD_DEFINITION_1))
            .build();

    final Type type =
        sourceProjectClient.execute(TypeCreateCommand.of(typeDraft)).toCompletableFuture().join();

    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of(ofEnglish("t-shirts"), ofEnglish("t-shirts"))
            .key(RESOURCE_KEY)
            .build();

    sourceProjectClient
        .execute(CategoryCreateCommand.of(categoryDraft))
        .toCompletableFuture()
        .join();

    final StateDraft stateDraft =
        StateDraftBuilder.of(RESOURCE_KEY, StateType.PRODUCT_STATE)
            .roles(Collections.emptySet())
            .description(ofEnglish("State 1"))
            .name(ofEnglish("State 1"))
            .initial(true)
            .transitions(Collections.emptySet())
            .build();
    final State state =
        sourceProjectClient.execute(StateCreateCommand.of(stateDraft)).toCompletableFuture().join();

    final TaxRateDraft taxRateDraft =
        TaxRateDraftBuilder.of("Tax-Rate-Name-1", 0.3, false, CountryCode.DE).build();

    final TaxCategoryDraft taxCategoryDraft =
        TaxCategoryDraftBuilder.of(
                "Tax-Category-Name-1", singletonList(taxRateDraft), "Tax-Category-Description-1")
            .key(RESOURCE_KEY)
            .build();

    final TaxCategory taxCategory =
        sourceProjectClient
            .execute(TaxCategoryCreateCommand.of(taxCategoryDraft))
            .toCompletableFuture()
            .join();

    final CustomerGroupDraft customerGroupDraft =
        CustomerGroupDraftBuilder.of("customerGroupName").key("customerGroupKey").build();

    CustomerGroup customerGroup =
        sourceProjectClient
            .execute(CustomerGroupCreateCommand.of(customerGroupDraft))
            .toCompletableFuture()
            .join();

    CTP_TARGET_CLIENT
        .execute(CustomerGroupCreateCommand.of(customerGroupDraft))
        .toCompletableFuture()
        .join();

    CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraft.ofTypeKeyAndJson(type.getKey(), emptyMap());

    final ChannelDraft draft =
        ChannelDraftBuilder.of("channelKey").roles(singleton(ChannelRole.INVENTORY_SUPPLY)).build();
    sourceProjectClient.execute(ChannelCreateCommand.of(draft)).toCompletableFuture().join();
    CTP_TARGET_CLIENT.execute(ChannelCreateCommand.of(draft)).toCompletableFuture().join();

    final PriceDraft priceBuilder =
        PriceDraftBuilder.of(
                getPriceDraft(
                    BigDecimal.valueOf(222),
                    EUR,
                    DE,
                    customerGroup.getId(),
                    null,
                    null,
                    null,
                    null))
            .customerGroup(customerGroup)
            .custom(customFieldsDraft)
            .channel(ResourceIdentifier.ofKey("channelKey"))
            .build();

    final AssetDraft assetDraft =
        AssetDraftBuilder.of(emptyList(), LocalizedString.ofEnglish("assetName"))
            .key("assetKey")
            .sources(singletonList(AssetSourceBuilder.ofUri("sourceUri").build()))
            .custom(customFieldsDraft)
            .build();

    final ProductVariantDraft variantDraft1 =
        ProductVariantDraftBuilder.of()
            .key("variantKey")
            .sku("sku1")
            .prices(priceBuilder)
            .assets(asList(assetDraft))
            .build();

    final ProductDraft productDraft =
        ProductDraftBuilder.of(
                productType,
                ofEnglish("V-neck Tee"),
                ofEnglish("v-neck-tee"),
                ProductVariantDraftBuilder.of().key(RESOURCE_KEY).sku(RESOURCE_KEY).build())
            .state(State.referenceOfId(state.getId()))
            .taxCategory(TaxCategory.referenceOfId(taxCategory.getId()))
            .productType(ProductType.referenceOfId(productType.getId()))
            .variants(asList(variantDraft1))
            .key(RESOURCE_KEY)
            .publish(true)
            .build();

    sourceProjectClient.execute(ProductCreateCommand.of(productDraft)).toCompletableFuture().join();
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
      @Nonnull final SphereClient targetClient) {

    assertProductTypeExists(targetClient, RESOURCE_KEY);
    assertCategoryExists(targetClient, RESOURCE_KEY);
    assertProductExists(targetClient, RESOURCE_KEY, RESOURCE_KEY, RESOURCE_KEY);

    final String queryPredicate = format("key=\"%s\"", RESOURCE_KEY);

    final PagedQueryResult<TaxCategory> taxCategoryQueryResult =
        targetClient
            .execute(TaxCategoryQuery.of().withPredicates(QueryPredicate.of(queryPredicate)))
            .toCompletableFuture()
            .join();
    assertThat(taxCategoryQueryResult.getResults())
        .hasSize(1)
        .singleElement()
        .satisfies(taxCategory -> assertThat(taxCategory.getKey()).isEqualTo(RESOURCE_KEY));
  }

  @Nonnull
  public static PriceDraft getPriceDraft(
      @Nonnull final BigDecimal value,
      @Nonnull final CurrencyUnit currencyUnits,
      @Nullable final CountryCode countryCode,
      @Nullable final String customerGroupId,
      @Nullable final ZonedDateTime validFrom,
      @Nullable final ZonedDateTime validUntil,
      @Nullable final String channelId,
      @Nullable final CustomFieldsDraft customFieldsDraft) {
    return PriceDraftBuilder.of(Price.of(value, currencyUnits))
        .country(countryCode)
        .customerGroup(
            ofNullable(customerGroupId).map(ResourceIdentifier::<CustomerGroup>ofId).orElse(null))
        .validFrom(validFrom)
        .validUntil(validUntil)
        .channel(ofNullable(channelId).map(ResourceIdentifier::<Channel>ofId).orElse(null))
        .custom(customFieldsDraft)
        .build();
  }
}
