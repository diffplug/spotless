# spotless-plugin-maven releases

### Version 1.22.0-SNAPSHOT - TBD ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-maven-plugin/snapshot/), [snapshot](https://oss.sonatype.org/content/repositories/snapshots/com/diffplug/spotless/spotless-maven-plugin/))

* Updated default eclipse-cdt from 4.7.3a to 4.11.0 ([#382](https://github.com/diffplug/spotless/pull/382)).

### Version 1.21.1 - March 29th 2019 ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-maven-plugin/1.21.1/), [jcenter](https://bintray.com/diffplug/opensource/spotless-maven-plugin/1.21.1))

* Fixes incorrect plugin and pom metadata in `1.21.0` ([#388](https://github.com/diffplug/spotless/issues/388)).

### Version 1.21.0 - March 28th 2019 ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-maven-plugin/1.21.0/), [jcenter](https://bintray.com/diffplug/opensource/spotless-maven-plugin/1.21.0))

* Updated default eclipse-wtp from 4.7.3b to 4.8.0 ([#382](https://github.com/diffplug/spotless/pull/382)).
* Updated default eclipse-groovy from 4.8.1 to 4.10.0 ([#382](https://github.com/diffplug/spotless/pull/382)).
* Updated default eclipse-jdt from 4.10.0 to 4.11.0 ([#384](https://github.com/diffplug/spotless/pull/384)).

### Version 1.20.0 - March 14th 2019 ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-maven-plugin/1.20.0/), [jcenter](https://bintray.com/diffplug/opensource/spotless-maven-plugin/1.20.0))

* Updated default eclipse-wtp from 4.7.3a to 4.7.3b ([#371](https://github.com/diffplug/spotless/pull/371)).
* Default behavior of XML formatter changed to ignore  external URIs ([#369](https://github.com/diffplug/spotless/issues/369)).
  * **WARNING RESOLVED: By default, xml formatter no longer downloads external entities. You can opt-in to resolve external entities by setting resolveExternalURI to true. However, if you do opt-in, be sure that all external entities are referenced over https and not http, or you may be vulnerable to XXE attacks.**

### Version 1.19.0 - March 11th 2019 ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-maven-plugin/1.19.0/), [jcenter](https://bintray.com/diffplug/opensource/spotless-maven-plugin/1.19.0))

**WARNING: xml formatter in this version may be vulnerable to XXE attacks, fixed in 1.20.0 (see [#358](https://github.com/diffplug/spotless/issues/358)).**

* Security fix: Updated groovy, c/c++, and eclipse WTP formatters so that they download their source jars securely using `https` rather than `http` ([#360](https://github.com/diffplug/spotless/issues/360)).
* Updated default eclipse-jdt from 4.9.0 to 4.10.0 ([#368](https://github.com/diffplug/spotless/pull/368))
* Add a skip parameter to apply mojo to enable to bypass it if desired. ([#367](https://github.com/diffplug/spotless/pull/367)).

### Version 1.18.0 - February 11th 2019 ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-maven-plugin/1.18.0/), [jcenter](https://bintray.com/diffplug/opensource/spotless-maven-plugin/1.18.0))

**WARNING: xml formatter in this version may be vulnerable to XXE attacks, fixed in 1.20.0 (see [#358](https://github.com/diffplug/spotless/issues/358)).**

* Provided eclipse-wtp formatters as part of custom source format element. ([#325](https://github.com/diffplug/spotless/pull/325)). This change obsoletes the CSS and XML source elements.
* Updated default google-java-format from 1.5 to 1.7 ([#335](https://github.com/diffplug/spotless/issues/335)).
* `<importOrder><file>somefile</file></importOrder>` is now lazy ([#218](https://github.com/diffplug/spotless/issues/218)).

### Version 1.17.0 - December 13th 2018 ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-maven-plugin/1.17.0/), [jcenter](https://bintray.com/diffplug/opensource/spotless-maven-plugin/1.17.0))

**WARNING: xml formatter in this version may be vulnerable to XXE attacks, fixed in 1.20.0 (see [#358](https://github.com/diffplug/spotless/issues/358)).**

* Updated default eclipse-jdt from 4.7.3a to 4.9.0 ([#316](https://github.com/diffplug/spotless/pull/316)). New version addresses enum-tab formatting bug in 4.8 ([#314](https://github.com/diffplug/spotless/issues/314)).

### Version 1.16.0 - October 30th 2018 ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-maven-plugin/1.16.0/), [jcenter](https://bintray.com/diffplug/opensource/spotless-maven-plugin/1.16.0))

**WARNING: xml formatter in this version may be vulnerable to XXE attacks, fixed in 1.20.0 (see [#358](https://github.com/diffplug/spotless/issues/358)).**

* Added support for Eclipse's CSS formatter from WTP ([#311](https://github.com/diffplug/spotless/pull/311)).

### Version 1.15.0 - September 23rd 2018 ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-maven-plugin/1.15.0/), [jcenter](https://bintray.com/diffplug/opensource/spotless-maven-plugin/1.15.0))

**WARNING: xml formatter in this version may be vulnerable to XXE attacks, fixed in 1.20.0 (see [#358](https://github.com/diffplug/spotless/issues/358)).**

* Added `xml` support ([#140](https://github.com/diffplug/spotless/issues/140)) using formatter of Eclipse WTP 3.9.5 ([#241](https://github.com/diffplug/spotless/pull/241)).
* Added C/C++ support using formatter of Eclipse CDT 9.4.3 ([#232](https://github.com/diffplug/spotless/issues/232)).
* Skip `package-info.java` and `module-info.java` files from license header formatting. ([#273](https://github.com/diffplug/spotless/pull/273))
* Updated JSR305 annotation from 3.0.0 to 3.0.2 ([#274](https://github.com/diffplug/spotless/pull/274))
* Migrated from FindBugs annotations 3.0.0 to SpotBugs annotations 3.1.6 ([#274](https://github.com/diffplug/spotless/pull/274))
* Fix Maven version prerequisite in the generated POM ([#289](https://github.com/diffplug/spotless/pull/289))

### Version 1.14.0 - July 24th 2018 ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-maven-plugin/1.14.0/), [jcenter](https://bintray.com/diffplug/opensource/spotless-maven-plugin/1.14.0))

* Updated default eclipse-jdt from 4.7.2 to 4.7.3a ([#263](https://github.com/diffplug/spotless/issues/263)). New version fixes a bug preventing Java code formatting within JavaDoc comments ([#191](https://github.com/diffplug/spotless/issues/191)).
* Updated default groovy-eclipse from 4.6.3 to 4.8.0 ([#244](https://github.com/diffplug/spotless/pull/244)). New version allows to ignore internal formatter errors/warnings.
* Require 3.1.0+ version of Maven. ([#259](https://github.com/diffplug/spotless/pull/259))
* Fixed integration with latest versions of scalafmt. ([#260](https://github.com/diffplug/spotless/pull/260))

### Version 1.13.0 - June 1st 2018 ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-maven-plugin/1.13.0/), [jcenter](https://bintray.com/diffplug/opensource/spotless-maven-plugin/1.13.0))

* Fixed a bug in configuration file resolution on Windows when file is denoted by a URL. ([#254](https://github.com/diffplug/spotless/pull/254))

### Version 1.0.0.BETA5 - May 14th 2018 ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-maven-plugin/1.0.0.BETA5/), [jcenter](https://bintray.com/diffplug/opensource/spotless-maven-plugin/1.0.0.BETA5))

* Fixed a bug in `LicenseHeaderStep` which caused an exception with some malformed date-aware licenses. ([#222](https://github.com/diffplug/spotless/pull/222))
* Added support for Kotlin and Ktlint in Maven plugin ([#223](https://github.com/diffplug/spotless/pull/223)).
* Updated default ktlint from 0.14.0 to 0.21.0
* Added support for multiple generic formatters in Maven plugin ([#242](https://github.com/diffplug/spotless/pull/242)).

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
