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

  /**
   * Creates or updates a custom object with the container named 'commercetools-project-sync.{@param
   * runnerName}.{@param syncModuleName}.timestampGenerator' and key equals 'timestampGenerator' and
   * then reading the 'lastModifiedAt' field of the persisted custom object and returning it.
   *
   * @param syncModuleName the name of the resource being synced. E.g. productSync, categorySync,
   *     etc..
   * @param runnerName the name of this specific running sync instance defined by the user.
   * @return a {@link CompletionStage} containing the current CTP timestamp as {@link
   *     ZonedDateTime}.
   */
  @Nonnull
  CompletionStage<ZonedDateTime> getCurrentCtpTimestamp(
      @Nullable final String runnerName, @Nonnull final String syncModuleName);

  /**
   * Get's a custom object which has a container named 'commercetools-project-sync.{@param
   * runnerName}.{@param syncModuleName}' and key equals {@param sourceProjectKey}. The value of the
   * fetched custom object is deserialized and wrapped in an {@link Optional}.
   *
   * @param sourceProjectKey the source project from which the data is coming.
   * @param syncModuleName the name of the resource being synced. E.g. productSync, categorySync,
   *     etc..
   * @param runnerName the name of this specific running sync instance defined by the user.
   * @return the custom object with container 'commercetools-project-sync.{@param
   *     runnerName}.{@param syncModuleName}' and key '{@param sourceProjectKey}', wrapped in an
   *     {@link Optional} as a result of a {@link CompletionStage}.
   */
  @Nonnull
  CompletionStage<Optional<LastSyncCustomObject>> getLastSyncCustomObject(
      @Nonnull final String sourceProjectKey,
      @Nonnull final String syncModuleName,
      @Nullable final String runnerName);

  /**
   * Creates (or updates an already existing) custom object, with the container named
   * 'commercetools-project-sync.{@param runnerName}.{@param syncModuleName}' and key equals {@param
   * sourceProjectKey}, enriched with the information in the passed {@link LastSyncCustomObject}
   * param.
   *
   * @param sourceProjectKey the source project key from which the data is coming.
   * @param syncModuleName the name of the resource being synced. E.g. productSync, categorySync,
   *     etc..
   * @param runnerName the name of this specific running sync instance defined by the user.
   * @param lastSyncCustomObject contains information about the last sync instance.
   * @return a {@link CompletableFuture} of {@link ApiHttpResponse} with the created/updated custom
   *     object resource.
   */
  @Nonnull
  CompletableFuture<ApiHttpResponse<CustomObject>> createLastSyncCustomObject(
      @Nonnull final String sourceProjectKey,
      @Nonnull final String syncModuleName,
      @Nullable final String runnerName,
      @Nonnull final LastSyncCustomObject lastSyncCustomObject);
}
