package com.commercetools.project.sync.util;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.commons.exceptions.SyncException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.ResourceView;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.slf4j.Logger;

public final class SyncUtils {

  public static final String APPLICATION_DEFAULT_NAME = "commercetools-project-sync";
  public static final String APPLICATION_DEFAULT_VERSION = "development-SNAPSHOT";

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

  public static <T extends ResourceView> void logErrorCallback(
      @Nonnull final Logger logger,
      @Nonnull final String resourceName,
      @Nonnull final SyncException exception,
      @Nonnull final String resourceIdentifier,
      @Nonnull final List<UpdateAction<T>> updateActions) {
    logger.error(
        format(
            "Error when trying to sync %s. Existing key: %s. Update actions: %s",
            resourceName,
            resourceIdentifier,
            updateActions.stream().map(Object::toString).collect(Collectors.joining(","))),
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

  private SyncUtils() {}
}
