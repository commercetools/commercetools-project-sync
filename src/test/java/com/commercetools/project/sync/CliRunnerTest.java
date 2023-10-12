package com.commercetools.project.sync;

import static com.commercetools.project.sync.CliRunner.HELP_OPTION_DESCRIPTION;
import static com.commercetools.project.sync.CliRunner.HELP_OPTION_LONG;
import static com.commercetools.project.sync.CliRunner.HELP_OPTION_SHORT;
import static com.commercetools.project.sync.CliRunner.RUNNER_NAME_OPTION_DESCRIPTION;
import static com.commercetools.project.sync.CliRunner.RUNNER_NAME_OPTION_LONG;
import static com.commercetools.project.sync.CliRunner.RUNNER_NAME_OPTION_SHORT;
import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_DESCRIPTION;
import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_LONG;
import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_SHORT;
import static com.commercetools.project.sync.CliRunner.VERSION_OPTION_DESCRIPTION;
import static com.commercetools.project.sync.CliRunner.VERSION_OPTION_LONG;
import static com.commercetools.project.sync.CliRunner.VERSION_OPTION_SHORT;
import static com.commercetools.project.sync.util.SyncUtils.APPLICATION_DEFAULT_NAME;
import static com.commercetools.project.sync.util.SyncUtils.APPLICATION_DEFAULT_VERSION;
import static com.commercetools.project.sync.util.TestUtils.getMockedClock;
import static com.commercetools.project.sync.util.TestUtils.stubClientsCustomObjectService;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.project.sync.exception.CliException;
import com.google.common.base.Optional;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

class CliRunnerTest {
  private static final TestLogger testLogger = TestLoggerFactory.getTestLogger(CliRunner.class);
  private static ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
  private static PrintStream originalSystemOut;
  private ProjectApiRoot sourceClient;
  private ProjectApiRoot targetClient;
  private ApiHttpResponse apiHttpResponse;

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

  @BeforeEach
  void setupTest() {
    sourceClient = mock(ProjectApiRoot.class);
    targetClient = mock(ProjectApiRoot.class);
    apiHttpResponse = mock(ApiHttpResponse.class);
  }

  @AfterEach
  void tearDownTest() {
    testLogger.clearAll();
    reset(sourceClient, targetClient, apiHttpResponse);
  }

  @Test
  void run_WithEmptyArgumentList_ShouldFailAndLogError() {
    // preparation
    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    // test
    CliRunner.of().run(new String[] {}, syncerFactory);

    // assertion
    assertThat(testLogger.getAllLoggingEvents())
        .hasSize(1)
        .singleElement()
        .satisfies(
            loggingEvent -> {
              assertThat(loggingEvent.getLevel()).isEqualTo(Level.ERROR);
              assertThat(loggingEvent.getMessage()).contains("Failed to run sync process.");
              final Optional<Throwable> actualThrowableOpt = loggingEvent.getThrowable();
              assertThat(actualThrowableOpt).isNotNull();
              assertThat(actualThrowableOpt.isPresent()).isTrue();
              final Throwable actualThrowable = actualThrowableOpt.get();
              assertThat(actualThrowable).isExactlyInstanceOf(CliException.class);
              assertThat(actualThrowable.getMessage())
                  .contains("Please pass at least 1 option to the CLI.");
            });
  }

