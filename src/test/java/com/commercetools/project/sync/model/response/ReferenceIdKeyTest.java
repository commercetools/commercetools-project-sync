package com.commercetools.project.sync.model.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReferenceIdKeyTest {

  @Test
  void newReferenceIdKey_WithNullKey_ShouldInstantiateCorrectly() {
    // preparation
    final String id = UUID.randomUUID().toString();

    // test
    final ReferenceIdKey referenceIdKey = new ReferenceIdKey(id, null);

    // assertion
    assertThat(referenceIdKey.getId()).isEqualTo(id);
    assertThat(referenceIdKey.getKey()).isNull();
  }

  @Test
  void newReferenceIdKey_WithNonNullKey_ShouldInstantiateCorrectly() {
    // preparation
    final String id = UUID.randomUUID().toString();

    // test
    final ReferenceIdKey referenceIdKey = new ReferenceIdKey(id, "foo");

    // assertion
    assertThat(referenceIdKey.getId()).isEqualTo(id);
    assertThat(referenceIdKey.getKey()).isEqualTo("foo");
  }

  @Test
  void equals_WithSameRef_ShouldReturnTrue() {
    // preparation
    final ReferenceIdKey referenceIdKey = new ReferenceIdKey(UUID.randomUUID().toString(), "foo");
    final ReferenceIdKey other = referenceIdKey;

    // test
    final boolean result = referenceIdKey.equals(other);

    // assertion
    assertThat(result).isTrue();
  }

  @Test
  void equals_WithDiffType_ShouldReturnFalse() {
    // preparation
    final ReferenceIdKey referenceIdKey = new ReferenceIdKey(UUID.randomUUID().toString(), "foo");
    final Object other = "";

    // test
    final boolean result = referenceIdKey.equals(other);

    // assertion
    assertThat(result).isFalse();
  }

  @Test
  void equals_WithDiffId_ShouldReturnFalse() {
    // preparation
    final ReferenceIdKey referenceIdKey = new ReferenceIdKey(UUID.randomUUID().toString(), "foo");
    final Object other = new ReferenceIdKey(UUID.randomUUID().toString(), "foo");

    // test
    final boolean result = referenceIdKey.equals(other);

    // assertion
    assertThat(result).isFalse();
  }

  @Test
  void equals_WithDiffKey_ShouldReturnFalse() {
    // preparation
    final String id = UUID.randomUUID().toString();
    final ReferenceIdKey referenceIdKey = new ReferenceIdKey(id, "foo");
    final Object other = new ReferenceIdKey(id, "bar");

    // test
    final boolean result = referenceIdKey.equals(other);

    // assertion
    assertThat(result).isFalse();
  }

  @Test
  void equals_WithEqualValues_ShouldReturnTrue() {
    // preparation
    final String id = UUID.randomUUID().toString();
    final ReferenceIdKey referenceIdKey = new ReferenceIdKey(id, "foo");
    final Object other = new ReferenceIdKey(id, "foo");

    // test
    final boolean result = referenceIdKey.equals(other);

    // assertion
    assertThat(result).isTrue();
  }

  @Test
  void hashCode_WithDiffId_ShouldBeDifferent() {
    // preparation
    final ReferenceIdKey referenceIdKey = new ReferenceIdKey(UUID.randomUUID().toString(), "foo");
    final Object other = new ReferenceIdKey(UUID.randomUUID().toString(), "foo");

    // test
    final boolean result = referenceIdKey.hashCode() == other.hashCode();

    // assertion
    assertThat(result).isFalse();
  }

  @Test
  void hashCode_WithDiffKey_ShouldReturnFalse() {
    // preparation
    final String id = UUID.randomUUID().toString();
    final ReferenceIdKey referenceIdKey = new ReferenceIdKey(id, "foo");
    final Object other = new ReferenceIdKey(id, "bar");

    // test
    final boolean result = referenceIdKey.hashCode() == other.hashCode();

    // assertion
    assertThat(result).isFalse();
  }

  @Test
  void hashCode_WithEqualValues_ShouldReturnTrue() {
    // preparation
    final String id = UUID.randomUUID().toString();
    final ReferenceIdKey referenceIdKey = new ReferenceIdKey(id, "foo");
    final Object other = new ReferenceIdKey(id, "foo");

    // test
    final boolean result = referenceIdKey.hashCode() == other.hashCode();

    // assertion
    assertThat(result).isTrue();
  }
}
