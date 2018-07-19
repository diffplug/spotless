# spotless-lib and spotless-lib-extra releases

You might be looking for:

- [plugin-gradle/CHANGES.md](plugin-gradle/CHANGES.md)
- [plugin-maven/CHANGES.md](plugin-maven/CHANGES.md)

### Version 1.14.0-SNAPSHOT - TBD (javadoc [lib](https://diffplug.github.io/spotless/javadoc/spotless-lib/snapshot/) [lib-extra](https://diffplug.github.io/spotless/javadoc/spotless-lib-extra/snapshot/), [snapshot repo](https://oss.sonatype.org/content/repositories/snapshots/com/diffplug/spotless/))

* Eclipse formatter versions decoupled from Spotless formatter step implementations to allow independent updates of M2 based Eclipse dependencies. ([#253](https://github.com/diffplug/spotless/pull/253))

### Version 1.13.0 - June 1st 2018 (javadoc [lib](https://diffplug.github.io/spotless/javadoc/spotless-lib/1.11.0/) [lib-extra](https://diffplug.github.io/spotless/javadoc/spotless-lib-extra/1.13.0/), artifact [lib]([jcenter](https://bintray.com/diffplug/opensource/spotless-lib), [lib-extra]([jcenter](https://bintray.com/diffplug/opensource/spotless-lib-extra)))

* Add line and column numbers to ktlint errors. ([#251](https://github.com/diffplug/spotless/pull/251))

### Version 1.12.0 - May 14th 2018 (javadoc [lib](https://diffplug.github.io/spotless/javadoc/spotless-lib/1.12.0/) [lib-extra](https://diffplug.github.io/spotless/javadoc/spotless-lib-extra/1.12.0/), artifact [lib]([jcenter](https://bintray.com/diffplug/opensource/spotless-lib), [lib-extra]([jcenter](https://bintray.com/diffplug/opensource/spotless-lib-extra)))

* Fixed a bug in `LicenseHeaderStep` which caused an exception with some malformed date-aware licenses. ([#222](https://github.com/diffplug/spotless/pull/222))
* Updated default ktlint from 0.14.0 to 0.21.0
* Add ability to pass custom options to ktlint in gradle plugin. See plugin-gradle/README for details.

### Version 1.11.0 - February 26th 2018 (javadoc [lib](https://diffplug.github.io/spotless/javadoc/spotless-lib/1.11.0/) [lib-extra](https://diffplug.github.io/spotless/javadoc/spotless-lib-extra/1.11.0/), artifact [lib]([jcenter](https://bintray.com/diffplug/opensource/spotless-lib), [lib-extra]([jcenter](https://bintray.com/diffplug/opensource/spotless-lib-extra)))

* Added default indentation of `4` to `IndentStep`. ([#209](https://github.com/diffplug/spotless/pull/209))

### Version 1.10.0 - February 15th 2018 (javadoc [lib](https://diffplug.github.io/spotless/javadoc/spotless-lib/1.10.0/) [lib-extra](https://diffplug.github.io/spotless/javadoc/spotless-lib-extra/1.10.0/), artifact [lib]([jcenter](https://bintray.com/diffplug/opensource/spotless-lib), [lib-extra]([jcenter](https://bintray.com/diffplug/opensource/spotless-lib-extra)))

* LicenseHeaderStep now supports customizing the year range separator in copyright notices. ([#199](https://github.com/diffplug/spotless/pull/199))
* Breaking change to testlib - removed `ResourceHarness.write` and added `ResourceHarness.[set/assert]File` for easier-to-read tests. ([#203](https://github.com/diffplug/spotless/pull/203))

### Version 1.9.0 - February 5th 2018 (javadoc [lib](https://diffplug.github.io/spotless/javadoc/spotless-lib/1.9.0/) [lib-extra](https://diffplug.github.io/spotless/javadoc/spotless-lib-extra/1.9.0/), artifact [lib]([jcenter](https://bintray.com/diffplug/opensource/spotless-lib), [lib-extra]([jcenter](https://bintray.com/diffplug/opensource/spotless-lib-extra)))

* Updated default ktlint from 0.6.1 to 0.14.0
* Updated default google-java-format from 1.3 to 1.5
* Updated default eclipse-jdt from 4.7.1 to 4.7.2
* Added a configuration option to `googleJavaFormat` to switch the formatter style ([#193](https://github.com/diffplug/spotless/pull/193))

### Version 1.8.0 - January 2nd 2018 (javadoc [lib](https://diffplug.github.io/spotless/javadoc/spotless-lib/1.8.0/) [lib-extra](https://diffplug.github.io/spotless/javadoc/spotless-lib-extra/1.8.0/), artifact [lib]([jcenter](https://bintray.com/diffplug/opensource/spotless-lib), [lib-extra]([jcenter](https://bintray.com/diffplug/opensource/spotless-lib-extra)))

* LicenseHeaderStep now supports time-aware copyright notices in license headers. ([#179](https://github.com/diffplug/spotless/pull/179), thanks to @baptistemesta)

### Version 1.7.0 - December 2nd 2017 (javadoc [lib](https://diffplug.github.io/spotless/javadoc/spotless-lib/1.7.0/) [lib-extra](https://diffplug.github.io/spotless/javadoc/spotless-lib-extra/1.7.0/), artifact [lib]([jcenter](https://bintray.com/diffplug/opensource/spotless-lib), [lib-extra]([jcenter](https://bintray.com/diffplug/opensource/spotless-lib-extra)))

* Updated default eclipse-jdt version to `4.7.1` from `4.6.3`.
* Updated jgit from `4.5.0.201609210915-r` to `4.9.0.201710071750-r`.
* Updated concurrent-trees from `2.6.0` to `2.6.1` (performance improvement).
* Added `dbeaverSql` formatter step, for formatting sql scripts. ([#166](https://github.com/diffplug/spotless/pull/166))
  + Many thanks to [Baptiste Mesta](https://github.com/baptistemesta) for porting to Spotless.
  + Many thanks to [DBeaver](https://dbeaver.jkiss.org/) and the [DBeaver contributors](https://github.com/serge-rider/dbeaver/graphs/contributors) for building the implementation.

### Version 1.6.0 - September 29th 2017 (javadoc [lib](https://diffplug.github.io/spotless/javadoc/spotless-lib/1.6.0/) [lib-extra](https://diffplug.github.io/spotless/javadoc/spotless-lib-extra/1.6.0/), artifact [lib]([jcenter](https://bintray.com/diffplug/opensource/spotless-lib), [lib-extra]([jcenter](https://bintray.com/diffplug/opensource/spotless-lib-extra)))

* Added `public static boolean PaddedCell::applyAnyChanged(Formatter formatter, File file)`.

### Version 1.5.1 - August 14th 2017 (javadoc [lib](https://diffplug.github.io/spotless/javadoc/spotless-lib/1.5.1/) [lib-extra](https://diffplug.github.io/spotless/javadoc/spotless-lib-extra/1.5.1/), artifact [lib]([jcenter](https://bintray.com/diffplug/opensource/spotless-lib), [lib-extra]([jcenter](https://bintray.com/diffplug/opensource/spotless-lib-extra)))

* Added `KtLintStep.createForScript`.

### Version 1.5.0 - August 13th 2017 (javadoc [lib](https://diffplug.github.io/spotless/javadoc/spotless-lib/1.5.0/) [lib-extra](https://diffplug.github.io/spotless/javadoc/spotless-lib-extra/1.5.0/), artifact [lib]([jcenter](https://bintray.com/diffplug/opensource/spotless-lib), [lib-extra]([jcenter](https://bintray.com/diffplug/opensource/spotless-lib-extra)))

* Deprecated `ImportOrderStep.createFromOrder(List<String>` in favor of `(String...`.

### Version 1.4.1 - July 11th 2017 (javadoc [lib](https://diffplug.github.io/spotless/javadoc/spotless-lib/1.4.1/) [lib-extra](https://diffplug.github.io/spotless/javadoc/spotless-lib-extra/1.4.1/), artifact [lib]([jcenter](https://bintray.com/diffplug/opensource/spotless-lib), [lib-extra]([jcenter](https://bintray.com/diffplug/opensource/spotless-lib-extra)))

* Default eclipse version for `EclipseFormatterStep` bumped to `4.6.3` from `4.6.1`. ([#116](https://github.com/diffplug/spotless/issues/116))
* Default scalafmt version for `ScalaFmtStep` bumped to `1.1.0` from `0.5.7` ([#124](https://github.com/diffplug/spotless/pull/124))
  + Also added support for the API change to scalafmt introduced in `0.7.0-RC1`

### Version 1.4.0 - May 21st 2017 (javadoc [lib](https://diffplug.github.io/spotless/javadoc/spotless-lib/1.4.0/) [lib-extra](https://diffplug.github.io/spotless/javadoc/spotless-lib-extra/1.4.0/), artifact [lib]([jcenter](https://bintray.com/diffplug/opensource/spotless-lib), [lib-extra]([jcenter](https://bintray.com/diffplug/opensource/spotless-lib-extra)))

* `ImportOrderStep` can now handle multi-line comments and misplaced imports.
  + Especially helpful for Groovy and Gradle files.

### Version 1.3.2 - May 3rd 2017 (javadoc [lib](https://diffplug.github.io/spotless/javadoc/spotless-lib/1.3.1/) [lib-extra](https://diffplug.github.io/spotless/javadoc/spotless-lib-extra/1.3.0/), artifact [lib]([jcenter](https://bintray.com/diffplug/opensource/spotless-lib), [lib-extra]([jcenter](https://bintray.com/diffplug/opensource/spotless-lib-extra)))

* Fixed a bug in `PaddedCellBulk.check()` which caused a `check` to fail even after an `apply` for cases which caused CYCLE.

### Version 1.3.0 - April 11th 2017 (javadoc [lib](https://diffplug.github.io/spotless/javadoc/spotless-lib/1.3.0/) [lib-extra](https://diffplug.github.io/spotless/javadoc/spotless-lib-extra/1.3.0/), artifact [lib]([jcenter](https://bintray.com/diffplug/opensource/spotless-lib), [lib-extra]([jcenter](https://bintray.com/diffplug/opensource/spotless-lib-extra)))

* Added support for Groovy via [greclipse](https://github.com/groovy/groovy-eclipse).
* When a JarState resolution failed, it threw a Gradle-specific error message. That message has been moved out of `lib` and into `plugin-gradle` where it belongs.

### Version 1.2.0 - April 3rd 2017 (javadoc [lib](https://diffplug.github.io/spotless/javadoc/spotless-lib/1.2.0/) [lib-extra](https://diffplug.github.io/spotless/javadoc/spotless-lib-extra/1.2.0/), artifact [lib]([jcenter](https://bintray.com/diffplug/opensource/spotless-lib), [lib-extra]([jcenter](https://bintray.com/diffplug/opensource/spotless-lib-extra)))

* Deprecated `FileSignature.from` in favor of `FileSignature.signAsSet` and the new `FileSignature.signAsList`.
* Added a `FormatterProperties` class which loads `.properties` files and eclipse-style `.xml` files.
* `SerializableFileFilter.skipFilesNamed` can now skip multiple file names.
* Update default KtLint from 0.3.1 to 0.6.1.
  + This means we no longer look for rules in the typo package `com.gihub.shyiko`, now only in `com.github.shyiko` (note the `t`).

### Version 1.1.0 - February 27th 2017 (javadoc [lib](https://diffplug.github.io/spotless/javadoc/spotless-lib/1.1.0/) [lib-extra](https://diffplug.github.io/spotless/javadoc/spotless-lib-extra/1.1.0/), artifact [lib]([jcenter](https://bintray.com/diffplug/opensource/spotless-lib), [lib-extra]([jcenter](https://bintray.com/diffplug/opensource/spotless-lib-extra)))

* Added support for Scala via [scalafmt](https://github.com/olafurpg/scalafmt).
* Added support for Kotlin via [ktlint](https://github.com/shyiko/ktlint).
* Better error messages for JarState.
* Improved test harnessing.
* Formatter now has pluggable exception policies,

### Version 1.0.0 - January 9th 2017 (javadoc [lib](https://diffplug.github.io/spotless/javadoc/spotless-lib/1.0.0/) [lib-extra](https://diffplug.github.io/spotless/javadoc/spotless-lib-extra/1.0.0/), artifact [lib]([jcenter](https://bintray.com/diffplug/opensource/spotless-lib), [lib-extra]([jcenter](https://bintray.com/diffplug/opensource/spotless-lib-extra)))

* Initial release!
