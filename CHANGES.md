# spotless-lib and spotless-lib-extra releases

If you are a Spotless user (as opposed to developer), then you are probably looking for:

- [plugin-gradle/CHANGES.md](plugin-gradle/CHANGES.md)
- [plugin-maven/CHANGES.md](plugin-maven/CHANGES.md)

This document is intended for Spotless developers.

We adhere to the [keepachangelog](https://keepachangelog.com/en/1.0.0/) format (starting after version `1.27.0`).

## [Unreleased]
### Changed
* Added support and bump Eclipse formatter default versions to `4.21` for `eclipse-groovy`. Change is only applied for JVM 11+.

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
* The `<url>` field in the maven POM is now set correctly ([#798](https://github.com/diffplug/spotless/issues/798))
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
* Exposed new methods in `GitRatchet` to support faster ratcheting in the maven plugin ([#706](https://github.com/diffplug/spotless/pull/706)).

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
  * This change allows the maven plugin to cache classloaders across subprojects when loading config resources from the classpath (fixes [#559](https://github.com/diffplug/spotless/issues/559)).
  * This change also allows the gradle plugin to work with the remote buildcache (fixes [#280](https://github.com/diffplug/spotless/issues/280)).
* **BREAKING** `FormatterFunc` no longer `extends ThrowingEx.Function` and `BiFunction`. In a major win for Java's idea of ["target typing"](https://cr.openjdk.java.net/~briangoetz/lambda/lambda-state-final.html), this required no changes anywhere in the codebase except deleting the `extends` part of `FormatterFunc` ([#638](https://github.com/diffplug/spotless/issues/638)).
* **BREAKING** Heavy refactor of the `LicenseHeaderStep` public API.  Doesn't change internal behavior, but makes implementation of the gradle and maven plugins much easier. ([#628](https://github.com/diffplug/spotless/pull/628))
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
* Previously, the nodejs-based steps would throw `UnsatisfiedLinkError` if they were ever used from more than one classloader.  Now they can be used from any number of classloaders (important for gradle build daemon). ([#586](https://github.com/diffplug/spotless/pull/586))

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
* We no longer publish `-SNAPSHOT` for every build to `main`, since we have good [JitPack integration](https://github.com/diffplug/spotless/blob/main/CONTRIBUTING.md#gradle---any-commit-in-a-public-github-repo-this-one-or-any-fork). ([#508](https://github.com/diffplug/spotless/pull/508))
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
