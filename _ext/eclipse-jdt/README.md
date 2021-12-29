# spotless-eclipse-jdt

Eclipse JDT and its dependencies require a large amount of byte code.
Hence they should not be directly be required by the Spotless, but only be requested in case
they are configured by the Spotless configuration. Hence we publish Eclipse's formatter and all its dependencies, along with a small amount of glue code, into the `com.diffplug.gradle.spotless:spotless-eclipse-jdt` artifact.

## Build

The `_ext` projects are disabled per default. [CONTRIBUTING.md](../../CONTRIBUTING.md)
describes how to enable them.

## License

Spotless at large is under the Apache 2.0 license, but this jar is under the EPL v1.

## Bump version

Most Eclipse updates do not require code or test modifications. But be aware that this project
needs to access internal methods of the JDT plugin. Hence, the build step may fail
after upgrading the Eclipse version.

1. Update the version information in [projects properties](gradle.properties), e.g. only `VER_ECLIPSE` (see [Eclipse project](https://projects.eclipse.org/projects/eclipse/governance)).
2. Build with latest Eclipse artifacts and update the dependency locks (`gradlew --write-locks :eclipse-jdt:build`).
3. Prepare the integration of the new version into the next Spotless release:
    * Execute `gradlew :eclipse-jdt:generateLibExtraLockFile`.
    * Update the default version in the `EclipseJdtFormatterStep` class of `lib-extra`.
    * Check your changes by running `gradlew :lib-extra:build`.
