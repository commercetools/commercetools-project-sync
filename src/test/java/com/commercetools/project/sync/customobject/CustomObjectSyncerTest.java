package com.commercetools.project.sync.customobject;

import static com.commercetools.project.sync.util.TestUtils.getMockedClock;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.project.sync.SyncModuleOption;
import com.commercetools.project.sync.util.SyncUtils;
import com.commercetools.sync.customobjects.CustomObjectSync;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class CustomObjectSyncerTest {

  @Test
  void of_ShouldCreateCustomObjectSyncerInstance() {
    // test
    final String runnerName = "";
    final CustomObjectSyncer customObjectSyncer =
        CustomObjectSyncer.of(
            mock(SphereClient.class),
            mock(SphereClient.class),
            getMockedClock(),
            runnerName,
            false);

    // assertions
    final List<String> excludedContainerNames = getExcludedContainerNames(runnerName);
    assertThat(customObjectSyncer).isNotNull();
    assertThat(customObjectSyncer.getQuery())
        .isEqualTo(
            CustomObjectQuery.of(JsonNode.class)
                .plusPredicates(
                    customObjectQueryModel ->
                        customObjectQueryModel.container().isNotIn(excludedContainerNames)));
    assertThat(customObjectSyncer.getSync()).isExactlyInstanceOf(CustomObjectSync.class);
  }

  @Test
  void transform_ShouldConvertResourcesToDrafts() {
    // preparation
    final CustomObjectSyncer customObjectSyncer =
        CustomObjectSyncer.of(
            mock(SphereClient.class), mock(SphereClient.class), getMockedClock(), "", false);

    final CustomObject<JsonNode> customObject1 = mock(CustomObject.class);
    when(customObject1.getContainer()).thenReturn("testContainer1");
    when(customObject1.getKey()).thenReturn("testKey1");
    when(customObject1.getValue()).thenReturn(JsonNodeFactory.instance.textNode("testValue1"));

    final CustomObject<JsonNode> customObject2 = mock(CustomObject.class);
    when(customObject2.getContainer()).thenReturn("testContainer2");
    when(customObject2.getKey()).thenReturn("testKey2");
    when(customObject2.getValue()).thenReturn(JsonNodeFactory.instance.booleanNode(true));
    final List<CustomObject<JsonNode>> customObjects = asList(customObject1, customObject2);

    // test
    final CompletionStage<List<CustomObjectDraft<JsonNode>>> draftsFromPageStage =
        customObjectSyncer.transform(customObjects);

    // assertions
    assertThat(draftsFromPageStage)
        .isCompletedWithValue(
            customObjects.stream()
                .map(
                    customObject ->
                        CustomObjectDraft.ofUnversionedUpsert(
                            customObject.getContainer(),
                            customObject.getKey(),
                            customObject.getValue()))
                .collect(toList()));
  }

  @Test
  void getQuery_doNotSyncProjectSyncCustomObjects_ShouldBuildCustomObjectQuery() {
    // preparation
    final String runnerName = "testRunnerName";
    final CustomObjectSyncer customObjectSyncer =
        CustomObjectSyncer.of(
            mock(SphereClient.class),
            mock(SphereClient.class),
            getMockedClock(),
            runnerName,
            false);

    // test
    final CustomObjectQuery query = customObjectSyncer.getQuery();

    // assertion
    final List<String> excludedContainerNames = getExcludedContainerNames(runnerName);
    assertThat(query)
        .isEqualTo(
            CustomObjectQuery.ofJsonNode()
                .plusPredicates(
                    customObjectQueryModel ->
                        customObjectQueryModel.container().isNotIn(excludedContainerNames)));
  }

  @Test
  void getQuery_syncProjectSyncCustomObjects_ShouldBuildCustomObjectQuery() {
    // preparation
    final String runnerName = "testRunnerName";
    final CustomObjectSyncer customObjectSyncer =
        CustomObjectSyncer.of(
            mock(SphereClient.class), mock(SphereClient.class), getMockedClock(), runnerName, true);

    // test
    final CustomObjectQuery query = customObjectSyncer.getQuery();

    // assertion
    assertThat(query).isEqualTo(CustomObjectQuery.ofJsonNode());
  }

  private List<String> getExcludedContainerNames(String runnerName) {
    final List<String> lastSyncTimestampContainerNames =
        Stream.of(SyncModuleOption.values())
            .map(
                syncModuleOption -> {
                  final String moduleName = syncModuleOption.getSyncModuleName();
                  return SyncUtils.buildLastSyncTimestampContainerName(moduleName, runnerName);
                })
            .collect(Collectors.toList());
    final List<String> currentCtpTimestampContainerNames =
        Stream.of(SyncModuleOption.values())
            .map(
                syncModuleOption -> {
                  final String moduleName = syncModuleOption.getSyncModuleName();
                  return SyncUtils.buildCurrentCtpTimestampContainerName(moduleName, runnerName);
                })
            .collect(Collectors.toList());
    final List<String> excludedContainerNames =
        Stream.concat(
                lastSyncTimestampContainerNames.stream(),
                currentCtpTimestampContainerNames.stream())
            .collect(Collectors.toList());
    return excludedContainerNames;
  }
}
