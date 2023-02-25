# spotless-eclipse-groovy

Groovy-Eclipse is not available in a form which can be easily consumed by maven or gradle.
To fix this, we publish Groovy-Eclipse's formatter and all its dependencies, along with a small amount of glue code, into the `com.diffplug.gradle.spotless:spotless-eclipse-groovy` artifact.

## Build

To publish a new version, update the `_ext/eclipse-groovy/gradle.properties` appropriately and see [CONTRIBUTING.md](../../CONTRIBUTING.md) how to enable
`_ext` projects.

## License

Spotless at large is under the Apache 2.0 license, but this jar is under the EPL v1.
