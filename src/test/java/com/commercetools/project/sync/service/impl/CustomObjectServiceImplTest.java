package com.commercetools.project.sync.service.impl;

import com.commercetools.project.sync.model.LastSyncCustomObject;
import com.commercetools.project.sync.service.CustomObjectService;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;
import io.sphere.sdk.customobjects.commands.CustomObjectUpsertCommand;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.utils.CompletableFutureUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.commercetools.project.sync.util.SyncUtils.DEFAULT_METHOD_NAME;
import static com.commercetools.project.sync.util.SyncUtils.DEFAULT_RUNNER_NAME;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class CustomObjectServiceImplTest {

  @Test
  @SuppressWarnings("unchecked")
  void getCurrentCtpTimestamp_OnSuccessfulUpsert_ShouldCompleteWithCtpTimestampMinusTwoMinutes() {
    // preparation
    final SphereClient client = mock(SphereClient.class);
    final CustomObject<String> stringCustomObject = mock(CustomObject.class);
    when(stringCustomObject.getLastModifiedAt()).thenReturn(ZonedDateTime.now());
    when(client.execute(any(CustomObjectUpsertCommand.class)))
        .thenReturn(CompletableFuture.completedFuture(stringCustomObject));
    final CustomObjectService customObjectService = new CustomObjectServiceImpl(client);

    // test
    final CompletionStage<ZonedDateTime> ctpTimestamp =
        customObjectService.getCurrentCtpTimestamp(DEFAULT_RUNNER_NAME, DEFAULT_METHOD_NAME);

    // assertions
    assertThat(ctpTimestamp)
        .isCompletedWithValue(stringCustomObject.getLastModifiedAt().minusMinutes(2));
  }

  @Test
  @SuppressWarnings("unchecked")
  void getCurrentCtpTimestamp_OnFailedUpsert_ShouldCompleteExceptionally() {
    // preparation
    final SphereClient client = mock(SphereClient.class);
    final CustomObject<String> stringCustomObject = mock(CustomObject.class);
    when(stringCustomObject.getLastModifiedAt()).thenReturn(ZonedDateTime.now());
    when(client.execute(any(CustomObjectUpsertCommand.class)))
        .thenReturn(
            CompletableFutureUtils.exceptionallyCompletedFuture(
                new BadGatewayException("CTP error!")));
    final CustomObjectService customObjectService = new CustomObjectServiceImpl(client);

    // test
    final CompletionStage<ZonedDateTime> ctpTimestamp =
        customObjectService.getCurrentCtpTimestamp(DEFAULT_RUNNER_NAME, DEFAULT_METHOD_NAME);

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
    final SphereClient client = mock(SphereClient.class);
    final CustomObject<LastSyncCustomObject> customObject = mock(CustomObject.class);
    when(customObject.getLastModifiedAt()).thenReturn(ZonedDateTime.now());

    final PagedQueryResult<CustomObject<LastSyncCustomObject>> queriedCustomObjects =
        spy(PagedQueryResult.empty());
    when(queriedCustomObjects.getResults()).thenReturn(singletonList(customObject));

    when(client.execute(any(CustomObjectQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(queriedCustomObjects));

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(client);

    // test
    final CompletionStage<Optional<CustomObject<LastSyncCustomObject>>> lastSyncCustomObject =
        customObjectService.getLastSyncCustomObject("foo", "bar", DEFAULT_RUNNER_NAME);

    // assertions
    assertThat(lastSyncCustomObject).isCompletedWithValue(Optional.of(customObject));
  }

  @Test
  @SuppressWarnings("unchecked")
  void getLastSyncCustomObject_OnSuccessfulQueryWithNoResults_ShouldCompleteWithEmptyOptional() {
    // preparation
    final SphereClient client = mock(SphereClient.class);

    final PagedQueryResult<CustomObject<LastSyncCustomObject>> queriedCustomObjects =
        PagedQueryResult.empty();

    when(client.execute(any(CustomObjectQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(queriedCustomObjects));

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(client);

    // test
    final CompletionStage<Optional<CustomObject<LastSyncCustomObject>>> lastSyncCustomObject =
        customObjectService.getLastSyncCustomObject("foo", "bar", DEFAULT_RUNNER_NAME);

    // assertions
    assertThat(lastSyncCustomObject).isCompletedWithValue(empty());
  }

  @Test
  @SuppressWarnings("unchecked")
  void getLastSyncCustomObject_OnFailedQuery_ShouldCompleteExceptionally() {
    // preparation
    final SphereClient client = mock(SphereClient.class);
    final CustomObject<LastSyncCustomObject> customObject = mock(CustomObject.class);
    when(customObject.getLastModifiedAt()).thenReturn(ZonedDateTime.now());

    final PagedQueryResult<CustomObject<LastSyncCustomObject>> queriedCustomObjects =
        spy(PagedQueryResult.empty());
    when(queriedCustomObjects.getResults()).thenReturn(singletonList(customObject));

    when(client.execute(any(CustomObjectQuery.class)))
        .thenReturn(
            CompletableFutureUtils.exceptionallyCompletedFuture(
                new BadGatewayException("CTP error!")));

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(client);

    // test
    final CompletionStage<Optional<CustomObject<LastSyncCustomObject>>> lastSyncCustomObject =
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
    final SphereClient client = mock(SphereClient.class);
    final CustomObject<LastSyncCustomObject> customObject = mock(CustomObject.class);
    when(customObject.getLastModifiedAt()).thenReturn(ZonedDateTime.now());

    when(client.execute(any(CustomObjectUpsertCommand.class)))
        .thenReturn(CompletableFuture.completedFuture(customObject));

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(client);

    // test
    final LastSyncCustomObject<ProductSyncStatistics> lastSyncCustomObject =
        LastSyncCustomObject.of(ZonedDateTime.now(), new ProductSyncStatistics(), 100);

    final CustomObject<LastSyncCustomObject> createdCustomObject =
        customObjectService
            .createLastSyncCustomObject("foo", "bar", DEFAULT_RUNNER_NAME, lastSyncCustomObject)
            .toCompletableFuture()
            .join();

    // assertions
    assertThat(createdCustomObject).isEqualTo(customObject);
  }

  @Test
  @SuppressWarnings("unchecked")
  void createLastSyncCustomObject_OnFailedCreation_ShouldCompleteExceptionally() {
    // preparation
    final SphereClient client = mock(SphereClient.class);
    final CustomObject<LastSyncCustomObject> customObject = mock(CustomObject.class);
    when(customObject.getLastModifiedAt()).thenReturn(ZonedDateTime.now());

    when(client.execute(any(CustomObjectUpsertCommand.class)))
        .thenReturn(
            CompletableFutureUtils.exceptionallyCompletedFuture(
                new BadGatewayException("CTP error!")));

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(client);

    // test
    final LastSyncCustomObject<ProductSyncStatistics> lastSyncCustomObject =
        LastSyncCustomObject.of(ZonedDateTime.now(), new ProductSyncStatistics(), 100);

    final CompletionStage<CustomObject<LastSyncCustomObject>> createdCustomObject =
        customObjectService.createLastSyncCustomObject(
            "foo", "bar", DEFAULT_RUNNER_NAME, lastSyncCustomObject);

    // assertions
    assertThat(createdCustomObject)
        .hasFailedWithThrowableThat()
        .isInstanceOf(BadGatewayException.class)
        .hasMessageContaining("CTP error!");
  }

  @Test
  void attachRunnerName_WithValidTestRunnerName() {
    // preparation
    final SphereClient client = mock(SphereClient.class);
    ArgumentCaptor<CustomObjectUpsertCommand> arg =
        ArgumentCaptor.forClass(CustomObjectUpsertCommand.class);
    when(client.execute(arg.capture())).thenReturn(null);

    final CustomObject<LastSyncCustomObject> customObject = mock(CustomObject.class);
    when(customObject.getLastModifiedAt()).thenReturn(ZonedDateTime.now());

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(client);

    final LastSyncCustomObject<ProductSyncStatistics> lastSyncCustomObject =
        LastSyncCustomObject.of(ZonedDateTime.now(), new ProductSyncStatistics(), 100);

    // test
    customObjectService.createLastSyncCustomObject(
        "foo", "bar", "testRunnerName", lastSyncCustomObject);

    // assertions
    assertThat(((CustomObjectDraft) arg.getValue().getDraft()).getContainer())
        .contains("testRunnerName");
  }

  @Test
  void attachRunnerName_WithEmptyRunnerName_ShouldRunWithDefaultRunnerName() {
    // preparation
    final SphereClient client = mock(SphereClient.class);
    ArgumentCaptor<CustomObjectUpsertCommand> arg =
        ArgumentCaptor.forClass(CustomObjectUpsertCommand.class);
    when(client.execute(arg.capture())).thenReturn(null);

    final CustomObject<LastSyncCustomObject> customObject = mock(CustomObject.class);
    when(customObject.getLastModifiedAt()).thenReturn(ZonedDateTime.now());

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(client);

    final LastSyncCustomObject<ProductSyncStatistics> lastSyncCustomObject =
        LastSyncCustomObject.of(ZonedDateTime.now(), new ProductSyncStatistics(), 100);

    // test
    customObjectService.createLastSyncCustomObject("foo", "bar", "", lastSyncCustomObject);

    // assertions
    assertThat(((CustomObjectDraft) arg.getValue().getDraft()).getContainer())
        .contains(DEFAULT_RUNNER_NAME);
  }

  @Test
  void attachMethodName_WithNullMethodName_ShouldRunWithDefaultMethodName() {
    // preparation
    final SphereClient client = mock(SphereClient.class);
    ArgumentCaptor<CustomObjectUpsertCommand> arg =
        ArgumentCaptor.forClass(CustomObjectUpsertCommand.class);
    when(client.execute(arg.capture()))
        .thenReturn(CompletableFuture.completedFuture(ZonedDateTime.now()));

    final CustomObject<LastSyncCustomObject> customObject = mock(CustomObject.class);
    when(customObject.getLastModifiedAt()).thenReturn(ZonedDateTime.now());

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(client);

    // test
    customObjectService.getCurrentCtpTimestamp(DEFAULT_RUNNER_NAME, DEFAULT_METHOD_NAME);

    // assertions
    assertThat(((CustomObjectDraft) arg.getValue().getDraft()).getContainer())
        .contains(DEFAULT_METHOD_NAME);
  }

  @Test
  void attachMethodName_WithEmptyMethodName_ShouldRunWithDefaultMethodName() {
    // preparation
    final SphereClient client = mock(SphereClient.class);
    ArgumentCaptor<CustomObjectUpsertCommand> arg =
        ArgumentCaptor.forClass(CustomObjectUpsertCommand.class);
    when(client.execute(arg.capture()))
        .thenReturn(CompletableFuture.completedFuture(ZonedDateTime.now()));

    final CustomObject<LastSyncCustomObject> customObject = mock(CustomObject.class);
    when(customObject.getLastModifiedAt()).thenReturn(ZonedDateTime.now());

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(client);

    // test
    customObjectService.getCurrentCtpTimestamp(DEFAULT_RUNNER_NAME, "");

    // assertions
    assertThat(((CustomObjectDraft) arg.getValue().getDraft()).getContainer())
        .contains(DEFAULT_METHOD_NAME);
  }

  @Test
  void attachMethodName_WithTestMethodName_ShouldRunWithTestMethodName() {
    // preparation
    final SphereClient client = mock(SphereClient.class);
    ArgumentCaptor<CustomObjectUpsertCommand> arg =
        ArgumentCaptor.forClass(CustomObjectUpsertCommand.class);
    when(client.execute(arg.capture()))
        .thenReturn(CompletableFuture.completedFuture(ZonedDateTime.now()));

    final CustomObject<LastSyncCustomObject> customObject = mock(CustomObject.class);
    when(customObject.getLastModifiedAt()).thenReturn(ZonedDateTime.now());

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(client);

    // test
    customObjectService.getCurrentCtpTimestamp("Test", DEFAULT_METHOD_NAME);

    // assertions
    assertThat(((CustomObjectDraft) arg.getValue().getDraft()).getContainer()).contains(".Test.");
  }
}
