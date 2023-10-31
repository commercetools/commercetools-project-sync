package com.commercetools.project.sync.service.impl;

import static com.commercetools.project.sync.util.SyncUtils.DEFAULT_RUNNER_NAME;
import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ByProjectKeyCustomObjectsByContainerByKeyGet;
import com.commercetools.api.client.ByProjectKeyCustomObjectsByContainerByKeyRequestBuilder;
import com.commercetools.api.client.ByProjectKeyCustomObjectsPost;
import com.commercetools.api.client.ByProjectKeyCustomObjectsRequestBuilder;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.api.models.custom_object.CustomObjectDraft;
import com.commercetools.project.sync.model.response.LastSyncCustomObject;
import com.commercetools.project.sync.service.CustomObjectService;
import com.commercetools.project.sync.util.TestUtils;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.BadGatewayException;
import io.vrap.rmf.base.client.utils.CompletableFutureUtils;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CustomObjectServiceImplTest {

  private ProjectApiRoot ctpClient;

  private static final ZonedDateTime now = ZonedDateTime.now();

  private static final CustomObject STRING_CUSTOM_OBJECT = mock(CustomObject.class);

  private static final LastSyncCustomObject<ProductSyncStatistics> LAST_SYNC_CUSTOM_OBJECT_VALUE =
      LastSyncCustomObject.of(now, new ProductSyncStatistics(), 0);

  private final ByProjectKeyCustomObjectsRequestBuilder byProjectKeyCustomObjectsRequestBuilder =
      mock();
  private final ByProjectKeyCustomObjectsPost byProjectKeyCustomObjectsPost = mock();
  private final ByProjectKeyCustomObjectsByContainerByKeyGet
      byProjectKeyCustomObjectsByContainerByKeyGet = mock();

  private ApiHttpResponse<CustomObject> apiHttpResponse;

  @BeforeAll
  static void setup() {
    when(STRING_CUSTOM_OBJECT.getLastModifiedAt()).thenReturn(now);
    when(STRING_CUSTOM_OBJECT.getValue()).thenReturn(LAST_SYNC_CUSTOM_OBJECT_VALUE);
  }

  @BeforeEach
  void init() {
    apiHttpResponse = mock(ApiHttpResponse.class);
    ctpClient = mock(ProjectApiRoot.class);

    when(ctpClient.customObjects()).thenReturn(byProjectKeyCustomObjectsRequestBuilder);
    when(byProjectKeyCustomObjectsRequestBuilder.post(any(CustomObjectDraft.class)))
        .thenReturn(byProjectKeyCustomObjectsPost);

    final ByProjectKeyCustomObjectsByContainerByKeyRequestBuilder
        byProjectKeyCustomObjectsByContainerByKeyRequestBuilder = mock();
    when(byProjectKeyCustomObjectsRequestBuilder.withContainerAndKey(anyString(), anyString()))
        .thenReturn(byProjectKeyCustomObjectsByContainerByKeyRequestBuilder);
    when(byProjectKeyCustomObjectsByContainerByKeyRequestBuilder.get())
        .thenReturn(byProjectKeyCustomObjectsByContainerByKeyGet);
  }

  @AfterEach
  void cleanup() {
    reset(ctpClient);
  }

  @Test
  void getCurrentCtpTimestamp_OnSuccessfulUpsert_ShouldCompleteWithCtpTimestamp() {
    // preparation
    when(apiHttpResponse.getBody()).thenReturn(STRING_CUSTOM_OBJECT);
    when(byProjectKeyCustomObjectsPost.execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    final CustomObjectService customObjectService = new CustomObjectServiceImpl(ctpClient);

    // test
    final CompletionStage<ZonedDateTime> ctpTimestamp =
        customObjectService.getCurrentCtpTimestamp(DEFAULT_RUNNER_NAME, "");

    // assertions
    assertThat(ctpTimestamp).isCompletedWithValue(STRING_CUSTOM_OBJECT.getLastModifiedAt());
  }

  @Test
  void getCurrentCtpTimestamp_OnFailedUpsert_ShouldCompleteExceptionally() {
    // preparation
    final BadGatewayException badGatewayException = TestUtils.createBadGatewayException();

    when(byProjectKeyCustomObjectsPost.execute())
        .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(badGatewayException));
    final CustomObjectService customObjectService = new CustomObjectServiceImpl(ctpClient);

    // test
    final CompletionStage<ZonedDateTime> ctpTimestamp =
        customObjectService.getCurrentCtpTimestamp(DEFAULT_RUNNER_NAME, "");

    // assertions
    assertThat(ctpTimestamp)
        .failsWithin(Duration.ZERO)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(BadGatewayException.class)
        .withMessageContaining("test");
  }

  @Test
  @Disabled("https://commercetools.atlassian.net/browse/DEVX-272")
  void
      getLastSyncCustomObject_OnSuccessfulQueryWithResults_ShouldCompleteWithLastSyncCustomObject() {
    // preparation
    when(apiHttpResponse.getBody()).thenReturn(STRING_CUSTOM_OBJECT);

    when(byProjectKeyCustomObjectsByContainerByKeyGet.execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(ctpClient);

    // test
    final LastSyncCustomObject lastSyncCustomObject =
        customObjectService.getLastSyncCustomObject("foo", "bar", DEFAULT_RUNNER_NAME).join().get();

    // assertions
    assertThat(lastSyncCustomObject.getLastSyncDurationInMillis())
        .isEqualTo(LAST_SYNC_CUSTOM_OBJECT_VALUE.getLastSyncDurationInMillis());
    assertThat(lastSyncCustomObject.getLastSyncStatistics())
        .isEqualTo(LAST_SYNC_CUSTOM_OBJECT_VALUE.getLastSyncStatistics());
    assertThat(lastSyncCustomObject.getApplicationVersion())
        .isEqualTo(LAST_SYNC_CUSTOM_OBJECT_VALUE.getApplicationVersion());
    assertThat(
            lastSyncCustomObject
                .getLastSyncTimestamp()
                .truncatedTo(ChronoUnit.SECONDS)
                .withZoneSameInstant(ZoneOffset.UTC))
        .isEqualTo(
            LAST_SYNC_CUSTOM_OBJECT_VALUE
                .getLastSyncTimestamp()
                .truncatedTo(ChronoUnit.SECONDS)
                .withZoneSameInstant(ZoneOffset.UTC));
  }

  @Test
  void getLastSyncCustomObject_OnSuccessfulQueryWithNoResults_ShouldCompleteWithEmptyOptional() {
    // preparation
    when(apiHttpResponse.getBody()).thenReturn(null);

    when(byProjectKeyCustomObjectsByContainerByKeyGet.execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(ctpClient);

    // test
    final CompletionStage<Optional<LastSyncCustomObject>> lastSyncCustomObject =
        customObjectService.getLastSyncCustomObject("foo", "bar", DEFAULT_RUNNER_NAME);

    // assertions
    assertThat(lastSyncCustomObject).isCompletedWithValue(empty());
  }

  @Test
  void getLastSyncCustomObject_OnFailedQuery_ShouldCompleteExceptionally() {
    // preparation
    when(byProjectKeyCustomObjectsByContainerByKeyGet.execute())
        .thenReturn(
            CompletableFutureUtils.exceptionallyCompletedFuture(
                TestUtils.createBadGatewayException()));

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(ctpClient);

    // test
    final CompletionStage<Optional<LastSyncCustomObject>> lastSyncCustomObject =
        customObjectService.getLastSyncCustomObject("foo", "bar", DEFAULT_RUNNER_NAME);

    // assertions
    assertThat(lastSyncCustomObject)
        .failsWithin(Duration.ZERO)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(RuntimeException.class)
        .satisfies(
            exception ->
                assertThat(exception.getCause().getCause())
                    .isInstanceOf(BadGatewayException.class)
                    .hasMessageContaining("test"));
  }

  @Test
  void createLastSyncCustomObject_OnSuccessfulCreation_ShouldCompleteWithLastSyncCustomObject() {
    // preparation
    when(apiHttpResponse.getBody()).thenReturn(STRING_CUSTOM_OBJECT);

    when(byProjectKeyCustomObjectsPost.execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(ctpClient);

    // test
    final LastSyncCustomObject<ProductSyncStatistics> lastSyncCustomObject =
        LastSyncCustomObject.of(ZonedDateTime.now(), new ProductSyncStatistics(), 100);

    final CustomObject createdCustomObject =
        customObjectService
            .createLastSyncCustomObject("foo", "bar", DEFAULT_RUNNER_NAME, lastSyncCustomObject)
            .toCompletableFuture()
            .join()
            .getBody();

    final LastSyncCustomObject customObjectValue =
        (LastSyncCustomObject) createdCustomObject.getValue();

    // assertions
    assertThat(customObjectValue).isEqualTo(LAST_SYNC_CUSTOM_OBJECT_VALUE);
  }

  @Test
  void createLastSyncCustomObject_OnFailedCreation_ShouldCompleteExceptionally() {
    // preparation
    when(byProjectKeyCustomObjectsPost.execute())
        .thenReturn(
            CompletableFutureUtils.exceptionallyCompletedFuture(
                TestUtils.createBadGatewayException()));

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(ctpClient);

    // test
    final CompletableFuture<ApiHttpResponse<CustomObject>> createdCustomObject =
        customObjectService.createLastSyncCustomObject(
            "foo", "bar", DEFAULT_RUNNER_NAME, LAST_SYNC_CUSTOM_OBJECT_VALUE);

    // assertions
    assertThat(createdCustomObject)
        .failsWithin(Duration.ZERO)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(BadGatewayException.class)
        .withMessageContaining("test");
  }

  @Test
  void createLastSyncCustomObject_WithValidTestRunnerName_ShouldCreateCorrectCustomObjectDraft() {
    // preparation
    final ArgumentCaptor<CustomObjectDraft> arg = ArgumentCaptor.forClass(CustomObjectDraft.class);
    when(byProjectKeyCustomObjectsRequestBuilder.post(arg.capture()))
        .thenReturn(byProjectKeyCustomObjectsPost);
    when(byProjectKeyCustomObjectsPost.execute()).thenReturn(null);

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(ctpClient);

    final LastSyncCustomObject<ProductSyncStatistics> lastSyncCustomObject =
        LastSyncCustomObject.of(ZonedDateTime.now(), new ProductSyncStatistics(), 100);

    // test
    customObjectService.createLastSyncCustomObject(
        "foo", "bar", "testRunnerName", lastSyncCustomObject);

    // assertions
    final CustomObjectDraft createdDraft = arg.getValue();
    assertThat(createdDraft.getContainer())
        .isEqualTo("commercetools-project-sync.testRunnerName.bar");
    assertThat(createdDraft.getKey()).isEqualTo("foo");
    assertThat(createdDraft.getValue()).isInstanceOf(LastSyncCustomObject.class);
    assertThat((LastSyncCustomObject) createdDraft.getValue()).isEqualTo(lastSyncCustomObject);
  }

  @Test
  void createLastSyncCustomObject_WithEmptyRunnerName_ShouldCreateCorrectCustomObjectDraft() {
    // preparation
    final ArgumentCaptor<CustomObjectDraft> arg = ArgumentCaptor.forClass(CustomObjectDraft.class);
    when(byProjectKeyCustomObjectsRequestBuilder.post(arg.capture()))
        .thenReturn(byProjectKeyCustomObjectsPost);
    when(byProjectKeyCustomObjectsPost.execute()).thenReturn(null);

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(ctpClient);

    final LastSyncCustomObject<ProductSyncStatistics> lastSyncCustomObject =
        LastSyncCustomObject.of(ZonedDateTime.now(), new ProductSyncStatistics(), 100);

    // test
    customObjectService.createLastSyncCustomObject("foo", "bar", "", lastSyncCustomObject);

    // assertions
    final CustomObjectDraft createdDraft = arg.getValue();
    assertThat(createdDraft.getContainer()).isEqualTo("commercetools-project-sync.runnerName.bar");
    assertThat(createdDraft.getKey()).isEqualTo("foo");
    assertThat(createdDraft.getValue()).isInstanceOf(LastSyncCustomObject.class);
    assertThat((LastSyncCustomObject) createdDraft.getValue()).isEqualTo(lastSyncCustomObject);
  }

  @Test
  void createLastSyncCustomObject_WithNullRunnerName_ShouldCreateCorrectCustomObjectDraft() {
    // preparation
    final ArgumentCaptor<CustomObjectDraft> arg = ArgumentCaptor.forClass(CustomObjectDraft.class);
    when(byProjectKeyCustomObjectsRequestBuilder.post(arg.capture()))
        .thenReturn(byProjectKeyCustomObjectsPost);
    when(byProjectKeyCustomObjectsPost.execute()).thenReturn(null);

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(ctpClient);

    final LastSyncCustomObject<ProductSyncStatistics> lastSyncCustomObject =
        LastSyncCustomObject.of(ZonedDateTime.now(), new ProductSyncStatistics(), 100);

    // test
    customObjectService.createLastSyncCustomObject("foo", "bar", null, lastSyncCustomObject);

    // assertions
    final CustomObjectDraft createdDraft = arg.getValue();
    assertThat(createdDraft.getContainer()).isEqualTo("commercetools-project-sync.runnerName.bar");
    assertThat(createdDraft.getKey()).isEqualTo("foo");
    assertThat(createdDraft.getValue()).isInstanceOf(LastSyncCustomObject.class);
    assertThat((LastSyncCustomObject) createdDraft.getValue()).isEqualTo(lastSyncCustomObject);
  }
}
