package com.commercetools.project.sync.model.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public class ResultingResourcesContainer {
  private final Set<ReferenceIdKey> results;

  @JsonCreator
  public ResultingResourcesContainer(@JsonProperty("results") final Set<ReferenceIdKey> results) {
    this.results = results;
  }

  public Set<ReferenceIdKey> getResults() {
    return results;
  }
}
