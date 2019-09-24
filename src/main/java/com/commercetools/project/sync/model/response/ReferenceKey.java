package com.commercetools.project.sync.model.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.sphere.sdk.models.Base;

public class ReferenceKey extends Base {
  private final String id;
  private final String key;

  @JsonCreator
  public ReferenceKey(@JsonProperty("id") final String id, @JsonProperty("key") final String key) {
    this.id = id;
    this.key = key;
  }

  public String getId() {
    return id;
  }

  public String getKey() {
    return key;
  }
}
