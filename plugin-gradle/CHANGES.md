# spotless-plugin-gradle releases

We adhere to the [keepachangelog](https://keepachangelog.com/en/1.0.0/) format (starting after version `3.27.0`).

## [Unreleased]
### Changed
* Added support and bump Eclipse formatter default versions for JVM 11+. For older JVMs the previous defaults remain.
  * `eclipse-cdt` from `4.16` to `4.20`
  * `eclipse-groovy` from `4.19` to `4.20`
  * `eclipse-jdt` from `4.19` to `4.20`
  * `eclipse-wtp` from `4.18` to `4.20`

## [5.15.0] - 2021-09-04
### Added
* Added support for `google-java-format`'s `skip-reflowing-long-strings` option ([#929](https://github.com/diffplug/spotless/pull/929))

## [5.14.3] - 2021-08-20
### Changed
* Added support for [scalafmt 3.0.0](https://github.com/scalameta/scalafmt/releases/tag/v3.0.0) and bump default scalafmt version to `3.0.0` ([#913](https://github.com/diffplug/spotless/pull/913)).
* Bump default versions ([#915](https://github.com/diffplug/spotless/pull/915))
  * `ktfmt` from `0.24` to `0.27`
  * `ktlint` from `0.35.0` to `0.42.1`
  * `google-java-format` from `1.10.0` to `1.11.0`

## [5.14.2] - 2021-07-20
### Fixed
 * Improved [SQL formatting](https://github.com/diffplug/spotless/pull/897) with respect to comments

## [5.14.1] - 2021-07-06
### Changed
* Improved exception messages for [JSON formatting](https://github.com/diffplug/spotless/pull/885) failures

## [5.14.0] - 2021-06-17

### Added
* Added Gradle configuration [JVM-based JSON formatting](https://github.com/diffplug/spotless/issues/850)
### Fixed
* Fixed IndexOutOfBoundsException in parallel execution of `eclipse-groovy` formatter ([#877](https://github.com/diffplug/spotless/issues/877))

## [5.13.0] - 2021-06-10
### Added
* Added support for `eclipse-cdt` at `4.19.0`. Note that version requires Java 11 or higher.
* Added support for `eclipse-groovy` at `4.18.0` and `4.19.0`.
* Added support for `eclipse-wtp` at `4.19.0`. Note that version requires Java 11 or higher.
### Changed
* Bump `eclipse-groovy` default version from `4.17.0` to `4.19.0`.

## [5.12.5] - 2021-05-13
### Changed
* Update ktfmt from 0.21 to 0.24
### Fixed
* The `<url>` field in the maven POM is now set correctly ([#798](https://github.com/diffplug/spotless/issues/798))
* Node is re-installed if some other build step removed it ([#863](https://github.com/diffplug/spotless/issues/863))

## [5.12.4] - 2021-04-21
### Fixed
* Dependency configurations are now named rather than detached, attempt to fix #815.

## [5.12.3] - 2021-04-21
### Fixed
* Explicitly separate target file from git arguments when parsing year for license header to prevent command from failing on argument-like paths ([#847](https://github.com/diffplug/spotless/pull/847))

## [5.12.2] - 2021-04-20
### Fixed
* LicenseHeaderStep treats address as copyright year ([#716](https://github.com/diffplug/spotless/issues/716))

## [5.12.1] - 2021-04-13
### Fixed
* Fix license header bug for years in range ([#840](https://github.com/diffplug/spotless/pull/840)).

## [5.12.0] - 2021-04-10
### Added
* Added support for `eclipse-jdt` at `4.19.0`.
### Changed
* Bump `eclipse-jdt` default version from `4.18.0` to `4.19.0`.
* Bump `google-java-format` default version from `1.9` to `1.10.0`.
* Expose configuration exceptions from scalafmt ([#837](https://github.com/diffplug/spotless/issues/837))
### Fixed
* Exclude `.git`, `.gradle` and `build` directories when multiple targets are specified ([#835](https://github.com/diffplug/spotless/issues/835)).
  * As part of this fix, `**/blah.txt` is now handled the same as `**/*.txt`, which was always the expected behavior. Very unlikely to cause any user-visible changes in behavior.

## [5.11.1] - 2021-03-26
### Fixed
* Ensure consistent ordering of task inputs for `RegisterDependenciesTask`,bso the task is `up-to-date` when the spotless config has not changed.

## [5.11.0] - 2021-03-05
### Added
* Bump ktfmt to 0.21 and add support to Google and Kotlinlang formats ([#812](https://github.com/diffplug/spotless/pull/812))

## [5.10.2] - 2021-02-16
### Fixed
* Allow licence headers to be blank ([#801](https://github.com/diffplug/spotless/pull/801)).

## [5.10.1] - 2021-02-11
### Fixed
* Fixed the `clean` task when Gradle's configuration cache is enabled ([#796](https://github.com/diffplug/spotless/issues/796))

## [5.10.0] - 2021-02-09
### Added
* Support for diktat in KotlinGradleExtension ([#789](https://github.com/diffplug/spotless/pull/789))

## [5.9.0] - 2021-01-04
### Added
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
* No longer declare inputs on `SpotlessCheck` and `SpotlessApply` since they don't have any outputs (worker task still does up-to-date and caching) ([#741](https://github.com/diffplug/spotless/issues/741)).

## [5.8.2] - 2020-11-16
### Fixed
* Fixed a bug which occurred if the root directory of the project was also the filesystem root ([#732](https://github.com/diffplug/spotless/pull/732))

## [5.8.1] - 2020-11-13
### Fixed
* Bump JGit from `5.8.0` to `5.9.0` to improve performance ([#726](https://github.com/diffplug/spotless/issues/726))

## [5.8.0] - 2020-11-12
### Added
* Added support to npm-based steps for picking up `.npmrc` files ([#727](https://github.com/diffplug/spotless/pull/727))

## [5.7.0] - 2020-10-20
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

## [5.6.1] - 2020-09-21
### Fixed
* `5.6.0` introduced a bug where it was no longer possible to configure a single format twice, e.g. to have two `java{}` blocks in a single file. Fixed by [#702](https://github.com/diffplug/spotless/pull/702).

## [5.6.0] - 2020-09-21
### Added
* [`withinBlocks` allows you to apply rules only to specific sections of files](README.md#inception-languages-within-languages-within), for example formatting javascript within html, code examples within markdown, and things like that (implements [#412](https://github.com/diffplug/spotless/issues/412) - formatting inception).

## [5.5.2] - 2020-09-18
### Fixed
* Don't assume that file content passed into Prettier is at least 50 characters (https://github.com/diffplug/spotless/pull/699).

## [5.5.1] - 2020-09-12
### Fixed
* Improved JRE parsing to handle strings like `16-loom` (fixes [#693](https://github.com/diffplug/spotless/issues/693)).

## [5.5.0] - 2020-09-11
### Added
* New option [`toggleOffOn()`](README.md#spotlessoff-and-spotlesson) which allows the tags `spotless:off` and `spotless:on` to protect sections of code from the rest of the formatters ([#691](https://github.com/diffplug/spotless/pull/691)).
### Changed
* When applying license headers for the first time, we are now more lenient about parsing existing years from the header ([#690](https://github.com/diffplug/spotless/pull/690)).

## [5.4.0] - 2020-09-08
### Added
* `googleJavaFormat()` default version is now `1.9` on JDK 11+, while continuing to be `1.7` on earlier JDKs. This is especially helpful to `removeUnusedImports()`, since it always uses the default version of GJF (fixes [#681](https://github.com/diffplug/spotless/issues/681)).
### Fixed
* We did not proactively check to ensure that the Gradle version was modern enough, now we do (fixes [#684](https://github.com/diffplug/spotless/pull/684)).

## [5.3.0] - 2020-08-29
### Added
* Added support for  eclipse-jdt 4.14.0, 4.15.0 and 4.16.0 ([#678](https://github.com/diffplug/spotless/pull/678)).
### Changed
* Updated default eclipse-jdt from 4.13.0 to 4.16.0 ([#678](https://github.com/diffplug/spotless/pull/678)).

## [5.2.0] - 2020-08-25
### Added
- It is now much easier for Spotless to [integrate formatters with native executables](../CONTRIBUTING.md#integrating-outside-the-jvm). ([#672](https://github.com/diffplug/spotless/pull/672))
  - Added support for [python](README.md#python), specifically [black](README.md#black).
  - Added support for [clang-format](README.md#clang-format) for all formats.
### Fixed
* If you executed `gradlew spotlessCheck` multiple times within a single second (hard in practice, easy for a unit test) you could sometimes get an erroneous failure message.  Fixed in [#671](https://github.com/diffplug/spotless/pull/671).

## [5.1.2] - 2020-08-21
### Fixed
* `ktlint()` is now more robust when parsing version string for version-dependent implementation details, fixes [#668](https://github.com/diffplug/spotless/issues/668).

## [5.1.1] - 2020-08-05
### Fixed
* Depending on the file system, executing `gradle spotlessApply` might change permission on the changed files from `644` to `755`; fixes ([#656](https://github.com/diffplug/spotless/pull/656))
* When using the `prettier` or `tsfmt` steps, if any files were dirty then `spotlessCheck` would fail with `java.net.ConnectException: Connection refused` rather than the proper error message ([#651](https://github.com/diffplug/spotless/issues/651)).
### Changed
* Bump default ktfmt from `0.15` to `0.16`, and remove duplicated logic for the `--dropbox-style` option ([#642](https://github.com/diffplug/spotless/pull/648))

## [5.1.0] - 2020-07-13
### Added
* Bump default ktfmt from 0.13 to 0.15, and add support for the --dropbox-style option ([#641](https://github.com/diffplug/spotless/issues/641)).

## [5.0.0] - 2020-07-12

This release is *exactly* the same as `4.5.1`, except:

- it now has plugin id `com.diffplug.spotless`, rather than `com.diffplug.gradle.spotless` ([why](https://dev.to/nedtwigg/names-in-java-maven-and-gradle-2fm2))
- the minimum required Gradle has bumped from `2.14` to `5.4`
- all deprecated functionality has been removed
- `-PspotlessModern=true` (introduced in [`4.3.0`](#430---2020-06-05)) is now always on

If `id 'com.diffplug.gradle.spotless' version '4.5.1'` works without deprecation warnings, then you can upgrade to `id 'com.diffplug.spotless' version '5.0.0'` and no changes will be required.

* **BREAKING** All deprecated functionality has been removed ([#640](https://github.com/diffplug/spotless/pull/640)).
  * (dev-only) `SpotlessTask` was deleted, and `SpotlessTaskModern` was renamed to `SpotlessTask` (ditto for `SpotlessPlugin` and `SpotlessExtension`).
* Introduced in earlier versions, but formerly gated behind `-PspotlessModern=true`
  * We now calculate incremental builds using the new `InputChanges` rather than the deprecated `IncrementalTaskInputs`. ([#607](https://github.com/diffplug/spotless/pull/607))
  * We now use Gradle's config avoidance APIs. ([#617](https://github.com/diffplug/spotless/pull/617))
  * Spotless no longer creates any tasks eagerly. ([#622](https://github.com/diffplug/spotless/pull/622))
  * **BREAKING** The closures inside each format specification are now executed lazily on task configuration. ([#618](https://github.com/diffplug/spotless/pull/618))

```groovy
String isEager = 'nope'
spotless {
    java {
        isEager = 'yup'
    }
}
println "isEager $isEager"
// 'com.diffplug.gradle.spotless' -> isEager yup
// 'com.diffplug.spotless'        -> isEager nope
```

## [4.5.1] - 2020-07-04
### Fixed
* Git-native handling of line endings was broken, now fixed ([#639](https://github.com/diffplug/spotless/pull/639)).

## [4.5.0] - 2020-07-02
### Added
* Full support for the Gradle buildcache - previously only supported local, now supports remote too. Fixes [#566](https://github.com/diffplug/spotless/issues/566) and [#280](https://github.com/diffplug/spotless/issues/280), via changes in [#621](https://github.com/diffplug/spotless/pull/621) and [#571](https://github.com/diffplug/spotless/pull/571).
* `prettier` will now autodetect the parser (and formatter) to use based on the filename, unless you override this using `config()` or `configFile()` with the option `parser` or `filepath`. ([#620](https://github.com/diffplug/spotless/pull/620))
* (user-invisible) moved the deprecated lib code which was only being used in deprecated parts of `plugin-gradle` into the `.libdeprecated` package. ([#630](https://github.com/diffplug/spotless/pull/630))
* Added ANTLR4 support ([#326](https://github.com/diffplug/spotless/issues/326)).
### Fixed
* LineEndings.GIT_ATTRIBUTES is now a bit more efficient, and paves the way for remote build cache support in Gradle. ([#621](https://github.com/diffplug/spotless/pull/621))
* `ratchetFrom` now ratchets from the merge base of `HEAD` and the specified branch.  This fixes the surprising behavior when a remote branch advanced ([#631](https://github.com/diffplug/spotless/pull/631) fixes [#627](https://github.com/diffplug/spotless/issues/627)).
### Deprecated
* The default targets for `C/C++`, `freshmark`, `sql`, and `typescript` now generate a warning, asking the user to specify a target manually. There is no well-established convention for these languages in the gradle ecosystem, and the performance of the default target is far worse than a user-provided one.  If you dislike this change, please complain in [#634](https://github.com/diffplug/spotless/pull/634).
* `customLazy` and `customLazyGroovy` now generate a warning, asking the user to migrate to `custom`.  There is no longer a performance advantage to `customLazy` in the new modern plugin. See [#635](https://github.com/diffplug/spotless/pull/635/files) for example migrations.
* inside the `cpp { }` block, the `eclipse` step now generates a warning, asking you to switch to `eclipseCdt`.  It is the same underlying step, but the new name clears up any confusion with the more common Java `eclipse`. [#636](https://github.com/diffplug/spotless/pull/635/files)

## [4.4.0] - 2020-06-19
### Added
* It is now possible to have multiple language-specific formats. ([9a02419](https://github.com/diffplug/spotless/pull/618/commits/9a024195982759977108b1d857670459939f4000))

```groovy
import com.diffplug.gradle.spotless.KotlinExtension
spotless {
  kotlin {
    target 'src/**/*.kt'
    ktlint()
  }
  format 'kotlinScript', KotlinExtension, {
    target 'src/**/*.kts'
    ktfmt()
  }
}
```

## [4.3.1] - 2020-06-17
### Changed
* Nodejs-based formatters `prettier` and `tsfmt` now use native node instead of the J2V8 approach. ([#606](https://github.com/diffplug/spotless/pull/606))
  * This removes the dependency to the no-longer-maintained Linux/Windows/macOs variants of J2V8.
  * This enables spotless to use the latest `prettier` versions (instead of being stuck at prettier version <= `1.19.0`)
  * Bumped default versions, prettier `1.16.4` -> `2.0.5`, tslint `5.12.1` -> `6.1.2`
### Fixed
* Using `ratchetFrom 'origin/main'` on a bare checkout generated a cryptic error, now generates a clear error. ([#608](https://github.com/diffplug/spotless/issues/608))
* Using `ratchetFrom 'nonexistent-reference` generated a cryptic error, now generates a clear error. ([#612](https://github.com/diffplug/spotless/issues/612))

## [4.3.0] - 2020-06-05
### Deprecated
* `-PspotlessFiles` has been deprecated and will be removed.  It is slow and error-prone, especially for win/unix cross-platform, and we have better options available now:
  * If you are formatting just one file, try the much faster [IDE hook](https://github.com/diffplug/spotless/blob/main/plugin-gradle/IDE_HOOK.md)
  * If you are integrating with git, try the much easier (and faster) [`ratchetFrom 'origin/main'`](https://github.com/diffplug/spotless/tree/main/plugin-gradle#ratchet)
  * If neither of these work for you, let us know in [this PR](https://github.com/diffplug/spotless/pull/602).
### Added
* If you specify `-PspotlessSetLicenseHeaderYearsFromGitHistory=true`, Spotless will perform an expensive search through git history to determine the oldest and newest commits for each file, and uses that to determine license header years. ([#604](https://github.com/diffplug/spotless/pull/604))
* It is now possible for individual formats to set their own `ratchetFrom` value, similar to how formats can have their own `encoding`. ([#605](https://github.com/diffplug/spotless/pull/605)).
* (spotless devs only) if you specify `-PspotlessModern=true` Spotless will run the in-progress Gradle `5.4+` code.  The `modernTest` build task runs our test suite in this way.  It will be weeks/months before this is recommended for end-users. ([#598](https://github.com/diffplug/spotless/pull/598))

## [4.2.1] - 2020-06-04
### Fixed
* `ratchetFrom` incorrectly marked every file as though it were clean on Windows. ([#596](https://github.com/diffplug/spotless/pull/596))
  * Also large [performance improvement (win and unix) for multiproject builds](https://github.com/diffplug/spotless/pull/597/commits/f66dc8de137a34d14768e83ab3cbff5344539b56). ([#597](https://github.com/diffplug/spotless/pull/597))
* Improved the warning message for `paddedCell` deprecation, along with many API-invisible fixes and cleanup. ([#592](https://github.com/diffplug/spotless/pull/592))

## [4.2.0] - 2020-06-03
### Added
* If you use `ratchetFrom` and `licenseHeader`, the year in your license header will now be automatically kept up-to-date for changed files. For example, if the current year is 2020: ([#593](https://github.com/diffplug/spotless/pull/593))
  * `/** Copyright 2020 */` -> unchanged
  * `/** Copyright 1990 */` -> `/** Copyright 1990-2020 */`
  * `/** Copyright 1990-1993 */` -> `/** Copyright 1990-2020 */`
  * You can disable this behavior with `licenseHeader(...).updateYearWithLatest(false)`, or you can enable it without using `ratchetFrom` by using `updateYearWithLatest(true)` (not recommended).
### Fixed
* `ratchetFrom` had a bug (now fixed) such that it reported all files outside the root directory as changed. ([#594](https://github.com/diffplug/spotless/pull/594))

## [4.1.0] - 2020-06-01
### Added
* You can now ratchet a project's style by limiting Spotless only to files which have changed since a given [git reference](https://javadoc.io/doc/org.eclipse.jgit/org.eclipse.jgit/5.6.1.202002131546-r/org/eclipse/jgit/lib/Repository.html#resolve-java.lang.String-), e.g. `ratchetFrom 'origin/main'`. ([#590](https://github.com/diffplug/spotless/pull/590))
* Support for ktfmt in KotlinGradleExtension. ([#583](https://github.com/diffplug/spotless/pull/583))
### Fixed
* Users can now run `spotlessCheck` and `spotlessApply` in the same build. ([#584](https://github.com/diffplug/spotless/pull/584))
* Fixed intermittent `UnsatisfiedLinkError` in nodejs-based steps. ([#586](https://github.com/diffplug/spotless/pull/586))
  * Also, a shared library used by the nodejs steps used to be extracted into the user home directory, but now it is extracted into `{rootProject}/build/spotless-nodejs-cache`.
* Fixed intermittent `java.nio.file.DirectoryNotEmptyException` on incremental builds where folders had been removed. ([#589](https://github.com/diffplug/spotless/pull/589))
* Starting in `4.0`, it is no longer possible for a project to format files which are not within its project folder (for example, `:a` can no longer format files in `:b`).  We did not explicitly note this in the changelog entry for `4.0`, and we gave a very confusing error message if users tried.  We now give a more helpful error message, and this breaking change has been retroactively noted in the changelog for `4.0.0`. ([#588](https://github.com/diffplug/spotless/pull/588))

## [4.0.1] - 2020-05-21
### Fixed
* If the encoding was set incorrectly, `spotlessApply` could clobber special characters.  Spotless now prevents this, and helps to suggest the correct encoding. ([#575](https://github.com/diffplug/spotless/pull/575))

## [4.0.0] - 2020-05-17
**TLDR: This version improves performance and adds support for the local Gradle Build Cache.  You will not need to make any changes in your buildscript.**  It is a breaking change only for a few users who have built *other* plugins on top of this one.

### Added
* Support for the gradle build cache. ([#576](https://github.com/diffplug/spotless/pull/576))
  * The local cache will work great, but the remote cache will always miss until [#566](https://github.com/diffplug/spotless/issues/566) is resolved.
### Removed
* **BREAKING** it used to be possible for any project to format files in any other project.  For example, `:a` could format files in `:b`.  It is now only possible to format files within the project directory.  It is okay (but not advised) to format files in subprojects, since they are within the project directory.
* (Power users only) **BREAKING** `void SpotlessTask::setCheck()` and `setApply()` have been removed. ([#576](https://github.com/diffplug/spotless/pull/576))
  * Previously, the `check` and `apply` tasks were just marker tasks, and they called `setCheck` and `setApply` on the "worker" task.  Now `check` and `apply` are real tasks in their own right, so the marker-task kludge is no longer necessary.
### Changed
* (Power users only) **BREAKING** `SpotlessTask FormatExtension::createIndependentTask` has been removed, and replaced with `SpotlessApply::createIndependentApplyTask`. ([#576](https://github.com/diffplug/spotless/pull/576))
* Improve suggested gradle invocation for running `spotlessApply`. ([#578](https://github.com/diffplug/spotless/pull/578))

## [3.30.0] - 2020-05-11
### Added
* `-PspotlessIdeHook` which makes the VS Code extension faster and more reliable.  See [`IDE_INTEGRATION.md`](IDE_INTEGRATION.md) for more details. ([#568](https://github.com/diffplug/spotless/pull/568))

## [3.29.0] - 2020-05-05
### Added
* 🎉🎉🎉 [**VS Code Extension**](https://marketplace.visualstudio.com/items?itemName=richardwillis.vscode-spotless-gradle) thanks to [@badsyntax](https://github.com/badsyntax) 🎉🎉🎉
* Support for google-java-format 1.8 (requires build to run on Java 11+) ([#562](https://github.com/diffplug/spotless/issues/562))
* Support for ktfmt 0.13 (requires build to run on Java 11+) ([#569](https://github.com/diffplug/spotless/pull/569))
### Changed
* PaddedCell is now always enabled.  It is strictly better than non-padded cell, and there is no performance penalty.  [See here](https://github.com/diffplug/spotless/pull/560#issuecomment-621752798) for detailed explanation. ([#561](https://github.com/diffplug/spotless/pull/561))
* Updated a bunch of dependencies, most notably jgit `5.5.0.201909110433-r` -> `5.7.0.202003110725-r`. ([#564](https://github.com/diffplug/spotless/pull/564))

## [3.28.1] - 2020-04-02
### Fixed
* Eclipse-WTP formatter (web tools platform, not java) handles some character encodings incorrectly on OS with non-unicode default file encoding [#545](https://github.com/diffplug/spotless/issues/545). Fixed for Eclipse-WTP formatter Eclipse version 4.13.0 (default version).

## [3.28.0] - 2020-03-20
### Added
* Enable IntelliJ-compatible token `$today.year` for specifying the year in license header files. ([#542](https://github.com/diffplug/spotless/pull/542))
### Fixed
* Eclipse-WTP formatter (web tools platform, not java) could encounter errors in parallel multiproject builds [#492](https://github.com/diffplug/spotless/issues/492). Fixed for Eclipse-WTP formatter Eclipse version 4.13.0 (default version).

## [3.27.2] - 2020-03-05
### Fixed
* Add tests to `SpecificFilesTest` to fix [#529](https://github.com/diffplug/spotless/issues/529)
* If you applied spotless to a subproject, but not to the root project, then on Gradle 6+ you would get the deprecation warning `Using method Project#afterEvaluate(Action) when the project is already evaluated has been deprecated.`  This has now been fixed. ([#506](https://github.com/diffplug/spotless/issues/506))

## [3.27.1] - 2020-01-14
### Fixed
* `licenseHeader` and `licenseHeaderFile` accidentally returned a package-private config object, which is now public, fixes ([#505](https://github.com/diffplug/spotless/issues/505)).

## [3.27.0] - 2020-01-01
* Added method `FormatExtension.createIndependentTask(String taskName)` which allows creating a Spotless task outside of the `check`/`apply` lifecycle.  See [javadoc](https://github.com/diffplug/spotless/blob/91ed7203994e52058ea6c2e0f88d023ed290e433/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/FormatExtension.java#L613-L639) for details. ([#500](https://github.com/diffplug/spotless/pull/500))
* Running `clean` and `spotlessCheck` during a parallel build could cause exceptions, fixed by ([#501](https://github.com/diffplug/spotless/pull/501)).
* Fixed Gradle 7 deprecation warnings that started being emitted in Gradle 6. ([#503](https://github.com/diffplug/spotless/pull/503))
  * Even if you're using a pre-6.0 version of Gradle, you will probably see small performance and stability improvements. The PR above finally fixed the root problems of ([#372](https://github.com/diffplug/spotless/issues/372)).

## [3.26.1] - 2019-11-27
* Revert the change in console display of errors from 3.26.0 ([#485](https://github.com/diffplug/spotless/pull/485)) because [of these problems](https://github.com/diffplug/spotless/pull/485#issuecomment-552925932).
* Bugfix: Fix NPE in EclipseXmlFormatterStepImpl ([#489](https://github.com/diffplug/spotless/pull/489))

## [3.26.0] - 2019-11-11
* Fix project URLs in poms. ([#478](https://github.com/diffplug/spotless/pull/478))
* Fix `ImportSorter` crashing with empty files. ([#474](https://github.com/diffplug/spotless/pull/474))
  * Fixes [#305](https://github.com/diffplug/spotless/issues/305) StringIndexOutOfBoundsException for empty Groovy file when performing importOrder
* Bugfix: CDT version `4.12.0` now properly uses `9.8`, whereas before it used `9.7`. ([#482](https://github.com/diffplug/spotless/pull/482#discussion_r341380884))
* Updated default eclipse-wtp from 4.12.0 to 4.13.0 ([#482](https://github.com/diffplug/spotless/pull/482)).
* Updated default eclipse-groovy from 4.12.0 to 4.13.0 ([#482](https://github.com/diffplug/spotless/pull/482)).
* Updated default eclipse-jdt from 4.12.0 to 4.13.0 ([#482](https://github.com/diffplug/spotless/pull/482)).
* Updated default eclipse-cdt from 4.12.0 to 4.13.0 ([#482](https://github.com/diffplug/spotless/pull/482)).
* Bump default version of KtLint from `0.34.2` to `0.35.0`. ([#473](https://github.com/diffplug/spotless/issues/473))
* Several improvements to the console display of formatting errors. ([#465](https://github.com/diffplug/spotless/pull/465))
    * Visualize \r and \n as ␍ and ␊ when possible ([#465](https://github.com/diffplug/spotless/pull/465))
    * Make end-of-lines visible when file contains whitespace and end-of-line issues at the same time ([#465](https://github.com/diffplug/spotless/pull/465))
    * Print actual diff line instead of "1 more lines that didn't fit" ([#467](https://github.com/diffplug/spotless/issues/467))

## [3.25.0] - 2019-10-06
* Spotless no longer breaks configuration avoidance for other tasks (specifically the `check` task and all of its dependees) ([#463](https://github.com/diffplug/spotless/pull/463)).
  * Important change: **Formerly, Spotless did not create its tasks until the `afterEvaluate` phase.  Spotless now creates them as soon as the plugin is applied**, and it creates the format-specific tasks as soon as the formats are defined.  There is no performance degradation associated with this change, and it makes configuring Spotless easier.
* Add support for ktlint `0.34+`, and bump default version from `0.32.0` to `0.34.2`. ([#469](https://github.com/diffplug/spotless/pull/469))

## [3.24.3] - 2019-09-23
* Update jgit from `5.3.2.201906051522-r` to `5.5.0.201909110433-r`. ([#445](https://github.com/diffplug/spotless/pull/445))
  * Fixes [#410](https://github.com/diffplug/spotless/issues/410) AccessDeniedException in MinGW/GitBash.
  * Also fixes occasional [hang on NFS due to filesystem timers](https://github.com/diffplug/spotless/pull/407#issuecomment-514824364).
* Eclipse-based formatters used to leave temporary files around ([#447](https://github.com/diffplug/spotless/issues/447)). This is now fixed, but only for eclipse 4.12+, no back-port to older Eclipse formatter versions is planned. ([#451](https://github.com/diffplug/spotless/issues/451))
* Fixed a bad but simple bug in `paddedCell()` ([#455](https://github.com/diffplug/spotless/pull/455))
    * if a formatter was behaving correctly on a given file (was idempotent)
    * but the file was not properly formatted
    * `spotlessCheck` would improperly say "all good" even though `spotlessApply` would properly change them
    * combined with up-to-date checking, could lead to even more confusing results,
     ([#338](https://github.com/diffplug/spotless/issues/338))
    * Fixed now!
* When you specify `targetExclude()`, spotless no longer silently removes `build` directories from the exclusion ([#457](https://github.com/diffplug/spotless/pull/457)).
* Bumped `scalafmt` default version from `1.1.0` to `2.0.1`, since there are [bugs](https://github.com/diffplug/spotless/issues/454) in the old default ([#458](https://github.com/diffplug/spotless/pull/458)).

## [3.24.2] - 2019-08-19
* Fixed `Warning deprecated usage found: Using the incremental task API without declaring any outputs has been deprecated.` that started appearing in Gradle 5.5 ([#434](https://github.com/diffplug/spotless/pull/434)).

## [3.24.1] - 2019-08-12
* Fixes class loading issue with Java 9+ ([#426](https://github.com/diffplug/spotless/pull/426)).

## [3.24.0] - 2019-07-29
* Updated default eclipse-wtp from 4.8.0 to 4.12.0 ([#423](https://github.com/diffplug/spotless/pull/423)).
* Updated default eclipse-groovy from 4.10 to 4.12.0 ([#423](https://github.com/diffplug/spotless/pull/423)).
* Updated default eclipse-jdt from 4.11.0 to 4.12.0 ([#423](https://github.com/diffplug/spotless/pull/423)).
* Updated default eclipse-cdt from 4.11.0 to 4.12.0 ([#423](https://github.com/diffplug/spotless/pull/423)).
    * **KNOWN BUG - accidentally published CDT 9.7 rather than 9.8 - fixed in 3.26.0**
* Added new maven coordinates for scalafmt 2.0.0+, maintains backwards compatability ([#415](https://github.com/diffplug/spotless/issues/415))

## [3.23.1] - 2019-06-17
* Fixes incorrect M2 cache directory path handling of Eclipse based formatters ([#401](https://github.com/diffplug/spotless/issues/401))
* Update jgit from `4.9.0.201710071750-r` to `5.3.2.201906051522-r` because gradle project is sometimes broken by `apache httpcomponents` in transitive dependency. ([#407](https://github.com/diffplug/spotless/pull/407))

## [3.23.0] - 2019-04-24
* Updated default ktlint from 0.21.0 to 0.32.0, and Maven coords to com.pinterest ([#394](https://github.com/diffplug/spotless/pull/394))

## [3.22.0] - 2019-04-15
* Updated default eclipse-cdt from 4.7.3a to 4.11.0 ([#390](https://github.com/diffplug/spotless/pull/390)).

## [3.21.1] - 2019-03-29
* Fixes incorrect plugin and pom metadata in `3.21.0` ([#388](https://github.com/diffplug/spotless/issues/388)).

## [3.21.0] - 2019-03-28
* Updated default eclipse-wtp from 4.7.3b to 4.8.0 ([#382](https://github.com/diffplug/spotless/pull/382)).
* Updated default eclipse-groovy from 4.8.1 to 4.10.0 ([#382](https://github.com/diffplug/spotless/pull/382)).
* Updated default eclipse-jdt from 4.10.0 to 4.11.0 ([#384](https://github.com/diffplug/spotless/pull/384)).
* Fixed intermittent concurrency error while downloading formatter dependencies in multi-project builds ([#372](https://github.com/diffplug/spotless/issues/372)).

## [3.20.0] - 2019-03-11
* Made npm package versions of [`prettier`](https://prettier.io/) and [`tsfmt`](https://github.com/vvakame/typescript-formatter) (and its internal packages) configurable. ([#363](https://github.com/diffplug/spotless/pull/363))
  * Updated default npm package version of `prettier` from 1.13.4 to 1.16.4
  * Updated default npm package version of internally used typescript package from 2.9.2 to 3.3.3 and tslint package from 5.1.0 to 5.12.0 (both used by `tsfmt`)
* Updated default eclipse-wtp from 4.7.3a to 4.7.3b ([#371](https://github.com/diffplug/spotless/pull/371)).
* Default behavior of XML formatter changed to ignore external URIs ([#369](https://github.com/diffplug/spotless/issues/369)).
  * **WARNING RESOLVED: By default, xml formatter no longer downloads external entities. You can opt-in to resolve external entities by setting resolveExternalURI to true. However, if you do opt-in, be sure that all external entities are referenced over https and not http, or you may be vulnerable to XXE attacks.**

## [3.19.0] - 2019-03-11
**WARNING: xml formatter in this version may be vulnerable to XXE attacks, fixed in 3.20.0 (see [#358](https://github.com/diffplug/spotless/issues/358)).**

* Security fix: Updated groovy, c/c++, and eclipse WTP formatters so that they download their source jars securely using `https` rather than `http` ([#360](https://github.com/diffplug/spotless/issues/360)).
* Updated default eclipse-jdt from 4.9.0 to 4.10.0 ([#368](https://github.com/diffplug/spotless/pull/368))

## [3.18.0] - 2019-02-11
**WARNING: xml formatter in this version may be vulnerable to XXE attacks, fixed in 3.20.0 (see [#358](https://github.com/diffplug/spotless/issues/358)).**

* Provided eclipse-wtp formatters in generic formatter extension. ([#325](https://github.com/diffplug/spotless/pull/325)). This change obsoletes the CSS and XML extensions.
* Improved configuration times for large projects (thanks to @oehme for finding [#348](https://github.com/diffplug/spotless/pull/348)).
* Updated default google-java-format from 1.5 to 1.7 ([#335](https://github.com/diffplug/spotless/issues/335)).
* Replacing a step no longer triggers early evaluation ([#219](https://github.com/diffplug/spotless/issues/219)).
* `importOrderFile(Object file)` for java and groovy is now lazy ([#218](https://github.com/diffplug/spotless/issues/218)).
* added `targetExclude(Object...)` which excludes the given files from processing ([#353](https://github.com/diffplug/spotless/pull/353)).
  * This resolves several related issues:
    * [#153](https://github.com/diffplug/spotless/issues/153) using a `PatternFilterable` to determine source processed by `JavaExtension` and `KotlinExtension`
    * [#194](https://github.com/diffplug/spotless/issues/194) ignoreErrorForPath does not work
    * [#324](https://github.com/diffplug/spotless/issues/324) better support for excluding files from processing
  * Our answer for a long time had been "just use `target(Object...)` to fix this" but there is clearly sufficient demand to justify `targetExclude`.

## [3.17.0] - 2018-12-13
**WARNING: xml formatter in this version may be vulnerable to XXE attacks, fixed in 3.20.0 (see [#358](https://github.com/diffplug/spotless/issues/358)).**

* Updated default eclipse-jdt from 4.7.3a to 4.9.0 ([#316](https://github.com/diffplug/spotless/pull/316)). New version addresses enum-tab formatting bug in 4.8 ([#314](https://github.com/diffplug/spotless/issues/314)).
* Added `-spotlessFiles` switch to allow targeting specific files ([#322](https://github.com/diffplug/spotless/pull/322))

## [3.16.0] - 2018-10-30
**WARNING: xml formatter in this version may be vulnerable to XXE attacks, fixed in 3.20.0 (see [#358](https://github.com/diffplug/spotless/issues/358)).**

* Added support for Eclipse's CSS formatter from WTP ([#311](https://github.com/diffplug/spotless/pull/311)).

## [3.15.0] - 2018-09-23
**WARNING: xml formatter in this version may be vulnerable to XXE attacks, fixed in 3.20.0 (see [#358](https://github.com/diffplug/spotless/issues/358)).**

* Added `xml` support ([#140](https://github.com/diffplug/spotless/issues/140)) using formatter of Eclipse WTP 3.9.5 ([#241](https://github.com/diffplug/spotless/pull/241)).
* Added [`prettier`](https://prettier.io/) and [`tsfmt`](https://github.com/vvakame/typescript-formatter) support ([#283](https://github.com/diffplug/spotless/pull/283)).
* Added C/C++ support using formatter of Eclipse CDT 9.4.3 ([#232](https://github.com/diffplug/spotless/issues/232)).
* Updated default groovy-eclipse from 4.8.0 to 4.8.1 ([#288](https://github.com/diffplug/spotless/pull/288)). New version is based on [Groovy-Eclipse 3.0.0](https://github.com/groovy/groovy-eclipse/wiki/3.0.0-Release-Notes).
* LicenseHeaderStep now wont attempt to add license to `module-info.java` ([#272](https://github.com/diffplug/spotless/pull/272)).
* Updated JSR305 annotation from 3.0.0 to 3.0.2 ([#274](https://github.com/diffplug/spotless/pull/274))
* Migrated from FindBugs annotations 3.0.0 to SpotBugs annotations 3.1.6 ([#274](https://github.com/diffplug/spotless/pull/274))
* Gradle/Groovy `importOrder` no longer adds semicolons. ([#237](https://github.com/diffplug/spotless/issues/237))

## [3.14.0] - 2018-07-24
* Updated default eclipse-jdt from 4.7.2 to 4.7.3a ([#263](https://github.com/diffplug/spotless/issues/263)). New version fixes a bug preventing Java code formatting within JavaDoc comments ([#191](https://github.com/diffplug/spotless/issues/191)).
* Updated default groovy-eclipse from 4.6.3 to 4.8.0 ([#244](https://github.com/diffplug/spotless/pull/244)). New version allows to ignore internal formatter errors/warnings.
* Fixed integration with latest versions of scalafmt. ([#260](https://github.com/diffplug/spotless/pull/260))

## [3.13.0] - 2018-06-01
* Add line and column numbers to ktlint errors. ([#251](https://github.com/diffplug/spotless/pull/251))

## [3.12.0] - 2018-05-14
* Migrated `plugin-gradle`'s tests away from `TaskInternal#execute` to a custom method to help with Gradle 5.0 migration later on. ([#208](https://github.com/diffplug/spotless/pull/208))
* Fixed a bug in `LicenseHeaderStep` which caused an exception with some malformed date-aware licenses. ([#222](https://github.com/diffplug/spotless/pull/222))
* Updated default ktlint from 0.14.0 to 0.21.0
* Add ability to pass custom options to ktlint. See README for details.
* Added interface `HasBuiltinDelimiterForLicense` to language extensions that have pre-defined licence header delimiter. ([#235](https://github.com/diffplug/spotless/pull/235))

## [3.10.0] - 2018-02-15
* LicenseHeaderStep now supports customizing the year range separator in copyright notices. ([#199](https://github.com/diffplug/spotless/pull/199))

## [3.9.0] - 2018-02-05
* Updated default ktlint from 0.6.1 to 0.14.0
* Updated default google-java-format from 1.3 to 1.5
* Updated default eclipse-jdt from 4.7.1 to 4.7.2
* Added a configuration option to `googleJavaFormat` to switch the formatter style ([#193](https://github.com/diffplug/spotless/pull/193))
  + Use `googleJavaFormat().aosp()` to use AOSP-compliant style (4-space indentation) instead of the default Google Style

## [3.8.0] - 2018-01-02
* Bugfix: if the specified target of a spotless task was reduced, Spotless could keep giving warnings until the cache file was deleted.
* LicenseHeader now supports time-aware license headers. ([docs](https://github.com/diffplug/spotless/tree/main/plugin-gradle#license-header), [#179](https://github.com/diffplug/spotless/pull/179), thanks to @baptistemesta)

## [3.7.0] - 2017-12-02
* Updated default eclipse-jdt version to `4.7.1` from `4.6.3`.
* All spotless tasks now run before the `clean` task. ([#159](https://github.com/diffplug/spotless/issues/159))
* Added `sql` ([#166](https://github.com/diffplug/spotless/pull/166)) and `dbeaverSql`. ([#166](https://github.com/diffplug/spotless/pull/166))
  + Many thanks to [Baptiste Mesta](https://github.com/baptistemesta) for porting to Spotless.
  + Many thanks to [DBeaver](https://dbeaver.jkiss.org/) and the [DBeaver contributors](https://github.com/serge-rider/dbeaver/graphs/contributors) for building the implementation.

## [3.6.0] - 2017-09-29
* Fixes a rare up-to-date bug. ([#144](https://github.com/diffplug/spotless/issues/144) and [#146](https://github.com/diffplug/spotless/pull/146))

## [3.5.2] - 2017-09-05
* Fix licenseHeader so it works with Kotlin files starting with `@file:...` instead of `package ...` ([#136](https://github.com/diffplug/spotless/issues/136)).

## [3.5.1] - 2017-08-14
* Fixed `kotlinGradle` linting [Gradle Kotlin DSL](https://github.com/gradle/kotlin-dsl) files throwing `ParseException` ([#132](https://github.com/diffplug/spotless/issues/132)).

## [3.5.0] - 2017-08-13
* Changed `importOrder` interface from array to varargs ([#125](https://github.com/diffplug/spotless/issues/125)).
* The `kotlin` extension was mis-spelled as `kotin`.
* Added `kotlinGradle` method to `SpotlessExtension` for linting [Gradle Kotlin DSL](https://github.com/gradle/kotlin-dsl) files with [ktlint](https://github.com/shyiko/ktlint) ([#115](https://github.com/diffplug/spotless/issues/115))
* Added dedicated `groovyGradle` for formatting of Gradle files.

## [3.4.1] - 2017-07-11
* Default eclipse version for `EclipseFormatterStep` bumped to `4.6.3` from `4.6.1`. ([#116](https://github.com/diffplug/spotless/issues/116))
* Default scalafmt version for `ScalaFmtStep` bumped to `1.1.0` from `0.5.7` ([#124](https://github.com/diffplug/spotless/pull/124))
  + Also added support for the API change to scalafmt introduced in `0.7.0-RC1`
* Fixed wildcard targets for `includeFlat` subprojects ([#121](https://github.com/diffplug/spotless/issues/121))
* When spotless needs to download a formatter, it now uses the buildscript repositories specified in the root buildscript. ([#123](https://github.com/diffplug/spotless/pull/123), [#120](https://github.com/diffplug/spotless/issues/120))

## [3.4.0] - 2017-05-21
* `ImportOrderStep` can now handle multi-line comments and misplaced imports.
* Groovy extension now checks for the `groovy` plugin to be applied.
* Deprecated the old syntax for the the eclipse formatter:
  + New syntax better separates the version from the other configuration options, and is more consistent with the other
  + `eclipseFormatFile('format.xml')` -> `eclipse().configFile('format.xml')`
  + `eclipseFormatFile('4.4.0', 'format.xml')` -> `eclipse('4.4.0').configFile('format.xml')`

## [3.3.2] - 2017-05-03
* Fixed a bug in `paddedCell()` which caused `spotlessCheck` to fail even after `spotlessApply` for cases where a rule is misbehaving and causing a cycle.

## [3.3.0] - 2017-04-11
* Added support for groovy formatting (huge thanks to Frank Vennemeyer! [#94](https://github.com/diffplug/spotless/pull/94), [#89](https://github.com/diffplug/spotless/pull/89), [#88](https://github.com/diffplug/spotless/pull/88), [#85](https://github.com/diffplug/spotless/pull/85))
* When special-purpose formatters need to be downloaded from maven, they are now resolved using the buildscript repositories rather than the project repositories. (thanks to [cal101](https://github.com/cal101) [#100](https://github.com/diffplug/spotless/pull/100))

## [3.2.0] - 2017-04-03
* Update default KtLint from 0.3.1 to 0.6.1 (thanks to @kvnxiao [#93](https://github.com/diffplug/spotless/pull/93)).
  + This means we no longer look for rules in the typo package `com.gihub.shyiko`, now only in `com.github.shyiko` (note the `t`).
* Added an `enforceCheck` property which allows users to disable adding `spotlessCheck` as a dependency of `check` (thanks to @gdecaso [#95](https://github.com/diffplug/spotless/pull/95)).
* Any errors in a step will now fail the build] - 201x-xx-xx previously they were only warned.
  + We claimed that we implemented this in 3.1.0, but it was broken.  We really fixed it this time.

## [3.1.0] - 2017-02-27
* Added support for Scala via [scalafmt](https://github.com/olafurpg/scalafmt).
* Added support for Kotlin via [ktlint](https://github.com/pinterest/ktlint).
* Added `FormatExtension::replaceStep`.
* `paddedCell()` is no longer required if a misbehaving rule converges.
* Any errors in a step will now fail the build] - 201x-xx-xx previously they were only warned.
* Added `FormatExtension::ignoreErrorForStep` and `FormatExtension::ignoreErrorForPath`.
* Bumped `google-java-format` to `1.3`.

## [3.0.0] - 2017-01-09
* BREAKING CHANGE: `customReplace` and `customReplaceRegex` renamed to just `replace` and `replaceRegex`.
* BREAKING CHANGE: Plugin portal ID is still `com.diffplug.gradle.spotless`, but maven coordinate has changed to `com.diffplug.spotless:spotless-plugin-gradle`.
* HUGE SPEEDUP: Now supports incremental build / up-to-date-checking.
  + If you are using `custom` or `customLazy`, you might want to take a look at [this javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/3.27.0/com/diffplug/gradle/spotless/FormatExtension.html#bumpThisNumberIfACustomStepChanges-int-).
* BREAKING CHANGE: `freshmark` no longer includes all project properties by default.  All properties must now be added manually:

```gradle
spotless {
  freshmark {
    propertiesFile('gradle.properties')
    properties {
      it.put('key', 'value')
    }
  }
}
```

* Fixed googleJavaFormat so that it can now sort imports and remove unused imports.
* Added an à la carte `removeUnusedImports()` step.

## [2.4.1] - 2017-01-02
* Java files under the `src/main/groovy` folder are now formatted by default. ([Issue #59](https://github.com/diffplug/spotless/issues/59), [PR #60](https://github.com/diffplug/spotless/pull/60), thanks @ajoberstar).

## [2.4.0] - 2016-11-01
* If a formatter step throws an `Error` or any of its subclasses, such as the `AssertionError`s thrown by JUnit, AssertJ, etc. that error will kill the build ([#46](https://github.com/diffplug/spotless/issues/46))
  + This allows custom rules like this:

```gradle
custom 'no swearing', {
  if (it.toLowerCase().contains('fubar')) {
    throw new AssertionError('No swearing!');
  }
}
```

## [2.3.0] - 2016-10-27
* When `spotlessCheck` fails, the error message now contains a short diff of what is neccessary to fix the issue ([#10](https://github.com/diffplug/spotless/issues/10), thanks to Jonathan Bluett-Duncan).
* Added a [padded-cell mode](https://github.com/diffplug/spotless/blob/main/PADDEDCELL.md) which allows spotless to band-aid over misbehaving rules, and generate error reports for these rules (See [#37](https://github.com/diffplug/spotless/issues/37) for an example).
* Character encoding is now configurable (spotless-global or format-by-format).
* Line endings were previously only spotless-global, they now also support format-by-format.
* Upgraded eclipse formatter from 4.6.0 to 4.6.1

## [2.2.0] - 2016-10-07
* Added support for [google-java-format](https://github.com/google/google-java-format).

```
spotless {
  java {
    googleJavaFormat()  // googleJavaFormat('1.1') to specify a specific version
  }
}
```

## [2.1.0] - 2016-10-07
* Added the method `FormatExtension::customLazyGroovy` which fixes the Groovy closure problem.

## [2.0.0] - 2016-08-16
* `spotlessApply` now writes out a file only if it needs to be changed (big performance improvement).
* Java import sorting now removes duplicate imports.
* Eclipse formatter now warns if the formatter xml contains multiple profiles.
* Updated eclipse formatter to Eclipse Neon (4.6).
* BREAKING CHANGE: Eclipse formatter now formats javadoc comments.
  + You might want to look at the following settings in your `spotless.eclipseformat.xml`:

```
org.eclipse.jdt.core.formatter.join_lines_in_comments=true/false
org.eclipse.jdt.core.formatter.comment.format_javadoc_comments=true/false
org.eclipse.jdt.core.formatter.comment.format_line_comments=true/false
org.eclipse.jdt.core.formatter.comment.format_block_comments=true/false
```

The most important breaking change of 2.0 is the new default line ending mode, `GIT_ATTRIBUTES`.  This line ending mode copies git's behavior exactly.  This change should require no intervention from users, and should be significantly easier to adopt for users who are already using `.gitattributes` or the `core.eol` property.

If you aren't using git, you can still use `.gitattributes` files for fine-grained control of line endings.  If no git information is found, it behaves the same as PLATFORM_NATIVE (the old default).

Below is the algorithm used by git and spotless to determine the proper line ending for a file.  As soon as a step succeeds in finding a line ending, the other steps will not take place.

1. If the code is a git repository, look in the `$GIT_DIR/info/attributes` file for the `eol` attribute.
2. Look at the `.gitattributes` in the file's directory, going up the directory tree.
3. Look at the global `.gitattributes` file, if any.
4. Look at the `core.eol` property in the git config (looking first at repo-specific, then user-specific, then system-specific).
5. Use the PLATFORM_NATIVE line ending.

## [1.3.3] - 2016-03-10
* Upgraded Eclipse formatter to 4.5.2, which fixes [37 bugs](https://bugs.eclipse.org/bugs/buglist.cgi?query_format=advanced&resolution=FIXED&short_desc=%5Bformatter%5D&short_desc_type=allwordssubstr&target_milestone=4.5.1&target_milestone=4.5.2) compared to the previous 4.5.0.
* If you have been using `custom 'Lambda fix', { it.replace('} )', '})').replace('} ,', '},') }`, you don't need it anymore.

## [1.3.2] - 2015-12-17
* Spotless no longer clobbers package-info.java, fixes [#1](https://github.com/diffplug/spotless/issues/1).
* Added some infrastructure which allows `FormatterStep`s to peek at the filename if they really need to.

## [1.3.1] - 2015-09-22
* Bumped the FreshMark dependency to 1.3.0, because it offers improved error reporting.

## [1.3.0] - 2015-09-22
* Added native support for FreshMark.

## [1.2.0] - 2015-08-25
* Updated from Eclipse 4.5 M6 to the official Eclipse 4.5 release, which fixes several bugs in the formatter.
* Fixed a bug in the import sorter which made it impossible to deal with "all unmatched type imports".
* Formatting is now relative to the project directory rather than the root directory.
* Improved the logging levels.

## [1.1] - 2015-05-14
* No functional changes, probably not worth the time for an upgrade.
* First version which is available on plugins.gradle.org as well as jcenter.
* Removed some code that was copy-pasted from Durian, and added a Durian dependency.

## [1.0] - 2015-04-29
* Initial release.

## [0.1] - 2015-04-28
* First release, to test out that we can release to jcenter and whatnot.
