package com.commercetools.project.sync.util;

import static java.lang.String.format;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.defaultconfig.ServiceRegion;
import com.commercetools.sync.commons.utils.ClientConfigurationUtils;
import io.vrap.rmf.base.client.oauth2.ClientCredentials;
import java.io.InputStream;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import javax.annotation.Nonnull;

public final class CtpClientUtils {
  private static final String CTP_CREDENTIALS_PROPERTIES = "ctp.credentials.properties";
  public static final String PROPERTIES_KEY_API_URL_SUFFIX = "apiUrl";
  public static final String PROPERTIES_KEY_AUTH_URL_SUFFIX = "authUrl";
  public static final String PROPERTIES_KEY_PROJECT_KEY_SUFFIX = "projectKey";
  public static final String PROPERTIES_KEY_CLIENT_ID_SUFFIX = "clientId";
  public static final String PROPERTIES_KEY_CLIENT_SECRET_SUFFIX = "clientSecret";
  public static final String PROPERTIES_KEY_SCOPES_SUFFIX = "scopes";

  public static final ProjectApiRoot CTP_SOURCE_CLIENT = getCtpSourceClient();
  public static final ProjectApiRoot CTP_TARGET_CLIENT = getCtpTargetClient();

  private static ProjectApiRoot getCtpSourceClient() {
    return getCtpClient("source.");
  }

  private static ProjectApiRoot getCtpTargetClient() {
    return getCtpClient("target.");
  }

  private static ProjectApiRoot getCtpClient(@Nonnull final String propertiesPrefix) {
    try {
      InputStream propStream =
          CtpClientUtils.class.getClassLoader().getResourceAsStream(CTP_CREDENTIALS_PROPERTIES);
      Properties properties = new Properties();
      if (propStream != null) {
        properties.load(propStream);
      }
      if (properties.isEmpty()) {
        properties = loadFromEnvVars(propertiesPrefix);
      }
      if (properties.isEmpty()) {
        throw new InvalidPropertiesFormatException("Please provide CTP credentials for running project sync.");
      }

      final String projectKey =
          extract(properties, propertiesPrefix, PROPERTIES_KEY_PROJECT_KEY_SUFFIX);
      final String clientId =
          extract(properties, propertiesPrefix, PROPERTIES_KEY_CLIENT_ID_SUFFIX);
      final String clientSecret =
          extract(properties, propertiesPrefix, PROPERTIES_KEY_CLIENT_SECRET_SUFFIX);
       final String apiUrl =
          extract(
              properties,
              propertiesPrefix,
              PROPERTIES_KEY_API_URL_SUFFIX,
              ServiceRegion.GCP_EUROPE_WEST1.getApiUrl());

      final String authUrl =
          extract(
              properties,
              propertiesPrefix,
              PROPERTIES_KEY_AUTH_URL_SUFFIX,
              ServiceRegion.GCP_EUROPE_WEST1.getOAuthTokenUrl());
      final String scopes =
          extract(
              properties,
              propertiesPrefix,
              PROPERTIES_KEY_SCOPES_SUFFIX,
              "manage_project:" + projectKey);

      final ClientCredentials credentials =
          ClientCredentials.of()
              .withClientId(clientId)
              .withClientSecret(clientSecret)
              .withScopes(scopes)
              .build();

      return ClientConfigurationUtils.createClient(projectKey, credentials, authUrl, apiUrl);
    } catch (Exception exception) {
      throw new IllegalStateException(
          format(
              "IT properties file \"%s\" found, but CTP properties"
                  + " for prefix \"%s\" can't be read",
              CTP_CREDENTIALS_PROPERTIES, propertiesPrefix),
          exception);
    }
  }

  private static Properties loadFromEnvVars(String propertiesPrefix) {
    String projectKeyKey = propertiesPrefix.toUpperCase().replace(".", "_") + "PROJECT_KEY";
    String projectKey = System.getenv(projectKeyKey);
    String clientIdKey = propertiesPrefix.toUpperCase().replace(".", "_") + "CLIENT_ID";
    String clientId = System.getenv(clientIdKey);
    String clientSecretKey = propertiesPrefix.toUpperCase().replace(".", "_") + "CLIENT_SECRET";
    String clientSecret = System.getenv(clientSecretKey);
    Properties properties = new Properties();
    properties.put(propertiesPrefix + PROPERTIES_KEY_PROJECT_KEY_SUFFIX, projectKey);
    properties.put(propertiesPrefix + PROPERTIES_KEY_CLIENT_ID_SUFFIX, clientId);
    properties.put(propertiesPrefix + PROPERTIES_KEY_CLIENT_SECRET_SUFFIX, clientSecret);
    return properties;
  }

  private static String extract(
      final Properties properties,
      final String prefix,
      final String suffix,
      final String defaultValue) {
    return properties.getProperty(buildPropKey(prefix, suffix), defaultValue);
  }

  private static String extract(
      final Properties properties, final String prefix, final String suffix) {
    final String mapKey = buildPropKey(prefix, suffix);
    return properties
        .computeIfAbsent(mapKey, key -> throwPropertiesException(prefix, mapKey))
        .toString();
  }

  private static String buildPropKey(final String prefix, final String suffix) {
    return prefix + suffix;
  }

  private static String throwPropertiesException(final String prefix, final String missingKey) {
    throw new IllegalArgumentException(
        "Missing property value '"
            + missingKey
            + "'.\n"
            + "Usage:\n"
            + ""
            + buildPropKey(prefix, PROPERTIES_KEY_PROJECT_KEY_SUFFIX)
            + "=YOUR project key\n"
            + ""
            + buildPropKey(prefix, PROPERTIES_KEY_CLIENT_ID_SUFFIX)
            + "=YOUR client id\n"
            + ""
            + buildPropKey(prefix, PROPERTIES_KEY_CLIENT_SECRET_SUFFIX)
            + "=YOUR client secret\n"
            + "#optional:\n"
            + ""
            + buildPropKey(prefix, PROPERTIES_KEY_API_URL_SUFFIX)
            + "=https://api.europe-west1.gcp.commercetools.com\n"
            + ""
            + buildPropKey(prefix, PROPERTIES_KEY_AUTH_URL_SUFFIX)
            + "=https://auth.europe-west1.gcp.commercetools.com\n"
            + ""
            + buildPropKey(prefix, PROPERTIES_KEY_SCOPES_SUFFIX)
            + "=manage_project"
            + "#don't use quotes for the property values\n");
  }

  private CtpClientUtils() {}
}
