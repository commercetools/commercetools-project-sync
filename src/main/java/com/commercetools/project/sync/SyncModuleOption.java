package com.commercetools.project.sync;

import com.commercetools.project.sync.util.SyncUtils;
import com.commercetools.sync.cartdiscounts.CartDiscountSync;
import com.commercetools.sync.categories.CategorySync;
import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.customobjects.CustomObjectSync;
import com.commercetools.sync.inventories.InventorySync;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.producttypes.ProductTypeSync;
import com.commercetools.sync.states.StateSync;
import com.commercetools.sync.taxcategories.TaxCategorySync;
import com.commercetools.sync.types.TypeSync;
import java.util.Arrays;
import java.util.stream.Stream;

public enum SyncModuleOption {
  TYPE_SYNC("types", TypeSync.class),
  PRODUCT_TYPE_SYNC("productTypes", ProductTypeSync.class),
  CART_DISCOUNT_SYNC("cartDiscounts", CartDiscountSync.class),
  CUSTOM_OBJECT_SYNC("customObjects", CustomObjectSync.class),
  CATEGORY_SYNC("categories", CategorySync.class),
  PRODUCT_SYNC("products", ProductSync.class),
  INVENTORY_ENTRY_SYNC("inventoryEntries", InventorySync.class),
  STATE_SYNC("states", StateSync.class),
  TAX_CATEGORY_SYNC("taxCategories", TaxCategorySync.class);

  public final String parameterName;
  private final Class<? extends BaseSync> syncClass;

  SyncModuleOption(String parameterName, Class<? extends BaseSync> syncClass) {
    this.parameterName = parameterName;
    this.syncClass = syncClass;
  }

  public String getParameterName() {
    return parameterName;
  }

  public String getSyncModuleName() {
    return SyncUtils.getSyncModuleName(this.syncClass);
  }

  public static String[] getParameterNames() {
    return Stream.of(SyncModuleOption.values())
        .map(SyncModuleOption::getParameterName)
        .toArray(String[]::new);
  }

  public static SyncModuleOption getSyncModuleOptionByParameterName(String name) {
    return Arrays.stream(SyncModuleOption.values())
        .filter(syncModuleOption -> syncModuleOption.getParameterName().equals(name))
        .findFirst()
        .orElseThrow(IllegalArgumentException::new);
  }
}
