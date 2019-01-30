package com.commercetools.project.sync.util;

import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.types.TypeSync;
import org.junit.jupiter.api.Test;

import static com.commercetools.project.sync.util.SyncUtils.APPLICATION_DEFAULT_NAME;
import static com.commercetools.project.sync.util.SyncUtils.APPLICATION_DEFAULT_VERSION;
import static com.commercetools.project.sync.util.SyncUtils.getApplicationName;
import static com.commercetools.project.sync.util.SyncUtils.getApplicationVersion;
import static com.commercetools.project.sync.util.SyncUtils.getSyncModuleName;
import static org.assertj.core.api.Assertions.assertThat;

class SyncUtilsTest {

  @Test
  void getSyncModuleName_WithProductSync_ShouldGetCorrectName() {

    final String syncModuleName = getSyncModuleName(ProductSync.class);

    assertThat(syncModuleName).isEqualTo("ProductSync");
  }

  @Test
  void getSyncModuleName_WithTypeSync_ShouldGetCorrectName() {

    final String syncModuleName = getSyncModuleName(TypeSync.class);

    assertThat(syncModuleName).isEqualTo("TypeSync");
  }

  @Test
  void getApplicationName_ShouldGetDefaultName() {

    final String applicationName = getApplicationName();

    assertThat(applicationName).isEqualTo(APPLICATION_DEFAULT_NAME);
  }

  @Test
  void getApplicationVersion_ShouldGetDefaultVersion() {

    final String applicationName = getApplicationVersion();

    assertThat(applicationName).isEqualTo(APPLICATION_DEFAULT_VERSION);
  }
}
