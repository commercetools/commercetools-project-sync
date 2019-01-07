package com.commercetools.project.sync;

import static com.commercetools.project.sync.SyncerFactory.AVAILABLE_OPTIONS;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.types.queries.TypeQuery;
import io.sphere.sdk.utils.CompletableFutureUtils;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import javax.annotation.Nonnull;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

public class SyncerFactoryTest {
  private static final TestLogger testLogger = TestLoggerFactory.getTestLogger(CliRunner.class);
  private static ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
  private static PrintStream originalSystemOut;

  @BeforeClass
  public static void setupSuite() throws UnsupportedEncodingException {
    final PrintStream printStream = new PrintStream(outputStream, false, "UTF-8");
    originalSystemOut = System.out;
    System.setOut(printStream);
  }

  @After
  public void tearDownTest() {
    testLogger.clearAll();
  }

  @Test
  public void sync_WithNullOptionValue_ShouldThrowIllegalArgumentException() {
    assertThatThrownBy(
            () -> SyncerFactory.of(mock(SphereClient.class), mock(SphereClient.class)).sync(null))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(
            format(
                "Blank argument supplied to \"-s\" or \"--sync\" option! %s", AVAILABLE_OPTIONS));
  }

  @Test
  public void sync_WithEmptyOptionValue_ShouldThrowIllegalArgumentException() {
    assertThatThrownBy(
            () -> SyncerFactory.of(mock(SphereClient.class), mock(SphereClient.class)).sync(null))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(
            format(
                "Blank argument supplied to \"-s\" or \"--sync\" option! %s", AVAILABLE_OPTIONS));
  }

  @Test
  public void sync_WithUnknownOptionValue_ShouldThrowIllegalArgumentException() {
    final String unknownOptionValue = "anyOption";
    assertThatThrownBy(
            () ->
                SyncerFactory.of(mock(SphereClient.class), mock(SphereClient.class))
                    .sync(unknownOptionValue))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(
            format(
                "Unknown argument \"%s\" supplied to \"-s\" or \"--sync\" option! %s",
                unknownOptionValue, AVAILABLE_OPTIONS));
  }

