# spotless-eclipse-cdt

Eclipse CDT is not available in a form which can be easily consumed by maven or gradle.  To fix this, we publish Eclipse's formatter and all its dependencies, along with a small amount of glue code, into the `com.diffplug.gradle.spotless:spotless-eclipse-cdt` artifact.

## Build

The `_ext` projects are disabled per default. [CONTRIBUTING.md](../../CONTRIBUTING.md)
describes how to enable them.

## License

Spotless at large is under the Apache 2.0 license, but this jar is under the EPL v1.

## Bump version

Most Eclipse updates do not require code or test modifications. But be aware that this project
needs to access internal methods of the CDT plugin. Hence, the build step may fail
after upgrading the Eclipse version.

1. Update the version information in [projects properties](gradle.properties), e.g.
    * `VER_ECLIPSE`: Eclipse version (see [Eclipse project](https://projects.eclipse.org/projects/eclipse/governance))
    * `VER_ECLIPSE_CDT`: CDT version (see [CDT downloads](https://www.eclipse.org/cdt/downloads.php))

2. Update the [change log](CHANGES.md).

3. This project creates a M2 artifact, containing classes from Eclipse P2 artifacts. Hence a new project version is published for each new Eclipse version. Build and publish the new version via [Circle CI](https://app.circleci.com/pipelines/github/diffplug spotless).

Once the new version is [available on maven central](https://search.maven.org/artifact/com.diffplug.spotless/spotless-eclipse-cdt), prepare the integration of the new version
into the next Spotless release.

1. Execute `gradlew :eclipse-cdt:generateLibExtraLockFile`.
2. Update the default version in the `EclipseCdtFormatterStep` class of `lib-extra`.
3. Check your changes by running `gradlew :lib-extra:build`.
