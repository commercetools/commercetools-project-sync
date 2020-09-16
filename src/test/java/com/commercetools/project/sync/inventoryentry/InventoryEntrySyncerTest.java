package com.commercetools.project.sync.inventoryentry;

import static com.commercetools.project.sync.util.TestUtils.getMockedClock;
import static com.commercetools.sync.inventories.utils.InventoryReferenceResolutionUtils.mapToInventoryEntryDrafts;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.sync.inventories.InventorySync;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.expansion.ExpansionPath;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.expansion.InventoryEntryExpansionModel;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class InventoryEntrySyncerTest {
  @Test
  void of_ShouldCreateInventoryEntrySyncerInstance() {
    // test
    final InventoryEntrySyncer inventorySyncer =
        InventoryEntrySyncer.of(
            mock(SphereClient.class), mock(SphereClient.class), getMockedClock());

    // assertions
    assertThat(inventorySyncer).isNotNull();
    assertThat(inventorySyncer.getSync()).isInstanceOf(InventorySync.class);
  }

  @Test
  void transform_ShouldReplaceInventoryEntryReferenceIdsWithKeys() {
    // preparation
    final InventoryEntrySyncer inventoryEntrySyncer =
        InventoryEntrySyncer.of(
            mock(SphereClient.class), mock(SphereClient.class), getMockedClock());
    final List<InventoryEntry> inventoryPage =
        asList(
            readObjectFromResource("inventory-sku-1.json", InventoryEntry.class),
            readObjectFromResource("inventory-sku-2.json", InventoryEntry.class));
    final List<String> referenceIds =
        inventoryPage
            .stream()
            .filter(inventoryEntry -> inventoryEntry.getSupplyChannel() != null)
            .filter(inventoryEntry -> inventoryEntry.getCustom() != null)
            .flatMap(
                inventoryEntry ->
                    Stream.of(
                        inventoryEntry.getCustom().getType().getId(),
                        inventoryEntry.getSupplyChannel().getId()))
            .collect(Collectors.toList());

    // test
    final CompletionStage<List<InventoryEntryDraft>> draftsFromPageStage =
        inventoryEntrySyncer.transform(inventoryPage);

    // assertions
    final List<InventoryEntryDraft> expectedResult = mapToInventoryEntryDrafts(inventoryPage);
    final List<String> referenceKeys =
        expectedResult
            .stream()
            .filter(inventoryEntry -> inventoryEntry.getSupplyChannel() != null)
            .filter(inventoryEntry -> inventoryEntry.getCustom() != null)
            .flatMap(
                inventoryEntry ->
                    Stream.of(
                        inventoryEntry.getCustom().getType().getId(),
                        inventoryEntry.getSupplyChannel().getId()))
            .collect(Collectors.toList());
    assertThat(referenceKeys).doesNotContainAnyElementsOf(referenceIds);
    assertThat(draftsFromPageStage).isCompletedWithValue(expectedResult);
  }
}
