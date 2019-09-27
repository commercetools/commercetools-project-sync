package com.commercetools.project.sync.model.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import javax.annotation.Nonnull;

public class ResultingResourcesContainer {
  private final Set<ReferenceIdKey> results;

  @JsonCreator
  public ResultingResourcesContainer(
      @JsonProperty("results") @Nonnull final Set<ReferenceIdKey> results) {
    this.results = results;
  }

  public Set<ReferenceIdKey> getResults() {
    return results;
  }
}
