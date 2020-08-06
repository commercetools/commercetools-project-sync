package com.commercetools.project.sync.util;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.commercetools.project.sync.model.response.LastSyncCustomObject;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.commands.CustomObjectUpsertCommand;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import org.assertj.core.api.Condition;
import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;

public final class TestUtils {

  public static void assertAllSyncersLoggingEvents(
      @Nonnull final TestLogger syncerTestLogger,
      @Nonnull final TestLogger cliRunnerTestLogger,
      final int numberOfResources) {

    assertThat(cliRunnerTestLogger.getAllLoggingEvents())
        .allMatch(loggingEvent -> !Level.ERROR.equals(loggingEvent.getLevel()));

    assertThat(syncerTestLogger.getAllLoggingEvents())
        .allMatch(loggingEvent -> !Level.ERROR.equals(loggingEvent.getLevel()));

    assertTypeSyncerLoggingEvents(syncerTestLogger, numberOfResources);
    assertProductTypeSyncerLoggingEvents(syncerTestLogger, numberOfResources);
    assertCategorySyncerLoggingEvents(syncerTestLogger, numberOfResources);
    assertProductSyncerLoggingEvents(syncerTestLogger, numberOfResources);
    assertInventoryEntrySyncerLoggingEvents(syncerTestLogger, numberOfResources);
    assertCartDiscountSyncerLoggingEvents(syncerTestLogger, numberOfResources);
    assertStateSyncerLoggingEvents(
        syncerTestLogger,
        numberOfResources + 1); // 2 states as 1 state is built-in and it cant be deleted

    // Every sync module (6 modules) is expected to have 2 logs (start and stats summary) = 12
    assertThat(syncerTestLogger.getAllLoggingEvents()).hasSize(14);
  }

  public static void assertTypeSyncerLoggingEvents(
      @Nonnull final TestLogger syncerTestLogger, final int numberOfResources) {
    final String typeStatsSummary =
        format(
            "Summary: %d types were processed in total (%d created, 0 updated "
                + "and 0 failed to sync).",
            numberOfResources, numberOfResources);

    assertSyncerLoggingEvents(syncerTestLogger, "TypeSync", typeStatsSummary);
  }

  public static void assertProductTypeSyncerLoggingEvents(
      @Nonnull final TestLogger syncerTestLogger, final int numberOfResources) {
    final String productTypesStatsSummary =
        format(
            "Summary: %d product types were processed in total (%d created, 0 updated, 0 failed to sync and 0 product"
                + " types with at least one NestedType or a Set of NestedType "
                + "attribute definition(s) referencing a missing product type).",
            numberOfResources, numberOfResources);

    assertSyncerLoggingEvents(syncerTestLogger, "ProductTypeSync", productTypesStatsSummary);
  }

  public static void assertCategorySyncerLoggingEvents(
      @Nonnull final TestLogger syncerTestLogger, final int numberOfResources) {
    final String categoryStatsSummary =
        format(
            "Summary: %d categories were processed in total (%d created, 0 updated, "
                + "0 failed to sync and 0 categories with a missing parent).",
            numberOfResources, numberOfResources);

    assertSyncerLoggingEvents(syncerTestLogger, "CategorySync", categoryStatsSummary);
  }

  public static void assertProductSyncerLoggingEvents(
      @Nonnull final TestLogger syncerTestLogger, final int numberOfResources) {
    final String productStatsSummary =
        format(
            "Summary: %d product(s) were processed in total (%d created, 0 updated, "
                + "0 failed to sync and 0 product(s) with missing reference(s)).",
            numberOfResources, numberOfResources);

    assertSyncerLoggingEvents(syncerTestLogger, "ProductSync", productStatsSummary);
  }

  public static void assertInventoryEntrySyncerLoggingEvents(
      @Nonnull final TestLogger syncerTestLogger, final int numberOfResources) {
    final String inventoryStatsSummary =
        format(
            "Summary: %d inventory entries were processed in total (%d created, 0 updated "
                + "and 0 failed to sync).",
            numberOfResources, numberOfResources);

    assertSyncerLoggingEvents(syncerTestLogger, "InventorySync", inventoryStatsSummary);
  }

