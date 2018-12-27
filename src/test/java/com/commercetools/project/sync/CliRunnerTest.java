package com.commercetools.project.sync;

import static com.commercetools.project.sync.CliRunner.APPLICATION_DEFAULT_NAME;
import static com.commercetools.project.sync.CliRunner.APPLICATION_DEFAULT_VERSION;
import static com.commercetools.project.sync.CliRunner.HELP_OPTION_DESCRIPTION;
import static com.commercetools.project.sync.CliRunner.HELP_OPTION_LONG;
import static com.commercetools.project.sync.CliRunner.HELP_OPTION_SHORT;
import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_DESCRIPTION;
import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_LONG;
import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_SHORT;
import static com.commercetools.project.sync.CliRunner.VERSION_OPTION_DESCRIPTION;
import static com.commercetools.project.sync.CliRunner.VERSION_OPTION_LONG;
import static com.commercetools.project.sync.CliRunner.VERSION_OPTION_SHORT;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.CompletableFuture;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

public class CliRunnerTest {
  private static final TestLogger testLogger = TestLoggerFactory.getTestLogger(CliRunner.class);
  private static ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
  private static PrintStream originalSystemOut;

  @BeforeClass
  public static void setupSuite() throws UnsupportedEncodingException {
    final PrintStream printStream = new PrintStream(outputStream, false, "UTF-8");
    originalSystemOut = System.out;
    System.setOut(printStream);
  }

  @AfterClass
  public static void tearDownSuite() {
    System.setOut(originalSystemOut);
  }

  @After
  public void tearDownTest() {
    testLogger.clearAll();
  }

  @Test
  public void run_WithEmptyArgumentList_ShouldLogErrorAndPrintHelp()
      throws UnsupportedEncodingException {
    // preparation
    final SyncerFactory syncerFactory =
        SyncerFactory.of(mock(SphereClient.class), mock(SphereClient.class));

    // test
    CliRunner.of().run(new String[] {}, () -> syncerFactory);

    // Assert error log
    assertThat(outputStream.toString("UTF-8"))
        .contains("Please pass at least 1 option to the CLI.");
    assertOutputStreamContainsHelpUsageWithSpecifiedCliOptions();
  }

  private void assertOutputStreamContainsHelpUsageWithSpecifiedCliOptions()
      throws UnsupportedEncodingException {
    assertThat(outputStream.toString("UTF-8"))
        .contains(format("usage: %s", APPLICATION_DEFAULT_NAME))
        .contains(format("-%s,--%s", HELP_OPTION_SHORT, HELP_OPTION_LONG))
        .contains(format("-%s,--%s", SYNC_MODULE_OPTION_SHORT, SYNC_MODULE_OPTION_LONG))
        .contains(format("-%s,--%s", VERSION_OPTION_SHORT, VERSION_OPTION_LONG));
  }

  @Test
  public void run_WithHelpAsLongArgument_ShouldPrintUsageHelpToSystemOut()
      throws UnsupportedEncodingException {
    // preparation
    final SyncerFactory syncerFactory =
        SyncerFactory.of(mock(SphereClient.class), mock(SphereClient.class));

    // test
    CliRunner.of().run(new String[] {"-help"}, () -> syncerFactory);

    assertThat(testLogger.getAllLoggingEvents()).isEmpty();
    assertOutputStreamContainsHelpUsageWithSpecifiedCliOptions();
  }

  @Test
  public void run_WithHelpAsShortArgument_ShouldPrintUsageHelpToSystemOut()
      throws UnsupportedEncodingException {
    // preparation
    final SyncerFactory syncerFactory =
        SyncerFactory.of(mock(SphereClient.class), mock(SphereClient.class));

    // test
    CliRunner.of().run(new String[] {"-h"}, () -> syncerFactory);

    assertThat(testLogger.getAllLoggingEvents()).isEmpty();
    assertOutputStreamContainsHelpUsageWithSpecifiedCliOptions();
  }

  @Test
  public void run_WithVersionAsShortArgument_ShouldLogApplicationVersionAsInfo()
      throws UnsupportedEncodingException {
    // preparation
    final SyncerFactory syncerFactory =
        SyncerFactory.of(mock(SphereClient.class), mock(SphereClient.class));

    // test
    CliRunner.of().run(new String[] {"-v"}, () -> syncerFactory);

    assertThat(outputStream.toString("UTF-8")).contains(APPLICATION_DEFAULT_VERSION);
  }

  @Test
  public void run_WithVersionAsLongArgument_ShouldLogApplicationVersionAsInfo()
      throws UnsupportedEncodingException {
    // preparation
    final SyncerFactory syncerFactory =
        SyncerFactory.of(mock(SphereClient.class), mock(SphereClient.class));

    // test
    CliRunner.of().run(new String[] {"--version"}, () -> syncerFactory);

    assertThat(outputStream.toString("UTF-8")).contains(APPLICATION_DEFAULT_VERSION);
  }

