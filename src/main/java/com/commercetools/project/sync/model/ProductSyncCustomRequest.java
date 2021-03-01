package com.commercetools.project.sync.model;

import static java.lang.String.format;

import com.commercetools.project.sync.exception.CliException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

public final class ProductSyncCustomRequest {

  private Long limit;
  private String where;

  public Long getLimit() {
    return limit;
  }

  public String getWhere() {
    return where;
  }

  public void setLimit(Long limit) {
    if (limit > 0) {
      this.limit = limit;
    } else {
      throw new IllegalArgumentException(format("limit %s cannot be less than 1.", limit));
    }
  }

  public void setWhere(String where) {
    this.where = where;
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
