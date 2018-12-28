package com.commercetools.project.sync;

import com.commercetools.project.sync.category.CategorySyncer;
import com.commercetools.project.sync.inventoryentry.InventoryEntrySyncer;
import com.commercetools.project.sync.product.ProductSyncer;
import com.commercetools.project.sync.producttype.ProductTypeSyncer;
import com.commercetools.project.sync.type.TypeSyncer;
import io.sphere.sdk.client.SphereClient;
import org.junit.jupiter.api.Test;

import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_CATEGORY_SYNC;
import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_INVENTORY_ENTRY_SYNC;
import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_PRODUCT_SYNC;
import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_PRODUCT_TYPE_SYNC;
import static com.commercetools.project.sync.CliRunner.SYNC_MODULE_OPTION_TYPE_SYNC;
import static com.commercetools.project.sync.SyncerFactory.AVAILABLE_OPTIONS;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

 class SyncerFactoryTest {
  @Test
   void getSyncer_WithNullOptionValue_ShouldThrowIllegalArgumentException() {
    assertThatThrownBy(
            () ->
                SyncerFactory.of(mock(SphereClient.class), mock(SphereClient.class))
                    .buildSyncer(null))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(
            format(
                "Blank argument supplied to \"-s\" or \"--sync\" option! %s", AVAILABLE_OPTIONS));
  }

  @Test
   void getSyncer_WithEmptyOptionValue_ShouldThrowIllegalArgumentException() {
    assertThatThrownBy(
            () ->
                SyncerFactory.of(mock(SphereClient.class), mock(SphereClient.class))
                    .buildSyncer(null))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(
            format(
                "Blank argument supplied to \"-s\" or \"--sync\" option! %s", AVAILABLE_OPTIONS));
  }

  @Test
   void getSyncer_WithUnknownOptionValue_ShouldThrowIllegalArgumentException() {
    final String unknownOptionValue = "anyOption";
    assertThatThrownBy(
            () ->
                SyncerFactory.of(mock(SphereClient.class), mock(SphereClient.class))
                    .buildSyncer(unknownOptionValue))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(
            format(
                "Unknown argument \"%s\" supplied to \"-s\" or \"--sync\" option! %s",
                unknownOptionValue, AVAILABLE_OPTIONS));
  }

  @Test
   void getSyncer_WithValidOptionValue_ShouldReturnCorrectSyncer() {
    assertThat(
            SyncerFactory.of(mock(SphereClient.class), mock(SphereClient.class))
                .buildSyncer(SYNC_MODULE_OPTION_CATEGORY_SYNC))
        .isExactlyInstanceOf(CategorySyncer.class);

    assertThat(
            SyncerFactory.of(mock(SphereClient.class), mock(SphereClient.class))
                .buildSyncer(SYNC_MODULE_OPTION_PRODUCT_SYNC))
        .isExactlyInstanceOf(ProductSyncer.class);

    assertThat(
            SyncerFactory.of(mock(SphereClient.class), mock(SphereClient.class))
                .buildSyncer(SYNC_MODULE_OPTION_INVENTORY_ENTRY_SYNC))
        .isExactlyInstanceOf(InventoryEntrySyncer.class);

    assertThat(
            SyncerFactory.of(mock(SphereClient.class), mock(SphereClient.class))
                .buildSyncer(SYNC_MODULE_OPTION_TYPE_SYNC))
        .isExactlyInstanceOf(TypeSyncer.class);

    assertThat(
            SyncerFactory.of(mock(SphereClient.class), mock(SphereClient.class))
                .buildSyncer(SYNC_MODULE_OPTION_PRODUCT_TYPE_SYNC))
        .isExactlyInstanceOf(ProductTypeSyncer.class);
  }
}
