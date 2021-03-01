package com.commercetools.project.sync.util;

import static com.commercetools.project.sync.service.impl.CustomObjectServiceImpl.TIMESTAMP_GENERATOR_KEY;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.commons.exceptions.SyncException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.models.ResourceView;
import io.sphere.sdk.models.WithKey;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

public final class SyncUtils {

  public static final String APPLICATION_DEFAULT_NAME = "commercetools-project-sync";
  public static final String APPLICATION_DEFAULT_VERSION = "development-SNAPSHOT";
  public static final String DEFAULT_RUNNER_NAME = "runnerName";
  public static final String IDENTIFIER_NOT_PRESENT = "<<not present>>";

  @Nonnull
  public static String getSyncModuleName(@Nonnull final Class<? extends BaseSync> syncClass) {
    return syncClass.getSimpleName();
  }

  @Nonnull
  public static String getApplicationName() {
    final String implementationTitle = SyncUtils.class.getPackage().getImplementationTitle();
    return isBlank(implementationTitle) ? APPLICATION_DEFAULT_NAME : implementationTitle;
  }

  @Nonnull
  public static String getApplicationVersion() {
    final String implementationVersion = SyncUtils.class.getPackage().getImplementationVersion();
    return isBlank(implementationVersion) ? APPLICATION_DEFAULT_VERSION : implementationVersion;
  }

  public static <T extends WithKey> void logErrorCallback(
      @Nonnull final Logger logger,
      @Nonnull final String resourceName,
      @Nonnull final SyncException exception,
      @Nonnull final Optional<T> resource,
      @Nullable final List<UpdateAction<T>> updateActions) {
    String updateActionsString = "[]";
    if (updateActions != null) {
      updateActionsString =
          updateActions.stream().map(Object::toString).collect(Collectors.joining(","));
    }
    logger.error(
        format(
            "Error when trying to sync %s. Existing key: %s. Update actions: %s",
            resourceName,
            resource.map(WithKey::getKey).orElse(IDENTIFIER_NOT_PRESENT),
            updateActionsString),
        exception);
  }

  public static <T extends ResourceView> void logErrorCallback(
      @Nonnull final Logger logger,
      @Nonnull final String resourceName,
      @Nonnull final SyncException exception,
      @Nonnull final String resourceIdentifier,
      @Nullable final List<UpdateAction<T>> updateActions) {
    String updateActionsString = "[]";
    if (updateActions != null) {
      updateActionsString =
          updateActions.stream().map(Object::toString).collect(Collectors.joining(","));
    }
    logger.error(
        format(
            "Error when trying to sync %s. Existing key: %s. Update actions: %s",
            resourceName, resourceIdentifier, updateActionsString),
        exception);
  }

  public static <T extends WithKey> void logWarningCallback(
      @Nonnull final Logger logger,
      @Nonnull final String resourceName,
      @Nonnull final SyncException exception,
      @Nonnull final Optional<T> resource) {
    logger.warn(
        format(
            "Warning when trying to sync %s. Existing key: %s",
            resourceName, resource.map(WithKey::getKey).orElse(IDENTIFIER_NOT_PRESENT)),
        exception);
  }

  public static void logWarningCallback(
      @Nonnull final Logger logger,
      @Nonnull final String resourceName,
      @Nonnull final SyncException exception,
      @Nonnull final String resourceIdentifier) {
    logger.warn(
        format(
            "Warning when trying to sync %s. Existing key: %s", resourceName, resourceIdentifier),
        exception);
  }

  @Nonnull
  public static String buildLastSyncTimestampContainerName(
      @Nonnull final String syncModuleName, @Nullable final String runnerName) {

    final String syncModuleNameWithLowerCasedFirstChar = StringUtils.uncapitalize(syncModuleName);
    return format(
        "%s.%s.%s",
        getApplicationName(),
        getRunnerNameValue(runnerName),
        syncModuleNameWithLowerCasedFirstChar);
  }

  public static String buildCurrentCtpTimestampContainerName(
      @Nonnull final String syncModuleName, @Nullable final String runnerName) {
    return format(
        "%s.%s.%s.%s",
        getApplicationName(),
        getRunnerNameValue(runnerName),
        syncModuleName,
        TIMESTAMP_GENERATOR_KEY);
  }

  @Nonnull
  private static String getRunnerNameValue(@Nullable final String runnerName) {
    return ofNullable(runnerName).filter(StringUtils::isNotBlank).orElse(DEFAULT_RUNNER_NAME);
  }

  /**
   * Given a reference to a resource of type {@code T}, this method checks if the reference is found
   * in cache. If it is, then it return the resource identifier with key. Otherwise, it returns the
   * resource identifier with id. Since, the reference could be {@code null}, this method could also
   * return null if the reference was not expanded and cached.
   *
   * @param reference the reference of the resource to check if it's expanded.
   * @param <T> the type of the resource.
   * @param referenceIdToKeyMap the map containing the cached id to key values.
   * @return returns the resource identifier with key if the {@code reference} was found in cache.
   *     Otherwise, it returns the resource identifier with id.
   */
  @Nullable
  public static <T extends WithKey> ResourceIdentifier<T> getResourceIdentifierWithKey(
      @Nullable final Reference<T> reference, Map<String, String> referenceIdToKeyMap) {

    if (reference != null) {
      final String id = reference.getId();
      if (referenceIdToKeyMap.containsKey(id)) {
        return ResourceIdentifier.ofKey(referenceIdToKeyMap.get(id));
      }
      return ResourceIdentifier.ofId(id);
    }

    return null;
  }

  private SyncUtils() {}
}
