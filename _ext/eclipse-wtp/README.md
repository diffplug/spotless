# spotless-eclipse-wtp

Eclipse WTP is not available in a form which can be easily consumed by maven or gradle. To fix this, we publish Eclipse's WTP formatters, along with a small amount of glue code, into the `com.diffplug.spotless.extra:spotless-eclipse-wtp` artifact.

## Build

The `_ext` projects are disabled per default. [CONTRIBUTING.md](../../CONTRIBUTING.md)
describes how to enable them.

## License

Spotless at large is under the Apache 2.0 license, but this jar is under the EPL v1.

## Bump version

Most Eclipse updates do not require code or test modifications. But be aware that this project
needs to access internal methods of the WTP plugin. Hence, the build step may fail
after upgrading the Eclipse version.

1. Update the version information in [projects properties](gradle.properties), e.g.
    * `VER_ECLIPSE`: Eclipse version (see [Eclipse project](https://projects.eclipse.org/projects/eclipse/governance))
    * `VER_ECLIPSE_WTP`: WTP version (see [WTP project](https://www.eclipse.org/webtools/))
    * `VER_ECLIPSE_WTP_REPO`: The PR repository name which is equal to the Eclipse year-month version (see [WTP repository overview](https://download.eclipse.org/webtools/repository/)).

2. Update the [change log](CHANGES.md).

3. This project creates a M2 artifact, containing classes from Eclipse P2 artifacts. Hence a new project version is published for each new Eclipse version. Build and publish the new version via [Circle CI](https://app.circleci.com/pipelines/github/diffplug spotless).

Once the new version is [available on maven central](https://search.maven.org/artifact/com.diffplug.spotless/spotless-eclipse-groovy), prepare the integration of the new version
into the next Spotless release.

1. Execute `gradlew :eclipse-wtp:generateLibExtraLockFile`.
2. Update the default version in the `EclipseWtpFormatterStep` class of `lib-extra`.
3. Check your changes by running `gradlew :lib-extra:build`.
