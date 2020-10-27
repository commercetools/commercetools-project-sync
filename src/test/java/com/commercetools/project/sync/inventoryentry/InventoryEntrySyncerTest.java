package com.commercetools.project.sync.inventoryentry;

import static com.commercetools.project.sync.util.TestUtils.getMockedClock;
import static com.commercetools.sync.inventories.utils.InventoryReferenceResolutionUtils.mapToInventoryEntryDrafts;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.inventories.InventorySync;
import io.sphere.sdk.client.SphereApiConfig;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

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

  @Test
  void syncWithError_ShouldCallErrorCallback() {
    final TestLogger syncerTestLogger = TestLoggerFactory.getTestLogger(InventoryEntrySyncer.class);
    // preparation: inventory entry with no key is synced
    final SphereClient sourceClient = mock(SphereClient.class);
    final SphereClient targetClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereApiConfig.of("source-project"));
    when(targetClient.getConfig()).thenReturn(SphereApiConfig.of("target-project"));
    final List<InventoryEntry> inventoryEntries =
        Collections.singletonList(
            readObjectFromResource("inventory-no-sku.json", InventoryEntry.class));

    final PagedQueryResult<InventoryEntry> pagedQueryResult = mock(PagedQueryResult.class);
    when(pagedQueryResult.getResults()).thenReturn(inventoryEntries);
    when(sourceClient.execute(any(InventoryEntryQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(pagedQueryResult));

    // test
    final InventoryEntrySyncer inventoryEntrySyncer =
        InventoryEntrySyncer.of(sourceClient, targetClient, mock(Clock.class));
    inventoryEntrySyncer.sync(null, true).toCompletableFuture().join();

    // assertion
    final LoggingEvent errorLog = syncerTestLogger.getAllLoggingEvents().get(0);
    assertThat(errorLog.getMessage())
        .isEqualTo(
            "Error when trying to sync inventory entry. Existing key: <<not present>>. Update actions: []");
    assertThat(errorLog.getThrowable().get().getMessage())
        .isEqualTo(
            "InventoryEntryDraft doesn't have a SKU. Please make sure all inventory entry drafts have SKUs.");
  }
}
