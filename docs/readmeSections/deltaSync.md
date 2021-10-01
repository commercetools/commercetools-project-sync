
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
