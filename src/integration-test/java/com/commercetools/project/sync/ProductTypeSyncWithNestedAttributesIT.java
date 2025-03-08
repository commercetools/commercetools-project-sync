package com.commercetools.project.sync;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.project.sync.util.CtpClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.project.sync.util.CtpClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.project.sync.util.IntegrationTestUtils.assertProductTypeExists;
import static com.commercetools.project.sync.util.IntegrationTestUtils.cleanUpProjects;
import static com.commercetools.project.sync.util.IntegrationTestUtils.createITSyncerFactory;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.product_type.AttributeConstraintEnum;
import com.commercetools.api.models.product_type.AttributeDefinitionDraft;
import com.commercetools.api.models.product_type.AttributeDefinitionDraftBuilder;
import com.commercetools.api.models.product_type.AttributeNestedType;
import com.commercetools.api.models.product_type.AttributeReferenceType;
import com.commercetools.api.models.product_type.AttributeReferenceTypeId;
import com.commercetools.api.models.product_type.AttributeSetType;
import com.commercetools.api.models.product_type.AttributeSetTypeBuilder;
import com.commercetools.api.models.product_type.AttributeType;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypeDraftBuilder;
import com.commercetools.project.sync.producttype.ProductTypeSyncer;
import com.github.valfirst.slf4jtest.TestLogger;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.org.lidalia.slf4jext.Level;

// This will suppress MoreThanOneLogger warnings in this class
@SuppressWarnings("PMD.MoreThanOneLogger")
class ProductTypeSyncWithNestedAttributesIT {
  private static final TestLogger productTypeSyncerTestLogger =
      TestLoggerFactory.getTestLogger(ProductTypeSyncer.class);

  private static final String MAIN_PRODUCT_TYPE_KEY = "sample-product-type";

  @BeforeEach
  void setup() {
    productTypeSyncerTestLogger.clearAll();

    cleanUpProjects(CTP_SOURCE_CLIENT, CTP_TARGET_CLIENT);
    setupSourceProjectData(CTP_SOURCE_CLIENT);
  }

  static void setupSourceProjectData(@Nonnull final ProjectApiRoot sourceProjectClient) {
    final AttributeDefinitionDraft attributeProductReferenceSetType =
        createAttributeDefinitionDraftBuilderOfType(
                AttributeSetTypeBuilder.of()
                    .elementType(
                        typeBuilder ->
                            typeBuilder
                                .referenceBuilder()
                                .referenceTypeId(AttributeReferenceTypeId.PRODUCT))
                    .build())
            .name("product_references")
            .label(ofEnglish("product_references"))
            .build();

    final ProductTypeDraft mainProductTypeDraft =
        ProductTypeDraftBuilder.of()
            .key(MAIN_PRODUCT_TYPE_KEY)
            .name(MAIN_PRODUCT_TYPE_KEY)
            .description("a main product type")
            .attributes(attributeProductReferenceSetType)
            .build();

    final ProductType mainProductType =
        sourceProjectClient.productTypes().post(mainProductTypeDraft).executeBlocking().getBody();

    final AttributeDefinitionDraft nestedAttribute =
        createAttributeDefinitionDraftBuilderOfType(
                AttributeSetTypeBuilder.of()
                    .elementType(
                        typeBuilder ->
                            typeBuilder
                                .nestedBuilder()
                                .typeReference(mainProductType.toReference()))
                    .build())
            .name("nested_product_self")
            .label(ofEnglish("nested_product_self"))
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
                                .attribute(nestedAttribute)))
        .executeBlocking();
  }

  @AfterAll
  static void tearDownSuite() {
    cleanUpProjects(CTP_SOURCE_CLIENT, CTP_TARGET_CLIENT);
  }

  @Test
  void
      run_WithSyncAsArgumentWithProductTypesWithSelfReference_ShouldResolveReferencesAndExecuteProductTypeSyncer() {
    // test
    CliRunner.of().run(new String[] {"-s", "productTypes", "-f"}, createITSyncerFactory());

    // assertions
    assertThat(productTypeSyncerTestLogger.getAllLoggingEvents())
        .allMatch(loggingEvent -> !Level.ERROR.equals(loggingEvent.getLevel()));

    // Every sync module is expected to have 2 logs (start and stats summary)
    assertThat(productTypeSyncerTestLogger.getAllLoggingEvents()).hasSize(2);
    final ProductType productType =
        assertProductTypeExists(CTP_TARGET_CLIENT, MAIN_PRODUCT_TYPE_KEY);
    assertThat(productType.getAttributes()).hasSize(2);
    assertThat(productType.getAttribute("product_references"))
        .satisfies(
            attributeDefinition -> {
              assertThat(attributeDefinition.getType()).isInstanceOf(AttributeSetType.class);
              final AttributeSetType attributeSetType =
                  (AttributeSetType) attributeDefinition.getType();
              assertThat(attributeSetType.getElementType())
                  .isInstanceOf(AttributeReferenceType.class);
              final AttributeReferenceType attributeReferenceType =
                  (AttributeReferenceType) attributeSetType.getElementType();
              assertThat(attributeReferenceType.getReferenceTypeId())
                  .isEqualTo(AttributeReferenceTypeId.PRODUCT);
            });

    assertThat(productType.getAttribute("nested_product_self"))
        .satisfies(
            attributeDefinition -> {
              assertThat(attributeDefinition.getType()).isInstanceOf(AttributeSetType.class);
              final AttributeSetType attributeSetType =
                  (AttributeSetType) attributeDefinition.getType();
              assertThat(attributeSetType.getElementType()).isInstanceOf(AttributeNestedType.class);
              final AttributeNestedType attributeNestedType =
                  (AttributeNestedType) attributeSetType.getElementType();
              assertThat(attributeNestedType.getTypeReference())
                  .isEqualTo(productType.toReference());
            });
  }

  private static AttributeDefinitionDraftBuilder createAttributeDefinitionDraftBuilderOfType(
      final AttributeType attributeType) {
    return AttributeDefinitionDraftBuilder.of()
        .type(attributeType)
        .isRequired(false)
        .isSearchable(true)
        .attributeConstraint(AttributeConstraintEnum.SAME_FOR_ALL);
  }
}