  @Test
  public void run_WithSyncAsArgumentWithNoArgs_ShouldLogErrorAndPrintHelpUsageToSystemOut()
      throws UnsupportedEncodingException {
    // preparation
    final SyncerFactory syncerFactory =
        SyncerFactory.of(mock(SphereClient.class), mock(SphereClient.class));

    // test
    CliRunner.of().run(new String[] {"-s"}, () -> syncerFactory);

    assertThat(outputStream.toString("UTF-8"))
        .contains("Parse error:\nMissing argument for option: s");
    assertOutputStreamContainsHelpUsageWithSpecifiedCliOptions();
  }

  @Test
  public void run_WithSyncAsArgumentWithProductsArg_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    when(sourceClient.execute(any(ProductQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));

    final SyncerFactory syncerFactory = spy(SyncerFactory.of(sourceClient, targetClient));

    // test
    CliRunner.of().run(new String[] {"-s", "products"}, () -> syncerFactory);

    // assertions
    verify(syncerFactory, times(1)).buildSyncer("products");
    verify(sourceClient, times(1)).execute(any(ProductQuery.class));
  }

  @Test
  public void run_WithSyncAsArgumentWithIllegalArgs_ShouldLogErrorAndPrintHelpUsageToSystemOut()
      throws UnsupportedEncodingException {
    // preparation
    final SyncerFactory syncerFactory =
        spy(SyncerFactory.of(mock(SphereClient.class), mock(SphereClient.class)));
    // test
    final String illegalArg = "illegal";
    CliRunner.of().run(new String[] {"-s", illegalArg}, () -> syncerFactory);
    // Assert error log
    assertThat(outputStream.toString("UTF-8"))
        .contains(
            format(
                "Parse error:%nUnknown argument \"%s\" supplied to \"-%s\" or"
                    + " \"--%s\" option!",
                illegalArg, SYNC_MODULE_OPTION_SHORT, SYNC_MODULE_OPTION_LONG));
    assertOutputStreamContainsHelpUsageWithSpecifiedCliOptions();
    verify(syncerFactory, times(1)).buildSyncer(illegalArg);
  }

  @Test
  public void run_WithSyncAsLongArgument_ShouldProcessSyncOption() {
    // preparation
    final SyncerFactory syncerFactory =
        spy(SyncerFactory.of(mock(SphereClient.class), mock(SphereClient.class)));
    // test
    CliRunner.of().run(new String[] {"-sync", "arg"}, () -> syncerFactory);
    // assertions
    verify(syncerFactory, times(1)).buildSyncer("arg");
  }

  @Test
  public void run_WithSyncAsShortArgument_ShouldProcessSyncOption() {
    // preparation
    final SyncerFactory syncerFactory =
        spy(SyncerFactory.of(mock(SphereClient.class), mock(SphereClient.class)));
    // test
    CliRunner.of().run(new String[] {"-s", "arg"}, () -> syncerFactory);
    // assertions
    verify(syncerFactory, times(1)).buildSyncer("arg");
  }

  @Test
  public void run_WithUnknownArgument_ShouldPrintErrorLogAndHelpUsage()
      throws UnsupportedEncodingException {
    // preparation
    final SyncerFactory syncerFactory =
        spy(SyncerFactory.of(mock(SphereClient.class), mock(SphereClient.class)));
    // test
    CliRunner.of().run(new String[] {"-u"}, () -> syncerFactory);
    // Assert error log
    assertThat(outputStream.toString("UTF-8")).contains("Parse error:\nUnrecognized option: -u");
    assertOutputStreamContainsHelpUsageWithSpecifiedCliOptions();
    verify(syncerFactory, never()).buildSyncer(any());
  }

  @Test
  public void run_WithHelpAsArgument_ShouldPrintThreeOptionsWithDescriptionsToSystemOut()
      throws UnsupportedEncodingException {
    // preparation
    final SyncerFactory syncerFactory =
        spy(SyncerFactory.of(mock(SphereClient.class), mock(SphereClient.class)));

    // test
    CliRunner.of().run(new String[] {"-h"}, () -> syncerFactory);

    // assertions
    assertThat(testLogger.getAllLoggingEvents()).isEmpty();

    // Remove line breaks from output stream string.
    final String outputStreamWithoutLineBreaks = outputStream.toString("UTF-8").replace("\n", "");

    // Replace multiple spaces with single space in output stream string.
    final String outputStreamWithSingleSpaces =
        outputStreamWithoutLineBreaks.trim().replaceAll(" +", " ");

    assertThat(outputStreamWithSingleSpaces)
        .contains(
            format("-%s,--%s %s", HELP_OPTION_SHORT, HELP_OPTION_LONG, HELP_OPTION_DESCRIPTION))
        .contains(
            format(
                "-%s,--%s <arg> %s",
                SYNC_MODULE_OPTION_SHORT, SYNC_MODULE_OPTION_LONG, SYNC_MODULE_OPTION_DESCRIPTION))
        .contains(
            format(
                "-%s,--%s %s",
                VERSION_OPTION_SHORT, VERSION_OPTION_LONG, VERSION_OPTION_DESCRIPTION));
    verify(syncerFactory, never()).buildSyncer(any());
  }
}
