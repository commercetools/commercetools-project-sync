package com.commercetools.project.sync;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.project.sync.util.CtpClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.project.sync.util.CtpClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.project.sync.util.IntegrationTestUtils.assertProductExists;
import static com.commercetools.project.sync.util.IntegrationTestUtils.assertProductTypeExists;
import static com.commercetools.project.sync.util.IntegrationTestUtils.cleanUpProjects;
import static com.commercetools.project.sync.util.IntegrationTestUtils.createAttributeObject;
import static com.commercetools.project.sync.util.IntegrationTestUtils.createITSyncerFactory;
import static com.commercetools.project.sync.util.IntegrationTestUtils.createReferenceOfType;
import static com.commercetools.project.sync.util.TestUtils.assertSyncerLoggingEvents;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.Reference;
import com.commercetools.api.models.common.ReferenceTypeId;
import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.AttributeAccessor;
import com.commercetools.api.models.product.Product;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductDraftBuilder;
import com.commercetools.api.models.product.ProductUpdateActionBuilder;
import com.commercetools.api.models.product.ProductVariant;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.product.ProductVariantDraftBuilder;
import com.commercetools.api.models.product_type.AttributeConstraintEnum;
import com.commercetools.api.models.product_type.AttributeDefinitionDraft;
import com.commercetools.api.models.product_type.AttributeDefinitionDraftBuilder;
import com.commercetools.api.models.product_type.AttributeReferenceTypeId;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypeDraftBuilder;
import com.commercetools.project.sync.product.ProductSyncer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.valfirst.slf4jtest.TestLogger;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.org.lidalia.slf4jext.Level;

class ProductSyncWithSelfReferencesIT {

  private static final TestLogger productSyncerTestLogger =
      TestLoggerFactory.getTestLogger(ProductSyncer.class);

  private static final String MAIN_PRODUCT_TYPE_KEY = "sample-product-type";
  private static final String MAIN_PRODUCT_MASTER_VARIANT_KEY = "main-product-master-variant-key";
  private static final String MAIN_PRODUCT_KEY = "product-with-nested";
  private static final String NESTED_ATTRIBUTE_NAME = "nested-products";
  private static final String PRODUCT_REFERENCE_ATTRIBUTE_NAME = "products";

  @BeforeEach
  void setup() {
    productSyncerTestLogger.clearAll();
    cleanUpProjects(CTP_SOURCE_CLIENT, CTP_TARGET_CLIENT);
    setupSourceProjectData(CTP_SOURCE_CLIENT);
  }

