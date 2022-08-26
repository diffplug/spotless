# spotless-eclipse-jdt

Eclipse JDT and its dependencies require a large amount of byte code.
Hence they should not be directly be required by the Spotless, but only be requested in case
they are configured by the Spotless configuration. Hence we publish Eclipse's formatter and all its dependencies, along with a small amount of glue code, into the `com.diffplug.gradle.spotless:spotless-eclipse-jdt` artifact.

To publish a new version, update the `_ext/eclipse-jdt/gradle.properties` appropriately and see [CONTRIBUTING.md](../../CONTRIBUTING.md) how to enable
`_ext` projects.

## License

Spotless at large is under the Apache 2.0 license, but this jar is under the EPL v1.
