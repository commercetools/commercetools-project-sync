package com.commercetools.project.sync.service.impl;

import static com.commercetools.project.sync.service.impl.CustomObjectServiceImpl.DEFAULT_RUNNER_NAME;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.commercetools.project.sync.model.response.LastSyncCustomObject;
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

class CustomObjectServiceImplTest {

  private static final SphereClient CLIENT = mock(SphereClient.class);

  @SuppressWarnings("unchecked")
  private static final CustomObject<String> STRING_CUSTOM_OBJECT = mock(CustomObject.class);

  @SuppressWarnings("unchecked")
  private static final CustomObject<LastSyncCustomObject> LAST_SYNC_CUSTOM_OBJECT =
      mock(CustomObject.class);

  @BeforeAll
  static void setup() {
    when(STRING_CUSTOM_OBJECT.getLastModifiedAt()).thenReturn(ZonedDateTime.now());
    when(LAST_SYNC_CUSTOM_OBJECT.getLastModifiedAt()).thenReturn(ZonedDateTime.now());
  }

  @Test
  @SuppressWarnings("unchecked")
  void getCurrentCtpTimestamp_OnSuccessfulUpsert_ShouldCompleteWithCtpTimestampMinusTwoMinutes() {
    // preparation
    when(CLIENT.execute(any(CustomObjectUpsertCommand.class)))
        .thenReturn(CompletableFuture.completedFuture(STRING_CUSTOM_OBJECT));
    final CustomObjectService customObjectService = new CustomObjectServiceImpl(CLIENT);

    // test
    final CompletionStage<ZonedDateTime> ctpTimestamp =
        customObjectService.getCurrentCtpTimestamp(DEFAULT_RUNNER_NAME, "");

    // assertions
    assertThat(ctpTimestamp)
        .isCompletedWithValue(STRING_CUSTOM_OBJECT.getLastModifiedAt().minusMinutes(2));
  }