  @Test
  public void sync_WithProductsArg_ShouldBuildSyncerAndExecuteSync()
      throws UnsupportedEncodingException {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    when(sourceClient.execute(any(ProductQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));

    final SyncerFactory syncerFactory = SyncerFactory.of(sourceClient, targetClient);

    // test
    syncerFactory.sync("products");

    // assertions
    verify(sourceClient, times(1)).execute(any(ProductQuery.class));
    verifyInteractionsWithClientAfterSync(sourceClient, 1);
    assertThat(outputStream.toString("UTF-8"))
        .contains(
            "Syncing Products from CTP project with key"
                + " 'foo' to project with key 'bar' is done.");
  }

  @Test
  public void sync_WithCategoriesArg_ShouldBuildSyncerAndExecuteSync()
      throws UnsupportedEncodingException {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    when(sourceClient.execute(any(CategoryQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));

    final SyncerFactory syncerFactory = SyncerFactory.of(sourceClient, targetClient);

    // test
    syncerFactory.sync("categories");

    // assertions
    verify(sourceClient, times(1)).execute(any(CategoryQuery.class));
    verifyInteractionsWithClientAfterSync(sourceClient, 1);
    assertThat(outputStream.toString("UTF-8"))
        .contains(
            "Syncing Categories from CTP project with key"
                + " 'foo' to project with key 'bar' is done.");
  }

  @Test
  public void sync_WithProductTypesArg_ShouldBuildSyncerAndExecuteSync()
      throws UnsupportedEncodingException {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    when(sourceClient.execute(any(ProductTypeQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));

    final SyncerFactory syncerFactory = SyncerFactory.of(sourceClient, targetClient);

    // test
    syncerFactory.sync("productTypes");

    // assertions
    verify(sourceClient, times(1)).execute(any(ProductTypeQuery.class));
    verifyInteractionsWithClientAfterSync(sourceClient, 1);
    assertThat(outputStream.toString("UTF-8"))
        .contains(
            "Syncing ProductTypes from CTP project with key"
                + " 'foo' to project with key 'bar' is done.");
  }

  @Test
  public void sync_WithTypesArg_ShouldBuildSyncerAndExecuteSync()
      throws UnsupportedEncodingException {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    when(sourceClient.execute(any(TypeQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));

    final SyncerFactory syncerFactory = SyncerFactory.of(sourceClient, targetClient);

    // test
    syncerFactory.sync("types");

    // assertions
    verify(sourceClient, times(1)).execute(any(TypeQuery.class));
    verifyInteractionsWithClientAfterSync(sourceClient, 1);
    assertThat(outputStream.toString("UTF-8"))
        .contains(
            "Syncing Types from CTP project with key"
                + " 'foo' to project with key 'bar' is done.");
  }

  @Test
  public void sync_WithInventoryEntriesArg_ShouldBuildSyncerAndExecuteSync()
      throws UnsupportedEncodingException {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    when(sourceClient.execute(any(InventoryEntryQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));

    final SyncerFactory syncerFactory = SyncerFactory.of(sourceClient, targetClient);

    // test
    syncerFactory.sync("inventoryEntries");

    // assertions
    verify(sourceClient, times(1)).execute(any(InventoryEntryQuery.class));
    verifyInteractionsWithClientAfterSync(sourceClient, 1);
    assertThat(outputStream.toString("UTF-8"))
        .contains(
            "Syncing Inventories from CTP project with key"
                + " 'foo' to project with key 'bar' is done.");
  }

  @Test
  public void sync_WithErrorOnFetch_ShouldLogAndPrintErrorAndCloseClient()
      throws UnsupportedEncodingException {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereClientConfig.of("foo", "foo", "foo"));

    final SphereClient targetClient = mock(SphereClient.class);
    when(targetClient.getConfig()).thenReturn(SphereClientConfig.of("bar", "bar", "bar"));

    final BadGatewayException badGatewayException = new BadGatewayException();
    when(sourceClient.execute(any(InventoryEntryQuery.class)))
        .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(badGatewayException));

    final SyncerFactory syncerFactory = SyncerFactory.of(sourceClient, targetClient);

    // test
    syncerFactory.sync("inventoryEntries");

    // assertions
    verify(sourceClient, times(1)).execute(any(InventoryEntryQuery.class));
    verifyInteractionsWithClientAfterSync(sourceClient, 0);
    assertThat(testLogger.getAllLoggingEvents())
        .hasOnlyOneElementSatisfying(
            loggingEvent -> {
              assertThat(loggingEvent.getLevel()).isEqualTo(Level.ERROR);
              assertThat(loggingEvent.getMessage()).isEqualTo("Failed to execute sync process");

              final Optional<Throwable> actualThrowableOpt = loggingEvent.getThrowable();
              assertThat(actualThrowableOpt).isNotNull();
              assertThat(actualThrowableOpt.isPresent()).isTrue();
              final Throwable actualThrowable = actualThrowableOpt.get();
              assertThat(actualThrowable).isExactlyInstanceOf(CompletionException.class);
              assertThat(actualThrowable).hasCause(badGatewayException);
            });
    assertThat(outputStream.toString("UTF-8")).contains("Failed to execute sync process");
  }

  @Test
  public void syncAll_ShouldBuildSyncerAndExecuteSync() throws UnsupportedEncodingException {
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

    final SyncerFactory syncerFactory = SyncerFactory.of(sourceClient, targetClient);

    // test
    syncerFactory.syncAll();

    // assertions
    verify(sourceClient, times(1)).execute(any(ProductTypeQuery.class));
    verify(sourceClient, times(1)).execute(any(TypeQuery.class));
    verify(sourceClient, times(1)).execute(any(CategoryQuery.class));
    verify(sourceClient, times(1)).execute(any(ProductQuery.class));
    verify(sourceClient, times(1)).execute(any(InventoryEntryQuery.class));
    verifyInteractionsWithClientAfterSync(sourceClient, 5);
    final String outputStringWithoutLineBreaks = outputStream.toString("UTF-8").replace("\n", "");
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
