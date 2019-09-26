package com.commercetools.project.sync;

import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_DESCRIPTION;
import static com.commercetools.project.sync.service.impl.CustomObjectServiceImpl.DEFAULT_RUNNER_NAME;
import static com.commercetools.project.sync.service.impl.CustomObjectServiceImpl.TIMESTAMP_GENERATOR_KEY;
import static com.commercetools.project.sync.service.impl.CustomObjectServiceImpl.TIMESTAMP_GENERATOR_VALUE;
import static com.commercetools.project.sync.util.SyncUtils.getApplicationName;
import static com.commercetools.project.sync.util.TestUtils.assertAllSyncersLoggingEvents;
import static com.commercetools.project.sync.util.TestUtils.getMockedClock;
import static com.commercetools.project.sync.util.TestUtils.mockLastSyncCustomObject;
import static com.commercetools.project.sync.util.TestUtils.stubClientsCustomObjectService;
import static com.commercetools.project.sync.util.TestUtils.verifyInteractionsWithClientAfterSync;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commercetools.project.sync.model.response.LastSyncCustomObject;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;
import io.sphere.sdk.customobjects.commands.CustomObjectUpsertCommand;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.queries.QueryPredicate;
import io.sphere.sdk.types.queries.TypeQuery;
import io.sphere.sdk.utils.CompletableFutureUtils;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

class SyncerFactoryTest {
  private static final TestLogger syncerTestLogger = TestLoggerFactory.getTestLogger(Syncer.class);
  private static final TestLogger cliRunnerTestLogger =
      TestLoggerFactory.getTestLogger(CliRunner.class);

  @AfterEach
  void tearDownTest() {
    syncerTestLogger.clearAll();
    cliRunnerTestLogger.clearAll();
  }

  @Test
  void sync_WithNullOptionValue_ShouldCompleteExceptionallyWithIllegalArgumentException() {
    assertThat(
            SyncerFactory.of(
                    () -> mock(SphereClient.class),
                    () -> mock(SphereClient.class),
                    getMockedClock())
                .sync(null, "myRunnerName", false))
        .hasFailedWithThrowableThat()
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(
            format(
                "Blank argument supplied to \"-s\" or \"--sync\" option! %s",
                SYNC_MODULE_OPTION_DESCRIPTION));
  }

  @Test
  void sync_WithEmptyOptionValue_ShouldCompleteExceptionallyWithIllegalArgumentException() {
    assertThat(
            SyncerFactory.of(
                    () -> mock(SphereClient.class),
                    () -> mock(SphereClient.class),
                    getMockedClock())
                .sync("", "myRunnerName", false))
        .hasFailedWithThrowableThat()
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(
            format(
                "Blank argument supplied to \"-s\" or \"--sync\" option! %s",
                SYNC_MODULE_OPTION_DESCRIPTION));
  }

  @Test
  void sync_WithUnknownOptionValue_ShouldCompleteExceptionallyWithIllegalArgumentException() {
    final String unknownOptionValue = "anyOption";

    assertThat(
            SyncerFactory.of(
                    () -> mock(SphereClient.class),
                    () -> mock(SphereClient.class),
                    getMockedClock())
                .sync(unknownOptionValue, "myRunnerName", false))
        .hasFailedWithThrowableThat()
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(
            format(
                "Unknown argument \"%s\" supplied to \"-s\" or \"--sync\" option! %s",
                unknownOptionValue, SYNC_MODULE_OPTION_DESCRIPTION));
  }

  @Test
  @SuppressWarnings("unchecked")
  void sync_AsProductsDeltaSync_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    when(sourceClient.execute(any(ProductQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    final ZonedDateTime currentCtpTimestamp = ZonedDateTime.now();
    stubClientsCustomObjectService(targetClient, currentCtpTimestamp);

    // test
    syncerFactory.sync("products", "myRunnerName", false);

    // assertions
    verify(sourceClient, times(1)).execute(any(ProductQuery.class));

    verifyTimestampGeneratorCustomObjectUpsert(targetClient, 1, "ProductSync", "myRunnerName");
    verifyLastSyncCustomObjectQuery(targetClient, "productSync", "myRunnerName", "foo", 1);
    // verify two custom object upserts : 1. current ctp timestamp and 2. last sync timestamp
    // creation)
    verify(targetClient, times(2)).execute(any(CustomObjectUpsertCommand.class));
    // TODO: Assert on actual last sync timestamp creation in detail after Statistics classes in
    // java-sync library
    // TODO: override #equals method:
    // https://github.com/commercetools/commercetools-sync-java/issues/376
    // TODO: e.g. verifyNewLastSyncCustomObjectCreation(targetClient,
    // currentCtpTimestamp.minusMinutes(2), any(ProductSyncStatistics.class), 0L, "productSync",
    // "foo");
    verifyInteractionsWithClientAfterSync(sourceClient, 1);

    final Condition<LoggingEvent> startLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent.getMessage().contains("Starting ProductSync"),
            "start log");

    final Condition<LoggingEvent> statisticsLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent
                        .getMessage()
                        .contains(
                            "Summary: 0 products were processed in total (0 created, 0 updated "
                                + "and 0 failed to sync)."),
            "statistics log");

    assertThat(syncerTestLogger.getAllLoggingEvents())
        .hasSize(2)
        .haveExactly(1, startLog)
        .haveExactly(1, statisticsLog);
  }

