package com.commercetools.project.sync.model;

import com.commercetools.project.sync.util.SyncUtils;
import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.ZonedDateTime;
import javax.annotation.Nonnull;

public class LastSyncCustomObject<T extends BaseSyncStatistics> {

  @JsonIgnoreProperties({
    "latestBatchStartTime",
    "latestBatchProcessingTimeInDays",
    "latestBatchProcessingTimeInHours",
    "latestBatchProcessingTimeInMinutes",
    "latestBatchProcessingTimeInSeconds",
    "latestBatchProcessingTimeInMillis",
    "latestBatchHumanReadableProcessingTime"
  })
  private T lastSyncStatistics;

  private ZonedDateTime lastSyncTimestamp;
  private String applicationVersion;
  private long lastSyncDurationInMillis;

  private LastSyncCustomObject(
      @Nonnull final ZonedDateTime lastSyncTimestamp,
      @Nonnull final T lastSyncStatistics,
      @Nonnull final String applicationVersion,
      final long lastSyncDurationInMillis) {

    this.lastSyncTimestamp = lastSyncTimestamp;
    this.lastSyncStatistics = lastSyncStatistics;
    this.applicationVersion = applicationVersion;
    this.lastSyncDurationInMillis = lastSyncDurationInMillis;
  }

  public LastSyncCustomObject() {}

  @Nonnull
  public static <T extends BaseSyncStatistics> LastSyncCustomObject<T> of(
      @Nonnull final ZonedDateTime lastSyncTimestamp,
      @Nonnull final T lastSyncStatistics,
      final long lastSyncDurationInSeconds) {

    return new LastSyncCustomObject<>(
        lastSyncTimestamp,
        lastSyncStatistics,
        SyncUtils.getApplicationVersion(),
        lastSyncDurationInSeconds);
  }

  public ZonedDateTime getLastSyncTimestamp() {
    return lastSyncTimestamp;
  }

  public T getLastSyncStatistics() {
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
