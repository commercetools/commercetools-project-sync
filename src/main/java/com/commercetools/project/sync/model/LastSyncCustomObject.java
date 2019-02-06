package com.commercetools.project.sync.model;

import com.commercetools.project.sync.util.SyncUtils;
import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

  // Needed for the 'com.fasterxml.jackson' deserialization, for example, when fetching
  // from CTP custom objects.
  public LastSyncCustomObject() {}

  @Nonnull
  public static <T extends BaseSyncStatistics> LastSyncCustomObject<T> of(
      @Nonnull final ZonedDateTime lastSyncTimestamp,
      @Nonnull final T lastSyncStatistics,
      final long lastSyncDurationInSeconds) {

    return new LastSyncCustomObject<>(
        lastSyncTimestamp, lastSyncStatistics, lastSyncDurationInSeconds);
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

  // TODO: Implement a settter for lastSyncStatistics too to be able to deserialize statisitcs. It
  // needs a
  // customized deserializer for because BaseSyncStatistics is an abstract class:
  // https://www.baeldung.com/jackson-inheritance
  // https://github.com/commercetools/commercetools-project-sync/issues/27

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
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof LastSyncCustomObject)) {
      return false;
    }
    final LastSyncCustomObject<?> that = (LastSyncCustomObject<?>) o;
    return getLastSyncDurationInMillis() == that.getLastSyncDurationInMillis()
        && getLastSyncTimestamp().equals(that.getLastSyncTimestamp());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getLastSyncTimestamp(), getLastSyncDurationInMillis());
  }
}
