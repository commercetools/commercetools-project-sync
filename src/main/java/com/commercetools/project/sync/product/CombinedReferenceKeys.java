package com.commercetools.project.sync.product;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.sphere.sdk.models.Base;

public class CombinedReferenceKeys extends Base {
    private final ReferenceKeys productKeys;
    private final ReferenceKeys categoryKeys;
    private final ReferenceKeys productTypeKeys;

    @JsonCreator
    public CombinedReferenceKeys(
        final ReferenceKeys productKeys,
        final ReferenceKeys categoryKeys,
        final ReferenceKeys productTypeKeys) {

        this.productKeys = productKeys;
        this.categoryKeys = categoryKeys;
        this.productTypeKeys = productTypeKeys;
    }

    public ReferenceKeys getProductKeys() {
        return productKeys;
    }

    public ReferenceKeys getCategoryKeys() {
        return categoryKeys;
    }

    public ReferenceKeys getProductTypeKeys() {
        return productTypeKeys;
    }
}