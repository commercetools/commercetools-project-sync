package com.commercetools.project.sync.model.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.sphere.sdk.models.Base;

import java.util.List;

public class ReferenceKeys extends Base {
    private final List<ReferenceKey> results;

    @JsonCreator
    public ReferenceKeys(@JsonProperty("results") final List<ReferenceKey> results) {
        this.results = results;
    }

    public List<ReferenceKey> getResults() {
        return results;
    }
}