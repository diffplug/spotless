# <img align="left" src="../_images/spotless_logo.png"> Spotless plugin for Gradle
*Keep your code Spotless with Gradle*

<!---freshmark shields
output = [
  link(shield('Gradle plugin', 'plugins.gradle.org', 'com.diffplug.spotless', 'blue'), 'https://plugins.gradle.org/plugin/com.diffplug.spotless'),
  link(shield('Changelog', 'changelog', '{{versionLast}}', 'blue'), 'CHANGES.md'),
  link(shield('MavenCentral', 'mavencentral', 'here', 'blue'), 'https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.diffplug.spotless%22%20AND%20a%3A%22spotless-plugin-gradle%22'),
  link(shield('Javadoc', 'javadoc', 'here', 'blue'), 'https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/{{versionLast}}/index.html'),
  '',
  link(shield('VS Code plugin', 'IDE', 'VS Code', 'blueviolet'), 'https://marketplace.visualstudio.com/items?itemName=richardwillis.vscode-spotless-gradle'),
  link(shield('IntelliJ plugin', 'IDE', 'IntelliJ', 'blueviolet'), 'https://plugins.jetbrains.com/plugin/18321-spotless-gradle'),
  link(shield('Add other IDE', 'IDE', 'add yours', 'blueviolet'), 'IDE_HOOK.md')
  ].join('\n');
