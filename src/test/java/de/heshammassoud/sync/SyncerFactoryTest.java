package com.commercetools.sync;

import static com.commercetools.sync.CliRunner.SYNC_MODULE_OPTION_CATEGORY_SYNC;
import static com.commercetools.sync.CliRunner.SYNC_MODULE_OPTION_PRODUCT_SYNC;
import static java.lang.String.format;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.commercetools.sync.category.CategorySyncer;
import com.commercetools.sync.product.ProductSyncer;
import org.junit.Test;

public class SyncerFactoryTest {
    @Test
    public void getSyncer_WithNullOptionValue_ShouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> SyncerFactory.getSyncer(null))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "Blank argument supplied to \"-s\" or \"--sync\" option! Please choose either"
                                + " \"productTypes\" or \"categories\" or \"products\" or \"inventoryEntries\".");
    }

    @Test
    public void getSyncer_WithEmptyOptionValue_ShouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> SyncerFactory.getSyncer(""))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "Blank argument supplied to \"-s\" or \"--sync\" option! Please choose either"
                                + " \"productTypes\" or \"categories\" or \"products\" or \"inventoryEntries\".");
    }

    @Test
    public void getSyncer_WithUnknownOptionValue_ShouldThrowIllegalArgumentException() {
        final String unknownOptionValue = "anyOption";
        assertThatThrownBy(() -> SyncerFactory.getSyncer(unknownOptionValue))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        format(
                                "Unknown argument \"%s\" supplied to \"-s\" or \"--sync\" option! Please "
                                        + "choose either \"productTypes\" or \"categories\" or \"products\" or \"inventoryEntries\".",
                                unknownOptionValue));
    }

    @Test
    public void getSyncer_WithValidOptionValue_ShouldReturnCorrectSyncer() {
        assertThat(SyncerFactory.getSyncer(SYNC_MODULE_OPTION_CATEGORY_SYNC))
                .isExactlyInstanceOf(CategorySyncer.class);
        assertThat(SyncerFactory.getSyncer(SYNC_MODULE_OPTION_PRODUCT_SYNC))
                .isExactlyInstanceOf(ProductSyncer.class);
    }
}
