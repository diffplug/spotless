# spotless-lib and spotless-lib-extra releases

If you are a Spotless user (as opposed to developer), then you are probably looking for:

- [plugin-gradle/CHANGES.md](plugin-gradle/CHANGES.md)
- [plugin-maven/CHANGES.md](plugin-maven/CHANGES.md)

This document is intended for Spotless developers.

We adhere to the [keepachangelog](https://keepachangelog.com/en/1.0.0/) format (starting after version `1.27.0`).

## [Unreleased]
### Added
* Support for `idea` ([#2020](https://github.com/diffplug/spotless/pull/2020), [#2535](https://github.com/diffplug/spotless/pull/2535))
* Add support for removing wildcard imports via `removeWildcardImports` step. ([#2517](https://github.com/diffplug/spotless/pull/2517))

### Fixed
* Make sure npm-based formatters use the correct `node_modules` directory when running in parallel. ([#2542](https://github.com/diffplug/spotless/pull/2542))

### Changed
* Bump internal dependencies for npm-based formatters ([#2542](https://github.com/diffplug/spotless/pull/2542))

### Changed
* scalafmt: enforce version consistency between the version configured in Spotless and the version declared in Scalafmt config file ([#2460](https://github.com/diffplug/spotless/issues/2460))

## [3.1.2] - 2025-05-27
### Fixed
* Fix `UnsupportedOperationException` in the Gradle plugin when using `targetExcludeContent[Pattern]` ([#2487](https://github.com/diffplug/spotless/pull/2487))
* pgp key had expired, this and future releases will be signed by new key ([details](https://github.com/diffplug/spotless/discussions/2464))
### Changed
* Bump default `eclipse` version to latest `4.34` -> `4.35`. ([#2458](https://github.com/diffplug/spotless/pull/2458))
* Bump default `greclipse` version to latest `4.32` -> `4.35`. ([#2458](https://github.com/diffplug/spotless/pull/2458))

## [3.1.1] - 2025-04-07
### Changed
* Use palantir-java-format 2.57.0 on Java 21. ([#2447](https://github.com/diffplug/spotless/pull/2447))
* Re-try `npm install` with `--prefer-online` after `ERESOLVE` error. ([#2448](https://github.com/diffplug/spotless/pull/2448))
* Allow multiple npm-based formatters having the same module dependencies, to share a `node_modules` dir without race conditions. [#2462](https://github.com/diffplug/spotless/pull/2462))

## [3.1.0] - 2025-02-20
### Added
* Support for`clang-format` on maven-plugin ([#2406](https://github.com/diffplug/spotless/pull/2406))
* Allow overriding classLoader for all `JarState`s to enable spotless-cli ([#2427](https://github.com/diffplug/spotless/pull/2427))

## [3.0.2] - 2025-01-14
### Fixed
* Node.JS-based tasks now work with the configuration cache ([#2372](https://github.com/diffplug/spotless/issues/2372))
* Eclipse-based tasks can now handle parallel configuration ([#2389](https://github.com/diffplug/spotless/issues/2389))

## [3.0.1] - 2025-01-07
### Fixed
* Deployment was missing part of the CDT formatter, now fixed. ([#2384](https://github.com/diffplug/spotless/issues/2384))

## [3.0.0] - 2025-01-06
## Headline changes
* All steps now support roundtrip serialization (end of [#987](https://github.com/diffplug/spotless/issues/987)).
* Spotless now supports [linting](https://github.com/diffplug/spotless/blob/main/CONTRIBUTING.md#lints) in addition to formatting.
### Changed
* Allow setting Eclipse config from a string, not only from files ([#2337](https://github.com/diffplug/spotless/pull/2337))
* Bump default `ktlint` version to latest `1.3.0` -> `1.4.0`. ([#2314](https://github.com/diffplug/spotless/pull/2314))
* Add _Sort Members_ feature based on [Eclipse JDT](plugin-gradle/README.md#eclipse-jdt) implementation. ([#2312](https://github.com/diffplug/spotless/pull/2312))
* Bump default `jackson` version to latest `2.18.0` -> `2.18.1`. ([#2319](https://github.com/diffplug/spotless/pull/2319))
* Bump default `ktfmt` version to latest `0.52` -> `0.53`. ([#2320](https://github.com/diffplug/spotless/pull/2320))
* Bump default `ktlint` version to latest `1.4.0` -> `1.5.0`. ([#2354](https://github.com/diffplug/spotless/pull/2354))
* Bump minimum `eclipse-cdt` version to `11.0` (removed support for `10.7`). ([#2373](https://github.com/diffplug/spotless/pull/2373))
* Bump default `eclipse` version to latest `4.32` -> `4.34`. ([#2381](https://github.com/diffplug/spotless/pull/2381))

### Fixed
* You can now use `removeUnusedImports` and `googleJavaFormat` at the same time again. (fixes [#2159](https://github.com/diffplug/spotless/issues/2159))
* The default list of type annotations used by `formatAnnotations` now includes Jakarta Validation's `Valid` and constraints validations (fixes [#2334](https://github.com/diffplug/spotless/issues/2334))


## [3.0.0.BETA4] - 2024-10-24
### Added
* APIs to support linting. (implemented in [#2148](https://github.com/diffplug/spotless/pull/2148), [#2149](https://github.com/diffplug/spotless/pull/2149), [#2307](https://github.com/diffplug/spotless/pull/2307))
  * Spotless is still primarily a formatter, not a linter. But when formatting fails, it's more flexible to model those failures as lints so that the formatting can continue and ideally we can also capture the line numbers causing the failure.
  * `Lint` models a single change. A `FormatterStep` can create a lint by:
    * throwing an exception during formatting, ideally `throw Lint.atLine(127, "code", "Well what happened was...")`
    * or by implementing the `List<Lint> lint(String content, File file)` method to create multiple of them
* Support for line ending policy `PRESERVE` which just takes the first line ending of every given file as setting (no matter if `\n`, `\r\n` or `\r`) ([#2304](https://github.com/diffplug/spotless/pull/2304))
### Changed
* **BREAKING** Moved `PaddedCell.DirtyState` to its own top-level class with new methods. ([#2148](https://github.com/diffplug/spotless/pull/2148))
  * **BREAKING** Removed `isClean`, `applyTo`, and `applyToAndReturnResultIfDirty` from `Formatter` because users should instead use `DirtyState`.
* `FenceStep` now uses `ConfigurationCacheHack`. ([#2378](https://github.com/diffplug/spotless/pull/2378) fixes [#2317](https://github.com/diffplug/spotless/issues/2317))  
### Fixed
* `ktlint` steps now read from the `string` instead of the `file` so they don't clobber earlier steps. (fixes [#1599](https://github.com/diffplug/spotless/issues/1599))

## [3.0.0.BETA3] - 2024-10-15
### Added
* Support for `rdf` ([#2261](https://github.com/diffplug/spotless/pull/2261))
* Support for `buf` on maven plugin ([#2291](https://github.com/diffplug/spotless/pull/2291))
* `ConfigurationCacheHack` so we can support Gradle's configuration cache and remote build cache at the same time. ([#2298](https://github.com/diffplug/spotless/pull/2298) fixes [#2168](https://github.com/diffplug/spotless/issues/2168))
### Changed
* Support configuring the Equo P2 cache. ([#2238](https://github.com/diffplug/spotless/pull/2238))
* Add explicit support for JSONC / CSS via biome, via the file extensions `.css` and `.jsonc`. 
  ([#2259](https://github.com/diffplug/spotless/pull/2259))
* Bump default `buf` version to latest `1.24.0` -> `1.44.0`. ([#2291](https://github.com/diffplug/spotless/pull/2291))
* Bump default `google-java-format` version to latest `1.23.0` -> `1.24.0`. ([#2294](https://github.com/diffplug/spotless/pull/2294))
* Bump default `jackson` version to latest `2.17.2` -> `2.18.0`. ([#2279](https://github.com/diffplug/spotless/pull/2279))
* Bump default `cleanthat` version to latest `2.21` -> `2.22`. ([#2296](https://github.com/diffplug/spotless/pull/2296))
### Fixed
* Java import order, ignore duplicate group entries. ([#2293](https://github.com/diffplug/spotless/pull/2293))

## [3.0.0.BETA2] - 2024-08-25
### Changed
* Support toning down sortPom logging. ([#2185](https://github.com/diffplug/spotless/pull/2185))
* Bump default `ktlint` version to latest `1.2.1` -> `1.3.0`. ([#2165](https://github.com/diffplug/spotless/pull/2165))
* Bump default `ktfmt` version to latest `0.49` -> `0.52`. ([#2172](https://github.com/diffplug/spotless/pull/2172), [#2231](https://github.com/diffplug/spotless/pull/2231))
* Rename property `ktfmt` option `removeUnusedImport` -> `removeUnusedImports` to match `ktfmt`. ([#2172](https://github.com/diffplug/spotless/pull/2172))
* Bump default `eclipse` version to latest `4.29` -> `4.32`. ([#2179](https://github.com/diffplug/spotless/pull/2179))
* Bump default `greclipse` version to latest `4.29` -> `4.32`. ([#2179](https://github.com/diffplug/spotless/pull/2179), [#2190](https://github.com/diffplug/spotless/pull/2190))
* Bump default `cdt` version to latest `11.3` -> `11.6`. ([#2179](https://github.com/diffplug/spotless/pull/2179))
* Bump default `gson` version to latest `2.10.1` -> `2.11.0`. ([#2128](https://github.com/diffplug/spotless/pull/2128))
* Bump default `jackson` version to latest `2.17.1` -> `2.17.2`. ([#2195](https://github.com/diffplug/spotless/pull/2195))
* Bump default `cleanthat` version to latest `2.20` -> `2.21`. ([#2210](https://github.com/diffplug/spotless/pull/2210))
* Bump default `google-java-format` version to latest `1.22.0` -> `1.23.0`. ([#2212](https://github.com/diffplug/spotless/pull/2212))
### Fixed
* Fix compatibility issue introduced by `ktfmt` `0.51`. ([#2172](https://github.com/diffplug/spotless/issues/2172))
### Added
* Add option `manageTrailingCommas` to `ktfmt`. ([#2177](https://github.com/diffplug/spotless/pull/2177))

## [3.0.0.BETA1] - 2024-06-04
### Added
* `FileSignature.Promised` and `JarState.Promised` to facilitate round-trip serialization for the Gradle configuration cache. ([#1945](https://github.com/diffplug/spotless/pull/1945))
* Respect `.editorconfig` settings for formatting shell via `shfmt` ([#2031](https://github.com/diffplug/spotless/pull/2031))
### Fixed
* Check if ktlint_code_style is set in .editorconfig before overriding it ([#2143](https://github.com/diffplug/spotless/issues/2143))
* Ignore system git config when running tests ([#1990](https://github.com/diffplug/spotless/issues/1990))
* Correctly provide EditorConfig property types for Ktlint ([#2052](https://github.com/diffplug/spotless/issues/2052))
* Made ShadowCopy (`npmInstallCache`) more robust by re-creating the cache dir if it goes missing ([#1984](https://github.com/diffplug/spotless/issues/1984),[2096](https://github.com/diffplug/spotless/pull/2096))
* scalafmt.conf fileOverride section now works correctly ([#1854](https://github.com/diffplug/spotless/pull/1854))
* Fix stdin pipe is being closed exception on Windows for large .proto files ([#2147](https://github.com/diffplug/spotless/issues/2147))
* Reworked ShadowCopy (`npmInstallCache`) to use atomic filesystem operations, resolving several race conditions that could arise ([#2151](https://github.com/diffplug/spotless/pull/2151))
### Changed
* Bump default `cleanthat` version to latest `2.16` -> `2.20`. ([#1725](https://github.com/diffplug/spotless/pull/1725))
* Bump default `gherkin-utils` version to latest `8.0.2` -> `9.0.0`. ([#1703](https://github.com/diffplug/spotless/pull/1703))
* Bump default `google-java-format` version to latest `1.19.2` -> `1.22.0`. ([#2129](https://github.com/diffplug/spotless/pull/2129))
* Bump default `jackson` version to latest `2.14.2` -> `2.17.1`. ([#1685](https://github.com/diffplug/spotless/pull/1685))
* Bump default `ktfmt` version to latest `0.46` -> `0.49`. ([#2045](https://github.com/diffplug/spotless/pull/2045), [#2127](https://github.com/diffplug/spotless/pull/2127))
* Bump default `ktlint` version to latest `1.1.1` -> `1.2.1`. ([#2057](https://github.com/diffplug/spotless/pull/2057))
* Bump default `scalafmt` version to latest `3.7.3` -> `3.8.1`. ([#1730](https://github.com/diffplug/spotless/pull/1730))
* Bump default `shfmt` version to latest `3.7.0` -> `3.8.0`. ([#2050](https://github.com/diffplug/spotless/pull/2050))
* Bump default `sortpom` version to latest `3.2.1` -> `4.0.0`. ([#2049](https://github.com/diffplug/spotless/pull/2049), [#2078](https://github.com/diffplug/spotless/pull/2078), [#2115](https://github.com/diffplug/spotless/pull/2115))
* Bump default `zjsonpatch` version to latest `0.4.14` -> `0.4.16`. ([#1969](https://github.com/diffplug/spotless/pull/1969))
### Removed
* **BREAKING** Remove `FormatterStep.createNeverUpToDate` methods, they are available only in `testlib`. ([#2145](https://github.com/diffplug/spotless/pull/2145))
* **BREAKING** Remove `JarState.getMavenCoordinate(String prefix)`. ([#1945](https://github.com/diffplug/spotless/pull/1945))
* **BREAKING** Replace `PipeStepPair` with `FenceStep`. ([#1954](https://github.com/diffplug/spotless/pull/1954))
* **BREAKING** Fully removed `Rome`, use `Biome` instead. ([#2119](https://github.com/diffplug/spotless/pull/2119))
* **BREAKING** Moved `PaddedCell.DirtyState` to its own top-level class with new methods. ([#2148](https://github.com/diffplug/spotless/pull/2148))
  * **BREAKING** Removed `isClean`, `applyTo`, and `applyToAndReturnResultIfDirty` from `Formatter` because users should instead use `DirtyState`.

## [2.45.0] - 2024-01-23
### Added
* Support for `gofmt` ([#2001](https://github.com/diffplug/spotless/pull/2001))
* Support for formatting Java Docs for the Palantir formatter ([#2009](https://github.com/diffplug/spotless/pull/2009))

## [2.44.0] - 2024-01-15
### Added
* New static method to `DiffMessageFormatter` which allows to retrieve diffs with their line numbers ([#1960](https://github.com/diffplug/spotless/issues/1960))
* Gradle - Support for formatting shell scripts via [shfmt](https://github.com/mvdan/sh). ([#1994](https://github.com/diffplug/spotless/pull/1994))
### Fixed
* Fix empty files with biome >= 1.5.0 when formatting files that are in the ignore list of the biome configuration file. ([#1989](https://github.com/diffplug/spotless/pull/1989) fixes [#1987](https://github.com/diffplug/spotless/issues/1987))
* Fix a regression in BufStep where the same arguments were being provided to every `buf` invocation. ([#1976](https://github.com/diffplug/spotless/issues/1976))
### Changed
* Use palantir-java-format 2.39.0 on Java 21. ([#1948](https://github.com/diffplug/spotless/pull/1948))
* Bump default `ktlint` version to latest `1.0.1` -> `1.1.1`. ([#1973](https://github.com/diffplug/spotless/pull/1973))
* Bump default `googleJavaFormat` version to latest `1.18.1` -> `1.19.2`. ([#1971](https://github.com/diffplug/spotless/pull/1971))
* Bump default `diktat` version to latest `1.2.5` -> `2.0.0`. ([#1972](https://github.com/diffplug/spotless/pull/1972))

## [2.43.1] - 2023-12-04
### Fixed
* Eclipse-based steps which contained any jars with a `+` in their path were broken, now fixed. ([#1860](https://github.com/diffplug/spotless/issues/1860#issuecomment-1826113332))
### Changed
* Bump default `palantir-java-format` version to latest `2.28.0` -> `2.38.0` on Java 21. ([#1920](https://github.com/diffplug/spotless/pull/1920))
* Bump default `googleJavaFormat` version to latest `1.17.0` -> `1.18.1`. ([#1920](https://github.com/diffplug/spotless/pull/1920))
* Bump default `ktfmt` version to latest `0.44` -> `0.46`. ([#1927](https://github.com/diffplug/spotless/pull/1927))
* Bump default `eclipse` version to latest `4.27` -> `4.29`. ([#1939](https://github.com/diffplug/spotless/pull/1939))
* Bump default `greclipse` version to latest `4.28` -> `4.29`. ([#1939](https://github.com/diffplug/spotless/pull/1939))
* Bump default `cdt` version to latest `11.1` -> `11.3`. ([#1939](https://github.com/diffplug/spotless/pull/1939))

## [2.43.0] - 2023-11-27
### Added
* Support custom rule sets for Ktlint. ([#1896](https://github.com/diffplug/spotless/pull/1896))
### Fixed
* Fix Eclipse JDT on some settings files. ([#1864](https://github.com/diffplug/spotless/pull/1864) fixes [#1638](https://github.com/diffplug/spotless/issues/1638))
### Changed
* Bump default `ktlint` version to latest `1.0.0` -> `1.0.1`. ([#1855](https://github.com/diffplug/spotless/pull/1855))
* Add a Step to remove semicolons from Groovy files. ([#1881](https://github.com/diffplug/spotless/pull/1881))

## [2.42.0] - 2023-09-28
### Added
* Support for biome. The Rome project [was renamed to Biome](https://biomejs.dev/blog/annoucing-biome/).
  The configuration is still the same, but you should switch to the new `biome` tag / function and adjust
  the version accordingly. ([#1804](https://github.com/diffplug/spotless/issues/1804)).
* Support for `google-java-format`'s `skip-javadoc-formatting` option. ([#1793](https://github.com/diffplug/spotless/pull/1793))
* Support configuration of mirrors for P2 repositories in Maven DSL ([#1697](https://github.com/diffplug/spotless/issues/1697)).
* New line endings mode `GIT_ATTRIBUTES_FAST_ALLSAME`. ([#1838](https://github.com/diffplug/spotless/pull/1838))
### Fixed
* Fix support for plugins when using Prettier version `3.0.0` and newer. ([#1802](https://github.com/diffplug/spotless/pull/1802))
* Fix configuration cache issue around `external process started '/usr/bin/git --version'`. ([#1806](https://github.com/diffplug/spotless/issues/1806))
### Changed
* Bump default `flexmark` version to latest `0.64.0` -> `0.64.8`. ([#1801](https://github.com/diffplug/spotless/pull/1801))
* Bump default `ktlint` version to latest `0.50.0` -> `1.0.0`. ([#1808](https://github.com/diffplug/spotless/pull/1808))

## [2.41.0] - 2023-08-29
### Added
* Add a `jsonPatch` step to `json` formatter configurations. This allows patching of JSON documents using [JSON Patches](https://jsonpatch.com). ([#1753](https://github.com/diffplug/spotless/pull/1753))
* Support GJF own import order. ([#1780](https://github.com/diffplug/spotless/pull/1780))
### Fixed
* Use latest versions of popular style guides for `eslint` tests to fix failing `useEslintXoStandardRules` test. ([#1761](https://github.com/diffplug/spotless/pull/1761), [#1756](https://github.com/diffplug/spotless/issues/1756))
* Add support for `prettier` version `3.0.0` and newer. ([#1760](https://github.com/diffplug/spotless/pull/1760), [#1751](https://github.com/diffplug/spotless/issues/1751))
* Fix npm install calls when npm cache is not up-to-date. ([#1760](https://github.com/diffplug/spotless/pull/1760), [#1750](https://github.com/diffplug/spotless/issues/1750))
### Changed
* Bump default `eslint` version to latest `8.31.0` -> `8.45.0` ([#1761](https://github.com/diffplug/spotless/pull/1761))
* Bump default `prettier` version to latest (v2) `2.8.1` -> `2.8.8`. ([#1760](https://github.com/diffplug/spotless/pull/1760))
* Bump default `greclipse` version to latest `4.27` -> `4.28`. ([#1775](https://github.com/diffplug/spotless/pull/1775))

## [2.40.0] - 2023-07-17
### Added
* Added support for Protobuf formatting based on [Buf](https://buf.build/). (#1208)
* `enum OnMatch { INCLUDE, EXCLUDE }` so that `FormatterStep.filterByContent` can not only include based on the pattern but also exclude. ([#1749](https://github.com/diffplug/spotless/pull/1749))
### Fixed
* Update documented default `semanticSort` to `false`. ([#1728](https://github.com/diffplug/spotless/pull/1728))
### Changed
* Bump default `cleanthat` version to latest `2.13` -> `2.17`. ([#1734](https://github.com/diffplug/spotless/pull/1734))
* Bump default `ktlint` version to latest `0.49.1` -> `0.50.0`. ([#1741](https://github.com/diffplug/spotless/issues/1741))
  * Dropped support for `ktlint 0.47.x` following our policy of supporting two breaking changes at a time.
  * Dropped support for deprecated `useExperimental` parameter in favor of the `ktlint_experimental` property.

## [2.39.0] - 2023-05-24
### Added
* `Jvm.Support` now accepts `-SNAPSHOT` versions, treated as the non`-SNAPSHOT`. ([#1583](https://github.com/diffplug/spotless/issues/1583))
* Support Rome as a formatter for JavaScript and TypeScript code. Adds a new `rome` step to `javascript` and `typescript` formatter configurations. ([#1663](https://github.com/diffplug/spotless/pull/1663))
* Add semantics-aware Java import ordering (i.e. sort by package, then class, then member). ([#522](https://github.com/diffplug/spotless/issues/522))
### Fixed
* Fixed a regression which changed the import sorting order in `googleJavaFormat` introduced in `2.38.0`. ([#1680](https://github.com/diffplug/spotless/pull/1680))
* Equo-based formatters now work on platforms unsupported by Eclipse such as PowerPC (fixes [durian-swt#20](https://github.com/diffplug/durian-swt/issues/20))
* When P2 download fails, indicate the responsible formatter. ([#1698](https://github.com/diffplug/spotless/issues/1698))
### Changed
* Equo-based formatters now download metadata to `~/.m2/repository/dev/equo/p2-data` rather than `~/.equo`, and for CI machines without a home directory the p2 data goes to `$GRADLE_USER_HOME/caches/p2-data`. ([#1714](https://github.com/diffplug/spotless/pull/1714))
* Bump default `googleJavaFormat` version to latest `1.16.0` -> `1.17.0`. ([#1710](https://github.com/diffplug/spotless/pull/1710))
* Bump default `ktfmt` version to latest `0.43` -> `0.44`. ([#1691](https://github.com/diffplug/spotless/pull/1691))
* Bump default `ktlint` version to latest `0.48.2` -> `0.49.1`. ([#1696](https://github.com/diffplug/spotless/issues/1696))
  * Dropped support for `ktlint 0.46.x` following our policy of supporting two breaking changes at a time.
* Bump default `sortpom` version to latest `3.0.0` -> `3.2.1`. ([#1675](https://github.com/diffplug/spotless/pull/1675))

## [2.38.0] - 2023-04-06
### Added
* Support configuration of mirrors for P2 repositories in `EquoBasedStepBuilder` ([#1629](https://github.com/diffplug/spotless/issues/1629)).
* The `style` option in Palantir Java Format ([#1654](https://github.com/diffplug/spotless/pull/1654)).
* Added formatter for Gherkin feature files ([#1649](https://github.com/diffplug/spotless/issues/1649)).
### Changed
* **POTENTIALLY BREAKING** Converted `googleJavaFormat` to a compile-only dependency and drop support for versions &lt; `1.8`. ([#1630](https://github.com/diffplug/spotless/pull/1630))
* Bump default `cleanthat` version to latest `2.6` -> `2.13`. ([#1589](https://github.com/diffplug/spotless/pull/1589) and [#1661](https://github.com/diffplug/spotless/pull/1661))
* Bump default `diktat` version `1.2.4.2` -> `1.2.5`. ([#1631](https://github.com/diffplug/spotless/pull/1631))
* Bump default `flexmark` version `0.62.2` -> `0.64.0`. ([#1302](https://github.com/diffplug/spotless/pull/1302))
* Bump default `googleJavaFormat` version `1.15.0` -> `1.16.0`. ([#1630](https://github.com/diffplug/spotless/pull/1630))
* Bump default `scalafmt` version `3.7.1` -> `3.7.3`. ([#1584](https://github.com/diffplug/spotless/pull/1584))
* Bump default Eclipse formatters for the 2023-03 release. ([#1662](https://github.com/diffplug/spotless/pull/1662))
  * JDT and GrEclipse `4.26` -> `4.27`
    * Improve GrEclipse error reporting. ([#1660](https://github.com/diffplug/spotless/pull/1660))
  * CDT `11.0` -> `11.1`

## [2.37.0] - 2023-03-13
### Added
* You can now put the filename into a license header template with `$FILE`. ([#1605](https://github.com/diffplug/spotless/pull/1605) fixes [#1147](https://github.com/diffplug/spotless/issues/1147))
### Changed
* We are now opting in to Gradle's new stable configuration cache. ([#1591](https://github.com/diffplug/spotless/pull/1591))
* Adopt [Equo Solstice OSGi and p2 shim](https://github.com/equodev/equo-ide/tree/main/solstice) to update all Eclipse-based plugins. ([#1524](https://github.com/diffplug/spotless/pull/1524))
  * Eclipse JDT now supports `4.9` through `4.26`. Also we now recommend dropping the last `.0`, e.g. `4.26` instead of `4.26.0`, you'll get warnings to help you switch.
  * Eclipse Groovy now supports `4.18` through `4.26`. Also we now recommend dropping the last `.0`, e.g. `4.26` instead of `4.26.0`, you'll get warnings to help you switch.
  * Eclipse CDT now supports `10.6` through `11.0`.
  * Eclipse WTP is still WIP at [#1622](https://github.com/diffplug/spotless/pull/1622).

## [2.36.0] - 2023-02-27
### Added
* `gradlew equoIde` opens a repeatable clean Spotless dev environment. ([#1523](https://github.com/diffplug/spotless/pull/1523))
* `cleanthat` added `includeDraft` option, to include draft mutators from composite mutators. ([#1574](https://github.com/diffplug/spotless/pull/1574))
* `npm`-based formatters now support caching of `node_modules` directory ([#1590](https://github.com/diffplug/spotless/pull/1590))
### Fixed
* `JacksonJsonFormatterFunc` handles json files with an Array as root. ([#1585](https://github.com/diffplug/spotless/pull/1585))
### Changed
* Bump default `cleanthat` version to latest `2.1` -> `2.6` ([#1569](https://github.com/diffplug/spotless/pull/1569) and [#1574](https://github.com/diffplug/spotless/pull/1574))
* Reduce logging-noise created by `npm`-based formatters ([#1590](https://github.com/diffplug/spotless/pull/1590) fixes [#1582](https://github.com/diffplug/spotless/issues/1582))

## [2.35.0] - 2023-02-10
### Added
* CleanThat Java Refactorer. ([#1560](https://github.com/diffplug/spotless/pull/1560))
* Introduce `LazyArgLogger` to allow for lazy evaluation of log messages in slf4j logging. ([#1565](https://github.com/diffplug/spotless/pull/1565))
### Fixed
* Allow multiple instances of the same npm-based formatter to be used by separating their `node_modules` directories. ([#1565](https://github.com/diffplug/spotless/pull/1565))
* `ktfmt` default style uses correct continuation indent. ([#1562](https://github.com/diffplug/spotless/pull/1562))
### Changed
* Bump default `ktfmt` version to latest `0.42` -> `0.43` ([#1561](https://github.com/diffplug/spotless/pull/1561))
* Bump default `jackson` version to latest `2.14.1` -> `2.14.2` ([#1536](https://github.com/diffplug/spotless/pull/1536))

## [2.34.1] - 2023-02-05
### Changed
* **POTENTIALLY BREAKING** Bump bytecode from Java 8 to 11 ([#1530](https://github.com/diffplug/spotless/pull/1530) part 2 of [#1337](https://github.com/diffplug/spotless/issues/1337))
### Fixed
* **POTENTIALLY BREAKING** `sortByKeys` for JSON formatting now takes into account objects inside arrays ([#1546](https://github.com/diffplug/spotless/pull/1546))
* `freshmark` fixed on java 15+ ([#1304](https://github.com/diffplug/spotless/pull/1304) fixes [#803](https://github.com/diffplug/spotless/issues/803))

## [2.34.0] - 2023-01-26
### Added
* `Formatter` now has a field `public static final File NO_FILE_SENTINEL` which can be used to pass string content to a Formatter or FormatterStep when there is no actual File to format. ([#1525](https://github.com/diffplug/spotless/pull/1525))

## [2.33.0] - 2023-01-26
### Added
* `ProcessRunner` has added some convenience methods so it can be used for Maven testing. ([#1496](https://github.com/diffplug/spotless/pull/1496))
* `ProcessRunner` allows to limit captured output to a certain number of bytes. ([#1511](https://github.com/diffplug/spotless/pull/1511))
* `ProcessRunner` is now capable of handling long-running tasks where waiting for exit is delegated to the caller. ([#1511](https://github.com/diffplug/spotless/pull/1511))
* Allow to specify node executable for node-based formatters using `nodeExecutable` parameter ([#1500](https://github.com/diffplug/spotless/pull/1500))
### Fixed
* The default list of type annotations used by `formatAnnotations` has had 8 more annotations from the Checker Framework added [#1494](https://github.com/diffplug/spotless/pull/1494)
### Changed
* **POTENTIALLY BREAKING** Bump minimum JRE from 8 to 11, next release likely to bump bytecode to Java 11 ([#1514](https://github.com/diffplug/spotless/pull/1514) part 1 of [#1337](https://github.com/diffplug/spotless/issues/1337))
* Rename `YamlJacksonStep` into `JacksonYamlStep` while normalizing Jackson usage ([#1492](https://github.com/diffplug/spotless/pull/1492))
* Convert `gson` integration to use a compile-only source set ([#1510](https://github.com/diffplug/spotless/pull/1510)).
* ** POTENTIALLY BREAKING** Removed support for KtLint 0.3x and 0.45.2 ([#1475](https://github.com/diffplug/spotless/pull/1475))
  * `KtLint` does not maintain a stable API - before this PR, we supported every breaking change in the API since 2019.
  * From now on, we will support no more than 2 breaking changes at a time.
* NpmFormatterStepStateBase delays `npm install` call until the formatter is first used. This enables better integration
  with `gradle-node-plugin`. ([#1522](https://github.com/diffplug/spotless/pull/1522))
* Bump default `ktlint` version to latest `0.48.1` -> `0.48.2` ([#1529](https://github.com/diffplug/spotless/pull/1529))
* Bump default `scalafmt` version to latest `3.6.1` -> `3.7.1` ([#1529](https://github.com/diffplug/spotless/pull/1529))

## [2.32.0] - 2023-01-13
### Added
* Add option `editorConfigFile` for `ktLint` [#142](https://github.com/diffplug/spotless/issues/142)
  * **POTENTIALLY BREAKING** `ktlint` step now modifies license headers. Make sure to put `licenseHeader` *after* `ktlint`.
* Added `skipLinesMatching` option to `licenseHeader` to support formats where license header cannot be immediately added to the top of the file (e.g. xml, sh). ([#1441](https://github.com/diffplug/spotless/pull/1441)).
* Add YAML support through Jackson ([#1478](https://github.com/diffplug/spotless/pull/1478))
* Added support for npm-based [ESLint](https://eslint.org/)-formatter for javascript and typescript ([#1453](https://github.com/diffplug/spotless/pull/1453))
* Better suggested messages when user's default is set by JVM limitation. ([#995](https://github.com/diffplug/spotless/pull/995))
### Fixed
* Support `ktlint` 0.48+ new rule disabling syntax ([#1456](https://github.com/diffplug/spotless/pull/1456)) fixes ([#1444](https://github.com/diffplug/spotless/issues/1444))
* Fix subgroups leading catch all matcher.
### Changed
* Bump default version for `prettier` from `2.0.5` to `2.8.1` ([#1453](https://github.com/diffplug/spotless/pull/1453))
* Bump the dev version of Gradle from `7.5.1` to `7.6` ([#1409](https://github.com/diffplug/spotless/pull/1409))
  * We also removed the no-longer-required dependency `org.codehaus.groovy:groovy-xml`
* Breaking changes to Spotless' internal testing infrastructure `testlib` ([#1443](https://github.com/diffplug/spotless/pull/1443))
  * `ResourceHarness` no longer has any duplicated functionality which was also present in `StepHarness`
  * `StepHarness` now operates on `Formatter` rather than a `FormatterStep`
  * `StepHarnessWithFile` now takes a `ResourceHarness` in its constructor to handle the file manipulation parts
  * Standardized that we test exception *messages*, not types, which will ease the transition to linting later on
  * Bump default `ktlint` version to latest `0.47.1` -> `0.48.1` ([#1456](https://github.com/diffplug/spotless/pull/1456))
* Switch our publishing infrastructure from CircleCI to GitHub Actions ([#1462](https://github.com/diffplug/spotless/pull/1462)).
  * Help wanted for moving our tests too ([#1472](https://github.com/diffplug/spotless/issues/1472))

## [2.31.1] - 2023-01-02
### Fixed
* Improve memory usage when using git ratchet ([#1426](https://github.com/diffplug/spotless/pull/1426))
* Support `ktlint` 0.48+ ([#1432](https://github.com/diffplug/spotless/pull/1432)) fixes ([#1430](https://github.com/diffplug/spotless/issues/1430))
### Changed
* Bump default `ktlint` version to latest `0.47.1` -> `0.48.0` ([#1432](https://github.com/diffplug/spotless/pull/1432))
* Bump default `ktfmt` version to latest `0.41` -> `0.42` ([#1421](https://github.com/diffplug/spotless/pull/1421))

## [2.31.0] - 2022-11-24
### Added
* `importOrder` now support groups of imports without blank lines ([#1401](https://github.com/diffplug/spotless/pull/1401))
### Fixed
* Don't treat `@Value` as a type annotation [#1367](https://github.com/diffplug/spotless/pull/1367)
* Support `ktlint_disabled_rules` in `ktlint` 0.47.x [#1378](https://github.com/diffplug/spotless/pull/1378)
* Share git repositories across projects when using ratchet ([#1426](https://github.com/diffplug/spotless/pull/1426))
### Changed
* Bump default `ktfmt` version to latest `0.40` -> `0.41` ([#1340](https://github.com/diffplug/spotless/pull/1340))
* Bump default `scalafmt` version to latest `3.5.9` -> `3.6.1` ([#1373](https://github.com/diffplug/spotless/pull/1373))
* Bump default `diktat` version to latest `1.2.3` -> `1.2.4.2` ([#1393](https://github.com/diffplug/spotless/pull/1393))
* Bump default `palantir-java-format` version to latest `2.10` -> `2.28` ([#1393](https://github.com/diffplug/spotless/pull/1393))

## [2.30.0] - 2022-09-14
### Added
* `formatAnnotations()` step to correct formatting of Java type annotations.  It puts type annotations on the same line as the type that they qualify.  Run it after a Java formatting step, such as `googleJavaFormat()`. ([#1275](https://github.com/diffplug/spotless/pull/1275))
### Changed
* Bump default `ktfmt` version to latest `0.39` -> `0.40` ([#1312](https://github.com/diffplug/spotless/pull/1312))
* Bump default `ktlint` version to latest `0.46.1` -> `0.47.1` ([#1303](https://github.com/diffplug/spotless/pull/1303))
  * Also restored support for older versions of ktlint back to `0.31.0`

## [2.29.0] - 2022-08-23
### Added
* `scalafmt` integration now has a configuration option `majorScalaVersion` that allows you to configure the Scala version that gets resolved from the Maven artifact ([#1283](https://github.com/diffplug/spotless/pull/1283))
  * Converted `scalafmt` integration to use a compile-only source set (fixes [#524](https://github.com/diffplug/spotless/issues/524))
### Changed
* Add the `ktlint` rule in error messages when `ktlint` fails to apply a fix ([#1279](https://github.com/diffplug/spotless/pull/1279))
* Bump default `scalafmt` to latest `3.0.8` -> `3.5.9` (removed support for pre-`3.0.0`) ([#1283](https://github.com/diffplug/spotless/pull/1283))

## [2.28.1] - 2022-08-10
### Fixed
* Fix Clang not knowing the filename and changing the format ([#1268](https://github.com/diffplug/spotless/pull/1268) fixes [#1267](https://github.com/diffplug/spotless/issues/1267)).
### Changed
* Bump default `diktat` version to latest `1.2.1` -> `1.2.3` ([#1266](https://github.com/diffplug/spotless/pull/1266))

## [2.28.0] - 2022-07-28
### Added
* Clang and Black no longer break the build when the binary is unavailable, if they will not be run during that build ([#1257](https://github.com/diffplug/spotless/pull/1257)).
* License header support for Kotlin files without `package` or `@file` but do at least have `import` ([#1263](https://github.com/diffplug/spotless/pull/1263)).

## [2.27.0] - 2022-06-30
### Added
* Support for `MAC_CLASSIC` (`\r`) line ending ([#1243](https://github.com/diffplug/spotless/pull/1243) fixes [#1196](https://github.com/diffplug/spotless/issues/1196))
### Changed
* Bump default `ktlint` version to latest `0.45.2` -> `0.46.1` ([#1239](https://github.com/diffplug/spotless/issues/1239))
  * Minimum supported version also bumped to `0.46.0` (we have abandoned strong backward compatibility for `ktlint`, from here on out Spotless will only support the most-recent breaking change).
* Bump default `diktat` version to latest `1.1.0` -> `1.2.1` ([#1246](https://github.com/diffplug/spotless/pull/1246))
  * Minimum supported version also bumped to `1.2.1` (diktat is based on ktlint and has the same backward compatibility issues).
* Bump default `ktfmt` version to latest `0.37` -> `0.39` ([#1240](https://github.com/diffplug/spotless/pull/1240))

## [2.26.2] - 2022-06-11
### Fixed
* `PalantirJavaFormatStep` no longer needs the `--add-exports` calls in the `org.gradle.jvmargs` property in `gradle.properties`. ([#1233](https://github.com/diffplug/spotless/pull/1233))

## [2.26.1] - 2022-06-10
### Fixed
* (Second try) `googleJavaFormat` and `removeUnusedImports` works on JDK16+ without jvm args workaround. ([#1228](https://github.com/diffplug/spotless/pull/1228))

## [2.26.0] - 2022-06-05
### Added
* Support for `editorConfigOverride` in `ktlint`. ([#1218](https://github.com/diffplug/spotless/pull/1218) fixes [#1193](https://github.com/diffplug/spotless/issues/1193))
### Fixed
* `google-java-format` and `RemoveUnusedImportsStep` works on JDK16+ without jvm args workaround. ([#1224](https://github.com/diffplug/spotless/pull/1224) fixes [#834](https://github.com/diffplug/spotless/issues/834))

## [2.25.3] - 2022-05-10
### Fixed
* Update the `black` version regex to fix `19.10b0` and earlier. (fixes [#1195](https://github.com/diffplug/spotless/issues/1195), regression introduced in `2.25.0`)
* `GitAttributesLineEndings$RelocatablePolicy` and `FormatterStepImpl` now null-out their initialization lambdas after their state has been calculated, which allows GC to collect variables which were incidentally captured but not needed in the calculated state. ([#1198](https://github.com/diffplug/spotless/pull/1198))
### Changed
* Bump default `ktfmt` version to latest `0.36` -> `0.37`. ([#1200](https://github.com/diffplug/spotless/pull/1200))

## [2.25.2] - 2022-05-03
### Changed
* Bump default `diktat` version to latest `1.0.1` -> `1.1.0`. ([#1190](https://github.com/diffplug/spotless/pull/1190))
  * Converted `diktat` integration to use a compile-only source set. (fixes [#524](https://github.com/diffplug/spotless/issues/524))
  * Use the full path to a file in `diktat` integration. (fixes [#1189](https://github.com/diffplug/spotless/issues/1189))

## [2.25.1] - 2022-04-27
### Changed
* Bump default `ktfmt` version to latest `0.35` -> `0.36`. ([#1183](https://github.com/diffplug/spotless/issues/1183))
* Bump default `google-java-format` version to latest `1.13.0` -> `1.15.0`.
  * ~~This means it is no longer necessary to use the `--add-exports` workaround (fixes [#834](https://github.com/diffplug/spotless/issues/834)).~~ `--add-exports` workaround is still needed.

## [2.25.0] - 2022-04-22
### Added
* Added support for enabling ktlint experimental ruleset. ([#1145](https://github.com/diffplug/spotless/pull/1168))
### Fixed
* Fixed support for Python Black's new version reporting. ([#1170](https://github.com/diffplug/spotless/issues/1170))
* Error messages for unexpected file encoding now works on Java 8. (fixes [#1081](https://github.com/diffplug/spotless/issues/1081))
### Changed
* Bump default `black` version to latest `19.10b0` -> `22.3.0`. ([#1170](https://github.com/diffplug/spotless/issues/1170))
* Bump default `ktfmt` version to latest `0.34` -> `0.35`. ([#1159](https://github.com/diffplug/spotless/pull/1159))
* Bump default `ktlint` version to latest `0.43.2` -> `0.45.2`. ([#1177](https://github.com/diffplug/spotless/pull/1177))

## [2.24.2] - 2022-04-06
### Fixed
* Git user config and system config also included for defaultEndings configuration. ([#540](https://github.com/diffplug/spotless/issues/540))

## [2.24.1] - 2022-03-30
### Fixed
* Fixed access modifiers for setters in KtfmtStep configuration

## [2.24.0] - 2022-03-28
### Added
* Added support for setting custom parameters for Kotlin ktfmt in Gradle and Maven plugins. ([#1145](https://github.com/diffplug/spotless/pull/1145))

## [2.23.0] - 2022-02-15
### Added
* Added support for JSON formatting based on [Gson](https://github.com/google/gson) ([#1125](https://github.com/diffplug/spotless/pull/1125)).
### Changed
* Use SLF4J for logging ([#1116](https://github.com/diffplug/spotless/issues/1116))

## [2.22.2] - 2022-02-09
### Changed
* Bump default ktfmt `0.30` -> `0.31` ([#1118](https://github.com/diffplug/spotless/pull/1118)).
### Fixed
* Add full support for git worktrees ([#1119](https://github.com/diffplug/spotless/pull/1119)).

## [2.22.1] - 2022-02-01
### Changed
* Bump CI from Java 15 to 17 ([#1094](https://github.com/diffplug/spotless/pull/1094)).
* Bump default versions of formatters ([#1095](https://github.com/diffplug/spotless/pull/1095)).
  * google-java-format `1.12.0` -> `1.13.0`
  * ktfmt `0.29` -> `0.30`
* Added support for git property `core.autocrlf` ([#540](https://github.com/diffplug/spotless/issues/540))

## [2.22.0] - 2022-01-13
### Added
* Added support for the [palantir-java-format](https://github.com/palantir/palantir-java-format) Java formatter ([#1083](https://github.com/diffplug/spotless/pull/1083)).

## [2.21.2] - 2022-01-07
### Fixed
* Update IndentStep to allow leading space on multiline comments ([#1072](https://github.com/diffplug/spotless/pull/1072)).

## [2.21.1] - 2022-01-06
### Changed
* Bumped default DiKTat from `0.4.0` to `1.0.1`. This is a breaking change for DiKTat users on the default version, because some rules were renamed/changed. Check [DiKTat changelog](https://github.com/analysis-dev/diktat/releases) for details.

## [2.21.0] - 2021-12-23
### Added
* Added support for Markdown with `flexmark` at `0.62.2` ([#1011](https://github.com/diffplug/spotless/pull/1011)).

## [2.20.3] - 2021-12-15
### Fixed
* Performance improvements to GitRatchet ([#1038](https://github.com/diffplug/spotless/pull/1038)).

## [2.20.2] - 2021-12-05
### Changed
* Bumped default ktlint from `0.43.0` to `0.43.2`.
* Converted `ktlint` integration to use a compile-only source set. ([#524](https://github.com/diffplug/spotless/issues/524))

## [2.20.1] - 2021-12-01
### Changed
* Added `named` option to `licenseHeader` to support alternate license header within same format (like java) ([872](https://github.com/diffplug/spotless/issues/872)).
* Added `onlyIfContentMatches` option to `licenseHeader` to skip license header application  based on source file content pattern ([#650](https://github.com/diffplug/spotless/issues/650)).
* Bump jgit version ([#992](https://github.com/diffplug/spotless/pull/992)).
  * jgit `5.10.0.202012080955-r` -> `5.13.0.202109080827-r`

## [2.20.0] - 2021-11-09
### Added
* `DiffMessageFormatter` can now generate messages based on a folder of cleaned files, as an alternative to a `Formatter` ([#982](https://github.com/diffplug/spotless/pull/982)).
### Fixed
* Fix CI and various spotbugs nits ([#988](https://github.com/diffplug/spotless/pull/988)).
### Changed
* Bump default formatter versions ([#989](https://github.com/diffplug/spotless/pull/989))
  * google-java-format `1.11.0` -> `1.12.0`
  * ktlint `0.42.1` -> `0.43.0`
  * ktfmt `0.27` -> `0.29`
  * scalafmt `3.0.0` -> `3.0.8`

## [2.19.2] - 2021-10-26
### Changed
* Added support and bump Eclipse formatter default versions to `4.21` for `eclipse-groovy`. Change is only applied for JVM 11+.
* Added support for ktlint's FilenameRule ([#974](https://github.com/diffplug/spotless/pull/974)).

### Fixed
 * Temporary workspace deletion for Eclipse based formatters on JVM shutdown ([#967](https://github.com/diffplug/spotless/issues/967)). Change is only applied for Eclipse versions using JVM 11+, no back-port to older versions is planned.

## [2.19.1] - 2021-10-13
### Fixed
 * [module-info formatting](https://github.com/diffplug/spotless/pull/958) in `eclipse-jdt` versions `4.20` and `4.21`. Note that the problem also affects older versions.
 * Added workaround to support projects using git worktrees ([#965](https://github.com/diffplug/spotless/pull/965))

## [2.19.0] - 2021-10-02
* Added `wildcardsLast` option for Java `ImportOrderStep` ([#954](https://github.com/diffplug/spotless/pull/954))

### Added
* Added support for JBDI bind list params in sql formatter ([#955](https://github.com/diffplug/spotless/pull/955))

## [2.18.0] - 2021-09-30
### Added
* Added support for custom JSR223 formatters ([#945](https://github.com/diffplug/spotless/pull/945))
* Added support for formatting and sorting Maven POMs ([#946](https://github.com/diffplug/spotless/pull/946))

## [2.17.0] - 2021-09-27
### Added
* Added support for calling local binary formatters ([#949](https://github.com/diffplug/spotless/pull/949))
### Changed
* Added support and bump Eclipse formatter default versions to `4.21` for `eclipse-cdt`, `eclipse-jdt`, `eclipse-wtp`. Change is only applied for JVM 11+.
* Added `groupArtifact` option for `google-java-format` ([#944](https://github.com/diffplug/spotless/pull/944))

## [2.16.1] - 2021-09-20
### Changed
* Added support and bump Eclipse formatter default versions for JVM 11+. For older JVMs the previous defaults remain.
  * `eclipse-cdt` from `4.16` to `4.20`
  * `eclipse-groovy` from `4.19` to `4.20`
  * `eclipse-jdt` from `4.19` to `4.20`
  * `eclipse-wtp` from `4.18` to `4.20`

## [2.16.0] - 2021-09-04
### Added
* Added support for `google-java-format`'s `skip-reflowing-long-strings` option ([#929](https://github.com/diffplug/spotless/pull/929))

## [2.15.3] - 2021-08-20
### Changed
* Added support for [scalafmt 3.0.0](https://github.com/scalameta/scalafmt/releases/tag/v3.0.0) and bump default scalafmt version to `3.0.0` ([#913](https://github.com/diffplug/spotless/pull/913)).
* Bump default versions ([#915](https://github.com/diffplug/spotless/pull/915))
  * `ktfmt` from `0.24` to `0.27`
  * `ktlint` from `0.35.0` to `0.42.1`
  * `google-java-format` from `1.10.0` to `1.11.0`
* Fix javadoc publishing ([#916](https://github.com/diffplug/spotless/pull/916) fixes [#775](https://github.com/diffplug/spotless/issues/775)).

## [2.15.2] - 2021-07-20
### Fixed
 * Improved [SQL formatting](https://github.com/diffplug/spotless/pull/897) with respect to comments

## [2.15.1] - 2021-07-06
### Changed
* Improved exception messages for [JSON formatting](https://github.com/diffplug/spotless/pull/885) failures

## [2.15.0] - 2021-06-17

### Added
* Added formatter for [JVM-based JSON formatting](https://github.com/diffplug/spotless/issues/850)
* Added Gradle configuration JVM-based JSON formatting

## [2.14.0] - 2021-06-10
### Added
* Added support for `eclipse-cdt` at `4.19.0`. Note that version requires Java 11 or higher.
* Added support for `eclipse-groovy` at `4.18.0` and `4.19.0`.
* Added support for `eclipse-wtp` at `4.19.0`. Note that version requires Java 11 or higher.
### Changed
* Bump `eclipse-groovy` default version from `4.17.0` to `4.19.0`.

## [2.13.5] - 2021-05-13
### Changed
* Update ktfmt from 0.21 to 0.24
### Fixed
* The `<url>` field in the Maven POM is now set correctly ([#798](https://github.com/diffplug/spotless/issues/798))
* Node is re-installed if some other build step removed it ([#863](https://github.com/diffplug/spotless/issues/863))

## [2.13.4] - 2021-04-21
### Fixed
* Explicitly separate target file from git arguments when parsing year for license header to prevent command from failing on argument-like paths ([#847](https://github.com/diffplug/spotless/pull/847))

## [2.13.3] - 2021-04-20
### Fixed
* LicenseHeaderStep treats address as copyright year ([#716](https://github.com/diffplug/spotless/issues/716))

## [2.13.2] - 2021-04-12
### Fixed
* Fix license header bug for years in range ([#840](https://github.com/diffplug/spotless/pull/840)).

## [2.13.1] - 2021-04-10
### Changed
* Update default google-java-format from 1.9 to 1.10.0
* Expose configuration exceptions from scalafmt ([#837](https://github.com/diffplug/spotless/issues/837))

## [2.13.0] - 2021-03-05
### Added
* Bump ktfmt to 0.21 and add support to Google and Kotlinlang formats ([#812](https://github.com/diffplug/spotless/pull/812))

## [2.12.1] - 2021-02-16
### Fixed
* Allow licence headers to be blank ([#801](https://github.com/diffplug/spotless/pull/801)).

## [2.12.0] - 2021-02-09
### Added
* Support for diktat ([#789](https://github.com/diffplug/spotless/pull/789))

## [2.11.0] - 2021-01-04
### Added
* Added support for `eclipse-cdt`, `eclipse-jdt`, and `eclipse-wtp` at `4.18.0`.
### Changed
* Bump `eclipse-jdt` default version from `4.17.0` to `4.18.0`.
* Bump `eclipse-wtp` default version from `4.17.0` to `4.18.0`.
* Bump `ktfmt` default version from `0.16` to `0.19` ([#748](https://github.com/diffplug/spotless/issues/748) and [#773](https://github.com/diffplug/spotless/issues/773)).
* Bump `jgit` from `5.9` to `5.10` ([#773](https://github.com/diffplug/spotless/issues/773)).
### Fixed
* Fixed `ratchetFrom` support for git-submodule ([#746](https://github.com/diffplug/spotless/issues/746)).
* Fixed `ratchetFrom` excess memory consumption ([#735](https://github.com/diffplug/spotless/issues/735)).
* `ktfmt` v0.19+ with dropbox-style works again ([#765](https://github.com/diffplug/spotless/pull/765)).
* `prettier` no longer throws errors on empty files ([#751](https://github.com/diffplug/spotless/pull/751)).
* Fixed error when running on root of windows mountpoint ([#760](https://github.com/diffplug/spotless/pull/760)).
* Fixed typo in javadoc comment for SQL\_FORMATTER\_INDENT\_TYPE ([#753](https://github.com/diffplug/spotless/pull/753)).

## [2.10.2] - 2020-11-16
### Fixed
* Fixed a bug which occurred if the root directory of the project was also the filesystem root ([#732](https://github.com/diffplug/spotless/pull/732))

## [2.10.1] - 2020-11-13
### Fixed
* Bump JGit from `5.8.0` to `5.9.0` to improve performance ([#726](https://github.com/diffplug/spotless/issues/726))

## [2.10.0] - 2020-11-02
### Added
* Added support to npm-based steps for picking up `.npmrc` files ([#727](https://github.com/diffplug/spotless/pull/727))

## [2.9.0] - 2020-10-20
### Added
* Added support for eclipse-cdt 4.14.0, 4.16.0 and 4.17.0 ([#722](https://github.com/diffplug/spotless/pull/722)).
* Added support for eclipse-groovy 4.14.0, 4.15.0, 4.16.0 and 4.17.0 ([#722](https://github.com/diffplug/spotless/pull/722)).
* Added support for eclipse-jdt 4.17.0 ([#722](https://github.com/diffplug/spotless/pull/722)).
* Added support for eclipse-wtp 4.14.0, 4.15.0, 4.16.0 and 4.17.0 ([#722](https://github.com/diffplug/spotless/pull/722)).
### Changed
* Updated default eclipse-cdt from 4.13.0 to 4.16.0 ([#722](https://github.com/diffplug/spotless/pull/722)). Note that version 4.17.0 is supported, but requires Java 11 or higher.
* Updated default eclipse-groovy from 4.13.0 to 4.17.0 ([#722](https://github.com/diffplug/spotless/pull/722)).
* Updated default eclipse-jdt from 4.16.0 to 4.17.0 ([#722](https://github.com/diffplug/spotless/pull/722)).
* Updated default eclipse-wtp from 4.13.0 to 4.17.0 ([#722](https://github.com/diffplug/spotless/pull/722)).

## [2.8.0] - 2020-10-05
### Added
* Exposed new methods in `GitRatchet` to support faster ratcheting in the Maven plugin ([#706](https://github.com/diffplug/spotless/pull/706)).

## [2.7.0] - 2020-09-21
### Added
* `PipeStepPair.Builder` now has a method `.buildStepWhichAppliesSubSteps(Path rootPath, Collection<FormatterStep> steps)`, which returns a single `FormatterStep` that applies the given steps within the regex defined earlier in the builder. Used for formatting inception (implements [#412](https://github.com/diffplug/spotless/issues/412)).

## [2.6.2] - 2020-09-18
### Fixed
* Don't assume that file content passed into Prettier is at least 50 characters (https://github.com/diffplug/spotless/pull/699).

## [2.6.1] - 2020-09-12
### Fixed
* Improved JRE parsing to handle strings like `16-loom` (fixes [#693](https://github.com/diffplug/spotless/issues/693)).

## [2.6.0] - 2020-09-11
### Added
* `PipeStepPair` which allows extracting blocks of text in one step, then injecting those blocks back in later. Currently only used for `spotless:off` `spotless:on`, but could also be used to [apply different steps in different places](https://github.com/diffplug/spotless/issues/412) ([#691](https://github.com/diffplug/spotless/pull/691)).
### Changed
* When applying license headers for the first time, we are now more lenient about parsing existing years from the header ([#690](https://github.com/diffplug/spotless/pull/690)).

## [2.5.0] - 2020-09-08
### Added
* `GoogleJavaFormatStep.defaultVersion()` now returns `1.9` on JDK 11+, while continuing to return `1.7` on earlier JDKs. This is especially helpful to `RemoveUnusedImportsStep`, since it always uses the default version of GJF (fixes [#681](https://github.com/diffplug/spotless/issues/681)).
### Fixed
* We now run all tests against JDK 8, JDK 11, and also JDK 14 ([#684](https://github.com/diffplug/spotless/pull/684)).
* We had test files in `testlib/src/main/resources` named `module-info.java` and `package-info.java`. They cause problems for the Eclipse IDE trying to interpret them literally. Added `.test` suffix to the filenames so that eclipse doesn't barf on them anymore ([#683](https://github.com/diffplug/spotless/pull/683)).

## [2.4.0] - 2020-08-29
### Added
* Added support for  eclipse-jdt 4.14.0, 4.15.0 and 4.16.0 ([#678](https://github.com/diffplug/spotless/pull/678)).
### Changed
* Updated default eclipse-jdt from 4.13.0 to 4.16.0 ([#678](https://github.com/diffplug/spotless/pull/678)).

## [2.3.0] - 2020-08-25
### Added
* The ability to shell out to formatters with their own executables. ([#672](https://github.com/diffplug/spotless/pull/672))
  * `ProcessRunner` makes it easy to efficiently and debuggably call foreign executables, and pipe their stdout and stderr to strings.
  * `ForeignExe` finds executables on the path (or other strategies), and confirms that they have the correct version (to facilitate Spotless' caching). If the executable is not present or the wrong version, it points the user towards how to fix the problem.
  * These classes were used to add support for [python black](https://github.com/psf/black) and [clang-format](https://clang.llvm.org/docs/ClangFormat.html).
  * Incidental to this effort, `FormatterFunc.Closeable` now has new methods which make resource-handling safer.  The old method is still available as `ofDangerous`, but it should not be used outside of a testing situation. There are some legacy usages of `ofDangerous` in the codebase, and it would be nice to fix them, but the existing usages are using it safely.

## [2.2.2] - 2020-08-21
### Fixed
* `KtLintStep` is now more robust when parsing version string for version-dependent implementation details, fixes [#668](https://github.com/diffplug/spotless/issues/668).

## [2.2.1] - 2020-08-05
### Fixed
* `FormatterFunc.Closeable` had a "use after free" bug which caused errors in the npm-based formatters (e.g. prettier) if `spotlessCheck` was called on dirty files. ([#651](https://github.com/diffplug/spotless/issues/651))
### Changed
* Bump default ktfmt from `0.15` to `0.16`, and remove duplicated logic for the `--dropbox-style` option ([#642](https://github.com/diffplug/spotless/pull/648))

## [2.2.0] - 2020-07-13
### Added
* Bump default ktfmt from 0.13 to 0.15, and add support for the --dropbox-style option ([#641](https://github.com/diffplug/spotless/issues/641)).

## [2.1.0] - 2020-07-04
### Added
* `FileSignature.machineIsWin()`, to replace the now-deprecated `LineEnding.nativeIsWin()`, because it turns out that `\r\n` is [not a reliable way](https://github.com/diffplug/spotless/issues/559#issuecomment-653739898) to detect the windows OS ([#639](https://github.com/diffplug/spotless/pull/639)).
### Fixed
* `GitAttributesLineEndings` was fatally broken (always returned platform default), and our tests missed it because they tested the part before the broken part ([#639](https://github.com/diffplug/spotless/pull/639)).

## [2.0.0] - 2020-07-02
### Changed
* `LineEnding.GIT_ATTRIBUTES` now creates a policy whose serialized state can be relocated from one machine to another.  No user-visible change, but paves the way for remote build cache support in Gradle. ([#621](https://github.com/diffplug/spotless/pull/621))
### Added
* `prettier` will now autodetect the parser (and formatter) to use based on the filename, unless you override this using `config` or `configFile` with the option `parser` or `filepath`. ([#620](https://github.com/diffplug/spotless/pull/620))
* `GitRatchet` now lives in `lib-extra`, and is shared across `plugin-gradle` and `plugin-maven` ([#626](https://github.com/diffplug/spotless/pull/626)).
* Added ANTLR4 support ([#326](https://github.com/diffplug/spotless/issues/326)).
* `FormatterFunc.NeedsFile` for implementing file-based formatters more cleanly than we have so far ([#637](https://github.com/diffplug/spotless/issues/637)).
### Changed
* **BREAKING** `FileSignature` can no longer sign folders, only files.  Signatures are now based only on filename (not path), size, and a content hash.  It throws an error if a signature is attempted on a folder or on multiple files with different paths but the same filename - it never breaks silently.  This change does not break any of Spotless' internal logic, so it is unlikely to affect any of Spotless' consumers either. ([#571](https://github.com/diffplug/spotless/pull/571))
  * This change allows the Maven plugin to cache classloaders across subprojects when loading config resources from the classpath (fixes [#559](https://github.com/diffplug/spotless/issues/559)).
  * This change also allows the Gradle plugin to work with the remote buildcache (fixes [#280](https://github.com/diffplug/spotless/issues/280)).
* **BREAKING** `FormatterFunc` no longer `extends ThrowingEx.Function` and `BiFunction`. In a major win for Java's idea of ["target typing"](https://cr.openjdk.java.net/~briangoetz/lambda/lambda-state-final.html), this required no changes anywhere in the codebase except deleting the `extends` part of `FormatterFunc` ([#638](https://github.com/diffplug/spotless/issues/638)).
* **BREAKING** Heavy refactor of the `LicenseHeaderStep` public API.  Doesn't change internal behavior, but makes implementation of the Gradle and Maven plugins much easier. ([#628](https://github.com/diffplug/spotless/pull/628))
* **BREAKING** Removed all deprecated methods and classes from `lib` and `lib-extra`.
  * [#629](https://github.com/diffplug/spotless/pull/629) removes the code which wasn't being used in plugin-gradle or plugin-maven.
  * [#630](https://github.com/diffplug/spotless/pull/630) moves the code which was still being used in deprecated parts of plugin-gradle into `.deprecated` package in `plugin-gradle`, which allows us to break `lib` without a breaking change in `plugin-gradle`.

## [1.34.1] - 2020-06-17
### Changed
* Nodejs-based formatters `prettier` and `tsfmt` now use native node instead of the J2V8 approach. ([#606](https://github.com/diffplug/spotless/pull/606))
  * This removes the dependency to the no-longer-maintained Linux/Windows/macOs variants of J2V8.
  * This enables spotless to use the latest `prettier` versions (instead of being stuck at prettier version <= `1.19.0`)
  * Bumped default versions, prettier `1.16.4` -> `2.0.5`, tslint `5.12.1` -> `6.1.2`
* Our main branch is now called `main`. ([#613](https://github.com/diffplug/spotless/pull/613))

## [1.34.0] - 2020-06-05
### Added
* `LicenseHeaderStep.setLicenseHeaderYearsFromGitHistory`, which does an expensive search through git history to determine the oldest and newest commits for each file, and uses that to determine license header years. ([#604](https://github.com/diffplug/spotless/pull/604))

## [1.33.1] - 2020-06-04
* We are now running CI on windows. ([#596](https://github.com/diffplug/spotless/pull/596))
* We are now dogfooding `ratchetFrom` and `licenseHeader` with a `$YEAR` token to ensure that Spotless copyright headers stay up-to-date without adding noise to file history. ([#595](https://github.com/diffplug/spotless/pull/595))
* Added `LineEnding.nativeIsWin()`, `FileSignature.pathNativeToUnix()`, and `FileSignature.pathUnixToNative()`, along with many API-invisible fixes and cleanup. ([#592](https://github.com/diffplug/spotless/pull/592))

## [1.33.0] - 2020-06-03
### Added
* `LicenseHeaderStep` now has an `updateYearWithLatest` parameter which can update copyright headers to today's date. ([#593](https://github.com/diffplug/spotless/pull/593))
  * Parsing of existing years from headers is now more lenient.
  * The `LicenseHeaderStep` constructor is now public, which allows capturing its state lazily, which is helpful for setting defaults based on `ratchetFrom`.

## [1.32.0] - 2020-06-01
### Added
* `NodeJsGlobal.setSharedLibFolder` allows to set the location of nodejs shared libs. ([#586](https://github.com/diffplug/spotless/pull/586))
* `PaddedCell.isClean()` returns the instance of `PaddedCell.DirtyState` which represents clean. ([#590](https://github.com/diffplug/spotless/pull/590))
### Fixed
* Previously, the nodejs-based steps would throw `UnsatisfiedLinkError` if they were ever used from more than one classloader.  Now they can be used from any number of classloaders (important for Gradle build daemon). ([#586](https://github.com/diffplug/spotless/pull/586))

## [1.31.0] - 2020-05-21
### Added
* `PaddedCell.calculateDirtyState` is now defensive about misconfigured character encoding. ([#575](https://github.com/diffplug/spotless/pull/575))

## [1.30.1] - 2020-05-17
### Fixed
* `PaddedCell.DirtyState::writeCanonicalTo(File)` can now create a new file if necessary (previously required to overwrite an existing file) ([#576](https://github.com/diffplug/spotless/pull/576)).

## [1.30.0] - 2020-05-11
### Added
* `PaddedCell.calculateDirtyState(Formatter, File, byte[])` to allow IDE integrations to send dirty editor buffers.

## [1.29.0] - 2020-05-05
### Added
* Support for google-java-format 1.8 (including test infrastructure for Java 11). ([#562](https://github.com/diffplug/spotless/issues/562))
* Improved PaddedCell such that it is as performant as non-padded cell - no reason not to have it always enabled.  Deprecated all of `PaddedCellBulk`. ([#561](https://github.com/diffplug/spotless/pull/561))
* Support for ktfmt 0.13 ([#569](https://github.com/diffplug/spotless/pull/569))
### Changed
* Updated a bunch of dependencies, most notably: ([#564](https://github.com/diffplug/spotless/pull/564))
  * jgit `5.5.0.201909110433-r` -> `5.7.0.202003110725-r`
  * Gradle `6.2.2` -> `6.3`
  * spotbugs Gradle plugin `2.0.0` -> `4.0.8`

## [1.28.1] - 2020-04-02
### Fixed
* Javadoc for the `ext/eclipse-*` projects.
* Replace the deprecated `compile` with `implementation` for the `ext/eclipse-*` projects.

## [1.28.0] - 2020-03-20
### Added
* Enable IntelliJ-compatible token `$today.year` for specifying the year in license header files. ([#542](https://github.com/diffplug/spotless/pull/542))
### Fixed
* Eclipse-WTP formatter (web tools platform, not java) could encounter errors in parallel multiproject builds [#492](https://github.com/diffplug/spotless/issues/492). Fixed for Eclipse-WTP formatter Eclipse version 4.13.0 (default version).
### Build
* All `CHANGES.md` are now in keepachangelog format. ([#507](https://github.com/diffplug/spotless/pull/507))
* We now use [javadoc.io](https://javadoc.io/) instead of github pages. ([#508](https://github.com/diffplug/spotless/pull/508))
* We no longer publish `-SNAPSHOT` for every build to `main`, since we have good [JitPack integration](https://github.com/diffplug/spotless/blob/main/CONTRIBUTING.md#gradle---any-commit-in-a-public-github-repo-this-one-or-any-fork). ([#508](https://github.com/diffplug/spotless/pull/508))
* Improved how we use Spotless on itself. ([#509](https://github.com/diffplug/spotless/pull/509))
* Fix build warnings when building on Gradle 6+, bump build Gradle to 6.2.2, and fix javadoc links. ([#536](https://github.com/diffplug/spotless/pull/536))

## [1.27.0] - 2020-01-01
* Ignored `KtLintStepTest`, because [gradle/gradle#11752](https://github.com/gradle/gradle/issues/11752) is causing too many CI failures. ([#499](https://github.com/diffplug/spotless/pull/499))
    * Also fixed a minor problem in TestProvisioner.
* If you set the environment variable `SPOTLESS_EXCLUDE_MAVEN=true` then the Maven plugin will be excluded from the build. ([#502](https://github.com/diffplug/spotless/pull/502))
    * We have set this in JitPack, as a workaround for [jitpack/jitpack.io#4112](https://github.com/jitpack/jitpack.io/issues/4112)
* Deprecated `SpotlessCache.clear()` in favor of the new `SpotlessCache.clearOnce(Object key)`. ([#501](https://github.com/diffplug/spotless/issues/#501))

## [1.26.1] - 2019-11-27
* Revert the change in console display of errors from 1.26.0 ([#485](https://github.com/diffplug/spotless/pull/485)) because [of these problems](https://github.com/diffplug/spotless/pull/485#issuecomment-552925932).
* Bugfix: Fix NPE in EclipseXmlFormatterStepImpl ([#489](https://github.com/diffplug/spotless/pull/489))

## [1.26.0] - 2019-11-11
* Fix project URLs in poms. ([#478](https://github.com/diffplug/spotless/pull/478))
* Fix `ImportSorter` crashing with empty files. ([#474](https://github.com/diffplug/spotless/pull/474))
    * Fixes [#305](https://github.com/diffplug/spotless/issues/305) StringIndexOutOfBoundsException for empty Groovy file when performing importOrder
* Bugfix: CDT version `4.12.0` now properly uses `9.8`, whereas before it used `9.7`. ([#482](https://github.com/diffplug/spotless/pull/482#discussion_r341380884))
* Add support for Eclipse 4.13 and all related formatters (JDT, CDT, WTP, and Groovy). ([#480](https://github.com/diffplug/spotless/issues/480))
* Bump default version of KtLint from `0.34.2` to `0.35.0`. ([#473](https://github.com/diffplug/spotless/issues/473))
* Several improvements to the console display of formatting errors. ([#465](https://github.com/diffplug/spotless/pull/465))
    * Visualize \r and \n as ␍ and ␊ when possible ([#465](https://github.com/diffplug/spotless/pull/465))
    * Make end-of-lines visible when file contains whitespace and end-of-line issues at the same time ([#465](https://github.com/diffplug/spotless/pull/465))
    * Print actual diff line instead of "1 more lines that didn't fit" ([#467](https://github.com/diffplug/spotless/issues/467))
* Automatically configure import order for IntelliJ IDEA with `.editorconfig` ([#486](https://github.com/diffplug/spotless/issues/486))

## [1.25.0] - 2019-10-06

* Add support for ktlint `0.34+`, and bump default version from `0.32.0` to `0.34.2`. ([#469](https://github.com/diffplug/spotless/pull/469))

## [1.24.3] - 2019-09-23
* Update jgit from `5.3.2.201906051522-r` to `5.5.0.201909110433-r`. ([#445](https://github.com/diffplug/spotless/pull/445))
  * Fixes [#410](https://github.com/diffplug/spotless/issues/410) AccessDeniedException in MinGW/ GitBash.
  * Also fixes occasional [hang on NFS due to filesystem timers](https://github.com/diffplug/spotless/pull/407#issuecomment-514824364).
* Eclipse-based formatters used to leave temporary files around ([#447](https://github.com/diffplug/spotless/issues/447)). This is now fixed, but only for eclipse 4.12+, no back-port to older Eclipse formatter versions is planned. ([#451](https://github.com/diffplug/spotless/issues/451))
* `PaddedCellBulk` had a bug where badly-formatted files with well-behaving formatters were being:
    - correctly formatted by "apply"
    - but incorrectly marked as good by "check"
    - this led to "check" says all good, but then "apply" still causes format (https://github.com/diffplug/spotless/issues/453)
    - combined with up-to-date checking, could lead to even more confusing results (https://github.com/diffplug/spotless/issues/338)
    - only affects the Gradle plugin, since that was the only plugin to use this feature
* Minor change to `TestProvisioner`, which should fix the cache-breaking issues, allowing us to speed-up the CI builds a bit.
* Bumped `scalafmt` default version from `1.1.0` to `2.0.1`, since there are [bugs](https://github.com/diffplug/spotless/issues/454) in the old default ([#458](https://github.com/diffplug/spotless/pull/458)).

## [1.24.1] - 2019-08-12
* Fixes class loading issue with Java 9+ ([#426](https://github.com/diffplug/spotless/pull/426)).

## [1.24.0] - 2019-07-29
* Updated default eclipse-wtp from 4.8.0 to 4.12.0 ([#423](https://github.com/diffplug/spotless/pull/423)).
* Updated default eclipse-groovy from 4.10 to 4.12.0 ([#423](https://github.com/diffplug/spotless/pull/423)).
* Updated default eclipse-jdt from 4.11.0 to 4.12.0 ([#423](https://github.com/diffplug/spotless/pull/423)).
* Updated default eclipse-cdt from 4.11.0 to 4.12.0 ([#423](https://github.com/diffplug/spotless/pull/423)).
    * **KNOWN BUG - accidentally published CDT 9.7 rather than 9.8 - fixed in 1.26.0**
* Added new Maven coordinates for scalafmt 2.0.0+, maintains backwards compatability ([#415](https://github.com/diffplug/spotless/issues/415))

## [1.23.1] - 2019-06-17
* Fixes incorrect M2 cache directory path handling of Eclipse based formatters ([#401](https://github.com/diffplug/spotless/issues/401))
* Update jgit from `4.9.0.201710071750-r` to `5.3.2.201906051522-r` because Gradle project is sometimes broken by `apache httpcomponents` in transitive dependency. ([#407](https://github.com/diffplug/spotless/pull/407))

## [1.23.0] - 2019-04-24
* Updated default ktlint from 0.21.0 to 0.32.0, and Maven coords to com.pinterest ([#394](https://github.com/diffplug/spotless/pull/394))

## [1.22.0] - 2019-04-15
* Updated default eclipse-cdt from 4.7.3a to 4.11.0 ([#390](https://github.com/diffplug/spotless/pull/390)).

## [1.21.1] - 2019-03-29
* Fixes incorrect plugin and pom metadata in `1.21.0` ([#388](https://github.com/diffplug/spotless/issues/388)).

## [1.21.0] - 2019-03-28
* We now use a remote build cache to speed up CI builds.  Reduced build time from ~13 minutes to as low as ~3 minutes, dependending on how deep the change is ([#380](https://github.com/diffplug/spotless/pull/380)).
* Updated default eclipse-wtp from 4.7.3b to 4.8.0 ([#382](https://github.com/diffplug/spotless/pull/382)).
* Updated default eclipse-groovy from 4.8.1 to 4.10.0 ([#382](https://github.com/diffplug/spotless/pull/382)).
* Updated default eclipse-jdt from 4.10.0 to 4.11.0 ([#384](https://github.com/diffplug/spotless/pull/384)).

## [1.20.0] - 2019-03-11
* Made npm package versions of [`prettier`](https://prettier.io/) and [`tsfmt`](https://github.com/vvakame/typescript-formatter) (and its internal packages) configurable. ([#363](https://github.com/diffplug/spotless/pull/363))
  * Updated default npm package version of `prettier` from 1.13.4 to 1.16.4
  * Updated default npm package version of internally used typescript package from 2.9.2 to 3.3.3 and tslint package from 5.1.0 to 5.12.0 (both used by `tsfmt`)
* Updated default eclipse-wtp from 4.7.3a to 4.7.3b ([#371](https://github.com/diffplug/spotless/pull/371)).
* Configured `build-scan` plugin in build ([#356](https://github.com/diffplug/spotless/pull/356)).
  * Runs on every CI build automatically.
  * Users need to opt-in on their local machine.
* Default behavior of XML formatter changed to ignore external URIs ([#369](https://github.com/diffplug/spotless/issues/369)).
  * **WARNING RESOLVED: By default, xml formatter no longer downloads external entities. You can opt-in to resolve external entities by setting resolveExternalURI to true. However, if you do opt-in, be sure that all external entities are referenced over https and not http, or you may be vulnerable to XXE attacks.**

## [1.19.0] - 2019-03-11
**WARNING: xml formatter in this version may be vulnerable to XXE attacks, fixed in 1.20.0 (see [#358](https://github.com/diffplug/spotless/issues/358)).**

* Security fix: Updated groovy, c/c++, and eclipse WTP formatters so that they download their source jars securely using `https` rather than `http` ([#360](https://github.com/diffplug/spotless/issues/360)).
* Updated default eclipse-jdt from 4.9.0 to 4.10.0 ([#368](https://github.com/diffplug/spotless/pull/368))

## [1.18.0] - 2019-02-11
**WARNING: xml formatter in this version may be vulnerable to XXE attacks, fixed in 1.20.0 (see [#358](https://github.com/diffplug/spotless/issues/358)).**

* CSS and XML extensions are discontinued ([#325](https://github.com/diffplug/spotless/pull/325)).
* Provided features with access to SLF4J interface of build tools. ([#236](https://github.com/diffplug/spotless/issues/236))
* Updated default google-java-format from 1.5 to 1.7 ([#335](https://github.com/diffplug/spotless/issues/335)).
* `ImportOrderStep.createFromFile` is now lazy ([#218](https://github.com/diffplug/spotless/issues/218)).

## [1.17.0] - 2018-10-30
**WARNING: xml formatter in this version may be vulnerable to XXE attacks, fixed in 1.20.0 (see [#358](https://github.com/diffplug/spotless/issues/358)).**

* Updated default eclipse-jdt from 4.7.3a to 4.9.0 ([#316](https://github.com/diffplug/spotless/pull/316)). New version addresses enum-tab formatting bug in 4.8 ([#314](https://github.com/diffplug/spotless/issues/314)).

## [1.16.0] - 2018-10-30
**WARNING: xml formatter in this version may be vulnerable to XXE attacks, fixed in 1.20.0 (see [#358](https://github.com/diffplug/spotless/issues/358)).**

* Minor support for plugin-gradle and plugin-maven CSS plugins ([#311](https://github.com/diffplug/spotless/pull/311)).

## [1.15.0] - 2018-09-23
**WARNING: xml formatter in this version may be vulnerable to XXE attacks, fixed in 1.20.0 (see [#358](https://github.com/diffplug/spotless/issues/358)).**

* Added C/C++ support ([#232](https://github.com/diffplug/spotless/issues/232)).
* Integrated Eclipse CDT formatter ([#274](https://github.com/diffplug/spotless/pull/274))
* Extended dependency provisioner to exclude transitives on request ([#297](https://github.com/diffplug/spotless/pull/297)).This prevents unnecessary downloads of unused transitive dependencies for Eclipse based formatter steps.
* Updated default groovy-eclipse from 4.8.0 to 4.8.1 ([#288](https://github.com/diffplug/spotless/pull/288)). New version is based on [Groovy-Eclipse 3.0.0](https://github.com/groovy/groovy-eclipse/wiki/3.0.0-Release-Notes).
* Integrated Eclipse WTP formatter ([#290](https://github.com/diffplug/spotless/pull/290))
* Updated JSR305 annotation from 3.0.0 to 3.0.2 ([#274](https://github.com/diffplug/spotless/pull/274))
* Migrated from FindBugs annotations 3.0.0 to SpotBugs annotations 3.1.6 ([#274](https://github.com/diffplug/spotless/pull/274))
* `Formatter` now implements `AutoCloseable`.  This means that users of `Formatter` are expected to use the try-with-resources pattern.  The reason for this change is so that `FormatterFunc.Closeable` actually works. ([#284](https://github.com/diffplug/spotless/pull/284))* Added [`prettier`](https://prettier.io/) and [`tsfmt`](https://github.com/vvakame/typescript-formatter) support, as well as general infrastructure for calling `nodeJS` code using `j2v8` ([#283](https://github.com/diffplug/spotless/pull/283)).

## [1.14.0] - 2018-07-24
* Updated default groovy-eclipse from 4.6.3 to 4.8.0 ([#244](https://github.com/diffplug/spotless/pull/244)). New version allows to ignore internal formatter errors/warnings.
* Updated default eclipse-jdt from 4.7.2 to 4.8.0 ([#239](https://github.com/diffplug/spotless/pull/239)). New version fixes a bug preventing Java code formatting within JavaDoc comments ([#191](https://github.com/diffplug/spotless/issues/191)).
* Eclipse formatter versions decoupled from Spotless formatter step implementations to allow independent updates of maven-based Eclipse dependencies. ([#253](https://github.com/diffplug/spotless/pull/253))
* Use guaranteed binary and source compatibility between releases of Scalafmt. ([#260](https://github.com/diffplug/spotless/pull/260))

## [1.13.0] - 2018-06-01
* Add line and column numbers to ktlint errors. ([#251](https://github.com/diffplug/spotless/pull/251))

## [1.12.0] - 2018-05-14
* Fixed a bug in `LicenseHeaderStep` which caused an exception with some malformed date-aware licenses. ([#222](https://github.com/diffplug/spotless/pull/222))
* Updated default ktlint from 0.14.0 to 0.21.0
* Add ability to pass custom options to ktlint in Gradle plugin. See plugin-gradle/README for details.

## [1.11.0] - 2018-02-26
* Added default indentation of `4` to `IndentStep`. ([#209](https://github.com/diffplug/spotless/pull/209))

## [1.10.0] - 2018-02-15
* LicenseHeaderStep now supports customizing the year range separator in copyright notices. ([#199](https://github.com/diffplug/spotless/pull/199))
* Breaking change to testlib - removed `ResourceHarness.write` and added `ResourceHarness.[set/assert]File` for easier-to-read tests. ([#203](https://github.com/diffplug/spotless/pull/203))

## [1.9.0] - 2018-02-05
* Updated default ktlint from 0.6.1 to 0.14.0
* Updated default google-java-format from 1.3 to 1.5
* Updated default eclipse-jdt from 4.7.1 to 4.7.2
* Added a configuration option to `googleJavaFormat` to switch the formatter style ([#193](https://github.com/diffplug/spotless/pull/193))

## [1.8.0] - 2018-01-02
* LicenseHeaderStep now supports time-aware copyright notices in license headers. ([#179](https://github.com/diffplug/spotless/pull/179), thanks to @baptistemesta)

## [1.7.0] - 2018-12-02
* Updated default eclipse-jdt version to `4.7.1` from `4.6.3`.
* Updated jgit from `4.5.0.201609210915-r` to `4.9.0.201710071750-r`.
* Updated concurrent-trees from `2.6.0` to `2.6.1` (performance improvement).
* Added `dbeaverSql` formatter step, for formatting sql scripts. ([#166](https://github.com/diffplug/spotless/pull/166))
  + Many thanks to [Baptiste Mesta](https://github.com/baptistemesta) for porting to Spotless.
  + Many thanks to [DBeaver](https://dbeaver.jkiss.org/) and the [DBeaver contributors](https://github.com/serge-rider/dbeaver/graphs/contributors) for building the implementation.

## [1.6.0] - 2017-09-29
* Added `public static boolean PaddedCell::applyAnyChanged(Formatter formatter, File file)`.

## [1.5.1] - 2017-08-14
* Added `KtLintStep.createForScript`.

## [1.5.0] - 2017-08-13
* Deprecated `ImportOrderStep.createFromOrder(List<String>` in favor of `(String...`.

## [1.4.1] - 2017-07-11
* Default eclipse version for `EclipseFormatterStep` bumped to `4.6.3` from `4.6.1`. ([#116](https://github.com/diffplug/spotless/issues/116))
* Default scalafmt version for `ScalaFmtStep` bumped to `1.1.0` from `0.5.7` ([#124](https://github.com/diffplug/spotless/pull/124))
  + Also added support for the API change to scalafmt introduced in `0.7.0-RC1`

## [1.4.0] - 2017-05-21
* `ImportOrderStep` can now handle multi-line comments and misplaced imports.
  + Especially helpful for Groovy and Gradle files.

## [1.3.2] - 2017-05-03
* Fixed a bug in `PaddedCellBulk.check()` which caused a `check` to fail even after an `apply` for cases which caused CYCLE.

## [1.3.0] - 2017-04-11
* Added support for Groovy via [greclipse](https://github.com/groovy/groovy-eclipse).
* When a JarState resolution failed, it threw a Gradle-specific error message. That message has been moved out of `lib` and into `plugin-gradle` where it belongs.

## [1.2.0] - 2017-04-03
* Deprecated `FileSignature.from` in favor of `FileSignature.signAsSet` and the new `FileSignature.signAsList`.
* Added a `FormatterProperties` class which loads `.properties` files and eclipse-style `.xml` files.
* `SerializableFileFilter.skipFilesNamed` can now skip multiple file names.
* Update default KtLint from 0.3.1 to 0.6.1.
  + This means we no longer look for rules in the typo package `com.gihub.shyiko`, now only in `com.github.shyiko` (note the `t`).

## [1.1.0] - 2017-02-27
* Added support for Scala via [scalafmt](https://github.com/olafurpg/scalafmt).
* Added support for Kotlin via [ktlint](https://github.com/pinterest/ktlint).
* Better error messages for JarState.
* Improved test harnessing.
* Formatter now has pluggable exception policies,

## [1.0.0] - 2017-01-09
* Initial release!
