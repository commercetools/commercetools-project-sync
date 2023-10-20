package com.commercetools.project.sync.inventoryentry;

import static com.commercetools.project.sync.util.TestUtils.*;
import static com.commercetools.sync.inventories.utils.InventoryTransformUtils.toInventoryEntryDrafts;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ByProjectKeyGraphqlPost;
import com.commercetools.api.client.ByProjectKeyGraphqlRequestBuilder;
import com.commercetools.api.client.ByProjectKeyInventoryGet;
import com.commercetools.api.client.ByProjectKeyInventoryRequestBuilder;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.graph_ql.GraphQLRequest;
import com.commercetools.api.models.graph_ql.GraphQLResponse;
import com.commercetools.api.models.inventory.InventoryEntry;
import com.commercetools.api.models.inventory.InventoryEntryDraft;
import com.commercetools.api.models.inventory.InventoryPagedQueryResponse;
import com.commercetools.api.models.inventory.InventoryPagedQueryResponseBuilder;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.inventories.InventorySync;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

class InventoryEntrySyncerTest {

  private final TestLogger syncerTestLogger =
      TestLoggerFactory.getTestLogger(InventoryEntrySyncer.class);
  private final ReferenceIdToKeyCache referenceIdToKeyCache =
      new CaffeineReferenceIdToKeyCacheImpl();

  @BeforeEach
  void setup() {
    syncerTestLogger.clearAll();
  }

  @Test
  void of_ShouldCreateInventoryEntrySyncerInstance() {
    // test
    final InventoryEntrySyncer inventorySyncer =
        InventoryEntrySyncer.of(
            mock(ProjectApiRoot.class), mock(ProjectApiRoot.class), getMockedClock());

    // assertions
    assertThat(inventorySyncer).isNotNull();
    assertThat(inventorySyncer.getSync()).isInstanceOf(InventorySync.class);
  }

