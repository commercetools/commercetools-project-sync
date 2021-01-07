package com.commercetools.project.sync;

import com.commercetools.project.sync.util.SyncUtils;
import com.commercetools.sync.cartdiscounts.CartDiscountSync;
import com.commercetools.sync.categories.CategorySync;
import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.customers.CustomerSync;
import com.commercetools.sync.customobjects.CustomObjectSync;
import com.commercetools.sync.inventories.InventorySync;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.producttypes.ProductTypeSync;
import com.commercetools.sync.shoppinglists.ShoppingListSync;
import com.commercetools.sync.states.StateSync;
import com.commercetools.sync.taxcategories.TaxCategorySync;
import com.commercetools.sync.types.TypeSync;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public enum SyncModuleOption {
  TYPE_SYNC("types", TypeSync.class, Collections.EMPTY_LIST),
  PRODUCT_TYPE_SYNC("productTypes", ProductTypeSync.class, Collections.EMPTY_LIST),
  CART_DISCOUNT_SYNC("cartDiscounts", CartDiscountSync.class, Collections.singletonList(TYPE_SYNC)),
  CUSTOM_OBJECT_SYNC("customObjects", CustomObjectSync.class, Collections.emptyList()),
  CATEGORY_SYNC("categories", CategorySync.class, Collections.singletonList(TYPE_SYNC)),
  INVENTORY_ENTRY_SYNC(
      "inventoryEntries", InventorySync.class, Collections.singletonList(TYPE_SYNC)),
  STATE_SYNC("states", StateSync.class, Collections.emptyList()),
  TAX_CATEGORY_SYNC("taxCategories", TaxCategorySync.class, Collections.emptyList()),
  CUSTOMER_SYNC("customers", CustomerSync.class, Collections.singletonList(TYPE_SYNC)),
  PRODUCT_SYNC(
      "products",
      ProductSync.class,
      Arrays.asList(TYPE_SYNC, PRODUCT_TYPE_SYNC, STATE_SYNC, CATEGORY_SYNC, TAX_CATEGORY_SYNC)),
  SHOPPING_LIST_SYNC(
      "shoppingLists",
      ShoppingListSync.class,
      Arrays.asList(TYPE_SYNC, CUSTOMER_SYNC, PRODUCT_SYNC));

  public final String syncOptionValue;
  private final Class<? extends BaseSync> syncClass;
  private final List<SyncModuleOption> essentialSyncOptions;

  SyncModuleOption(
      String syncOptionValue,
      Class<? extends BaseSync> syncClass,
      List<SyncModuleOption> essentialSyncOptions) {
    this.syncOptionValue = syncOptionValue;
    this.syncClass = syncClass;
    this.essentialSyncOptions = essentialSyncOptions;
  }

  public String getSyncOptionValue() {
    return syncOptionValue;
  }

  public String getSyncModuleName() {
    return SyncUtils.getSyncModuleName(this.syncClass);
  }

  public static String[] getSyncOptionValues() {
    return Stream.of(SyncModuleOption.values())
        .map(SyncModuleOption::getSyncOptionValue)
        .toArray(String[]::new);
  }

  public static SyncModuleOption getSyncModuleOptionBySyncOptionValue(String syncOptionValue) {
    return Arrays.stream(SyncModuleOption.values())
        .filter(syncModuleOption -> syncModuleOption.getSyncOptionValue().equals(syncOptionValue))
        .findFirst()
        .orElseThrow(IllegalArgumentException::new);
  }

  public List<SyncModuleOption> getEssentialSyncOptions() {
    return essentialSyncOptions;
  }
}
