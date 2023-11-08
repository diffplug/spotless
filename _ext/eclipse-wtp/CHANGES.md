# spotless-eclipse-wtp

We adhere to the [keepachangelog](https://keepachangelog.com/en/1.0.0/) format (starting after version `3.15.1`).

## [Unreleased]
### Fixed
* Fix typo in Gradle variable names ([#1425](https://github.com/diffplug/spotless/pull/1425))

## [3.23.0] - 2021-09-22
### Added
* Switch to Web Tools Platform release 3.23.0 for Eclipse 4.21.

## [3.22.0] - 2021-06-27
### Added
* Switch to Web Tools Platform release 3.22.0 for Eclipse 4.20.

## [3.21.0] - 2021-06-07
### Added
* Switch to Web Tools Platform release 3.21.0 for Eclipse 4.19. Minimum required Java version changed from 8 to 11.

## [3.20.0] - 2020-12-26
### Added
* Switch to Web Tools Platform release 3.20.0 for Eclipse 4.18.

## [3.19.0] - 2020-10-17
### Added
* Fixed version number determined by change log.

## [3.18.1] - 2020-10-17
### Added
* Switch to Web Tools Platform release 3.19.0 for Eclipse 4.17.

## [3.18.0] - 2020-10-03
### Added
* Switch to Web Tools Platform release 3.18.0 for Eclipse 4.16.

## [3.17.0] - 2020-10-03
### Added
* Switch to Web Tools Platform release 3.17.0 for Eclipse 4.15.

## [3.16.0] - 2020-09-26
### Added
* Switch to Web Tools Platform release 3.16.0 for Eclipse 4.14.

## [3.15.3] - 2020-03-26
### Fixed
* Handling of character encodings on OS with non-unicode default file encoding format. CSS, HTML and JSON formatter steps encoded intermediately the input using the default file encoding, which was not lossless if the default file encoding did not support the full unicode character set. ([#545](https://github.com/diffplug/spotless/issues/545)).

## [3.15.2] - 2020-03-04
### Fixed
* Racing conditions in WTP formatter configurations. Multiple configurations within the same project are no longer supported. ([#492](https://github.com/diffplug/spotless/pull/492)).

## [3.15.1] - 2019-11-27
* Bugfix: Fix NPE in EclipseXmlFormatterStepImpl ([#490](https://github.com/diffplug/spotless/pull/490)).

## [3.15.0] - 2019-11-06
* Switch to Web Tools Platform release 3.15.0 for Eclipse 4.13 ([#480](https://github.com/diffplug/spotless/issues/480)).

## [3.14.0] - 2019-06-24
* Switch to Web Tools Platform release 3.14.0 for Eclipse 4.12 ([#423](https://github.com/diffplug/spotless/pull/423)).

## [3.10.0] - 2019-03-17
* Switch to Web Tools Platform release 3.10.0 for Eclipse 4.8 ([#378](https://github.com/diffplug/spotless/pull/378)).
* Include Eclipse logging allowing formatter warnings/errors to be logged via SLF4J ([#236](https://github.com/diffplug/spotless/issues/236)).

## [3.9.8] - 2019-03-10
* XML formatter ignores external URIs per default. ([#369](https://github.com/diffplug/spotless/issues/369)). Add `resolveExternalURI=true` property to switch to previous behavior.

## [3.9.7] - 2019-02-25
* Replaced `http` update-site with `https` ([#360](https://github.com/diffplug/spotless/issues/360)).

## [3.9.6] - 2019-02-11
* Fixed formatting of JSON arrays ([#344](https://github.com/diffplug/spotless/issues/344)).

## [3.9.5] - 2018-08-08
* Initial release!
