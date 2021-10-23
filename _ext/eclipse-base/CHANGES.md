# spotless-eclipse-base

We adhere to the [keepachangelog](https://keepachangelog.com/en/1.0.0/) format (starting after version `3.2.1`).

## [Unreleased]

## [3.5.2] - 2021-10-23
### Fixed
* Racing condition when cleaning up temporary workspace on JVM runtime shutdown (see [#967](https://github.com/diffplug/spotless/issues/967)). Can lead to error logs and remaining files in workspace.

## [3.5.1] - 2021-10-16
### Fixed
* ~~Racing condition when cleaning up temporary workspace on JVM runtime shutdown (see [#967](https://github.com/diffplug/spotless/issues/967)). Can lead to error logs and remaining files in workspace.~~

## [3.5.0] - 2021-06-20
### Added
* Support of `org.eclipse.core.resources` version `3.15.0` required by Eclipse `4.20`.
* Minimum required Java version changed from 8 to 11.

## [3.4.2] - 2020-12-26
### Fixed
* `org.eclipse.osgi` version `3.16.100` does not allow `null` as Debug service for `CloseableBundleFile`.

## [3.4.1] - 2020-09-24
### Fixed
* Restored scope of transitive dependencies to 'compile'.

## [3.4.0] - 2020-09-23
### Added
* Upgraded to `org.eclipse.osgi` version `3.16`.

## [3.3.0] - 2020-03-04
### Added
* Added support of plugin extensions without activator ([#533](https://github.com/diffplug/spotless/issues/533)). Required since `org.eclipse.core.filesystem` version `1.7.600` (see
[Bug 550548](https://bugs.eclipse.org/bugs/show_bug.cgi?id=550548))
* Updated configuration via single interface implementation. Functional based configuration still supported.

## [3.2.1] - 2019-09-03
* Fixed deletion of temporary workspace. ([#447](https://github.com/diffplug/spotless/issues/447))

## [3.2.0] - 2019-06-30
* Added support of Eclipse 4.12 framework wiring. ([#413](https://github.com/diffplug/spotless/issues/413))

## [3.1.1] - 2019-06-04
* Fixed problem handling URL escaped characters in JAR file location. ([#401](https://github.com/diffplug/spotless/issues/401))

## [3.1.0] - 2019-02-10
* Added logging service based on SLF4J. ([#236](https://github.com/diffplug/spotless/issues/236))
* Updated internal interfaces to support `org.eclipse.osgi` version 3.13.
* Corrected documentation of version number usage in `gradle.properties`.

## [3.0.0] - 2018-06-18
* Initial release!
