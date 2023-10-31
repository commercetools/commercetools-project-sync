package com.commercetools.project.sync.state;

import static com.commercetools.project.sync.util.SyncUtils.logErrorCallback;
import static com.commercetools.project.sync.util.SyncUtils.logWarningCallback;
import static com.commercetools.sync.states.utils.StateTransformUtils.toStateDrafts;

import com.commercetools.api.client.ByProjectKeyStatesGet;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.state.State;
import com.commercetools.api.models.state.StateDraft;
import com.commercetools.api.models.state.StatePagedQueryResponse;
import com.commercetools.api.models.state.StateUpdateAction;
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
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StateSyncer
    extends Syncer<
        State,
        StateUpdateAction,
        StateDraft,
        StateSyncStatistics,
        StateSyncOptions,
        ByProjectKeyStatesGet,
        StatePagedQueryResponse,
        StateSync> {

  private static final Logger LOGGER = LoggerFactory.getLogger(StateSyncer.class);

  private StateSyncer(
      @Nonnull StateSync sync,
      @Nonnull ProjectApiRoot sourceClient,
      @Nonnull ProjectApiRoot targetClient,
      @Nonnull CustomObjectService customObjectService,
      @Nonnull Clock clock) {
    super(sync, sourceClient, targetClient, customObjectService, clock);
  }

  public static StateSyncer of(
      @Nonnull final ProjectApiRoot sourceClient,
      @Nonnull final ProjectApiRoot targetClient,
      @Nonnull final Clock clock) {
    final QuadConsumer<
            SyncException, Optional<StateDraft>, Optional<State>, List<StateUpdateAction>>
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
    return toStateDrafts(getSourceClient(), referenceIdToKeyCache, states)
        .handle(
            (stateDrafts, throwable) -> {
              if (throwable != null) {
                if (LOGGER.isWarnEnabled()) {
                  LOGGER.warn(throwable.getMessage(), getCompletionExceptionCause(throwable));
                }
                return List.of();
              }
              return stateDrafts;
            });
  }

  @Nonnull
  private static Throwable getCompletionExceptionCause(@Nonnull final Throwable exception) {
    if (exception instanceof CompletionException) {
      return getCompletionExceptionCause(exception.getCause());
    }
    return exception;
  }

  @Nonnull
  @Override
  protected ByProjectKeyStatesGet getQuery() {
    return getSourceClient().states().get();
  }

  @Nonnull
  @Override
  protected Logger getLoggerInstance() {
    return LOGGER;
  }
}
