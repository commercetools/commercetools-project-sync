package com.commercetools.project.sync.service.impl;

import io.sphere.sdk.client.SphereClient;
import javax.annotation.Nonnull;

class BaseServiceImpl {
  private final SphereClient ctpClient;

  BaseServiceImpl(@Nonnull final SphereClient ctpClient) {
    this.ctpClient = ctpClient;
  }

  SphereClient getCtpClient() {
    return ctpClient;
  }
}
