package com.commercetools.project.sync.customobject;

import static com.commercetools.project.sync.util.TestUtils.getMockedClock;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.ByProjectKeyCustomObjectsGet;
import com.commercetools.api.client.ByProjectKeyCustomObjectsRequestBuilder;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.api.models.custom_object.CustomObjectDraft;
import com.commercetools.api.models.custom_object.CustomObjectDraftBuilder;
import com.commercetools.project.sync.SyncModuleOption;
import com.commercetools.project.sync.util.SyncUtils;
import com.commercetools.sync.customobjects.CustomObjectSync;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CustomObjectSyncerTest {

  private final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);

  private final ByProjectKeyCustomObjectsGet byProjectKeyCustomObjectsGet = mock();

  @BeforeEach
  void setup() {
    final ByProjectKeyCustomObjectsRequestBuilder byProjectKeyCustomObjectsRequestBuilder = mock();
    when(sourceClient.customObjects()).thenReturn(byProjectKeyCustomObjectsRequestBuilder);
    when(byProjectKeyCustomObjectsRequestBuilder.get()).thenReturn(byProjectKeyCustomObjectsGet);
    when(byProjectKeyCustomObjectsGet.withWhere(anyString()))
        .thenReturn(byProjectKeyCustomObjectsGet);
  }

  @Test
  void of_ShouldCreateCustomObjectSyncerInstance() {
    final String runnerName = "";

    final String[] argument = {""};
    when(byProjectKeyCustomObjectsGet.withPredicateVar(eq("excludedNames"), anyString()))
        .then(
            invocation -> {
              argument[0] = invocation.getArgument(1);
              return byProjectKeyCustomObjectsGet;
            });

    // test
    final CustomObjectSyncer customObjectSyncer =
        CustomObjectSyncer.of(
            sourceClient, mock(ProjectApiRoot.class), getMockedClock(), runnerName, false);

    // assertions
    final List<String> excludedContainerNames = getExcludedContainerNames(runnerName);

    assertThat(customObjectSyncer).isNotNull();
    assertThat(customObjectSyncer.getQuery()).isEqualTo(byProjectKeyCustomObjectsGet);
    assertThat(argument[0]).isEqualTo(String.join(",", excludedContainerNames));
    assertThat(customObjectSyncer.getSync()).isExactlyInstanceOf(CustomObjectSync.class);
  }

  @Test
  void transform_ShouldConvertResourcesToDrafts() {
    // preparation
    final CustomObjectSyncer customObjectSyncer =
        CustomObjectSyncer.of(
            mock(ProjectApiRoot.class), mock(ProjectApiRoot.class), getMockedClock(), "", false);

    final CustomObject customObject1 = mock(CustomObject.class);
    when(customObject1.getContainer()).thenReturn("testContainer1");
    when(customObject1.getKey()).thenReturn("testKey1");
    when(customObject1.getValue()).thenReturn("testValue1");

    final CustomObject customObject2 = mock(CustomObject.class);
    when(customObject2.getContainer()).thenReturn("testContainer2");
    when(customObject2.getKey()).thenReturn("testKey2");
    when(customObject2.getValue()).thenReturn(true);
    final List<CustomObject> customObjects = asList(customObject1, customObject2);

    // test
    final CompletionStage<List<CustomObjectDraft>> draftsFromPageStage =
        customObjectSyncer.transform(customObjects);

    // assertions
    assertThat(draftsFromPageStage)
        .isCompletedWithValue(
            customObjects.stream()
                .map(
                    customObject ->
                        CustomObjectDraftBuilder.of()
                            .container(customObject.getContainer())
                            .key(customObject.getKey())
                            .value(customObject.getValue())
                            .build())
                .collect(toList()));
  }

  @Test
  void getQuery_syncProjectSyncCustomObjects_ShouldBuildCustomObjectQuery() {
    // preparation
    final String runnerName = "testRunnerName";
    final CustomObjectSyncer customObjectSyncer =
        CustomObjectSyncer.of(
            sourceClient, mock(ProjectApiRoot.class), getMockedClock(), runnerName, true);

    // test
    final ByProjectKeyCustomObjectsGet query = customObjectSyncer.getQuery();

    // assertion
    assertThat(query).isEqualTo(byProjectKeyCustomObjectsGet);
    verify(byProjectKeyCustomObjectsGet, never()).withWhere(anyString());
  }

  private List<String> getExcludedContainerNames(String runnerName) {
    final List<String> lastSyncTimestampContainerNames =
        Stream.of(SyncModuleOption.values())
            .map(
                syncModuleOption -> {
                  final String moduleName = syncModuleOption.getSyncModuleName();
                  return SyncUtils.buildLastSyncTimestampContainerName(moduleName, runnerName);
                })
            .collect(toList());
    final List<String> currentCtpTimestampContainerNames =
        Stream.of(SyncModuleOption.values())
            .map(
                syncModuleOption -> {
                  final String moduleName = syncModuleOption.getSyncModuleName();
                  return SyncUtils.buildCurrentCtpTimestampContainerName(moduleName, runnerName);
                })
            .collect(toList());
    final List<String> excludedContainerNames =
        Stream.concat(
                lastSyncTimestampContainerNames.stream(),
                currentCtpTimestampContainerNames.stream())
            .collect(toList());
    return excludedContainerNames;
  }
}
