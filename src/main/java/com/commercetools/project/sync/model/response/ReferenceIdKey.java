package com.commercetools.project.sync.model.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ReferenceIdKey {
  private final String id;
  private final String key;

  @JsonCreator
  public ReferenceIdKey(
      @JsonProperty("id") @Nonnull final String id,
      @JsonProperty("key") @Nullable final String key) {
    this.id = id;
    this.key = key;
  }

  public String getId() {
    return id;
  }

  public String getKey() {
    return key;
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReferenceIdKey)) {
      return false;
    }
    final ReferenceIdKey that = (ReferenceIdKey) other;
    return Objects.equals(getId(), that.getId()) && Objects.equals(getKey(), that.getKey());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getKey());
  }
}