  @Test
  @SuppressWarnings("unchecked")
  void getCurrentCtpTimestamp_OnFailedUpsert_ShouldCompleteExceptionally() {
    // preparation
    when(CLIENT.execute(any(CustomObjectUpsertCommand.class)))
        .thenReturn(
            CompletableFutureUtils.exceptionallyCompletedFuture(
                new BadGatewayException("CTP error!")));
    final CustomObjectService customObjectService = new CustomObjectServiceImpl(CLIENT);

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
    final PagedQueryResult<CustomObject<LastSyncCustomObject>> queriedCustomObjects =
        spy(PagedQueryResult.empty());
    when(queriedCustomObjects.getResults()).thenReturn(singletonList(LAST_SYNC_CUSTOM_OBJECT));

    when(CLIENT.execute(any(CustomObjectQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(queriedCustomObjects));

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(CLIENT);

    // test
    final CompletionStage<Optional<CustomObject<LastSyncCustomObject>>> lastSyncCustomObject =
        customObjectService.getLastSyncCustomObject("foo", "bar", DEFAULT_RUNNER_NAME);

    // assertions
    assertThat(lastSyncCustomObject).isCompletedWithValue(Optional.of(LAST_SYNC_CUSTOM_OBJECT));
  }

  @Test
  @SuppressWarnings("unchecked")
  void getLastSyncCustomObject_OnSuccessfulQueryWithNoResults_ShouldCompleteWithEmptyOptional() {
    // preparation
    final PagedQueryResult<CustomObject<LastSyncCustomObject>> queriedCustomObjects =
        PagedQueryResult.empty();

    when(CLIENT.execute(any(CustomObjectQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(queriedCustomObjects));

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(CLIENT);

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
    final PagedQueryResult<CustomObject<LastSyncCustomObject>> queriedCustomObjects =
        spy(PagedQueryResult.empty());
    when(queriedCustomObjects.getResults()).thenReturn(singletonList(LAST_SYNC_CUSTOM_OBJECT));

    when(CLIENT.execute(any(CustomObjectQuery.class)))
        .thenReturn(
            CompletableFutureUtils.exceptionallyCompletedFuture(
                new BadGatewayException("CTP error!")));

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(CLIENT);

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
    when(CLIENT.execute(any(CustomObjectUpsertCommand.class)))
        .thenReturn(CompletableFuture.completedFuture(LAST_SYNC_CUSTOM_OBJECT));

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(CLIENT);

    // test
    final LastSyncCustomObject<ProductSyncStatistics> lastSyncCustomObject =
        LastSyncCustomObject.of(ZonedDateTime.now(), new ProductSyncStatistics(), 100);

    final CustomObject<LastSyncCustomObject> createdCustomObject =
        customObjectService
            .createLastSyncCustomObject("foo", "bar", DEFAULT_RUNNER_NAME, lastSyncCustomObject)
            .toCompletableFuture()
            .join();

    // assertions
    assertThat(createdCustomObject).isEqualTo(LAST_SYNC_CUSTOM_OBJECT);
  }

  @Test
  @SuppressWarnings("unchecked")
  void createLastSyncCustomObject_OnFailedCreation_ShouldCompleteExceptionally() {
    // preparation
    when(CLIENT.execute(any(CustomObjectUpsertCommand.class)))
        .thenReturn(
            CompletableFutureUtils.exceptionallyCompletedFuture(
                new BadGatewayException("CTP error!")));

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(CLIENT);

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
  @SuppressWarnings("unchecked")
  void createLastSyncCustomObject_WithValidTestRunnerName_ShouldCreateCorrectCustomObjectDraft() {
    // preparation
    final SphereClient client = mock(SphereClient.class);
    final ArgumentCaptor<CustomObjectUpsertCommand> arg =
        ArgumentCaptor.forClass(CustomObjectUpsertCommand.class);
    when(client.execute(arg.capture())).thenReturn(null);

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(client);

    final LastSyncCustomObject<ProductSyncStatistics> lastSyncCustomObject =
        LastSyncCustomObject.of(ZonedDateTime.now(), new ProductSyncStatistics(), 100);

    // test
    customObjectService.createLastSyncCustomObject(
        "foo", "bar", "testRunnerName", lastSyncCustomObject);

    // assertions
    final CustomObjectDraft createdDraft = (CustomObjectDraft) arg.getValue().getDraft();
    assertThat(createdDraft.getContainer())
        .isEqualTo("commercetools-project-sync.testRunnerName.bar");
    assertThat(createdDraft.getKey()).isEqualTo("foo");
  }

  @Test
  @SuppressWarnings("unchecked")
  void createLastSyncCustomObject_WithEmptyRunnerName_ShouldCreateCorrectCustomObjectDraft() {
    // preparation
    final SphereClient client = mock(SphereClient.class);
    final ArgumentCaptor<CustomObjectUpsertCommand> arg =
        ArgumentCaptor.forClass(CustomObjectUpsertCommand.class);
    when(client.execute(arg.capture())).thenReturn(null);

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(client);

    final LastSyncCustomObject<ProductSyncStatistics> lastSyncCustomObject =
        LastSyncCustomObject.of(ZonedDateTime.now(), new ProductSyncStatistics(), 100);

    // test
    customObjectService.createLastSyncCustomObject("foo", "bar", "", lastSyncCustomObject);

    // assertions
    final CustomObjectDraft createdDraft = (CustomObjectDraft) arg.getValue().getDraft();
    assertThat(createdDraft.getContainer()).isEqualTo("commercetools-project-sync.runnerName.bar");
    assertThat(createdDraft.getKey()).isEqualTo("foo");
  }

  @Test
  @SuppressWarnings("unchecked")
  void createLastSyncCustomObject_WithNullRunnerName_ShouldCreateCorrectCustomObjectDraft() {
    // preparation
    final SphereClient client = mock(SphereClient.class);
    final ArgumentCaptor<CustomObjectUpsertCommand> arg =
        ArgumentCaptor.forClass(CustomObjectUpsertCommand.class);
    when(client.execute(arg.capture())).thenReturn(null);

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(client);

    final LastSyncCustomObject<ProductSyncStatistics> lastSyncCustomObject =
        LastSyncCustomObject.of(ZonedDateTime.now(), new ProductSyncStatistics(), 100);

    // test
    customObjectService.createLastSyncCustomObject("foo", "bar", null, lastSyncCustomObject);

    // assertions
    final CustomObjectDraft createdDraft = (CustomObjectDraft) arg.getValue().getDraft();
    assertThat(createdDraft.getContainer()).isEqualTo("commercetools-project-sync.runnerName.bar");
    assertThat(createdDraft.getKey()).isEqualTo("foo");
  }
}
