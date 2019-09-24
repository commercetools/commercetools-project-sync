package com.commercetools.project.sync.model.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;

public class ResultingResourcesContainer {
  private final Set<ReferenceIdKey> results;

  @JsonCreator
  public ResultingResourcesContainer(@JsonProperty("results") final Set<ReferenceIdKey> results) {
    this.results = results;
  }

  public Set<ReferenceIdKey> getResults() {
    return results;
  }

  @Override
  public boolean equals(@Nullable final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ResultingResourcesContainer)) {
      return false;
    }
    final ResultingResourcesContainer that = (ResultingResourcesContainer) other;
    return getResults().equals(that.getResults());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getResults());
  }
}
