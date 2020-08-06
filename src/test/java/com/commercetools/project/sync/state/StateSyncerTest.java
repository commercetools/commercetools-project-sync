package com.commercetools.project.sync.state;

import static com.commercetools.project.sync.util.TestUtils.getMockedClock;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.sync.states.StateSync;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.StateDraftBuilder;
import io.sphere.sdk.states.StateType;
import io.sphere.sdk.states.expansion.StateExpansionModel;
import io.sphere.sdk.states.queries.StateQuery;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

class StateSyncerTest {

  @Test
  void of_ShouldCreateStateSyncerInstance() {
    final StateSyncer stateSyncer =
        StateSyncer.of(mock(SphereClient.class), mock(SphereClient.class), getMockedClock());

    // assertions
    assertThat(stateSyncer).isNotNull();
    assertThat(stateSyncer.getSync()).isInstanceOf(StateSync.class);
  }

  @Test
  void transform_ShouldReplaceStateTransitionIdsWithKeys() {
    final StateSyncer stateSyncer =
        StateSyncer.of(mock(SphereClient.class), mock(SphereClient.class), getMockedClock());
    final List<State> states =
        asList(
            readObjectFromResource("state-1.json", State.class),
            readObjectFromResource("state-2.json", State.class));

    final CompletionStage<List<StateDraft>> stateDrafts = stateSyncer.transform(states);
    final StateDraft draft1 =
        StateDraftBuilder.of("State 1", StateType.LINE_ITEM_STATE)
            .roles(Collections.emptySet())
            .description(LocalizedString.ofEnglish("State 1"))
            .name(LocalizedString.ofEnglish("State 1"))
            .initial(true)
            .transitions(Collections.emptySet())
            .build();
    final StateDraft draft2 =
        StateDraftBuilder.of("State 2", StateType.LINE_ITEM_STATE)
            .roles(Collections.emptySet())
            .description(LocalizedString.ofEnglish("State 2"))
            .name(LocalizedString.ofEnglish("State 2"))
            .initial(false)
            .transitions(Collections.singleton(State.referenceOfId("Initial")))
            .build();

    assertThat(stateDrafts).isCompletedWithValue(Arrays.asList(draft1, draft2));
  }

  @Test
  void getQuery_ShouldBuildStateQueryWithTransitionsExpanded() {
    // preparation
    final StateSyncer stateSyncer =
        StateSyncer.of(mock(SphereClient.class), mock(SphereClient.class), getMockedClock());

    // test
    final StateQuery query = stateSyncer.getQuery();

    // assertion
    assertThat(query)
        .isEqualTo(StateQuery.of().withExpansionPaths(StateExpansionModel::transitions));
  }
}
