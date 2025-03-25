package com.commercetools.project.sync;

import static com.commercetools.project.sync.util.CtpClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.project.sync.util.CtpClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.project.sync.util.IntegrationTestUtils.cleanUpProjects;
import static com.commercetools.project.sync.util.IntegrationTestUtils.createITSyncerFactory;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.channel.Channel;
import com.commercetools.api.models.channel.ChannelDraft;
import com.commercetools.api.models.channel.ChannelDraftBuilder;
import com.commercetools.api.models.channel.ChannelRoleEnum;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.inventory.InventoryEntry;
import com.commercetools.api.models.inventory.InventoryEntryDraft;
import com.commercetools.api.models.inventory.InventoryEntryDraftBuilder;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.FieldContainer;
import com.commercetools.api.models.type.FieldContainerBuilder;
import com.commercetools.api.models.type.FieldDefinition;
import com.commercetools.api.models.type.FieldDefinitionBuilder;
import com.commercetools.api.models.type.FieldTypeBuilder;
import com.commercetools.api.models.type.ResourceTypeId;
import com.commercetools.api.models.type.TypeDraft;
import com.commercetools.api.models.type.TypeDraftBuilder;
import com.commercetools.project.sync.inventoryentry.InventoryEntrySyncer;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.valfirst.slf4jtest.TestLogger;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

@Disabled(
    "Disabled for normal builds, "
        + "This scenario created to evaluate multiple channel use case"
        + "see: https://github.com/commercetools/commercetools-project-sync/issues/301")
public class InventorySyncMultiChannelIT {
  private static final TestLogger testLogger =
      TestLoggerFactory.getTestLogger(InventoryEntrySyncer.class);

  public static final String SKU_1 = "SKU_ONE";
  public static final String SKU_2 = "SKU_TWO";
  public static final String SUPPLY_CHANNEL_KEY_1 = "channel-key_1";
  public static final String SUPPLY_CHANNEL_KEY_2 = "channel-key_2";

  public static final String CUSTOM_TYPE = "inventory-custom-type-name";
  public static final String CUSTOM_FIELD_NAME = "inventory-custom-field-1";

  @BeforeEach
  void setup() {
    testLogger.clearAll();
    cleanUpProjects(CTP_SOURCE_CLIENT, CTP_TARGET_CLIENT);
    create249InventoryEntry(CTP_SOURCE_CLIENT);
    create249InventoryEntry(CTP_TARGET_CLIENT);
    setupProjectData(CTP_SOURCE_CLIENT);
    setupProjectData(CTP_TARGET_CLIENT);
  }

