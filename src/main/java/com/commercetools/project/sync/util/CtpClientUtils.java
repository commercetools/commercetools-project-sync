package com.commercetools.project.sync.util;

import static java.lang.String.format;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.defaultconfig.ApiRootBuilder;
import com.commercetools.api.defaultconfig.ServiceRegion;
import com.commercetools.api.json.ApiModuleOptions;
import com.commercetools.http.okhttp4.CtOkHttp4Client;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vrap.rmf.base.client.ResponseSerializer;
import io.vrap.rmf.base.client.oauth2.ClientCredentials;
import io.vrap.rmf.base.client.utils.json.JsonUtils;
import java.io.InputStream;
import java.util.Arrays;
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
        throw new InvalidPropertiesFormatException(
            "Please provide CTP credentials for running project sync.");
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

      return createCtpClient(authUrl, apiUrl, credentials, projectKey);
    } catch (Exception exception) {
      throw new IllegalStateException(
          format(
              "IT properties file \"%s\" found, but CTP properties"
                  + " for prefix \"%s\" can't be read",
              CTP_CREDENTIALS_PROPERTIES, propertiesPrefix),
          exception);
    }
  }

  private static ProjectApiRoot createCtpClient(
      @Nonnull String authUrl,
      @Nonnull String apiUrl,
      @Nonnull ClientCredentials credentials,
      @Nonnull String projectKey) {
    final ApiModuleOptions options =
        ApiModuleOptions.of().withDateAttributeAsString(true).withDateCustomFieldAsString(true);
    final ObjectMapper mapper = JsonUtils.createObjectMapper(options);

    return ApiRootBuilder.of(new CtOkHttp4Client(200, 200))
        .defaultClient(credentials, authUrl, apiUrl)
        .withSerializer(ResponseSerializer.of(mapper))
        .withRetryMiddleware(5, Arrays.asList(500, 502, 503, 504))
        .build(projectKey);
  }

  private static Properties loadFromEnvVars(final String propertiesPrefix) {
    final Properties properties = new Properties();

    final String capitalizeAndReplaceDot = propertiesPrefix.toUpperCase().replace(".", "_");

    properties.put(
        propertiesPrefix + PROPERTIES_KEY_PROJECT_KEY_SUFFIX,
        getPropertyFromEnv(capitalizeAndReplaceDot + "PROJECT_KEY"));
    properties.put(
        propertiesPrefix + PROPERTIES_KEY_CLIENT_ID_SUFFIX,
        getPropertyFromEnv(capitalizeAndReplaceDot + "CLIENT_ID"));
    properties.put(
        propertiesPrefix + PROPERTIES_KEY_CLIENT_SECRET_SUFFIX,
        getPropertyFromEnv(capitalizeAndReplaceDot + "CLIENT_SECRET"));
    final String authUrl = getPropertyFromEnv(capitalizeAndReplaceDot + "AUTH_URL");
    if (authUrl != null) {
      properties.put(propertiesPrefix + PROPERTIES_KEY_AUTH_URL_SUFFIX, authUrl);
    }
    final String apiUrl = getPropertyFromEnv(capitalizeAndReplaceDot + "API_URL");
    if (apiUrl != null) {
      properties.put(propertiesPrefix + PROPERTIES_KEY_API_URL_SUFFIX, apiUrl);
    }

    final String scopes = getPropertyFromEnv(capitalizeAndReplaceDot + "SCOPES");
    if (scopes != null) {
      properties.put(propertiesPrefix + PROPERTIES_KEY_SCOPES_SUFFIX, scopes);
    }

    return properties;
  }

  private static String getPropertyFromEnv(String key) {
    return System.getenv(key);
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
