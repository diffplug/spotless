# spotless-groovy-eclipse

Groovy-Eclipse is not available in a form which can be easily consumed by maven or gradle.
To fix this, we publish Groovy-Eclipse's formatter and all its dependencies, along with a small amount of glue code, into the `com.diffplug.gradle.spotless:spotless-greclipse` artifact.

## Build

To publish a new version, update the `_ext/greclipse/gradle.properties` appropriately and run this from the root directory:

```
gradlew -b _ext/greclipse/build.gradle publish
```

Spotless at large is under the Apache 2.0 license, but this jar is under the EPL v1.

## IDE

For IDE support the JAR dependencies and their sources are downloaded from P2 repositories.
This can, due to restrictions on server side, take up to half an hour.


### Eclipse

Run the `eclipse` task:

```
gradlew -b _ext/greclipse/build.gradle eclipse
```

Import the existing general project (NOT the Gradle project) from `_ext/greclipse` into your workspace.


### IntelliJ IDEA

Run the `idea` task:

```
gradlew -b _ext/greclipse/build.gradle idea
```

Import the project from `_ext/greclipse` using the external Gradle model.
