package com.commercetools.project.sync.util;

import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.types.TypeSync;
import org.junit.jupiter.api.Test;

import static com.commercetools.project.sync.util.SyncUtils.getSyncModuleName;
import static org.assertj.core.api.Assertions.assertThat;

class SyncUtilsTest {

  @Test
  void getSyncModuleName_WithProductSync_ShouldBuildCorrectName() {

    final String syncModuleName = getSyncModuleName(ProductSync.class);

    assertThat(syncModuleName).isEqualTo("ProductSync");
  }

  @Test
  void getSyncModuleName_WithTypeSync_ShouldBuildCorrectName() {

    final String syncModuleName = getSyncModuleName(TypeSync.class);

    assertThat(syncModuleName).isEqualTo("TypeSync");
  }
}
