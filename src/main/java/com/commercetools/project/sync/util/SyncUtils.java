package com.commercetools.project.sync.util;

import static com.commercetools.project.sync.service.impl.CustomObjectServiceImpl.TIMESTAMP_GENERATOR_KEY;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.api.models.ResourceUpdateAction;
import com.commercetools.api.models.WithKey;
import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.commons.exceptions.SyncException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

// This class compiles but not tested yet
// TODO: Test class and adjust logic if needed
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

  public static <T extends WithKey, U extends ResourceUpdateAction> void logErrorCallback(
      @Nonnull final Logger logger,
      @Nonnull final String resourceName,
      @Nonnull final SyncException exception,
      @Nonnull final Optional<T> resource,
      @Nullable final List<U> updateActions) {
    String updateActionsString = "[]";
    if (updateActions != null && !updateActions.isEmpty()) {
      updateActionsString =
          updateActions.stream().map(Object::toString).collect(Collectors.joining(","));
    }
    if (logger.isErrorEnabled()) {
      logger.error(
          format(
              "Error when trying to sync %s. Existing key: %s. Update actions: %s",
              resourceName,
              resource.map(WithKey::getKey).orElse(IDENTIFIER_NOT_PRESENT),
              updateActionsString),
          exception);
    }
  }

  public static <U extends ResourceUpdateAction> void logErrorCallback(
      @Nonnull final Logger logger,
      @Nonnull final String resourceName,
      @Nonnull final SyncException exception,
      @Nonnull final String resourceIdentifier,
      @Nullable final List<U> updateActions) {
    String updateActionsString = "[]";
    if (updateActions != null && !updateActions.isEmpty()) {
      updateActionsString =
          updateActions.stream().map(Object::toString).collect(Collectors.joining(","));
    }
    if (logger.isErrorEnabled()) {
      logger.error(
          format(
              "Error when trying to sync %s. Existing key: %s. Update actions: %s",
              resourceName, resourceIdentifier, updateActionsString),
          exception);
    }
  }

  public static <T extends WithKey> void logWarningCallback(
      @Nonnull final Logger logger,
      @Nonnull final String resourceName,
      @Nonnull final SyncException exception,
      @Nonnull final Optional<T> resource) {
    if (logger.isWarnEnabled()) {
      logger.warn(
          format(
              "Warning when trying to sync %s. Existing key: %s",
              resourceName, resource.map(WithKey::getKey).orElse(IDENTIFIER_NOT_PRESENT)),
          exception);
    }
  }

  public static void logWarningCallback(
      @Nonnull final Logger logger,
      @Nonnull final String resourceName,
      @Nonnull final SyncException exception,
      @Nonnull final String resourceIdentifier) {
    if (logger.isWarnEnabled()) {
      logger.warn(
          format(
              "Warning when trying to sync %s. Existing key: %s", resourceName, resourceIdentifier),
          exception);
    }
  }

  @Nonnull
  public static Throwable getCompletionExceptionCause(@Nonnull final Throwable exception) {
    if (exception instanceof CompletionException) {
      return getCompletionExceptionCause(exception.getCause());
    }
    return exception;
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

  private SyncUtils() {}
}
