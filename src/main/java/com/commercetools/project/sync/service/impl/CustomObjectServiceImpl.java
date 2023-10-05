package com.commercetools.project.sync.service.impl;

import static com.commercetools.project.sync.util.SyncUtils.buildCurrentCtpTimestampContainerName;
import static com.commercetools.project.sync.util.SyncUtils.buildLastSyncTimestampContainerName;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.api.models.custom_object.CustomObjectDraft;
import com.commercetools.api.models.custom_object.CustomObjectDraftBuilder;
import com.commercetools.project.sync.model.response.LastSyncCustomObject;
import com.commercetools.project.sync.service.CustomObjectService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.utils.json.JsonUtils;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

// This class compiles but not tested yet
// TODO: Test class and adjust logic if needed
public class CustomObjectServiceImpl implements CustomObjectService {

  public static final String TIMESTAMP_GENERATOR_KEY = "timestampGenerator";
  private final ProjectApiRoot ctpClient;

  public CustomObjectServiceImpl(@Nonnull final ProjectApiRoot ctpClient) {
    this.ctpClient = ctpClient;
  }

  /**
   * Gets the current timestamp of CTP by creating/updating a custom object with the container:
   * 'commercetools-project-sync.{@code runnerName}.{@code syncModuleName}.timestampGenerator' and
   * key: 'timestampGenerator' and then returning the lastModifiedAt of this created/updated custom
   * object.
   *
   * @param syncModuleName the name of the resource being synced. E.g. productSync, categorySync,
   *     etc..
   * @param runnerName the name of this specific running sync instance defined by the user.
   * @return a {@link CompletionStage} containing the current CTP timestamp as {@link
   *     ZonedDateTime}.
   */
  @Nonnull
  @Override
  public CompletionStage<ZonedDateTime> getCurrentCtpTimestamp(
      @Nullable final String runnerName, @Nonnull final String syncModuleName) {

    final String container = buildCurrentCtpTimestampContainerName(syncModuleName, runnerName);

    final CustomObjectDraft currentTimestampDraft =
        CustomObjectDraftBuilder.of()
            .container(container)
            .key(TIMESTAMP_GENERATOR_KEY)
            .value(UUID.randomUUID().toString())
            .build();

    return createCustomObject(currentTimestampDraft)
        .thenApply(ApiHttpResponse::getBody)
        .thenApply(CustomObject::getLastModifiedAt);
  }

  @Nonnull
  private CompletableFuture<ApiHttpResponse<CustomObject>> createCustomObject(
      @Nonnull final CustomObjectDraft customObjectDraft) {
    return this.ctpClient.customObjects().post(customObjectDraft).execute();
  }

  /**
   * Queries for custom objects, on the CTP project defined by the {@code sphereClient}, which have
   * a container: 'commercetools-project-sync.{@code runnerName}.{@code syncModuleName}' and key:
   * {@code sourceProjectKey}. The method then returns the first custom object returned in the
   * result set if there is, wrapped in an {@link Optional} as a result of a {@link
   * CompletionStage}. It will be, at most, one custom object since the key is unique per custom
   * object container as per CTP documentation.
   *
   * @param sourceProjectKey the source project from which the data is coming.
   * @param syncModuleName the name of the resource being synced. E.g. productSync, categorySync,
   *     etc..
   * @param runnerName the name of this specific running sync instance defined by the user.
   * @return the first custom object returned in the result set if there is, wrapped in an {@link
   *     Optional} as a result of a {@link CompletionStage}. It will be, at most, one custom object
   *     since the key is unique per custom object container as per CTP documentation.
   */
  @Nonnull
  @Override
  public CompletableFuture<Optional<LastSyncCustomObject>> getLastSyncCustomObject(
      @Nonnull final String sourceProjectKey,
      @Nonnull final String syncModuleName,
      @Nullable final String runnerName) {

    final String containerName = buildLastSyncTimestampContainerName(syncModuleName, runnerName);

    return this.ctpClient
        .customObjects()
        .withContainerAndKey(containerName, sourceProjectKey)
        .get()
        .execute()
        .handle(
            (customObjectApiHttpResponse, throwable) -> {
              if (throwable != null) {
                return Optional.empty();
              } else {
                final CustomObject responseBody = customObjectApiHttpResponse.getBody();
                final ObjectMapper objectMapper = JsonUtils.getConfiguredObjectMapper();
                final LastSyncCustomObject lastSyncCustomObject =
                    responseBody == null
                        ? null
                        : objectMapper.convertValue(
                            responseBody.getValue(), LastSyncCustomObject.class);
                return Optional.ofNullable(lastSyncCustomObject);
              }
            });
  }

  /**
   * Creates (or updates an already existing) custom object, with the container:
   * 'commercetools-project-sync.{@code runnerName}.{@code syncModuleName}' and key: {@code
   * sourceProjectKey}, enriched with the information in the passed {@link LastSyncCustomObject}
   * param.
   *
   * @param sourceProjectKey the source project from which the data is coming.
   * @param syncModuleName the name of the resource being synced. E.g. productSync, categorySync,
   *     etc..
   * @param runnerName the name of this specific running sync instance defined by the user.
   * @param lastSyncCustomObject contains information about the last sync instance.
   * @return the first custom object returned in the result set if there is, wrapped in an {@link
   *     Optional} as a result of a {@link CompletionStage}. It will be, at most, one custom object
   *     since the key is unique per custom object container as per CTP documentation.
   */
  @Nonnull
  @Override
  public CompletableFuture<ApiHttpResponse<CustomObject>> createLastSyncCustomObject(
      @Nonnull final String sourceProjectKey,
      @Nonnull final String syncModuleName,
      @Nullable final String runnerName,
      @Nonnull final LastSyncCustomObject lastSyncCustomObject) {

    final CustomObjectDraft lastSyncCustomObjectDraft =
        CustomObjectDraftBuilder.of()
            .container(buildLastSyncTimestampContainerName(syncModuleName, runnerName))
            .key(sourceProjectKey)
            .value(lastSyncCustomObject)
            .build();

    return createCustomObject(lastSyncCustomObjectDraft);
  }
}
