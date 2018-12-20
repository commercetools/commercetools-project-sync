package com.commercetools.project.sync.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.sync.categories.helpers.CategorySyncStatistics;
import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;
import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

public class StatisticsUtilsTest {
  private static final TestLogger testLogger =
      TestLoggerFactory.getTestLogger(StatisticsUtils.class);
  private BaseSyncStatistics syncStatistics;

  @Test
  public void logStatistics_WithProductSyncStatistics_ShouldLogStatistics()
      throws JsonProcessingException {
    syncStatistics = new ProductSyncStatistics();
    syncStatistics.incrementCreated(10);
    syncStatistics.incrementFailed(10);
    syncStatistics.incrementUpdated(10);
    syncStatistics.incrementProcessed(30);

    final String statisticsAsJSONString = StatisticsUtils.getStatisticsAsJSONString(syncStatistics);

    StatisticsUtils.logStatistics(syncStatistics, testLogger);

    assertThat(testLogger.getAllLoggingEvents()).hasSize(1);
    final LoggingEvent loggingEvent = testLogger.getAllLoggingEvents().get(0);
    assertThat(loggingEvent).isExactlyInstanceOf(LoggingEvent.class);
    assertThat(loggingEvent.getLevel()).isEqualTo(Level.INFO);
    assertThat(loggingEvent.getMessage()).contains(statisticsAsJSONString);
  }

  @Test
  public void getStatisticsAsJSONString_WithProductSyncStatistics_ShouldBuildJsonString()
      throws JsonProcessingException {
    syncStatistics = new ProductSyncStatistics();
    syncStatistics.incrementCreated(10);
    syncStatistics.incrementFailed(10);
    syncStatistics.incrementUpdated(10);
    syncStatistics.incrementProcessed(30);

    final String statisticsAsJSONString = StatisticsUtils.getStatisticsAsJSONString(syncStatistics);
    assertThat(statisticsAsJSONString).contains("\"created\":10");
    assertThat(statisticsAsJSONString).contains("\"failed\":10");
    assertThat(statisticsAsJSONString).contains("\"updated\":10");
    assertThat(statisticsAsJSONString).contains("\"processed\":30");
  }

  @Test
  public void getStatisticsAsJSONString_WithCategorySyncStatistics_ShouldBuildJsonString()
      throws JsonProcessingException {
    syncStatistics = new CategorySyncStatistics();
    syncStatistics.incrementCreated(10);
    syncStatistics.incrementProcessed(30);

    final String statisticsAsJSONString = StatisticsUtils.getStatisticsAsJSONString(syncStatistics);
    assertThat(statisticsAsJSONString).contains("\"created\":10");
    assertThat(statisticsAsJSONString).contains("\"processed\":30");
    assertThat(statisticsAsJSONString).contains("\"categoryKeysWithMissingParents\":{}");
  }
}
