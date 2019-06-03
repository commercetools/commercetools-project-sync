# commercetools-project-sync
[![Build Status](https://travis-ci.org/commercetools/commercetools-project-sync.svg?branch=master)](https://travis-ci.org/commercetools/commercetools-project-sync)
[![codecov](https://codecov.io/gh/commercetools/commercetools-project-sync/branch/master/graph/badge.svg)](https://codecov.io/gh/commercetools/commercetools-project-sync)

Dockerized CLI application which allows to automatically sync different resources between commercetools projects

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [Prerequisites](#prerequisites)
- [Usage](#usage)
  - [Running the Docker Image](#running-the-docker-image)
    - [Download](#download)
    - [Run](#run)
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
    -h,--help               Print help information.
    -s,--sync <arg>         Choose one of the following modules to run: "types", "productTypes", "categories", 
                            "products", "inventoryEntries" or "all" (will run all the modules).
    -r,--runnerName <arg>   name for the running sync instance. Please make sure the name is unique, otherwise running 
                            more than 1 sync instance with the same name would lead to an unexpected behaviour. 
                            (optional parameter) default: 'runnerName'.
    -f,--full               By default, delta sync runs using last sync timestamp logic. Use this flag to run a full
                            sync. i.e. sync the entire data set.               
    -v,--version            Print the version of the application.
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
    "applicationVersion": "1.1.0",
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
    "clientId": "8bV3XSW-taCpi873-GQTa8lf",
    "isPlatformClient": false
  },
  "createdBy": {
    "clientId": "8bV3XSW-taCpi873-GQTa8lf",
    "isPlatformClient": false
  }
}
```

- The `container` has the convention: `commercetools-project-sync.{runnerName}.{syncModuleName}`.
- The `key` contains the source project key.
- The `value` contains the information  `lastSyncDurationInMillis`, `applicationVersion`, `lastSyncTimestamp` and `lastSyncStatistics`.

_Note:_ Another `customObject` with the `container` convention `commercetools-project-sync.{runnerName}.{syncModuleName}.timestampGenerator` is also created on the target project for capturing a unified timestamp from commercetools.

Running a Full sync using `-f` or `--full` option will not create any `customObjects`.

#### Running the Docker Image

##### Download

   ```bash
docker pull commercetools/commercetools-project-sync:1.1.0
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
commercetools/commercetools-project-sync:1.1.0 -s all
```
  

### Examples   
 - To run the all sync modules from a source project to a target project
   ```bash
   docker run commercetools/commercetools-project-sync:1.1.0 -s all
   ```
   This will run the following sync modules in the given order:
 1. `Type` Sync and `ProductType` Sync in parallel.
 2. `Category` Sync.
 3. `Product` Sync.
 4. `InventoryEntry` Sync.

 - To run the type sync
   ```bash
   docker run commercetools/commercetools-project-sync:1.1.0 -s types
   ```  

 - To run the productType sync
   ```bash
   docker run commercetools/commercetools-project-sync:1.1.0 -s productTypes
   ```  
    
- To run the category sync
   ```bash
   docker run commercetools/commercetools-project-sync:1.1.0 -s categories
   ```  
   
- To run the product sync
   ```bash
   docker run commercetools/commercetools-project-sync:1.1.0 -s products
   ```  
    
- To run the inventoryEntry sync
   ```bash
   docker run commercetools/commercetools-project-sync:1.1.0 -s inventoryEntries
   ```   
       
- To run all sync modules using a runner name
   ```bash
   docker run commercetools/commercetools-project-sync:1.1.0 -s all -r myRunnerName
   ```     
   