  private void setupProjectData(ProjectApiRoot projectApiRoot) {
    final ChannelDraft channelDraft1 =
        ChannelDraftBuilder.of()
            .key(SUPPLY_CHANNEL_KEY_1)
            .roles(ChannelRoleEnum.INVENTORY_SUPPLY)
            .build();
    final ChannelDraft channelDraft2 =
        ChannelDraftBuilder.of()
            .key(SUPPLY_CHANNEL_KEY_2)
            .roles(ChannelRoleEnum.INVENTORY_SUPPLY)
            .build();

    final Channel channel1 =
        projectApiRoot
            .channels()
            .create(channelDraft1)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();
    final Channel channel2 =
        projectApiRoot
            .channels()
            .create(channelDraft2)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    createInventoriesCustomType(projectApiRoot);

    // NOTE: There can only be one inventory entry for the combination of sku and supplyChannel.
    final InventoryEntryDraft draft1 =
        InventoryEntryDraftBuilder.of().sku(SKU_1).quantityOnStock(0L).build();
    final InventoryEntryDraft draft2 =
        InventoryEntryDraftBuilder.of()
            .sku(SKU_1)
            .quantityOnStock(0L)
            .supplyChannel(channel1.toResourceIdentifier())
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(builder -> builder.key(CUSTOM_TYPE))
                    .fields(getMockFieldContainer())
                    .build())
            .build();
    final InventoryEntryDraft draft3 =
        InventoryEntryDraftBuilder.of()
            .sku(SKU_1)
            .quantityOnStock(1L)
            .supplyChannel(channel2.toResourceIdentifier())
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(builder -> builder.key(CUSTOM_TYPE))
                    .fields(getMockFieldContainer())
                    .build())
            .build();

    final InventoryEntryDraft draft4 =
        InventoryEntryDraftBuilder.of().sku(SKU_2).quantityOnStock(0L).build();
    final InventoryEntryDraft draft5 =
        InventoryEntryDraftBuilder.of()
            .sku(SKU_2)
            .quantityOnStock(0L)
            .supplyChannel(channel1.toResourceIdentifier())
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(builder -> builder.key(CUSTOM_TYPE))
                    .fields(getMockFieldContainer())
                    .build())
            .build();
    final InventoryEntryDraft draft6 =
        InventoryEntryDraftBuilder.of()
            .sku(SKU_2)
            .quantityOnStock(1L)
            .supplyChannel(channel2.toResourceIdentifier())
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(builder -> builder.key(CUSTOM_TYPE))
                    .fields(getMockFieldContainer())
                    .build())
            .build();

    CompletableFuture.allOf(
            projectApiRoot.inventory().create(draft1).execute().toCompletableFuture(),
            projectApiRoot.inventory().create(draft2).execute().toCompletableFuture(),
            projectApiRoot.inventory().create(draft3).execute().toCompletableFuture(),
            projectApiRoot.inventory().create(draft4).execute().toCompletableFuture(),
            projectApiRoot.inventory().create(draft5).execute().toCompletableFuture(),
            projectApiRoot.inventory().create(draft6).execute().toCompletableFuture())
        .join();
  }

  private static void createInventoriesCustomType(@Nonnull final ProjectApiRoot ctpClient) {
    final FieldDefinition fieldDefinition =
        FieldDefinitionBuilder.of()
            .name(CUSTOM_FIELD_NAME)
            .label(LocalizedString.ofEnglish(CUSTOM_FIELD_NAME))
            .required(false)
            .type(FieldTypeBuilder.of().stringBuilder().build())
            .build();

    final TypeDraft typeDraft =
        TypeDraftBuilder.of()
            .key(CUSTOM_TYPE)
            .name(LocalizedString.ofEnglish(CUSTOM_TYPE))
            .resourceTypeIds(ResourceTypeId.INVENTORY_ENTRY)
            .fieldDefinitions(fieldDefinition)
            .build();

    ctpClient.types().create(typeDraft).execute().toCompletableFuture().join();
  }

  private static FieldContainer getMockFieldContainer() {
    return FieldContainerBuilder.of()
        .addValue(CUSTOM_FIELD_NAME, JsonNodeFactory.instance.textNode("customValue"))
        .build();
  }

  private void create249InventoryEntry(ProjectApiRoot projectApiRoot) {
    CompletableFuture.allOf(
            IntStream.range(0, 10)
                .mapToObj(
                    value ->
                        projectApiRoot
                            .inventory()
                            .create(
                                InventoryEntryDraftBuilder.of()
                                    .sku("SKU_" + value)
                                    .quantityOnStock(1L)
                                    .build())
                            .execute()
                            .toCompletableFuture())
                .toArray(CompletableFuture[]::new))
        .join();

    CompletableFuture.allOf(
            IntStream.range(0, 251)
                .mapToObj(value -> create(projectApiRoot, value))
                .toArray(CompletableFuture[]::new))
        .join();
  }

  private CompletableFuture<InventoryEntry> create(ProjectApiRoot projectApiRoot, int value) {
    final ChannelDraft channelDraft1 =
        ChannelDraftBuilder.of()
            .key("other-channel-key_" + value)
            .roles(ChannelRoleEnum.INVENTORY_SUPPLY)
            .build();

    final Channel channel =
        projectApiRoot
            .channels()
            .create(channelDraft1)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    return projectApiRoot
        .inventory()
        .create(
            InventoryEntryDraftBuilder.of()
                .supplyChannel(channel.toResourceIdentifier())
                .sku("SKU_CHANNEL")
                .quantityOnStock(0L)
                .build())
        .execute()
        .thenApply(ApiHttpResponse::getBody)
        .toCompletableFuture();
  }

  @Test
  void run_asInventoryFullSync_ShouldNotReturnAnError() {
    // test
    CliRunner.of().run(new String[] {"-s", "inventoryEntries", "-f"}, createITSyncerFactory());

    // assertions
    assertThat(testLogger.getAllLoggingEvents())
        .allMatch(loggingEvent -> !Level.ERROR.equals(loggingEvent.getLevel()));

    // Every sync module is expected to have 2 logs (start and stats summary)
    assertThat(testLogger.getAllLoggingEvents()).hasSize(2);
  }
}
