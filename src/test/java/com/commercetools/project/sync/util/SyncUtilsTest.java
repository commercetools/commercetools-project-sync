package com.commercetools.project.sync.util;

import static com.commercetools.project.sync.util.SyncUtils.APPLICATION_DEFAULT_NAME;
import static com.commercetools.project.sync.util.SyncUtils.APPLICATION_DEFAULT_VERSION;
import static com.commercetools.project.sync.util.SyncUtils.getApplicationName;
import static com.commercetools.project.sync.util.SyncUtils.getApplicationVersion;
import static com.commercetools.project.sync.util.SyncUtils.getSyncModuleName;
import static com.commercetools.project.sync.util.SyncUtils.logErrorCallback;
import static com.commercetools.project.sync.util.SyncUtils.logWarningCallback;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.types.TypeSync;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.ResourceView;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

class SyncUtilsTest {

  @BeforeEach
  void tearDownTest() {
    TestLoggerFactory.clearAll();
  }

  @Test
  void getSyncModuleName_WithProductSync_ShouldGetCorrectName() {

    final String syncModuleName = getSyncModuleName(ProductSync.class);

    assertThat(syncModuleName).isEqualTo("ProductSync");
  }

  @Test
  void getSyncModuleName_WithTypeSync_ShouldGetCorrectName() {

    final String syncModuleName = getSyncModuleName(TypeSync.class);

    assertThat(syncModuleName).isEqualTo("TypeSync");
  }

  @Test
  void getApplicationName_ShouldGetDefaultName() {

    final String applicationName = getApplicationName();

    assertThat(applicationName).isEqualTo(APPLICATION_DEFAULT_NAME);
  }

  @Test
  void getApplicationVersion_ShouldGetDefaultVersion() {

    final String applicationName = getApplicationVersion();

    assertThat(applicationName).isEqualTo(APPLICATION_DEFAULT_VERSION);
  }

  @Test
  void logErrorCallback_ShouldLogErrorWithCorrectMessage() {
    final TestLogger testLogger = TestLoggerFactory.getTestLogger(SyncUtilsTest.class);
    SyncException exception = new SyncException("test sync exception");
    UpdateAction<ResourceView> updateAction1 = mock(UpdateAction.class);
    when(updateAction1.toString()).thenReturn("updateAction1");
    UpdateAction<ResourceView> updateAction2 = mock(UpdateAction.class);
    when(updateAction2.toString()).thenReturn("updateAction2");

    logErrorCallback(
        testLogger,
        "test resource",
        exception,
        "test identifier",
        Arrays.asList(updateAction1, updateAction2));

    assertThat(testLogger.getAllLoggingEvents()).hasSize(1);
    final LoggingEvent loggingEvent = testLogger.getAllLoggingEvents().get(0);
    assertThat(loggingEvent.getMessage())
        .isEqualTo(
            "Error when trying to sync test resource. Existing key: test identifier. Update actions: updateAction1,updateAction2");
    assertThat(loggingEvent.getThrowable().isPresent()).isTrue();
    assertThat(loggingEvent.getThrowable().get()).isInstanceOf(SyncException.class);
  }

  @Test
  void logWarningCallback_ShouldLogWarningWithCorrectMessage() {
    final TestLogger testLogger = TestLoggerFactory.getTestLogger(SyncUtilsTest.class);
    SyncException exception = new SyncException("test sync exception");

    logWarningCallback(testLogger, "test resource", exception, "test identifier");

    assertThat(testLogger.getAllLoggingEvents()).hasSize(1);
    final LoggingEvent loggingEvent = testLogger.getAllLoggingEvents().get(0);
    assertThat(loggingEvent.getMessage())
        .isEqualTo("Warning when trying to sync test resource. Existing key: test identifier");
    assertThat(loggingEvent.getThrowable().isPresent()).isTrue();
    assertThat(loggingEvent.getThrowable().get()).isInstanceOf(SyncException.class);
  }
}
