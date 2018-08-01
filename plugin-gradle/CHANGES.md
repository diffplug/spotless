# spotless-plugin-gradle releases

### Version 3.15.0-SNAPSHOT - TBD ([javadoc](https://diffplug.github.io/spotless/javadoc/snapshot/), [snapshot](https://oss.sonatype.org/content/repositories/snapshots/com/diffplug/spotless/spotless-plugin-gradle/))

* LicenseHeaderStep now wont attempt to add license to `module-info.java` ([#272](https://github.com/diffplug/spotless/pull/272)).

### Version 3.14.0 - July 24th 2018 ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-plugin-gradle/3.14.0/), [jcenter](https://bintray.com/diffplug/opensource/spotless-plugin-gradle/3.14.0))

* Updated default eclipse-jdt from 4.7.2 to 4.7.3a ([#263](https://github.com/diffplug/spotless/issues/263)). New version fixes a bug preventing Java code formatting within JavaDoc comments ([#191](https://github.com/diffplug/spotless/issues/191)).
* Updated default groovy-eclipse from 4.6.3 to 4.8.0 ([#244](https://github.com/diffplug/spotless/pull/244)). New version allows to ignore internal formatter errors/warnings.
* Fixed integration with latest versions of scalafmt. ([#260](https://github.com/diffplug/spotless/pull/260))

### Version 3.13.0 - June 1st 2018 ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-plugin-gradle/3.13.0/), [jcenter](https://bintray.com/diffplug/opensource/spotless-plugin-gradle/3.13.0))

* Add line and column numbers to ktlint errors. ([#251](https://github.com/diffplug/spotless/pull/251))

### Version 3.12.0 - May 14th 2018 ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-plugin-gradle/3.12.0/), [jcenter](https://bintray.com/diffplug/opensource/spotless-plugin-gradle/3.12.0))

* Migrated `plugin-gradle`'s tests away from `TaskInternal#execute` to a custom method to help with Gradle 5.0 migration later on. ([#208](https://github.com/diffplug/spotless/pull/208))
* Fixed a bug in `LicenseHeaderStep` which caused an exception with some malformed date-aware licenses. ([#222](https://github.com/diffplug/spotless/pull/222))
* Updated default ktlint from 0.14.0 to 0.21.0
* Add ability to pass custom options to ktlint. See README for details.
* Added interface `HasBuiltinDelimiterForLicense` to language extensions that have pre-defined licence header delimiter. ([#235](https://github.com/diffplug/spotless/pull/235))

### Version 3.10.0 - February 15th 2018 ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-plugin-gradle/3.10.0/), [jcenter](https://bintray.com/diffplug/opensource/spotless-plugin-gradle/3.10.0))

* LicenseHeaderStep now supports customizing the year range separator in copyright notices. ([#199](https://github.com/diffplug/spotless/pull/199))

### Version 3.9.0 - February 5th 2018 ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-plugin-gradle/3.9.0/), [jcenter](https://bintray.com/diffplug/opensource/spotless-plugin-gradle/3.9.0))

* Updated default ktlint from 0.6.1 to 0.14.0
* Updated default google-java-format from 1.3 to 1.5
* Updated default eclipse-jdt from 4.7.1 to 4.7.2
* Added a configuration option to `googleJavaFormat` to switch the formatter style ([#193](https://github.com/diffplug/spotless/pull/193))
  + Use `googleJavaFormat().aosp()` to use AOSP-compliant style (4-space indentation) instead of the default Google Style

### Version 3.8.0 - January 2nd 2018 ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-plugin-gradle/3.8.0/), [jcenter](https://bintray.com/diffplug/opensource/spotless-plugin-gradle/3.8.0))

* Bugfix: if the specified target of a spotless task was reduced, Spotless could keep giving warnings until the cache file was deleted.
* LicenseHeader now supports time-aware license headers. ([docs](https://github.com/diffplug/spotless/tree/master/plugin-gradle#license-header), [#179](https://github.com/diffplug/spotless/pull/179), thanks to @baptistemesta)

### Version 3.7.0 - December 2nd 2017 ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-plugin-gradle/3.7.0/), [jcenter](https://bintray.com/diffplug/opensource/spotless-plugin-gradle/3.7.0))

* Updated default eclipse-jdt version to `4.7.1` from `4.6.3`.
* All spotless tasks now run before the `clean` task. ([#159](https://github.com/diffplug/spotless/issues/159))
* Added `sql` ([#166](https://github.com/diffplug/spotless/pull/166)) and `dbeaverSql`. ([#166](https://github.com/diffplug/spotless/pull/166))
  + Many thanks to [Baptiste Mesta](https://github.com/baptistemesta) for porting to Spotless.
  + Many thanks to [DBeaver](https://dbeaver.jkiss.org/) and the [DBeaver contributors](https://github.com/serge-rider/dbeaver/graphs/contributors) for building the implementation.

### Version 3.6.0 - September 29th 2017 ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-plugin-gradle/3.6.0/), [jcenter](https://bintray.com/diffplug/opensource/spotless-plugin-gradle/3.6.0))

* Fixes a rare up-to-date bug. ([#144](https://github.com/diffplug/spotless/issues/144) and [#146](https://github.com/diffplug/spotless/pull/146))

### Version 3.5.2 - September 5th 2017 ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-plugin-gradle/3.5.2/), [jcenter](https://bintray.com/diffplug/opensource/spotless-plugin-gradle/3.5.2))

* Fix licenseHeader so it works with Kotlin files starting with `@file:...` instead of `package ...` ([#136](https://github.com/diffplug/spotless/issues/136)).

### Version 3.5.1 - August 14th 2017 ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-plugin-gradle/3.5.1/), [jcenter](https://bintray.com/diffplug/opensource/spotless-plugin-gradle/3.5.1))

* Fixed `kotlinGradle` linting [Gradle Kotlin DSL](https://github.com/gradle/kotlin-dsl) files throwing `ParseException` ([#132](https://github.com/diffplug/spotless/issues/132)).

### Version 3.5.0 - August 13th 2017 ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-plugin-gradle/3.5.0/), [jcenter](https://bintray.com/diffplug/opensource/spotless-plugin-gradle/3.5.0))

* Changed `importOrder` interface from array to varargs ([#125](https://github.com/diffplug/spotless/issues/125)).
* The `kotlin` extension was mis-spelled as `kotin`.
* Added `kotlinGradle` method to `SpotlessExtension` for linting [Gradle Kotlin DSL](https://github.com/gradle/kotlin-dsl) files with [ktlint](https://github.com/shyiko/ktlint) ([#115](https://github.com/diffplug/spotless/issues/115))
* Added dedicated `groovyGradle` for formatting of Gradle files.

### Version 3.4.1 - July 11th 2017 ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-plugin-gradle/3.4.1/), [jcenter](https://bintray.com/diffplug/opensource/spotless-plugin-gradle/3.4.1))

* Default eclipse version for `EclipseFormatterStep` bumped to `4.6.3` from `4.6.1`. ([#116](https://github.com/diffplug/spotless/issues/116))
* Default scalafmt version for `ScalaFmtStep` bumped to `1.1.0` from `0.5.7` ([#124](https://github.com/diffplug/spotless/pull/124))
  + Also added support for the API change to scalafmt introduced in `0.7.0-RC1`
* Fixed wildcard targets for `includeFlat` subprojects ([#121](https://github.com/diffplug/spotless/issues/121))
* When spotless needs to download a formatter, it now uses the buildscript repositories specified in the root buildscript. ([#123](https://github.com/diffplug/spotless/pull/123), [#120](https://github.com/diffplug/spotless/issues/120))

### Version 3.4.0 - May 21st 2017 ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-plugin-gradle/3.4.0/), [jcenter](https://bintray.com/diffplug/opensource/spotless-plugin-gradle/3.4.0))

* `ImportOrderStep` can now handle multi-line comments and misplaced imports.
* Groovy extension now checks for the `groovy` plugin to be applied.
* Deprecated the old syntax for the the eclipse formatter:
  + New syntax better separates the version from the other configuration options, and is more consistent with the other
  + `eclipseFormatFile('format.xml')` -> `eclipse().configFile('format.xml')`
  + `eclipseFormatFile('4.4.0', 'format.xml')` -> `eclipse('4.4.0').configFile('format.xml')`

### Version 3.3.2 - May 3rd 2017 ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-plugin-gradle/3.3.1/), [jcenter](https://bintray.com/diffplug/opensource/spotless-plugin-gradle/3.3.1))

* Fixed a bug in `paddedCell()` which caused `spotlessCheck` to fail even after `spotlessApply` for cases where a rule is misbehaving and causing a cycle.

### Version 3.3.0 - April 11th 2017 ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-plugin-gradle/3.3.0/), [jcenter](https://bintray.com/diffplug/opensource/spotless-plugin-gradle/3.3.0))

* Added support for groovy formatting (huge thanks to Frank Vennemeyer! [#94](https://github.com/diffplug/spotless/pull/94), [#89](https://github.com/diffplug/spotless/pull/89), [#88](https://github.com/diffplug/spotless/pull/88), [#85](https://github.com/diffplug/spotless/pull/85))
* When special-purpose formatters need to be downloaded from maven, they are now resolved using the buildscript repositories rather than the project repositories. (thanks to [cal101](https://github.com/cal101) [#100](https://github.com/diffplug/spotless/pull/100))

### Version 3.2.0 - April 3rd 2017 ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-plugin-gradle/3.2.0/), [jcenter](https://bintray.com/diffplug/opensource/spotless-plugin-gradle/3.2.0))

* Update default KtLint from 0.3.1 to 0.6.1 (thanks to @kvnxiao [#93](https://github.com/diffplug/spotless/pull/93)).
  + This means we no longer look for rules in the typo package `com.gihub.shyiko`, now only in `com.github.shyiko` (note the `t`).
* Added an `enforceCheck` property which allows users to disable adding `spotlessCheck` as a dependency of `check` (thanks to @gdecaso [#95](https://github.com/diffplug/spotless/pull/95)).
* Any errors in a step will now fail the build - previously they were only warned.
  + We claimed that we implemented this in 3.1.0, but it was broken.  We really fixed it this time.

### Version 3.1.0 - February 27th 2017 ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-plugin-gradle/3.1.0/), [jcenter](https://bintray.com/diffplug/opensource/spotless-plugin-gradle/3.1.0))

* Added support for Scala via [scalafmt](https://github.com/olafurpg/scalafmt).
* Added support for Kotlin via [ktlint](https://github.com/shyiko/ktlint).
* Added `FormatExtension::replaceStep`.
* `paddedCell()` is no longer required if a misbehaving rule converges.
* Any errors in a step will now fail the build - previously they were only warned.
* Added `FormatExtension::ignoreErrorForStep` and `FormatExtension::ignoreErrorForPath`.
* Bumped `google-java-format` to `1.3`.

### Version 3.0.0 - January 9th 2017 ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-plugin-gradle/3.0.0/), [jcenter](https://bintray.com/diffplug/opensource/spotless-plugin-gradle/3.0.0))

* BREAKING CHANGE: `customReplace` and `customReplaceRegex` renamed to just `replace` and `replaceRegex`.
* BREAKING CHANGE: Plugin portal ID is still `com.diffplug.gradle.spotless`, but maven coordinate has changed to `com.diffplug.spotless:spotless-plugin-gradle`.
* HUGE SPEEDUP: Now supports incremental build / up-to-date-checking.
  + If you are using `custom` or `customLazy`, you might want to take a look at [this javadoc](https://diffplug.github.io/spotless/javadoc/snapshot/spotless-gradle-plugin/snapshot/com/diffplug/gradle/spotless/FormatExtension.html#bumpThisNumberIfACustomStepChanges-int-).
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

### Version 2.4.1 - January 2nd 2017 ([javadoc](https://diffplug.github.io/spotless/javadoc/2.4.1/), [jcenter](https://bintray.com/diffplug/opensource/spotless/2.4.1/view))

* Java files under the `src/main/groovy` folder are now formatted by default. ([Issue #59](https://github.com/diffplug/spotless/issues/59), [PR #60](https://github.com/diffplug/spotless/pull/60), thanks @ajoberstar).

### Version 2.4.0 - November 1st 2016 ([javadoc](https://diffplug.github.io/spotless/javadoc/2.4.0/), [jcenter](https://bintray.com/diffplug/opensource/spotless/2.4.0/view))

* If a formatter step throws an `Error` or any of its subclasses, such as the `AssertionError`s thrown by JUnit, AssertJ, etc. that error will kill the build ([#46](https://github.com/diffplug/spotless/issues/46))
  + This allows custom rules like this:

```gradle
custom 'no swearing', {
  if (it.toLowerCase().contains('fubar')) {
    throw new AssertionError('No swearing!');
  }
}
```

### Version 2.3.0 - October 27th 2016 ([javadoc](https://diffplug.github.io/spotless/javadoc/2.3.0/), [jcenter](https://bintray.com/diffplug/opensource/spotless/2.3.0/view))

* When `spotlessCheck` fails, the error message now contains a short diff of what is neccessary to fix the issue ([#10](https://github.com/diffplug/spotless/issues/10), thanks to Jonathan Bluett-Duncan).
* Added a [padded-cell mode](https://github.com/diffplug/spotless/blob/master/PADDEDCELL.md) which allows spotless to band-aid over misbehaving rules, and generate error reports for these rules (See [#37](https://github.com/diffplug/spotless/issues/37) for an example).
* Character encoding is now configurable (spotless-global or format-by-format).
* Line endings were previously only spotless-global, they now also support format-by-format.
* Upgraded eclipse formatter from 4.6.0 to 4.6.1

### Version 2.2.0 - October 7th 2016 ([javadoc](https://diffplug.github.io/spotless/javadoc/2.2.0/), [jcenter](https://bintray.com/diffplug/opensource/spotless/2.2.0/view))

* Added support for [google-java-format](https://github.com/google/google-java-format).

```
spotless {
  java {
    googleJavaFormat()  // googleJavaFormat('1.1') to specify a specific version
  }
}
```

### Version 2.1.0 - October 7th 2016 ([javadoc](https://diffplug.github.io/spotless/javadoc/2.1.0/), [jcenter](https://bintray.com/diffplug/opensource/spotless/2.1.0/view))

* Added the method `FormatExtension::customLazyGroovy` which fixes the Groovy closure problem.

### Version 2.0.0 - August 16th 2016 ([javadoc](https://diffplug.github.io/spotless/javadoc/2.0.0/), [jcenter](https://bintray.com/diffplug/opensource/spotless/2.0.0/view))

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

### Version 1.3.3 - March 10th 2016 ([jcenter](https://bintray.com/diffplug/opensource/spotless/1.3.3/view))

* Upgraded Eclipse formatter to 4.5.2, which fixes [37 bugs](https://bugs.eclipse.org/bugs/buglist.cgi?query_format=advanced&resolution=FIXED&short_desc=%5Bformatter%5D&short_desc_type=allwordssubstr&target_milestone=4.5.1&target_milestone=4.5.2) compared to the previous 4.5.0.
* If you have been using `custom 'Lambda fix', { it.replace('} )', '})').replace('} ,', '},') }`, you don't need it anymore.

### Version 1.3.2 - December 17th 2015 ([jcenter](https://bintray.com/diffplug/opensource/spotless/1.3.2/view))

* Spotless no longer clobbers package-info.java, fixes [#1](https://github.com/diffplug/spotless/issues/1).
* Added some infrastructure which allows `FormatterStep`s to peek at the filename if they really need to.

### Version 1.3.1 - September 22nd 2015 ([jcenter](https://bintray.com/diffplug/opensource/spotless/1.3.1/view))

* Bumped the FreshMark dependency to 1.3.0, because it offers improved error reporting.

### Version 1.3.0 - September 22nd 2015 ([jcenter](https://bintray.com/diffplug/opensource/spotless/1.3.0/view))

* Added native support for FreshMark.

### Version 1.2.0 - August 25th 2015 ([jcenter](https://bintray.com/diffplug/opensource/spotless/1.2.0/view))

* Updated from Eclipse 4.5 M6 to the official Eclipse 4.5 release, which fixes several bugs in the formatter.
* Fixed a bug in the import sorter which made it impossible to deal with "all unmatched type imports".
* Formatting is now relative to the project directory rather than the root directory.
* Improved the logging levels.

### Version 1.1 - May 14th 2015 ([jcenter](https://bintray.com/diffplug/opensource/spotless/1.1/view))

* No functional changes, probably not worth the time for an upgrade.
* First version which is available on plugins.gradle.org as well as jcenter.
* Removed some code that was copy-pasted from Durian, and added a Durian dependency.

### Version 1.0 - April 29th 2015 ([jcenter](https://bintray.com/diffplug/opensource/spotless/1.0/view))

* Initial release.

### Version 0.1 - April 28th 2015 ([jcenter](https://bintray.com/diffplug/opensource/spotless/0.1/view))

* First release, to test out that we can release to jcenter and whatnot.
