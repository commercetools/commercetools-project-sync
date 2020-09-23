package com.commercetools.project.sync.customobject;

import static com.commercetools.project.sync.util.TestUtils.getMockedClock;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.customobjects.CustomObjectSync;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import java.util.List;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

public class CustomObjectSyncerTest {

  @Test
  void of_ShouldCreateCustomObjectSyncerInstance() {
    // test
    final CustomObjectSyncer customObjectSyncer =
        CustomObjectSyncer.of(mock(SphereClient.class), mock(SphereClient.class), getMockedClock());

    // assertions
    assertThat(customObjectSyncer).isNotNull();
    assertThat(customObjectSyncer.getQuery()).isEqualTo(CustomObjectQuery.of(JsonNode.class));
    assertThat(customObjectSyncer.getSync()).isExactlyInstanceOf(CustomObjectSync.class);
  }

  @Test
  void transform_ShouldConvertResourcesToDrafts() {
    // preparation
    final CustomObjectSyncer customObjectSyncer =
        CustomObjectSyncer.of(mock(SphereClient.class), mock(SphereClient.class), getMockedClock());

    final CustomObject<JsonNode> customObject1 = mock(CustomObject.class);
    when(customObject1.getContainer()).thenReturn("testContainer1");
    when(customObject1.getKey()).thenReturn("testKey1");
    when(customObject1.getValue()).thenReturn(JsonNodeFactory.instance.textNode("testValue1"));

    final CustomObject<JsonNode> customObject2 = mock(CustomObject.class);
    when(customObject2.getContainer()).thenReturn("testContainer2");
    when(customObject2.getKey()).thenReturn("testKey2");
    when(customObject2.getValue()).thenReturn(JsonNodeFactory.instance.booleanNode(true));
    final List<CustomObject<JsonNode>> drafts = asList(customObject1, customObject2);

    // test
    final CompletionStage<List<CustomObjectDraft<JsonNode>>> draftsFromPageStage =
        customObjectSyncer.transform(drafts);

    // assertions
    assertThat(draftsFromPageStage)
        .isCompletedWithValue(
            drafts
                .stream()
                .map(
                    customObject ->
                        CustomObjectDraft.ofUnversionedUpsert(
                            customObject.getContainer(),
                            customObject.getKey(),
                            customObject.getValue()))
                .collect(toList()));
  }

  @Test
  void getQuery_ShouldBuildCustomObjectQuery() {
    // preparation
    final CustomObjectSyncer customObjectSyncer =
        CustomObjectSyncer.of(mock(SphereClient.class), mock(SphereClient.class), getMockedClock());

    // test
    final CustomObjectQuery query = customObjectSyncer.getQuery();

    // assertion
    assertThat(query).isEqualTo(CustomObjectQuery.ofJsonNode());
  }
}
