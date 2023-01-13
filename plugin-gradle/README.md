# <img align="left" src="../_images/spotless_logo.png"> Spotless plugin for Gradle
*Keep your code Spotless with Gradle*

<!---freshmark shields
output = [
  link(shield('Gradle plugin', 'plugins.gradle.org', 'com.diffplug.spotless', 'blue'), 'https://plugins.gradle.org/plugin/com.diffplug.spotless'),
  link(shield('Maven central', 'mavencentral', 'yes', 'blue'), 'https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.diffplug.spotless%22%20AND%20a%3A%22spotless-plugin-gradle%22'),
  link(shield('Javadoc', 'javadoc', 'yes', 'blue'), 'https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/{{versionLast}}/index.html'),
  link(shield('License Apache', 'license', 'apache', 'blue'), 'https://tldrlegal.com/license/apache-license-2.0-(apache-2.0)'),
  link(shield('Changelog', 'changelog', '{{versionLast}}', 'blue'), 'CHANGES.md'),
  '',
  link(image('Circle CI', 'https://circleci.com/gh/diffplug/spotless/tree/main.svg?style=shield'), 'https://circleci.com/gh/diffplug/spotless/tree/main'),
  link(shield('Live chat', 'gitter', 'chat', 'brightgreen'), 'https://gitter.im/{{org}}/{{name}}'),
  link(shield('VS Code plugin', 'IDE', 'VS Code', 'blueviolet'), 'https://marketplace.visualstudio.com/items?itemName=richardwillis.vscode-spotless-gradle'),
  link(shield('IntelliJ plugin', 'IDE', 'IntelliJ', 'blueviolet'), 'https://plugins.jetbrains.com/plugin/18321-spotless-gradle'),
  link(shield('Add other IDE', 'IDE', 'add yours', 'blueviolet'), 'IDE_HOOK.md')
  ].join('\n');
