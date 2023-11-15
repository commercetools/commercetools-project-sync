package com.commercetools.project.sync.model.response;

import com.commercetools.project.sync.BaseSyncStatisticsDeserializer;
import com.commercetools.project.sync.util.SyncUtils;
import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.ZonedDateTime;
import java.util.Objects;
import javax.annotation.Nonnull;

public final class LastSyncCustomObject<T extends BaseSyncStatistics> {

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
      final long lastSyncDurationInMillis) {

    this.lastSyncTimestamp = lastSyncTimestamp;
    this.lastSyncStatistics = lastSyncStatistics;
    this.applicationVersion = SyncUtils.getApplicationVersion();
    this.lastSyncDurationInMillis = lastSyncDurationInMillis;
  }

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

  // Needed for the 'com.fasterxml.jackson' deserialization, for example, when fetching
  // from CTP custom objects.
  public LastSyncCustomObject() {}

  @Nonnull
  public static <T extends BaseSyncStatistics<T>> LastSyncCustomObject<T> of(
      @Nonnull final ZonedDateTime lastSyncTimestamp,
      @Nonnull final T lastSyncStatistics,
      final long lastSyncDurationInSeconds) {

    return new LastSyncCustomObject<>(
        lastSyncTimestamp, lastSyncStatistics, lastSyncDurationInSeconds);
  }

  @Nonnull
  public static <T extends BaseSyncStatistics<T>> LastSyncCustomObject<T> of(
      @Nonnull final ZonedDateTime lastSyncTimestamp,
      @Nonnull final T lastSyncStatistics,
      @Nonnull final String applicationVersion,
      final long lastSyncDurationInSeconds) {

    return new LastSyncCustomObject<>(
        lastSyncTimestamp, lastSyncStatistics, applicationVersion, lastSyncDurationInSeconds);
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

  // Setters are needed for the 'com.fasterxml.jackson' deserialization, for example, when fetching
  // from CTP custom objects.
  public void setLastSyncTimestamp(@Nonnull final ZonedDateTime lastSyncTimestamp) {
    this.lastSyncTimestamp = lastSyncTimestamp;
  }

  @JsonDeserialize(using = BaseSyncStatisticsDeserializer.class)
  public void setLastSyncStatistics(T lastSyncStatistics) {
    this.lastSyncStatistics = lastSyncStatistics;
  }

  public void setApplicationVersion(@Nonnull final String applicationVersion) {
    this.applicationVersion = applicationVersion;
  }

  public void setLastSyncDurationInMillis(final long lastSyncDurationInMillis) {
    this.lastSyncDurationInMillis = lastSyncDurationInMillis;
  }

  // TODO: Also include statistics in equals comparison after
  // https://github.com/commercetools/commercetools-sync-java/issues/376 is resolved
  // https://github.com/commercetools/commercetools-project-sync/issues/28
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof LastSyncCustomObject)) {
      return false;
    }
    LastSyncCustomObject<?> that = (LastSyncCustomObject<?>) o;
    return getLastSyncDurationInMillis() == that.getLastSyncDurationInMillis()
        && Objects.equals(getLastSyncTimestamp(), that.getLastSyncTimestamp())
        && Objects.equals(getApplicationVersion(), that.getApplicationVersion());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getLastSyncTimestamp(), getApplicationVersion(), getLastSyncDurationInMillis());
  }
}
