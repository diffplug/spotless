# spotless-lib and spotless-lib-extra releases

If you are a Spotless user (as opposed to developer), then you are probably looking for:

- [plugin-gradle/CHANGES.md](plugin-gradle/CHANGES.md)
- [plugin-maven/CHANGES.md](plugin-maven/CHANGES.md)

This document is intended for Spotless developers.

We adhere to the [keepachangelog](https://keepachangelog.com/en/1.0.0/) format (starting after version `1.27.0`).

## [Unreleased]
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
  * gradle `6.2.2` -> `6.3`
  * spotbugs gradle plugin `2.0.0` -> `4.0.8`

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
* We no longer publish `-SNAPSHOT` for every build to `master`, since we have good [JitPack integration](https://github.com/diffplug/spotless/blob/master/CONTRIBUTING.md#gradle---any-commit-in-a-public-github-repo-this-one-or-any-fork). ([#508](https://github.com/diffplug/spotless/pull/508))
* Improved how we use Spotless on itself. ([#509](https://github.com/diffplug/spotless/pull/509))
* Fix build warnings when building on Gradle 6+, bump build gradle to 6.2.2, and fix javadoc links. ([#536](https://github.com/diffplug/spotless/pull/536))

## [1.27.0] - 2020-01-01
* Ignored `KtLintStepTest`, because [gradle/gradle#11752](https://github.com/gradle/gradle/issues/11752) is causing too many CI failures. ([#499](https://github.com/diffplug/spotless/pull/499))
    * Also fixed a minor problem in TestProvisioner.
* If you set the environment variable `SPOTLESS_EXCLUDE_MAVEN=true` then the maven plugin will be excluded from the build. ([#502](https://github.com/diffplug/spotless/pull/502))
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
    - only affects the gradle plugin, since that was the only plugin to use this feature
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
* Added new maven coordinates for scalafmt 2.0.0+, maintains backwards compatability ([#415](https://github.com/diffplug/spotless/issues/415))

## [1.23.1] - 2019-06-17
* Fixes incorrect M2 cache directory path handling of Eclipse based formatters ([#401](https://github.com/diffplug/spotless/issues/401))
* Update jgit from `4.9.0.201710071750-r` to `5.3.2.201906051522-r` because gradle project is sometimes broken by `apache httpcomponents` in transitive dependency. ([#407](https://github.com/diffplug/spotless/pull/407))

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
* Configured `buìld-scan` plugin in build ([#356](https://github.com/diffplug/spotless/pull/356)).
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
* Add ability to pass custom options to ktlint in gradle plugin. See plugin-gradle/README for details.

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
