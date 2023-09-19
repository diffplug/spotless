# spotless-plugin-maven releases

We adhere to the [keepachangelog](https://keepachangelog.com/en/1.0.0/) format (starting after version `1.27.0`).

## [Unreleased]
### Added
* Support Ktlint 1.0.0 ([#1808](https://github.com/diffplug/spotless/pull/1808)).

## [2.39.0] - 2023-08-29
### Added
* Add a `jsonPatch` step to `json` formatter configurations. This allows patching of JSON documents using [JSON Patches](https://jsonpatch.com). ([#1753](https://github.com/diffplug/spotless/pull/1753))
* Support GJF own import order. ([#1780](https://github.com/diffplug/spotless/pull/1780))
### Fixed
* Add support for `prettier` version `3.0.0` and newer. ([#1760](https://github.com/diffplug/spotless/pull/1760), [#1751](https://github.com/diffplug/spotless/issues/1751))
* Fix npm install calls when npm cache is not up-to-date. ([#1760](https://github.com/diffplug/spotless/pull/1760), [#1750](https://github.com/diffplug/spotless/issues/1750))
### Changes
* Bump default `eslint` version to latest `8.31.0` -> `8.45.0` ([#1761](https://github.com/diffplug/spotless/pull/1761))
* Bump default `prettier` version to latest (v2) `2.8.1` -> `2.8.8`. ([#1760](https://github.com/diffplug/spotless/pull/1760))
* Bump default `greclipse` version to latest `4.27` -> `4.28`. ([#1775](https://github.com/diffplug/spotless/pull/1775))

## [2.38.0] - 2023-07-17
### Added
* Support pass skip (`-Dspotless.skip=true`) from command-line. ([#1729](https://github.com/diffplug/spotless/pull/1729))
### Fixed
* Update documented default `semanticSort` to `false`. ([#1728](https://github.com/diffplug/spotless/pull/1728))
### Changes
* Bump default `cleanthat` version to latest `2.13` -> `2.17`. ([#1734](https://github.com/diffplug/spotless/pull/1734))
* Bump default `ktlint` version to latest `0.49.1` -> `0.50.0`. ([#1741](https://github.com/diffplug/spotless/issues/1741))
  * Dropped support for `ktlint 0.47.x` following our policy of supporting two breaking changes at a time.
  * Dropped support for deprecated `useExperimental` parameter in favor of the `ktlint_experimental` property.

## [2.37.0] - 2023-05-24
### Added
* Support Rome as a formatter for JavaScript and TypeScript code. Adds a new `rome` step to `javascript` and `typescript` formatter configurations. ([#1663](https://github.com/diffplug/spotless/pull/1663))
* Add semantics-aware Java import ordering (i.e. sort by package, then class, then member). ([#522](https://github.com/diffplug/spotless/issues/522))
### Fixed
* `palantir` step now accepts a `style` parameter, which is documentation had already claimed to do. ([#1694](https://github.com/diffplug/spotless/pull/1694))
* Fixed a regression which changed the import sorting order in `googleJavaFormat` introduced in `2.36.0`. ([#1680](https://github.com/diffplug/spotless/pull/1680))
* Equo-based formatters now work on platforms unsupported by Eclipse such as PowerPC (fixes [durian-swt#20](https://github.com/diffplug/durian-swt/issues/20))
* When P2 download fails, indicate the responsible formatter. ([#1698](https://github.com/diffplug/spotless/issues/1698))
### Changes
* Equo-based formatters now download metadata to `~/.m2/repository/dev/equo/p2-data` rather than `~/.equo`, and for CI machines without a home directory the p2 data goes to `$GRADLE_USER_HOME/caches/p2-data`. ([#1714](https://github.com/diffplug/spotless/pull/1714))
* Bump default `googleJavaFormat` version to latest `1.16.0` -> `1.17.0`. ([#1710](https://github.com/diffplug/spotless/pull/1710))
* Bump default `ktfmt` version to latest `0.43` -> `0.44`. ([#1691](https://github.com/diffplug/spotless/pull/1691))
* Bump default `ktlint` version to latest `0.48.2` -> `0.49.1`. ([#1696](https://github.com/diffplug/spotless/issues/1696))
  * Dropped support for `ktlint 0.46.x` following our policy of supporting two breaking changes at a time.
* Bump default `sortpom` version to latest `3.0.0` -> `3.2.1`. ([#1675](https://github.com/diffplug/spotless/pull/1675))

## [2.36.0] - 2023-04-06
### Added
* `removeUnusedImport` can be configured to rely on `cleanthat-javaparser-unnecessaryimport`. Default remains `google-java-format`. ([#1589](https://github.com/diffplug/spotless/pull/1589))
* The `style` option in Palantir Java Format ([#1654](https://github.com/diffplug/spotless/pull/1654)).
* Added formatter for Gherkin feature files ([#1649](https://github.com/diffplug/spotless/issues/1649)).
### Fixed
* Fix non deterministic computation of cache fingerprint when using multiple formatters. ([#1643](https://github.com/diffplug/spotless/pull/1643) fixes [#1642](https://github.com/diffplug/spotless/pull/1642))
### Changes
* **POTENTIALLY BREAKING** Drop support for `googleJavaFormat` versions &lt; `1.8`. ([#1630](https://github.com/diffplug/spotless/pull/1630))
* Bump default `cleanthat` version to latest `2.6` -> `2.13`. ([#1589](https://github.com/diffplug/spotless/pull/1589) and [#1661](https://github.com/diffplug/spotless/pull/1661))
* Bump default `diktat` version `1.2.4.2` -> `1.2.5`. ([#1631](https://github.com/diffplug/spotless/pull/1631))
* Bump default `flexmark` version `0.62.2` -> `0.64.0`. ([#1302](https://github.com/diffplug/spotless/pull/1302))
* Bump default `googleJavaFormat` version `1.15.0` -> `1.16.0`. ([#1630](https://github.com/diffplug/spotless/pull/1630))
* Bump default `scalafmt` version `3.7.1` -> `3.7.3`. ([#1584](https://github.com/diffplug/spotless/pull/1584))
* Bump default Eclipse formatters for the 2023-03 release. ([#1662](https://github.com/diffplug/spotless/pull/1662))
  * JDT and GrEclipse `4.26` -> `4.27`
    * Improve GrEclipse error reporting. ([#1660](https://github.com/diffplug/spotless/pull/1660))
  * CDT `11.0` -> `11.1`

## [2.35.0] - 2023-03-13
### Added
* You can now put the filename into a license header template with `$FILE`. ([#1605](https://github.com/diffplug/spotless/pull/1605) fixes [#1147](https://github.com/diffplug/spotless/issues/1147))
### Fixed
* `licenseHeader` default pattern for Java files is updated to `(package|import|public|class|module) `. ([#1614](https://github.com/diffplug/spotless/pull/1614))
### Changes
* Enable incremental up-to-date checking by default. ([#1621](https://github.com/diffplug/spotless/pull/1621))
* All Eclipse formatters are now based on [Equo Solstice OSGi and p2 shim](https://github.com/equodev/equo-ide/tree/main/solstice). ([#1524](https://github.com/diffplug/spotless/pull/1524))
  * Eclipse JDT bumped default to `4.26` from `4.21`, oldest supported is `4.9`.
    * We now recommend dropping the last `.0`, e.g. `4.26` instead of `4.26.0`, you'll get warnings to help you switch.
  * Eclipse Groovy bumped default to `4.26` from `4.21`, oldest supported is `4.18`.
  * Eclipse CDT bumped default to `11.0` from `4.21`, oldest supported is `10.6`.
  * Eclipse WTP is still WIP at [#1622](https://github.com/diffplug/spotless/pull/1622).

## [2.34.0] - 2023-02-27
### Added
* `cleanthat` added `includeDraft` option, to include draft mutators from composite mutators. ([#1574](https://github.com/diffplug/spotless/pull/1574))
* `npm`-based formatters (`prettier`, `tsfmt` and `eslint`) now support caching of `node_modules` directory.
  To enable it, provide the `<npmInstallCache>` option. ([#1590](https://github.com/diffplug/spotless/pull/1590))
### Fixed
* `<json><jackson>` can now handle `Array` as a root element. ([#1585](https://github.com/diffplug/spotless/pull/1585))
* Reduce logging-noise created by `npm`-based formatters ([#1590](https://github.com/diffplug/spotless/pull/1590) fixes [#1582](https://github.com/diffplug/spotless/issues/1582))
### Changes
* Bump default `cleanthat` version to latest `2.1` -> `2.6` ([#1569](https://github.com/diffplug/spotless/pull/1569) and [#1574](https://github.com/diffplug/spotless/pull/1574))

## [2.33.0] - 2023-02-10
### Added
* CleanThat Java Refactorer. ([#1560](https://github.com/diffplug/spotless/pull/1560))
### Fixed
* Allow multiple instances of the same npm-based formatter to be used simultaneously. E.g. use prettier for typescript
  *and* Java (using the community prettier-plugin-java) without messing up their respective `node_module` dependencies. ([#1565](https://github.com/diffplug/spotless/pull/1565))
* `ktfmt` default style uses correct continuation indent. ([#1562](https://github.com/diffplug/spotless/pull/1562))
### Changes
* Bump default `ktfmt` version to latest `0.42` -> `0.43` ([#1561](https://github.com/diffplug/spotless/pull/1561))
* Bump default `jackson` version to latest `2.14.1` -> `2.14.2` ([#1536](https://github.com/diffplug/spotless/pull/1536))

## [2.32.0] - 2023-02-05
### Added
* A synthesis log with the number of considered files is added after each formatter execution ([#1507](https://github.com/diffplug/spotless/pull/1507))
### Fixed
* Respect `sourceDirectory` and `testSourceDirectory` POM configurations for Java formatters ([#1553](https://github.com/diffplug/spotless/pull/1553))
* **POTENTIALLY BREAKING** `sortByKeys` for JSON formatting now takes into account objects inside arrays ([#1546](https://github.com/diffplug/spotless/pull/1546))
* Any commit of the Spotless maven plugin now available via JitPack ([#1547](https://github.com/diffplug/spotless/pull/1547))

## [2.31.0] - 2023-01-26
### Added
* Prettier will now suggest to install plugins if a parser cannot be inferred from the file extension ([#1511](https://github.com/diffplug/spotless/pull/1511))
* Jackson (`json` and `yaml`) has new `spaceBeforeSeparator` option
  * **POTENTIALLY BREAKING** `spaceBeforeSeparator` is defaulted to false while the formatter was behaving with `<spaceBeforeSeparator>true<spaceBeforeSeparator/>`
* Introduce `<json><jackson/></json>` ([#1492](https://github.com/diffplug/spotless/pull/1492))
  * **POTENTIALLY BREAKING** `JacksonYaml` is now configured with a `Map<String, Boolean>` to configure features
* Allow to specify node executable for node-based formatters using `nodeExecutable` parameter ([#1500](https://github.com/diffplug/spotless/pull/1500))
### Fixed
* The default list of type annotations used by `formatAnnotations` has had 8 more annotations from the Checker Framework added [#1494](https://github.com/diffplug/spotless/pull/1494)
* **POTENTIALLY BREAKING** Generate the correct qualifiedRuleId for Ktlint 0.48.x [#1495](https://github.com/diffplug/spotless/pull/1495)
### Changes
* **POTENTIALLY BREAKING** Bump minimum JRE from 8 to 11 ([#1514](https://github.com/diffplug/spotless/pull/1514) part 1 of [#1337](https://github.com/diffplug/spotless/issues/1337))
  * You can bump your build JRE without bumping your requirements ([docs](https://maven.apache.org/plugins/maven-compiler-plugin/examples/set-compiler-source-and-target.html)).
* Spotless' custom build was replaced by [`maven-plugin-development`](https://github.com/britter/maven-plugin-development). ([#1496](https://github.com/diffplug/spotless/pull/1496) fixes [#554](https://github.com/diffplug/spotless/issues/554))
* **POTENTIALLY BREAKING** Removed support for KtLint 0.3x and 0.45.2 ([#1475](https://github.com/diffplug/spotless/pull/1475))
  * `KtLint` does not maintain a stable API - before this PR, we supported every breaking change in the API since 2019.
  * From now on, we will support no more than 2 breaking changes at a time.
* `npm`-based formatters `ESLint`, `prettier` and `tsfmt` delay their `npm install` call until the formatters are first used. ([#1522](https://github.com/diffplug/spotless/pull/1522)
* Bump default `ktlint` version to latest `0.48.1` -> `0.48.2` ([#1529](https://github.com/diffplug/spotless/pull/1529))
* Bump default `scalafmt` version to latest `3.6.1` -> `3.7.1` ([#1529](https://github.com/diffplug/spotless/pull/1529))

## [2.30.0] - 2023-01-13
### Added
* Add option `editorConfigFile` for `ktLint` [#142](https://github.com/diffplug/spotless/issues/142)
  * **POTENTIALLY BREAKING** `ktlint` step now modifies license headers. Make sure to put `licenseHeader` *after* `ktlint`.
* Added `skipLinesMatching` option to `licenseHeader` to support formats where license header cannot be immediately added to the top of the file (e.g. xml, sh). ([#1441](https://github.com/diffplug/spotless/pull/1441))
* Add JSON support ([#1446](https://github.com/diffplug/spotless/pull/1446))
* Add YAML support through Jackson ([#1478](https://github.com/diffplug/spotless/pull/1478))
* Added support for npm-based [ESLint](https://eslint.org/)-formatter for javascript and typescript ([#1453](https://github.com/diffplug/spotless/pull/1453))
* Better suggested messages when user's default is set by JVM limitation. ([#995](https://github.com/diffplug/spotless/pull/995))
### Fixed
* Support `ktlint` 0.48+ new rule disabling syntax ([#1456](https://github.com/diffplug/spotless/pull/1456)) fixes ([#1444](https://github.com/diffplug/spotless/issues/1444))
* Fix subgroups leading catch all matcher.
### Changes
* Bump default `ktlint` version to latest `0.47.1` -> `0.48.1` ([#1456](https://github.com/diffplug/spotless/pull/1456))
* Reduce spurious invalidations of the up-to-date index file ([#1461](https://github.com/diffplug/spotless/pull/1461))
* Bump default version for `prettier` from `2.0.5` to `2.8.1` ([#1453](https://github.com/diffplug/spotless/pull/1453))

## [2.29.0] - 2023-01-02
### Added
* Added support for M2E's incremental compilation ([#1414](https://github.com/diffplug/spotless/pull/1414) fixes [#1413](https://github.com/diffplug/spotless/issues/1413))
### Fixed
* Improve memory usage when using git ratchet ([#1426](https://github.com/diffplug/spotless/pull/1426))
* Support `ktlint` 0.48+ ([#1432](https://github.com/diffplug/spotless/pull/1432)) fixes ([#1430](https://github.com/diffplug/spotless/issues/1430))
### Changes
* Bump default `ktlint` version to latest `0.47.1` -> `0.48.0` ([#1432](https://github.com/diffplug/spotless/pull/1432))
* Bump default `ktfmt` version to latest `0.41` -> `0.42` ([#1421](https://github.com/diffplug/spotless/pull/1421))

## [2.28.0] - 2022-11-24
### Added
* `importOrder` now support groups of imports without blank lines ([#1401](https://github.com/diffplug/spotless/pull/1401))
### Fixed
* Don't treat `@Value` as a type annotation [#1367](https://github.com/diffplug/spotless/pull/1367)
* Support `ktlint_disabled_rules` in `ktlint` 0.47.x [#1378](https://github.com/diffplug/spotless/pull/1378)
* Share git repositories across projects when using ratchet ([#1426](https://github.com/diffplug/spotless/pull/1426))
### Changes
* Bump default `ktfmt` version to latest `0.40` -> `0.41` ([#1340](https://github.com/diffplug/spotless/pull/1340))
* Bump default `scalafmt` version to latest `3.5.9` -> `3.6.1` ([#1373](https://github.com/diffplug/spotless/pull/1373))
* Bump default `diktat` version to latest `1.2.3` -> `1.2.4.2` ([#1393](https://github.com/diffplug/spotless/pull/1393))
* Bump default `palantir-java-format` version to latest `2.10` -> `2.28` ([#1393](https://github.com/diffplug/spotless/pull/1393))

## [2.27.2] - 2022-10-10
### Fixed
* `replace` and `replaceRegex` steps now allow you to replace something with an empty string, previously this would generate a null pointer exception. (fixes [#1359](https://github.com/diffplug/spotless/issues/1359))

## [2.27.1] - 2022-09-28
### Fixed
* `skip` config key should work again now. ([#1353](https://github.com/diffplug/spotless/pull/1353) fixes [#1227](https://github.com/diffplug/spotless/issues/1227) and [#491](https://github.com/diffplug/spotless/issues/491))

## [2.27.0] - 2022-09-19
### Added
* Support for `editorConfigOverride` in `ktlint`, `plugin-maven`. ([#1335](https://github.com/diffplug/spotless/pull/1335) fixes [#1334](https://github.com/diffplug/spotless/issues/1334))

## [2.26.0] - 2022-09-14
### Added
* `formatAnnotations` step to correct formatting of Java type annotations.  It puts type annotations on the same line as the type that they qualify.  Run it after a Java formatting step, such as `googleJavaFormat`. ([#1275](https://github.com/diffplug/spotless/pull/1275))
### Changes
* Bump default `ktfmt` version to latest `0.39` -> `0.40` ([#1312](https://github.com/diffplug/spotless/pull/1312))
* Bump default `ktlint` version to latest `0.46.1` -> `0.47.1` ([#1303](https://github.com/diffplug/spotless/pull/1303))
  * Also restored support for older versions of ktlint back to `0.31.0`

## [2.25.0] - 2022-08-23
### Added
* `scalafmt` integration now has a configuration option `majorScalaVersion` that allows you to configure the Scala version that gets resolved from the maven artifact ([#1283](https://github.com/diffplug/spotless/pull/1283))
### Changes
* Add the `ktlint` rule in error messages when `ktlint` fails to apply a fix ([#1279](https://github.com/diffplug/spotless/pull/1279))
* Bump default `scalafmt` to latest `3.0.8` -> `3.5.9` (removed support for pre-`3.0.0`) ([#1283](https://github.com/diffplug/spotless/pull/1283))

## [2.24.1] - 2022-08-10
### Fixed
* Fix Clang not knowing the filename and changing the format ([#1268](https://github.com/diffplug/spotless/pull/1268) fixes [#1267](https://github.com/diffplug/spotless/issues/1267)).
### Changes
* Bump default `diktat` version to latest `1.2.1` -> `1.2.3` ([#1266](https://github.com/diffplug/spotless/pull/1266))

## [2.24.0] - 2022-07-28
### Added
* Clang and Black no longer break the build when the binary is unavailable, if they will not be run during that build ([#1257](https://github.com/diffplug/spotless/pull/1257)).
* License header support for Kotlin files without `package` or `@file` but do at least have `import` ([#1263](https://github.com/diffplug/spotless/pull/1263)).

## [2.23.0] - 2022-06-30
### Added
* Support for `MAC_CLASSIC` (`\r`) line ending ([#1243](https://github.com/diffplug/spotless/pull/1243) fixes [#1196](https://github.com/diffplug/spotless/issues/1196))
### Changes
* Bump default `ktlint` version to latest `0.45.2` -> `0.46.1` ([#1239](https://github.com/diffplug/spotless/issues/1239))
  * Minimum supported version also bumped to `0.46.0` (we have abandoned strong backward compatibility for `ktlint`, from here on out Spotless will only support the most-recent breaking change).
* Bump default `diktat` version to latest `1.1.0` -> `1.2.1` ([#1246](https://github.com/diffplug/spotless/pull/1246))
  * Minimum supported version also bumped to `1.2.1` (diktat is based on ktlint and has the same backward compatibility issues).
* Bump default `ktfmt` version to latest `0.37` -> `0.39` ([#1240](https://github.com/diffplug/spotless/pull/1240))

## [2.22.8] - 2022-06-11
### Fixed
* `PalantirJavaFormatStep` no longer needs the `--add-exports` calls in `MAVEN_OPTS` or `.mvn/jvm.config`. ([#1233](https://github.com/diffplug/spotless/pull/1233))

## [2.22.7] - 2022-06-10
### Fixed
* (Second try) `googleJavaFormat` and `removeUnusedImports` works on JDK16+ without jvm args workaround. ([#1228](https://github.com/diffplug/spotless/pull/1228))
  * If you have a bunch of `--add-exports` calls in `MAVEN_OPTS` or `.mvn/jvm.config`, you should be able to remove them. (fixes [#834](https://github.com/diffplug/spotless/issues/834#issuecomment-817524058))

## [2.22.6] - 2022-06-05
### Fixed
* `googleJavaFormat` and `removeUnusedImports` works on JDK16+ without jvm args workaround. ([#1224](https://github.com/diffplug/spotless/pull/1224))
  * If you have a bunch of `--add-exports` calls in `MAVEN_OPTS` or `.mvn/jvm.config`, you should be able to remove them. (fixes [#834](https://github.com/diffplug/spotless/issues/834#issuecomment-817524058))

## [2.22.5] - 2022-05-10
### Fixed
* Update the `black` version regex to fix `19.10b0` and earlier. (fixes [#1195](https://github.com/diffplug/spotless/issues/1195), regression introduced in `2.22.2`)
### Changes
* Bump default `ktfmt` version to latest `0.36` -> `0.37`. ([#1200](https://github.com/diffplug/spotless/pull/1200))

## [2.22.4] - 2022-05-03
### Changes
* Bump default `diktat` version to latest `1.0.1` -> `1.1.0`. ([#1190](https://github.com/diffplug/spotless/pull/1190))
  * Converted `diktat` integration to use a compile-only source set. (fixes [#524](https://github.com/diffplug/spotless/issues/524))
  * Use the full path to a file in `diktat` integration. (fixes [#1189](https://github.com/diffplug/spotless/issues/1189))

## [2.22.3] - 2022-04-27
### Changes
* Bump default `ktfmt` version to latest `0.35` -> `0.36`. ([#1183](https://github.com/diffplug/spotless/issues/1183))
* Bump default `google-java-format` version to latest `1.13.0` -> `1.15.0`.
  * ~~This means it is no longer necessary to use the `--add-exports` workaround (fixes [#834](https://github.com/diffplug/spotless/issues/834)).~~ `--add-exports` workaround is still needed.

## [2.22.2] - 2022-04-22
### Fixed
* Fixed support for Python Black's new version reporting. ([#1170](https://github.com/diffplug/spotless/issues/1170))
* Error messages for unexpected file encoding now works on Java 8. (fixes [#1081](https://github.com/diffplug/spotless/issues/1081))
### Changes
* Bump default `black` version to latest `19.10b0` -> `22.3.0`. ([#1170](https://github.com/diffplug/spotless/issues/1170))
* Bump default `ktfmt` version to latest `0.34` -> `0.35`. ([#1159](https://github.com/diffplug/spotless/pull/1159))
* Bump default `ktlint` version to latest `0.43.2` -> `0.45.2`. ([#1177](https://github.com/diffplug/spotless/pull/1177))

## [2.22.1] - 2022-04-06
### Fixed
* Git user config and system config also included for defaultEndings configuration. ([#540](https://github.com/diffplug/spotless/issues/540))

## [2.22.0] - 2022-03-28
### Added
* Added support for setting custom parameters for Kotlin ktfmt in Maven plugin. ([#1145](https://github.com/diffplug/spotless/pull/1145))

## [2.21.0] - 2022-02-19
### Added
* Magic value 'NONE' for disabling ratchet functionality ([#1134](https://github.com/diffplug/spotless/issues/1134))

### Changed
* Use SLF4J for logging ([#1116](https://github.com/diffplug/spotless/issues/1116))

## [2.20.2] - 2022-02-09
### Changed
* Bump default ktfmt `0.30` -> `0.31` ([#1118](https://github.com/diffplug/spotless/pull/1118)).
### Fixed
* Add full support for git worktrees ([#1119](https://github.com/diffplug/spotless/pull/1119)).

## [2.20.1] - 2022-02-01
* Bump default versions of formatters ([#1095](https://github.com/diffplug/spotless/pull/1095)).
  * google-java-format `1.12.0` -> `1.13.0`
  * ktfmt `0.29` -> `0.30`
* Added support for git property `core.autocrlf` ([#540](https://github.com/diffplug/spotless/issues/540))

## [2.20.0] - 2022-01-13
### Added
* Added support for the [palantir-java-format](https://github.com/palantir/palantir-java-format) Java formatter ([#1083](https://github.com/diffplug/spotless/pull/1083)).

## [2.19.2] - 2022-01-10
### Fixed
* Enabling the upToDateChecking with the plugin configured inside pluginManagement, with an additional dependency and running under Maven 3.6.3 leads to a java.io.NotSerializableException. ([#1074](https://github.com/diffplug/spotless/pull/1074)).

## [2.19.1] - 2022-01-07
### Fixed
* Update IndentStep to allow leading space on multiline comments ([#1072](https://github.com/diffplug/spotless/pull/1072)).

## [2.19.0] - 2022-01-06
### Added
* Support custom index files for incremental up-to-date checking ([#1055](https://github.com/diffplug/spotless/pull/1055)).
### Fixed
* Remove Java files from default Maven Groovy formatting ([#1051](https://github.com/diffplug/spotless/pull/1051)).
  * Before this release, the default target of groovy was
    * `src/main/groovy/**/*.groovy`
    * `src/test/groovy/**/*.groovy`
    * `src/main/java/**/*.java`
    * `src/test/java/**/*.java`
  * This release removes the `.java` includes.
### Changed
* Bumped default DiKTat from `0.4.0` to `1.0.1`. This is a breaking change for DiKTat users on the default version, because some rules were renamed/changed. Check [DiKTat changelog](https://github.com/analysis-dev/diktat/releases) for details.

## [2.18.0] - 2021-12-23
### Added
* Incremental up-to-date checking ([#935](https://github.com/diffplug/spotless/pull/935)).
* Support for Markdown with `flexmark` at `0.62.2` ([#1011](https://github.com/diffplug/spotless/pull/1011)).

## [2.17.7] - 2021-12-16
### Fixed
* `ratchetFrom` is now faster ([#1038](https://github.com/diffplug/spotless/pull/1038)).

## [2.17.6] - 2021-12-05
### Changed
* Bumped default ktlint from `0.43.0` to `0.43.2`.

## [2.17.5] - 2021-12-01
### Changed
* Bump jgit version ([#992](https://github.com/diffplug/spotless/pull/992)).
  * jgit `5.10.0.202012080955-r` -> `5.13.0.202109080827-r`

## [2.17.4] - 2021-11-09
### Changed
* Bump default formatter versions ([#989](https://github.com/diffplug/spotless/pull/989))
  * google-java-format `1.11.0` -> `1.12.0`
  * ktlint `0.42.1` -> `0.43.0`
  * ktfmt `0.27` -> `0.29`
  * scalafmt `3.0.0` -> `3.0.8`

## [2.17.3] - 2021-10-26
### Changed
* Added support and bump Eclipse formatter default versions to `4.21` for `eclipse-groovy`. Change is only applied for JVM 11+.
* Added support for ktlint's FilenameRule ([#974](https://github.com/diffplug/spotless/pull/974)).

### Fixed
 * Revert change from 2.17.2 regarding [skip bug](https://github.com/diffplug/spotless/pull/969) because fixing the skip bug caused inconsistent behavior between `check.skip` and `apply.skip`.
 * [skip bug](https://github.com/diffplug/spotless/issues/968) if ratchetFrom is specified, the build will still fail in if no Git repository is found, even if `skip` is true (new fix).

### Fixed
 * Temporary workspace deletion for Eclipse based formatters on JVM shutdown ([#967](https://github.com/diffplug/spotless/issues/967)). Change is only applied for Eclipse versions using JVM 11+, no back-port to older versions is planned.

## [2.17.2] - 2021-10-14
### Fixed
 * [skip bug](https://github.com/diffplug/spotless/issues/968) if ratchetFrom is specified, the build will still fail in if no Git repository is found, even if `skip` is true.

## [2.17.1] - 2021-10-13
### Fixed
 * [module-info formatting](https://github.com/diffplug/spotless/pull/958) in `eclipse-jdt` versions `4.20` and `4.21`. Note that the problem also affects older versions.
 * Added workaround to support projects using git worktrees ([#965](https://github.com/diffplug/spotless/pull/965))

## [2.17.0] - 2021-10-04
### Added
* Added `wildcardsLast` option for Java `importOrder` ([#956](https://github.com/diffplug/spotless/pull/956))

## [2.16.0] - 2021-10-02
### Added
* Added support for JBDI bind list params in sql formatter ([#955](https://github.com/diffplug/spotless/pull/955))

## [2.15.0] - 2021-09-30
### Added
* Added support for custom JSR223 formatters ([#945](https://github.com/diffplug/spotless/pull/945))
* Added support for formatting and sorting Maven POMs ([#946](https://github.com/diffplug/spotless/pull/946))

## [2.14.0] - 2021-09-27
### Added
* Added support for calling local binary formatters ([#949](https://github.com/diffplug/spotless/pull/949))
### Changed
* Added support and bump Eclipse formatter default versions to `4.21` for `eclipse-cdt`, `eclipse-jdt`, `eclipse-wtp`. Change is only applied for JVM 11+.
* Added `groupArtifact` option for `google-java-format` ([#944](https://github.com/diffplug/spotless/pull/944))

## [2.13.1] - 2021-09-20
### Changed
* Added support and bump Eclipse formatter default versions for JVM 11+. For older JVMs the previous defaults remain.
  * `eclipse-cdt` from `4.16` to `4.20`
  * `eclipse-groovy` from `4.19` to `4.20`
  * `eclipse-jdt` from `4.19` to `4.20`
  * `eclipse-wtp` from `4.18` to `4.20`

## [2.13.0] - 2021-09-04
### Added
* Added support for `google-java-format`'s `skip-reflowing-long-strings` option ([#929](https://github.com/diffplug/spotless/pull/929))

## [2.12.3] - 2021-08-20
### Changed
* Added support for [scalafmt 3.0.0](https://github.com/scalameta/scalafmt/releases/tag/v3.0.0) and bump default scalafmt version to `3.0.0` ([#913](https://github.com/diffplug/spotless/pull/913)).
* Bump default versions ([#915](https://github.com/diffplug/spotless/pull/915))
  * `ktfmt` from `0.24` to `0.27`
  * `ktlint` from `0.35.0` to `0.42.1`
  * `google-java-format` from `1.10.0` to `1.11.0`

## [2.12.2] - 2021-07-20
### Fixed
 * Improved [SQL formatting](https://github.com/diffplug/spotless/pull/897) with respect to comments

## [2.12.1] - 2021-06-17

### Fixed
* Fixed IndexOutOfBoundsException in parallel execution of `eclipse-groovy` formatter ([#877](https://github.com/diffplug/spotless/issues/877))

## [2.12.0] - 2021-06-10
### Added
* Added support for `eclipse-cdt` at `4.19.0`. Note that version requires Java 11 or higher.
* Added support for `eclipse-groovy` at `4.18.0` and `4.19.0`.
* Added support for `eclipse-wtp` at `4.19.0`. Note that version requires Java 11 or higher.
### Changed
* Bump `eclipse-groovy` default version from `4.17.0` to `4.19.0`.

## [2.11.1] - 2021-05-13
### Fixed
* Node is re-installed if some other build step removed it ([#863](https://github.com/diffplug/spotless/issues/863))

## [2.11.0] - 2021-05-03
### Added
* Added support for [python](README.md#python), specifically [black](README.md#black).
### Changed
* Update ktfmt from 0.21 to 0.24
### Fixed
* The `<url>` field in the maven POM is now set correctly ([#798](https://github.com/diffplug/spotless/issues/798))

## [2.10.3] - 2021-04-21
### Fixed
* Explicitly separate target file from git arguments when parsing year for license header to prevent command from failing on argument-like paths ([#847](https://github.com/diffplug/spotless/pull/847))

## [2.10.2] - 2021-04-20
### Fixed
* LicenseHeaderStep treats address as copyright year ([#716](https://github.com/diffplug/spotless/issues/716))

## [2.10.1] - 2021-04-13
### Fixed
* Fix license header bug for years in range ([#840](https://github.com/diffplug/spotless/pull/840)).

## [2.10.0] - 2021-04-10
### Added
* Added support for `eclipse-jdt` at `4.19.0`.
### Changed
* Bump `eclipse-jdt` default version from `4.18.0` to `4.19.0`.
* Bump `google-java-format` default version from `1.9` to `1.10.0`.
* Expose configuration exceptions from scalafmt ([#837](https://github.com/diffplug/spotless/issues/837))

## [2.9.0] - 2021-03-05
### Added
* Bump ktfmt to 0.21 and add support to Google and Kotlinlang formats ([#812](https://github.com/diffplug/spotless/pull/812))

## [2.8.1] - 2021-02-16
### Fixed
* Allow licence headers to be blank ([#801](https://github.com/diffplug/spotless/pull/801)).

## [2.8.0] - 2021-02-09
### Added
* Support for diktat ([#789](https://github.com/diffplug/spotless/pull/789))

## [2.7.0] - 2021-01-04
### Added
* Added ability to specify dropbox style for ktfmt `<style>DROPBOX</style>` ([#764](https://github.com/diffplug/spotless/pull/764))
* Added support for `eclipse-cdt`, `eclipse-jdt`, and `eclipse-wtp` at `4.18.0`.
### Changed
* Bump `eclipse-jdt` default version from `4.17.0` to `4.18.0`.
* Bump `eclipse-wtp` default version from `4.17.0` to `4.18.0`.
* Bump `ktfmt` default version from `0.16` to `0.19` ([#748](https://github.com/diffplug/spotless/issues/748) and [#773](https://github.com/diffplug/spotless/issues/773)).
### Fixed
* Fixed `ratchetFrom` support for git-submodule ([#746](https://github.com/diffplug/spotless/issues/746)).
* Fixed `ratchetFrom` excess memory consumption ([#735](https://github.com/diffplug/spotless/issues/735)).
* `ktfmt` v0.19+ with dropbox-style works again ([#765](https://github.com/diffplug/spotless/pull/765)).
* `prettier` no longer throws errors on empty files ([#751](https://github.com/diffplug/spotless/pull/751)).
* Fixed error when running on root of windows mountpoint ([#760](https://github.com/diffplug/spotless/pull/760)).
* Fix broken test for spotlessFiles parameter on windows ([#737](https://github.com/diffplug/spotless/pull/737)).

## [2.6.1] - 2020-11-16
### Fixed
* Fixed a bug which occurred if the root directory of the project was also the filesystem root ([#732](https://github.com/diffplug/spotless/pull/732)).
* Upgraded org.codehaus.plexus:plexus-utils to its latest version (3.3.0) to improve directory scanning time ([#729](https://github.com/diffplug/spotless/pull/729)).
  * Whether this helps with the directory scanning time is unconfirmed, please report your experience in the issue above.

## [2.6.0] - 2020-11-13
### Added
* Added support to npm-based steps for picking up `.npmrc` files ([#727](https://github.com/diffplug/spotless/pull/727))
### Fixed
* Fixed bug in import order which woudld cause trailing empty strings to get dropped ([731](https://github.com/diffplug/spotless/issues/731))
  * e.g. `<importorder><order>java,javafx,com.mycompany,</order></importorder>`
* Bump JGit from `5.8.0` to `5.9.0` to improve performance ([#726](https://github.com/diffplug/spotless/issues/726))

## [2.5.0] - 2020-10-20
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

## [2.4.2] - 2020-10-05
### Fixed
* Improve speed by ~4x when using `<ratchetFrom>` ([#701](https://github.com/diffplug/spotless/pull/706)).

## [2.4.1] - 2020-09-18
### Fixed
* Don't assume that file content passed into Prettier is at least 50 characters (https://github.com/diffplug/spotless/pull/699).

## [2.4.0] - 2020-09-17
### Added
* Added support for groovy formatting ([#698](https://github.com/diffplug/spotless/pull/697)).
* Added support for sql formatting ([#698](https://github.com/diffplug/spotless/pull/698)).

## [2.3.1] - 2020-09-12
### Fixed
* Improved JRE parsing to handle strings like `16-loom` (fixes [#693](https://github.com/diffplug/spotless/issues/693)).
* Added support for groovy formatting ([#697](https://github.com/diffplug/spotless/pull/697))

## [2.3.0] - 2020-09-11
### Added
* New option [`<toggleOffOn />`](README.md#spotlessoff-and-spotlesson) which allows the tags `spotless:off` and `spotless:on` to protect sections of code from the rest of the formatters ([#691](https://github.com/diffplug/spotless/pull/691)).
### Changed
* When applying license headers for the first time, we are now more lenient about parsing existing years from the header ([#690](https://github.com/diffplug/spotless/pull/690)).

## [2.2.0] - 2020-09-08
### Added
* `<googleJavaFormat>` default version is now `1.9` on JDK 11+, while continuing to be `1.7` on earlier JDKs. This is especially helpful to `<removeUnusedImports />`, since it always uses the default version of GJF (fixes [#681](https://github.com/diffplug/spotless/issues/681)).

## [2.1.0] - 2020-08-29
### Added
* Added support for  eclipse-jdt 4.14.0, 4.15.0 and 4.16.0 ([#678](https://github.com/diffplug/spotless/pull/678)).
### Changed
* Updated default eclipse-jdt from 4.13.0 to 4.16.0 ([#678](https://github.com/diffplug/spotless/pull/678)).

## [2.0.3] - 2020-08-21
### Fixed
* `<ktlint>` is now more robust when parsing version string for version-dependent implementation details, fixes [#668](https://github.com/diffplug/spotless/issues/668).

## [2.0.2] - 2020-08-10
### Changes
*  Bump default ktfmt from 0.13 to 0.16 ([#642](https://github.com/diffplug/spotless/pull/648)).

### Fixed
* `<importOrder />` was broken (fixes [#663](https://github.com/diffplug/spotless/issues/663)).
* `<ratchetFrom>` was broken when set at global level (fixes [#664](https://github.com/diffplug/spotless/issues/664)).

## [2.0.1] - 2020-07-04
### Fixed
* Git-native handling of line endings was broken, now fixed ([#639](https://github.com/diffplug/spotless/pull/639)).

## [2.0.0] - 2020-07-02
### Added
* You can now ratchet a project's style by limiting Spotless only to files which have changed since a given [git reference](https://javadoc.io/doc/org.eclipse.jgit/org.eclipse.jgit/5.6.1.202002131546-r/org/eclipse/jgit/lib/Repository.html#resolve-java.lang.String-), e.g. `ratchetFrom 'origin/main'`. ([#590](https://github.com/diffplug/spotless/pull/590))
* Huge speed improvement for multi-module projects thanks to improved cross-project classloader caching ([#571](https://github.com/diffplug/spotless/pull/571), fixes [#559](https://github.com/diffplug/spotless/issues/559)).
* If you specify `-DspotlessSetLicenseHeaderYearsFromGitHistory=true`, Spotless will perform an expensive search through git history to determine the oldest and newest commits for each file, and uses that to determine license header years. ([#626](https://github.com/diffplug/spotless/pull/626))
* `prettier` will now autodetect the parser (and formatter) to use based on the filename, unless you override this using `config` or `configFile` with the option `parser` or `filepath` ([#620](https://github.com/diffplug/spotless/pull/620)).
* Added ANTLR4 support ([#326](https://github.com/diffplug/spotless/issues/326)).
### Removed
* **BREAKING** the default includes for `<typescript>` and `<cpp>` were removed, and will now generate an error if an `<include>` is not specified.  There is no well-established convention for these languages in the maven ecosystem, and the performance of the default includes is far worse than a user-provided one.  If you dislike this change, please complain in [#634](https://github.com/diffplug/spotless/pull/634), it would not be a breaking change to bring the defaults back.
* **BREAKING** inside the `<cpp>` block, `<eclipse>` has been renamed to `<eclipseCdt>` to avoid any confusion with the java `<eclipse>` ([#636](https://github.com/diffplug/spotless/pull/636)).
* **BREAKING** the long-deprecated `<xml>` and `<css>` formats have been removed, in favor of the long-available [`<eclipseWtp>`](https://github.com/diffplug/spotless/tree/main/plugin-maven#eclipse-wtp) step which is available in every generic format ([#630](https://github.com/diffplug/spotless/pull/630)).
  * This probably doesn't affect you, but if it does, you just need to change `<xml>...` into `<formats><format><eclipseWtp><type>XML</type>...`
  * In [`1.15.0` (released 2018-09-23)](#1150---2018-09-23), we added support for `xml` and `css` formats using the Eclipse WTP.
  * In [`1.18.0` (released 2019-02-11)](#1180---2019-02-11), we deprecated these, in favor of the generic `eclipseWtp` step which is available for all generic formats.  This allows you to have multiple XML and CSS formats, rather than just one.
  * And now we removed them entirely.

## [1.31.3] - 2020-06-17
### Changed
* Nodejs-based formatters `prettier` and `tsfmt` now use native node instead of the J2V8 approach. ([#606](https://github.com/diffplug/spotless/pull/606))
  * This removes the dependency to the no-longer-maintained Linux/Windows/macOs variants of J2V8.
  * This enables spotless to use the latest `prettier` versions (instead of being stuck at prettier version <= `1.19.0`)
  * Bumped default versions, prettier `1.16.4` -> `2.0.5`, tslint `5.12.1` -> `6.1.2`
### Fixed
* `licenseHeader` is now more robust when parsing years from existing license headers. ([#593](https://github.com/diffplug/spotless/pull/593))

## [1.31.2] - 2020-06-01
### Fixed
* Shared library used by the nodejs-based steps used to be extracted into the user home directory, but now it is extracted into a temporary directory and deleted on VM shutdown. ([#586](https://github.com/diffplug/spotless/pull/586))
* If you specified a config file for a formatter, it used to be needlessly copied to a randomly-named file in the build folder.  This could cause performance to suffer, especially for [large multi-project builds that use eclipse](https://github.com/diffplug/spotless/issues/559). ([#572](https://github.com/diffplug/spotless/pull/572))
  * Note: if you are extracting config files from resource jars, we still have bad performance for this case, see [#559](https://github.com/diffplug/spotless/issues/559) for details.

## [1.31.1] - 2020-05-21
### Fixed
* If the encoding was set incorrectly, `spotless:apply` could clobber special characters.  Spotless now prevents this, and helps to suggest the correct encoding. ([#575](https://github.com/diffplug/spotless/pull/575))

## [1.31.0] - 2020-05-05
### Added
* Support for google-java-format 1.8 (requires build to run on Java 11+) ([#562](https://github.com/diffplug/spotless/issues/562))
* Support for ktfmt 0.13 (requires build to run on Java 11+) ([#569](https://github.com/diffplug/spotless/pull/569))
* `mvn spotless:apply` is now guaranteed to be idempotent, even if some of the formatters are not.  See [`PADDEDCELL.md` for details](https://github.com/diffplug/spotless/blob/main/PADDEDCELL.md) if you're curious. ([#565](https://github.com/diffplug/spotless/pull/565))
* Updated a bunch of dependencies, most notably jgit `5.5.0.201909110433-r` -> `5.7.0.202003110725-r`. ([#564](https://github.com/diffplug/spotless/pull/564))

## [1.30.0] - 2020-04-10
### Added
* Support for prettier ([#555](https://github.com/diffplug/spotless/pull/555)).

## [1.29.0] - 2020-04-02
### Added
* Support for tsfmt ([#548](https://github.com/diffplug/spotless/pull/548)).
### Fixed
* Eclipse-WTP formatter (web tools platform, not java) handles some character encodings incorrectly on OS with non-unicode default file encoding [#545](https://github.com/diffplug/spotless/issues/545). Fixed for Eclipse-WTP formatter Eclipse version 4.13.0 (default version).

## [1.28.0] - 2020-03-20
### Added
* Enable IntelliJ-compatible token `$today.year` for specifying the year in license header files. ([#542](https://github.com/diffplug/spotless/pull/542))
### Fixed
* Fix scala and kotlin maven config documentation.
* Eclipse-WTP formatter (web tools platform, not java) could encounter errors in parallel multiproject builds [#492](https://github.com/diffplug/spotless/issues/492). Fixed for Eclipse-WTP formatter Eclipse version 4.13.0 (default version).

## [1.27.0] - 2020-01-01
* Should be no changes whatsoever!  Released only for consistency with lib and plugin-gradle.

## [1.26.1] - 2019-11-27
* Revert the change in console display of errors from 1.26.0 ([#485](https://github.com/diffplug/spotless/pull/485)) because [of these problems](https://github.com/diffplug/spotless/pull/485#issuecomment-552925932).
* Bugfix: Fix NPE in EclipseXmlFormatterStepImpl ([#489](https://github.com/diffplug/spotless/pull/489))

## [1.26.0] - 2019-11-11
* Fix project URLs in poms. ([#478](https://github.com/diffplug/spotless/pull/478))
* Fix `ImportSorter` crashing with empty files. ([#474](https://github.com/diffplug/spotless/pull/474))
  * Fixes [#305](https://github.com/diffplug/spotless/issues/305) StringIndexOutOfBoundsException for empty Groovy file when performing importOrder
* Bugfix: CDT version `4.12.0` now properly uses `9.8`, whereas before it used `9.7`. ([#482](https://github.com/diffplug/spotless/pull/482#discussion_r341380884))
* Updated default eclipse-wtp from 4.12.0 to 4.13.0 ([#482](https://github.com/diffplug/spotless/pull/482))
* Updated default eclipse-groovy from 4.12.0 to 4.13.0 ([#482](https://github.com/diffplug/spotless/pull/482))
* Updated default eclipse-jdt from 4.12.0 to 4.13.0 ([#482](https://github.com/diffplug/spotless/pull/482))
* Updated default eclipse-cdt from 4.12.0 to 4.13.0 ([#482](https://github.com/diffplug/spotless/pull/482))
* Bump default version of KtLint from `0.34.2` to `0.35.0`. ([#473](https://github.com/diffplug/spotless/issues/473))
* Several improvements to the console display of formatting errors. ([#465](https://github.com/diffplug/spotless/pull/465))
    * Visualize \r and \n as ␍ and ␊ when possible ([#465](https://github.com/diffplug/spotless/pull/465))
    * Make end-of-lines visible when file contains whitespace and end-of-line issues at the same time ([#465](https://github.com/diffplug/spotless/pull/465))
    * Print actual diff line instead of "1 more lines that didn't fit" ([#467](https://github.com/diffplug/spotless/issues/467))

## [1.25.1] - 2019-10-07
* Fixed problem which could cause a stale `.jar` to be published. ([#471](https://github.com/diffplug/spotless/pull/471))

## [1.25.0] - 2019-10-06
* **KNOWN ISSUE:** published jar is the same as `1.24.3`, causes `Invalid plugin descriptor`. ([#470](https://github.com/diffplug/spotless/issues/470))
* Add support for ktlint `0.34+`, and bump default version from `0.32.0` to `0.34.2`. ([#469](https://github.com/diffplug/spotless/pull/469))

## [1.24.3] - 2019-09-23
* Update jgit from `5.3.2.201906051522-r` to `5.5.0.201909110433-r`. ([#445](https://github.com/diffplug/spotless/pull/445))
  * Fixes [#410](https://github.com/diffplug/spotless/issues/410) AccessDeniedException in MinGW/ GitBash.
  * Also fixes occasional [hang on NFS due to filesystem timers](https://github.com/diffplug/spotless/pull/407#issuecomment-514824364).
* Eclipse-based formatters used to leave temporary files around ([#447](https://github.com/diffplug/spotless/issues/447)). This is now fixed, but only for eclipse 4.12+, no back-port to older Eclipse formatter versions is planned. ([#451](https://github.com/diffplug/spotless/issues/451))
* Bumped `scalafmt` default version from `1.1.0` to `2.0.1`, since there are [bugs](https://github.com/diffplug/spotless/issues/454) in the old default ([#458](https://github.com/diffplug/spotless/pull/458)).

## [1.24.1] - 2019-08-12
* Fixes class loading issue with Java 9+ ([#426](https://github.com/diffplug/spotless/pull/426)).

## [1.24.0] - 2019-07-29
* Updated default eclipse-wtp from 4.8.0 to 4.12.0 ([#423](https://github.com/diffplug/spotless/pull/423)).
* Updated default eclipse-groovy from 4.10 to 4.12.0 ([#423](https://github.com/diffplug/spotless/pull/423)).
* Updated default eclipse-jdt from 4.11.0 to 4.12.0 ([#423](https://github.com/diffplug/spotless/pull/423)).
* Updated default eclipse-cdt from 4.11.0 to 4.12.0 ([#423](https://github.com/diffplug/spotless/pull/423)).
    * **KNOWN BUG - accidentally published CDT 9.7 rather than 9.8 fixed in 1.26.0**
* Added new maven coordinates for scalafmt 2.0.0+, maintains backwards compatability ([#415](https://github.com/diffplug/spotless/issues/415))

## [1.23.1] - 2019-06-17
* Fixes incorrect M2 cache directory path handling of Eclipse based formatters ([#401](https://github.com/diffplug/spotless/issues/401))
* Update jgit from `4.9.0.201710071750-r` to `5.3.2.201906051522-r` because gradle project is sometimes broken by `apache httpcomponents` in transitive dependency. ([#407](https://github.com/diffplug/spotless/pull/407))

## [1.23.0] - 2019-04-24
* Updated default ktlint from 0.21.0 to 0.32.0, and Maven coords to com.pinterest ([#394](https://github.com/diffplug/spotless/pull/394))

## [1.22.0] - 2019-04-15
* Updated default eclipse-cdt from 4.7.3a to 4.11.0 ([#390](https://github.com/diffplug/spotless/pull/390)).
* Added `-DspotlessFiles` switch to allow targeting specific files ([#392](https://github.com/diffplug/spotless/pull/392))

## [1.21.1] - 2019-03-29
* Fixes incorrect plugin and pom metadata in `1.21.0` ([#388](https://github.com/diffplug/spotless/issues/388)).

## [1.21.0] - 2019-03-28
* Updated default eclipse-wtp from 4.7.3b to 4.8.0 ([#382](https://github.com/diffplug/spotless/pull/382)).
* Updated default eclipse-groovy from 4.8.1 to 4.10.0 ([#382](https://github.com/diffplug/spotless/pull/382)).
* Updated default eclipse-jdt from 4.10.0 to 4.11.0 ([#384](https://github.com/diffplug/spotless/pull/384)).

## [1.20.0] - 2019-03-14
* Updated default eclipse-wtp from 4.7.3a to 4.7.3b ([#371](https://github.com/diffplug/spotless/pull/371)).
* Default behavior of XML formatter changed to ignore  external URIs ([#369](https://github.com/diffplug/spotless/issues/369)).
  * **WARNING RESOLVED: By default, xml formatter no longer downloads external entities. You can opt-in to resolve external entities by setting resolveExternalURI to true. However, if you do opt-in, be sure that all external entities are referenced over https and not http, or you may be vulnerable to XXE attacks.**

## [1.19.0] - 2019-03-11
**WARNING: xml formatter in this version may be vulnerable to XXE attacks, fixed in 1.20.0 (see [#358](https://github.com/diffplug/spotless/issues/358)).**

* Security fix: Updated groovy, c/c++, and eclipse WTP formatters so that they download their source jars securely using `https` rather than `http` ([#360](https://github.com/diffplug/spotless/issues/360)).
* Updated default eclipse-jdt from 4.9.0 to 4.10.0 ([#368](https://github.com/diffplug/spotless/pull/368))
* Add a skip parameter to apply mojo to enable to bypass it if desired. ([#367](https://github.com/diffplug/spotless/pull/367)).

## [1.18.0] - 2019-02-11
**WARNING: xml formatter in this version may be vulnerable to XXE attacks, fixed in 1.20.0 (see [#358](https://github.com/diffplug/spotless/issues/358)).**

* Provided eclipse-wtp formatters as part of custom source format element. ([#325](https://github.com/diffplug/spotless/pull/325)). This change obsoletes the CSS and XML source elements.
* Updated default google-java-format from 1.5 to 1.7 ([#335](https://github.com/diffplug/spotless/issues/335)).
* `<importOrder><file>somefile</file></importOrder>` is now lazy ([#218](https://github.com/diffplug/spotless/issues/218)).

## [1.17.0] - 2018-12-13
**WARNING: xml formatter in this version may be vulnerable to XXE attacks, fixed in 1.20.0 (see [#358](https://github.com/diffplug/spotless/issues/358)).**

* Updated default eclipse-jdt from 4.7.3a to 4.9.0 ([#316](https://github.com/diffplug/spotless/pull/316)). New version addresses enum-tab formatting bug in 4.8 ([#314](https://github.com/diffplug/spotless/issues/314)).

## [1.16.0] - 2018-10-30
**WARNING: xml formatter in this version may be vulnerable to XXE attacks, fixed in 1.20.0 (see [#358](https://github.com/diffplug/spotless/issues/358)).**

* Added support for Eclipse's CSS formatter from WTP ([#311](https://github.com/diffplug/spotless/pull/311)).

## [1.15.0] - 2018-09-23
**WARNING: xml formatter in this version may be vulnerable to XXE attacks, fixed in 1.20.0 (see [#358](https://github.com/diffplug/spotless/issues/358)).**

* Added `xml` support ([#140](https://github.com/diffplug/spotless/issues/140)) using formatter of Eclipse WTP 3.9.5 ([#241](https://github.com/diffplug/spotless/pull/241)).
* Added C/C++ support using formatter of Eclipse CDT 9.4.3 ([#232](https://github.com/diffplug/spotless/issues/232)).
* Skip `package-info.java` and `module-info.java` files from license header formatting. ([#273](https://github.com/diffplug/spotless/pull/273))
* Updated JSR305 annotation from 3.0.0 to 3.0.2 ([#274](https://github.com/diffplug/spotless/pull/274))
* Migrated from FindBugs annotations 3.0.0 to SpotBugs annotations 3.1.6 ([#274](https://github.com/diffplug/spotless/pull/274))
* Fix Maven version prerequisite in the generated POM ([#289](https://github.com/diffplug/spotless/pull/289))

## [1.14.0] - 2018-07-24
* Updated default eclipse-jdt from 4.7.2 to 4.7.3a ([#263](https://github.com/diffplug/spotless/issues/263)). New version fixes a bug preventing Java code formatting within JavaDoc comments ([#191](https://github.com/diffplug/spotless/issues/191)).
* Updated default groovy-eclipse from 4.6.3 to 4.8.0 ([#244](https://github.com/diffplug/spotless/pull/244)). New version allows to ignore internal formatter errors/warnings.
* Require 3.1.0+ version of Maven. ([#259](https://github.com/diffplug/spotless/pull/259))
* Fixed integration with latest versions of scalafmt. ([#260](https://github.com/diffplug/spotless/pull/260))

## [1.13.0] - 2018-06-01
* Fixed a bug in configuration file resolution on Windows when file is denoted by a URL. ([#254](https://github.com/diffplug/spotless/pull/254))

## [1.0.0.BETA5] - 2018-05-14
* Fixed a bug in `LicenseHeaderStep` which caused an exception with some malformed date-aware licenses. ([#222](https://github.com/diffplug/spotless/pull/222))
* Added support for Kotlin and Ktlint in Maven plugin ([#223](https://github.com/diffplug/spotless/pull/223)).
* Updated default ktlint from 0.14.0 to 0.21.0
* Added support for multiple generic formatters in Maven plugin ([#242](https://github.com/diffplug/spotless/pull/242)).

## [1.0.0.BETA4] - 2018-02-27
* Fixed published POM to include dependency on plexus-resources ([#213](https://github.com/diffplug/spotless/pull/213)).

## [1.0.0.BETA3] - 2018-02-26
* Improved support for multi-module Maven projects ([#210](https://github.com/diffplug/spotless/pull/210)).
* Added generic format support for maven-plugin ([#209](https://github.com/diffplug/spotless/pull/209)).

## [1.0.0.BETA2] - 2018-02-15
* Fix build to ensure that published versions never have snapshot deps ([#205](https://github.com/diffplug/spotless/pull/205)).

## [1.0.0.BETA1] - 2018-02-11
* Maven plugin written by [Konstantin Lutovich](https://github.com/lutovich).
* Full support for the Java and Scala formatters.
* Initial release, after user feedback we will ship `1.x`.
