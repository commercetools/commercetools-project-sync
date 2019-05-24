package com.commercetools.project.sync.service.impl;

import static com.commercetools.project.sync.util.SyncUtils.getApplicationName;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;

import com.commercetools.project.sync.model.LastSyncCustomObject;
import com.commercetools.project.sync.service.CustomObjectService;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;
import io.sphere.sdk.customobjects.commands.CustomObjectUpsertCommand;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.models.ResourceView;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.queries.QueryPredicate;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

public class CustomObjectServiceImpl implements CustomObjectService {

  public static final String TIMESTAMP_GENERATOR_KEY = "timestampGenerator";
  public static final String TIMESTAMP_GENERATOR_VALUE = "";
  public static final String DEFAULT_RUNNER_NAME = "runnerName";
  private static final long MINUTES_BEFORE_CURRENT_TIMESTAMP = 2;

  private SphereClient sphereClient;

  public CustomObjectServiceImpl(@Nonnull final SphereClient sphereClient) {
    this.sphereClient = sphereClient;
  }

  @Nonnull
  @Override
  public CompletionStage<ZonedDateTime> getCurrentCtpTimestamp(
      @Nullable final String runnerName, @Nonnull final String syncModuleName) {

    final String container =
        format(
            "%s.%s.%s.%s",
            getApplicationName(),
            getRunnerNameValue(runnerName),
            syncModuleName,
            TIMESTAMP_GENERATOR_KEY);

    final CustomObjectDraft<String> currentTimestampDraft =
        CustomObjectDraft.ofUnversionedUpsert(
            container, TIMESTAMP_GENERATOR_KEY, TIMESTAMP_GENERATOR_VALUE, String.class);

    return createCustomObject(currentTimestampDraft)
        .thenApply(ResourceView::getLastModifiedAt)
        .thenApply(lastModifiedAt -> lastModifiedAt.minusMinutes(MINUTES_BEFORE_CURRENT_TIMESTAMP));
  }

  @Nonnull
  private static String getRunnerNameValue(@Nullable final String runnerName) {
    return ofNullable(runnerName).filter(StringUtils::isNotBlank).orElse(DEFAULT_RUNNER_NAME);
  }

  @Nonnull
  private <T> CompletionStage<CustomObject<T>> createCustomObject(
      @Nonnull final CustomObjectDraft<T> customObjectDraft) {
    return sphereClient.execute(CustomObjectUpsertCommand.of(customObjectDraft));
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<CustomObject<LastSyncCustomObject>>> getLastSyncCustomObject(
      @Nonnull final String sourceProjectKey,
      @Nonnull final String syncModuleName,
      @Nullable final String runnerName) {

    final QueryPredicate<CustomObject<LastSyncCustomObject>> queryPredicate =
        QueryPredicate.of(
            format(
                "container=\"%s\" AND key=\"%s\"",
                buildLastSyncTimestampContainerName(syncModuleName, getRunnerNameValue(runnerName)),
                sourceProjectKey));

    return sphereClient
        .execute(CustomObjectQuery.of(LastSyncCustomObject.class).plusPredicates(queryPredicate))
        .thenApply(PagedQueryResult::getResults)
        .thenApply(Collection::stream)
        .thenApply(Stream::findFirst);
  }

  @Nonnull
  @Override
  public CompletionStage<CustomObject<LastSyncCustomObject>> createLastSyncCustomObject(
      @Nonnull final String sourceProjectKey,
      @Nonnull final String syncModuleName,
      @Nullable final String runnerName,
      @Nonnull final LastSyncCustomObject lastSyncCustomObject) {

    final CustomObjectDraft<LastSyncCustomObject> lastSyncCustomObjectDraft =
        CustomObjectDraft.ofUnversionedUpsert(
            buildLastSyncTimestampContainerName(syncModuleName, getRunnerNameValue(runnerName)),
            sourceProjectKey,
            lastSyncCustomObject,
            LastSyncCustomObject.class);

    return createCustomObject(lastSyncCustomObjectDraft);
  }

  @Nonnull
  private String buildLastSyncTimestampContainerName(
      @Nonnull final String syncModuleName, @Nonnull final String runnerName) {

    final String syncModuleNameWithLowerCasedFirstChar = lowerCaseFirstChar(syncModuleName);
    return format(
        "%s.%s.%s", getApplicationName(), runnerName, syncModuleNameWithLowerCasedFirstChar);
  }

  @Nonnull
  private String lowerCaseFirstChar(@Nonnull final String string) {

    final char firstChar = string.charAt(0);
    final char lowerCasedFirstChar = Character.toLowerCase(firstChar);
    final String stringWithoutFirstChar = string.substring(1);
    return format("%s%s", lowerCasedFirstChar, stringWithoutFirstChar);
  }
}
