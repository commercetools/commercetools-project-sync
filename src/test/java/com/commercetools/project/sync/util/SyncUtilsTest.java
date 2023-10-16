package com.commercetools.project.sync.util;

import static com.commercetools.project.sync.util.SyncUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.ResourceUpdateAction;
import com.commercetools.api.models.WithKey;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.types.TypeSync;
import java.util.Arrays;
import java.util.Optional;
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
  void logErrorCallbackWithStringResourceIdentifier_ShouldLogErrorWithCorrectMessage() {
    final TestLogger testLogger = TestLoggerFactory.getTestLogger(SyncUtilsTest.class);
    SyncException exception = new SyncException("test sync exception");
    ResourceUpdateAction updateAction1 = mock(ResourceUpdateAction.class);
    when(updateAction1.toString()).thenReturn("updateAction1");
    ResourceUpdateAction updateAction2 = mock(ResourceUpdateAction.class);
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
  void
      logErrorCallbackWithStringResourceIdentifierAndNullUpdateActions_ShouldLogErrorWithCorrectMessage() {
    final TestLogger testLogger = TestLoggerFactory.getTestLogger(SyncUtilsTest.class);
    SyncException exception = new SyncException("test sync exception");

    logErrorCallback(testLogger, "test resource", exception, "test identifier", null);

    assertThat(testLogger.getAllLoggingEvents()).hasSize(1);
    final LoggingEvent loggingEvent = testLogger.getAllLoggingEvents().get(0);
    assertThat(loggingEvent.getMessage())
        .isEqualTo(
            "Error when trying to sync test resource. Existing key: test identifier. Update actions: []");
    assertThat(loggingEvent.getThrowable().isPresent()).isTrue();
    assertThat(loggingEvent.getThrowable().get()).isInstanceOf(SyncException.class);
  }

  @Test
  void logErrorCallbackWithResource_ShouldLogErrorWithCorrectMessage() {
    final TestLogger testLogger = TestLoggerFactory.getTestLogger(SyncUtilsTest.class);
    final SyncException exception = new SyncException("test sync exception");
    final ResourceUpdateAction updateAction1 = mock(ResourceUpdateAction.class);
    when(updateAction1.toString()).thenReturn("updateAction1");
    final ResourceUpdateAction updateAction2 = mock(ResourceUpdateAction.class);
    when(updateAction2.toString()).thenReturn("updateAction2");
    final WithKey resource = mock(WithKey.class);
    when(resource.getKey()).thenReturn("test identifier");

    logErrorCallback(
        testLogger,
        "test resource",
        exception,
        Optional.of(resource),
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
  void logErrorCallbackWithResourceAndNullUpdateActions_ShouldLogErrorWithCorrectMessage() {
    final TestLogger testLogger = TestLoggerFactory.getTestLogger(SyncUtilsTest.class);
    final SyncException exception = new SyncException("test sync exception");
    final WithKey resource = mock(WithKey.class);
    when(resource.getKey()).thenReturn("test identifier");

    logErrorCallback(testLogger, "test resource", exception, Optional.of(resource), null);

    assertThat(testLogger.getAllLoggingEvents()).hasSize(1);
    final LoggingEvent loggingEvent = testLogger.getAllLoggingEvents().get(0);
    assertThat(loggingEvent.getMessage())
        .isEqualTo(
            "Error when trying to sync test resource. Existing key: test identifier. Update actions: []");
    assertThat(loggingEvent.getThrowable().isPresent()).isTrue();
    assertThat(loggingEvent.getThrowable().get()).isInstanceOf(SyncException.class);
  }

  @Test
  void logWarningCallbackStringResourceIdentifier_ShouldLogWarningWithCorrectMessage() {
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

  @Test
  void logWarningCallbackWithResource_ShouldLogWarningWithCorrectMessage() {
    final TestLogger testLogger = TestLoggerFactory.getTestLogger(SyncUtilsTest.class);
    SyncException exception = new SyncException("test sync exception");
    WithKey resource = mock(WithKey.class);
    when(resource.getKey()).thenReturn("test identifier");

    logWarningCallback(testLogger, "test resource", exception, Optional.of(resource));

    assertThat(testLogger.getAllLoggingEvents()).hasSize(1);
    final LoggingEvent loggingEvent = testLogger.getAllLoggingEvents().get(0);
    assertThat(loggingEvent.getMessage())
        .isEqualTo("Warning when trying to sync test resource. Existing key: test identifier");
    assertThat(loggingEvent.getThrowable().isPresent()).isTrue();
    assertThat(loggingEvent.getThrowable().get()).isInstanceOf(SyncException.class);
  }
}
