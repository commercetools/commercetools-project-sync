package com.commercetools.project.sync.model;

import static java.lang.String.format;

public final class ProductSyncCustomRequest {

  private Long fetchSize;
  private String customQuery;

  public Long getFetchSize() {
    return fetchSize;
  }

  public String getCustomQuery() {
    return customQuery;
  }

  public void setFetchSize(Long fetchSize) {
    if (fetchSize > 0) {
      this.fetchSize = fetchSize;
    } else {
      throw new IllegalArgumentException(format("fetchSize %s cannot be less than 1.", fetchSize));
    }
  }

  public void setCustomQuery(String customQuery) {
    this.customQuery = customQuery;
  }
}
