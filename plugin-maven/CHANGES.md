# spotless-plugin-maven releases

### Version 1.10.0-SNAPSHOT - TBD ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-maven-plugin/snapshot/), [snapshot](https://oss.sonatype.org/content/repositories/snapshots/com/diffplug/spotless/spotless-maven-plugin/))

* Fixed a bug in `LicenseHeaderStep` which caused an exception with some malformed date-aware licenses. ([#222](https://github.com/diffplug/spotless/pull/222))
* Added support for Kotlin and Ktlint in Maven plugin ([#223](https://github.com/diffplug/spotless/pull/223)).
* Updated default ktlint from 0.14.0 to 0.21.0

### Version 1.0.0.BETA4 - February 27th 2018 ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-maven-plugin/1.0.0.BETA4/), [jcenter](https://bintray.com/diffplug/opensource/spotless-maven-plugin/1.0.0.BETA4))

* Fixed published POM to include dependency on plexus-resources ([#213](https://github.com/diffplug/spotless/pull/213)).

### Version 1.0.0.BETA3 - February 26th 2018 ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-maven-plugin/1.0.0.BETA3/), [jcenter](https://bintray.com/diffplug/opensource/spotless-maven-plugin/1.0.0.BETA3))

* Improved support for multi-module Maven projects ([#210](https://github.com/diffplug/spotless/pull/210)).
* Added generic format support for maven-plugin ([#209](https://github.com/diffplug/spotless/pull/209)).

### Version 1.0.0.BETA2 - February 15th 2018 ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-maven-plugin/1.0.0.BETA2/), [jcenter](https://bintray.com/diffplug/opensource/spotless-maven-plugin/1.0.0.BETA2))

* Fix build to ensure that published versions never have snapshot deps ([#205](https://github.com/diffplug/spotless/pull/205)).

### Version 1.0.0.BETA1 - February 11th 2018 ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-maven-plugin/1.0.0.BETA1/), [jcenter](https://bintray.com/diffplug/opensource/spotless-maven-plugin/1.0.0.BETA1))

* Maven plugin written by [Konstantin Lutovich](https://github.com/lutovich).
* Full support for the Java and Scala formatters.
* Initial release, after user feedback we will ship `1.x`.
