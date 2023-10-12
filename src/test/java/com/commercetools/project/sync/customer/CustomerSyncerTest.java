package com.commercetools.project.sync.customer;

import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import org.junit.jupiter.api.BeforeEach;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

// These tests aren't migrated
// TODO: Migrate tests
class CustomerSyncerTest {
  private final TestLogger syncerTestLogger = TestLoggerFactory.getTestLogger(CustomerSyncer.class);

  private final ReferenceIdToKeyCache referenceIdToKeyCache =
      new CaffeineReferenceIdToKeyCacheImpl();

  @BeforeEach
  void setup() {
    syncerTestLogger.clearAll();
  }

  //  @Test
  //  void of_ShouldCreateCustomerSyncerInstance() {
  //    // test
  //    final CustomerSyncer customerSyncer =
  //        CustomerSyncer.of(mock(SphereClient.class), mock(SphereClient.class),
  // mock(Clock.class));
  //
  //    // assertion
  //    assertThat(customerSyncer).isNotNull();
  //    assertThat(customerSyncer.getSync()).isInstanceOf(CustomerSync.class);
  //  }
  //
  //  @Test
  //  void transform_ShouldReplaceCustomerReferenceIdsWithKeys() {
  //    // preparation
  //    final SphereClient sourceClient = mock(SphereClient.class);
  //    final CustomerSyncer customerSyncer =
  //        CustomerSyncer.of(sourceClient, mock(SphereClient.class), mock(Clock.class));
  //    final List<Customer> customers =
  //        Collections.singletonList(readObjectFromResource("customer-key-1.json",
  // Customer.class));
  //
  //    final String jsonStringCustomerGroups =
  //        "{\"results\":[{\"id\":\"d1229e6f-2b79-441e-b419-180311e52754\","
  //            + "\"key\":\"customerGroupKey\"} ]}";
  //    final ResourceKeyIdGraphQlResult customerGroupsResult =
  //        SphereJsonUtils.readObject(jsonStringCustomerGroups, ResourceKeyIdGraphQlResult.class);
  //
  //    when(sourceClient.execute(any(ResourceIdsGraphQlRequest.class)))
  //        .thenReturn(CompletableFuture.completedFuture(customerGroupsResult));
  //
  //    // test
  //    final CompletionStage<List<CustomerDraft>> draftsFromPageStage =
  //        customerSyncer.transform(customers);
  //
  //    // assertion
  //    assertThat(draftsFromPageStage)
  //        .isCompletedWithValue(
  //            toCustomerDrafts(sourceClient, referenceIdToKeyCache, customers).join());
  //  }
  //
  //  @Test
  //  void getQuery_ShouldBuildCustomerQuery() {
  //    // test
  //    final CustomerSyncer customerSyncer =
  //        CustomerSyncer.of(mock(SphereClient.class), mock(SphereClient.class),
  // mock(Clock.class));
  //
  //    // assertion
  //    final CustomerQuery query = customerSyncer.getQuery();
  //    assertThat(query).isEqualTo(CustomerQuery.of());
  //  }
  //
  //  @Test
  //  void syncWithError_ShouldCallErrorCallback() {
  //    // preparation: customer with no key is synced
  //    final SphereClient sourceClient = mock(SphereClient.class);
  //    final SphereClient targetClient = mock(SphereClient.class);
  //    when(sourceClient.getConfig()).thenReturn(SphereApiConfig.of("source-project"));
  //    when(targetClient.getConfig()).thenReturn(SphereApiConfig.of("target-project"));
  //    final List<Customer> customers =
  //        Collections.singletonList(readObjectFromResource("customer-no-key.json",
  // Customer.class));
  //
  //    final PagedQueryResult<Customer> pagedQueryResult = mock(PagedQueryResult.class);
  //    when(pagedQueryResult.getResults()).thenReturn(customers);
  //    when(sourceClient.execute(any(CustomerQuery.class)))
  //        .thenReturn(CompletableFuture.completedFuture(pagedQueryResult));
  //
  //    // test
  //    final CustomerSyncer customerSyncer =
  //        CustomerSyncer.of(sourceClient, targetClient, mock(Clock.class));
  //    customerSyncer.sync(null, true).toCompletableFuture().join();
  //
  //    // assertion
  //    final LoggingEvent errorLog = syncerTestLogger.getAllLoggingEvents().get(1);
  //    assertThat(errorLog.getMessage())
  //        .isEqualTo(
  //            "Error when trying to sync customer. Existing key: <<not present>>. Update actions:
  // []");
  //    assertThat(errorLog.getThrowable().get().getMessage())
  //        .isEqualTo(
  //            "CustomerDraft with email: email@email.com doesn't have a key. Please make sure all
  // customer drafts have keys.");
  //  }
  //
  //  @Test
  //  void syncWithWarning_ShouldCallWarningCallback() {
  //    // preparation: source customer has a different customer number than target customer
  //    final SphereClient sourceClient = mock(SphereClient.class);
  //    final SphereClient targetClient = mock(SphereClient.class);
  //    when(sourceClient.getConfig()).thenReturn(SphereApiConfig.of("source-project"));
  //    when(targetClient.getConfig()).thenReturn(SphereApiConfig.of("target-project"));
  //    final List<Customer> sourceCustomers =
  //        Collections.singletonList(readObjectFromResource("customer-id-1.json", Customer.class));
  //    final List<Customer> targetCustomers =
  //        Collections.singletonList(readObjectFromResource("customer-id-2.json", Customer.class));
  //
  //    final PagedQueryResult<Customer> sourcePagedQueryResult = mock(PagedQueryResult.class);
  //    when(sourcePagedQueryResult.getResults()).thenReturn(sourceCustomers);
  //    when(sourceClient.execute(any(CustomerQuery.class)))
  //        .thenReturn(CompletableFuture.completedFuture(sourcePagedQueryResult));
  //
  //    final PagedQueryResult<Customer> targetPagedQueryResult = mock(PagedQueryResult.class);
  //    when(targetPagedQueryResult.getResults()).thenReturn(targetCustomers);
  //    when(targetClient.execute(any(CustomerQuery.class)))
  //        .thenReturn(CompletableFuture.completedFuture(targetPagedQueryResult));
  //
  //    // test
  //    final CustomerSyncer customerSyncer =
  //        CustomerSyncer.of(sourceClient, targetClient, mock(Clock.class));
  //    customerSyncer.sync(null, true).toCompletableFuture().join();
  //
  //    // assertion
  //    final LoggingEvent errorLog = syncerTestLogger.getAllLoggingEvents().get(1);
  //    assertThat(errorLog.getMessage())
  //        .isEqualTo("Warning when trying to sync customer. Existing key: customerKey");
  //    assertThat(errorLog.getThrowable().get().getMessage())
  //        .isEqualTo(
  //            "Customer with key: \"customerKey\" has already a customer number: \"2\", once it's
  // set it cannot be changed. Hereby, the update action is not created.");
  //  }
}
