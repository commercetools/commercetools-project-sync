package com.commercetools.project.sync.inventoryentry;

import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import org.junit.jupiter.api.BeforeEach;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

// These tests aren't migrated
// TODO: Migrate tests
class InventoryEntrySyncerTest {

  private final TestLogger syncerTestLogger =
      TestLoggerFactory.getTestLogger(InventoryEntrySyncer.class);
  private final ReferenceIdToKeyCache referenceIdToKeyCache =
      new CaffeineReferenceIdToKeyCacheImpl();

  @BeforeEach
  void setup() {
    syncerTestLogger.clearAll();
  }

  //  @Test
  //  void of_ShouldCreateInventoryEntrySyncerInstance() {
  //    // test
  //    final InventoryEntrySyncer inventorySyncer =
  //        InventoryEntrySyncer.of(
  //            mock(SphereClient.class), mock(SphereClient.class), getMockedClock());
  //
  //    // assertions
  //    assertThat(inventorySyncer).isNotNull();
  //    assertThat(inventorySyncer.getSync()).isInstanceOf(InventorySync.class);
  //  }
  //
  //  @Test
  //  void transform_ShouldReplaceInventoryEntryReferenceIdsWithKeys() {
  //    // preparation
  //    final SphereClient sourceClient = mock(SphereClient.class);
  //    final InventoryEntrySyncer inventoryEntrySyncer =
  //        InventoryEntrySyncer.of(sourceClient, mock(SphereClient.class), getMockedClock());
  //    final List<InventoryEntry> inventoryPage =
  //        asList(
  //            readObjectFromResource("inventory-sku-1.json", InventoryEntry.class),
  //            readObjectFromResource("inventory-sku-2.json", InventoryEntry.class));
  //    final List<String> referenceIds =
  //        inventoryPage.stream()
  //            .filter(inventoryEntry -> inventoryEntry.getSupplyChannel() != null)
  //            .filter(inventoryEntry -> inventoryEntry.getCustom() != null)
  //            .flatMap(
  //                inventoryEntry ->
  //                    Stream.of(
  //                        inventoryEntry.getCustom().getType().getId(),
  //                        inventoryEntry.getSupplyChannel().getId()))
  //            .collect(Collectors.toList());
  //
  //    final String jsonStringCustomTypes =
  //        "{\"results\":[{\"id\":\"02e915e7-7763-48d1-83bd-d4e940a1a368\","
  //            + "\"key\":\"test-custom-type-key\"} ]}";
  //    final ResourceKeyIdGraphQlResult customTypesResult =
  //        SphereJsonUtils.readObject(jsonStringCustomTypes, ResourceKeyIdGraphQlResult.class);
  //
  //    final String jsonStringSupplyChannels =
  //        "{\"results\":[{\"id\":\"5c0516b5-f506-4b6a-b4d1-c06ca29ab7e1\","
  //            + "\"key\":\"test-channel-key\"} ]}";
  //    final ResourceKeyIdGraphQlResult supplyChannelsResult =
  //        SphereJsonUtils.readObject(jsonStringSupplyChannels, ResourceKeyIdGraphQlResult.class);
  //
  //    when(sourceClient.execute(any(ResourceIdsGraphQlRequest.class)))
  //        .thenReturn(CompletableFuture.completedFuture(customTypesResult))
  //        .thenReturn(CompletableFuture.completedFuture(supplyChannelsResult));
  //
  //    // test
  //    final CompletionStage<List<InventoryEntryDraft>> draftsFromPageStage =
  //        inventoryEntrySyncer.transform(inventoryPage);
  //
  //    // assertions
  //    final List<InventoryEntryDraft> expectedResult =
  //        toInventoryEntryDrafts(sourceClient, referenceIdToKeyCache, inventoryPage).join();
  //    final List<String> referenceKeys =
  //        expectedResult.stream()
  //            .filter(inventoryEntry -> inventoryEntry.getSupplyChannel() != null)
  //            .filter(inventoryEntry -> inventoryEntry.getCustom() != null)
  //            .flatMap(
  //                inventoryEntry ->
  //                    Stream.of(
  //                        inventoryEntry.getCustom().getType().getId(),
  //                        inventoryEntry.getSupplyChannel().getId()))
  //            .collect(Collectors.toList());
  //    assertThat(referenceKeys).doesNotContainAnyElementsOf(referenceIds);
  //    assertThat(draftsFromPageStage).isCompletedWithValue(expectedResult);
  //  }
  //
  //  @Test
  //  void syncWithError_ShouldCallErrorCallback() {
  //    // preparation: inventory entry with no key is synced
  //    final SphereClient sourceClient = mock(SphereClient.class);
  //    final SphereClient targetClient = mock(SphereClient.class);
  //    when(sourceClient.getConfig()).thenReturn(SphereApiConfig.of("source-project"));
  //    when(targetClient.getConfig()).thenReturn(SphereApiConfig.of("target-project"));
  //    final List<InventoryEntry> inventoryEntries =
  //        Collections.singletonList(
  //            readObjectFromResource("inventory-no-sku.json", InventoryEntry.class));
  //
  //    final PagedQueryResult<InventoryEntry> pagedQueryResult = mock(PagedQueryResult.class);
  //    when(pagedQueryResult.getResults()).thenReturn(inventoryEntries);
  //    when(sourceClient.execute(any(InventoryEntryQuery.class)))
  //        .thenReturn(CompletableFuture.completedFuture(pagedQueryResult));
  //
  //    mockResourceIdsGraphQlRequest(
  //        sourceClient, "4db98ea6-38dc-4ccb-b20f-466e1566567h", "customTypeKey");
  //    mockResourceIdsGraphQlRequest(
  //        sourceClient, "1489488b-f737-4a9e-ba49-2d42d84c4c6f", "channelKey");
  //
  //    // test
  //    final InventoryEntrySyncer inventoryEntrySyncer =
  //        InventoryEntrySyncer.of(sourceClient, targetClient, mock(Clock.class));
  //    inventoryEntrySyncer.sync(null, true).toCompletableFuture().join();
  //
  //    // assertion
  //    final LoggingEvent errorLog = syncerTestLogger.getAllLoggingEvents().get(1);
  //    assertThat(errorLog.getMessage())
  //        .isEqualTo(
  //            "Error when trying to sync inventory entry. Existing key: <<not present>>. Update
  // actions: []");
  //    assertThat(errorLog.getThrowable().get().getMessage())
  //        .isEqualTo(
  //            "InventoryEntryDraft doesn't have a SKU. Please make sure all inventory entry drafts
  // have SKUs.");
  //  }
}
