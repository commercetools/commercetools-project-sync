package com.commercetools.project.sync.customer;

import static com.commercetools.project.sync.util.TestUtils.mockResourceIdsGraphQlRequest;
import static com.commercetools.project.sync.util.TestUtils.readObjectFromResource;
import static com.commercetools.sync.customers.utils.CustomerTransformUtils.toCustomerDrafts;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ByProjectKeyCustomersGet;
import com.commercetools.api.client.ByProjectKeyCustomersRequestBuilder;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerDraft;
import com.commercetools.api.models.customer.CustomerPagedQueryResponse;
import com.commercetools.api.models.customer.CustomerPagedQueryResponseBuilder;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.customers.CustomerSync;
import com.github.valfirst.slf4jtest.LoggingEvent;
import com.github.valfirst.slf4jtest.TestLogger;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomerSyncerTest {
  private final TestLogger syncerTestLogger = TestLoggerFactory.getTestLogger(CustomerSyncer.class);

  private final ReferenceIdToKeyCache referenceIdToKeyCache =
      new CaffeineReferenceIdToKeyCacheImpl();

  @BeforeEach
  void setup() {
    syncerTestLogger.clearAll();
  }

  @Test
  void of_ShouldCreateCustomerSyncerInstance() {
    // test
    final CustomerSyncer customerSyncer =
        CustomerSyncer.of(
            mock(ProjectApiRoot.class), mock(ProjectApiRoot.class), mock(Clock.class));

    // assertion
    assertThat(customerSyncer).isNotNull();
    assertThat(customerSyncer.getSync()).isInstanceOf(CustomerSync.class);
  }

  @Test
  void transform_ShouldReplaceCustomerReferenceIdsWithKeys() {
    // preparation
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final CustomerSyncer customerSyncer =
        CustomerSyncer.of(sourceClient, mock(ProjectApiRoot.class), mock(Clock.class));
    final List<Customer> customers =
        Collections.singletonList(readObjectFromResource("customer-key-1.json", Customer.class));
    mockResourceIdsGraphQlRequest(
        sourceClient, "customerGroups", "d1229e6f-2b79-441e-b419-180311e52754", "customerGroupKey");

    // test
    final CompletionStage<List<CustomerDraft>> draftsFromPageStage =
        customerSyncer.transform(customers);

    // assertion
    assertThat(draftsFromPageStage)
        .isCompletedWithValue(
            toCustomerDrafts(sourceClient, referenceIdToKeyCache, customers).join());
  }

  @Test
  void getQuery_ShouldBuildCustomerQuery() {
    final ProjectApiRoot projectApiRoot = mock(ProjectApiRoot.class);
    final ByProjectKeyCustomersRequestBuilder byProjectKeyCustomersRequestBuilder = mock();
    when(projectApiRoot.customers()).thenReturn(byProjectKeyCustomersRequestBuilder);
    final ByProjectKeyCustomersGet byProjectKeyCustomersGet = mock();
    when(byProjectKeyCustomersRequestBuilder.get()).thenReturn(byProjectKeyCustomersGet);

    // test
    final CustomerSyncer customerSyncer =
        CustomerSyncer.of(projectApiRoot, mock(ProjectApiRoot.class), mock(Clock.class));

    // assertion
    assertThat(customerSyncer.getQuery()).isEqualTo(byProjectKeyCustomersGet);
  }

  @Test
  void syncWithError_ShouldCallErrorCallback() {
    // preparation: customer with no key is synced
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final List<Customer> customers =
        Collections.singletonList(readObjectFromResource("customer-no-key.json", Customer.class));
    mockProjectApiRootGetRequest(sourceClient, customers);

    // test
    final CustomerSyncer customerSyncer =
        CustomerSyncer.of(sourceClient, mock(ProjectApiRoot.class), mock(Clock.class));
    customerSyncer.sync(null, true).toCompletableFuture().join();

    // assertion
    //    final LoggingEvent errorLog = syncerTestLogger.getAllLoggingEvents().get(1);
    //    assertThat(errorLog.getMessage())
    //        .isEqualTo(
    //            "Error when trying to sync customer. Existing key: <<not present>>. Update
    // actions: []");
    //    assertThat(errorLog.getThrowable().get().getMessage())
    //        .isEqualTo(
    //            "CustomerDraft with email: email@email.com doesn't have a key. Please make sure
    // all customer drafts have keys.");
  }

  @Test
  void syncWithWarning_ShouldCallWarningCallback() {
    // preparation: source customer has a different customer number than target customer
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final ProjectApiRoot targetClient = mock(ProjectApiRoot.class);
    final List<Customer> sourceCustomers =
        Collections.singletonList(readObjectFromResource("customer-id-1.json", Customer.class));
    final List<Customer> targetCustomers =
        Collections.singletonList(readObjectFromResource("customer-id-2.json", Customer.class));

    mockProjectApiRootGetRequest(sourceClient, sourceCustomers);
    mockProjectApiRootGetRequest(targetClient, targetCustomers);

    // test
    final CustomerSyncer customerSyncer =
        CustomerSyncer.of(sourceClient, targetClient, mock(Clock.class));
    customerSyncer.sync(null, true).toCompletableFuture().join();

    // assertion
    final LoggingEvent errorLog = syncerTestLogger.getAllLoggingEvents().get(1);
    assertThat(errorLog.getMessage())
        .isEqualTo("Warning when trying to sync customer. Existing key: customerKey");
    assertThat(errorLog.getThrowable().get().getMessage())
        .isEqualTo(
            "Customer with key: \"customerKey\" has already a customer number: \"2\", once it's set it cannot be changed. Hereby, the update action is not created.");
  }

  @Test
  void transform_WithAdditionalAddressInfoAndState_ShouldPreserveAddressFields() {
    // preparation
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final CustomerSyncer customerSyncer =
        CustomerSyncer.of(sourceClient, mock(ProjectApiRoot.class), mock(Clock.class));
    final List<Customer> customers =
        Collections.singletonList(
            readObjectFromResource("customer-with-state-reference.json", Customer.class));

    // test
    final CompletionStage<List<CustomerDraft>> draftsFromPageStage =
        customerSyncer.transform(customers);

    // assertion
    final List<CustomerDraft> customerDrafts = draftsFromPageStage.toCompletableFuture().join();
    assertThat(customerDrafts).isNotEmpty();
    assertThat(customerDrafts.get(0).getAddresses()).isNotEmpty();
    assertThat(customerDrafts.get(0).getAddresses().get(0).getState()).isEqualTo("New York");
    assertThat(customerDrafts.get(0).getAddresses().get(0).getAdditionalAddressInfo())
        .isEqualTo("Building B, Floor 5");
  }

  @Test
  void transform_WithMultipleAddressesWithStateAndAdditionalInfo_ShouldPreserveAllAddressFields() {
    // preparation
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final CustomerSyncer customerSyncer =
        CustomerSyncer.of(sourceClient, mock(ProjectApiRoot.class), mock(Clock.class));
    final List<Customer> customers =
        Collections.singletonList(
            readObjectFromResource("customer-with-multiple-addresses.json", Customer.class));

    // test
    final CompletionStage<List<CustomerDraft>> draftsFromPageStage =
        customerSyncer.transform(customers);

    // assertion
    final List<CustomerDraft> customerDrafts = draftsFromPageStage.toCompletableFuture().join();
    assertThat(customerDrafts).isNotEmpty();
    assertThat(customerDrafts.get(0).getAddresses()).hasSize(2);

    // Verify first address
    assertThat(customerDrafts.get(0).getAddresses().get(0).getState()).isEqualTo("California");
    assertThat(customerDrafts.get(0).getAddresses().get(0).getAdditionalAddressInfo())
        .isEqualTo("Ring doorbell twice");

    // Verify second address
    assertThat(customerDrafts.get(0).getAddresses().get(1).getState()).isEqualTo("California");
    assertThat(customerDrafts.get(0).getAddresses().get(1).getAdditionalAddressInfo())
        .isEqualTo("Suite 300, Reception on 3rd floor");
  }

  private void mockProjectApiRootGetRequest(
      final ProjectApiRoot projectApiRoot, final List<Customer> results) {
    final ByProjectKeyCustomersRequestBuilder byProjectKeyCustomersRequestBuilder = mock();
    when(projectApiRoot.customers()).thenReturn(byProjectKeyCustomersRequestBuilder);
    final ByProjectKeyCustomersGet byProjectKeyCustomersGet = mock();
    when(byProjectKeyCustomersRequestBuilder.get()).thenReturn(byProjectKeyCustomersGet);
    when(byProjectKeyCustomersGet.withSort(anyString())).thenReturn(byProjectKeyCustomersGet);
    when(byProjectKeyCustomersGet.withWithTotal(anyBoolean())).thenReturn(byProjectKeyCustomersGet);
    when(byProjectKeyCustomersGet.withLimit(anyInt())).thenReturn(byProjectKeyCustomersGet);
    when(byProjectKeyCustomersGet.withWhere(anyString())).thenReturn(byProjectKeyCustomersGet);
    when(byProjectKeyCustomersGet.withPredicateVar(anyString(), any()))
        .thenReturn(byProjectKeyCustomersGet);

    final ApiHttpResponse<CustomerPagedQueryResponse> response = mock(ApiHttpResponse.class);
    final CustomerPagedQueryResponse customerPagedQueryResponse =
        CustomerPagedQueryResponseBuilder.of()
            .results(results)
            .limit(20L)
            .offset(0L)
            .count(1L)
            .build();
    when(response.getBody()).thenReturn(customerPagedQueryResponse);
    when(byProjectKeyCustomersGet.execute())
        .thenReturn(CompletableFuture.completedFuture(response));
  }
}
