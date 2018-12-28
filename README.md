# commercetools-project-sync
[![Build Status](https://travis-ci.org/commercetools/commercetools-project-sync.svg?branch=master)](https://travis-ci.org/commercetools/commercetools-project-sync)
[![codecov](https://codecov.io/gh/commercetools/commercetools-project-sync/branch/master/graph/badge.svg)](https://codecov.io/gh/commercetools/commercetools-project-sync)

Dockerized CLI application which allows to automatically sync different resources between commercetools projects

<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->

- [Prerequisites](#prerequisites)
- [Usage](#usage)
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

### Examples   
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
   

