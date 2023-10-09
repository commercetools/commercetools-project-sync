package com.commercetools.project.sync.service.impl;

import static com.commercetools.project.sync.util.SyncUtils.DEFAULT_RUNNER_NAME;
import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

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
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

// These tests do compile but not valid
// TODO: Make tests valid
class CustomObjectServiceImplTest {

  private ProjectApiRoot ctpClient;

  @SuppressWarnings("unchecked")
  private static CustomObject STRING_CUSTOM_OBJECT = mock(CustomObject.class);

  @SuppressWarnings("unchecked")
  private static LastSyncCustomObject LAST_SYNC_CUSTOM_OBJECT = mock(LastSyncCustomObject.class);

  private ApiHttpResponse apiHttpResponse;

  @BeforeAll
  static void setup() {
    when(STRING_CUSTOM_OBJECT.getLastModifiedAt()).thenReturn(ZonedDateTime.now());
    when(LAST_SYNC_CUSTOM_OBJECT.getLastSyncTimestamp()).thenReturn(ZonedDateTime.now());
    when(STRING_CUSTOM_OBJECT.getValue()).thenReturn(LAST_SYNC_CUSTOM_OBJECT);
  }

  @BeforeEach
  void init() {
    apiHttpResponse = mock(ApiHttpResponse.class);
    ctpClient = mock(ProjectApiRoot.class);
  }

  @AfterEach
  void cleanup() {
    reset(ctpClient);
  }

  @Test
  @SuppressWarnings("unchecked")
  void getCurrentCtpTimestamp_OnSuccessfulUpsert_ShouldCompleteWithCtpTimestamp() {
    // preparation
    when(apiHttpResponse.getBody()).thenReturn(STRING_CUSTOM_OBJECT);
    when(ctpClient.customObjects().post(any(CustomObjectDraft.class)).execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    final CustomObjectService customObjectService = new CustomObjectServiceImpl(ctpClient);

    // test
    final CompletionStage<ZonedDateTime> ctpTimestamp =
        customObjectService.getCurrentCtpTimestamp(DEFAULT_RUNNER_NAME, "");

    // assertions
    assertThat(ctpTimestamp).isCompletedWithValue(STRING_CUSTOM_OBJECT.getLastModifiedAt());
  }

  @Test
  @SuppressWarnings("unchecked")
  void getCurrentCtpTimestamp_OnFailedUpsert_ShouldCompleteExceptionally() {
    // preparation
    final BadGatewayException badGatewayException = TestUtils.createBadGatewayException();
    when(ctpClient.customObjects().post(any(CustomObjectDraft.class)).execute())
        .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(badGatewayException));
    final CustomObjectService customObjectService = new CustomObjectServiceImpl(ctpClient);

    // test
    final CompletionStage<ZonedDateTime> ctpTimestamp =
        customObjectService.getCurrentCtpTimestamp(DEFAULT_RUNNER_NAME, "");

    // assertions
    assertThat(ctpTimestamp)
        .hasFailedWithThrowableThat()
        .isInstanceOf(BadGatewayException.class)
        .hasMessageContaining("CTP error!");
  }

