package com.commercetools.project.sync.model.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import javax.annotation.Nullable;

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

  @Override
  public boolean equals(@Nullable final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof CombinedResult)) {
      return false;
    }
    final CombinedResult that = (CombinedResult) other;
    return getProducts().equals(that.getProducts())
        && getCategories().equals(that.getCategories())
        && getProductTypes().equals(that.getProductTypes());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getProducts(), getCategories(), getProductTypes());
  }
}
