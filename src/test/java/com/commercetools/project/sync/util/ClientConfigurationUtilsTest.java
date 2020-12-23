package com.commercetools.project.sync.util;

import static org.assertj.core.api.Assertions.assertThat;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import org.junit.jupiter.api.Test;

class ClientConfigurationUtilsTest {

  @Test
  void createClient_WithConfig_ReturnsSphereClient() {
    final SphereClientConfig clientConfig =
        SphereClientConfig.of("project-key", "client-id", "client-secret");
    final SphereClient sphereClient = ClientConfigurationUtils.createClient(clientConfig);

    assertThat(sphereClient.getConfig().getProjectKey()).isEqualTo("project-key");
  }
}
