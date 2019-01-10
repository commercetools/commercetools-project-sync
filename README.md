# commercetools-project-sync
[![Build Status](https://travis-ci.org/commercetools/commercetools-project-sync.svg?branch=master)](https://travis-ci.org/commercetools/commercetools-project-sync)
[![codecov](https://codecov.io/gh/commercetools/commercetools-project-sync/branch/master/graph/badge.svg)](https://codecov.io/gh/commercetools/commercetools-project-sync)

Dockerized CLI application which allows to automatically sync different resources between commercetools projects

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [Prerequisites](#prerequisites)
- [Usage](#usage)
- [Logging](#logging)
    - [Printing to `Standard Out`](#printing-to-standard-out)
    - [Logging](#logging-1)
- [Examples](#examples)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->


### Prerequisites
 
 - Make sure you have `JDK 8` installed.
 - The following fields are **required** to be set on the following resources (and sub-resources) that should be synced:
    - `Product`
        - `key`
        - `Variant`
            - `key`
            - `Asset`
                - `key`
    - `ProductType`
        - `key`
    - `Category`
        - `key`         
 
 - Set the following environment variables before running the application
   ```bash
   export SOURCE_PROJECT_KEY = xxxxxxxxxxxxx
   export SOURCE_CLIENT_ID = xxxxxxxxxxxxxxx
   export SOURCE_CLIENT_SECRET = xxxxxxxxxxx
   export TARGET_PROJECT_KEY = xxxxxxxxxxxxx
   export TARGET_CLIENT_ID = xxxxxxxxxxxxxxx
   export TARGET_CLIENT_SECRET = xxxxxxxxxxx
   ```

### Usage

   ```bash
   usage: commercetools-project-sync
    -h,--help         Print help information.
    -s,--sync <arg>   Choose one of the following modules to run: "types", "productTypes", "categories", "products", "inventoryEntries" or "all" (will run all the modules).
    -v,--version      Print the version of the application.
   ```
   
### Logging

##### Printing to `Standard Out`
The CLI application will only print to standard out on the occurrence of 2 events:

- On successful completion of each sync process. 

    For example, on the completion of the product sync, the CLI will output this to standard out:
    ```
    Syncing products from CTP project with key 'foo' to project with key 'bar' is done.
    ```
- On errors whether on supplying options to the CLI or errors that could terminate a sync process.

##### Logging
The CLI will log the following events:

- The previously mentioned events (printed to standard out) with severity `INFO` and `ERROR` respectively.
- Before starting a sync process 
- The statistics of a sync process once it's done.
- The events triggered by an `errorCallback` or a `warningCallback` of a sync process.
      

### Examples   
 - To run the all sync modules from a source project to a target project
   ```bash
   java -jar build/libs/commercetools-project-sync.jar -s all
   ```
   This will run the following sync modules in the given order:
 1. `Type` Sync and `ProductType` Sync in parallel.
 2. `Category` Sync.
 3. `Product` Sync.
 4. `InventoryEntry` Sync.

 - To run the type sync
   ```bash
   java -jar build/libs/commercetools-project-sync.jar -s types
   ```  

 - To run the productType sync
   ```bash
   java -jar build/libs/commercetools-project-sync.jar -s productTypes
   ```  
    
- To run the category sync
   ```bash
   java -jar build/libs/commercetools-project-sync.jar -s categories
   ```  
   
- To run the product sync
   ```bash
   java -jar build/libs/commercetools-project-sync.jar -s products
   ```  
    
- To run the inventoryEntry sync
   ```bash
   java -jar build/libs/commercetools-project-sync.jar -s inventoryEntries
   ```     
   

