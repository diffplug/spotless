# spotless-lib and spotless-lib-extra releases

You might be looking for:

- [plugin-gradle/CHANGES.md](plugin-gradle/CHANGES.md)
- [plugin-maven/CHANGES.md](plugin-maven/CHANGES.md)

### Version 1.3.0-SNAPSHOT - TBD (javadoc [lib](https://diffplug.github.io/spotless/javadoc/spotless-lib/snapshot/) [lib-extra](https://diffplug.github.io/spotless/javadoc/spotless-lib-extra/snapshot/), [snapshot repo](https://oss.sonatype.org/content/repositories/snapshots/com/diffplug/spotless/))

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
