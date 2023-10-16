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

  @Nonnull
  @Override
  public CompletableFuture<ZonedDateTime> getCurrentCtpTimestamp(
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

  /**
   * Helper to create a custom object of {@param customObjectDraft} on the CTP project defined by
   * the {@code ctpClient}.
   *
   * @param customObjectDraft draft of custom object to create
   * @return a {@link CompletableFuture} of {@link ApiHttpResponse} with the created custom object
   *     resource.
   */
  @Nonnull
  private CompletableFuture<ApiHttpResponse<CustomObject>> createCustomObject(
      @Nonnull final CustomObjectDraft customObjectDraft) {
    return this.ctpClient.customObjects().post(customObjectDraft).execute();
  }

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
        .thenApply(
            (customObjectApiHttpResponse) -> {
              final CustomObject responseBody = customObjectApiHttpResponse.getBody();
              final ObjectMapper objectMapper = JsonUtils.getConfiguredObjectMapper();
              final LastSyncCustomObject lastSyncCustomObject =
                  responseBody == null
                      ? null
                      : objectMapper.convertValue(
                          responseBody.getValue(), LastSyncCustomObject.class);
              return Optional.ofNullable(lastSyncCustomObject);
            });
  }

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
