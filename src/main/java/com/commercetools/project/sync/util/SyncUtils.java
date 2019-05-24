package com.commercetools.project.sync.util;

import com.commercetools.sync.commons.BaseSync;

import javax.annotation.Nonnull;

import static org.apache.commons.lang3.StringUtils.isBlank;

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

  private SyncUtils() {}
}