-->
[![Gradle plugin](https://img.shields.io/badge/plugins.gradle.org-com.diffplug.spotless-blue.svg)](https://plugins.gradle.org/plugin/com.diffplug.spotless)
[![Maven central](https://img.shields.io/badge/mavencentral-yes-blue.svg)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.diffplug.spotless%22%20AND%20a%3A%22spotless-plugin-gradle%22)
[![Javadoc](https://img.shields.io/badge/javadoc-yes-blue.svg)](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.12.1/index.html)
[![License Apache](https://img.shields.io/badge/license-apache-blue.svg)](https://tldrlegal.com/license/apache-license-2.0-(apache-2.0))
[![Changelog](https://img.shields.io/badge/changelog-6.12.1-blue.svg)](CHANGES.md)

[![Circle CI](https://circleci.com/gh/diffplug/spotless/tree/main.svg?style=shield)](https://circleci.com/gh/diffplug/spotless/tree/main)
[![Live chat](https://img.shields.io/badge/gitter-chat-brightgreen.svg)](https://gitter.im/diffplug/spotless)
[![VS Code plugin](https://img.shields.io/badge/IDE-VS_Code-blueviolet.svg)](https://marketplace.visualstudio.com/items?itemName=richardwillis.vscode-spotless-gradle)
[![IntelliJ plugin](https://img.shields.io/badge/IDE-IntelliJ-blueviolet.svg)](https://plugins.jetbrains.com/plugin/18321-spotless-gradle)
[![Add other IDE](https://img.shields.io/badge/IDE-add_yours-blueviolet.svg)](IDE_HOOK.md)
<!---freshmark /shields -->

<!---freshmark javadoc
output = prefixDelimiterReplace(input, 'https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/', '/', versionLast)
-->

Spotless is a general-purpose formatting plugin used by [4,000 projects on GitHub (August 2020)](https://github.com/search?l=gradle&q=spotless&type=Code).  It is completely à la carte, but also includes powerful "batteries-included" if you opt-in.

To people who use your build, it looks like this ([IDE support also available](IDE_HOOK.md)):

```console
user@machine repo % ./gradlew build
:spotlessJavaCheck FAILED
  The following files had format violations:
  src\main\java\com\diffplug\gradle\spotless\FormatExtension.java
    -\t\t····if·(targets.length·==·0)·{
    +\t\tif·(targets.length·==·0)·{
  Run './gradlew spotlessApply' to fix these violations.
user@machine repo % ./gradlew spotlessApply
:spotlessApply
BUILD SUCCESSFUL
user@machine repo % ./gradlew build
BUILD SUCCESSFUL
```

Spotless supports all of Gradle's built-in performance features (incremental build, remote and local buildcache, lazy configuration, etc), and also automatically fixes [idempotence issues](https://github.com/diffplug/spotless/blob/main/PADDEDCELL.md), infers [line-endings from git](#line-endings-and-encodings-invisible-stuff), is cautious about [misconfigured encoding](https://github.com/diffplug/spotless/blob/08340a11566cdf56ecf50dbd4d557ed84a70a502/testlib/src/test/java/com/diffplug/spotless/EncodingErrorMsgTest.java#L34-L38) bugs, and can use git to [ratchet formatting](#ratchet) without "format-everything" commits.


### Table of Contents

- [**Quickstart**](#quickstart)
  - [Requirements](#requirements)
- **Languages**
  - [Java](#java) ([google-java-format](#google-java-format), [eclipse jdt](#eclipse-jdt), [clang-format](#clang-format), [prettier](#prettier), [palantir-java-format](#palantir-java-format), [formatAnnotations](#formatAnnotations))
  - [Groovy](#groovy) ([eclipse groovy](#eclipse-groovy))
  - [Kotlin](#kotlin) ([ktfmt](#ktfmt), [ktlint](#ktlint), [diktat](#diktat), [prettier](#prettier))
  - [Scala](#scala) ([scalafmt](#scalafmt))
  - [C/C++](#cc) ([clang-format](#clang-format), [eclipse cdt](#eclipse-cdt))
  - [Python](#python) ([black](#black))
  - [FreshMark](#freshmark) aka markdown
  - [Antlr4](#antlr4) ([antlr4formatter](#antlr4formatter))
  - [SQL](#sql) ([dbeaver](#dbeaver), [prettier](#prettier))
  - [Typescript](#typescript) ([tsfmt](#tsfmt), [prettier](#prettier), [ESLint](#eslint-typescript))
  - [Javascript](#javascript) ([prettier](#prettier), [ESLint](#eslint-javascript))
  - [JSON](#json)
  - Multiple languages
    - [Prettier](#prettier) ([plugins](#prettier-plugins), [npm detection](#npm-detection), [`.npmrc` detection](#npmrc-detection))
      - javascript, jsx, angular, vue, flow, typescript, css, less, scss, html, json, graphql, markdown, ymaml
    - [clang-format](#clang-format)
      - c, c++, c#, objective-c, protobuf, javascript, java
    - [eclipse web tools platform](#eclipse-web-tools-platform)
      - css, html, js, json, xml
- **Language independent**
  - [Generic steps](#generic-steps)
  - [License header](#license-header) ([slurp year from git](#retroactively-slurp-years-from-git-history))
  - [How can I enforce formatting gradually? (aka "ratchet")](#ratchet)
  - [`spotless:off` and `spotless:on`](#spotlessoff-and-spotlesson)
  - [Line endings and encodings (invisible stuff)](#line-endings-and-encodings-invisible-stuff)
  - [Custom steps](#custom-steps)
  - [Multiple (or custom) language-specific blocks](#multiple-or-custom-language-specific-blocks)
  - [Inception (languages within languages within...)](#inception-languages-within-languages-within)
  - [Disabling warnings and error messages](#disabling-warnings-and-error-messages)
  - [Dependency resolution modes](#dependency-resolution-modes)
  - [How do I preview what `spotlessApply` will do?](#how-do-i-preview-what-spotlessapply-will-do)
  - [Example configurations (from real-world projects)](#example-configurations-from-real-world-projects)

***Contributions are welcome, see [the contributing guide](../CONTRIBUTING.md) for development info.***

## Quickstart

To use it in your buildscript, just [add the Spotless dependency](https://plugins.gradle.org/plugin/com.diffplug.spotless), and configure it like so:

```gradle
spotless {
  // optional: limit format enforcement to just the files changed by this feature branch
  ratchetFrom 'origin/main'

  format 'misc', {
    // define the files to apply `misc` to
    target '*.gradle', '*.md', '.gitignore'

    // define the steps to apply to those files
    trimTrailingWhitespace()
    indentWithTabs() // or spaces. Takes an integer argument if you don't like 4
    endWithNewline()
  }
  java {
    // don't need to set target, it is inferred from java

    // apply a specific flavor of google-java-format
    googleJavaFormat('1.8').aosp().reflowLongStrings()
    // fix formatting of type annotations
    formatAnnotations()
    // make sure every file has the following copyright header.
    // optionally, Spotless can set copyright years by digging
    // through git history (see "license" section below)
    licenseHeader '/* (C)$YEAR */'
  }
}
```

Spotless consists of a list of formats (in the example above, `misc` and `java`), and each format has:
- a `target` (the files to format), which you set with [`target`](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.12.1/com/diffplug/gradle/spotless/FormatExtension.html#target-java.lang.Object...-) and [`targetExclude`](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.12.1/com/diffplug/gradle/spotless/FormatExtension.html#targetExclude-java.lang.Object...-)
- a list of `FormatterStep`, which are just `String -> String` functions, such as [`replace`](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.12.1/com/diffplug/gradle/spotless/FormatExtension.html#replace-java.lang.String-java.lang.CharSequence-java.lang.CharSequence-), [`replaceRegex`](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.12.1/com/diffplug/gradle/spotless/FormatExtension.html#replaceRegex-java.lang.String-java.lang.String-java.lang.String-), [`trimTrailingWhitespace`](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.12.1/com/diffplug/gradle/spotless/FormatExtension.html#replace-java.lang.String-java.lang.CharSequence-java.lang.CharSequence-), [`custom`](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.12.1/com/diffplug/gradle/spotless/FormatExtension.html#custom-java.lang.String-groovy.lang.Closure-), [`prettier`](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.12.1/com/diffplug/gradle/spotless/FormatExtension.html#prettier--), [`eclipseWtp`](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.12.1/com/diffplug/gradle/spotless/FormatExtension.html#eclipseWtp-com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep-), [`licenseHeader`](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.12.1/com/diffplug/gradle/spotless/FormatExtension.html#licenseHeader-java.lang.String-java.lang.String-) etc.

All the generic steps live in [`FormatExtension`](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.12.1/com/diffplug/gradle/spotless/FormatExtension.html), and there are many language-specific steps which live in its language-specific subclasses, which are described below.

### Requirements

Spotless requires JRE 8+, and Gradle 6.1.1+.  Some steps require JRE 11+, `Unsupported major.minor version` means you're using a step that needs a newer JRE.

If you're stuck on an older version of Gradle, `id 'com.diffplug.gradle.spotless' version '4.5.1'` supports all the way back to Gradle 2.x.

<a name="applying-to-java-source"></a>

## Java

`com.diffplug.gradle.spotless.JavaExtension` [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.12.1/com/diffplug/gradle/spotless/JavaExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/JavaExtension.java)

```gradle
spotless {
  java {
    // Use the default importOrder configuration
    importOrder()
    // optional: you can specify import groups directly
    // note: you can use an empty string for all the imports you didn't specify explicitly, '|' to join group without blank line, and '\\#` prefix for static imports
    importOrder('java|javax', 'com.acme', '', '\\#com.acme', '\\#')
    // optional: instead of specifying import groups directly you can specify a config file
    // export config file: https://github.com/diffplug/spotless/blob/main/ECLIPSE_SCREENSHOTS.md#creating-spotlessimportorder
    importOrderFile('eclipse-import-order.txt') // import order file as exported from eclipse

    removeUnusedImports()

    // Choose one of these formatters.
    googleJavaFormat()   // has its own section below
    eclipse()            // has its own section below
    prettier()           // has its own section below
    clangFormat()        // has its own section below

    formatAnnotations()  // fixes formatting of type annotations, see below

    licenseHeader '/* (C) $YEAR */' // or licenseHeaderFile
  }
}
```

<a name="applying-to-android-java-source"></a>

The target is usually inferred automatically from the java source sets. However, Spotless cannot automatically detect [android](https://github.com/diffplug/spotless/issues/111) or [java-gradle-plugin](https://github.com/diffplug/spotless/issues/437) sources, but you can fix this easily:

```gradle
spotless {
  java {
    target 'src/*/java/**/*.java'
```

### google-java-format

[homepage](https://github.com/google/google-java-format). [changelog](https://github.com/google/google-java-format/releases).
```gradle
spotless {
  java {
    googleJavaFormat()
    // optional: you can specify a specific version and/or switch to AOSP style
    //   and/or reflow long strings (requires at least 1.8)
    //   and/or use custom group artifact (you probably don't need this)
    googleJavaFormat('1.8').aosp().reflowLongStrings().groupArtifact('com.google.googlejavaformat:google-java-format')
```

### palantir-java-format

[homepage](https://github.com/palantir/palantir-java-format). [changelog](https://github.com/palantir/palantir-java-format/releases).
```gradle
spotless {
  java {
    palantirJavaFormat()
    // optional: you can specify a specific version
    palantirJavaFormat('2.9.0')
```

### eclipse jdt

[homepage](https://www.eclipse.org/downloads/packages/). [compatible versions](https://github.com/diffplug/spotless/tree/main/lib-extra/src/main/resources/com/diffplug/spotless/extra/eclipse_jdt_formatter). See [here](../ECLIPSE_SCREENSHOTS.md) for screenshots that demonstrate how to get and install the config file mentioned below.

```gradle
spotless {
  java {
    eclipse()
    // optional: you can specify a specific version and/or config file
    eclipse('4.17').configFile('eclipse-prefs.xml')
```


### formatAnnotations

Type annotations should be on the same line as the type that they qualify.

```java
  @Override
  @Deprecated
  @Nullable @Interned String s;
```

However, some tools format them incorrectly, like this:

```java
  @Override
  @Deprecated
  @Nullable
  @Interned
  String s;
```

To fix the incorrect formatting, add the `formatAnnotations()` rule after a Java formatter.  For example:

```gradle
spotless {
  java {
    googleJavaFormat()
    formatAnnotations()
  }
}
```

This does not re-order annotations, it just removes incorrect newlines.

A type annotation is an annotation that is meta-annotated with `@Target({ElementType.TYPE_USE})`.
Spotless has a default list of well-known type annotations.
You can use `addTypeAnnotation()` and `removeTypeAnnotation()` to override its defaults:

```gradle
    formatAnnotations().addTypeAnnotation("Empty").addTypeAnnotation("NonEmpty").removeTypeAnnotation("Localized")
```

You can make a pull request to add new annotations to Spotless's default list.


<a name="applying-to-groovy-source"></a>

## Groovy

- `com.diffplug.gradle.spotless.GroovyExtension` [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.12.1/com/diffplug/gradle/spotless/GroovyExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/GroovyExtension.java)
- `com.diffplug.gradle.spotless.GroovyGradleExtension` [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.12.1/com/diffplug/gradle/spotless/GroovyGradleExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/GroovyGradleExtension.java)

Configuration for Groovy is similar to [Java](#java), in that it also supports `licenseHeader` and `importOrder`.

The groovy formatter's default behavior is to format all `.groovy` and `.java` files found in the Java and Groovy source sets.  If you would like to exclude the `.java` files, set the parameter `excludeJava`, or you can set the `target` parameter as described in the [Custom rules](#custom) section.

```gradle
apply plugin: 'groovy'
spotless {
  groovy {
    // Use the default importOrder configuration
    importOrder()
    // optional: you can specify import groups directly
    // note: you can use an empty string for all the imports you didn't specify explicitly, and '\\#` prefix for static imports
    importOrder('java', 'javax', 'com.acme', '', '\\#com.acme', '\\#')
    // optional: instead of specifying import groups directly you can specify a config file
    // export config file: https://github.com/diffplug/spotless/blob/main/ECLIPSE_SCREENSHOTS.md#creating-spotlessimportorder
    importOrderFile('eclipse-import-order.txt') // import order file as exported from eclipse

    excludeJava() // excludes all Java sources within the Groovy source dirs from formatting
    // the Groovy Eclipse formatter extends the Java Eclipse formatter,
    // so it formats Java files by default (unless `excludeJava` is used).
    greclipse() // has its own section below

    licenseHeader '/* (C) $YEAR */' // or licenseHeaderFile
  }
  groovyGradle {
    target '*.gradle' // default target of groovyGradle
    greclipse()
  }
}
```

### eclipse groovy

[homepage](https://github.com/groovy/groovy-eclipse/wiki). [changelog](https://github.com/groovy/groovy-eclipse/releases). [compatible versions](https://github.com/diffplug/spotless/tree/main/lib-extra/src/main/resources/com/diffplug/spotless/extra/eclipse_groovy_formatter). The Groovy formatter uses some of the [eclipse jdt](#eclipse-jdt) configuration parameters in addition to groovy-specific ones. All parameters can be configured within a single file, like the Java properties file [greclipse.properties](../testlib/src/main/resources/groovy/greclipse/format/greclipse.properties) in the previous example. The formatter step can also load the [exported Eclipse properties](../ECLIPSE_SCREENSHOTS.md) and augment it with the `.metadata/.plugins/org.eclipse.core.runtime/.settings/org.codehaus.groovy.eclipse.ui.prefs` from your Eclipse workspace as shown below.

```gradle
spotless {
  groovy {
    // Use the default version and Groovy-Eclipse default configuration
    greclipse()
    // optional: you can specify a specific version or config file(s)
    // compatible versions: https://github.com/diffplug/spotless/tree/main/lib-extra/src/main/resources/com/diffplug/spotless/extra/groovy_eclipse_formatter
    greclipse('2.3.0').configFile('spotless.eclipseformat.xml', 'org.codehaus.groovy.eclipse.ui.prefs')
```

Groovy-Eclipse formatting errors/warnings lead per default to a build failure. This behavior can be changed by adding the property/key value `ignoreFormatterProblems=true` to a configuration file. In this scenario, files causing problems, will not be modified by this formatter step.

## Kotlin

- `com.diffplug.gradle.spotless.KotlinExtension` [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.12.1/com/diffplug/gradle/spotless/KotlinExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/KotlinExtension.java)
- `com.diffplug.gradle.spotless.KotlinGradleExtension` [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.12.1/com/diffplug/gradle/spotless/KotlinGradleExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/KotlinGradleExtension.java)

```gradle
spotless { // if you are using build.gradle.kts, instead of 'spotless {' use:
           // configure<com.diffplug.gradle.spotless.SpotlessExtension> {
  kotlin {
    // by default the target is every '.kt' and '.kts` file in the java sourcesets
    ktfmt()    // has its own section below
    ktlint()   // has its own section below
    diktat()   // has its own section below
    prettier() // has its own section below
    licenseHeader '/* (C)$YEAR */' // or licenseHeaderFile
  }
  kotlinGradle {
    target '*.gradle.kts' // default target for kotlinGradle
    ktlint() // or ktfmt() or prettier()
  }
}
```

### ktfmt

[homepage](https://github.com/facebookincubator/ktfmt). [changelog](https://github.com/facebookincubator/ktfmt/releases).

```kotlin
spotless {
  kotlin {
    ktfmt('0.30').dropboxStyle() // version and dropbox style are optional
```

<a name="applying-ktlint-to-kotlin-files"></a>

### ktlint

[homepage](https://github.com/pinterest/ktlint). [changelog](https://github.com/pinterest/ktlint/releases).

Spotless respects the `.editorconfig` settings by providing `editorConfigPath` option.
([ktlint docs](https://github.com/pinterest/ktlint#editorconfig)).
Default value is the `.editorconfig` file located in the top project.
Passing `null` will clear the option.

Additionally, `editorConfigOverride` options will override what's supplied in `.editorconfig` file.

```kotlin
spotless {
  kotlin {
    // version, setUseExperimental, userData and editorConfigOverride are all optional
    ktlint("0.45.2")
      .setUseExperimental(true)
      .userData(mapOf("android" to "true"))
      .editorConfigPath("$projectDir/config/.editorconfig")  // sample unusual placement
      .editorConfigOverride(mapOf("indent_size" to 2))
  }
}
```

### diktat

[homepage](https://github.com/cqfn/diKTat). [changelog](https://github.com/cqfn/diKTat/releases). You can provide configuration path manually as `configFile`.

```kotlin
spotless {
  kotlin {
    // version and configFile are both optional
    diktat('1.0.1').configFile("full/path/to/diktat-analysis.yml")
```

<a name="applying-scalafmt-to-scala-files"></a>

## Scala

`com.diffplug.gradle.spotless.ScalaExtension` [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.12.1/com/diffplug/gradle/spotless/ScalaExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/ScalaExtension.java)

```gradle
spotless {
  scala {
    // by default, all `.scala` and `.sc` files in the java sourcesets will be formatted

    scalafmt() // has its own section below

    licenseHeader '/* (C) $YEAR */', 'package ' // or licenseHeaderFile
    // note the 'package ' argument - this is a regex which identifies the top
    // of the file, be careful that all of your sources have a package declaration,
    // or pick a regex which works better for your code
  }
}
```

### scalafmt

[homepage](https://scalameta.org/scalafmt/). [changelog](https://github.com/scalameta/scalafmt/releases). [config docs](https://scalameta.org/scalafmt/docs/configuration.html).

```gradle
spotless {
  scala {
    // version and configFile, majorScalaVersion are all optional
    scalafmt('3.5.9').configFile('scalafmt.conf').majorScalaVersion('2.13')
```

<a name="applying-to-cc-sources"></a>

## C/C++

`com.diffplug.gradle.spotless.CppExtension` [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.12.1/com/diffplug/gradle/spotless/CppExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/CppExtension.java)

```gradle
spotless {
  cpp {
    target 'src/native/**' // you have to set the target manually

    clangFormat()  // has its own section below
    eclipseCdt()   // has its own section below

    licenseHeader '/* (C) $YEAR */' // or licenseHeaderFile
  }
}
```

<a name="eclipse-cdt-formatter"></a>

### eclipse cdt

[homepage](https://www.eclipse.org/cdt/). [compatible versions](https://github.com/diffplug/spotless/tree/main/lib-extra/src/main/resources/com/diffplug/spotless/extra/eclipse_cdt_formatter).

```gradle
spotles {
  cpp {
    // version and configFile are both optional
    eclipseCdt('4.13.0').configFile('eclipse-cdt.xml')
  }
}
```

## Python

`com.diffplug.gradle.spotless.PythonExtension` [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.12.1/com/diffplug/gradle/spotless/PythonExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/PythonExtension.java)

```gradle
spotless {
  python {
    target 'src/main/**/*.py' // have to set manually

    black()  // has its own section below

    licenseHeader '/* (C) $YEAR */', 'REGEX_TO_DEFINE_TOP_OF_FILE' // or licenseHeaderFile
  }
}
```

### black

[homepage](https://github.com/psf/black). [changelog](https://github.com/psf/black/blob/master/CHANGES.md).

```gradle
black('19.10b0') // version is optional

// if black is not on your path, you must specify its location manually
black().pathToExe('C:/myuser/.pyenv/versions/3.8.0/scripts/black.exe')
// Spotless always checks the version of the black it is using
// and will fail with an error if it does not match the expected version
// (whether manually specified or default). If there is a problem, Spotless
// will suggest commands to help install the correct version.
//   TODO: handle installation & packaging automatically - https://github.com/diffplug/spotless/issues/674
```

<a name="applying-freshmark-to-markdown-files"></a>

## FreshMark

`com.diffplug.gradle.spotless.FreshMarkExtension` [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.12.1/com/diffplug/gradle/spotless/FreshMarkExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/FreshMarkExtension.java)

[homepage](https://github.com/diffplug/freshmark). [changelog](https://github.com/diffplug/freshmark/blob/master/CHANGES.md). FreshMark lets you generate markdown in the comments of your markdown.  This helps to keep badges and links up-to-date (see the source for this file), and can
also be helpful for generating complex tables (see the source for [the parent readme](../README.md)).

To apply freshmark to all of the `.md` files in your project, with all of your project's properties available for templating, use this snippet:

```gradle
spotless {
  freshmark {
    target '*.md' // you have to set the target manually
    propertiesFile('gradle.properties')		// loads all the properties in the given file
    properties {
      it.put('key', 'value')				// specify other properties manually
    }
  }
}
```

## Antlr4

`com.diffplug.gradle.spotless.Antlr4Extension` [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.12.1/com/diffplug/gradle/spotless/Antlr4Extension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/Antlr4Extension.java)

```gradle
spotless {
  antlr4 {
    target 'src/*/antlr4/**/*.g4' // default value, you can change if you want
    antlr4Formatter() // has its own section below
    licenseHeader '/* (C) $YEAR */' // or licenseHeaderFile
  }
}
```

### antlr4formatter

[homepage](https://github.com/antlr/Antlr4Formatter). [available versions](https://search.maven.org/artifact/com.khubla.antlr4formatter/antlr4-formatter).

```gradle
antlr4formatter('1.2.1') // version is optional
```

<a name="sql-dbeaver"></a>
<a name="applying-dbeaver-to-sql-scripts"></a>

## SQL

`com.diffplug.gradle.spotless.SqlExtension` [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.12.1/com/diffplug/gradle/spotless/SqlExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/SqlExtension.java)

```gradle
spotless {
  sql {
    target 'src/main/resources/**/*.sql' // have to set manually

    dbeaver()  // has its own section below
    prettier() // has its own section below
  }
}
```

### dbeaver

[homepage](https://dbeaver.io/). DBeaver is only distributed as a monolithic jar, so the formatter used here was copy-pasted into Spotless, and thus there is no version to change.

```gradle
spotless {
  sql {
    dbeaver().configFile('dbeaver.properties') // configFile is optional
```

Default configuration file, other options [available here](https://github.com/diffplug/spotless/blob/main/lib/src/main/java/com/diffplug/spotless/sql/dbeaver/DBeaverSQLFormatterConfiguration.java).

```properties
# case of the keywords (UPPER, LOWER or ORIGINAL)
sql.formatter.keyword.case=UPPER
# Statement delimiter
sql.formatter.statement.delimiter=;
# Indentation style (space or tab)
sql.formatter.indent.type=space
# Number of indentation characters
sql.formatter.indent.size=4
```

<a name="applying-to-typescript-source"></a>

## Typescript

- `com.diffplug.gradle.spotless.TypescriptExtension` [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.12.1/com/diffplug/gradle/spotless/TypescriptExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/TypescriptExtension.java)

```gradle
spotless {
  typescript {
    target 'src/**/*.ts' // you have to set the target manually

    tsfmt()    // has its own section below
    prettier() // has its own section below
    eslint() // has its own section below

    licenseHeader '/* (C) $YEAR */', '(import|const|declare|export|var) ' // or licenseHeaderFile
    // note the '(import|const|...' argument - this is a regex which identifies the top
    // of the file, be careful that all of your sources have a suitable top-level declaration,
    // or pick a regex which works better for your code
  }
}
```

### tsfmt

[npm](https://www.npmjs.com/package/typescript-formatter). [changelog](https://github.com/vvakame/typescript-formatter/blob/master/CHANGELOG.md). *Please note:*
The auto-discovery of config files (up the file tree) will not work when using tsfmt within spotless,
  hence you are required to provide resolvable file paths for config files, or alternatively provide the configuration inline. See [tsfmt's default config settings](https://github.com/vvakame/typescript-formatter/blob/7764258ad42ac65071399840d1b8701868510ca7/lib/utils.ts#L11L32) for what is available.

```gradle
spotless {
  typescript {
    tsfmt('7.2.2')
      // provide config inline: https://github.com/vvakame/typescript-formatter/blob/7764258ad42ac65071399840d1b8701868510ca7/lib/utils.ts#L11L32
      .config(['indentSize': 1, 'convertTabsToSpaces': true])
      // or according to tsfmt-parameters: https://github.com/vvakame/typescript-formatter/blob/7764258ad42ac65071399840d1b8701868510ca7/lib/index.ts#L27L34
      .tsconfigFile('tsconfig.json')
      .tslintFile('tslint.json')
      .vscodeFile('vscode.json')
      .tsfmtFile('tsfmt.json')
```

**Prerequisite: tsfmt requires a working NodeJS version**

For details, see the [npm detection](#npm-detection) and [`.npmrc` detection](#npmrc-detection) sections of prettier, which apply also to tsfmt.

### ESLint (Typescript)

[npm](https://www.npmjs.com/package/eslint). [changelog](https://github.com/eslint/eslint/blob/main/CHANGELOG.md). *Please note:*
The auto-discovery of config files (up the file tree) will not work when using ESLint within spotless,
hence you are required to provide resolvable file paths for config files, or alternatively provide the configuration inline.

The configuration is very similar to the [ESLint (Javascript)](#eslint-javascript) configuration. In typescript, a
reference to a `tsconfig.json` is required.

```gradle
spotless {
  typescript {
    eslint('8.30.0') // version is optional
    eslint(['my-eslint-fork': '1.2.3', 'my-eslint-plugin': '1.2.1']) // can specify exactly which npm packages to use

    eslint()
      // configuration is mandatory. Provide inline config or a config file.
      // a) inline-configuration
      .configJs('''
        {
          env: {
            browser: true,
            es2021: true
          },
          extends: 'standard-with-typescript',
          overrides: [
          ],
          parserOptions: {
            ecmaVersion: 'latest',
            sourceType: 'module',
            project: './tsconfig.json',
          },
          rules: {
          }
        }
      ''')
      // b) config file
      .configFile('.eslintrc.js')
      // recommended: provide a tsconfig.json - especially when using the styleguides
      .tsconfigFile('tsconfig.json')
  }
}
```

**Prerequisite: ESLint requires a working NodeJS version**

For details, see the [npm detection](#npm-detection) and [`.npmrc` detection](#npmrc-detection) sections of prettier, which apply also to ESLint.

## Javascript

- `com.diffplug.gradle.spotless.JavascriptExtension` [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.12.1/com/diffplug/gradle/spotless/JavascriptExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/JavascriptExtension.java)

```gradle
spotless {
  javascript {
    target 'src/**/*.js' // you have to set the target manually

    prettier() // has its own section below
    eslint() // has its own section below

    licenseHeader '/* (C) $YEAR */', 'REGEX_TO_DEFINE_TOP_OF_FILE' // or licenseHeaderFile
  }
}
```

### ESLint (Javascript)

[npm](https://www.npmjs.com/package/eslint). [changelog](https://github.com/eslint/eslint/blob/main/CHANGELOG.md). *Please note:*
The auto-discovery of config files (up the file tree) will not work when using ESLint within spotless,
hence you are required to provide resolvable file paths for config files, or alternatively provide the configuration inline.

The configuration is very similar to the [ESLint (Typescript)](#eslint-typescript) configuration. In javascript, *no*
`tsconfig.json` is supported.

```gradle

```gradle
spotless {
  javascript {
    eslint('8.30.0') // version is optional
    eslint(['my-eslint-fork': '1.2.3', 'my-eslint-plugin': '1.2.1']) // can specify exactly which npm packages to use

    eslint()
      // configuration is mandatory. Provide inline config or a config file.
      // a) inline-configuration
      .configJs('''
        {
          env: {
            browser: true,
            es2021: true
          },
          extends: 'standard',
          overrides: [
          ],
          parserOptions: {
            ecmaVersion: 'latest',
            sourceType: 'module'
          },
          rules: {
          }
        }
      ''')
      // b) config file
      .configFile('.eslintrc.js')
  }
}
```

**Prerequisite: ESLint requires a working NodeJS version**

For details, see the [npm detection](#npm-detection) and [`.npmrc` detection](#npmrc-detection) sections of prettier, which apply also to ESLint.

## JSON

- `com.diffplug.gradle.spotless.JsonExtension` [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.12.1/com/diffplug/gradle/spotless/JsonExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/JsonExtension.java)

```gradle
spotless {
  json {
    target 'src/**/*.json' // you have to set the target manually
    simple() // has its own section below
    prettier().config(['parser': 'json']) // see Prettier section below
    eclipseWtp('json') // see Eclipse web tools platform section
    gson() // has its own section below
  }
}
```

### simple

Uses a JSON pretty-printer that optionally allows configuring the number of spaces that are used to pretty print objects:

```gradle
spotless {
  json {
    target 'src/**/*.json'
    simple()
    // optional: specify the number of spaces to use
    simple().indentWithSpaces(6)
  }
}
```

### Gson

Uses Google Gson to also allow sorting by keys besides custom indentation - useful for i18n files.

```gradle
spotless {
  json {
    target 'src/**/*.json'
    gson()
      .indentWithSpaces(6) // optional: specify the number of spaces to use
      .sortByKeys()        // optional: sort JSON by its keys
      .escapeHtml()        // optional: escape HTML in values
      .version('2.8.1')    // optional: specify version
  }
}
```

Notes:
* There's no option in Gson to leave HTML as-is (i.e. escaped HTML would remain escaped, raw would remain raw). Either
all HTML characters are written escaped or none. Set `escapeHtml` if you prefer the former.
* `sortByKeys` will apply lexicographic order on the keys of the input JSON. See the
[javadoc of String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html#compareTo(java.lang.String))
for details.

<a name="applying-prettier-to-javascript--flow--typescript--css--scss--less--jsx--graphql--yaml--etc"></a>

## Prettier

[homepage](https://prettier.io/). [changelog](https://github.com/prettier/prettier/blob/master/CHANGELOG.md). [official plugins](https://prettier.io/docs/en/plugins.html#official-plugins). [community plugins](https://prettier.io/docs/en/plugins.html#community-plugins). Prettier is a formatter that can format almost every anything - JavaScript, JSX, Angular, Vue, Flow, TypeScript, CSS, Less, SCSS, HTML, JSON, GraphQL, Markdown (including GFM and MDX), and YAML.  It can format even more [using plugins](https://prettier.io/docs/en/plugins.html) (PHP, Ruby, Swift, XML, Apex, Elm, Java (!!), Kotlin, pgSQL, .properties, solidity, svelte, toml, shellscript, ...).

You can use prettier in any language-specific format, but usually you will be creating a generic format.

```gradle
spotless {
  format 'styling', {
    // you have to set the target manually
    target 'src/*/webapp/**/*.css', 'src/*/webapp/**/*.scss'

    prettier('2.0.4') // version is optional
    prettier(['my-prettier-fork': '1.16.4']) // can specify exactly which npm packages to use

    // by default, prettier uses the file's extension to guess a parser
    // but you can override that and specify the parser manually
    prettier().config(['parser': 'css'])

    // you can also set some style options
    // https://prettier.io/docs/en/configuration.html
    prettier().config(['tabWidth': 4])

    // you can also slurp from a file or even provide both (inline always takes precedence over file)
    prettier().config(['tabWidth': 4]).configFile('path-to/.prettierrc.yml')
  }
}
```

**Limitations:**
- The auto-discovery of config files (up the file tree) will not work when using prettier within spotless.
- Prettier's override syntax is not supported when using prettier within spotless.

To apply prettier to more kinds of files, just add more formats

<a name="using-plugins-for-prettier"></a>

### prettier plugins

Since spotless uses the actual npm prettier package behind the scenes, it is possible to use prettier with
[plugins](https://prettier.io/docs/en/plugins.html#official-plugins) or [community-plugins](https://www.npmjs.com/search?q=prettier-plugin) in order to support even more file types.

```gradle
spotless {
  java {
    prettier(['prettier': '2.0.5', 'prettier-plugin-java': '0.8.0']).config(['parser': 'java', 'tabWidth': 4])
  }
  format 'php', {
    target 'src/**/*.php'
    prettier(['prettier': '2.0.5', '@prettier/plugin-php': '0.14.2']).config(['parser': 'php', 'tabWidth': 3])
  }
}
```

### npm detection

Prettier is based on NodeJS, so a working NodeJS installation (especially npm) is required on the host running spotless.
Spotless will try to auto-discover an npm installation. If that is not working for you, it is possible to directly configure the npm binary to use.

```gradle
spotless {
  format 'javascript', {
    prettier().npmExecutable('/usr/bin/npm').config(...)
```

### `.npmrc` detection

Spotless picks up npm configuration stored in a `.npmrc` file either in the project directory or in your user home.
Alternatively you can supply spotless with a location of the `.npmrc` file to use. (This can be combined with `npmExecutable`, of course.)

```gradle
spotless {
  typescript {
    prettier().npmrc("$projectDir/config/.npmrc").config(...)
```

## clang-format

[homepage](https://clang.llvm.org/docs/ClangFormat.html). [changelog](https://releases.llvm.org/download.html). `clang-format` is a formatter for c, c++, c#, objective-c, protobuf, javascript, and java. You can use clang-format in any language-specific format, but usually you will be creating a generic format.

```gradle
spotless {
  format 'csharp', {
    // you have to set the target manually
    target 'src/**/*.cs'

    clangFormat('10.0.1') // version is optional

    // can also specify a code style
    clangFormat().style('LLVM') // or Google, Chromium, Mozilla, WebKit
    // TODO: support arbitrary .clang-format

    // if clang-format is not on your path, you must specify its location manually
    clangFormat().pathToExe('/usr/local/Cellar/clang-format/10.0.1/bin/clang-format')
    // Spotless always checks the version of the clang-format it is using
    // and will fail with an error if it does not match the expected version
    // (whether manually specified or default). If there is a problem, Spotless
    // will suggest commands to help install the correct version.
    //   TODO: handle installation & packaging automatically - https://github.com/diffplug/spotless/issues/673
  }
}
```

<a name="applying-eclipse-wtp-to-css--html--etc"></a>

## Eclipse web tools platform

[changelog](https://www.eclipse.org/webtools/). [compatible versions](https://github.com/diffplug/spotless/tree/main/lib-extra/src/main/resources/com/diffplug/spotless/extra/eclipse_wtp_formatters).

```gradle
spotless {
  format 'xml', {
    target 'src/**/*.xml' // must specify target
    eclipseWtp('xml')     // must specify a type (table below)
    eclipseWtp('xml', '4.13.0') // optional version
    // you can also specify an arbitrary number of config files
    eclipseWtp('xml').configFile('spotless.xml.prefs', 'spotless.common.properties'
  }
}
```
The WTP formatter accept multiple configuration files. All Eclipse configuration file formats are accepted as well as simple Java property files. Omit the `configFile` entirely to use the default Eclipse configuration. The following formatters and configurations are supported:

| Type | Configuration       | File location
| ---- | ------------------- | -------------
| CSS  | editor preferences  | .metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.wst.css.core.prefs
|      | cleanup preferences | .metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.wst.css.core.prefs
| HTML | editor preferences  | .metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.wst.html.core.prefs
|      | cleanup preferences | .metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.wst.html.core.prefs
|      | embedded CSS        | .metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.wst.css.core.prefs
|      | embedded JS         | Use the export in the Eclipse editor configuration dialog
| JS   | editor preferences  | Use the export in the Eclipse editor configuration dialog
| JSON | editor preferences  | .metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.wst.json.core.prefs
| XML  | editor preferences  | .metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.wst.xml.core.prefs

Note that `HTML` should be used for `X-HTML` sources instead of `XML`.

The Eclipse XML catalog cannot be configured for the Spotless WTP formatter, instead a
user defined catalog file can be specified using the property `userCatalog`. Catalog versions
1.0 and 1.1 are supported by Spotless.

Unlike Eclipse, Spotless WTP ignores per default external URIs in schema location hints and
external entities. To allow the access of external URIs, set the property `resolveExternalURI`
to true.

## Generic steps

[Prettier](#prettier), [eclipse wtp](#eclipse-web-tools-platform), and [license header](#license-header) are available in every format, and they each have their own section. As mentioned in the [quickstart](#quickstart), there are a variety of simple generic steps which are also available in every format, here are examples of these:

```gradle
spotless {
  // run a native binary
  format 'terraform', {
    target 'src/**/*.tf', 'src/**/*.tfvars' // you have to set the target manually
    nativeCmd('terraform', '/opt/homebrew/bin/terraform', ['fmt', '-']) // name, path to binary, additional arguments
  }
}
```

<a name="license-header-options"></a>

## License header

If the license header (specified with `licenseHeader` or `licenseHeaderFile`) contains `$YEAR` or `$today.year`, then that token will be replaced with the current 4-digit year.  For example, if Spotless is launched in 2020, then `/* Licensed under Apache-2.0 $YEAR. */` will produce `/* Licensed under Apache-2.0 2020. */`

Once a file's license header has a valid year, whether it is a year (`2020`) or a year range (`2017-2020`), it will not be changed.  If you want the date to be updated when it changes, enable the [`ratchetFrom` functionality](#ratchet), and the year will be automatically set to today's year according to the following table (assuming the current year is 2020):

* No license header -> `2020`
* `2017` -> `2017-2020`
* `2017-2019` -> `2017-2020`

See the [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.12.1/com/diffplug/gradle/spotless/FormatExtension.LicenseHeaderConfig.html) for a complete listing of options.

<a name="retroactively-populating-year-range-from-git-history"></a>

### Retroactively slurp years from git history

If your project has not been rigorous with copyright headers, and you'd like to use git history to repair this retroactively, you can do so with `-PspotlessSetLicenseHeaderYearsFromGitHistory=true`.  When run in this mode, Spotless will do an expensive search through git history for each file, and set the copyright header based on the oldest and youngest commits for that file.  This is intended to be a one-off sort of thing.

### Files with fixed header lines

Some files have fixed header lines (e.g. `<?xml version="1.0" ...` in XMLs, or `#!/bin/bash` in bash scripts). Comments cannot precede these, so the license header has to come after them, too.

To define what lines to skip at the beginning of such files, fill the `skipLinesMatching` option with a regular expression that matches them (e.g. `.skipLinesMatching("^#!.+?\$")` to skip shebangs).

<a name="ratchet"></a>

## How can I enforce formatting gradually? (aka "ratchet")

If your project is not currently enforcing formatting, then it can be a noisy transition.  Having a giant commit where every single file gets changed makes the history harder to read.  To address this, you can use the `ratchet` feature:

```gradle
spotless {
  ratchetFrom 'origin/main' // only format files which have changed since origin/main
```

In this mode, Spotless will apply only to files which have changed since `origin/main`.  You can ratchet from [any point you want](https://javadoc.io/doc/org.eclipse.jgit/org.eclipse.jgit/5.6.1.202002131546-r/org/eclipse/jgit/lib/Repository.html#resolve-java.lang.String-), even `HEAD`.  You can also set `ratchetFrom` per-format if you prefer (e.g. `spotless { java { ratchetFrom ...`).

However, we strongly recommend that you use a non-local branch, such as a tag or `origin/main`.  The problem with `HEAD` or any local branch is that as soon as you commit a file, that is now the canonical formatting, even if it was formatted incorrectly.  By instead specifying `origin/main` or a tag, your CI server will fail unless every changed file is at least as good or better than it was before the change.

This is especially helpful for injecting accurate copyright dates using the [license step](#license-header).

## `spotless:off` and `spotless:on`

Sometimes there is a chunk of code  which you have carefully handcrafted, and you would like to exclude just this one little part from getting clobbered by the autoformat. Some formatters have a way to do this, many don't, but who cares.  If you setup your spotless like this:

```gradle
spotless {
  java { // or kotlin, or c, or python, or whatever
    toggleOffOn()
```

Then whenever Spotless encounters a pair of `spotless:off` / `spotless:on`, it will exclude the code between them from formatting, regardless of all other rules. If you want, you can change the tags to be whatever you want, e.g. `toggleOffOn('fmt:off', 'fmt:on')`. If you decide to change the default, be sure to [read this](https://github.com/diffplug/spotless/pull/691) for some gotchas.

<a name="invisible"></a>

## Line endings and encodings (invisible stuff)

Spotless uses UTF-8 by default, but you can use [any encoding which the JVM supports](https://docs.oracle.com/javase/8/docs/technotes/guides/intl/encoding.doc.html).  You can set it globally, and you can also set it per-format.

```gradle
spotless {
  encoding 'UTF-8' // all formats will be interpreted as UTF-8
  java {
    encoding 'Cp1252' // except java, which will be Cp1252
```

Line endings can also be set globally or per-format using the `lineEndings` property.  Spotless supports four line ending modes: `UNIX`, `WINDOWS`, `MAC_CLASSIC`, `PLATFORM_NATIVE`, and `GIT_ATTRIBUTES`.  The default value is `GIT_ATTRIBUTES`, and *we highly recommend that you* ***do not change*** *this value*.  Git has opinions about line endings, and if Spotless and git disagree, then you're going to have a bad time.

You can easily set the line endings of different files using [a `.gitattributes` file](https://help.github.com/articles/dealing-with-line-endings/).  Here's an example `.gitattributes` which sets all files to unix newlines: `* text eol=lf`.

<a name="custom"></a>
<a name="custom-rulwa"></a>

## Custom steps

As described in the [quickstart](#quickstart), Spotless is just a set of files ("target"), passed through a list of `String -> String` functions.  The string each function gets will always have unix `\n` endings, and Spotless doesn't care which endings the function provides back, it will renormalize regardless.  You can easily make a new step directly in your buildscript, like so:

```gradle
spotless {
  format 'misc', {
    custom 'lowercase', { str -> str.toLowerCase() }
```

However, custom rules will disable up-to-date checking and caching, unless you read [this javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.12.1/com/diffplug/gradle/spotless/FormatExtension.html#bumpThisNumberIfACustomStepChanges-int-) and follow its instructions carefully.

Another option is to create proper `FormatterStep` in your `buildSrc`, and then call [`addStep`](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.12.1/com/diffplug/gradle/spotless/FormatExtension.html#addStep-com.diffplug.spotless.FormatterStep-).  The contributing guide describes [how to do this](https://github.com/diffplug/spotless/blob/main/CONTRIBUTING.md#how-to-add-a-new-formatterstep).  If the step is generally-useful, we hope you'll open a PR to share it!


```gradle
spotless {
  format 'misc', {
    addStep(MyFormatterStep.create())
```

### Throwing errors

Ideally, your formatter will be able to silently fix any problems that it finds, that's the beauty of the `String -> String` model.  But sometimes that's not possible.  If you throw

- `AssertionError` or a subclass -> Spotless reports as a problem in the file being formatted
- anything else -> Spotless reports as a bug in the formatter itself

## Multiple (or custom) language-specific blocks

If you want to have two independent `java` blocks, you can do something like this:

```gradle
spotless { java { ... } }
spotless { format 'javaFoo', com.diffplug.gradle.spotless.JavaExtension, { ... } }
// has to be 'javaFoo' not 'java' because each format needs a unique name
```

That's how the [real `spotless { java {` works anyway](https://github.com/diffplug/spotless/blob/1a19b3f6e92da9ccfb6e4a0024b8fc5de8898ade/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/SpotlessExtensionBase.java#L109-L113). As a follow-on, you can make your own subclass to `FormatExtension` in the `buildSrc` directory, and then use it in your buildscript like so:

```gradle
spotless {
  format 'foo', com.acme.FooLanguageExtension, {
```

If you'd like to create a one-off Spotless task outside of the `check`/`apply` framework, see [`FormatExtension.createIndependentApplyTask`](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.12.1/com/diffplug/gradle/spotless/FormatExtension.html#createIndependentApplyTask-java.lang.String-).

## Inception (languages within languages within...)

In very rare cases, you might want to format e.g. javascript which is written inside JSP templates, or maybe java within a markdown file, or something wacky like that.  You can specify hunks within a file using either open/close tags or a regex with a single capturing group, and then specify rules within it, like so.  See [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.12.1/com/diffplug/gradle/spotless/FormatExtension.html#withinBlocks-java.lang.String-java.lang.String-java.lang.String-org.gradle.api.Action-) for more details.

```gradle
import com.diffplug.gradle.spotless.JavaExtension

spotless {
  format 'templates', {
    target 'src/templates/**/*.foo.html'
    prettier().config(['parser': 'html'])
    withinBlocks 'javascript block', '<script>', '</script>', {
      prettier().config(['parser': 'javascript'])
    }
    withinBlocksRegex 'single-line @(java-expresion)', '@\\((.*?)\\)', JavaExtension, {
      googleJavaFormat()
    }
```

<a name="enforceCheck"></a>

## Disabling warnings and error messages

The `check` task is Gradle's built-in task for grouping all verification tasks - unit tests, static analysis, etc.  By default, `spotlessCheck` is added as a dependency to `check`.

You might want to disable this behavior.  We [recommend against this](https://github.com/diffplug/spotless/issues/79#issuecomment-290844602), but it's easy to do if you'd like:

```gradle
spotless {
  enforceCheck false
}
```

When a misformatted file throws an exception, it will be for one of two reasons:

1) Spotless calculated the properly formatted version, and it is different than the current contents.
2) One of the formatters threw an exception while attempting to calculate the properly formatted version.

You can fix (1) by excluding the file from formatting using the `targetExclude` method, see the [quickstart](#quickstart) section for details.  You can fix (2) and turn these exceptions into warnings like this:

```gradle
spotless {
  java {
    custom 'my-glitchy-step', { ... }

    ignoreErrorForStep('my-glitchy-step')   // ignore errors on all files thrown by a specific step
    ignoreErrorForPath('path/to/file.java') // ignore errors by all steps on this specific file
```

<a name="dependency-resolution-modes"></a>
## Dependency resolution modes

By default, Spotless resolves dependencies on a per-project basis. For very large parallel builds, this can sometimes cause problems. As an alternative, Spotless can be configured to resolve all dependencies in the root project like so:

```gradle
spotless {
  ...
  predeclareDeps()
}
spotlessPredeclare {
  java { eclipse() }
  kotlin { ktfmt('0.28') }
}
```

Alternatively, you can also use `predeclareDepsFromBuildscript()` to resolve the dependencies from the buildscript repositories rather than the project repositories.

If you use this feature, you will get an error if you use a formatter in a subproject which is not declared in the `spotlessPredeclare` block.

<a name="preview"></a>

## How do I preview what `spotlessApply` will do?

- Save your working tree with `git add -A`, then `git commit -m "Checkpoint before spotless."`
- Run `gradlew spotlessApply`
- View the changes with `git diff`
- If you don't like what spotless did, `git reset --hard`
- If you'd like to remove the "checkpoint" commit, `git reset --soft head~1` will make the checkpoint commit "disappear" from history, but keeps the changes in your working directory.

<a name="examples"></a>

## Example configurations (from real-world projects)

* [A few thousand github projects](https://github.com/search?l=gradle&q=spotless&type=Code)
* [JUnit 5](https://github.com/junit-team/junit-lambda/blob/151d52ffab07881de71a8396a9620f18072c65ec/build.gradle#L86-L101) (aka JUnit Lambda)
* [Apache Beam](https://beam.apache.org/) ([direct link to spotless section in its build.gradle](https://github.com/apache/beam/blob/1d9daf1aca101fa5a194cbbba969886734e08902/buildSrc/src/main/groovy/org/apache/beam/gradle/BeamModulePlugin.groovy#L776-L789))
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
