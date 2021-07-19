package com.commercetools.project.sync;

import static com.commercetools.project.sync.util.IntegrationTestUtils.cleanUpProjects;
import static com.commercetools.project.sync.util.IntegrationTestUtils.createITSyncerFactory;
import static com.commercetools.project.sync.util.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.project.sync.util.SphereClientUtils.CTP_TARGET_CLIENT;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.project.sync.inventoryentry.InventoryEntrySyncer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelDraft;
import io.sphere.sdk.channels.ChannelRole;
import io.sphere.sdk.channels.commands.ChannelCreateCommand;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import io.sphere.sdk.inventory.commands.InventoryEntryCreateCommand;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.FieldDefinition;
import io.sphere.sdk.types.StringFieldType;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.TypeDraftBuilder;
import io.sphere.sdk.types.commands.TypeCreateCommand;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

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

  private void setupProjectData(SphereClient sphereClient) {
    final ChannelDraft channelDraft1 =
        ChannelDraft.of(SUPPLY_CHANNEL_KEY_1).withRoles(ChannelRole.INVENTORY_SUPPLY);
    final ChannelDraft channelDraft2 =
        ChannelDraft.of(SUPPLY_CHANNEL_KEY_2).withRoles(ChannelRole.INVENTORY_SUPPLY);

    final String channelId1 =
        sphereClient
            .execute(ChannelCreateCommand.of(channelDraft1))
            .toCompletableFuture()
            .join()
            .getId();
    final String channelId2 =
        sphereClient
            .execute(ChannelCreateCommand.of(channelDraft2))
            .toCompletableFuture()
            .join()
            .getId();

    final Reference<Channel> supplyChannelReference1 = Channel.referenceOfId(channelId1);
    final Reference<Channel> supplyChannelReference2 = Channel.referenceOfId(channelId2);

    createInventoriesCustomType(sphereClient);

    // NOTE: There can only be one inventory entry for the combination of sku and supplyChannel.
    final InventoryEntryDraft draft1 = InventoryEntryDraftBuilder.of(SKU_1, 0L).build();
    final InventoryEntryDraft draft2 =
        InventoryEntryDraftBuilder.of(SKU_1, 0L)
            .supplyChannel(supplyChannelReference1)
            .custom(CustomFieldsDraft.ofTypeKeyAndJson(CUSTOM_TYPE, getMockCustomFieldsJsons()))
            .build();
    final InventoryEntryDraft draft3 =
        InventoryEntryDraftBuilder.of(SKU_1, 1L)
            .supplyChannel(supplyChannelReference2)
            .custom(CustomFieldsDraft.ofTypeKeyAndJson(CUSTOM_TYPE, getMockCustomFieldsJsons()))
            .build();

    final InventoryEntryDraft draft4 = InventoryEntryDraftBuilder.of(SKU_2, 0L).build();
    final InventoryEntryDraft draft5 =
        InventoryEntryDraftBuilder.of(SKU_2, 0L)
            .supplyChannel(supplyChannelReference1)
            .custom(CustomFieldsDraft.ofTypeKeyAndJson(CUSTOM_TYPE, getMockCustomFieldsJsons()))
            .build();
    final InventoryEntryDraft draft6 =
        InventoryEntryDraftBuilder.of(SKU_2, 1L)
            .supplyChannel(supplyChannelReference2)
            .custom(CustomFieldsDraft.ofTypeKeyAndJson(CUSTOM_TYPE, getMockCustomFieldsJsons()))
            .build();

    CompletableFuture.allOf(
            sphereClient.execute(InventoryEntryCreateCommand.of(draft1)).toCompletableFuture(),
            sphereClient.execute(InventoryEntryCreateCommand.of(draft2)).toCompletableFuture(),
            sphereClient.execute(InventoryEntryCreateCommand.of(draft3)).toCompletableFuture(),
            sphereClient.execute(InventoryEntryCreateCommand.of(draft4)).toCompletableFuture(),
            sphereClient.execute(InventoryEntryCreateCommand.of(draft5)).toCompletableFuture(),
            sphereClient.execute(InventoryEntryCreateCommand.of(draft6)).toCompletableFuture())
        .join();
  }

  private static void createInventoriesCustomType(@Nonnull final SphereClient ctpClient) {
    final FieldDefinition fieldDefinition =
        FieldDefinition.of(
            StringFieldType.of(),
            CUSTOM_FIELD_NAME,
            LocalizedString.of(Locale.ENGLISH, CUSTOM_FIELD_NAME),
            false);
    final TypeDraft typeDraft =
        TypeDraftBuilder.of(
                CUSTOM_TYPE,
                LocalizedString.of(Locale.ENGLISH, CUSTOM_TYPE),
                Collections.singleton(InventoryEntry.resourceTypeId()))
            .fieldDefinitions(singletonList(fieldDefinition))
            .build();
    ctpClient.execute(TypeCreateCommand.of(typeDraft)).toCompletableFuture().join();
  }

  private static Map<String, JsonNode> getMockCustomFieldsJsons() {
    final Map<String, JsonNode> customFieldsJsons = new HashMap<>();
    customFieldsJsons.put(CUSTOM_FIELD_NAME, JsonNodeFactory.instance.textNode("customValue"));
    return customFieldsJsons;
  }

  private void create249InventoryEntry(SphereClient sphereClient) {
    CompletableFuture.allOf(
            IntStream.range(0, 10)
                .mapToObj(
                    value ->
                        sphereClient
                            .execute(
                                InventoryEntryCreateCommand.of(
                                    InventoryEntryDraftBuilder.of("SKU_" + value, 1L).build()))
                            .toCompletableFuture())
                .toArray(CompletableFuture[]::new))
        .join();

    CompletableFuture.allOf(
            IntStream.range(0, 251)
                .mapToObj(value -> create(sphereClient, value))
                .toArray(CompletableFuture[]::new))
        .join();
  }

  private CompletableFuture<InventoryEntry> create(SphereClient sphereClient, int value) {
    final ChannelDraft channelDraft1 =
        ChannelDraft.of("other-channel-key_" + value).withRoles(ChannelRole.INVENTORY_SUPPLY);

    final String channelId =
        sphereClient
            .execute(ChannelCreateCommand.of(channelDraft1))
            .toCompletableFuture()
            .join()
            .getId();

    final Reference<Channel> supplyChannelReference = Channel.referenceOfId(channelId);

    return sphereClient
        .execute(
            InventoryEntryCreateCommand.of(
                InventoryEntryDraftBuilder.of("SKU_CHANNEL", 0L)
                    .supplyChannel(supplyChannelReference)
                    .build()))
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
