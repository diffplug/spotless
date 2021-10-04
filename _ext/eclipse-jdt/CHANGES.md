# spotless-eclipse-jdt

We adhere to the [keepachangelog](https://keepachangelog.com/en/1.0.0/) format (starting after version `4.8.0`).

## [Unreleased]

## [4.8.1] - 2021-10-04
### Changed
* Bumped minimum supported Eclipse JDT core version to 3.27.0
* `format` interface requires source file information to distinguish module-info from compilation unit. Old interface marked as deprecated.
### Fixed
* Fixed module-info formatting. Previous versions did not recognized content and skipped formatting.

## [4.8.0] - 2018-07-19
* Initial release!