-->
[![Gradle plugin](https://img.shields.io/badge/plugins.gradle.org-com.diffplug.spotless-blue.svg)](https://plugins.gradle.org/plugin/com.diffplug.spotless)
[![Changelog](https://img.shields.io/badge/changelog-6.25.0-blue.svg)](CHANGES.md)
[![MavenCentral](https://img.shields.io/badge/mavencentral-here-blue.svg)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.diffplug.spotless%22%20AND%20a%3A%22spotless-plugin-gradle%22)
[![Javadoc](https://img.shields.io/badge/javadoc-here-blue.svg)](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/index.html)

[![VS Code plugin](https://img.shields.io/badge/IDE-VS_Code-blueviolet.svg)](https://marketplace.visualstudio.com/items?itemName=richardwillis.vscode-spotless-gradle)
[![IntelliJ plugin](https://img.shields.io/badge/IDE-IntelliJ-blueviolet.svg)](https://plugins.jetbrains.com/plugin/18321-spotless-gradle)
[![Add other IDE](https://img.shields.io/badge/IDE-add_yours-blueviolet.svg)](IDE_HOOK.md)
<!---freshmark /shields -->

<!---freshmark javadoc
output = prefixDelimiterReplace(input, 'https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/', '/', versionLast)
-->

Spotless is a general-purpose formatting plugin used by [15,000 projects on GitHub (Jan 2023)](https://github.com/search?l=gradle&q=spotless&type=Code).  It is completely à la carte, but also includes powerful "batteries-included" if you opt-in.

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
  - [Java](#java) ([google-java-format](#google-java-format), [eclipse jdt](#eclipse-jdt), [clang-format](#clang-format), [prettier](#prettier), [palantir-java-format](#palantir-java-format), [formatAnnotations](#formatAnnotations), [cleanthat](#cleanthat))
  - [Groovy](#groovy) ([eclipse groovy](#eclipse-groovy))
  - [Kotlin](#kotlin) ([ktfmt](#ktfmt), [ktlint](#ktlint), [diktat](#diktat), [prettier](#prettier))
  - [Scala](#scala) ([scalafmt](#scalafmt))
  - [C/C++](#cc) ([clang-format](#clang-format), [eclipse cdt](#eclipse-cdt))
  - [Protobuf](#protobuf) ([buf](#buf), [clang-format](#clang-format))
  - [Python](#python) ([black](#black))
  - [FreshMark](#freshmark) aka markdown
  - [Flexmark](#flexmark) aka markdown
  - [Antlr4](#antlr4) ([antlr4formatter](#antlr4formatter))
  - [SQL](#sql) ([dbeaver](#dbeaver), [prettier](#prettier))
  - [Maven POM](#maven-pom) ([sortPom](#sortpom))
  - [Typescript](#typescript) ([tsfmt](#tsfmt), [prettier](#prettier), [ESLint](#eslint-typescript), [Biome](#biome))
  - [Javascript](#javascript) ([prettier](#prettier), [ESLint](#eslint-javascript), [Biome](#biome))
  - [JSON](#json) ([simple](#simple), [gson](#gson), [jackson](#jackson), [Biome](#biome), [jsonPatch](#jsonPatch))
  - [YAML](#yaml)
  - [Shell](#shell)
  - [Gherkin](#gherkin)
  - Multiple languages
    - [Prettier](#prettier) ([plugins](#prettier-plugins), [npm detection](#npm-detection), [`.npmrc` detection](#npmrc-detection), [caching `npm install` results](#caching-results-of-npm-install))
      - javascript, jsx, angular, vue, flow, typescript, css, less, scss, html, json, graphql, markdown, ymaml
    - [clang-format](#clang-format)
      - c, c++, c#, objective-c, protobuf, javascript, java
    - [eclipse web tools platform](#eclipse-web-tools-platform)
      - css, html, js, json, xml
    - [Biome](#biome) ([binary detection](#biome-binary), [config file](#biome-configuration-file), [input language](#biome-input-language))
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
    target '*.gradle', '.gitattributes', '.gitignore'

    // define the steps to apply to those files
    trimTrailingWhitespace()
    indentWithTabs() // or spaces. Takes an integer argument if you don't like 4
    endWithNewline()
  }
  java {
    // don't need to set target, it is inferred from java

    // apply a specific flavor of google-java-format
    googleJavaFormat('1.8').aosp().reflowLongStrings().skipJavadocFormatting()
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
- a `target` (the files to format), which you set with [`target`](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/FormatExtension.html#target-java.lang.Object...-) and [`targetExclude`](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/FormatExtension.html#targetExclude-java.lang.Object...-)
- a list of `FormatterStep`, which are just `String -> String` functions, such as [`replace`](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/FormatExtension.html#replace-java.lang.String-java.lang.CharSequence-java.lang.CharSequence-), [`replaceRegex`](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/FormatExtension.html#replaceRegex-java.lang.String-java.lang.String-java.lang.String-), [`trimTrailingWhitespace`](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/FormatExtension.html#replace-java.lang.String-java.lang.CharSequence-java.lang.CharSequence-), [`custom`](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/FormatExtension.html#custom-java.lang.String-groovy.lang.Closure-), [`prettier`](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/FormatExtension.html#prettier--), [`eclipseWtp`](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/FormatExtension.html#eclipseWtp-com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep-), [`licenseHeader`](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/FormatExtension.html#licenseHeader-java.lang.String-java.lang.String-) etc.

All the generic steps live in [`FormatExtension`](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/FormatExtension.html), and there are many language-specific steps which live in its language-specific subclasses, which are described below.

### Requirements

Spotless requires JRE 11+ and Gradle 6.1.1 or newer.

- If you're stuck on JRE 8, use [`id 'com.diffplug.spotless' version '6.13.0'` or older](https://github.com/diffplug/spotless/blob/main/plugin-gradle/CHANGES.md#6130---2023-01-14).
- If you're stuck on an older version of Gradle, [`id 'com.diffplug.gradle.spotless' version '4.5.1'` supports all the way back to Gradle 2.x](https://github.com/diffplug/spotless/blob/main/plugin-gradle/CHANGES.md#451---2020-07-04).

<a name="applying-to-java-source"></a>

## Java

`com.diffplug.gradle.spotless.JavaExtension` [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/JavaExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/JavaExtension.java)

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

    // Cleanthat will refactor your code, but it may break your style: apply it before your formatter
    cleanthat()          // has its own section below

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

### removeUnusedImports

```
spotless {
  java {
  removeUnusedImports()
  // optional: you may switch for `google-java-format` as underlying engine to `cleanthat-javaparser-unnecessaryimport`
  // which enables processing any language level source file with a JDK8+ Runtime
  removeUnusedImports('cleanthat-javaparser-unnecessaryimport')
```

### google-java-format

[homepage](https://github.com/google/google-java-format). [changelog](https://github.com/google/google-java-format/releases).
```gradle
spotless {
  java {
    googleJavaFormat()
    // optional: you can specify a specific version (>= 1.8) and/or switch to AOSP style
    //   and/or reflow long strings
    //   and/or use custom group artifact (you probably don't need this)
    googleJavaFormat('1.8').aosp().reflowLongStrings().formatJavadoc(false).reorderImports(false).groupArtifact('com.google.googlejavaformat:google-java-format')
```

### palantir-java-format

[homepage](https://github.com/palantir/palantir-java-format). [changelog](https://github.com/palantir/palantir-java-format/releases).
```gradle
spotless {
  java {
    palantirJavaFormat()
    // optional: you can specify a specific version and/or switch to AOSP/GOOGLE style
    palantirJavaFormat('2.9.0').style("GOOGLE")
    // optional: you can also format Javadocs, requires at least Palantir 2.39.0
    palantirJavaFormat('2.39.0').formatJavadoc(true)
```

### eclipse jdt

[homepage](https://www.eclipse.org/downloads/packages/). [compatible versions](https://github.com/diffplug/spotless/tree/main/lib-extra/src/main/resources/com/diffplug/spotless/extra/eclipse_jdt_formatter). See [here](../ECLIPSE_SCREENSHOTS.md) for screenshots that demonstrate how to get and install the config file mentioned below.

```gradle
spotless {
  java {
    eclipse()
    // optional: you can specify a specific version and/or config file
    eclipse('4.26').configFile('eclipse-prefs.xml')
    // if the access to the p2 repositories is restricted, mirrors can be
    // specified using a URI prefix map as follows:
    echlise().withP2Mirrors(['https://download.eclipse.org/eclipse/updates/4.29/':'https://some.internal.mirror/4-29-updates-p2/'])
       
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

### cleanthat

[homepage](https://github.com/solven-eu/cleanthat). CleanThat enables automatic refactoring of Java code. [ChangeLog](https://github.com/solven-eu/cleanthat/blob/master/CHANGES.MD)

```gradle
spotless {
  java {
    cleanthat()
    // optional: you can specify a specific version and/or config file
    cleanthat()
      .groupArtifact('io.github.solven-eu.cleanthat:java') // Optional. Default is 'io.github.solven-eu.cleanthat:java'
      .version('2.8')                                      // You may force a custom version of Cleanthat
      .sourceCompatibility('1.7')                          // default is '1.7'
      .addMutator('SafeAndConsensual')                     // Default includes the SafeAndConsensual composite mutator
      .addMutator('your.custom.MagicMutator')              // List of mutators: https://github.com/solven-eu/cleanthat/blob/master/MUTATORS.generated.MD
      .excludeMutator('UseCollectionIsEmpty')              // You may exclude some mutators (from Composite ones)
      .includeDraft(false)                                 // You may exclude draft mutators (from Composite ones)
```


<a name="applying-to-groovy-source"></a>

## Groovy

- `com.diffplug.gradle.spotless.GroovyExtension` [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/GroovyExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/GroovyExtension.java)
- `com.diffplug.gradle.spotless.GroovyGradleExtension` [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/GroovyGradleExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/GroovyGradleExtension.java)

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
    // removes semicolons at the end of lines
    removeSemicolons()
    // the Groovy Eclipse formatter extends the Java Eclipse formatter,
    // so it formats Java files by default (unless `excludeJava` is used).
    greclipse() // has its own section below
    
    licenseHeader('/* (C) $YEAR */') // or licenseHeaderFile

    //---- Below is for `groovy` only ----
    
    // excludes all Java sources within the Groovy source dirs from formatting
    excludeJava()
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
    // optional: you can specify a specific version or config file(s), version matches the Eclipse Platform
    greclipse('4.26').configFile('spotless.eclipseformat.xml', 'org.codehaus.groovy.eclipse.ui.prefs')
```

Groovy-Eclipse formatting errors/warnings lead per default to a build failure. This behavior can be changed by adding the property/key value `ignoreFormatterProblems=true` to a configuration file. In this scenario, files causing problems, will not be modified by this formatter step.

## Kotlin

- `com.diffplug.gradle.spotless.KotlinExtension` [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/KotlinExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/KotlinExtension.java)
- `com.diffplug.gradle.spotless.KotlinGradleExtension` [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/KotlinGradleExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/KotlinGradleExtension.java)

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
    // version, editorConfigPath, editorConfigOverride and customRuleSets are all optional
    ktlint("1.0.0")
      .setEditorConfigPath("$projectDir/config/.editorconfig")  // sample unusual placement
      .editorConfigOverride(
        mapOf(
          "indent_size" to 2,
          // intellij_idea is the default style we preset in Spotless, you can override it referring to https://pinterest.github.io/ktlint/latest/rules/code-styles.
          "ktlint_code_style" to "intellij_idea",
        )
      )
      .customRuleSets(
        listOf(
          "io.nlopez.compose.rules:ktlint:0.3.3"
        )
      )
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

`com.diffplug.gradle.spotless.ScalaExtension` [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/ScalaExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/ScalaExtension.java)

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
    // version and configFile, scalaMajorVersion are all optional
    scalafmt('3.5.9').configFile('scalafmt.conf').scalaMajorVersion('2.13')
```

<a name="applying-to-cc-sources"></a>

## C/C++

`com.diffplug.gradle.spotless.CppExtension` [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/CppExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/CppExtension.java)

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

`com.diffplug.gradle.spotless.PythonExtension` [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/PythonExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/PythonExtension.java)

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

## Protobuf

### buf

`com.diffplug.gradle.spotless.ProtobufExtension` [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/ProtobufExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/ProtobufExtension.java)

**WARNING** this step **must** be the first step in the chain, steps before it will be ignored. Thumbs up [this issue](https://github.com/bufbuild/buf/issues/1035) for a resolution, see [here](https://github.com/diffplug/spotless/pull/1208#discussion_r1264439669) for more details on the problem.

```gradle
spotless {
  protobuf {
    // by default the target is every '.proto' file in the project
    buf()

    licenseHeader '/* (C) $YEAR */' // or licenseHeaderFile
  }
}
```

When used in conjunction with the [buf-gradle-plugin](https://github.com/bufbuild/buf-gradle-plugin), the `buf` executable can be resolved from its `bufTool` configuration:

```gradle
spotless {
  protobuf {
    buf().pathToExe(configurations.getByName(BUF_BINARY_CONFIGURATION_NAME).getSingleFile().getAbsolutePath())
  }
}

// Be sure to disable the buf-gradle-plugin's execution of `buf format`:
buf {
  enforceFormat = false
}
```

## FreshMark

`com.diffplug.gradle.spotless.FreshMarkExtension` [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/FreshMarkExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/FreshMarkExtension.java)

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

## Flexmark

`com.diffplug.gradle.spotless.FlexmarkExtension` [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/FlexmarkExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/FlexmarkExtension.java)

[homepage](https://github.com/vsch/flexmark-java). Flexmark is a flexible Commonmark/Markdown parser that can be used to format Markdown files. It supports different [flavors of Markdown](https://github.com/vsch/flexmark-java#markdown-processor-emulation) and [many formatting options](https://github.com/vsch/flexmark-java/wiki/Markdown-Formatter#options).

Currently, none of the available options can be configured yet. It uses only the default options together with `COMMONMARK` as `FORMATTER_EMULATION_PROFILE`.

To apply flexmark to all of the `.md` files in your project, use this snippet:

```gradle
spotless {
  flexmark {
    target '**/*.md' // you have to set the target manually
    flexmark() // or flexmark('0.64.8') // version is optional
  }
}
```

## Antlr4

`com.diffplug.gradle.spotless.Antlr4Extension` [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/Antlr4Extension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/Antlr4Extension.java)

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

`com.diffplug.gradle.spotless.SqlExtension` [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/SqlExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/SqlExtension.java)

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

## Maven POM

`com.diffplug.gradle.spotless.PomExtension` [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/PomExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/PomExtension.java)

```gradle
spotless {
  pom {
    target('pom.xml') // default value, you can change if you want

    sortPom()  // has its own section below
  }
}
```

### sortPom

[homepage](https://github.com/Ekryd/sortpom).

All configuration settings are optional, they are described in detail [here](https://github.com/Ekryd/sortpom/wiki/Parameters).

```gradle
spotless {
  pom {
    sortPom('4.0.0')
      .encoding('UTF-8') // The encoding of the pom files
      .lineSeparator(System.getProperty('line.separator')) // line separator to use
      .expandEmptyElements(true) // Should empty elements be expanded
      .spaceBeforeCloseEmptyElement(false) // Should a space be added inside self-closing elements
      .keepBlankLines(true) // Keep empty lines
      .endWithNewline(true) // Whether sorted pom ends with a newline
      .nrOfIndentSpace(2) // Indentation
      .indentBlankLines(false) // Should empty lines be indented
      .indentSchemaLocation(false) // Should schema locations be indented
      .indentAttribute(null) // Should the xml attributes be indented
      .predefinedSortOrder('recommended_2008_06') // Sort order of elements: https://github.com/Ekryd/sortpom/wiki/PredefinedSortOrderProfiles
      .sortOrderFile(null) // Custom sort order of elements: https://raw.githubusercontent.com/Ekryd/sortpom/master/sorter/src/main/resources/custom_1.xml
      .sortDependencies(null) // Sort dependencies: https://github.com/Ekryd/sortpom/wiki/SortDependencies
      .sortDependencyManagement(null) // Sort dependency management: https://github.com/Ekryd/sortpom/wiki/SortDependencies
      .sortDependencyExclusions(null) // Sort dependency exclusions: https://github.com/Ekryd/sortpom/wiki/SortDependencies
      .sortPlugins(null) // Sort plugins: https://github.com/Ekryd/sortpom/wiki/SortPlugins
      .sortProperties(false) // Sort properties
      .sortModules(false) // Sort modules
      .sortExecutions(false) // Sort plugin executions
  }
}
```

<a name="applying-to-typescript-source"></a>

## Typescript

- `com.diffplug.gradle.spotless.TypescriptExtension` [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/TypescriptExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/TypescriptExtension.java)

```gradle
spotless {
  typescript {
    target 'src/**/*.ts' // you have to set the target manually

    tsfmt()    // has its own section below
    prettier() // has its own section below
    eslint()   // has its own section below
    biome()    // has its own section below

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

For details, see the [npm detection](#npm-detection), [`.npmrc` detection](#npmrc-detection) and [caching results of `npm install`](#caching-results-of-npm-install) sections of prettier, which apply also to tsfmt.


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

For details, see the [npm detection](#npm-detection), [`.npmrc` detection](#npmrc-detection) and [caching results of `npm install`](#caching-results-of-npm-install) sections of prettier, which apply also to ESLint.

## Javascript

- `com.diffplug.gradle.spotless.JavascriptExtension` [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/JavascriptExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/JavascriptExtension.java)

```gradle
spotless {
  javascript {
    target 'src/**/*.js' // you have to set the target manually

    prettier() // has its own section below
    eslint()   // has its own section below
    biome()    // has its own section below

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

For details, see the [npm detection](#npm-detection), [`.npmrc` detection](#npmrc-detection) and [caching results of `npm install`](#caching-results-of-npm-install) sections of prettier, which apply also to ESLint.

## JSON

- `com.diffplug.gradle.spotless.JsonExtension` [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/JsonExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/JsonExtension.java)

```gradle
spotless {
  json {
    target 'src/**/*.json'                // you have to set the target manually
    simple()                              // has its own section below
    prettier().config(['parser': 'json']) // see Prettier section below
    eclipseWtp('json')                    // see Eclipse web tools platform section
    gson()                                // has its own section below
    jackson()                             // has its own section below
    biome()                               // has its own section below
    jsonPatch([])                         // has its own section below
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

### Jackson

Uses Jackson for json files.

```gradle
spotless {
  json {
    target 'src/**/*.json'
    jackson()
      .spaceBeforeSeparator(false)                   // optional: add a whitespace before key separator. False by default
      .feature('INDENT_OUTPUT', true)                // optional: true by default
      .feature('ORDER_MAP_ENTRIES_BY_KEYS', true)    // optional: false by default
      .feature('ANY_OTHER_FEATURE', true|false)      // optional: any SerializationFeature can be toggled on or off
      .jsonFeature('ANY_OTHER_FEATURE', true|false)  // any JsonGenerator.Feature can be toggled on or off
  }
}
```

### jsonPatch

Uses [zjsonpatch](https://github.com/flipkart-incubator/zjsonpatch) to apply [JSON Patches](https://jsonpatch.com/) as per [RFC 6902](https://datatracker.ietf.org/doc/html/rfc6902/) to JSON documents.

This enables you to add, replace or remove properties at locations in the JSON document that you specify using [JSON Pointers](https://datatracker.ietf.org/doc/html/rfc6901/).

In Spotless Gradle, these JSON patches are represented as a `List<Map<String, Object>>`, or a list of patch operations.

Each patch operation must be a map with the following properties:

* `"op"` - the operation to apply, one of `"replace"`, `"add"` or `"remove"`.
* `"path"` - a JSON Pointer string, for example `"/foo"`
* `"value"` - the value to `"add"` or `"replace"` at the specified path. Not needed for `"remove"` operations.

For example, to apply the patch from the [JSON Patch homepage](https://jsonpatch.com/#the-patch):

```gradle
spotless {
  json {
    target 'src/**/*.json'
    jsonPatch([
      [op: 'replace', path: '/baz', value: 'boo'],
      [op: 'add', path: '/hello', value: ['world']],
      [op: 'remove', path: '/foo']
    ])
  }
}
```

Or using the Kotlin DSL:

```kotlin
spotless {
  json {
    target("src/**/*.json")
    jsonPatch(listOf(
      mapOf("op" to "replace", "path" to "/baz", "value" to "boo"),
      mapOf("op" to "add", "path" to "/hello", "value" to listOf("world")),
      mapOf("op" to "remove", "path" to "/foo")
    ))
  }
}
```

## YAML

- `com.diffplug.gradle.spotless.YamlExtension` [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/JsonExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/YamlExtension.java)

```gradle
spotless {
  yaml {
    target 'src/**/*.yaml'                // you have to set the target manually
    jackson()                             // has its own section below
    prettier()                            // has its own section below
  }
}
```

### Jackson

Uses Jackson for `yaml` files.

```gradle
spotless {
  yaml {
    target 'src/**/*.yaml'
    jackson()
      .spaceBeforeSeparator(false)                   // optional: add a whitespace before key separator. False by default
      .feature('INDENT_OUTPUT', true)                // optional: true by default
      .feature('ORDER_MAP_ENTRIES_BY_KEYS', true)    // optional: false by default
      .feature('ANY_OTHER_FEATURE', true|false)      // optional: any SerializationFeature can be toggled on or off
      .yamlFeature('ANY_OTHER_FEATURE', true|false)  // any YAMLGenerator.Feature can be toggled on or off
  }
}
```

## Shell

`com.diffplug.gradle.spotless.ShellExtension` [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/ShellExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/ShellExtension.java)

```gradle
spotless {
  shell {
    target 'scripts/**/*.sh' // default: '**/*.sh'

    shfmt()  // has its own section below
  }
}
```

### shfmt

[homepage](https://github.com/mvdan/sh). [changelog](https://github.com/mvdan/sh/blob/master/CHANGELOG.md).

When formatting shell scripts via `shfmt`, configure `shfmt` settings via `.editorconfig`. 
Refer to the `shfmt` [man page](https://github.com/mvdan/sh/blob/master/cmd/shfmt/shfmt.1.scd) for `.editorconfig` settings. 

```gradle
shfmt('3.8.0') // version is optional

// if shfmt is not on your path, you must specify its location manually
shfmt().pathToExe('/opt/homebrew/bin/shfmt')

// Spotless always checks the version of the shfmt it is using
// and will fail with an error if it does not match the expected version
// (whether manually specified or default). If there is a problem, Spotless
// will suggest commands to help install the correct version.
//   TODO: handle installation & packaging automatically - https://github.com/diffplug/spotless/issues/674
```

<a name="applying-freshmark-to-markdown-files"></a>

## Gherkin

- `com.diffplug.gradle.spotless.GherkinExtension` [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/GherkinExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/GherkinExtension.java)

```gradle
spotless {
  gherkin {
    target 'src/**/*.feature' // you have to set the target manually
    gherkinUtils() // has its own section below
  }
}
```

### gherkinUtils

[homepage](https://github.com/cucumber/gherkin-utils). [changelog](https://github.com/cucumber/gherkin-utils/blob/main/CHANGELOG.md).

Uses a Gherkin pretty-printer that optionally allows configuring the number of spaces that are used to pretty print objects:

```gradle
spotless {
  gherkin {
    target 'src/**/*.feature'     // required to be set explicitly
    gherkinUtils()
      .version('8.0.2')           // optional: custom version of 'io.cucumber:gherkin-utils'
  }
}
```

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

#### prettier version below 3

```gradle
spotless {
  java {
    prettier(['prettier': '2.8.8', 'prettier-plugin-java': '2.2.0']).config(['parser': 'java', 'tabWidth': 4])
  }
  format 'php', {
    target 'src/**/*.php'
    prettier(['prettier': '2.8.8', '@prettier/plugin-php': '0.19.6']).config(['parser': 'php', 'tabWidth': 3])
  }
}
```

#### prettier version 3+

With version 3 prettier it is required to pass in an additional 'plugins' parameter to the config block with a list of plugins you want to use.

```gradle
spotless {
  java {
    prettier(['prettier': '3.0.3', 'prettier-plugin-java': '2.3.0'])
      .config(['parser': 'java', 'tabWidth': 4, 'plugins': ['prettier-plugin-java']])
  }
  format 'php', {
    target 'src/**/*.php'
    prettier(['prettier': '3.0.3', '@prettier/plugin-php': '0.20.1'])
      .config(['parser': 'php', 'tabWidth': 3, 'plugins': ['@prettier/plugin-php']])
  }
}
```

### npm detection

Prettier is based on NodeJS, so a working NodeJS installation (especially npm) is required on the host running spotless.
Spotless will try to auto-discover an npm installation. If that is not working for you, it is possible to directly configure the npm
and/or node binary to use.

```gradle
spotless {
  format 'javascript', {
    prettier().npmExecutable('/usr/bin/npm').nodeExecutable('/usr/bin/node').config(...)
```

If you provide both `npmExecutable` and `nodeExecutable`, spotless will use these paths. If you specify only one of the
two, spotless will assume the other one is in the same directory.

If you use the `gradle-node-plugin` ([github](https://github.com/node-gradle/gradle-node-plugin)), it is possible to use the
node- and npm-binaries dynamically installed by this plugin. See
[this](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/test/resources/com/diffplug/gradle/spotless/NpmTestsWithoutNpmInstallationTest_gradle_node_plugin_example_1.gradle)
or [this](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/test/resources/com/diffplug/gradle/spotless/NpmTestsWithoutNpmInstallationTest_gradle_node_plugin_example_2.gradle) example.

### `.npmrc` detection

Spotless picks up npm configuration stored in a `.npmrc` file either in the project directory or in your user home.
Alternatively you can supply spotless with a location of the `.npmrc` file to use. (This can be combined with
`npmExecutable` and `nodeExecutable`, of course.)

```gradle
spotless {
  typescript {
    prettier().npmrc("$projectDir/config/.npmrc").config(...)
```

### Caching results of `npm install`

Spotless uses `npm` behind the scenes to install `prettier`. This can be a slow process, especially if you are using a slow internet connection or
if you need large plugins. You can instruct spotless to cache the results of the `npm install` calls, so that for the next installation,
it will not need to download the packages again, but instead reuse the cached version.

```gradle
spotless {
  typescript {
    prettier().npmInstallCache() // will use the default cache directory (the build-directory of the respective module)
    prettier().npmInstallCache("${rootProject.rootDir}/.gradle/spotless-npm-cache") // will use the specified directory (creating it if not existing)
```

Depending on your filesystem and the location of the cache directory, spotless will use hardlinks when caching the npm packages. If that is not
possible, it will fall back to copying the files.

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
    eclipseWtp('xml', '11.0') // optional version, others at https://download.eclipse.org/tools/cdt/releases/
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

## Biome

[homepage](https://biomejs.dev/). [changelog](https://github.com/biomejs/biome/blob/main/CHANGELOG.md). Biome is
a formatter that for the frontend written in Rust, which has a native binary, does not require Node.js and as such,
is pretty fast. It can currently format JavaScript, TypeScript, JSX, and JSON, and may support
[more frontend languages](https://biomejs.dev/internals/language-support/) such as CSS in the future.

You can use Biome in any language-specific format for supported languages, but
usually you will be creating a generic format.

```gradle
spotless {
  format 'styling', {
    // you have to set the target manually
    target 'src/*/webapp/**/*.js'

    // Download Biome from the network if not already downloaded, see below for more info
    biome('1.2.0')

    // (optional) Path to the directory with the biome.json conig file
    biome('1.2.0').configPath("path/config/dir")

    // (optional) Biome will auto detect the language based on the file extension.
    // See below for possible values.
    biome('1.2.0').language("js")
  }
}
```

To apply Biome to more kinds of files with a different configuration, just add
more formats:

```gradle
spotless {
  format 'biome-js', {
    target '**/*.js'
    biome('1.2.0')
  }
  format 'biome-ts', {
    target '**/*.ts'
    biome('1.2.0')
  }
  format 'biome-json', {
    target '**/*.json'
    biome('1.2.0')
  }
}
```

**Limitations:**

- The auto-discovery of config files (up the file tree) will not work when using
  Biome within spotless.
- The `ignore` option of the `biome.json` configuration file will not be applied.
  Include and exclude patterns are configured in the spotless configuration in the
  Gradle settings file instead.

Note: Due to a limitation of biome, if the name of a file matches a pattern in
the `ignore` option of the specified `biome.json` configuration file, it will not be
formatted, even if included in the biome configuration section of the Gradle settings
file.
You could specify a different `biome.json` configuration file without an `ignore`
pattern to circumvent this.

Note 2: Biome is hard-coded to ignore certain special files, such as `package.json`
or `tsconfig.json`. These files will never be formatted.

### Biome binary

To format with Biome, spotless needs to find the Biome binary. By default,
spotless downloads the binary for the given version from the network. This
should be fine in most cases, but may not work e.g. when there is not connection
to the internet.

To download the Biome binary from the network, just specify a version:

```gradle
spotless {
  format 'biome', {
    target '**/*.js','**/*.ts','**/*.json'
    biome('1.2.0')
  }
}
```

Spotless uses a default version when you do not specify a version, but this
may change at any time, so we recommend that you always set the Biome version
you want to use. Optionally, you can also specify a directory for the downloaded
Biome binaries (defaults to `~/.m2/repository/com/diffplug/spotless/spotless-data/biome`):

```gradle
spotless {
  format 'biome', {
    target '**/*.js','**/*.ts','**/*.json'
     // Relative paths are resolved against the project's base directory
    biome('1.2.0').downloadDir("${project.gradle.gradleUserHomeDir}/biome")
  }
}
```

To use a fixed binary, omit the `version` and specify a `pathToExe`:

```gradle
spotless {
  format 'biome', {
    target '**/*.js','**/*.ts','**/*.json'
    biome().pathToExe("${project.layout.buildDirectory.asFile.get().absolutePath}/bin/biome")
  }
}
```

Absolute paths are used as-is. Relative paths are resolved against the project's
base directory. To use a pre-installed Biome binary on the user's path, specify
just a name without any slashes / backslashes:

```gradle
spotless {
  format 'biome', {
    target '**/*.js','**/*.ts','**/*.json'
    // Uses the "biome" command, which must be on the user's path. -->
    biome().pathToExe('biome')
  }
}
```

### Biome configuration file

Biome is a biased formatter and linter without many options, but there are a few
basic options. Biome uses a file named [biome.json](https://biomejs.dev/reference/configuration/)
for its configuration. When none is specified, the default configuration from
Biome is used. To use a custom configuration:

```gradle
spotless {
  format 'biome', {
    target '**/*.js','**/*.ts','**/*.json'
    // Must point to the directory with the "biome.json" config file -->
    // Relative paths are resolved against the project's base directory -->
    biome('1.2.0').configPath('./config')
  }
}
```

### Biome input language

By default, Biome detects the language / syntax of the files to format
automatically from the file extension. This may fail if your source code files
have unusual extensions for some reason. If you are using the generic format,
you can force a certain language like this:

```xml
spotless {
  format 'biome', {
    target 'src/**/typescript/**/*.mjson'
    biome('1.2.0').language('json')
  }
}
```

The following languages are currently recognized:

* `js` -- JavaScript
* `jsx` -- JavaScript + JSX (React)
* `js?` -- JavaScript, with or without JSX, depending on the file extension
* `ts` -- TypeScript
* `tsx` -- TypeScript + JSX (React)
* `ts?` -- TypeScript, with or without JSX, depending on the file extension
* `json` -- JSON

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

See the [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/FormatExtension.LicenseHeaderConfig.html) for a complete listing of options.

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

### Using `ratchetFrom` on CI systems

Many popular CI systems (GitHub, GitLab, BitBucket, and Travis) use a "shallow clone". This means that `ratchetFrom 'origin/main'` will fail with `No such reference`. You can fix this by:

- calling `git fetch origin main` before you call Spotless
- disabling the shallow clone [like so](https://github.com/diffplug/spotless/issues/710)

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

Line endings can also be set globally or per-format using the `lineEndings` property.  Spotless supports four line ending modes: `UNIX`, `WINDOWS`, `MAC_CLASSIC`, `PLATFORM_NATIVE`, `GIT_ATTRIBUTES`, and `GIT_ATTRIBUTES_FAST_ALLSAME`.  The default value is `GIT_ATTRIBUTES_FAST_ALLSAME`, and *we highly recommend that you* ***do not change*** *this value*.  Git has opinions about line endings, and if Spotless and git disagree, then you're going to have a bad time. `FAST_ALLSAME` just means that Spotless can assume that every file being formatted has the same line endings ([more info](https://github.com/diffplug/spotless/pull/1838)).

You can easily set the line endings of different files using [a `.gitattributes` file](https://help.github.com/articles/dealing-with-line-endings/).  Here's an example `.gitattributes` which sets all files to unix newlines: `* text eol=lf`.

<a name="custom"></a>
<a name="custom-steps"></a>

## Custom steps

As described in the [quickstart](#quickstart), Spotless is just a set of files ("target"), passed through a list of `String -> String` functions.  The string each function gets will always have unix `\n` endings, and Spotless doesn't care which endings the function provides back, it will renormalize regardless.  You can easily make a new step directly in your buildscript, like so:

```gradle
spotless {
  format 'misc', {
    custom 'lowercase', { str -> str.toLowerCase() }
```

However, custom rules will disable up-to-date checking and caching, unless you read [this javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/FormatExtension.html#bumpThisNumberIfACustomStepChanges-int-) and follow its instructions carefully.

Another option is to create proper `FormatterStep` in your `buildSrc`, and then call [`addStep`](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/FormatExtension.html#addStep-com.diffplug.spotless.FormatterStep-).  The contributing guide describes [how to do this](https://github.com/diffplug/spotless/blob/main/CONTRIBUTING.md#how-to-add-a-new-formatterstep).  If the step is generally-useful, we hope you'll open a PR to share it!


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

If you'd like to create a one-off Spotless task outside of the `check`/`apply` framework, see [`FormatExtension.createIndependentApplyTask`](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/FormatExtension.html#createIndependentApplyTask-java.lang.String-).

## Inception (languages within languages within...)

In very rare cases, you might want to format e.g. javascript which is written inside JSP templates, or maybe java within a markdown file, or something wacky like that.  You can specify hunks within a file using either open/close tags or a regex with a single capturing group, and then specify rules within it, like so.  See [javadoc](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/6.25.0/com/diffplug/gradle/spotless/FormatExtension.html#withinBlocks-java.lang.String-java.lang.String-java.lang.String-org.gradle.api.Action-) for more details.

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
