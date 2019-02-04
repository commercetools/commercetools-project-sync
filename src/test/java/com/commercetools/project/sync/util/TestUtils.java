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

import com.commercetools.project.sync.model.LastSyncCustomObject;
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
      @Nonnull final TestLogger testLogger, final int numberOfResources) {

    final Condition<LoggingEvent> typesStartLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent.getMessage().contains("Starting TypeSync"),
            "types start log");

    final Condition<LoggingEvent> typesStatisticsLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent
                        .getMessage()
                        .contains(
                            format(
                                "Summary: %d types were processed in total (%d created, 0 updated "
                                    + "and 0 failed to sync).",
                                numberOfResources, numberOfResources)),
            "TypeSync statistics log");

    final Condition<LoggingEvent> productTypesStartLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent.getMessage().contains("Starting ProductTypeSync"),
            "ProductTypes start log");

    final Condition<LoggingEvent> productTypesStatisticsLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent
                        .getMessage()
                        .contains(
                            format(
                                "Summary: %d product types were processed in total (%d created, 0 updated "
                                    + "and 0 failed to sync).",
                                numberOfResources, numberOfResources)),
            "ProductTypeSync statistics log");

    final Condition<LoggingEvent> categoriesStartLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent.getMessage().contains("Starting CategorySync"),
            "categories start log");

    final Condition<LoggingEvent> categoriesStatisticsLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent
                        .getMessage()
                        .contains(
                            format(
                                "Summary: %d categories were processed in total (%d created, 0 updated, "
                                    + "0 failed to sync and 0 categories with a missing parent).",
                                numberOfResources, numberOfResources)),
            "CategorySync statistics log");

    final Condition<LoggingEvent> productsStartLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent.getMessage().contains("Starting ProductSync"),
            "products start log");

    final Condition<LoggingEvent> productsStatisticsLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent
                        .getMessage()
                        .contains(
                            format(
                                "Summary: %d products were processed in total (%d created, 0 updated "
                                    + "and 0 failed to sync).",
                                numberOfResources, numberOfResources)),
            "ProductSync statistics log");

    final Condition<LoggingEvent> inventoriesStartLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent.getMessage().contains("Starting InventorySync"),
            "inventories start log");

    final Condition<LoggingEvent> inventoriesStatisticsLog =
        new Condition<>(
            loggingEvent ->
                Level.INFO.equals(loggingEvent.getLevel())
                    && loggingEvent
                        .getMessage()
                        .contains(
                            format(
                                "Summary: %d inventory entries were processed in total (%d created, 0 updated "
                                    + "and 0 failed to sync).",
                                numberOfResources, numberOfResources)),
            "InventorySync statistics log");

    assertThat(testLogger.getAllLoggingEvents())
        .hasSize(10)
        .haveExactly(1, typesStartLog)
        .haveExactly(1, productTypesStartLog)
        .haveExactly(1, categoriesStartLog)
        .haveExactly(1, productsStartLog)
        .haveExactly(1, inventoriesStartLog)
        .haveExactly(1, typesStatisticsLog)
        .haveExactly(1, productTypesStatisticsLog)
        .haveExactly(1, categoriesStatisticsLog)
        .haveExactly(1, productsStatisticsLog)
        .haveExactly(1, inventoriesStatisticsLog);
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
