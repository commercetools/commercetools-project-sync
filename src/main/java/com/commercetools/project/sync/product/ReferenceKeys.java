package com.commercetools.project.sync.product;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.sphere.sdk.models.Base;

import java.util.List;

public class ReferenceKeys extends Base {
    private final List<ReferenceKey> referenceKeys;

    @JsonCreator
    public ReferenceKeys(final List<ReferenceKey> referenceKeys) {
        this.referenceKeys = referenceKeys;
    }

    public List<ReferenceKey> getReferenceKeys() {
        return referenceKeys;
    }
}