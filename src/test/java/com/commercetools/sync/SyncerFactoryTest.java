package com.commercetools.sync;

import static com.commercetools.sync.CliRunner.SYNC_MODULE_OPTION_CATEGORY_SYNC;
import static com.commercetools.sync.CliRunner.SYNC_MODULE_OPTION_INVENTORY_ENTRY_SYNC;
import static com.commercetools.sync.CliRunner.SYNC_MODULE_OPTION_PRODUCT_SYNC;
import static com.commercetools.sync.CliRunner.SYNC_MODULE_OPTION_PRODUCT_TYPE_SYNC;
import static com.commercetools.sync.CliRunner.SYNC_MODULE_OPTION_TYPE_SYNC;
import static com.commercetools.sync.SyncerFactory.AVAILABLE_OPTIONS;
import static java.lang.String.format;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.commercetools.sync.category.CategorySyncer;
import com.commercetools.sync.inventoryentry.InventoryEntrySyncer;
import com.commercetools.sync.product.ProductSyncer;
import com.commercetools.sync.producttype.ProductTypeSyncer;
import com.commercetools.sync.type.TypeSyncer;
import org.junit.Test;

public class SyncerFactoryTest {
  @Test
  public void getSyncer_WithNullOptionValue_ShouldThrowIllegalArgumentException() {
    assertThatThrownBy(() -> SyncerFactory.getSyncer(null))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(
            format(
                "Blank argument supplied to \"-s\" or \"--sync\" option! %s", AVAILABLE_OPTIONS));
  }

  @Test
  public void getSyncer_WithEmptyOptionValue_ShouldThrowIllegalArgumentException() {
    assertThatThrownBy(() -> SyncerFactory.getSyncer(""))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(
            format(
                "Blank argument supplied to \"-s\" or \"--sync\" option! %s", AVAILABLE_OPTIONS));
  }

  @Test
  public void getSyncer_WithUnknownOptionValue_ShouldThrowIllegalArgumentException() {
    final String unknownOptionValue = "anyOption";
    assertThatThrownBy(() -> SyncerFactory.getSyncer(unknownOptionValue))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(
            format(
                "Unknown argument \"%s\" supplied to \"-s\" or \"--sync\" option! %s",
                unknownOptionValue, AVAILABLE_OPTIONS));
  }

  @Test
  public void getSyncer_WithValidOptionValue_ShouldReturnCorrectSyncer() {
    assertThat(SyncerFactory.getSyncer(SYNC_MODULE_OPTION_CATEGORY_SYNC))
        .isExactlyInstanceOf(CategorySyncer.class);

    assertThat(SyncerFactory.getSyncer(SYNC_MODULE_OPTION_PRODUCT_SYNC))
        .isExactlyInstanceOf(ProductSyncer.class);

    assertThat(SyncerFactory.getSyncer(SYNC_MODULE_OPTION_INVENTORY_ENTRY_SYNC))
        .isExactlyInstanceOf(InventoryEntrySyncer.class);

    assertThat(SyncerFactory.getSyncer(SYNC_MODULE_OPTION_TYPE_SYNC))
        .isExactlyInstanceOf(TypeSyncer.class);

    assertThat(SyncerFactory.getSyncer(SYNC_MODULE_OPTION_PRODUCT_TYPE_SYNC))
        .isExactlyInstanceOf(ProductTypeSyncer.class);
  }
}