  @Test
  @SuppressWarnings("unchecked")
  void
      getLastSyncCustomObject_OnSuccessfulQueryWithResults_ShouldCompleteWithLastSyncCustomObject() {
    // preparation
    when(apiHttpResponse.getBody()).thenReturn(STRING_CUSTOM_OBJECT);
    when(ctpClient.customObjects().withContainerAndKey(anyString(), anyString()).get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(ctpClient);

    // test
    final CompletionStage<Optional<LastSyncCustomObject>> lastSyncCustomObject =
        customObjectService.getLastSyncCustomObject("foo", "bar", DEFAULT_RUNNER_NAME);

    // assertions
    assertThat(lastSyncCustomObject).isCompletedWithValue(Optional.of(LAST_SYNC_CUSTOM_OBJECT));
  }

  @Test
  @SuppressWarnings("unchecked")
  void getLastSyncCustomObject_OnSuccessfulQueryWithNoResults_ShouldCompleteWithEmptyOptional() {
    // preparation
    when(apiHttpResponse.getBody()).thenReturn(null);
    when(ctpClient.customObjects().withContainerAndKey(anyString(), anyString()).get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(ctpClient);

    // test
    final CompletionStage<Optional<LastSyncCustomObject>> lastSyncCustomObject =
        customObjectService.getLastSyncCustomObject("foo", "bar", DEFAULT_RUNNER_NAME);

    // assertions
    assertThat(lastSyncCustomObject).isCompletedWithValue(empty());
  }

  @Test
  @SuppressWarnings("unchecked")
  void getLastSyncCustomObject_OnFailedQuery_ShouldCompleteExceptionally() {
    // preparation
    when(ctpClient.customObjects().withContainerAndKey(anyString(), anyString()).get().execute())
        .thenReturn(
            CompletableFutureUtils.exceptionallyCompletedFuture(
                TestUtils.createBadGatewayException()));

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(ctpClient);

    // test
    final CompletionStage<Optional<LastSyncCustomObject>> lastSyncCustomObject =
        customObjectService.getLastSyncCustomObject("foo", "bar", DEFAULT_RUNNER_NAME);

    // assertions
    assertThat(lastSyncCustomObject)
        .hasFailedWithThrowableThat()
        .isInstanceOf(BadGatewayException.class)
        .hasMessageContaining("CTP error!");
  }

  @Test
  @SuppressWarnings("unchecked")
  void createLastSyncCustomObject_OnSuccessfulCreation_ShouldCompleteWithLastSyncCustomObject() {
    // preparation
    when(apiHttpResponse.getBody()).thenReturn(LAST_SYNC_CUSTOM_OBJECT);
    when(ctpClient.customObjects().post(any(CustomObjectDraft.class)).execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(ctpClient);

    // test
    final LastSyncCustomObject<ProductSyncStatistics> lastSyncCustomObject =
        LastSyncCustomObject.of(ZonedDateTime.now(), new ProductSyncStatistics(), 100);

    final ApiHttpResponse<CustomObject> createdCustomObject =
        customObjectService
            .createLastSyncCustomObject("foo", "bar", DEFAULT_RUNNER_NAME, lastSyncCustomObject)
            .toCompletableFuture()
            .join();

    // assertions
    assertThat(createdCustomObject.getBody()).isEqualTo(LAST_SYNC_CUSTOM_OBJECT);
  }

  @Test
  @SuppressWarnings("unchecked")
  void createLastSyncCustomObject_OnFailedCreation_ShouldCompleteExceptionally() {
    // preparation
    when(ctpClient.customObjects().post(any(CustomObjectDraft.class)).execute())
        .thenReturn(
            CompletableFutureUtils.exceptionallyCompletedFuture(
                TestUtils.createBadGatewayException()));

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(ctpClient);

    // test
    final CompletableFuture<ApiHttpResponse<CustomObject>> createdCustomObject =
        customObjectService.createLastSyncCustomObject(
            "foo", "bar", DEFAULT_RUNNER_NAME, LAST_SYNC_CUSTOM_OBJECT);

    // assertions
    assertThat(createdCustomObject)
        .isCompletedExceptionally()
        .isInstanceOf(BadGatewayException.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  void createLastSyncCustomObject_WithValidTestRunnerName_ShouldCreateCorrectCustomObjectDraft() {
    // preparation
    final ArgumentCaptor<CustomObjectDraft> arg = ArgumentCaptor.forClass(CustomObjectDraft.class);
    when(ctpClient.customObjects().post(arg.capture()).execute()).thenReturn(null);

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
  @SuppressWarnings("unchecked")
  void createLastSyncCustomObject_WithEmptyRunnerName_ShouldCreateCorrectCustomObjectDraft() {
    // preparation
    final ArgumentCaptor<CustomObjectDraft> arg = ArgumentCaptor.forClass(CustomObjectDraft.class);
    when(ctpClient.customObjects().post(arg.capture()).execute()).thenReturn(null);

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
  @SuppressWarnings("unchecked")
  void createLastSyncCustomObject_WithNullRunnerName_ShouldCreateCorrectCustomObjectDraft() {
    // preparation
    final ArgumentCaptor<CustomObjectDraft> arg = ArgumentCaptor.forClass(CustomObjectDraft.class);
    when(ctpClient.customObjects().post(arg.capture()).execute()).thenReturn(null);

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
