# Spotless: Make your Java spotless with Gradle

[![Release](http://img.shields.io/badge/master-1.0-lightgrey.svg)](https://github.com/diffplug/spotless/releases/latest)
[![Build Status](https://travis-ci.org/diffplug/durian.svg?branch=master)](https://travis-ci.org/diffplug/spotless)
[![Snapshot](http://img.shields.io/badge/develop-1.1--SNAPSHOT-lightgrey.svg)](https://github.com/diffplug/spotless/tree/develop)
[![Build Status](https://travis-ci.org/diffplug/durian.svg?branch=develop)](https://travis-ci.org/diffplug/spotless)
[![License](https://img.shields.io/badge/license-Apache-blue.svg)](https://tldrlegal.com/license/apache-license-2.0-(apache-2.0))

Spotless can check and apply formatting, including:
* license headers
* import ordering (using Eclipse's import ordering)
* formatting style (using Eclipse's code formatter)

To add Spotless to your project, you can create your formatting style and export it from the Eclipse gui.

To work with a project that already uses Spotless, you can import the formatting style into your Eclipse.

Even if you don't use Eclipse, Spotless makes it painless to find and correct formatting errors:

```
cmd> gradlew build
   ...
:spotlessCheckTest FAILED
> Format violations were found. Run 'gradlew spotlessApply' to fix them.
    src\test\java\com\github\youribonnaffe\gradle\format\ResourceTest.java

cmd> gradlew spotlessApply
:spotlessApply
BUILD SUCCESSFUL

cmd> gradlew build
BUILD SUCCESSFUL
```

## Adding spotless to your build

To add spotless to your build:

```groovy
buildscript {
	repositories {
		jcenter()
	}
	dependencies {
		classpath 'com.diffplug.gradle.spotless:spotless:1.0'
	}
}

apply plugin: 'com.diffplug.gradle.spotless'
```

Spotless doesn't require any configuration - it uses all of Eclipse's default settings out of the box.  They can be easily overwritten as shown:

```groovy
spotless {
	// If you don't specify a license, then spotless will leave everything above the package statement alone
	licenseHeader = '/** Licensed under Apache-2.0 */'	// License header
	licenseHeaderFile = file('spotless_header.java')	// License header file
	// Obviously, you can't specify both licenseHeader and licenseHeaderFile at the same time

	importsOrder = ['java', 'javax', 'org']			// This is the default order, but you can change it if you'd like
	importsOrderFile = file('spotless.importorder')	// An import ordering file, exported from Eclipse
	// As before, you can't specify both importsOrder and importsOrderFile at the same time

	eclipseFormatFile = file('spotless.xml')	// XML file dumped out by the Eclipse formatter
	// If you have an older Eclipse properties file, you can use that too.

	// If you'd like to specify that files should always have a certain line ending, you can,
	// but the default value of PLATFORM_NATIVE is highly recommended
	LineEnding lineEndings = PLATFORM_NATIVE 	// can be WINDOWS, UNIX, or PLATFORM_NATIVE
}
```

## Exporting / importing from Eclipse

There are two files to import / export with Eclipse - the import ordering file and the code formatting file.

## Acknowledgements

* Formatting by Eclipse 4.5 M6
    + Special thanks to [Mateusz Matela](https://waynebeaton.wordpress.com/2015/03/15/great-fixes-for-mars-winners-part-i/) for huge improvements to the eclipse code formatter!
* Forked from [gradle-license-plugin](https://github.com/youribonnaffe/gradle-format-plugin) by Youri Bonnaffé.
* Import ordering from [EclipseCodeFormatter](https://github.com/krasa/EclipseCodeFormatter).
* Formatted by [spotless](https://github.com/diffplug/spotless).
* Built by [gradle](http://gradle.org/).
* Tested by [junit](http://junit.org/).
* Artifacts hosted by [jcenter](https://bintray.com/bintray/jcenter) and uploaded by [gradle-bintray-plugin](https://github.com/bintray/gradle-bintray-plugin).
