

# commercetools-project-sync
[![Build Status](https://travis-ci.com/commercetools/commercetools-project-sync.svg?branch=master)](https://travis-ci.com/commercetools/commercetools-project-sync)
[![codecov](https://codecov.io/gh/commercetools/commercetools-project-sync/branch/master/graph/badge.svg)](https://codecov.io/gh/commercetools/commercetools-project-sync)
[![Docker Pulls](https://img.shields.io/docker/pulls/commercetools/commercetools-project-sync)](https://hub.docker.com/r/commercetools/commercetools-project-sync)


<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [What is this?](#what-is-this)
- [Prerequisites](#prerequisites)
- [Usage](#usage)
  - [Delta Sync](#delta-sync)
  - [Running Multiple syncers](#running-multiple-syncers)
  - [Running the Docker Image](#running-the-docker-image)
    - [Download](#download)
    - [Run](#run)
- [Examples](#examples)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

### What is this?

A Dockerized CLI application which allows you to automatically sync different resources between two commercetools projects.
As of now, these are the supported resources:

- CartDiscounts
- Categories
- InventoryEntries
- Products
- ProductTypes
- Types
- States
- TaxCategories
- CustomObjects
- Customers
- ShoppingLists


### Prerequisites

 - Make sure you have installed docker to run the docker image.
 - The following fields are **required** to be set on the following resources (and sub-resources), if they will be
  synced:

     |  Resource/ Sub-resource |  Required Fields |
     |---|---|
     | Product | `key` |
     | Product Variant  | `key`, `sku`  |
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
   export SOURCE_AUTH_URL = "https://auth.eu-central-1.aws.commercetools.com" #optional parameter
   export SOURCE_API_URL = "https://api.eu-central-1.aws.commercetools.com" #optional parameter
   export SOURCE_SCOPES = "manage_project" #optional parameter
   
   export TARGET_PROJECT_KEY = "target-project-key"
   export TARGET_CLIENT_ID = "targetClientId"
   export TARGET_CLIENT_SECRET = "targetClientSecret"
   export TARGET_AUTH_URL = "https://auth.eu-central-1.aws.commercetools.com" #optional parameter
   export TARGET_API_URL = "https://api.eu-central-1.aws.commercetools.com" #optional parameter
   export TARGET_SCOPES = "manage_project" #optional parameter
   ```
   Note: For *_AUTH_URL and *_API_URL parameter values,
    you can use different [authentication endpoints](https://docs.commercetools.com/api/authorization#requesting-an-access-token-using-the-composable-commerce-oauth-20-service) and [API endpoints](https://docs.commercetools.com/api/general-concepts#hosts).
   
   Note 2: Project-sync uses `manage_project` [scope](https://docs.commercetools.com/api/scopes) by default.
    if you want to use different scope you might set `SOURCE_SCOPES` and `TARGET_SCOPES` environment variables, for instance:
    `export SOURCE_SCOPES="manage_products"` or `export TARGET_SCOPES="manage_products, manage_customers"`(separate multiple scope elements with a comma).
   
   Note 3: be careful there is no trailing slash in the URLs. Please make sure the URLs do not include `/` as this would result in a wrong URLs like so and fail the process: `https://auth.eu-central-1.gcp.commercetools.com//oauth/token`

### Usage

   ```bash
   usage: commercetools-project-sync
    -f,--full                           By default, a delta sync runs using
                                        last-sync-timestamp logic. Use this
                                        flag to run a full sync. i.e. sync
                                        the entire data set. This option must
                                        be added after `-s` option.
    -h,--help                           Print help information.
    -r,--runnerName <arg>               Choose a name for the running sync
                                        instance. Please make sure the name
                                        is unique, otherwise running more
                                        than 1 sync instance with the same
                                        name would lead to an unexpected
                                        behaviour. This option must
                                        be added after `-s` option. (optional parameter)
                                        default: 'runnerName'.
    -s,--sync <args>                    Choose one or more of the following modules
                                        to run: "types", "productTypes",
                                        "cartDiscounts", "customObjects",
                                        "categories", "products",
                                        "inventoryEntries", "states",
                                        "taxCategories", "customers",
                                        "shoppingLists" or "all".
       --syncProjectSyncCustomObjects   Sync custom objects that were created
                                        with project sync (this application). This option must
                                        be added after `-s` option.
       --productQueryParameters         Pass your customized product fetch limit
                                        and a product projection predicate to filter  
                                        product resources to sync in the JSON format. 
                                        Example: "{\"limit\": 100, \"where\": \"
                                        published=true\"}" could be used to fetch 
                                        only published products to sync and limit 
                                        max 100 elements in one page. This option must
                                        be added after `-s` option.                
    -v,--version                        Print the version of the application.
   ```

#### Delta Sync

By default, running the sync without using `-f` or `--full` option would run a delta sync; which means that only resources
which have been modified since the last time the sync has run would be synced. The application achieves that by
persisting the last sync timestamp on commercetools using `CustomObjects` on every sync run.

The last sync timestamp `customObject` for a runner name `testRun` running a **Type Sync** from a source commercetools project with the key `java-sync-source-dev1` looks as follows:

```javascript
{
  "id": "0ee39da2-21fd-46b4-9f99-44eae7f249a1",
  "version": 2,
  "container": "commercetools-project-sync.testRun.typeSync",
  "key": "java-sync-source-dev1",
  "value": {
    "lastSyncDurationInMillis": 972,
    "applicationVersion": "3.1.0",
    "lastSyncTimestamp": "2019-05-24T11:17:00.602Z",
    "lastSyncStatistics": {
      "processed": 0,
      "failed": 0,
      "created": 0,
      "updated": 0,
      "reportMessage": "Summary: 0 types were processed in total (0 created, 0 updated and 0 failed to sync)."
    }
  },
  "createdAt": "2019-05-24T11:18:12.831Z",
  "lastModifiedAt": "2019-05-24T11:19:01.822Z",
  "lastModifiedBy": {
    "clientId": "8bV3XSW-taCph843-GQTa8lf",
    "isPlatformClient": false
  },
  "createdBy": {
    "clientId": "8bV3XSW-taCph843-GQTa8lf",
    "isPlatformClient": false
  }
}
```

- The `container` has the convention: `commercetools-project-sync.{runnerName}.{syncModuleName}`.
- The `key` contains the source project key.
- The `value` contains the information  `lastSyncDurationInMillis`, `applicationVersion`, `lastSyncTimestamp` and `lastSyncStatistics`.
- These custom objects will not be synced with the custom object syncer unless the option --syncProjectSyncCustomObjects is added.

_Note:_ Another `customObject` with the `container` convention `commercetools-project-sync.{runnerName}.{syncModuleName}.timestampGenerator` is also created on the target project for capturing a unified timestamp from commercetools.

Running a **Full sync** using `-f` or `--full` option will not create any `customObjects`.

#### Running Multiple Syncers

The application can sync multiple resources. For example, to run `type` and `productType` sync together, 
the  `-s` option with `types productTypes` as below:
```bash
-s types productTypes
```

#### Running ProductSync with custom product query parameters

You might pass your customized product fetch limit, and a product projection predicate to filter product resources to sync in the JSON format.

For instance:

```bash
-s products -productQueryParameters "{\"limit\": 100, \"where\": \"published=true AND masterVariant(attributes(name=\\\"attribute-name\\\" and value=\\\"attribute-value\\\"))\"}"
```

Predicates provide a way for complex filter expressions when querying resources. Refer commercetools docs for more details.

Note: The value of the productQueryParameters argument should be in JSON format and as shown in the above example, please use escape character \ for the nested double quote values.
Example: 
```bash
-s products -productQueryParameters "{\"limit\": 100, \"where\": \"published=true AND masterVariant(key= \\\"variantKey\\\")\"}"
```

#### Running the Docker Image

##### Download

```bash
docker pull commercetools/commercetools-project-sync:5.3.1
```
##### Run

```bash
docker run \
-e SOURCE_PROJECT_KEY=xxxx \
-e SOURCE_CLIENT_ID=xxxx \
-e SOURCE_CLIENT_SECRET=xxxx \
-e TARGET_PROJECT_KEY=xxxx \
-e TARGET_CLIENT_ID=xxxx \
-e TARGET_CLIENT_SECRET=xxxx \
commercetools/commercetools-project-sync:5.3.1 -s all
```


### Examples
 - To run the all sync modules from a source project to a target project
   ```bash
   docker run commercetools/commercetools-project-sync:5.3.1 -s all
   ```
   This will run the following sync modules in the given order:
 1. `Type` Sync and `ProductType` Sync and `States` Sync and `TaxCategory` Sync and `CustomObject` Sync in parallel.
 2. `Category` Sync and `InventoryEntry` Sync and `CartDiscount` Sync and `Customer` Sync in parallel.
 3. `Product` Sync.
 4. `ShoppingList` Sync.

 - To run the type sync
   ```bash
   docker run commercetools/commercetools-project-sync:5.3.1 -s types
   ```

 - To run the productType sync
   ```bash
   docker run commercetools/commercetools-project-sync:5.3.1 -s productTypes
   ```

- To run the states sync
   ```bash
   docker run commercetools/commercetools-project-sync:5.3.1 -s states
   ```
- To run the taxCategory sync
   ```bash
   docker run commercetools/commercetools-project-sync:5.3.1 -s taxCategories
   ```

- To run the category sync
   ```bash
   docker run commercetools/commercetools-project-sync:5.3.1 -s categories
   ```

- To run the product sync
   ```bash
   docker run commercetools/commercetools-project-sync:5.3.1 -s products
   ```

- To run the cartDiscount sync
   ```bash
   docker run commercetools/commercetools-project-sync:5.3.1 -s cartDiscounts
   ```

- To run the inventoryEntry sync
   ```bash
   docker run commercetools/commercetools-project-sync:5.3.1 -s inventoryEntries
   ```

- To run the customObject sync
   ```bash
   docker run commercetools/commercetools-project-sync:5.3.1 -s customObjects
   ```

- To run the customer sync
   ```bash
   docker run commercetools/commercetools-project-sync:5.3.1 -s customers
   ```
  
- To run the shoppingList sync
   ```bash
   docker run commercetools/commercetools-project-sync:5.3.1 -s shoppingLists
   ```
- To run both products and shoppingList sync
   ```bash
   docker run commercetools/commercetools-project-sync:5.3.1 -s products shoppingLists
   ```
  
- To run type, productType and shoppingList sync
   ```bash
   docker run commercetools/commercetools-project-sync:5.3.1 -s types productTypes shoppingLists
   ```

- To run all sync modules using a runner name
   ```bash
   docker run commercetools/commercetools-project-sync:5.3.1 -s all -r myRunnerName
   ```