  @Test
  @SuppressWarnings("unchecked")
  void sync_AsProductsFullSync_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    when(sourceClient.execute(any(ProductQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    final ZonedDateTime currentCtpTimestamp = ZonedDateTime.now();
    stubClientsCustomObjectService(targetClient, currentCtpTimestamp);

    // test
    syncerFactory.sync("products", "myRunnerName", true);

    // assertions
    verify(sourceClient, times(1)).execute(any(ProductQuery.class));

    verifyTimestampGeneratorCustomObjectUpsert(targetClient, 0, "ProductSync", "myRunnerName");
    verifyLastSyncCustomObjectQuery(targetClient, "productSync", "myRunnerName", "foo", 0);
    verify(targetClient, times(0)).execute(any(CustomObjectUpsertCommand.class));
    verifyInteractionsWithClientAfterSync(sourceClient, 1);

    final Condition<LoggingEvent> startLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent.getMessage().contains("Starting ProductSync"),
            "start log");

    final Condition<LoggingEvent> statisticsLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent
                        .getMessage()
                        .contains(
                            "Summary: 0 products were processed in total (0 created, 0 updated "
                                + "and 0 failed to sync)."),
            "statistics log");

    assertThat(syncerTestLogger.getAllLoggingEvents())
        .hasSize(2)
        .haveExactly(1, startLog)
        .haveExactly(1, statisticsLog);
  }

  private static void verifyTimestampGeneratorCustomObjectUpsert(
      @Nonnull final SphereClient client,
      final int expectedInvocations,
      @Nonnull final String syncMethodName,
      @Nonnull final String syncRunnerName) {

    final CustomObjectDraft<String> currentTimestampDraft =
        CustomObjectDraft.ofUnversionedUpsert(
            format(
                "%s.%s.%s.%s",
                getApplicationName(), syncRunnerName, syncMethodName, TIMESTAMP_GENERATOR_KEY),
            TIMESTAMP_GENERATOR_KEY,
            TIMESTAMP_GENERATOR_VALUE,
            String.class);

    verify(client, times(expectedInvocations))
        .execute(CustomObjectUpsertCommand.of(currentTimestampDraft));
  }

  private static void verifyLastSyncCustomObjectQuery(
      @Nonnull final SphereClient client,
      @Nonnull final String syncModuleName,
      @Nonnull final String syncRunnerName,
      @Nonnull final String sourceProjectKey,
      final int expectedInvocations) {

    final QueryPredicate<CustomObject<LastSyncCustomObject>> queryPredicate =
        QueryPredicate.of(
            format(
                "container=\"commercetools-project-sync.%s.%s\" AND key=\"%s\"",
                syncRunnerName, syncModuleName, sourceProjectKey));

    verify(client, times(expectedInvocations))
        .execute(CustomObjectQuery.of(LastSyncCustomObject.class).plusPredicates(queryPredicate));
  }

