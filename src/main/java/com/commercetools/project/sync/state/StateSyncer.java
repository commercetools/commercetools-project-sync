package com.commercetools.project.sync.state;

import static com.commercetools.project.sync.util.SyncUtils.logErrorCallback;
import static com.commercetools.project.sync.util.SyncUtils.logWarningCallback;
import static com.commercetools.sync.states.utils.StateTransformUtils.toStateDrafts;

import com.commercetools.project.sync.Syncer;
import com.commercetools.project.sync.service.CustomObjectService;
import com.commercetools.project.sync.service.impl.CustomObjectServiceImpl;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.states.StateSync;
import com.commercetools.sync.states.StateSyncOptions;
import com.commercetools.sync.states.StateSyncOptionsBuilder;
import com.commercetools.sync.states.helpers.StateSyncStatistics;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.queries.StateQuery;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StateSyncer
    extends Syncer<
        State, State, StateDraft, StateSyncStatistics, StateSyncOptions, StateQuery, StateSync> {

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
    final QuadConsumer<
            SyncException, Optional<StateDraft>, Optional<State>, List<UpdateAction<State>>>
        logErrorCallback =
            (exception, newResourceDraft, oldResource, updateActions) ->
                logErrorCallback(LOGGER, "state", exception, oldResource, updateActions);
    final TriConsumer<SyncException, Optional<StateDraft>, Optional<State>> logWarningCallback =
        (exception, newResourceDraft, oldResource) ->
            logWarningCallback(LOGGER, "state", exception, oldResource);
    StateSyncOptions syncOptions =
        StateSyncOptionsBuilder.of(targetClient)
            .errorCallback(logErrorCallback)
            .warningCallback(logWarningCallback)
            .build();
    StateSync stateSync = new StateSync(syncOptions);
    CustomObjectService customObjectService = new CustomObjectServiceImpl(targetClient);
    return new StateSyncer(stateSync, sourceClient, targetClient, customObjectService, clock);
  }

  @Nonnull
  @Override
  protected CompletionStage<List<StateDraft>> transform(@Nonnull List<State> states) {
    return toStateDrafts(getSourceClient(), referenceIdToKeyCache, states);
  }

  @Nonnull
  @Override
  protected StateQuery getQuery() {
    return StateQuery.of();
  }

  @Nonnull
  @Override
  protected Logger getLoggerInstance() {
    return LOGGER;
  }
}
