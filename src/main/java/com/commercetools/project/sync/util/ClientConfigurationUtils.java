package com.commercetools.project.sync.util;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.client.retry.RetryableSphereClientBuilder;
import io.sphere.sdk.http.AsyncHttpClientAdapter;
import io.sphere.sdk.http.HttpClient;
import javax.annotation.Nonnull;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;

public final class ClientConfigurationUtils {
  /**
   * Creates a {@link SphereClient} with a default {@code timeout} value of 60 seconds.
   *
   * @param clientConfig the client configuration for the client.
   * @return the instantiated {@link SphereClient}.
   */
  public static SphereClient createClient(@Nonnull final SphereClientConfig clientConfig) {

    final HttpClient httpClient = getHttpClient();
    return RetryableSphereClientBuilder.of(clientConfig, httpClient).build();
  }

  /**
   * Gets an asynchronous {@link HttpClient} of `asynchttpclient` library, to be used by as an
   * underlying http client for the {@link SphereClient}.
   *
   * @return an asynchronous {@link HttpClient}
   */
  private static HttpClient getHttpClient() {
    final AsyncHttpClient asyncHttpClient =
        new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder().build());
    return AsyncHttpClientAdapter.of(asyncHttpClient);
  }

  private ClientConfigurationUtils() {}
}