  @Test
  @SuppressWarnings("unchecked")
  void sync_AsCategoriesDeltaSync_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    when(sourceClient.execute(any(CategoryQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    final ZonedDateTime currentCtpTimestamp = ZonedDateTime.now();
    stubClientsCustomObjectService(targetClient, currentCtpTimestamp);

    // test
    syncerFactory.sync("categories", null, false);

    // assertions
    verify(sourceClient, times(1)).execute(any(CategoryQuery.class));
    verifyTimestampGeneratorCustomObjectUpsert(
        targetClient, 1, "CategorySync", DEFAULT_RUNNER_NAME);
    verifyLastSyncCustomObjectQuery(targetClient, "categorySync", DEFAULT_RUNNER_NAME, "foo", 1);
    // verify two custom object upserts : 1. current ctp timestamp and 2. last sync timestamp
    // creation)
    verify(targetClient, times(2)).execute(any(CustomObjectUpsertCommand.class));
    // TODO: Assert on actual last sync timestamp creation in detail after Statistics classes in
    // java-sync library
    // TODO: override #equals method:
    // https://github.com/commercetools/commercetools-sync-java/issues/376
    // TODO: e.g. verifyNewLastSyncCustomObjectCreation(targetClient,
    // currentCtpTimestamp.minusMinutes(2), any(ProductSyncStatistics.class), 0L, "productSync",
    // "foo");
    verifyInteractionsWithClientAfterSync(sourceClient, 1);

    final Condition<LoggingEvent> startLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent.getMessage().contains("Starting CategorySync"),
            "start log");

    final Condition<LoggingEvent> statisticsLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent
                        .getMessage()
                        .contains(
                            "Summary: 0 categories were processed in total (0 created, 0 updated, "
                                + "0 failed to sync and 0 categories with a missing parent)."),
            "statistics log");

    assertThat(syncerTestLogger.getAllLoggingEvents())
        .hasSize(2)
        .haveExactly(1, startLog)
        .haveExactly(1, statisticsLog);
  }

  @Test
  @SuppressWarnings("unchecked")
  void sync_AsProductTypesDeltaSync_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    when(sourceClient.execute(any(ProductTypeQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    final ZonedDateTime currentCtpTimestamp = ZonedDateTime.now();
    stubClientsCustomObjectService(targetClient, currentCtpTimestamp);

    // test
    syncerFactory.sync("productTypes", "", false);

    // assertions
    verify(sourceClient, times(1)).execute(any(ProductTypeQuery.class));
    verifyTimestampGeneratorCustomObjectUpsert(
        targetClient, 1, "ProductTypeSync", DEFAULT_RUNNER_NAME);
    verifyLastSyncCustomObjectQuery(targetClient, "productTypeSync", DEFAULT_RUNNER_NAME, "foo", 1);
    // verify two custom object upserts : 1. current ctp timestamp and 2. last sync timestamp
    // creation)
    verify(targetClient, times(2)).execute(any(CustomObjectUpsertCommand.class));
    // TODO: Assert on actual last sync timestamp creation in detail after Statistics classes in
    // java-sync library
    // TODO: override #equals method:
    // https://github.com/commercetools/commercetools-sync-java/issues/376
    // TODO: e.g. verifyNewLastSyncCustomObjectCreation(targetClient,
    // currentCtpTimestamp.minusMinutes(2), any(ProductSyncStatistics.class), 0L, "productSync",
    // "foo");
    verifyInteractionsWithClientAfterSync(sourceClient, 1);

    final Condition<LoggingEvent> startLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent.getMessage().contains("Starting ProductTypeSync"),
            "start log");

    final Condition<LoggingEvent> statisticsLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent
                        .getMessage()
                        .contains(
                            "Summary: 0 product types were processed in total (0 created, 0 updated, 0 failed to sync"
                                + " and 0 product types with at least one NestedType or a Set of NestedType attribute"
                                + " definition(s) referencing a missing product type)."),
            "statistics log");

    assertThat(syncerTestLogger.getAllLoggingEvents())
        .hasSize(2)
        .haveExactly(1, startLog)
        .haveExactly(1, statisticsLog);
  }

  @Test
  @SuppressWarnings("unchecked")
  void sync_AsTypesDeltaSync_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    when(sourceClient.execute(any(TypeQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));

    final ZonedDateTime currentCtpTimestamp = ZonedDateTime.now();
    stubClientsCustomObjectService(targetClient, currentCtpTimestamp);

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    // test
    syncerFactory.sync("types", "foo", false);

    // assertions
    verify(sourceClient, times(1)).execute(any(TypeQuery.class));
    verifyTimestampGeneratorCustomObjectUpsert(targetClient, 1, "TypeSync", "foo");
    verifyLastSyncCustomObjectQuery(targetClient, "typeSync", "foo", "foo", 1);
    // verify two custom object upserts : 1. current ctp timestamp and 2. last sync timestamp
    // creation)
    verify(targetClient, times(2)).execute(any(CustomObjectUpsertCommand.class));
    // TODO: Assert on actual last sync timestamp creation in detail after Statistics classes in
    // java-sync library
    // TODO: override #equals method:
    // https://github.com/commercetools/commercetools-sync-java/issues/376
    // TODO: e.g. verifyNewLastSyncCustomObjectCreation(targetClient,
    // currentCtpTimestamp.minusMinutes(2), any(ProductSyncStatistics.class), 0L, "productSync",
    // "foo");
    verifyInteractionsWithClientAfterSync(sourceClient, 1);

    final Condition<LoggingEvent> startLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent.getMessage().contains("Starting TypeSync"),
            "start log");

