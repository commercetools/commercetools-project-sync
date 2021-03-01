package com.commercetools.project.sync.model;

import static java.lang.String.format;

import com.commercetools.project.sync.exception.CliException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

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

  public static ProductSyncCustomRequest parseProductQueryParametersOption(String customRequest) {

    final ObjectMapper objectMapper = new ObjectMapper();

    final ProductSyncCustomRequest productSyncCustomRequest;
    try {
      productSyncCustomRequest =
          objectMapper.readValue(customRequest, ProductSyncCustomRequest.class);
    } catch (IOException | IllegalArgumentException e) {
      throw new CliException(e.getMessage());
    }

    return productSyncCustomRequest;
  }
}
