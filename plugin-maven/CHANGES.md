# spotless-plugin-maven releases

We adhere to the [keepachangelog](https://keepachangelog.com/en/1.0.0/) format (starting after version `1.27.0`).

## [Unreleased]
###
* Prettier Maven Plugin ([#555](https://github.com/diffplug/spotless/pull/555))

## [1.29.0] - 2020-04-02
### Added
* Tsfmt Maven Plugin ([#548](https://github.com/diffplug/spotless/pull/548))
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