  static void setupSourceProjectData(@Nonnull final ProjectApiRoot sourceProjectClient) {
    final AttributeDefinitionDraft setOfProductsAttributeDef =
        AttributeDefinitionDraftBuilder.of()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            elementBuilder ->
                                elementBuilder
                                    .referenceBuilder()
                                    .referenceTypeId(AttributeReferenceTypeId.PRODUCT)))
            .name(PRODUCT_REFERENCE_ATTRIBUTE_NAME)
            .label(ofEnglish(PRODUCT_REFERENCE_ATTRIBUTE_NAME))
            .isRequired(false)
            .isSearchable(false)
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .build();

    final ProductTypeDraft productTypeDraft =
        ProductTypeDraftBuilder.of()
            .key(MAIN_PRODUCT_TYPE_KEY)
            .name(MAIN_PRODUCT_TYPE_KEY)
            .description("a productType for t-shirts")
            .attributes(setOfProductsAttributeDef)
            .build();

    final ProductType mainProductType =
        sourceProjectClient.productTypes().post(productTypeDraft).executeBlocking().getBody();

    final AttributeDefinitionDraft nestedAttributeDefinition =
        AttributeDefinitionDraftBuilder.of()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            typeBuilder ->
                                typeBuilder
                                    .nestedBuilder()
                                    .typeReference(mainProductType.toReference())))
            .isRequired(false)
            .isSearchable(true)
            .attributeConstraint(AttributeConstraintEnum.NONE)
            .name(NESTED_ATTRIBUTE_NAME)
            .label(ofEnglish(NESTED_ATTRIBUTE_NAME))
            .build();

    sourceProjectClient
        .productTypes()
        .withKey(mainProductType.getKey())
        .post(
            productTypeUpdateBuilder ->
                productTypeUpdateBuilder
                    .version(mainProductType.getVersion())
                    .withActions(
                        productTypeUpdateActionBuilder ->
                            productTypeUpdateActionBuilder
                                .addAttributeDefinitionBuilder()
                                .attribute(nestedAttributeDefinition)))
        .executeBlocking();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .key(MAIN_PRODUCT_MASTER_VARIANT_KEY)
            .sku(MAIN_PRODUCT_MASTER_VARIANT_KEY)
            .build();

    final ProductDraft productDraft =
        ProductDraftBuilder.of()
            .productType(mainProductType.toResourceIdentifier())
            .name(ofEnglish(MAIN_PRODUCT_KEY))
            .slug(ofEnglish(MAIN_PRODUCT_KEY))
            .masterVariant(masterVariant)
            .key(MAIN_PRODUCT_KEY)
            .build();

    final Product productWithSelfReference =
        sourceProjectClient.products().post(productDraft).executeBlocking().getBody();

    final ArrayNode setAttributeValue = JsonNodeFactory.instance.arrayNode();
    final ArrayNode nestedAttributeValue = JsonNodeFactory.instance.arrayNode();

    final ObjectNode referenceOfType =
        createReferenceOfType(
            ReferenceTypeId.PRODUCT.getJsonName(), productWithSelfReference.getId());
    setAttributeValue.add(referenceOfType);
    nestedAttributeValue.add(
        createAttributeObject(PRODUCT_REFERENCE_ATTRIBUTE_NAME, setAttributeValue));

    sourceProjectClient
        .products()
        .withKey(MAIN_PRODUCT_KEY)
        .post(
            productUpdateBuilder ->
                productUpdateBuilder
                    .version(productWithSelfReference.getVersion())
                    .actions(
                        ProductUpdateActionBuilder.of()
                            .setAttributeInAllVariantsBuilder()
                            .name(PRODUCT_REFERENCE_ATTRIBUTE_NAME)
                            .value(setAttributeValue)
                            .build(),
                        ProductUpdateActionBuilder.of()
                            .setAttributeInAllVariantsBuilder()
                            .name(NESTED_ATTRIBUTE_NAME)
                            .value(JsonNodeFactory.instance.arrayNode().add(nestedAttributeValue))
                            .build()))
        .executeBlocking();
  }

  @AfterAll
  static void tearDownSuite() {
    cleanUpProjects(CTP_SOURCE_CLIENT, CTP_TARGET_CLIENT);
  }

  @Test
  void run_WithSyncAsArgumentWithAllArgAsFullSync_ShouldExecuteAllSyncers() {
    // test
    CliRunner.of()
        .run(new String[] {"-s", "all", "-r", "runnerName", "-f"}, createITSyncerFactory());

    assertThat(productSyncerTestLogger.getAllLoggingEvents())
        .allMatch(loggingEvent -> !Level.ERROR.equals(loggingEvent.getLevel()));
    final String productStatsSummary =
        "Summary: 1 product(s) were processed in total (1 created, 1 updated, "
            + "0 failed to sync and 0 product(s) with missing reference(s)).";
    assertSyncerLoggingEvents(productSyncerTestLogger, "ProductSync", productStatsSummary);

    assertAllResourcesAreSyncedToTarget(CTP_TARGET_CLIENT);
  }

  private static void assertAllResourcesAreSyncedToTarget(
      @Nonnull final ProjectApiRoot targetClient) {

    assertProductTypeExists(targetClient, MAIN_PRODUCT_TYPE_KEY);
    final Product mainProduct =
        assertProductExists(
            targetClient,
            MAIN_PRODUCT_KEY,
            MAIN_PRODUCT_MASTER_VARIANT_KEY,
            MAIN_PRODUCT_MASTER_VARIANT_KEY);

    assertThat(mainProduct.getKey()).isEqualTo(MAIN_PRODUCT_KEY);
    final ProductVariant stagedMasterVariant =
        mainProduct.getMasterData().getStaged().getMasterVariant();
    assertThat(stagedMasterVariant.getKey()).isEqualTo(MAIN_PRODUCT_MASTER_VARIANT_KEY);
    assertThat(stagedMasterVariant.getAttributes()).hasSize(2);
    assertThat(stagedMasterVariant.getAttribute(PRODUCT_REFERENCE_ATTRIBUTE_NAME))
        .satisfies(
            attribute -> {
              final List<Reference> referenceList = AttributeAccessor.asSetReference(attribute);
              assertThat(referenceList).hasSize(1);

              final Reference productReference = referenceList.get(0);
              assertThat(productReference.getTypeId().getJsonName())
                  .isEqualTo(AttributeReferenceTypeId.PRODUCT.getJsonName());
              assertThat(productReference.getId()).isEqualTo(mainProduct.getId());
            });
    assertThat(stagedMasterVariant.getAttribute(NESTED_ATTRIBUTE_NAME))
        .satisfies(
            attribute -> {
              final List<List<Attribute>> attributeAsSetNested =
                  AttributeAccessor.asSetNested(attribute);
              assertThat(attributeAsSetNested).hasSize(1);

              final List<Attribute> nestedAttributeElement = attributeAsSetNested.get(0);
              assertThat(nestedAttributeElement).hasSize(1);

              assertThat(nestedAttributeElement.get(0).getName())
                  .isEqualTo(PRODUCT_REFERENCE_ATTRIBUTE_NAME);
              assertThat(nestedAttributeElement.get(0).getValue())
                  .isExactlyInstanceOf(ArrayList.class);
              final List<Reference> productReferences =
                  AttributeAccessor.asSetReference(nestedAttributeElement.get(0));
              assertThat(productReferences)
                  .singleElement()
                  .satisfies(
                      productReference ->
                          assertThat(productReference.getId()).isEqualTo(mainProduct.getId()));
            });
  }
}
