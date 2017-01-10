# <img align="left" src="../_images/spotless_logo.png"> Spotless: Keep your code spotless with Gradle

<!---freshmark shields
output = [
	link(shield('Gradle plugin', 'plugins.gradle.org', 'com.diffplug.gradle.spotless', 'blue'), 'https://plugins.gradle.org/plugin/com.diffplug.gradle.spotless'),
	link(shield('Maven central', 'mavencentral', 'com.diffplug.gradle.spotless:spotless', 'blue'), 'http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.diffplug.spotless%22%20AND%20a%3A%22spotless-plugin-gradle%22'),
	link(shield('Javadoc', 'javadoc', '{{stableGradle}}', 'blue'), 'https://{{org}}.github.io/{{name}}/javadoc/spotless-plugin-gradle/{{stableGradle}}/'),
	'',
	link(shield('Changelog', 'changelog', '{{versionGradle}}', 'brightgreen'), 'CHANGES.md'),
	link(image('Travis CI', 'https://travis-ci.org/{{org}}/{{name}}.svg?branch=master'), 'https://travis-ci.org/{{org}}/{{name}}'),
	link(shield('Live chat', 'gitter', 'chat', 'brightgreen'), 'https://gitter.im/{{org}}/{{name}}'),
	link(shield('License Apache', 'license', 'apache', 'brightgreen'), 'https://tldrlegal.com/license/apache-license-2.0-(apache-2.0)')
	].join('\n');
