# spotless-plugin-maven releases

We adhere to the [keepachangelog](https://keepachangelog.com/en/1.0.0/) format (starting after version `1.27.0`).

## [Unreleased]
### Added
* Added support for eclipse-cdt 4.18.0.
* Added support for eclipse-jdt 4.18.0.
* Added support for eclipse-wtp 4.18.0.
### Changed
* Updated default eclipse-jdt from 4.17.0 to 4.18.0.
* Updated default eclipse-wtp from 4.17.0 to 4.18.0.
### Fixed
* `ratchetFrom` now works with git-submodule ([#746](https://github.com/diffplug/spotless/issues/746))
* Fix broken test for spotlessFiles parameter on windows ([#737](https://github.com/diffplug/spotless/pull/737))
* ktfmt v0.19+ with dropbox-style works again.

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
* You can now ratchet a project's style by limiting Spotless only to files which have changed since a given [git reference](https://javadoc.io/static/org.eclipse.jgit/org.eclipse.jgit/5.6.1.202002131546-r/org/eclipse/jgit/lib/Repository.html#resolve-java.lang.String-), e.g. `ratchetFrom 'origin/main'`. ([#590](https://github.com/diffplug/spotless/pull/590))
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