  @Test
  void transform_ShouldReplaceInventoryEntryReferenceIdsWithKeys() throws JsonProcessingException {
    // preparation
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final InventoryEntrySyncer inventoryEntrySyncer =
        InventoryEntrySyncer.of(sourceClient, mock(ProjectApiRoot.class), getMockedClock());
    final List<InventoryEntry> inventoryPage =
        asList(
            readObjectFromResource("inventory-sku-1.json", InventoryEntry.class),
            readObjectFromResource("inventory-sku-2.json", InventoryEntry.class));
    final List<String> referenceIds =
        inventoryPage.stream()
            .filter(inventoryEntry -> inventoryEntry.getSupplyChannel() != null)
            .filter(inventoryEntry -> inventoryEntry.getCustom() != null)
            .flatMap(
                inventoryEntry ->
                    Stream.of(
                        inventoryEntry.getCustom().getType().getId(),
                        inventoryEntry.getSupplyChannel().getId()))
            .collect(Collectors.toList());

    final String jsonStringCustomTypes =
        "{\"data\":{\"typeDefinitions\":{\"results\":[{\"id\":\"02e915e7-7763-48d1-83bd-d4e940a1a368\","
            + "\"key\":\"test-custom-type-key\"}]}}}";
    final GraphQLResponse customTypesResult =
        readObject(jsonStringCustomTypes, GraphQLResponse.class);

    final String jsonStringSupplyChannels =
        "{\"data\":{\"channels\":{\"results\":[{\"id\":\"5c0516b5-f506-4b6a-b4d1-c06ca29ab7e1\","
            + "\"key\":\"test-channel-key\"}]}}}";
    final GraphQLResponse supplyChannelsResult =
        readObject(jsonStringSupplyChannels, GraphQLResponse.class);

    final ApiHttpResponse<GraphQLResponse> graphQLResponse = mock(ApiHttpResponse.class);

    when(graphQLResponse.getBody()).thenReturn(customTypesResult).thenReturn(supplyChannelsResult);
    final ByProjectKeyGraphqlRequestBuilder byProjectKeyGraphqlRequestBuilder = mock();
    when(sourceClient.graphql()).thenReturn(byProjectKeyGraphqlRequestBuilder);
    final ByProjectKeyGraphqlPost byProjectKeyGraphqlPost = mock();
    when(byProjectKeyGraphqlRequestBuilder.post(any(GraphQLRequest.class)))
        .thenReturn(byProjectKeyGraphqlPost);
    when(byProjectKeyGraphqlPost.execute())
        .thenReturn(CompletableFuture.completedFuture(graphQLResponse));

    // test
    final CompletionStage<List<InventoryEntryDraft>> draftsFromPageStage =
        inventoryEntrySyncer.transform(inventoryPage);

    // assertions
    final List<InventoryEntryDraft> expectedResult =
        toInventoryEntryDrafts(sourceClient, referenceIdToKeyCache, inventoryPage).join();
    final List<String> referenceKeys =
        expectedResult.stream()
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
  @Disabled("https://commercetools.atlassian.net/browse/DEVX-275")
  void syncWithError_ShouldCallErrorCallback() throws JsonProcessingException {
    // preparation: inventory entry with no key is synced
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final ProjectApiRoot targetClient = mock(ProjectApiRoot.class);

    final List<InventoryEntry> inventoryEntries =
        Collections.singletonList(
            readObjectFromResource("inventory-no-sku.json", InventoryEntry.class));

    final ByProjectKeyInventoryRequestBuilder byProjectKeyInventoryRequestBuilder = mock();
    when(sourceClient.inventory()).thenReturn(byProjectKeyInventoryRequestBuilder);
    final ByProjectKeyInventoryGet byProjectKeyInventoryGet = mock();
    when(byProjectKeyInventoryRequestBuilder.get()).thenReturn(byProjectKeyInventoryGet);
    when(byProjectKeyInventoryGet.withSort(anyString())).thenReturn(byProjectKeyInventoryGet);
    when(byProjectKeyInventoryGet.withLimit(anyInt())).thenReturn(byProjectKeyInventoryGet);
    when(byProjectKeyInventoryGet.withWithTotal(anyBoolean())).thenReturn(byProjectKeyInventoryGet);
    final ApiHttpResponse<InventoryPagedQueryResponse> response = mock(ApiHttpResponse.class);
    final InventoryPagedQueryResponse inventoryPagedQueryResponse =
        InventoryPagedQueryResponseBuilder.of()
            .results(inventoryEntries)
            .limit(20L)
            .offset(0L)
            .count(1L)
            .build();
    when(response.getBody()).thenReturn(inventoryPagedQueryResponse);
    when(byProjectKeyInventoryGet.execute())
        .thenReturn(CompletableFuture.completedFuture(response));

    mockResourceIdsGraphQlRequest(
        sourceClient, "typeDefinitions", "4db98ea6-38dc-4ccb-b20f-466e1566567h", "customTypeKey");
    mockResourceIdsGraphQlRequest(
        sourceClient, "channels", "1489488b-f737-4a9e-ba49-2d42d84c4c6f", "channelKey");

    // test
    final InventoryEntrySyncer inventoryEntrySyncer =
        InventoryEntrySyncer.of(sourceClient, targetClient, mock(Clock.class));
    inventoryEntrySyncer.sync(null, true).toCompletableFuture().join();

    // assertion
    final LoggingEvent errorLog = syncerTestLogger.getAllLoggingEvents().get(1);
    assertThat(errorLog.getMessage())
        .isEqualTo(
            "Error when trying to sync inventory entry. Existing key: <<not present>>. Update actions: []");
    assertThat(errorLog.getThrowable().get().getMessage())
        .isEqualTo(
            "InventoryEntryDraft doesn't have a SKU. Please make sure all inventory entry drafts have SKUs.");
  }
}
