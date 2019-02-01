package com.commercetools.project.sync.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.project.sync.util.SyncUtils;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class LastSyncCustomObjectTest {

  @Test
  void of_WithProductSyncStatistics_ShouldBuildCorrectLastSyncTimestamp() {
    // preparation
    final ZonedDateTime lastSyncTimestamp = ZonedDateTime.now();
    final int lastSyncDurationInSeconds = 100;
    final ProductSyncStatistics lastSyncStatistics = new ProductSyncStatistics();

    // test
    final LastSyncCustomObject<ProductSyncStatistics> lastSyncCustomObject =
        LastSyncCustomObject.of(lastSyncTimestamp, lastSyncStatistics, lastSyncDurationInSeconds);

    // assertions
    assertThat(lastSyncCustomObject.getLastSyncTimestamp()).isEqualTo(lastSyncTimestamp);
    assertThat(lastSyncCustomObject.getApplicationVersion())
        .isEqualTo(SyncUtils.getApplicationVersion());
    assertThat(lastSyncCustomObject.getLastSyncDurationInMillis())
        .isEqualTo(lastSyncDurationInSeconds);
    assertThat(lastSyncCustomObject.getLastSyncStatistics()).isEqualTo(lastSyncStatistics);
  }
}
