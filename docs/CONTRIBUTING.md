# Contributing

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


  - [Development](#development)
      - [Prerequisites](#prerequisites)
      - [Run unit tests](#run-unit-tests)
      - [Run Spotless Style Check](#run-spotless-style-check)
      - [Fix Spotless style violations](#fix-spotless-style-violations)
      - [Package JARs (Without dependencies)](#package-jars-without-dependencies)
      - [Package an über JAR (packed with all needed dependencies)](#package-an-%C3%BCber-jar-packed-with-all-needed-dependencies)
      - [Package JARs and run tests (Recommended for development)](#package-jars-and-run-tests-recommended-for-development)
      - [Full build with tests](#full-build-with-tests)
  - [Build](#build)
- [License](#license)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->


- Every PR should address an issue on the repository. If the issue doesn't exist, please create it first.
- Pull requests should always follow the following naming convention: 
`[issue-number]-[pr-name]`. For example,
to address issue #2055 which refers to a style bug, the PR addressing it should have a name that looks something like
 `2055-fix-style-bug`.
- Commit messages should always be prefixed with the number of the issue that they address. 
For example, `#2055: Remove redundant space.`
- After your PR is merged to master:
    - Delete the branch.
    - Mark the issue it addresses with the `merged-to-master` label.
    - Close the issue **only** if the change was released.

### Development

##### Prerequisites
 - For Intellij users, make sure to use Intellij IDEA version 2018.2 or higher.

##### Run unit tests
````bash
./gradlew test
````

##### Run integration tests
- Create a file `ctp.credentials.properties` and add credentials for CTP source project and CTP target project. 
````bash
./gradlew integrationTest
````
Note: to run integration tests using IntelliJ, go to `Preferences - Build, Execution, Deployment - Build Tools - Gradle` and select `Run tests using IntelliJ IDEA`.

##### Run Spotless Style Check
````bash
./gradlew spotlessCheck
````

##### Fix Spotless style violations
````bash
./gradlew spotlessApply
````

It uses the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) as rules for the style. 
It is recommended to set it in your IDE auto formatting settings for this project. 
[More info](https://github.com/google/google-java-format#intellij).


##### Package JARs (Without dependencies)
````bash
./gradlew clean jar
````

##### Package an über JAR (packed with all needed dependencies)
````bash
./gradlew clean shadowJar
````

##### Package JARs and run tests (Recommended for development)
````bash
./gradlew clean check
````

##### Full build with tests
````bash
./gradlew clean build
````

### Build 

 Gradle docker plugin is used to build and deploy the docker images. 
 To build the docker image locally, please run
 ````bash
./gradlew dockerBuildImage
````
The docker image has been built and published to your desktop docker.

Example:
For testing all resources sync,
````bash
docker run commercetools/commercetools-project-sync:<version> -s all
````

For more detailed information on the build and release process, see [Build and Release](BUILD.md) documentation.

## License
Copyright (c) 2019 commercetools
