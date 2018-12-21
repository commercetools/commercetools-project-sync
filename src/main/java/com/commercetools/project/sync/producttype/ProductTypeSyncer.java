package com.commercetools.project.sync.producttype;

import static com.commercetools.project.sync.util.SphereClientUtils.CTP_TARGET_CLIENT;

import com.commercetools.project.sync.Syncer;
import com.commercetools.sync.producttypes.ProductTypeSync;
import com.commercetools.sync.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.producttypes.ProductTypeSyncOptionsBuilder;
import com.commercetools.sync.producttypes.helpers.ProductTypeSyncStatistics;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProductTypeSyncer
    extends Syncer<
        ProductType,
        ProductTypeDraft,
        ProductTypeSyncStatistics,
        ProductTypeSyncOptions,
        ProductTypeQuery,
        ProductTypeSync> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProductTypeSyncer.class);

  /** Instantiates a {@link Syncer} instance. */
  private ProductTypeSyncer(
      @Nonnull final ProductTypeSync productTypeSync, @Nonnull final ProductTypeQuery query) {
    super(productTypeSync, query);
  }

  public static ProductTypeSyncer of() {
    final ProductTypeSyncOptions syncOptions =
        ProductTypeSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(LOGGER::error)
            .warningCallback(LOGGER::warn)
            .build();

    final ProductTypeSync productTypeSync = new ProductTypeSync(syncOptions);

    return new ProductTypeSyncer(productTypeSync, ProductTypeQuery.of());
  }

  @Nonnull
  @Override
  protected List<ProductTypeDraft> getDraftsFromPage(@Nonnull final List<ProductType> page) {
    return page.stream()
        .map(productType -> ProductTypeDraftBuilder.of(productType).build())
        .collect(Collectors.toList());
  }
}
