package com.commercetools.project.sync.util;

import static java.lang.String.format;

import com.commercetools.sync.commons.utils.ClientConfigurationUtils;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import java.io.InputStream;
import java.util.Properties;
import javax.annotation.Nonnull;

public final class SphereClientUtils {
  private static final String CTP_CREDENTIALS_PROPERTIES = "ctp.credentials.properties";
  private static final SphereClientConfig CTP_SOURCE_CLIENT_CONFIG = getCtpSourceClientConfig();
  private static final SphereClientConfig CTP_TARGET_CLIENT_CONFIG = getCtpTargetClientConfig();
  public static final SphereClient CTP_SOURCE_CLIENT =
      ClientConfigurationUtils.createClient(CTP_SOURCE_CLIENT_CONFIG);
  public static final SphereClient CTP_TARGET_CLIENT =
      ClientConfigurationUtils.createClient(CTP_TARGET_CLIENT_CONFIG);

  public static void closeCtpClients() {
    CTP_SOURCE_CLIENT.close();
    CTP_TARGET_CLIENT.close();
  }

  private static SphereClientConfig getCtpSourceClientConfig() {
    return getCtpClientConfig("source.", "SOURCE");
  }

  private static SphereClientConfig getCtpTargetClientConfig() {
    return getCtpClientConfig("target.", "TARGET");
  }

  private static SphereClientConfig getCtpClientConfig(
      @Nonnull final String propertiesPrefix, @Nonnull final String envVarPrefix) {
    try {
      final InputStream propStream =
          SphereClientUtils.class.getClassLoader().getResourceAsStream(CTP_CREDENTIALS_PROPERTIES);
      if (propStream != null) {
        final Properties ctpCredsProperties = new Properties();
        ctpCredsProperties.load(propStream);
        return SphereClientConfig.ofProperties(ctpCredsProperties, propertiesPrefix);
      }
    } catch (Exception exception) {
      throw new IllegalStateException(
          format(
              "CTP credentials file \"%s\" found, but CTP properties"
                  + " for prefix \"%s\" can't be read",
              CTP_CREDENTIALS_PROPERTIES, propertiesPrefix),
          exception);
    }

    return SphereClientConfig.ofEnvironmentVariables(envVarPrefix);
  }
}
