package com.commercetools.project.sync.model.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CombinedResult {
  private final ResultingResourcesContainer products;
  private final ResultingResourcesContainer categories;
  private final ResultingResourcesContainer productTypes;

  @JsonCreator
  public CombinedResult(
      @JsonProperty("products") final ResultingResourcesContainer products,
      @JsonProperty("categories") final ResultingResourcesContainer categories,
      @JsonProperty("productTypes") final ResultingResourcesContainer productTypes) {

    this.products = products;
    this.categories = categories;
    this.productTypes = productTypes;
  }

  public ResultingResourcesContainer getProducts() {
    return products;
  }

  public ResultingResourcesContainer getCategories() {
    return categories;
  }

  public ResultingResourcesContainer getProductTypes() {
    return productTypes;
  }
}
