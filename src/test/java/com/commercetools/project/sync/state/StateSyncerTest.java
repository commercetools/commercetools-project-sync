package com.commercetools.project.sync.state;

import org.junit.jupiter.api.BeforeEach;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

// These tests aren't migrated
// TODO: Migrate tests
class StateSyncerTest {
  private final TestLogger syncerTestLogger = TestLoggerFactory.getTestLogger(StateSyncer.class);

  @BeforeEach
  void setup() {
    syncerTestLogger.clearAll();
  }
  //
  //  @Test
  //  void of_ShouldCreateStateSyncerInstance() {
  //    // test
  //    final StateSyncer stateSyncer =
  //        StateSyncer.of(mock(SphereClient.class), mock(SphereClient.class), getMockedClock());
  //
  //    // assertions
  //    assertThat(stateSyncer).isNotNull();
  //    assertThat(stateSyncer.getSync()).isInstanceOf(StateSync.class);
  //  }
  //
  //  @Test
  //  void transform_ShouldReplaceStateTransitionIdsWithKeys() {
  //    // preparation
  //    final SphereClient sourceClient = mock(SphereClient.class);
  //    final StateSyncer stateSyncer =
  //        StateSyncer.of(sourceClient, mock(SphereClient.class), getMockedClock());
  //    final List<State> states =
  //        asList(
  //            readObjectFromResource("state-1.json", State.class),
  //            readObjectFromResource("state-2.json", State.class));
  //
  //    final String jsonStringTransitions =
  //        "{\"results\":[{\"id\":\"ab949f1b-c441-4c70-9cf0-4182c36d6a6c\","
  //            + "\"key\":\"Initial\"} ]}";
  //    final ResourceKeyIdGraphQlResult transitionsResult =
  //        SphereJsonUtils.readObject(jsonStringTransitions, ResourceKeyIdGraphQlResult.class);
  //
  //    when(sourceClient.execute(any(ResourceIdsGraphQlRequest.class)))
  //        .thenReturn(CompletableFuture.completedFuture(transitionsResult));
  //
  //    // test
  //    final CompletionStage<List<StateDraft>> stateDrafts = stateSyncer.transform(states);
  //
  //    // assertions
  //    final StateDraft draft1 =
  //        StateDraftBuilder.of("State 1", StateType.LINE_ITEM_STATE)
  //            .roles(Collections.emptySet())
  //            .description(LocalizedString.ofEnglish("State 1"))
  //            .name(LocalizedString.ofEnglish("State 1"))
  //            .initial(true)
  //            .build();
  //    final StateDraft draft2 =
  //        StateDraftBuilder.of("State 2", StateType.LINE_ITEM_STATE)
  //            .roles(Collections.emptySet())
  //            .description(LocalizedString.ofEnglish("State 2"))
  //            .name(LocalizedString.ofEnglish("State 2"))
  //            .initial(false)
  //            .transitions(Collections.singleton(State.referenceOfId("Initial")))
  //            .build();
  //
  //    assertThat(stateDrafts).isCompletedWithValue(Arrays.asList(draft1, draft2));
  //  }
  //
  //  @Test
  //  void getQuery_ShouldBuildStateQuery() {
  //    // preparation
  //    final StateSyncer stateSyncer =
  //        StateSyncer.of(mock(SphereClient.class), mock(SphereClient.class), getMockedClock());
  //
  //    // test
  //    final StateQuery query = stateSyncer.getQuery();
  //
  //    // assertion
  //    assertThat(query).isEqualTo(StateQuery.of());
  //  }
  //
  //  @Test
  //  void syncWithError_WhenNoKeyIsProvided_ShouldCallErrorCallback() {
  //    // preparation
  //    final SphereClient sourceClient = mock(SphereClient.class);
  //    final SphereClient targetClient = mock(SphereClient.class);
  //    when(sourceClient.getConfig()).thenReturn(SphereApiConfig.of("source-project"));
  //    when(targetClient.getConfig()).thenReturn(SphereApiConfig.of("target-project"));
  //
  //    final List<State> stateTypePage =
  //        asList(readObjectFromResource("state-without-key.json", State.class));
  //
  //    final PagedQueryResult<State> pagedQueryResult = mock(PagedQueryResult.class);
  //    when(pagedQueryResult.getResults()).thenReturn(stateTypePage);
  //    when(sourceClient.execute(any(StateQuery.class)))
  //        .thenReturn(CompletableFuture.completedFuture(pagedQueryResult));
  //
  //    // test
  //    final StateSyncer stateSyncer = StateSyncer.of(sourceClient, targetClient,
  // mock(Clock.class));
  //    stateSyncer.sync(null, true).toCompletableFuture().join();
  //
  //    // assertion
  //    final LoggingEvent errorLog = syncerTestLogger.getAllLoggingEvents().get(1);
  //    assertThat(errorLog.getMessage())
  //        .isEqualTo(
  //            "Error when trying to sync state. Existing key: <<not present>>. Update actions:
  // []");
  //    assertThat(errorLog.getThrowable().get().getMessage())
  //        .isEqualTo(
  //            "StateDraft with name: LocalizedString(en -> State 1) doesn't have a key. Please
  // make sure all state drafts have keys.");
  //  }
}
