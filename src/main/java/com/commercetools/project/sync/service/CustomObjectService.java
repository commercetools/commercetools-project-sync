package com.commercetools.project.sync.service;

import com.commercetools.project.sync.model.LastSyncCustomObject;
import io.sphere.sdk.customobjects.CustomObject;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;

public interface CustomObjectService {
  @Nonnull
  CompletionStage<ZonedDateTime> getCurrentCtpTimestamp();

  @Nonnull
  CompletionStage<Optional<CustomObject<LastSyncCustomObject>>> getLastSyncCustomObject(
      @Nonnull final String sourceProjectKey, @Nonnull final String syncModuleName);

  @Nonnull
  CompletionStage<CustomObject<LastSyncCustomObject>> createLastSyncCustomObject(
      @Nonnull final String sourceProjectKey,
      @Nonnull final String syncModuleName,
      @Nonnull final LastSyncCustomObject lastSyncCustomObject);

  CustomObjectService attachRunnerName(String runnerName);
}
