package com.commercetools.project.sync.model;

import com.commercetools.project.sync.util.SyncUtils;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

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

  @Test
  void equals_WithEqualInstances_ShouldReturnTrue() {
    // preparation
    final ZonedDateTime lastSyncTimestamp = ZonedDateTime.now();
    final int lastSyncDurationInSeconds = 100;
    final ProductSyncStatistics lastSyncStatistics = new ProductSyncStatistics();
    final LastSyncCustomObject<ProductSyncStatistics> lastSyncCustomObject =
        LastSyncCustomObject.of(lastSyncTimestamp, lastSyncStatistics, lastSyncDurationInSeconds);

    final LastSyncCustomObject<ProductSyncStatistics> otherLastSyncCustomObject =
        LastSyncCustomObject.of(lastSyncTimestamp, lastSyncStatistics, lastSyncDurationInSeconds);

    // test
    final boolean result = lastSyncCustomObject.equals(otherLastSyncCustomObject);

    // assertions
    assertThat(result).isTrue();
  }

  @Test
  void equals_WithUnequalDurations_ShouldReturnFalse() {
    // preparation
    final ZonedDateTime lastSyncTimestamp = ZonedDateTime.now();
    final int lastSyncDurationInSeconds = 100;
    final ProductSyncStatistics lastSyncStatistics = new ProductSyncStatistics();
    final LastSyncCustomObject<ProductSyncStatistics> lastSyncCustomObject =
        LastSyncCustomObject.of(lastSyncTimestamp, lastSyncStatistics, lastSyncDurationInSeconds);

    final LastSyncCustomObject<ProductSyncStatistics> otherLastSyncCustomObject =
        LastSyncCustomObject.of(lastSyncTimestamp, lastSyncStatistics, 90);

    // test
    final boolean result = lastSyncCustomObject.equals(otherLastSyncCustomObject);

    // assertions
    assertThat(result).isFalse();
  }

  @Test
  void equals_WithUnequalVersions_ShouldReturnFalse() {
    // preparation
    final ZonedDateTime lastSyncTimestamp = ZonedDateTime.now();
    final int lastSyncDurationInSeconds = 100;
    final ProductSyncStatistics lastSyncStatistics = new ProductSyncStatistics();
    final LastSyncCustomObject<ProductSyncStatistics> lastSyncCustomObject =
        LastSyncCustomObject.of(
            lastSyncTimestamp, lastSyncStatistics, "foo", lastSyncDurationInSeconds);

    final LastSyncCustomObject<ProductSyncStatistics> otherLastSyncCustomObject =
        LastSyncCustomObject.of(lastSyncTimestamp, lastSyncStatistics, "bar", 90);

    // test
    final boolean result = lastSyncCustomObject.equals(otherLastSyncCustomObject);

    // assertions
    assertThat(result).isFalse();
  }

  @Test
  void equals_WithUnequalLastSyncTimeStamp_ShouldReturnFalse() {
    // preparation
    final int lastSyncDurationInSeconds = 100;
    final ProductSyncStatistics lastSyncStatistics = new ProductSyncStatistics();
    final ZonedDateTime timestamp = ZonedDateTime.now();
    final LastSyncCustomObject<ProductSyncStatistics> lastSyncCustomObject =
        LastSyncCustomObject.of(timestamp, lastSyncStatistics, lastSyncDurationInSeconds);

    final LastSyncCustomObject<ProductSyncStatistics> otherLastSyncCustomObject =
        LastSyncCustomObject.of(
            timestamp.minusMinutes(2), lastSyncStatistics, lastSyncDurationInSeconds);

    // test
    final boolean result = lastSyncCustomObject.equals(otherLastSyncCustomObject);

    // assertions
    assertThat(result).isFalse();
  }

  @Test
  void equals_WithUnequalLastSyncTimeStampAndDuration_ShouldReturnFalse() {
    // preparation
    final int lastSyncDurationInSeconds = 100;
    final ProductSyncStatistics lastSyncStatistics = new ProductSyncStatistics();
    final LastSyncCustomObject<ProductSyncStatistics> lastSyncCustomObject =
        LastSyncCustomObject.of(ZonedDateTime.now(), lastSyncStatistics, lastSyncDurationInSeconds);

    final LastSyncCustomObject<ProductSyncStatistics> otherLastSyncCustomObject =
        LastSyncCustomObject.of(ZonedDateTime.now(), lastSyncStatistics, 90);

    // test
    final boolean result = lastSyncCustomObject.equals(otherLastSyncCustomObject);

    // assertions
    assertThat(result).isFalse();
  }

  @Test
  @SuppressFBWarnings(
      value = "EC_UNRELATED_TYPES",
      justification =
          "We suppress the FB warning here, to cover the test case of using "
              + "LastSyncCustomObject#equals on comparing different types.")
  void equals_WithDifferentType_ShouldReturnFalse() {
    // preparation
    final int lastSyncDurationInSeconds = 100;
    final ProductSyncStatistics lastSyncStatistics = new ProductSyncStatistics();
    final LastSyncCustomObject<ProductSyncStatistics> lastSyncCustomObject =
        LastSyncCustomObject.of(ZonedDateTime.now(), lastSyncStatistics, lastSyncDurationInSeconds);

    // test
    final boolean result = lastSyncCustomObject.equals("foo");

    // assertions
    assertThat(result).isFalse();
  }

  @Test
  void equals_WithSameInstance_ShouldReturnTrue() {
    // preparation
    final int lastSyncDurationInSeconds = 100;
    final ProductSyncStatistics lastSyncStatistics = new ProductSyncStatistics();
    final LastSyncCustomObject<ProductSyncStatistics> lastSyncCustomObject =
        LastSyncCustomObject.of(ZonedDateTime.now(), lastSyncStatistics, lastSyncDurationInSeconds);

    // test
    final boolean result = lastSyncCustomObject.equals(lastSyncCustomObject);

    // assertions
    assertThat(result).isTrue();
  }

  @Test
  void hashCode_WithEqualInstances_ShouldBeEqual() {
    // preparation
    final ZonedDateTime timestamp = ZonedDateTime.now();
    final LastSyncCustomObject<ProductSyncStatistics> lastSyncCustomObject =
        LastSyncCustomObject.of(timestamp, new ProductSyncStatistics(), 100);

    final LastSyncCustomObject<ProductSyncStatistics> otherLastSyncCustomObject =
        LastSyncCustomObject.of(timestamp, new ProductSyncStatistics(), 100);

    // test
    final int hashCode1 = lastSyncCustomObject.hashCode();
    final int hashCode2 = otherLastSyncCustomObject.hashCode();

    // assertions
    assertThat(hashCode1).isEqualTo(hashCode2);
  }

  @Test
  void hashCode_WithUnequalDurations_ShouldNotBeEqual() {
    // preparation
    final ZonedDateTime lastSyncTimestamp = ZonedDateTime.now();
    final LastSyncCustomObject<ProductSyncStatistics> lastSyncCustomObject =
        LastSyncCustomObject.of(lastSyncTimestamp, new ProductSyncStatistics(), 100);

    final LastSyncCustomObject<ProductSyncStatistics> otherLastSyncCustomObject =
        LastSyncCustomObject.of(lastSyncTimestamp, new ProductSyncStatistics(), 90);

    // test
    final int hashCode1 = lastSyncCustomObject.hashCode();
    final int hashCode2 = otherLastSyncCustomObject.hashCode();

    // assertions
    assertThat(hashCode1).isNotEqualTo(hashCode2);
  }

  @Test
  void hashCode_WithUnequalVersions_ShouldNotBeEqual() {
    // preparation
    final ZonedDateTime lastSyncTimestamp = ZonedDateTime.now();
    final LastSyncCustomObject<ProductSyncStatistics> lastSyncCustomObject =
        LastSyncCustomObject.of(lastSyncTimestamp, new ProductSyncStatistics(), 100);

    final LastSyncCustomObject<ProductSyncStatistics> otherLastSyncCustomObject =
        LastSyncCustomObject.of(lastSyncTimestamp, new ProductSyncStatistics(), "foo", 100);

    // test
    final int hashCode1 = lastSyncCustomObject.hashCode();
    final int hashCode2 = otherLastSyncCustomObject.hashCode();

    // assertions
    assertThat(hashCode1).isNotEqualTo(hashCode2);
  }

  @Test
  void hashCode_WithUnequalLastSyncTimeStamp_ShouldNotBeEqual() {
    // preparation
    final ZonedDateTime timestamp = ZonedDateTime.now();
    final LastSyncCustomObject<ProductSyncStatistics> lastSyncCustomObject =
        LastSyncCustomObject.of(timestamp, new ProductSyncStatistics(), 100);

    final LastSyncCustomObject<ProductSyncStatistics> otherLastSyncCustomObject =
        LastSyncCustomObject.of(timestamp.minusMinutes(2), new ProductSyncStatistics(), 100);

    // test
    final int hashCode1 = lastSyncCustomObject.hashCode();
    final int hashCode2 = otherLastSyncCustomObject.hashCode();

    // assertions
    assertThat(hashCode1).isNotEqualTo(hashCode2);
  }

  @Test
  void hashCode_WithUnequalLastSyncTimeStampAndDuration_ShouldNotBeEqual() {
    // preparation
    final ZonedDateTime timestamp = ZonedDateTime.now();
    final LastSyncCustomObject<ProductSyncStatistics> lastSyncCustomObject =
        LastSyncCustomObject.of(timestamp, new ProductSyncStatistics(), 100);

    final LastSyncCustomObject<ProductSyncStatistics> otherLastSyncCustomObject =
        LastSyncCustomObject.of(timestamp.minusMinutes(2), new ProductSyncStatistics(), 90);

    // test
    final int hashCode1 = lastSyncCustomObject.hashCode();
    final int hashCode2 = otherLastSyncCustomObject.hashCode();

    // assertions
    assertThat(hashCode1).isNotEqualTo(hashCode2);
  }
}