-->
[![Gradle plugin](https://img.shields.io/badge/plugins.gradle.org-com.diffplug.gradle.spotless-blue.svg)](https://plugins.gradle.org/plugin/com.diffplug.gradle.spotless)
[![Maven central](https://img.shields.io/badge/mavencentral-com.diffplug.gradle.spotless%3Aspotless-blue.svg)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.diffplug.spotless%22%20AND%20a%3A%22spotless-plugin-gradle%22)
[![Javadoc](https://img.shields.io/badge/javadoc-3.0.0.RC2-blue.svg)](https://diffplug.github.io/spotless/javadoc/spotless-plugin-gradle/3.0.0.RC2/)

[![Changelog](https://img.shields.io/badge/changelog-3.0.0--SNAPSHOT-brightgreen.svg)](CHANGES.md)
[![Travis CI](https://travis-ci.org/diffplug/spotless.svg?branch=master)](https://travis-ci.org/diffplug/spotless)
[![Live chat](https://img.shields.io/badge/gitter-chat-brightgreen.svg)](https://gitter.im/diffplug/spotless)
[![License Apache](https://img.shields.io/badge/license-apache-brightgreen.svg)](https://tldrlegal.com/license/apache-license-2.0-(apache-2.0))
<!---freshmark /shields -->

<!---freshmark javadoc
output = prefixDelimiterReplace(input, 'https://{{org}}.github.io/{{name}}/javadoc/spotless-plugin-gradle/', '/', stableGradle)
-->

Spotless is a general-purpose formatting plugin.  It is completely à la carte, but also includes powerful "batteries-included" if you opt-in.

To people who use your build, it looks like this:

```
cmd> gradlew build
...
:spotlessJavaCheck FAILED
> The following files had format violations:
	src\main\java\com\diffplug\gradle\spotless\FormatExtension.java
		@@ -109,7 +109,7 @@
		...
		-\t\t····if·(targets.length·==·0)·{
		+\t\tif·(targets.length·==·0)·{
		...
	Run 'gradlew spotlessApply' to fix these violations.

cmd> gradlew spotlessApply
:spotlessApply
BUILD SUCCESSFUL

cmd> gradlew build
BUILD SUCCESSFUL
```

Inside your buildscript, it looks like this:

```gradle
spotless {
	format 'misc', {
		target '**/*.gradle', '**/*.md', '**/.gitignore'

		trimTrailingWhitespace()
		indentWithTabs() // or spaces. Takes an integer argument if you don't like 4
		endWithNewline()
	}
	format 'cpp', {
		target '**/*.hpp', '**/*.cpp'

		replace      'Not enough space after if', 'if(', 'if ('
		replaceRegex 'Too much space after if', 'if +\\(', 'if ('

		// Everything before the first #include or #pragma will
		// be replaced with whatever is in `spotless.license.cpp`
		licenseHeaderFile 'spotless.license.cpp', '#'
	}
}
```

Spotless can check and apply formatting to any plain-text file, using simple rules ([javadoc](https://diffplug.github.io/spotless/javadoc/spotless-plugin-gradle/3.0.0.RC2/com/diffplug/gradle/spotless/FormatExtension.html)) like those above.  It also supports more powerful formatters:

* Eclipse's java code formatter (including style and import ordering)
* Google's [google-java-format](https://github.com/google/google-java-format)
* [FreshMark](https://github.com/diffplug/freshmark) (markdown with variables)
* Any user-defined function which takes an unformatted string and outputs a formatted version.

Contributions are welcome, see [the contributing guide](../CONTRIBUTING.md) for development info.

Spotless requires Gradle to be running on JRE 8+.<sup>See [issue #7](https://github.com/diffplug/spotless/issues/7) for details.</sup>

## Applying to Java source

```gradle
apply plugin: 'java'
...

spotless {
	java {
		// By default, all Java source sets will be formatted.  To change
		// this, set the 'target' parameter as described in the next section.

		licenseHeader '/* Licensed under Apache-2.0 */'	// License header
		licenseHeaderFile 'spotless.license.java'		// License header file
		// Obviously, you can't specify both licenseHeader and licenseHeaderFile at the same time

		importOrder ['java', 'javax', 'org', 'com', 'com.diffplug', '']	// An array of package names
		importOrderFile 'spotless.importorder'							// An import ordering file, exported from Eclipse
		// As before, you can't specify both importOrder and importOrderFile at the same time
		// You probably want an empty string at the end - all of the imports you didn't specify
		// explicitly will go there.

		removeUnusedImports() // removes any unused imports

		eclipseFormatFile 'spotless.eclipseformat.xml'	// XML file dumped out by the Eclipse formatter
		// If you have an older Eclipse properties file, you can use that too.
	}
}
```

See [ECLIPSE_SCREENSHOTS](../ECLIPSE_SCREENSHOTS.md) for screenshots that demonstrate how to get and install the eclipseFormatFile and importOrderFile mentioned above.

## Applying to Java source ([google-java-format](https://github.com/google/google-java-format))

```gradle
spotless {
	java {
		googleJavaFormat() // googleJavaFormat('1.1') to specify a specific version
		// you can then layer other format steps, such as
		licenseHeaderFile 'spotless.license.java'
	}
}
```

## Applying [FreshMark](https://github.com/diffplug/freshmark) to markdown files

Freshmark lets you generate markdown in the comments of your markdown.  This helps to keep badges and links up-to-date (see the source for this file), and can
also be helpful for generating complex tables (see the source for [the parent readme](../README.md)).

To apply freshmark to all of the `.md` files in your project, with all of your project's properties available for templating, use this snippet:

```gradle
spotless {
	freshmark {
		target 'README.md', 'CONTRIBUTING.md'	// defaults to '**/*.md'
		propertiesFile('gradle.properties')		// loads all the properties in the given file
		properties {
			it.put('key', 'value')				// specify other properties manually
		}
	}
}
```

## Custom rules

Spotless is a generic system for specifying a sequence of steps which are applied to a set of files.

```gradle
spotless {
	// this will create two tasks: spotlessMiscCheck and spotlessMiscApply
	format 'misc', {
		// target determines which files this format will apply to
		// - if you pass a string or a list of strings, they will be treated
		//       as 'include' parameters to a fileTree in the root directory
		// - if you pass a FileCollection, it will pass through untouched
		//       e.g. project.files('build.gradle', 'settings.gradle')
		// - if you pass anything else, it will be sent to project.files(yourArg)
		target '**/*.gradle', '**/*.md', '**/.gitignore'

		// spotless has built-in rules for the most basic formatting tasks
		trimTrailingWhitespace()
		indentWithTabs() // or spaces. Takes an integer argument if you don't like 4
		endWithNewline()

		// you can also call out to your own function
		custom 'superFormatter', {
			// when writing a custom step, it will be helpful to know
			// how the formatting process works, which is as follows:

			// 1) Load each target file, and convert it to unix-style line endings ('\n')
			// 2) Pass its content through a series of steps, feeding the output of each step to the next
			// 3) Put the correct line endings back on, then either check or apply

			// each step receives a string as input, and should output
			// a formatted string as output.  Each step can trust that its
			// input will have unix newlines, and it must promise to output
			// only unix newlines.  Other than that, anything is fair game!
		}
	}
}
```

If you use `custom` or `customLazy`, you might want to take a look at [this javadoc](https://diffplug.github.io/spotless/javadoc/spotless-plugin-gradle/3.0.0.RC2/com/diffplug/gradle/spotless/FormatExtension.html#bumpThisNumberIfACustomStepChanges-int-) for a big performance win.

See [`JavaExtension.java`](src/main/java/com/diffplug/gradle/spotless/java/JavaExtension.java?ts=4) if you'd like to see how a language-specific set of custom rules is implemented.  We'd love PR's which add support for other languages.

## Line endings and encodings (invisible stuff)

Spotless uses UTF-8 by default, but you can use [any encoding which Java supports](https://docs.oracle.com/javase/8/docs/technotes/guides/intl/encoding.doc.html).  You can set it globally, and you can also set it per-format.

```gradle
spotless {
	java {
		...
		encoding 'Cp1252' // java will have Cp1252
	}
	encoding 'US-ASCII'   // but all other formats will be interpreted as US-ASCII
}
```

Line endings can also be set globally or per-format using the `lineEndings` property.  Spotless supports four line ending modes: `UNIX`, `WINDOWS`, `PLATFORM_NATIVE`, and `GIT_ATTRIBUTES`.  The default value is `GIT_ATTRIBUTES`, and *we highly recommend that you* ***do not change*** *this value*.  Git has opinions about line endings, and if Spotless and git disagree, then you're going to have a bad time.

You can easily set the line endings of different files using [a `.gitattributes` file](https://help.github.com/articles/dealing-with-line-endings/).  Here's an example `.gitattributes` which sets all files to unix newlines: `* text eol=lf`.

## How do I preview what `spotlessApply` will do?

- Save your working tree with `git add -A`, then `git commit -m "Checkpoint before spotless."`
- Run `gradlew spotlessApply`
- View the changes with `git diff`
- If you don't like what spotless did, `git reset --hard`
- If you'd like to remove the "checkpoint" commit, `git reset --soft head~1` will make the checkpoint commit "disappear" from history, but keeps the changes in your working directory.

## Example configurations (from real-world projects)

Spotless is hosted on jcenter and at plugins.gradle.org. [Go here](https://plugins.gradle.org/plugin/com.diffplug.gradle.spotless) if you're not sure how to import the plugin.

* [JUnit 5](https://github.com/junit-team/junit-lambda/blob/151d52ffab07881de71a8396a9620f18072c65ec/build.gradle#L86-L101) (aka JUnit Lambda)
* [opentest4j](https://github.com/ota4j-team/opentest4j/blob/aab8c204be05609e9f76c2c964c3d6845cd0de14/build.gradle#L63-L80)
* [Durian](https://github.com/diffplug/durian) ([direct link to spotless section in its build.gradle](https://github.com/diffplug/durian/blob/v3.2.0/build.gradle#L65-L85))
* [DurianRx](https://github.com/diffplug/durian-rx) ([direct link to spotless section in its build.gradle](https://github.com/diffplug/durian-rx/blob/v1.1.0/build.gradle#L92-L113))
* [DurianSwt](https://github.com/diffplug/durian-swt) ([direct link to spotless section in its build.gradle](https://github.com/diffplug/durian-swt/blob/v1.3.0/build.gradle#L137-L158))
* [MatConsoleCtl](https://github.com/diffplug/matconsolectl) ([direct link to spotless section in its build.gradle](https://github.com/diffplug/matconsolectl/blob/v4.4.1/build.gradle#L169-L189))
* [MatFileRW](https://github.com/diffplug/matfilerw) ([direct link to spotless section in its build.gradle](https://github.com/diffplug/matfilerw/blob/v1.3.1/build.gradle#L129-L149))
* [Goomph](https://github.com/diffplug/goomph) ([direct link to spotless section in its build.gradle](https://github.com/diffplug/goomph/blob/v1.0.0/build.gradle#L78-L99))
* [FreshMark](https://github.com/diffplug/freshmark) ([direct link to spotless section in its build.gradle](https://github.com/diffplug/freshmark/blob/v1.3.0/build.gradle#L52-L73))
* [JScriptBox](https://github.com/diffplug/jscriptbox) ([direct link to spotless section in its build.gradle](https://github.com/diffplug/jscriptbox/blob/v3.0.0/build.gradle#L45-L65))
* (Your project here)

<!---freshmark /javadoc -->
