package com.commercetools.project.sync.service;

import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.project.sync.model.response.LastSyncCustomObject;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface CustomObjectService {
  @Nonnull
  CompletionStage<ZonedDateTime> getCurrentCtpTimestamp(
      @Nullable final String runnerName, @Nonnull final String syncModuleName);

  @Nonnull
  CompletionStage<Optional<LastSyncCustomObject>> getLastSyncCustomObject(
      @Nonnull final String sourceProjectKey,
      @Nonnull final String syncModuleName,
      @Nullable final String runnerName);

  @Nonnull
  CompletableFuture<ApiHttpResponse<CustomObject>> createLastSyncCustomObject(
      @Nonnull final String sourceProjectKey,
      @Nonnull final String syncModuleName,
      @Nullable final String runnerName,
      @Nonnull final LastSyncCustomObject lastSyncCustomObject);
}
