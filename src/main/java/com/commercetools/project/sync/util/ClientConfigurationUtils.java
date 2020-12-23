package com.commercetools.project.sync.util;

import static io.sphere.sdk.http.HttpStatusCode.BAD_GATEWAY_502;
import static io.sphere.sdk.http.HttpStatusCode.GATEWAY_TIMEOUT_504;
import static io.sphere.sdk.http.HttpStatusCode.SERVICE_UNAVAILABLE_503;

import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.QueueSphereClientDecorator;
import io.sphere.sdk.client.RetrySphereClientDecorator;
import io.sphere.sdk.client.SphereAccessTokenSupplier;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.client.retry.RetryableSphereClientBuilder;
import io.sphere.sdk.http.AsyncHttpClientAdapter;
import io.sphere.sdk.http.HttpClient;
import io.sphere.sdk.retry.RetryAction;
import io.sphere.sdk.retry.RetryPredicate;
import io.sphere.sdk.retry.RetryRule;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;

public final class ClientConfigurationUtils {
  private static final long DEFAULT_TIMEOUT = 30000;

  /**
   * Creates a {@link SphereClient} with a default {@code timeout} value of 30 seconds.
   *
   * @param clientConfig the client configuration for the client.
   * @return the instantiated {@link SphereClient}.
   */
  public static SphereClient createClient(@Nonnull final SphereClientConfig clientConfig) {

    final HttpClient httpClient = getHttpClient();
    return RetryableSphereClientBuilder.of(clientConfig, httpClient)
                                                     .withStatusCodesToRetry(Arrays.asList(BAD_GATEWAY_502,
                                                         SERVICE_UNAVAILABLE_503,
                                                         GATEWAY_TIMEOUT_504))
                                                     .build();
  }

  /**
   * Gets an asynchronous {@link HttpClient} to be used by the {@link SphereClient}. Client
   * is created during first invocation and then cached.
   *
   * @return {@link HttpClient}
   */
  public static HttpClient getHttpClient() {
    final AsyncHttpClient asyncHttpClient =
        new DefaultAsyncHttpClient(
            new DefaultAsyncHttpClientConfig.Builder()
                .setHandshakeTimeout((int) DEFAULT_TIMEOUT)
                .build());
    return AsyncHttpClientAdapter.of(asyncHttpClient);
  }

  private ClientConfigurationUtils() {}
}
