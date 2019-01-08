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
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.types.queries.TypeQuery;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

class CliRunnerTest {
  private static final TestLogger testLogger = TestLoggerFactory.getTestLogger(CliRunner.class);
  private static ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
  private static PrintStream originalSystemOut;

  @BeforeAll
  static void setupSuite() throws UnsupportedEncodingException {
    final PrintStream printStream = new PrintStream(outputStream, false, "UTF-8");
    originalSystemOut = System.out;
    System.setOut(printStream);
  }

  @AfterAll
  static void tearDownSuite() {
    System.setOut(originalSystemOut);
  }

  @AfterEach
  void tearDownTest() {
    testLogger.clearAll();
  }

  @Test
  void run_WithEmptyArgumentList_ShouldLogErrorAndPrintHelp() throws UnsupportedEncodingException {
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
  void run_WithHelpAsLongArgument_ShouldPrintUsageHelpToStandardOut()
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
  void run_WithHelpAsShortArgument_ShouldPrintUsageHelpToStandardOut()
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
  void run_WithVersionAsShortArgument_ShouldPrintApplicationVersionToStandardOut()
      throws UnsupportedEncodingException {
    // preparation
    final SyncerFactory syncerFactory =
        SyncerFactory.of(mock(SphereClient.class), mock(SphereClient.class));

    // test
    CliRunner.of().run(new String[] {"-v"}, () -> syncerFactory);

    assertThat(outputStream.toString("UTF-8")).contains(APPLICATION_DEFAULT_VERSION);
  }

  @Test
  void run_WithVersionAsLongArgument_ShouldPrintApplicationVersionToStandardOut()
      throws UnsupportedEncodingException {
    // preparation
    final SyncerFactory syncerFactory =
        SyncerFactory.of(mock(SphereClient.class), mock(SphereClient.class));

    // test
    CliRunner.of()
        .run(new String[] {"--version"}, () -> syncerFactory)
        .toCompletableFuture()
        .join();

    assertThat(outputStream.toString("UTF-8")).contains(APPLICATION_DEFAULT_VERSION);
  }

  @Test
  void run_WithSyncAsArgumentWithNoArgs_ShouldLogErrorAndPrintHelpUsageToSystemOut()
      throws UnsupportedEncodingException {
    // preparation
    final SyncerFactory syncerFactory =
        SyncerFactory.of(mock(SphereClient.class), mock(SphereClient.class));

    // test
    CliRunner.of().run(new String[] {"-s"}, () -> syncerFactory).toCompletableFuture().join();

    assertThat(outputStream.toString("UTF-8"))
        .contains("Error:\nMissing argument for option: s");
    assertOutputStreamContainsHelpUsageWithSpecifiedCliOptions();
  }

  @Test
  void run_WithSyncAsArgumentWithProductsArg_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    when(sourceClient.execute(any(ProductQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));

    final SyncerFactory syncerFactory = spy(SyncerFactory.of(sourceClient, targetClient));

    // test
    CliRunner.of()
        .run(new String[] {"-s", "products"}, () -> syncerFactory)
        .toCompletableFuture()
        .join();

    // assertions
    verify(syncerFactory, times(1)).sync("products");
    verify(sourceClient, times(1)).execute(any(ProductQuery.class));
    verify(syncerFactory, never()).syncAll();
  }

  @Test
  void run_WithSyncAsArgumentWithIllegalArgs_ShouldLogErrorAndPrintHelpUsageToSystemOut()
      throws UnsupportedEncodingException {
    // preparation
    final SyncerFactory syncerFactory =
        spy(SyncerFactory.of(mock(SphereClient.class), mock(SphereClient.class)));
    // test
    final String illegalArg = "illegal";
    CliRunner.of()
        .run(new String[] {"-s", illegalArg}, () -> syncerFactory)
        .toCompletableFuture()
        .join();
    // Assert error log
    assertThat(outputStream.toString("UTF-8"))
        .contains(
            format(
                "Error:%nUnknown argument \"%s\" supplied to \"-%s\" or"
                    + " \"--%s\" option!",
                illegalArg, SYNC_MODULE_OPTION_SHORT, SYNC_MODULE_OPTION_LONG));
    assertOutputStreamContainsHelpUsageWithSpecifiedCliOptions();
    verify(syncerFactory, times(1)).sync(illegalArg);
    verify(syncerFactory, never()).syncAll();
  }

  @Test
  void run_WithSyncAsLongArgument_ShouldProcessSyncOption() {
    // preparation
    final SyncerFactory syncerFactory =
        spy(SyncerFactory.of(mock(SphereClient.class), mock(SphereClient.class)));
    // test
    CliRunner.of()
        .run(new String[] {"-sync", "arg"}, () -> syncerFactory)
        .toCompletableFuture()
        .join();
    // assertions
    verify(syncerFactory, times(1)).sync("arg");
    verify(syncerFactory, never()).syncAll();
  }

  @Test
  void run_WithSyncAsShortArgument_ShouldProcessSyncOption() {
    // preparation
    final SyncerFactory syncerFactory =
        spy(SyncerFactory.of(mock(SphereClient.class), mock(SphereClient.class)));
    // test
    CliRunner.of()
        .run(new String[] {"-s", "arg"}, () -> syncerFactory)
        .toCompletableFuture()
        .join();
    // assertions
    verify(syncerFactory, times(1)).sync("arg");
    verify(syncerFactory, never()).syncAll();
  }

  @Test
  void run_WithUnknownArgument_ShouldPrintErrorLogAndHelpUsage()
      throws UnsupportedEncodingException {
    // preparation
    final SyncerFactory syncerFactory =
        spy(SyncerFactory.of(mock(SphereClient.class), mock(SphereClient.class)));
    // test
    CliRunner.of().run(new String[] {"-u"}, () -> syncerFactory).toCompletableFuture().join();
    // Assert error log
    assertThat(outputStream.toString("UTF-8")).contains("Error:\nUnrecognized option: -u");
    assertOutputStreamContainsHelpUsageWithSpecifiedCliOptions();
    verify(syncerFactory, never()).sync(any());
    verify(syncerFactory, never()).syncAll();
  }

  @Test
  void run_WithHelpAsArgument_ShouldPrintThreeOptionsWithDescriptionsToSystemOut()
      throws UnsupportedEncodingException {
    // preparation
    final SyncerFactory syncerFactory =
        spy(SyncerFactory.of(mock(SphereClient.class), mock(SphereClient.class)));

    // test
    CliRunner.of().run(new String[] {"-h"}, () -> syncerFactory).toCompletableFuture().join();

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
    verify(syncerFactory, never()).sync(any());
  }

  @Test
  void run_WithSyncAsArgumentWithAllArg_ShouldExecuteAllSyncers()
      throws UnsupportedEncodingException {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    when(sourceClient.execute(any(ProductTypeQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));
    when(sourceClient.execute(any(TypeQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));
    when(sourceClient.execute(any(CategoryQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));
    when(sourceClient.execute(any(InventoryEntryQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));
    when(sourceClient.execute(any(ProductQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));

    final SyncerFactory syncerFactory = spy(SyncerFactory.of(sourceClient, targetClient));

    // test
    CliRunner.of()
        .run(new String[] {"-s", "all"}, () -> syncerFactory)
        .toCompletableFuture()
        .join();

    // assertions
    verify(syncerFactory, times(1)).syncAll();
    verify(sourceClient, times(1)).execute(any(ProductTypeQuery.class));
    verify(sourceClient, times(1)).execute(any(TypeQuery.class));
    verify(sourceClient, times(1)).execute(any(CategoryQuery.class));
    verify(sourceClient, times(1)).execute(any(ProductQuery.class));
    verify(sourceClient, times(1)).execute(any(InventoryEntryQuery.class));

    verifyInteractionsWithClientAfterSync(sourceClient, 5);
    assertThat(outputStream.toString("UTF-8"))
        .contains(
            "Syncing ProductTypes from CTP project with key 'foo' to project with key 'bar' is done.",
            "Syncing Types from CTP project with key 'foo' to project with key 'bar' is done.",
            "Syncing Categories from CTP project with key 'foo' to project with key 'bar' is done.",
            "Syncing Products from CTP project with key 'foo' to project with key 'bar' is done.",
            "Syncing Inventories from CTP project with key 'foo' to project with key 'bar' is done.");
  }

  @Test
  void run_WithSyncAsArgumentWithAllArg_ShouldExecuteAllSyncersInCorrectOrder()
      throws UnsupportedEncodingException {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    when(sourceClient.execute(any(ProductTypeQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));
    when(sourceClient.execute(any(TypeQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));
    when(sourceClient.execute(any(CategoryQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));
    when(sourceClient.execute(any(InventoryEntryQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));
    when(sourceClient.execute(any(ProductQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));

    final SyncerFactory syncerFactory = spy(SyncerFactory.of(sourceClient, targetClient));

    // test
    CliRunner.of()
        .run(new String[] {"-s", "all"}, () -> syncerFactory)
        .toCompletableFuture()
        .join();

    // assertions
    verify(syncerFactory, times(1)).syncAll();

    final InOrder inOrder = Mockito.inOrder(sourceClient);

    inOrder.verify(sourceClient).execute(any(ProductTypeQuery.class));
    inOrder.verify(sourceClient).execute(any(TypeQuery.class));
    inOrder.verify(sourceClient).execute(any(CategoryQuery.class));
    inOrder.verify(sourceClient).execute(any(ProductQuery.class));
    inOrder.verify(sourceClient).execute(any(InventoryEntryQuery.class));

    final String outputStringWithoutLineBreaks = outputStream.toString("UTF-8").replace("\n", "");
    verifyInteractionsWithClientAfterSync(sourceClient, 5);
    assertThat(outputStringWithoutLineBreaks)
        .containsSequence(
            "Syncing ProductTypes from CTP project with key 'foo' to project with key 'bar' is done.",
            "Syncing Types from CTP project with key 'foo' to project with key 'bar' is done.",
            "Syncing Categories from CTP project with key 'foo' to project with key 'bar' is done.",
            "Syncing Products from CTP project with key 'foo' to project with key 'bar' is done.",
            "Syncing Inventories from CTP project with key 'foo' to project with key 'bar' is done.");
  }

  private void verifyInteractionsWithClientAfterSync(
      @Nonnull final SphereClient client, final int expectedNumberOfGetConfigCalls) {

    // Verify config is accessed for the success message after sync:
    // " example: Syncing products from CTP project with key 'x' to project with key 'y' is done","
    verify(client, times(expectedNumberOfGetConfigCalls)).getConfig();
    verify(client, times(1)).close();
    verifyNoMoreInteractions(client);
  }
}
