# spotless-eclipse-groovy

Groovy-Eclipse is not available in a form which can be easily consumed by maven or gradle.
To fix this, we publish Groovy-Eclipse's formatter and all its dependencies, along with a small amount of glue code, into the `com.diffplug.gradle.spotless:spotless-eclipse-groovy` artifact.

## Build

The `_ext` projects are disabled per default. [CONTRIBUTING.md](../../CONTRIBUTING.md)
describes how to enable them.

## License

Spotless at large is under the Apache 2.0 license, but this jar is under the EPL v1.

## Bump version

Most Eclipse updates do not require code or test modifications. But be aware that this project
needs to access internal methods of the Groovy-Eclipse plugin. Hence, the build step may fail
after upgrading the Eclipse version.

1. Update the version information in [projects properties](gradle.properties), e.g.
    * `VER_ECLIPSE`: Eclipse version (see [Eclipse project](https://projects.eclipse.org/projects/eclipse/governance))
    * `VER_GRECLIPSE`: Groovy-Eclipse version (see [Groovy-Eclipse project](https://github.com/groovy/groovy-eclipse/wiki#news))
    * `VER_GROOVY`: The groovy JAR is part of the org.codehaus.groovy JAR. Either download the P2 repository from `dist.springsource.org`, or have a look at the [Groovy-Eclipse about page](https://github.com/groovy/groovy-eclipse/blob/master/base/org.codehaus.groovy40/about.html), which also lists the current version in the *Third Party Content* section.
    * `VAR_GRECLIPSE_JDT_PATCH`: The P2 repository contains two JDT JARs, one original and a patched one for groovy. Both are only distinguished by their version. Assure that you use the patched version, which contains the Groovy-Eclipse extended version string (timestamp, year, ...). Either download the P2 repository from `dist.springsource.org`, or have a look at the [Groovy-Eclipse release notes](https://github.com/groovy/groovy-eclipse/wiki#news), which lists the Groovy-Eclipse extended version string for the org.codehaus.groovy update sites at the bottom of the page.

2. Update the [change log](CHANGES.md).

3. This project creates a M2 artifact, containing classes from Eclipse P2 artifacts. Hence a new project version is published for each new Eclipse version. Build and publish the new version via [Circle CI](https://app.circleci.com/pipelines/github/diffplug spotless).

Once the new version is [available on maven central](https://search.maven.org/artifact/com.diffplug.spotless/spotless-eclipse-groovy), prepare the integration of the new version
into the next Spotless release.

1. Execute `gradlew :eclipse-groovy:generateLibExtraLockFile`.
2. Update the default version in the `GrEclipseFormatterStep` class of `lib-extra`.
3. Check your changes by running `gradlew :lib-extra:build`.
