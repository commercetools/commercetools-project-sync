package com.commercetools.sync;

import com.commercetools.sync.category.CategorySyncer;
import com.commercetools.sync.inventoryentry.InventoryEntrySyncer;
import com.commercetools.sync.product.ProductSyncer;
import com.commercetools.sync.producttype.ProductTypeSyncer;
import com.commercetools.sync.type.TypeSyncer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.commercetools.sync.CliRunner.SYNC_MODULE_OPTION_CATEGORY_SYNC;
import static com.commercetools.sync.CliRunner.SYNC_MODULE_OPTION_INVENTORY_ENTRY_SYNC;
import static com.commercetools.sync.CliRunner.SYNC_MODULE_OPTION_LONG;
import static com.commercetools.sync.CliRunner.SYNC_MODULE_OPTION_PRODUCT_SYNC;
import static com.commercetools.sync.CliRunner.SYNC_MODULE_OPTION_PRODUCT_TYPE_SYNC;
import static com.commercetools.sync.CliRunner.SYNC_MODULE_OPTION_SHORT;
import static com.commercetools.sync.CliRunner.SYNC_MODULE_OPTION_TYPE_SYNC;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

public final class SyncerFactory {

  public static final String AVAILABLE_OPTIONS =
      format(
          "Please use any of the following options: \"%s\", \"%s\", \"%s\", \"%s\", \"%s\".",
          SYNC_MODULE_OPTION_TYPE_SYNC,
          SYNC_MODULE_OPTION_PRODUCT_TYPE_SYNC,
          SYNC_MODULE_OPTION_CATEGORY_SYNC,
          SYNC_MODULE_OPTION_PRODUCT_SYNC,
          SYNC_MODULE_OPTION_INVENTORY_ENTRY_SYNC);

  private SyncerFactory() {}

  /**
   * Builds an instance of {@link Syncer} corresponding to the passed option value. Acceptable
   * values are either "products" or "productTypes" or "categories" or "inventoryEntries" or
   * "types". Other cases, would cause an {@link IllegalArgumentException} to be thrown.
   *
   * @param syncOptionValue the string value passed to the sync option. Acceptable values are either
   *     "products" or "productTypes" or "categories" or "inventoryEntries" or "types". Other cases,
   *     would cause an {@link IllegalArgumentException} to be thrown.
   * @return The instance of the syncer corresponding to the passed option value.
   * @throws IllegalArgumentException if a wrong option value is passed to the sync option. (Wrong
   *     values are anything other than "types" or "products" or "categories" or "productTypes" or
   *     "inventoryEntries".
   */
  @Nonnull
  static Syncer getSyncer(@Nullable final String syncOptionValue) {
    if (isBlank(syncOptionValue)) {
      final String errorMessage =
          format(
              "Blank argument supplied to \"-%s\" or \"--%s\" option! %s",
              SYNC_MODULE_OPTION_SHORT,
              SYNC_MODULE_OPTION_LONG,
              AVAILABLE_OPTIONS);
      throw new IllegalArgumentException(errorMessage);
    }

    final String trimmedValue = syncOptionValue.trim();
    switch (trimmedValue) {
      case SYNC_MODULE_OPTION_PRODUCT_TYPE_SYNC:
        return new ProductTypeSyncer();
      case SYNC_MODULE_OPTION_CATEGORY_SYNC:
        return new CategorySyncer();
      case SYNC_MODULE_OPTION_PRODUCT_SYNC:
        return new ProductSyncer();
      case SYNC_MODULE_OPTION_INVENTORY_ENTRY_SYNC:
        return new InventoryEntrySyncer();
      case SYNC_MODULE_OPTION_TYPE_SYNC:
        return new TypeSyncer();
      default:
        final String errorMessage =
            format(
                "Unknown argument \"%s\" supplied to \"-%s\" or \"--%s\" option! %s",
                syncOptionValue,
                SYNC_MODULE_OPTION_SHORT,
                SYNC_MODULE_OPTION_LONG,
                AVAILABLE_OPTIONS);
        throw new IllegalArgumentException(errorMessage);
    }
  }
}
