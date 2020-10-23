package com.commercetools.project.sync.customer;

import static com.commercetools.project.sync.util.SyncUtils.logErrorCallback;
import static com.commercetools.project.sync.util.SyncUtils.logWarningCallback;

import com.commercetools.project.sync.Syncer;
import com.commercetools.project.sync.service.CustomObjectService;
import com.commercetools.project.sync.service.impl.CustomObjectServiceImpl;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.customers.CustomerSync;
import com.commercetools.sync.customers.CustomerSyncOptions;
import com.commercetools.sync.customers.CustomerSyncOptionsBuilder;
import com.commercetools.sync.customers.helpers.CustomerSyncStatistics;
import com.commercetools.sync.customers.utils.CustomerReferenceResolutionUtils;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.queries.CustomerQuery;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CustomerSyncer
    extends Syncer<
        Customer,
        CustomerDraft,
        CustomerSyncStatistics,
        CustomerSyncOptions,
        CustomerQuery,
        CustomerSync> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CustomerSyncer.class);

  private CustomerSyncer(
      @Nonnull final CustomerSync customerSync,
      @Nonnull final SphereClient sourceClient,
      @Nonnull final SphereClient targetClient,
      @Nonnull final CustomObjectService customObjectService,
      @Nonnull final Clock clock) {
    super(customerSync, sourceClient, targetClient, customObjectService, clock);
  }

  public static CustomerSyncer of(
      @Nonnull final SphereClient sourceClient,
      @Nonnull final SphereClient targetClient,
      @Nonnull final Clock clock) {
    final QuadConsumer<
            SyncException,
            Optional<CustomerDraft>,
            Optional<Customer>,
            List<UpdateAction<Customer>>>
        logErrorCallback =
            (exception, newResourceDraft, oldResource, updateActions) ->
                logErrorCallback(LOGGER, "customer", exception, oldResource, updateActions);
    final TriConsumer<SyncException, Optional<CustomerDraft>, Optional<Customer>>
        logWarningCallback =
            (exception, newResourceDraft, oldResource) ->
                logWarningCallback(LOGGER, "customer", exception, oldResource);
    final CustomerSyncOptions customerSyncOptions =
        CustomerSyncOptionsBuilder.of(targetClient)
            .errorCallback(logErrorCallback)
            .warningCallback(logWarningCallback)
            .build();

    final CustomerSync customerSync = new CustomerSync(customerSyncOptions);

    final CustomObjectService customObjectService = new CustomObjectServiceImpl(targetClient);

    return new CustomerSyncer(customerSync, sourceClient, targetClient, customObjectService, clock);
  }

  @Nonnull
  @Override
  protected CompletionStage<List<CustomerDraft>> transform(@Nonnull final List<Customer> page) {
    return CompletableFuture.completedFuture(
        CustomerReferenceResolutionUtils.mapToCustomerDrafts(page));
  }

  @Nonnull
  @Override
  protected CustomerQuery getQuery() {
    return CustomerReferenceResolutionUtils.buildCustomerQuery();
  }
}
