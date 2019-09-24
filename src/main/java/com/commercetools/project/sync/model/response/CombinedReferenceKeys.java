package com.commercetools.project.sync.model.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.sphere.sdk.models.Base;

public class CombinedReferenceKeys extends Base {
  private final ReferenceKeys products;
  private final ReferenceKeys categories;
  private final ReferenceKeys productTypes;

  @JsonCreator
  public CombinedReferenceKeys(
      @JsonProperty("products") final ReferenceKeys products,
      @JsonProperty("categories") final ReferenceKeys categories,
      @JsonProperty("productTypes") final ReferenceKeys productTypes) {

    this.products = products;
    this.categories = categories;
    this.productTypes = productTypes;
  }

  public ReferenceKeys getProducts() {
    return products;
  }

  public ReferenceKeys getCategories() {
    return categories;
  }

  public ReferenceKeys getProductTypes() {
    return productTypes;
  }
}
