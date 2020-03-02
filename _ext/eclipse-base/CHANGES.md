# spotless-eclipse-base

### Version 3.2.2 - TBD ([artifact]([jcenter](https://bintray.com/diffplug/opensource/spotless-eclipse-base)))

* Added support of plugin extensions without activator ([#533](https://github.com/diffplug/spotless/issues/533)). Required since `org.eclipse.core.filesystem` version `1.7.600` (see
[Bug 550548](https://bugs.eclipse.org/bugs/show_bug.cgi?id=550548))
* Updated configuration via single interface implementation. Functional based configuration still supported.

### Version 3.2.1 - September 3rd 2019 ([artifact]([jcenter](https://bintray.com/diffplug/opensource/spotless-eclipse-base)))

* Fixed deletion of temporary workspace. ([#447](https://github.com/diffplug/spotless/issues/447))

### Version 3.2.0 - June 30th 2019 ([artifact]([jcenter](https://bintray.com/diffplug/opensource/spotless-eclipse-base)))

* Added support of Eclipse 4.12 framework wiring. ([#413](https://github.com/diffplug/spotless/issues/413))

### Version 3.1.1 - June 4th 2019 ([artifact]([jcenter](https://bintray.com/diffplug/opensource/spotless-eclipse-base)))

* Fixed problem handling URL escaped characters in JAR file location. ([#401](https://github.com/diffplug/spotless/issues/401))

### Version 3.1.0 - February 10th 2019 ([artifact]([jcenter](https://bintray.com/diffplug/opensource/spotless-eclipse-base)))

* Added logging service based on SLF4J. ([#236](https://github.com/diffplug/spotless/issues/236))
* Updated internal interfaces to support `org.eclipse.osgi` version 3.13.
* Corrected documentation of version number usage in `gradle.properties`.

### Version 3.0.0 - July 18th 2018 ([artifact]([jcenter](https://bintray.com/diffplug/opensource/spotless-eclipse-base)))

* Initial release!