  public static void assertCartDiscountSyncerLoggingEvents(
      @Nonnull final TestLogger syncerTestLogger, final int numberOfResources) {
    final String cartDiscountStatsSummary =
        format(
            "Summary: %d cart discounts were processed in total (%d created, 0 updated "
                + "and 0 failed to sync).",
            numberOfResources, numberOfResources);

    assertSyncerLoggingEvents(syncerTestLogger, "CartDiscount", cartDiscountStatsSummary);
  }

  public static void assertStateSyncerLoggingEvents(
      @Nonnull final TestLogger syncerTestLogger, final int numberOfResources) {
    final String stateSyncerStatsSummary =
        format(
            "Summary: %d state(s) were processed in total (%d created, 0 updated, 0 failed to sync and 0 state(s) with missing transition(s)).",
            numberOfResources,
            Math.max(
                0,
                numberOfResources
                    - 1)); // 1 state is automatically built-in in all projects and processed, but
    // it's not created

    assertSyncerLoggingEvents(syncerTestLogger, "State", stateSyncerStatsSummary);
  }

  public static void assertSyncerLoggingEvents(
      @Nonnull final TestLogger testLogger,
      @Nonnull final String syncModuleName,
      @Nonnull final String statisticsSummary) {

    final Condition<LoggingEvent> startLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent.getMessage().contains(format("Starting %s", syncModuleName)),
            format("%s start log", syncModuleName));

    final Condition<LoggingEvent> statisticsLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent.getMessage().contains(statisticsSummary),
            format("%s statistics log", syncModuleName));

    assertThat(testLogger.getAllLoggingEvents())
        .haveExactly(1, startLog)
        .haveExactly(1, statisticsLog);
  }

  public static void verifyInteractionsWithClientAfterSync(
      @Nonnull final SphereClient client, final int numberOfGetConfigInvocations) {

    verify(client, times(1)).close();
    // Verify config is accessed for the success message after sync:
    // " example: Syncing products from CTP project with key 'x' to project with key 'y' is done","
    verify(client, times(numberOfGetConfigInvocations)).getConfig();
    verifyNoMoreInteractions(client);
  }

  @SuppressWarnings("unchecked")
  public static void stubClientsCustomObjectService(
      @Nonnull final SphereClient client, @Nonnull final ZonedDateTime currentCtpTimestamp) {

    final CustomObject<LastSyncCustomObject<ProductSyncStatistics>> customObject =
        mockLastSyncCustomObject(currentCtpTimestamp);

    when(client.execute(any(CustomObjectUpsertCommand.class)))
        .thenReturn(CompletableFuture.completedFuture(customObject));

    final PagedQueryResult<CustomObject<LastSyncCustomObject<ProductSyncStatistics>>>
        queriedCustomObjects = spy(PagedQueryResult.empty());
    when(queriedCustomObjects.getResults()).thenReturn(singletonList(customObject));

    when(client.execute(any(CustomObjectQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(queriedCustomObjects));
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  public static CustomObject<LastSyncCustomObject<ProductSyncStatistics>> mockLastSyncCustomObject(
      @Nonnull ZonedDateTime currentCtpTimestamp) {
    final CustomObject<LastSyncCustomObject<ProductSyncStatistics>> customObject =
        mock(CustomObject.class);

    final LastSyncCustomObject<ProductSyncStatistics> lastSyncCustomObject =
        LastSyncCustomObject.of(ZonedDateTime.now(), new ProductSyncStatistics(), 100);

    when(customObject.getLastModifiedAt()).thenReturn(currentCtpTimestamp);
    when(customObject.getValue()).thenReturn(lastSyncCustomObject);
    return customObject;
  }

  @Nonnull
  public static Clock getMockedClock() {
    final Clock clock = mock(Clock.class);
    when(clock.millis()).thenReturn(0L);
    return clock;
  }

  private TestUtils() {}
}
