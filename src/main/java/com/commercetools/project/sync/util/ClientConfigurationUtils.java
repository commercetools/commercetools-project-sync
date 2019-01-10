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
import io.sphere.sdk.http.AsyncHttpClientAdapter;
import io.sphere.sdk.http.HttpClient;
import io.sphere.sdk.retry.RetryAction;
import io.sphere.sdk.retry.RetryPredicate;
import io.sphere.sdk.retry.RetryRule;
import java.time.Duration;
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
   * Creates a {@link BlockingSphereClient} with a custom {@code timeout} with a custom {@link
   * TimeUnit}.
   *
   * @param clientConfig the client configuration for the client.
   * @return the instantiated {@link BlockingSphereClient}.
   */
  public static SphereClient createClient(@Nonnull final SphereClientConfig clientConfig) {

    final HttpClient httpClient = getHttpClient();
    final SphereAccessTokenSupplier tokenSupplier =
        SphereAccessTokenSupplier.ofAutoRefresh(clientConfig, httpClient, false);
    final SphereClient underlying = SphereClient.of(clientConfig, httpClient, tokenSupplier);
    final SphereClient retryClient = withRetry(underlying);
    return withLimitedParallelRequests(retryClient);
  }

  /**
   * Gets an asynchronous {@link HttpClient} to be used by the {@link BlockingSphereClient}. Client
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

  private static SphereClient withRetry(final SphereClient delegate) {
    final int maxAttempts = 5;
    final RetryAction scheduledRetry =
        RetryAction.ofScheduledRetry(
            maxAttempts, context -> calculateVariableDelay(context.getAttempt()));
    final RetryPredicate http5xxMatcher =
        RetryPredicate.ofMatchingStatusCodes(
            BAD_GATEWAY_502, SERVICE_UNAVAILABLE_503, GATEWAY_TIMEOUT_504);
    final List<RetryRule> retryRules =
        Collections.singletonList(RetryRule.of(http5xxMatcher, scheduledRetry));
    return RetrySphereClientDecorator.of(delegate, retryRules);
  }

  /**
   * Computes a variable delay in seconds (grows with attempts count with a random component).
   *
   * @param triedAttempts the number of attempts already tried by the client.
   * @return a computed variable delay in seconds, that grows with the number of attempts with a
   *     random component.
   */
  private static Duration calculateVariableDelay(final long triedAttempts) {
    final long timeoutInSeconds = TimeUnit.SECONDS.convert(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
    final long randomNumberInRange = getRandomNumberInRange(50, timeoutInSeconds);
    final long timeoutMultipliedByTriedAttempts = timeoutInSeconds * triedAttempts;
    return Duration.ofSeconds(timeoutMultipliedByTriedAttempts + randomNumberInRange);
  }

  private static long getRandomNumberInRange(final long min, final long max) {
    return new Random().longs(min, (max + 1)).limit(1).findFirst().getAsLong();
  }

  private static SphereClient withLimitedParallelRequests(final SphereClient delegate) {
    final int maxParallelRequests = 20;
    return QueueSphereClientDecorator.of(delegate, maxParallelRequests);
  }

  private ClientConfigurationUtils() {}
}
