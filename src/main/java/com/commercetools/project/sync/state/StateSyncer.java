package com.commercetools.project.sync.state;

import com.commercetools.project.sync.Syncer;
import com.commercetools.project.sync.service.CustomObjectService;
import com.commercetools.project.sync.service.impl.CustomObjectServiceImpl;
import com.commercetools.sync.states.StateSync;
import com.commercetools.sync.states.StateSyncOptions;
import com.commercetools.sync.states.StateSyncOptionsBuilder;
import com.commercetools.sync.states.helpers.StateSyncStatistics;
import com.commercetools.sync.states.utils.StateReferenceResolutionUtils;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.expansion.StateExpansionModel;
import io.sphere.sdk.states.queries.StateQuery;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public final class StateSyncer
    extends Syncer<
        State, StateDraft, StateSyncStatistics, StateSyncOptions, StateQuery, StateSync> {

  private static final Logger LOGGER = LoggerFactory.getLogger(StateSyncer.class);

  private StateSyncer(
      @Nonnull StateSync sync,
      @Nonnull SphereClient sourceClient,
      @Nonnull SphereClient targetClient,
      @Nonnull CustomObjectService customObjectService,
      @Nonnull Clock clock) {
    super(sync, sourceClient, targetClient, customObjectService, clock);
  }

  public static StateSyncer of(
      @Nonnull final SphereClient sourceClient,
      @Nonnull final SphereClient targetClient,
      @Nonnull final Clock clock) {
    StateSyncOptions syncOptions =
        StateSyncOptionsBuilder.of(targetClient)
             .errorCallback((exception, newResourceDraft, oldResource, updateActions) -> {
               LOGGER.error(format(
                       "Error when trying to sync states. Existing state key: %s. Update actions: %s",
                       oldResource.map(State::getKey).orElse(""),
                       updateActions.stream()
                                    .map(Object::toString)
                                    .collect(Collectors.joining(","))
                       )
                       , exception);
             })
             .warningCallback((exception, newResourceDraft, oldResource) -> {
               LOGGER.warn(format(
                       "Warning when trying to sync states. Existing state key: %s",
                       oldResource.map(State::getKey).orElse("")
               ), exception);
             })
            .build();
    StateSync stateSync = new StateSync(syncOptions);
    CustomObjectService customObjectService = new CustomObjectServiceImpl(targetClient);
    return new StateSyncer(stateSync, sourceClient, targetClient, customObjectService, clock);
  }

  @Nonnull
  @Override
  protected CompletionStage<List<StateDraft>> transform(@Nonnull List<State> states) {
    return CompletableFuture.completedFuture(
            StateReferenceResolutionUtils.mapToStateDrafts(states));
  }

  @Nonnull
  @Override
  protected StateQuery getQuery() {
    return StateQuery.of().withExpansionPaths(StateExpansionModel::transitions);
  }
}
