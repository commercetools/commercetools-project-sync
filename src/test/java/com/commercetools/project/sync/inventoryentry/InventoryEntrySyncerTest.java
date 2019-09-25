package com.commercetools.project.sync.inventoryentry;

import com.commercetools.sync.inventories.InventorySync;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.expansion.ExpansionPath;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.expansion.InventoryEntryExpansionModel;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletionStage;

import static com.commercetools.project.sync.util.TestUtils.getMockedClock;
import static com.commercetools.sync.inventories.utils.InventoryReferenceReplacementUtils.replaceInventoriesReferenceIdsWithKeys;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class InventoryEntrySyncerTest {
  @Test
  void of_ShouldCreateInventoryEntrySyncerInstance() {
    // test
    final InventoryEntrySyncer inventorySyncer =
        InventoryEntrySyncer.of(
            mock(SphereClient.class), mock(SphereClient.class), getMockedClock());

    // assertions
    assertThat(inventorySyncer).isNotNull();

    final InventoryEntryQuery expectedQuery =
        InventoryEntryQuery.of()
            .withExpansionPaths(InventoryEntryExpansionModel::supplyChannel)
            .plusExpansionPaths(ExpansionPath.of("custom.type"));

    assertThat(inventorySyncer.getQuery()).isEqualTo(expectedQuery);
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

    // test
    final CompletionStage<List<InventoryEntryDraft>> draftsFromPageStage =
        inventoryEntrySyncer.transform(inventoryPage);

    // assertions
    final List<InventoryEntryDraft> expectedResult =
        replaceInventoriesReferenceIdsWithKeys(inventoryPage);
    assertThat(draftsFromPageStage).isCompletedWithValue(expectedResult);
  }

  @Test
  void getQuery_ShouldBuildInventoryEntryQuery() {
    // preparation
    final InventoryEntrySyncer inventoryEntrySyncer =
        InventoryEntrySyncer.of(
            mock(SphereClient.class), mock(SphereClient.class), getMockedClock());

    // test
    final InventoryEntryQuery query = inventoryEntrySyncer.getQuery();

    // assertion
    assertThat(query)
        .isEqualTo(
            InventoryEntryQuery.of()
                .withExpansionPaths(InventoryEntryExpansionModel::supplyChannel)
                .plusExpansionPaths(ExpansionPath.of("custom.type")));
  }
}
