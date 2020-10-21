package com.commercetools.project.sync.customer;

import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.sync.customers.CustomerSync;
import com.commercetools.sync.customers.utils.CustomerReferenceResolutionUtils;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.queries.CustomerQuery;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

class CustomerSyncerTest {
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
}
