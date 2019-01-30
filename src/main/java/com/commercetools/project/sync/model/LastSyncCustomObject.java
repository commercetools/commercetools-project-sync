package com.commercetools.project.sync.model;

import com.commercetools.project.sync.util.SyncUtils;
import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.ZonedDateTime;
import javax.annotation.Nonnull;

public class LastSyncCustomObject {

  @JsonIgnoreProperties({
    "latestBatchStartTime",
    "latestBatchProcessingTimeInDays",
    "latestBatchProcessingTimeInHours",
    "latestBatchProcessingTimeInMinutes",
    "latestBatchProcessingTimeInSeconds",
    "latestBatchProcessingTimeInMillis",
    "latestBatchHumanReadableProcessingTime"
  })
  private BaseSyncStatistics lastSyncStatistics;

  private ZonedDateTime lastSyncTimestamp;
  private String applicationVersion;
  private long lastSyncDurationInMillis;

  private LastSyncCustomObject(
      @Nonnull final ZonedDateTime lastSyncTimestamp,
      @Nonnull final BaseSyncStatistics lastSyncStatistics,
      @Nonnull final String applicationVersion,
      final long lastSyncDurationInMillis) {
    this.lastSyncTimestamp = lastSyncTimestamp;
    this.lastSyncStatistics = lastSyncStatistics;
    this.applicationVersion = applicationVersion;
    this.lastSyncDurationInMillis = lastSyncDurationInMillis;
  }

  public LastSyncCustomObject() {}

  @Nonnull
  public static LastSyncCustomObject of(
      @Nonnull final ZonedDateTime timestamp,
      @Nonnull final BaseSyncStatistics statistics,
      final long lastSyncDurationInSeconds) {
    return new LastSyncCustomObject(
        timestamp, statistics, SyncUtils.getApplicationVersion(), lastSyncDurationInSeconds);
  }

  public ZonedDateTime getLastSyncTimestamp() {
    return lastSyncTimestamp;
  }

  public BaseSyncStatistics getLastSyncStatistics() {
    return lastSyncStatistics;
  }

  public String getApplicationVersion() {
    return applicationVersion;
  }

  public long getLastSyncDurationInMillis() {
    return lastSyncDurationInMillis;
  }

  public void setLastSyncTimestamp(@Nonnull final ZonedDateTime lastSyncTimestamp) {
    this.lastSyncTimestamp = lastSyncTimestamp;
  }

  public void setApplicationVersion(@Nonnull final String applicationVersion) {
    this.applicationVersion = applicationVersion;
  }

  public void setLastSyncDurationInMillis(final long lastSyncDurationInMillis) {
    this.lastSyncDurationInMillis = lastSyncDurationInMillis;
  }
}
