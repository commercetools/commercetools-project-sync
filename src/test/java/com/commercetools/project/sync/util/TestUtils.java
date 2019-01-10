package com.commercetools.project.sync.util;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

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

  private TestUtils() {}
}