    final Condition<LoggingEvent> statisticsLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent
                        .getMessage()
                        .contains(
                            "Summary: 0 types were processed in total (0 created, 0 updated "
                                + "and 0 failed to sync)."),
            "statistics log");

    assertThat(syncerTestLogger.getAllLoggingEvents())
        .hasSize(2)
        .haveExactly(1, startLog)
        .haveExactly(1, statisticsLog);
  }

  @Test
  @SuppressWarnings("unchecked")
  void sync_AsInventoryEntriesDeltaSync_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    when(sourceClient.execute(any(InventoryEntryQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));

    final ZonedDateTime currentCtpTimestamp = ZonedDateTime.now();
    stubClientsCustomObjectService(targetClient, currentCtpTimestamp);

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    // test
    syncerFactory.sync("inventoryEntries", null, false);

    // assertions
    verify(sourceClient, times(1)).execute(any(InventoryEntryQuery.class));
    verifyTimestampGeneratorCustomObjectUpsert(
        targetClient, 1, "InventorySync", DEFAULT_RUNNER_NAME);
    verifyLastSyncCustomObjectQuery(targetClient, "inventorySync", DEFAULT_RUNNER_NAME, "foo", 1);
    // verify two custom object upserts : 1. current ctp timestamp and 2. last sync timestamp
    // creation)
    verify(targetClient, times(2)).execute(any(CustomObjectUpsertCommand.class));
    // TODO: Assert on actual last sync timestamp creation in detail after Statistics classes in
    // java-sync library
    // TODO: override #equals method:
    // https://github.com/commercetools/commercetools-sync-java/issues/376
    // TODO: e.g. verifyNewLastSyncCustomObjectCreation(targetClient,
    // currentCtpTimestamp.minusMinutes(2), any(ProductSyncStatistics.class), 0L, "productSync",
    // "foo");
    verifyInteractionsWithClientAfterSync(sourceClient, 1);

    final Condition<LoggingEvent> startLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent.getMessage().contains("Starting InventorySync"),
            "start log");

    final Condition<LoggingEvent> statisticsLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent
                        .getMessage()
                        .contains(
                            "Summary: 0 inventory entries were processed in total (0 created, 0 updated "
                                + "and 0 failed to sync)."),
            "statistics log");

    assertThat(syncerTestLogger.getAllLoggingEvents())
        .hasSize(2)
        .haveExactly(1, startLog)
        .haveExactly(1, statisticsLog);
  }

  @Test
  void sync_WithErrorOnFetch_ShouldCloseClientAndCompleteExceptionally() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    final BadGatewayException badGatewayException = new BadGatewayException();
    when(sourceClient.execute(any(InventoryEntryQuery.class)))
        .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(badGatewayException));

    final ZonedDateTime currentCtpTimestamp = ZonedDateTime.now();
    stubClientsCustomObjectService(targetClient, currentCtpTimestamp);

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    // test
    final CompletionStage<Void> result = syncerFactory.sync("inventoryEntries", null, false);

    // assertions
    verifyTimestampGeneratorCustomObjectUpsert(
        targetClient, 1, "InventorySync", DEFAULT_RUNNER_NAME);
    verify(sourceClient, times(1)).execute(any(InventoryEntryQuery.class));
    verifyInteractionsWithClientAfterSync(sourceClient, 1);
    assertThat(result).hasFailedWithThrowableThat().isExactlyInstanceOf(BadGatewayException.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  void
      sync_WithErrorOnCurrentCtpTimestampUpsert_ShouldCloseClientAndCompleteExceptionallyWithoutSyncing() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    final BadGatewayException badGatewayException = new BadGatewayException();
    when(targetClient.execute(any(CustomObjectUpsertCommand.class)))
        .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(badGatewayException));

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    // test
    final CompletionStage<Void> result = syncerFactory.sync("inventoryEntries", "", false);

    // assertions
    verifyTimestampGeneratorCustomObjectUpsert(
        targetClient, 1, "InventorySync", DEFAULT_RUNNER_NAME);
    verify(sourceClient, times(0)).execute(any(InventoryEntryQuery.class));
    verifyInteractionsWithClientAfterSync(sourceClient, 1);
    assertThat(result).hasFailedWithThrowableThat().isExactlyInstanceOf(BadGatewayException.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  void
      sync_WithErrorOnQueryLastSyncTimestamp_ShouldCloseClientAndCompleteExceptionallyWithoutSyncing() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    final BadGatewayException badGatewayException = new BadGatewayException();
    when(targetClient.execute(any(CustomObjectQuery.class)))
        .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(badGatewayException));

    final CustomObject<LastSyncCustomObject<ProductSyncStatistics>>
        lastSyncCustomObjectCustomObject = mockLastSyncCustomObject(ZonedDateTime.now());
    when(targetClient.execute(any(CustomObjectUpsertCommand.class)))
        .thenReturn(CompletableFuture.completedFuture(lastSyncCustomObjectCustomObject));

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    // test
    final CompletionStage<Void> result = syncerFactory.sync("inventoryEntries", "bar", false);

    // assertions
    verifyTimestampGeneratorCustomObjectUpsert(targetClient, 1, "InventorySync", "bar");
    verifyLastSyncCustomObjectQuery(targetClient, "inventorySync", "bar", "foo", 1);
    verify(sourceClient, times(0)).execute(any(InventoryEntryQuery.class));
    verifyInteractionsWithClientAfterSync(sourceClient, 1);
    assertThat(result).hasFailedWithThrowableThat().isExactlyInstanceOf(BadGatewayException.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  @Disabled
  void syncAll_AsDelta_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    when(sourceClient.execute(any(ProductTypeQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));
    when(sourceClient.execute(any(TypeQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));
    when(sourceClient.execute(any(CategoryQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));
    when(sourceClient.execute(any(InventoryEntryQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));
    when(sourceClient.execute(any(ProductQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));

    final ZonedDateTime currentCtpTimestamp = ZonedDateTime.now();
    stubClientsCustomObjectService(targetClient, currentCtpTimestamp);

    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    // test
    syncerFactory.syncAll(null, false);

    // assertions
    verifyTimestampGeneratorCustomObjectUpsert(
        targetClient, 1, "ProductTypeSync", DEFAULT_RUNNER_NAME);
    verifyTimestampGeneratorCustomObjectUpsert(targetClient, 1, "ProductSync", DEFAULT_RUNNER_NAME);
    verifyTimestampGeneratorCustomObjectUpsert(
        targetClient, 1, "CategorySync", DEFAULT_RUNNER_NAME);
    verifyTimestampGeneratorCustomObjectUpsert(targetClient, 1, "TypeSync", DEFAULT_RUNNER_NAME);
    verifyTimestampGeneratorCustomObjectUpsert(
        targetClient, 1, "InventorySync", DEFAULT_RUNNER_NAME);
    verify(targetClient, times(10)).execute(any(CustomObjectUpsertCommand.class));
    verifyLastSyncCustomObjectQuery(targetClient, "inventorySync", DEFAULT_RUNNER_NAME, "foo", 1);
    verifyLastSyncCustomObjectQuery(targetClient, "productTypeSync", DEFAULT_RUNNER_NAME, "foo", 1);
    verifyLastSyncCustomObjectQuery(targetClient, "productSync", DEFAULT_RUNNER_NAME, "foo", 1);
    verifyLastSyncCustomObjectQuery(targetClient, "categorySync", DEFAULT_RUNNER_NAME, "foo", 1);
    verifyLastSyncCustomObjectQuery(targetClient, "typeSync", DEFAULT_RUNNER_NAME, "foo", 1);
    verify(sourceClient, times(1)).execute(any(ProductTypeQuery.class));
    verify(sourceClient, times(1)).execute(any(TypeQuery.class));
    verify(sourceClient, times(1)).execute(any(CategoryQuery.class));
    verify(sourceClient, times(1)).execute(any(ProductQuery.class));
    verify(sourceClient, times(1)).execute(any(InventoryEntryQuery.class));
    verifyInteractionsWithClientAfterSync(sourceClient, 5);
    assertAllSyncersLoggingEvents(syncerTestLogger, cliRunnerTestLogger, 0);
  }
}