  @Test
  void run_WithHelpAsLongArgument_ShouldPrintUsageHelpToStandardOut()
      throws UnsupportedEncodingException {
    // preparation
    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    // test
    CliRunner.of().run(new String[] {"-help"}, syncerFactory);

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
  void run_WithHelpAsShortArgument_ShouldPrintUsageHelpToStandardOut()
      throws UnsupportedEncodingException {
    // preparation
    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    // test
    CliRunner.of().run(new String[] {"-h"}, syncerFactory);

    assertOutputStreamContainsHelpUsageWithSpecifiedCliOptions();
  }

  @Test
  void run_WithVersionAsShortArgument_ShouldPrintApplicationVersionToStandardOut()
      throws UnsupportedEncodingException {
    // preparation
    final SyncerFactory syncerFactory =
        SyncerFactory.of(
            () -> mock(ProjectApiRoot.class), () -> mock(ProjectApiRoot.class), getMockedClock());

    // test
    CliRunner.of().run(new String[] {"-v"}, syncerFactory);

    assertThat(outputStream.toString("UTF-8")).contains(APPLICATION_DEFAULT_VERSION);
  }

  @Test
  void run_WithVersionAsLongArgument_ShouldPrintApplicationVersionToStandardOut()
      throws UnsupportedEncodingException {
    // preparation
    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    // test
    CliRunner.of().run(new String[] {"--version"}, syncerFactory);

    assertThat(outputStream.toString("UTF-8")).contains(APPLICATION_DEFAULT_VERSION);
  }

  @Test
  void run_WithSyncAsArgumentWithNoArgs_ShouldFailAndLogError() {
    // preparation
    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    // test
    CliRunner.of().run(new String[] {"-s"}, syncerFactory);

    // assertion
    assertThat(testLogger.getAllLoggingEvents())
        .hasSize(1)
        .singleElement()
        .satisfies(
            loggingEvent -> {
              assertThat(loggingEvent.getLevel()).isEqualTo(Level.ERROR);
              assertThat(loggingEvent.getMessage()).contains("Failed to run sync process.");
              final Optional<Throwable> actualThrowableOpt = loggingEvent.getThrowable();
              assertThat(actualThrowableOpt).isNotNull();
              assertThat(actualThrowableOpt.isPresent()).isTrue();
              final Throwable actualThrowable = actualThrowableOpt.get();
              assertThat(actualThrowable).isExactlyInstanceOf(CliException.class);
              assertThat(actualThrowable.getMessage()).contains("Missing argument for option: s");
            });
  }

  @Test
  void run_WithFullSyncAsFirstArgument_ShouldFailAndLogError() {
    // preparation
    final SyncerFactory syncerFactory =
        SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock());

    // test
    CliRunner.of().run(new String[] {"-f"}, syncerFactory);

    // assertion
    assertThat(testLogger.getAllLoggingEvents())
        .hasSize(1)
        .singleElement()
        .satisfies(
            loggingEvent -> {
              assertThat(loggingEvent.getLevel()).isEqualTo(Level.ERROR);
              assertThat(loggingEvent.getMessage()).contains("Failed to run sync process.");
              final Optional<Throwable> actualThrowableOpt = loggingEvent.getThrowable();
              assertThat(actualThrowableOpt).isNotNull();
              assertThat(actualThrowableOpt.isPresent()).isTrue();
              final Throwable actualThrowable = actualThrowableOpt.get();
              assertThat(actualThrowable).isExactlyInstanceOf(CliException.class);
              assertThat(actualThrowable.getMessage())
                  .contains("Please check that the first sync option is either -s, -h or -v.");
            });
  }

  @Test
  void run_AsProductDeltaSync_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    when(apiHttpResponse.getBody()).thenReturn(emptyList());
    when(sourceClient.productProjections().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

    final SyncerFactory syncerFactory =
        spy(SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock()));

    stubClientsCustomObjectService(targetClient, ZonedDateTime.now());

    // test
    CliRunner.of().run(new String[] {"-s", "products"}, syncerFactory);

