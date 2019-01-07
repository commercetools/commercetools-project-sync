package com.commercetools.project.sync.util;

import static com.commercetools.project.sync.util.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.project.sync.util.SphereClientUtils.CTP_TARGET_CLIENT;
import static org.assertj.core.api.Assertions.assertThat;

import io.sphere.sdk.client.SphereClient;
import org.junit.jupiter.api.Test;

class SphereClientUtilsTest {
  @Test
  void getCtpSourceClientConfig_WithCredentialsInPropertiesFile_ShouldCreateSourceClient() {
    final SphereClient ctpSourceClient = CTP_SOURCE_CLIENT;
    assertThat(ctpSourceClient).isNotNull();
    assertThat(ctpSourceClient.getConfig().getProjectKey()).isEqualTo("testSourceProjectKey");
  }

  @Test
  void getCtpTargetClientConfig_WithCredentialsInPropertiesFile_ShouldCreateTargetClient() {
    final SphereClient ctpTargetClient = CTP_TARGET_CLIENT;
    assertThat(ctpTargetClient).isNotNull();
    assertThat(ctpTargetClient.getConfig().getProjectKey()).isEqualTo("testTargetProjectKey");
  }
}
