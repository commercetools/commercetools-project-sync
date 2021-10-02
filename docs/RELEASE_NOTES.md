# Release Notes

<!-- RELEASE NOTE FORMAT

1. Please use the following format for the release note subtitle
### {version} - {date}

2. link to commits of release.
3. link to Javadoc of release.
4. link to Jar of release.

5. Add a summary of the release that is not too detailed or technical.

6. Depending on the contents of the release use the subitems below to 
  document the new changes in the release accordingly. Please always include
  a link to the related issue number. 
   **New Features** (n) 🎉 
   **Breaking Changes** (n) 🚧 
   **Enhancements** (n) ✨
   **Dependency Updates** (n) 🛠️ 
   **Documentation** (n) 📋
   **Critical Bug Fixes** (n) 🔥 
   **Bug Fixes** (n)🐞
   - **Category Sync** - Sync now supports product variant images syncing. [#114](https://github.com/commercetools/commercetools-sync-java/issues/114)
   - **Build Tools** - Convenient handling of env vars for integration tests.

7. Add Migration guide section which specifies explicitly if there are breaking changes and how to tackle them.
-->

---

All notable changes to the [commercetools-project-sync project](https://github.com/commercetools/commercetools-project-sync) will be documented in this file

This project adheres to [Semantic Versioning](https://semver.org/).


### 5.0.0 - Oct 1, 2021
[Commits](https://github.com/commercetools/commercetools-project-sync/compare/4.0.10...5.0.0)

- ✨ **Enhancements** (1)
  - Removed support of changing the attribute definition type ([#787](https://github.com/commercetools/commercetools-sync-java/pull/787)) since removal and addition of the attribute with the same 
  name in a single request is not possible by commercetools API anymore. For more information please [check](https://github.com/commercetools/commercetools-sync-java/blob/master/docs/adr/0003-syncing-attribute-type-changes.md).
  
- 🛠️ **Dependency Updates** (1)
  - `com.commercetools:commercetools-sync-java: 7.0.2 -> 8.0.0`

- 🐞 **Bug Fixes** (1)
  - Removed support of changing the attribute definition type ([#787](https://github.com/commercetools/commercetools-sync-java/pull/787))

---

### 4.0.10 - Sep 27, 2021
[Commits](https://github.com/commercetools/commercetools-project-sync/compare/4.0.9...4.0.10)

- ✨ **Enhancements** (1)
  - Dependency management - Migrate Dependabot to Renovate. ([#324](https://github.com/commercetools/commercetools-project-sync/pull/324))

- 🛠️ **Dependency Updates** (1)
  - `com.commercetools:commercetools-sync-java: 7.0.1 -> 7.0.2`

---

### 4.0.9 - Sep 16, 2021
[Commits](https://github.com/commercetools/commercetools-project-sync/compare/4.0.8...4.0.9)

- 🛠️ **Dependency Updates** (1)
  - `com.commercetools:commercetools-sync-java: 7.0.0 -> 7.0.1`

- 🐞 **Bug Fixes** (2)
  - Fixes for Syncing state with no transitions configured leads to empty transition. Issue: ([#321](https://github.com/commercetools/commercetools-project-sync/issues/321)) , PR: ([#322](https://github.com/commercetools/commercetools-project-sync/pull/322))

  - Fix flaky test failure. ([#323](https://github.com/commercetools/commercetools-project-sync/pull/323))

---

### 4.0.8 - Aug 25, 2021
[Commits](https://github.com/commercetools/commercetools-project-sync/compare/4.0.7...4.0.8)

- 🛠️ **Dependency Updates** (2)
  - Remove `commercetoolsJvmSdk` library dependencies and use them from `commercetoolsSyncJava` dependencies.
  - `com.commercetools:commercetools-sync-java: 6.0.0 -> 7.0.0`

- 🐞 **Bug Fixes** (1)
  - To avoid dependency version mismatch between projects, Use the same commercetoolsJvmSdk dependencies from the commercetoolsSyncJava library.

---

### 4.0.7 - Jul 29, 2021
[Commits](https://github.com/commercetools/commercetools-project-sync/compare/4.0.6...4.0.7)

- 🛠️ **Dependency Updates** (1)
  - `commercetoolsJvmSdkVersion 2.0.0 -> 1.64.0`

- 🐞 **Bug Fixes** (1)
  - Fix for SDK version mismatch. ([#318](https://github.com/commercetools/commercetools-project-sync/pull/318))

---

### 4.0.6 - Jul 26, 2021
[Commits](https://github.com/commercetools/commercetools-project-sync/compare/4.0.5...4.0.6)

- ✨ **Enhancements** (1)
  - Update user-agent header to display SDK version for proper usage statistics of the library. ([#315](https://github.com/commercetools/commercetools-project-sync/pull/315))

- 🛠️ **Dependency Updates** (3)
  - `commercetoolsJvmSdkVersion 1.64.0 -> 2.0.0`
  - `com.diffplug.spotless 5.14.1 -> 5.14.2`
  - `logbackVersion 1.2.3 -> 1.2.4-groovyless`

---

### 4.0.5 - Jul 19, 2021
[Commits](https://github.com/commercetools/commercetools-project-sync/compare/4.0.4...4.0.5)

- 🛠️ **Dependency Updates** (1)
  - `com.commercetools:commercetools-sync-java: 5.1.3 -> 6.0.0`

- 🐞 **Bug Fixes** (1)
  - Fixes the DuplicateField bug in the InventorySync related to fetching and syncing inventories with multiple channels. Issue: ([#301](https://github.com/commercetools/commercetools-project-sync/issues/301)), PR: ([#310](https://github.com/commercetools/commercetools-project-sync/pull/310))

---

### 4.0.4 - Jul 8, 2021
[Commits](https://github.com/commercetools/commercetools-project-sync/compare/4.0.3...4.0.4)

- 🛠️ **Dependency Updates** (2)
  - `com.commercetools:commercetools-sync-java: 5.1.2 -> 5.1.3`
  - `commercetools-jvm-sdk 1.63.0 -> 1.64.0`

- 🐞 **Bug Fixes** (1)
  - TaxCategory Sync - TaxCategories to sync properly when we have many TaxRates with different states.

---

### 4.0.3 - Jun 2, 2021
[Commits](https://github.com/commercetools/commercetools-project-sync/compare/4.0.2...4.0.3)

- 🛠️ **Dependency Updates** (1)
  - `com.commercetools:commercetools-sync-java: 5.1.1 -> 5.1.2`

- 🐞 **Bug Fixes** (1)
  - Product Sync - The user is now aware of unresolvable references as the transform service will not skip the products.

---

### 4.0.2 - May 28, 2021
[Commits](https://github.com/commercetools/commercetools-project-sync/compare/4.0.1...4.0.2)

- 🛠️ **Dependency Updates** (3)
  - `com.diffplug.spotless 5.12.4 -> 5.12.5`
  - `netty-codec-http 4.1.63.Final -> 4.1.65.Final`
  - `mockito-core 3.8.0 -> 3.10.0`

- 🐞 **Bug Fixes** (2)
  - Enable unit test case in InventoryEntries sync, which was found failure during build and it is now resolved.

  - Remove Caffeine artifact in the project, which has version conflict with the underlying Caffeine dependency inside sync-java library during findbugs checking.

---

### 4.0.1 - May 19, 2021
[Commits](https://github.com/commercetools/commercetools-project-sync/compare/4.0.0...4.0.1)

- 🛠️ **Dependency Updates** (2)
  - `com.commercetools:commercetools-sync-java: 5.1.0 -> 5.1.1`
  - `commercetools-jvm-sdk 1.62.0 -> 1.63.0`

- 🐞 **Bug Fixes** (2)
  - Product Sync - Special characters can be defined for ProductDraft key. ([#269](https://github.com/commercetools/commercetools-project-sync/issues/269))

  - Product Sync - PriceTiers can be synched successfully. ([#271](https://github.com/commercetools/commercetools-project-sync/issues/271))

---

### 4.0.0 - Apr 22, 2021
[Commits](https://github.com/commercetools/commercetools-project-sync/compare/3.12.0...4.0.0)

- ✨ **Enhancements** (4)
  - Adapt changes from Java-Sync V5.0.0.
  - Remove duplication code in Project-Sync and use Java-Sync library Transform Utils(with cache for better performance) to map resources to drafts.
  - Add custom deserializer used to deserialize lastSyncStatistics.
  - Use additional context for the statistic logs instead of escaped json and use concrete loggerName.

- 🛠️ **Dependency Updates** (2)
  - `com.commercetools:commercetools-sync-java: 4.0.1 -> 5.1.0`
  - `commercetools-jvm-sdk 1.60.0-> 1.62.0`

---

### 3.12.0 - Apr 9, 2021
[Commits](https://github.com/commercetools/commercetools-project-sync/compare/3.11.0...3.12.0)

- ✨ **Enhancements** (2)
  - Migrate java version from java 1.8 to java 11
  - Structure the log entries ([#11](https://github.com/commercetools/commercetools-project-sync/issues/11))

- ✨ **Documentation** (1)
  - Update prerequisite section in README.md since JDK installation is no longer necessary.

- 🛠️ **Dependency Updates** (5)
  - `com.commercetools:commercetools-sync-java: 4.0.0 -> 4.0.1`
  - `com.fasterxml.jackson.dataformat:jackson-dataformat-cbor : 2.11.4 -> 2.12.2`
  - `io.netty:netty-codec-http: 4.1.59.Final -> 4.1.63.Final`
  - `com.diffplug.spotless: 5.11.0-> 5.11.1`
  - `org.asynchttpclient:async-http-client 2.12.2 -> 2.12.3`

---

### 3.11.0 - Mar 8, 2021
[Commits](https://github.com/commercetools/commercetools-project-sync/compare/3.10.1...3.11.0)

- 🎉 **New Features** (1)
  - Custom product queries and custom product fetch size limit ([#219](https://github.com/commercetools/commercetools-project-sync/pull/219))
    The application can accept command line argument productQueryParameters with custom query and fetch size limit.
  
- ✨ **Enhancements** (3)
  - Added `asyncHttpClientVersion 2.12.2`
  - Cache all references on products and categories instead of expanding all the references and do reference resolution using cached(Id to Key) values.
  - Use chunking on results of Product Attribute references of GraphQL request.

- 🛠️ **Dependency Updates** (2)
  - `com.commercetools:commercetools-sync-java: 3.1.0 -> 4.0.0`
  - `commercetools-jvm-sdk 1.56.0 -> 1.60.0`

---

### 3.10.1 - Feb 1, 2021
[Commits](https://github.com/commercetools/commercetools-project-sync/compare/3.10.0...3.10.1)

- ✨ **Enhancements** (1)
  - Remove stacktraces from expected exceptions. ([#226](https://github.com/commercetools/commercetools-project-sync/pull/226))

- ✨ **Build Tools** (1)
  - CI and CD changes: Travis to GitHub actions migration ([#228](https://github.com/commercetools/commercetools-project-sync/pull/228))
  
- ✨ **Documentation** (1)
  - Documentation update for build and publish process of docker image ([#16](https://github.com/commercetools/commercetools-project-sync/issues/16))

- 🛠️ **Dependency Updates** (1)
  - `com.commercetools:commercetools-sync-java: 3.0.2 -> 3.1.0`

---

### 3.10.0 - Jan 7, 2021
[Commits](https://github.com/commercetools/commercetools-project-sync/compare/3.9.0...3.10.0)

- ✨ **Enhancements** (1)
  - Running Multiple Syncers ([#219](https://github.com/commercetools/commercetools-project-sync/pull/219))
    The application can sync multiple resources. For example, to run type and productType sync together,
    the -s option with types productTypes as below:
    >docker run commercetools/commercetools-project-sync:3.10.0 -s types productTypes

- 🛠️ **Dependency Updates** (2)
  - `com.commercetools:commercetools-sync-java: 3.0.0 -> 3.0.2`
  - `commercetools-jvm-sdk 1.55.0 -> 1.56.0`

- 🐞 **Bug Fixes** (1)
  - Fixed a bug in the duration calculation of decorated retry sphere client RetrySphereClientDecorator created by ClientConfigurationUtils, and used the utility called RetryableSphereClientBuilder in the latest version of the JVM-sdk.

---

### 3.9.0 - Nov 19, 2020
[Commits](https://github.com/commercetools/commercetools-project-sync/compare/3.8.0...3.9.0)

- ✨ **Enhancements** (1)
  - Support shopping list sync ([#199](https://github.com/commercetools/commercetools-project-sync/issues/199))

- 🛠️ **Dependency Updates** (1)
  - `com.commercetools:commercetools-sync-java: 2.3.0 -> 3.0.0`

---

### 3.8.0 - Oct 22, 2020
[Commits](https://github.com/commercetools/commercetools-project-sync/compare/3.7.0...3.8.0)

- ✨ **Enhancements** (1)
  - Add customer sync ([#184](https://github.com/commercetools/commercetools-project-sync/pull/184))

---

### 3.7.0 - Oct 20, 2020
[Commits](https://github.com/commercetools/commercetools-project-sync/compare/3.6.0...3.7.0)

- ✨ **Enhancements** (1)
  - Support new option to enable synchronisation of generated custom objects to target project ([#179](https://github.com/commercetools/commercetools-project-sync/issues/179))

- 🛠️ **Dependency Updates** (3)
  - `org.mockito:mockito-core: 3.5.13 -> 3.5.15`
  - `com.commercetools:commercetools-sync-java: 2.2.1 -> 2.3.0`
  - `com.adarshr.test-logger:com.adarshr.test-logger.gradle.plugin: 2.1.0 -> 2.1.1`

---

### 3.6.0 - Sep 29, 2020
[Commits](https://github.com/commercetools/commercetools-project-sync/compare/3.5.1...3.6.0)

- ✨ **Enhancements** (1)
  - Support key-value-document (custom object) reference sync ([#176](https://github.com/commercetools/commercetools-project-sync/pull/176))

---

### 3.5.1 - Sep 28, 2020
[Commits](https://github.com/commercetools/commercetools-project-sync/compare/3.5.0...3.5.1)

- 🐞 **Bug Fixes** (1)
  - Project sync didn't sync on updates ([#170](https://github.com/commercetools/commercetools-project-sync/pull/170))

---

### 3.5.0 - Sep 25, 2020
[Commits](https://github.com/commercetools/commercetools-project-sync/compare/3.4.2...3.5.0)

- ✨ **Enhancements** (1)
  - Support for custom object sync ([#164](https://github.com/commercetools/commercetools-project-sync/issues/164))

---

### 3.4.2 - Sep 17, 2020
[Commits](https://github.com/commercetools/commercetools-project-sync/compare/3.4.1...3.4.2)

- 🛠️ **Dependency Updates** (4)
  - `com.commercetools:commercetools-sync-java: 1.9.1 -> 2.0.0` ([#158](https://github.com/commercetools/commercetools-project-sync/pull/158))
  - `com.commercetools.sdk.jvm.core:commercetools-models: 1.52.0 -> 1.53.0`
  - `com.commercetools.sdk.jvm.core:commercetools-java-client: 1.52.0 -> 1.53.0`
  - `com.commercetools.sdk.jvm.core:commercetools-convenience: 1.52.0 -> 1.53.0`

---

### 3.4.1 - Aug 27, 2020
[Commits](https://github.com/commercetools/commercetools-project-sync/compare/3.4.0...3.4.1)

- ✨ **Enhancements** (2)
  - Execute Tax Category Sync parallel with Type/ProductType/State Sync. Execute Category/InventoryEntries/CartDiscount Sync in parallel before running Product Sync. ([#143](https://github.com/commercetools/commercetools-project-sync/issues/143))
  - Modify the execution order of different sync modules. https://github.com/commercetools/commercetools-project-sync#examples

- 🛠️ **Dependency Updates** (3)
  - `mockito-core: 3.4.6 -> 3.5.7`
  - `commercetools-jvm-sdk: 1.51.0 -> 1.52.0`
  - `com.diffplug.gradle.spotless: 4.5.1 -> 5.2.0`

---

### 3.4.0 - Aug 13, 2020
[Commits](https://github.com/commercetools/commercetools-project-sync/compare/3.3.0...3.4.0)

- 🎉 **New Features** (1)
  - New Tax Category Syncer module to sync tax categories between commercetools projects. ([#134](https://github.com/commercetools/commercetools-project-sync/issues/134))

- ✨ **Documentation** (2)
  - Update link for docker pull in README.
  - Modify the description in docker command example.

---

### 3.3.0 - Aug 11, 2020
[Commits](https://github.com/commercetools/commercetools-project-sync/compare/3.2.1...3.3.0)

- 🎉 **New Features** (1)
  - New State Syncer module to sync states between commercetools projects.

- 🛠️ **Dependency Updates** (5)
  - `mockito-core: 3.3.3 -> 3.4.6`
  - `commercetools-sync-java: 1.8.2 -> 1.9.1`
  - `com.github.ben-manes.versions: 0.28.0 -> 0.29.0`
  - `com.adarshr.test-logger: 2.0.0 -> 2.1.0`
  - `com.diffplug.gradle.spotless: 4.3.0 -> 4.5.1`

---

### 3.2.1 - Jun 12, 2020
[Commits](https://github.com/commercetools/commercetools-project-sync/compare/3.2.0...3.2.1)

- ✨ **Enhancements** (1)
  - Add retry strategy for product type deletion and update in integration test to avoid unexpected 409 (concurrent modification exceptions) ([#108](https://github.com/commercetools/commercetools-project-sync/pull/108))

- 🛠️ **Dependency Updates** (4)
  - `com.diffplug.gradle.spotless: 3.27.1 -> 3.28.0`
  - `org.mockito:mockito-core: 3.2.4 -> 3.3.3`
  - `org.assertj:assertj-core: 3.14.0 -> 3.15.0`
  - `com.github.ben-manes.versions: 0.26.0 -> 0.28.0`

---

### 3.2.0 - Jan 24, 2020
[Commits](https://github.com/commercetools/commercetools-project-sync/compare/3.1.0...3.2.0)

- 🛠️ **Dependency Updates** (8)
  - `commercetools-jvm-sdk: 1.47.0 -> 1.49.0`
  - `commercetools-sync-java: 1.6.1 -> 1.8.0`
  - `com.diffplug.gradle.spotless: 3.25.0 -> 3.27.1`
  - `org.mockito:mockito-core: 3.1.0 -> 3.2.4`
  - `org.slf4j:slf4j-api: 1.7.25 -> 1.7.30`
  - `org.slf4j:slf4j-simple : 1.7.25 -> 1.7.30`
  - `org.assertj:assertj-core: 3.13.2 -> 3.14.0`
  - `org.junit.jupiter:junit-jupiter-engine: 5.5.2 -> 5.6.0`

- 🐞 **Bug Fixes** (1)
  - Fixes a bug in the delta sync: ([#35](https://github.com/commercetools/commercetools-project-sync/pull/35))

---

### 3.1.0 - Oct 17, 2019
[Commits](https://github.com/commercetools/commercetools-project-sync/compare/3.0.0...3.1.0)

- 🛠️ **Dependency Updates** (2)
  - `commercetools-jvm-sdk: 1.46.0 -> 1.47.0`
  - `commercetools-sync-java: 1.6.0 -> 1.6.1`

- 🐞 **Bug Fixes** (1)
  - Contains updates included in [commercetools-sync-java:1.6.1](https://commercetools.github.io/commercetools-sync-java/doc/RELEASE_NOTES/#161-oct-17-2019)

---

### 3.0.0 - Oct 10, 2019
[Commits](https://github.com/commercetools/commercetools-project-sync/compare/2.0.0...3.0.0)

- 🎉 **New Features** (2)
  - New CartDiscount Syncer module to sync cartDiscounts between commercetools projects.
  - All the new features/changes that come with the jump of commercetools-sync-java from [1.1.1 -> 1.6.0](https://commercetools.github.io/commercetools-sync-java/doc/RELEASE_NOTES/)

- 🛠️ **Dependency Updates** (11)
  - `commercetoolsjvm-sdk: 1.38.0 -> 1.46.0`
  - `commercetools-sync-java: 1.1.1 -> 1.6.0`
  - `assertj-core: 3.12.2 -> 3.13.2`
  - `mockito-core: 2.28.2 -> 3.1.0`
  - `jupiter-engine: 5.3.2 -> 5.5.2`
  - `gradle: 5.4.1 -> 5.6.2`
  - `com.github.jengelman.gradle.plugins:shadow: 5.0.0 -> 5.1.0`
  - `com.github.ben-manes.versions: 0.21.0 -> 0.26.0`
  - `com.adarshr.test-logger: 1.7.0 -> 2.0.0`
  - `com.diffplug.gradle.spotless: 3.23.0 -> 3.25.0`
  - `com.bmuschko.docker-java-application: 4.9.0 -> 5.2.0`

---

### 2.0.0 - Jan 16, 2019
[Commits](https://github.com/commercetools/commercetools-project-sync/compare/1.1.0...2.0.0)

- ✨ **Enhancements** (1)
  - Now by default, running the sync without using -f or --full option would run a delta sync; which means that only resources which have been modified since the last time the sync has run would be synced.
  To run a full sync use `-f` or `--full`

---

### 1.1.0 - Jan 16, 2019
[Commits](https://github.com/commercetools/commercetools-project-sync/compare/1.0.0...1.1.0)

- ✨ **Enhancements** (1)
  - Merge pull request ([#20](https://github.com/commercetools/commercetools-project-sync/pull/20))
---

### 1.0.0 - Jan 11, 2019

- 🎉 **New Features** (1)
  - 1st Release

---