    // assertions
    verify(syncerFactory, times(1)).sync(new String[] {"products"}, null, false, false, null);
    verify(sourceClient, times(1)).productProjections().get().execute();
  }

  @Test
  void run_AsProductFullSync_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    when(apiHttpResponse.getBody()).thenReturn(emptyList());
    when(sourceClient.productProjections().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

    final SyncerFactory syncerFactory =
        spy(SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock()));

    stubClientsCustomObjectService(targetClient, ZonedDateTime.now());

    // test
    CliRunner.of().run(new String[] {"-s", "products", "-f"}, syncerFactory);

    // assertions
    verify(syncerFactory, times(1)).sync(new String[] {"products"}, null, true, false, null);
    verify(sourceClient, times(1)).productProjections().get().execute();
  }

  @Test
  void
      run_AsProductSyncWithCustomProductQueriesAndLimit_ShouldBuildSyncerAndExecuteQuerySuccessfully() {
    // preparation
    when(apiHttpResponse.getBody()).thenReturn(emptyList());
    when(sourceClient.productProjections().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

    final Long limit = 100L;
    final String customQuery =
        "\"published=true AND masterData(masterVariant(attributes(name= \\\"abc\\\" AND value=123)))\"";
    final String productQueryParametersValue =
        "{\"limit\": " + limit + ", \"where\": " + customQuery + "}";

    final SyncerFactory syncerFactory =
        spy(SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock()));

    stubClientsCustomObjectService(targetClient, ZonedDateTime.now());
    // test
    CliRunner.of()
        .run(
            new String[] {
              "-s", "products", "-f", "-productQueryParameters", productQueryParametersValue
            },
            syncerFactory);

    // assertions
    verify(sourceClient, times(1)).productProjections().get().execute();
  }

  @Test
  void
      run_AsProductSyncWithProductQueryParametersAndOnlyLimit_ShouldBuildSyncerAndExecuteQuerySuccessfully() {
    // preparation
    when(apiHttpResponse.getBody()).thenReturn(emptyList());
    when(sourceClient.productProjections().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

    final Long limit = 100L;
    final String productQueryParametersValue = "{\"limit\": " + limit + "}";

    final SyncerFactory syncerFactory =
        spy(SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock()));

    stubClientsCustomObjectService(targetClient, ZonedDateTime.now());
    // test
    CliRunner.of()
        .run(
            new String[] {
              "-s", "products", "-f", "-productQueryParameters", productQueryParametersValue
            },
            syncerFactory);

    // assertions
    verify(sourceClient, times(1)).productProjections().get().execute();
  }

  @Test
  void
      run_AsProductSyncWithProductQueryParametersAndOnlyWhere_ShouldBuildSyncerAndExecuteQuerySuccessfully() {
    // preparation
    when(apiHttpResponse.getBody()).thenReturn(emptyList());
    when(sourceClient.productProjections().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

    final String customQuery =
        "\"published=true AND masterVariant(attributes(name= \\\"abc\\\" AND value=123))\"";
    final String productQueryParametersValue = "{\"where\": " + customQuery + "}";

    final SyncerFactory syncerFactory =
        spy(SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock()));

    stubClientsCustomObjectService(targetClient, ZonedDateTime.now());
    // test
    CliRunner.of()
        .run(
            new String[] {
              "-s", "products", "-f", "-productQueryParameters", productQueryParametersValue
            },
            syncerFactory);

    // assertions
    verify(sourceClient, times(1)).productProjections().get().execute();
  }

  @Test
  void run_WithWrongFormatProductQueryParametersArgument_ShouldThrowCLIException() {
    // preparation
    final Long limit = 100L;
    final String customQuery =
        "\"published=true AND masterVariant(attributes(name= \"abc\\\" AND value=123))\"";
    final String productQueryParametersValue =
        "{\"limit\": " + limit + ", \"where\": " + customQuery + "}";

    final SyncerFactory syncerFactory =
        spy(SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock()));

    // test
    CliRunner.of()
        .run(
            new String[] {
              "-s", "products", "-f", "-productQueryParameters", productQueryParametersValue
            },
            syncerFactory);

    // assertion
    assertThat(testLogger.getAllLoggingEvents())
        .hasSize(1)
        .singleElement()
        .satisfies(
            loggingEvent -> {
              assertThat(loggingEvent.getLevel()).isEqualTo(Level.ERROR);
              assertThat(loggingEvent.getMessage()).contains("Failed to run sync process.");
              final Optional<Throwable> actualThrowableOpt = loggingEvent.getThrowable();
              assertThat(actualThrowableOpt).isNotNull();
              assertThat(actualThrowableOpt.isPresent()).isTrue();
              final Throwable actualThrowable = actualThrowableOpt.get();
              assertThat(actualThrowable).isExactlyInstanceOf(CliException.class);
            });
  }

  @Test
  void run_WithInvalidLimitInProductQueryParametersArgument_ShouldThrowCLIException() {
    // preparation
    final Long limit = -100L;
    final String customQuery =
        "\"published=true AND masterVariant(attributes(name= \"abc\\\" AND value=123))\"";
    final String productQueryParametersValue =
        "{\"limit\": " + limit + ", \"where\": " + customQuery + "}";

    final SyncerFactory syncerFactory =
        spy(SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock()));

    // test
    CliRunner.of()
        .run(
            new String[] {
              "-s", "products", "-f", "-productQueryParameters", productQueryParametersValue
            },
            syncerFactory);

    // assertion
    assertThat(testLogger.getAllLoggingEvents())
        .hasSize(1)
        .singleElement()
        .satisfies(
            loggingEvent -> {
              assertThat(loggingEvent.getLevel()).isEqualTo(Level.ERROR);
              assertThat(loggingEvent.getMessage()).contains("Failed to run sync process.");
              final Optional<Throwable> actualThrowableOpt = loggingEvent.getThrowable();
              assertThat(actualThrowableOpt).isNotNull();
              assertThat(actualThrowableOpt.isPresent()).isTrue();
              final Throwable actualThrowable = actualThrowableOpt.get();
              assertThat(actualThrowable).isExactlyInstanceOf(CliException.class);
              assertThat(actualThrowable.getMessage())
                  .contains("limit -100 cannot be less than 1.");
            });
  }

  @Test
  void run_AsTaxCategoryDeltaSync_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    when(apiHttpResponse.getBody()).thenReturn(emptyList());
    when(sourceClient.productProjections().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

    final SyncerFactory syncerFactory =
        spy(SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock()));

    stubClientsCustomObjectService(targetClient, ZonedDateTime.now());

    // test
    CliRunner.of().run(new String[] {"-s", "taxCategories"}, syncerFactory);

    // assertions
    verify(syncerFactory, times(1)).sync(new String[] {"taxCategories"}, null, false, false, null);
    verify(sourceClient, times(1)).taxCategories().get().execute();
  }

  @Test
  void run_AsTaxCategoryFullSync_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    when(apiHttpResponse.getBody()).thenReturn(emptyList());
    when(sourceClient.taxCategories().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

    final SyncerFactory syncerFactory =
        spy(SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock()));

    stubClientsCustomObjectService(targetClient, ZonedDateTime.now());

    // test
    CliRunner.of().run(new String[] {"-s", "taxCategories", "-f"}, syncerFactory);

    // assertions
    verify(syncerFactory, times(1)).sync(new String[] {"taxCategories"}, null, true, false, null);
    verify(sourceClient, times(1)).taxCategories().get().execute();
  }

  @Test
  void run_AsCustomerDeltaSync_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    final SyncerFactory syncerFactory =
        spy(SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock()));

    stubClientsCustomObjectService(targetClient, ZonedDateTime.now());

    // test
    CliRunner.of().run(new String[] {"-s", "customers"}, syncerFactory);

    // assertions
    verify(syncerFactory, times(1)).sync(new String[] {"customers"}, null, false, false, null);
    verify(sourceClient, times(1)).customers().get().execute();
  }

  @Test
  void run_AsCustomerFullSync_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    when(apiHttpResponse.getBody()).thenReturn(emptyList());
    when(sourceClient.customers().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

    final SyncerFactory syncerFactory =
        spy(SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock()));

    stubClientsCustomObjectService(targetClient, ZonedDateTime.now());

    // test
    CliRunner.of().run(new String[] {"-s", "customers", "-f"}, syncerFactory);

    // assertions
    verify(syncerFactory, times(1)).sync(new String[] {"customers"}, null, true, false, null);
    verify(sourceClient, times(1)).customers().get().execute();
  }

  @Test
  void run_AsShoppingListDeltaSync_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    final SyncerFactory syncerFactory =
        spy(SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock()));

    stubClientsCustomObjectService(targetClient, ZonedDateTime.now());

    // test
    CliRunner.of().run(new String[] {"-s", "shoppingLists"}, syncerFactory);

    // assertions
    verify(syncerFactory, times(1)).sync(new String[] {"shoppingLists"}, null, false, false, null);
    verify(sourceClient, times(1)).shoppingLists().get().execute();
  }

  @Test
  void run_AsShoppingListFullSync_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    when(apiHttpResponse.getBody()).thenReturn(emptyList());
    when(sourceClient.shoppingLists().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

    final SyncerFactory syncerFactory =
        spy(SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock()));

    stubClientsCustomObjectService(targetClient, ZonedDateTime.now());

    // test
    CliRunner.of().run(new String[] {"-s", "shoppingLists", "-f"}, syncerFactory);

    // assertions
    verify(syncerFactory, times(1)).sync(new String[] {"shoppingLists"}, null, true, false, null);
    verify(sourceClient, times(1)).shoppingLists().get().execute();
  }

  @Test
  void run_AsCustomObjectDeltaSync_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    when(apiHttpResponse.getBody()).thenReturn(emptyList());
    when(sourceClient.customObjects().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

    final SyncerFactory syncerFactory =
        spy(SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock()));

    stubClientsCustomObjectService(targetClient, ZonedDateTime.now());

    // test
    CliRunner.of().run(new String[] {"-s", "customObjects"}, syncerFactory);

    // assertions
    verify(syncerFactory, times(1)).sync(new String[] {"customObjects"}, null, false, false, null);
    verify(sourceClient, times(1)).customObjects().get().execute();
  }

  @Test
  void run_AsCustomObjectFullSync_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    when(apiHttpResponse.getBody()).thenReturn(emptyList());
    when(sourceClient.customObjects().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

    final SyncerFactory syncerFactory =
        spy(SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock()));

    stubClientsCustomObjectService(targetClient, ZonedDateTime.now());

    // test
    CliRunner.of().run(new String[] {"-s", "customObjects", "-f"}, syncerFactory);

    // assertions
    verify(syncerFactory, times(1)).sync(new String[] {"customObjects"}, null, true, false, null);
    verify(sourceClient, times(1)).customObjects().get().execute();
  }

  @Test
  void run_AsCartDiscountFullSync_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    when(apiHttpResponse.getBody()).thenReturn(emptyList());
    when(sourceClient.cartDiscounts().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

    final SyncerFactory syncerFactory =
        spy(SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock()));

    stubClientsCustomObjectService(targetClient, ZonedDateTime.now());

    // test
    CliRunner.of().run(new String[] {"-s", "cartDiscounts", "-f"}, syncerFactory);

    // assertions
    verify(syncerFactory, times(1)).sync(new String[] {"cartDiscounts"}, null, true, false, null);
    verify(sourceClient, times(1)).cartDiscounts().get().execute();
  }

  @Test
  void run_AsStateFullSync_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    when(apiHttpResponse.getBody()).thenReturn(emptyList());
    when(sourceClient.states().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

    final SyncerFactory syncerFactory =
        spy(SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock()));

    stubClientsCustomObjectService(targetClient, ZonedDateTime.now());

    // test
    CliRunner.of().run(new String[] {"-s", "states", "-f"}, syncerFactory);

    // assertions
    verify(syncerFactory, times(1)).sync(new String[] {"states"}, null, true, false, null);
    verify(sourceClient, times(1)).states().get().execute();
  }

  @Test
  void run_WithSyncAsLongArgument_ShouldBuildSyncerAndExecuteSync() {
    // preparation
    when(apiHttpResponse.getBody()).thenReturn(emptyList());
    when(sourceClient.productProjections().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

    final SyncerFactory syncerFactory =
        spy(SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock()));

    stubClientsCustomObjectService(targetClient, ZonedDateTime.now());

    // test
    CliRunner.of().run(new String[] {"--sync", "products"}, syncerFactory);

    // assertions
    verify(syncerFactory, times(1)).sync(new String[] {"products"}, null, false, false, null);
    verify(sourceClient, times(1)).productProjections().get().execute();
  }

  @Test
  void run_WithRunnerName_ShouldProcessSyncOption() {
    // preparation
    when(apiHttpResponse.getBody()).thenReturn(emptyList());
    when(sourceClient.productProjections().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

    final SyncerFactory syncerFactory =
        spy(SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock()));

    stubClientsCustomObjectService(targetClient, ZonedDateTime.now());

    // test
    CliRunner.of().run(new String[] {"--sync", "products", "-r", "Runner123"}, syncerFactory);

    // assertions
    verify(syncerFactory, times(1))
        .sync(new String[] {"products"}, "Runner123", false, false, null);
    verify(sourceClient, times(1)).productProjections().get().execute();
  }

  @Test
  void run_WithRunnerNameLong_ShouldProcessSyncOption() {
    // preparation
    when(apiHttpResponse.getBody()).thenReturn(emptyList());
    when(sourceClient.productProjections().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

    final SyncerFactory syncerFactory =
        spy(SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock()));

    stubClientsCustomObjectService(targetClient, ZonedDateTime.now());

    // test
    CliRunner.of()
        .run(
            new String[] {"--sync", "products", "--runnerName", "Runner123", "--full"},
            syncerFactory);

    // assertions
    verify(syncerFactory, times(1)).sync(new String[] {"products"}, "Runner123", true, false, null);
    verify(sourceClient, times(1)).productProjections().get().execute();
  }

  @Test
  void run_WithUnknownArgument_ShouldPrintAndLogError() {
    // preparation
    final SyncerFactory syncerFactory =
        spy(
            SyncerFactory.of(
                () -> mock(ProjectApiRoot.class),
                () -> mock(ProjectApiRoot.class),
                getMockedClock()));
    // test
    CliRunner.of().run(new String[] {"-u"}, syncerFactory);

    // Assert error log
    verify(syncerFactory, never()).sync(any(), any(), anyBoolean(), anyBoolean(), any());
  }

  @Test
  void run_WithHelpAsArgument_ShouldPrintThreeOptionsWithDescriptionsToSystemOut()
      throws UnsupportedEncodingException {
    // preparation
    final SyncerFactory syncerFactory =
        spy(
            SyncerFactory.of(
                () -> mock(ProjectApiRoot.class),
                () -> mock(ProjectApiRoot.class),
                getMockedClock()));

    // test
    CliRunner.of().run(new String[] {"-h"}, syncerFactory);

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
                "-%s,--%s <arg> %s",
                RUNNER_NAME_OPTION_SHORT, RUNNER_NAME_OPTION_LONG, RUNNER_NAME_OPTION_DESCRIPTION))
        .contains(
            format(
                "-%s,--%s %s",
                VERSION_OPTION_SHORT, VERSION_OPTION_LONG, VERSION_OPTION_DESCRIPTION));
    verify(syncerFactory, never()).sync(any(), any(), anyBoolean(), anyBoolean(), any());
  }

  @Test
  void run_WithSyncAsArgumentWithAllArg_ShouldExecuteAllSyncers() {
    // preparation
    when(apiHttpResponse.getBody()).thenReturn(emptyList());
    when(sourceClient.productTypes().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(sourceClient.types().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(sourceClient.categories().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(sourceClient.inventory().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(sourceClient.productProjections().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(sourceClient.cartDiscounts().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(sourceClient.states().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(sourceClient.taxCategories().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(sourceClient.customObjects().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(sourceClient.customers().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(sourceClient.shoppingLists().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

    stubClientsCustomObjectService(targetClient, ZonedDateTime.now());

    final SyncerFactory syncerFactory =
        spy(SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock()));

    // test
    CliRunner.of().run(new String[] {"-s", "all"}, syncerFactory);

    // assertions
    verify(syncerFactory, times(1)).sync(new String[] {"all"}, null, false, false, null);
    verify(sourceClient, times(1)).productTypes().get().execute();
    verify(sourceClient, times(1)).types().get().execute();
    verify(sourceClient, times(1)).taxCategories().get().execute();
    verify(sourceClient, times(1)).categories().get().execute();
    verify(sourceClient, times(1)).productProjections().get().execute();
    verify(sourceClient, times(1)).inventory().get().execute();
    verify(sourceClient, times(1)).cartDiscounts().get().execute();
    verify(sourceClient, times(1)).states().get().execute();
    verify(sourceClient, times(1)).customObjects().get().execute();
    verify(sourceClient, times(1)).customers().get().execute();
    verify(sourceClient, times(1)).shoppingLists().get().execute();
  }

  @Test
  void run_WithSyncAsArgumentWithAllArgWithRunnerName_ShouldExecuteAllSyncers() {
    // preparation
    when(apiHttpResponse.getBody()).thenReturn(emptyList());
    when(sourceClient.productTypes().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(sourceClient.types().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(sourceClient.categories().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(sourceClient.inventory().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(sourceClient.productProjections().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(sourceClient.cartDiscounts().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(sourceClient.states().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(sourceClient.taxCategories().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(sourceClient.customObjects().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(sourceClient.customers().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(sourceClient.shoppingLists().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

    stubClientsCustomObjectService(targetClient, ZonedDateTime.now());

    final SyncerFactory syncerFactory =
        spy(SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock()));

    // test
    CliRunner.of().run(new String[] {"-s", "all", "-r", "myRunner"}, syncerFactory);

    // assertions
    verify(syncerFactory, times(1)).sync(new String[] {"all"}, "myRunner", false, false, null);
    verify(sourceClient, times(1)).productTypes().get().execute();
    verify(sourceClient, times(1)).types().get().execute();
    verify(sourceClient, times(1)).taxCategories().get().execute();
    verify(sourceClient, times(1)).categories().get().execute();
    verify(sourceClient, times(1)).productProjections().get().execute();
    verify(sourceClient, times(1)).inventory().get().execute();
    verify(sourceClient, times(1)).cartDiscounts().get().execute();
    verify(sourceClient, times(1)).states().get().execute();
    verify(sourceClient, times(1)).customObjects().get().execute();
    verify(sourceClient, times(1)).customers().get().execute();
    verify(sourceClient, times(1)).shoppingLists().get().execute();
  }

  @Test
  void run_WithSyncAsArgumentWithAllArg_ShouldExecuteAllSyncersInCorrectOrder() {
    // preparation
    when(sourceClient.productTypes().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(sourceClient.types().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(sourceClient.categories().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(sourceClient.inventory().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(sourceClient.productProjections().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(sourceClient.cartDiscounts().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(sourceClient.states().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(sourceClient.taxCategories().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(sourceClient.customObjects().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(sourceClient.customers().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(sourceClient.shoppingLists().get().execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));

    stubClientsCustomObjectService(targetClient, ZonedDateTime.now());

    final SyncerFactory syncerFactory =
        spy(SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock()));

    // test
    CliRunner.of().run(new String[] {"-s", "all", "-f"}, syncerFactory);

    // assertions
    verify(syncerFactory, times(1)).sync(new String[] {"all"}, null, true, false, null);

    final InOrder inOrder = Mockito.inOrder(sourceClient);

    // Resources are grouped based on their references count.
    // Each group will run sequentially but the sync within the group runs in parallel.
    // So verifying the order of one resource in each group.
    inOrder.verify(sourceClient).productTypes().get().execute();
    verify(sourceClient, times(1)).types().get().execute();
    verify(sourceClient, times(1)).customObjects().get().execute();
    verify(sourceClient, times(1)).states().get().execute();
    verify(sourceClient, times(1)).taxCategories().get().execute();

    inOrder.verify(sourceClient).inventory().get().execute();
    verify(sourceClient, times(1)).categories().get().execute();
    verify(sourceClient, times(1)).cartDiscounts().get().execute();
    verify(sourceClient, times(1)).customers().get().execute();

    inOrder.verify(sourceClient).productProjections().get().execute();

    inOrder.verify(sourceClient).shoppingLists().get().execute();

    verify(sourceClient, times(1)).close();
    //    verify(sourceClient, times(11)).getConfig
  }

  @Test
  void run_WithOnlySyncCustomObjectArgument_ShouldThrowException() {
    // preparation
    final SyncerFactory syncerFactory =
        spy(SyncerFactory.of(() -> sourceClient, () -> targetClient, getMockedClock()));

    // test
    CliRunner.of().run(new String[] {"--syncProjectSyncCustomObjects"}, syncerFactory);

    // assertions
    assertThat(testLogger.getAllLoggingEvents())
        .hasSize(1)
        .singleElement()
        .satisfies(
            loggingEvent -> {
              assertThat(loggingEvent.getLevel()).isEqualTo(Level.ERROR);
              assertThat(loggingEvent.getMessage()).contains("Failed to run sync process.");
              final Optional<Throwable> actualThrowableOpt = loggingEvent.getThrowable();
              assertThat(actualThrowableOpt).isNotNull();
              assertThat(actualThrowableOpt.isPresent()).isTrue();
              final Throwable actualThrowable = actualThrowableOpt.get();
              assertThat(actualThrowable).isExactlyInstanceOf(CliException.class);
              assertThat(actualThrowable.getMessage())
                  .isEqualTo(
                      format(
                          "Please pass at least 1 more option other than %s to the CLI.",
                          CliRunner.SYNC_PROJECT_SYNC_CUSTOM_OBJECTS_OPTION_LONG));
            });
  }
}
