package com.commercetools.project.sync;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.project.sync.util.CtpClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.project.sync.util.CtpClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.project.sync.util.IntegrationTestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.category.CategoryDraftBuilder;
import com.commercetools.api.models.common.AssetDraft;
import com.commercetools.api.models.common.AssetDraftBuilder;
import com.commercetools.api.models.common.AssetSourceBuilder;
import com.commercetools.api.models.common.LocalizedStringBuilder;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.FieldDefinition;
import com.commercetools.api.models.type.FieldDefinitionBuilder;
import com.commercetools.api.models.type.ResourceTypeId;
import com.commercetools.api.models.type.Type;
import com.commercetools.api.models.type.TypeDraft;
import com.commercetools.api.models.type.TypeDraftBuilder;
import com.commercetools.api.models.type.TypeTextInputHint;
import com.commercetools.project.sync.category.CategorySyncer;
import com.github.valfirst.slf4jtest.TestLogger;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.org.lidalia.slf4jext.Level;

public class CategorySyncWithReferenceResolutionIT {

  private static final TestLogger testLogger =
      TestLoggerFactory.getTestLogger(CategorySyncer.class);
  private static final String RESOURCE_KEY = "foo";
  private static final String TYPE_KEY = "typeKey";

  @BeforeEach
  void setup() {
    testLogger.clearAll();
    cleanUpProjects(CTP_SOURCE_CLIENT, CTP_TARGET_CLIENT);
    setupSourceProjectData(CTP_SOURCE_CLIENT);
  }

  private void setupSourceProjectData(ProjectApiRoot sourceProjectClient) {
    final FieldDefinition FIELD_DEFINITION =
        FieldDefinitionBuilder.of()
            .type(fieldTypeBuilder -> fieldTypeBuilder.stringBuilder())
            .name("field_name")
            .label(LocalizedStringBuilder.of().addValue("en", "label_1").build())
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
            .fieldDefinitions(FIELD_DEFINITION)
            .build();

    final Type type = sourceProjectClient.types().create(typeDraft).executeBlocking().getBody();

    CTP_TARGET_CLIENT.types().create(typeDraft).executeBlocking();

    final CustomFieldsDraft customFieldsDraft =
        CustomFieldsDraftBuilder.of().type(type.toResourceIdentifier()).build();

    final AssetDraft assetDraft =
        AssetDraftBuilder.of()
            .name(ofEnglish("assetName"))
            .key("assetKey")
            .sources(AssetSourceBuilder.of().uri("sourceUri").build())
            .custom(customFieldsDraft)
            .build();

    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of()
            .name(ofEnglish("t-shirts"))
            .slug(ofEnglish("t-shirts"))
            .key(RESOURCE_KEY)
            .assets(assetDraft)
            .custom(customFieldsDraft)
            .build();

    sourceProjectClient.categories().create(categoryDraft).execute().toCompletableFuture().join();
  }

  @AfterAll
  static void tearDownSuite() {
    cleanUpProjects(CTP_SOURCE_CLIENT, CTP_TARGET_CLIENT);
  }

  @Test
  void
      run_WithSyncAsArgumentWithTypesAndCategories_ShouldResolveReferencesAndExecuteCategorySyncer() {
    // test
    CliRunner.of().run(new String[] {"-s", "categories"}, createITSyncerFactory());

    // assertions
    assertThat(testLogger.getAllLoggingEvents())
        .allMatch(loggingEvent -> !Level.ERROR.equals(loggingEvent.getLevel()));

    // Every sync module is expected to have 2 logs (start and stats summary)
    assertThat(testLogger.getAllLoggingEvents()).hasSize(2);
    assertCategoryExists(CTP_TARGET_CLIENT, RESOURCE_KEY);
  }
}
