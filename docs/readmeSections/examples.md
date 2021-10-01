### Examples
 - To run the all sync modules from a source project to a target project
   ```bash
   docker run commercetools/commercetools-project-sync:4.0.10 -s all
   ```
   This will run the following sync modules in the given order:
 1. `Type` Sync and `ProductType` Sync and `States` Sync and `TaxCategory` Sync and `CustomObject` Sync in parallel.
 2. `Category` Sync and `InventoryEntry` Sync and `CartDiscount` Sync and `Customer` Sync in parallel.
 3. `Product` Sync.
 4. `ShoppingList` Sync.

 - To run the type sync
   ```bash
   docker run commercetools/commercetools-project-sync:4.0.10 -s types
   ```

 - To run the productType sync
   ```bash
   docker run commercetools/commercetools-project-sync:4.0.10 -s productTypes
   ```

- To run the states sync
   ```bash
   docker run commercetools/commercetools-project-sync:4.0.10 -s states
   ```
- To run the taxCategory sync
   ```bash
   docker run commercetools/commercetools-project-sync:4.0.10 -s taxCategories
   ```

- To run the category sync
   ```bash
   docker run commercetools/commercetools-project-sync:4.0.10 -s categories
   ```

- To run the product sync
   ```bash
   docker run commercetools/commercetools-project-sync:4.0.10 -s products
   ```

- To run the cartDiscount sync
   ```bash
   docker run commercetools/commercetools-project-sync:4.0.10 -s cartDiscounts
   ```

- To run the inventoryEntry sync
   ```bash
   docker run commercetools/commercetools-project-sync:4.0.10 -s inventoryEntries
   ```

- To run the customObject sync
   ```bash
   docker run commercetools/commercetools-project-sync:4.0.10 -s customObjects
   ```

- To run the customer sync
   ```bash
   docker run commercetools/commercetools-project-sync:4.0.10 -s customers
   ```
  
- To run the shoppingList sync
   ```bash
   docker run commercetools/commercetools-project-sync:4.0.10 -s shoppingLists
   ```
- To run both products and shoppingList sync
   ```bash
   docker run commercetools/commercetools-project-sync:4.0.10 -s products shoppingLists
   ```
  
- To run type, productType and shoppingList sync
   ```bash
   docker run commercetools/commercetools-project-sync:4.0.10 -s types productTypes shoppingLists
   ```

- To run all sync modules using a runner name
   ```bash
   docker run commercetools/commercetools-project-sync:4.0.10 -s all -r myRunnerName
   ```
