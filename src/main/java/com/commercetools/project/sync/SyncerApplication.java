package com.commercetools.project.sync;

import static com.commercetools.project.sync.util.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.project.sync.util.SphereClientUtils.CTP_TARGET_CLIENT;

public class SyncerApplication {

  /**
   * Application entry point.
   *
   * @param args all args
   */
  public static void main(final String[] args) {
    CliRunner.of().run(args, () -> SyncerFactory.of(CTP_SOURCE_CLIENT, CTP_TARGET_CLIENT));
  }
}
