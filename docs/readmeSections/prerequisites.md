### Prerequisites

 - Make sure you have installed docker to run the docker image.
 - The following fields are **required** to be set on the following resources (and sub-resources), if they will be
  synced:

     |  Resource/ Sub-resource |  Required Fields |
     |---|---|
     | Product | `key`, `sku` |
     | Product Variant  | `key`  |
     | Product Variant Asset (if exists) | `key`  |
     | ProductType  | `key`  |
     | Type  | `key`  |
     | Category  | `key`  |
     | Category Asset (if exists)  | `key`  |
     | CartDiscount | `key`  |
     | InventoryEntry  | `sku`  |
     | State  | `key`  |
     | TaxCategory  | `key`  |
     | CustomObject  | `container` AND `key`  |
     | Customer  | `key`  |
     | Customer Address | `key` |
     | ShoppingList | `key` |
     | ShoppingList LineItem - Product Variant | `sku` |
     | ShoppingList TextLineItem | `name` |

 - Set the following environment variables before running the application
   ```bash
   export SOURCE_PROJECT_KEY = "source-project-key"
   export SOURCE_CLIENT_ID = "sourceClientId"
   export SOURCE_CLIENT_SECRET = "sourceClientSecret"
   export SOURCE_AUTH_URL = "https://auth.eu-central-1.aws.commercetools.com/" #optional parameter
   export SOURCE_API_URL = "https://api.eu-central-1.aws.commercetools.com/" #optional parameter
   export SOURCE_SCOPES = "manage_project" #optional parameter
   
   export TARGET_PROJECT_KEY = "target-project-key"
   export TARGET_CLIENT_ID = "targetClientId"
   export TARGET_CLIENT_SECRET = "targetClientSecret"
   export TARGET_AUTH_URL = "https://auth.eu-central-1.aws.commercetools.com/" #optional parameter
   export TARGET_API_URL = "https://api.eu-central-1.aws.commercetools.com/" #optional parameter
   export TARGET_SCOPES = "manage_project" #optional parameter
   ```
   Note: For *_AUTH_URL and *_API_URL parameter values,
    you can use different [authentication endpoints](https://docs.commercetools.com/http-api-authorization#requesting-an-access-token-using-commercetools-oauth2-server) and [API endpoints](https://docs.commercetools.com/http-api#hosts).
   
   Note 2: Project-sync uses `manage_project` [scope](https://docs.commercetools.com/api/scopes) by default.
    if you want to use different scope you might set `SOURCE_SCOPES` and `TARGET_SCOPES` environment variables, for instance:
    `export SOURCE_SCOPES="manage_products"` or `export TARGET_SCOPES="manage_products, manage_customers"`(separate multiple scope elements with a comma).