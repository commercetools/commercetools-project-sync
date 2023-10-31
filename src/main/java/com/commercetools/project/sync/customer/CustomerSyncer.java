package com.commercetools.project.sync.customer;

import static com.commercetools.project.sync.util.SyncUtils.logErrorCallback;
import static com.commercetools.project.sync.util.SyncUtils.logWarningCallback;
import static com.commercetools.sync.customers.utils.CustomerTransformUtils.toCustomerDrafts;

import com.commercetools.api.client.ByProjectKeyCustomersGet;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerDraft;
import com.commercetools.api.models.customer.CustomerPagedQueryResponse;
import com.commercetools.api.models.customer.CustomerUpdateAction;
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
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CustomerSyncer
    extends Syncer<
        Customer,
        CustomerUpdateAction,
        CustomerDraft,
        CustomerSyncStatistics,
        CustomerSyncOptions,
        ByProjectKeyCustomersGet,
        CustomerPagedQueryResponse,
        CustomerSync> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CustomerSyncer.class);

  private CustomerSyncer(
      @Nonnull final CustomerSync customerSync,
      @Nonnull final ProjectApiRoot sourceClient,
      @Nonnull final ProjectApiRoot targetClient,
      @Nonnull final CustomObjectService customObjectService,
      @Nonnull final Clock clock) {
    super(customerSync, sourceClient, targetClient, customObjectService, clock);
  }

  public static CustomerSyncer of(
      @Nonnull final ProjectApiRoot sourceClient,
      @Nonnull final ProjectApiRoot targetClient,
      @Nonnull final Clock clock) {
    final QuadConsumer<
            SyncException, Optional<CustomerDraft>, Optional<Customer>, List<CustomerUpdateAction>>
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
    return toCustomerDrafts(getSourceClient(), referenceIdToKeyCache, page);
  }

  @Nonnull
  @Override
  protected ByProjectKeyCustomersGet getQuery() {
    return getSourceClient().customers().get();
  }

  @Nonnull
  @Override
  protected Logger getLoggerInstance() {
    return LOGGER;
  }
}
