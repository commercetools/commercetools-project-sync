package com.commercetools.project.sync.customer;

import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.customers.CustomerSync;
import com.commercetools.sync.customers.utils.CustomerReferenceResolutionUtils;
import io.sphere.sdk.client.SphereApiConfig;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.queries.CustomerQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

class CustomerSyncerTest {
  private final TestLogger syncerTestLogger = TestLoggerFactory.getTestLogger(CustomerSyncer.class);

  @BeforeEach
  void setup() {
    syncerTestLogger.clearAll();
  }

  @Test
  void of_ShouldCreateCustomerSyncerInstance() {
    // test
    final CustomerSyncer customerSyncer =
        CustomerSyncer.of(mock(SphereClient.class), mock(SphereClient.class), mock(Clock.class));

    // assertion
    assertThat(customerSyncer).isNotNull();
    assertThat(customerSyncer.getSync()).isInstanceOf(CustomerSync.class);
  }

  @Test
  void transform_ShouldReplaceCustomerReferenceIdsWithKeys() {
    // preparation
    final CustomerSyncer customerSyncer =
        CustomerSyncer.of(mock(SphereClient.class), mock(SphereClient.class), mock(Clock.class));
    final List<Customer> customers =
        Collections.singletonList(readObjectFromResource("customer-key-1.json", Customer.class));

    // test
    final CompletionStage<List<CustomerDraft>> draftsFromPageStage =
        customerSyncer.transform(customers);

    // assertion
    assertThat(draftsFromPageStage)
        .isCompletedWithValue(CustomerReferenceResolutionUtils.mapToCustomerDrafts(customers));
  }

  @Test
  void getQuery_ShouldBuildCustomerQuery() {
    // test
    final CustomerSyncer customerSyncer =
        CustomerSyncer.of(mock(SphereClient.class), mock(SphereClient.class), mock(Clock.class));

    // assertion
    final CustomerQuery query = customerSyncer.getQuery();
    assertThat(query).isEqualTo(CustomerReferenceResolutionUtils.buildCustomerQuery());
  }

  @Test
  void syncWithError_ShouldCallErrorCallback() {
    // preparation: customer with no key is synced
    final SphereClient sourceClient = mock(SphereClient.class);
    final SphereClient targetClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereApiConfig.of("source-project"));
    when(targetClient.getConfig()).thenReturn(SphereApiConfig.of("target-project"));
    final List<Customer> customers =
        Collections.singletonList(readObjectFromResource("customer-no-key.json", Customer.class));

    final PagedQueryResult<Customer> pagedQueryResult = mock(PagedQueryResult.class);
    when(pagedQueryResult.getResults()).thenReturn(customers);
    when(sourceClient.execute(any(CustomerQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(pagedQueryResult));

    // test
    final CustomerSyncer customerSyncer =
        CustomerSyncer.of(sourceClient, targetClient, mock(Clock.class));
    customerSyncer.sync(null, true).toCompletableFuture().join();

    // assertion
    final LoggingEvent errorLog = syncerTestLogger.getAllLoggingEvents().get(0);
    assertThat(errorLog.getMessage())
        .isEqualTo(
            "Error when trying to sync customer. Existing key: <<not present>>. Update actions: []");
    assertThat(errorLog.getThrowable().get().getMessage())
        .isEqualTo(
            "CustomerDraft with email: email@email.com doesn't have a key. Please make sure all customer drafts have keys.");
  }

  @Test
  void syncWithWarning_ShouldCallWarningCallback() {
    // preparation: source customer has a different customer number than target customer
    final SphereClient sourceClient = mock(SphereClient.class);
    final SphereClient targetClient = mock(SphereClient.class);
    when(sourceClient.getConfig()).thenReturn(SphereApiConfig.of("source-project"));
    when(targetClient.getConfig()).thenReturn(SphereApiConfig.of("target-project"));
    final List<Customer> sourceCustomers =
        Collections.singletonList(readObjectFromResource("customer-id-1.json", Customer.class));
    final List<Customer> targetCustomers =
        Collections.singletonList(readObjectFromResource("customer-id-2.json", Customer.class));

    final PagedQueryResult<Customer> sourcePagedQueryResult = mock(PagedQueryResult.class);
    when(sourcePagedQueryResult.getResults()).thenReturn(sourceCustomers);
    when(sourceClient.execute(any(CustomerQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(sourcePagedQueryResult));

    final PagedQueryResult<Customer> targetPagedQueryResult = mock(PagedQueryResult.class);
    when(targetPagedQueryResult.getResults()).thenReturn(targetCustomers);
    when(targetClient.execute(any(CustomerQuery.class)))
        .thenReturn(CompletableFuture.completedFuture(targetPagedQueryResult));

    // test
    final CustomerSyncer customerSyncer =
        CustomerSyncer.of(sourceClient, targetClient, mock(Clock.class));
    customerSyncer.sync(null, true).toCompletableFuture().join();

    // assertion
    final LoggingEvent errorLog = syncerTestLogger.getAllLoggingEvents().get(0);
    assertThat(errorLog.getMessage())
        .isEqualTo("Warning when trying to sync customer. Existing key: customerKey");
    assertThat(errorLog.getThrowable().get().getMessage())
        .isEqualTo(
            "Customer with key: \"customerKey\" has already a customer number: \"2\", once it's set it cannot be changed. Hereby, the update action is not created.");
  }
}
