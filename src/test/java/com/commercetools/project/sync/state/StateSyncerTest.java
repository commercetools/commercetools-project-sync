package com.commercetools.project.sync.state;

import static com.commercetools.project.sync.util.TestUtils.getMockedClock;
import static com.commercetools.project.sync.util.TestUtils.mockResourceIdsGraphQlRequest;
import static com.commercetools.project.sync.util.TestUtils.readObjectFromResource;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ByProjectKeyStatesGet;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.defaultconfig.ApiRootBuilder;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.state.State;
import com.commercetools.api.models.state.StateDraft;
import com.commercetools.api.models.state.StateDraftBuilder;
import com.commercetools.api.models.state.StatePagedQueryResponse;
import com.commercetools.api.models.state.StatePagedQueryResponseBuilder;
import com.commercetools.api.models.state.StateResourceIdentifierBuilder;
import com.commercetools.api.models.state.StateTypeEnum;
import com.commercetools.sync.states.StateSync;
import com.github.valfirst.slf4jtest.LoggingEvent;
import com.github.valfirst.slf4jtest.TestLogger;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StateSyncerTest {
  private final TestLogger syncerTestLogger = TestLoggerFactory.getTestLogger(StateSyncer.class);

  @BeforeEach
  void setup() {
    syncerTestLogger.clearAll();
  }

  @Test
  void of_ShouldCreateStateSyncerInstance() {
    // test
    final StateSyncer stateSyncer =
        StateSyncer.of(mock(ProjectApiRoot.class), mock(ProjectApiRoot.class), getMockedClock());

    // assertions
    assertThat(stateSyncer).isNotNull();
    assertThat(stateSyncer.getSync()).isInstanceOf(StateSync.class);
  }

  @Test
  void transform_ShouldReplaceStateTransitionIdsWithKeys() {
    // preparation
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    mockResourceIdsGraphQlRequest(
        sourceClient, "states", "ab949f1b-c441-4c70-9cf0-4182c36d6a6c", "Initial");

    final StateSyncer stateSyncer =
        StateSyncer.of(sourceClient, mock(ProjectApiRoot.class), getMockedClock());
    final List<State> states =
        List.of(
            readObjectFromResource("state-1.json", State.class),
            readObjectFromResource("state-2.json", State.class));

    // test
    final CompletionStage<List<StateDraft>> stateDrafts = stateSyncer.transform(states);

    // assertions
    final StateDraft draft1 =
        StateDraftBuilder.of()
            .key("State 1")
            .type(StateTypeEnum.LINE_ITEM_STATE)
            .roles(List.of())
            .description(LocalizedString.ofEnglish("State 1"))
            .name(LocalizedString.ofEnglish("State 1"))
            .initial(true)
            .build();
    final StateDraft draft2 =
        StateDraftBuilder.of()
            .key("State 2")
            .type(StateTypeEnum.LINE_ITEM_STATE)
            .roles(List.of())
            .description(LocalizedString.ofEnglish("State 2"))
            .name(LocalizedString.ofEnglish("State 2"))
            .initial(false)
            .transitions(StateResourceIdentifierBuilder.of().key("Initial").build())
            .build();

    assertThat(stateDrafts).isCompletedWithValue(List.of(draft1, draft2));
  }

  @Test
  void getQuery_ShouldBuildStateQuery() {
    // preparation
    final ProjectApiRoot apiRoot =
        ApiRootBuilder.of().withApiBaseUrl("apiBaseUrl").build("testProjectKey");
    final StateSyncer stateSyncer =
        StateSyncer.of(apiRoot, mock(ProjectApiRoot.class), getMockedClock());

    // test + assertion
    assertThat(stateSyncer.getQuery()).isInstanceOf(ByProjectKeyStatesGet.class);
  }

  @Test
  void transform_WhenNoKeyIsProvided_ShouldContinueWithEmptyStateDraft() {
    // preparation
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final List<State> stateTypePage =
        List.of(readObjectFromResource("state-without-key.json", State.class));

    // test
    final StateSyncer stateSyncer =
        StateSyncer.of(sourceClient, mock(ProjectApiRoot.class), getMockedClock());
    final CompletableFuture<List<StateDraft>> stateDrafts =
        stateSyncer.transform(stateTypePage).toCompletableFuture();

    // assertion
    assertThat(stateDrafts).isCompletedWithValue(List.of(StateDraft.of()));
  }

  @Test
  void syncWithError_WhenTransitionReferencesAreInvalid_ShouldCallErrorCallback() {
    // preparation
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final State state = readObjectFromResource("state-2.json", State.class);
    state.getTransitions().get(0).setId("invalidReference");
    final StatePagedQueryResponse queryResponse =
        StatePagedQueryResponseBuilder.of()
            .results(List.of(state))
            .limit(20L)
            .offset(0L)
            .count(1L)
            .build();
    final ApiHttpResponse apiHttpResponse = mock(ApiHttpResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(queryResponse);
    final ByProjectKeyStatesGet byProjectKeyStatesGet = mock(ByProjectKeyStatesGet.class);
    when(byProjectKeyStatesGet.withLimit(anyInt())).thenReturn(byProjectKeyStatesGet);
    when(byProjectKeyStatesGet.withWithTotal(anyBoolean())).thenReturn(byProjectKeyStatesGet);
    when(byProjectKeyStatesGet.withSort(anyString())).thenReturn(byProjectKeyStatesGet);
    when(byProjectKeyStatesGet.execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(sourceClient.states()).thenReturn(mock());
    when(sourceClient.states().get()).thenReturn(byProjectKeyStatesGet);

    mockResourceIdsGraphQlRequest(sourceClient, "states", "", "");
    // test
    final StateSyncer stateSyncer =
        StateSyncer.of(sourceClient, mock(ProjectApiRoot.class), getMockedClock());
    stateSyncer.sync(null, true).toCompletableFuture().join();

    // assertion
    final LoggingEvent errorLog = syncerTestLogger.getAllLoggingEvents().get(1);
    assertThat(errorLog.getMessage())
        .isEqualTo(
            "Error when trying to sync state. Existing key: <<not present>>. Update actions: []");
    assertThat(errorLog.getThrowable().get().getMessage())
        .isEqualTo(
            format("StateDraft with key: '%s' has invalid state transitions", state.getKey()));
  }
}
