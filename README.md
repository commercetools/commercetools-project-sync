# commercetools-project-sync
[![Build Status](https://travis-ci.org/commercetools/commercetools-project-sync.svg?branch=master)](https://travis-ci.org/commercetools/commercetools-project-sync)
[![codecov](https://codecov.io/gh/commercetools/commercetools-project-sync/branch/master/graph/badge.svg)](https://codecov.io/gh/commercetools/commercetools-project-sync)

Dockerized CLI application which allows to automatically sync different resources between commercetools projects

- [Usage](#usage)
  - [Prerequisites](#prerequisites)
  - [Run the application](#run-the-application)
## Usage
### Prerequisites
 
 - install Java 8
 - keys are **required** to be set on the resources that should be synced.
 - set the following environment variables before running the application
   ```bash
   export SOURCE_PROJECT_KEY = xxxxxxxxxxxxx
   export SOURCE_CLIENT_ID = xxxxxxxxxxxxxxx
   export SOURCE_CLIENT_SECRET = xxxxxxxxxxx
   export TARGET_PROJECT_KEY = xxxxxxxxxxxxx
   export TARGET_CLIENT_ID = xxxxxxxxxxxxxxx
   export TARGET_CLIENT_SECRET = xxxxxxxxxxx
   ```
   
### Run the application   
 - First, package the JAR
   ```bash
   ./gradlew clean shadowJar
   ```
 - To run the productType sync
   ```bash
   java -jar build/libs/commercetools-sync.jar -s productTypes
   ```  
    
- To run the category sync
   ```bash
   java -jar build/libs/commercetools-sync.jar -s categories
   ```  
   
- To run the product sync
   ```bash
   java -jar build/libs/commercetools-sync.jar -s products
   ```  
    
- To run the inventoryEntry sync
   ```bash
   java -jar build/libs/commercetools-sync.jar -s inventoryEntries
   ```     
   
- Usage
  ```bash
  usage: commercetools-sync
   -h,--help         Print help information to System.out.
   -s,--sync <arg>   Choose which sync module to run: "productTypes",
                      "categories", "products" or "inventoryEntries".
   -v,--version      Print the version of the application.
  ```
