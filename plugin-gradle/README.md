# <img align="left" src="../_images/spotless_logo.png"> Spotless plugin for Gradle
*Keep your code Spotless with Gradle*

<!---freshmark shields
output = [
  link(shield('Gradle plugin', 'plugins.gradle.org', 'com.diffplug.gradle.spotless', 'blue'), 'https://plugins.gradle.org/plugin/com.diffplug.gradle.spotless'),
  link(shield('Maven central', 'mavencentral', 'yes', 'blue'), 'https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.diffplug.spotless%22%20AND%20a%3A%22spotless-plugin-gradle%22'),
  link(shield('Javadoc', 'javadoc', 'yes', 'blue'), 'https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/{{versionLast}}/index.html'),
  link(shield('License Apache', 'license', 'apache', 'blue'), 'https://tldrlegal.com/license/apache-license-2.0-(apache-2.0)'),
  link(shield('Changelog', 'changelog', '{{versionLast}}', 'blue'), 'CHANGES.md'),
  '',
  link(image('Circle CI', 'https://circleci.com/gh/diffplug/spotless/tree/main.svg?style=shield'), 'https://circleci.com/gh/diffplug/spotless/tree/main'),
  link(shield('Live chat', 'gitter', 'chat', 'brightgreen'), 'https://gitter.im/{{org}}/{{name}}'),
  link(shield('VS Code plugin Apache', 'IDE', 'VS Code', 'blueviolet'), 'https://marketplace.visualstudio.com/items?itemName=richardwillis.vscode-spotless-gradle'),
  link(shield('VS Code plugin Apache', 'IDE', 'add yours', 'blueviolet'), 'IDE_HOOK.md')
  ].join('\n');
-->
[![Gradle plugin](https://img.shields.io/badge/plugins.gradle.org-com.diffplug.gradle.spotless-blue.svg)](https://plugins.gradle.org/plugin/com.diffplug.gradle.spotless)
[![Maven central](https://img.shields.io/badge/mavencentral-yes-blue.svg)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.diffplug.spotless%22%20AND%20a%3A%22spotless-plugin-gradle%22)
[![Javadoc](https://img.shields.io/badge/javadoc-yes-blue.svg)](https://javadoc.io/doc/com.diffplug.spotless/spotless-plugin-gradle/4.4.0/index.html)
[![License Apache](https://img.shields.io/badge/license-apache-blue.svg)](https://tldrlegal.com/license/apache-license-2.0-(apache-2.0))
[![Changelog](https://img.shields.io/badge/changelog-4.4.0-blue.svg)](CHANGES.md)

[![Circle CI](https://circleci.com/gh/diffplug/spotless/tree/main.svg?style=shield)](https://circleci.com/gh/diffplug/spotless/tree/main)
[![Live chat](https://img.shields.io/badge/gitter-chat-brightgreen.svg)](https://gitter.im/diffplug/spotless)
[![VS Code plugin Apache](https://img.shields.io/badge/IDE-VS_Code-blueviolet.svg)](https://marketplace.visualstudio.com/items?itemName=richardwillis.vscode-spotless-gradle)
[![VS Code plugin Apache](https://img.shields.io/badge/IDE-add_yours-blueviolet.svg)](IDE_HOOK.md)
<!---freshmark /shields -->

<!---freshmark javadoc
output = prefixDelimiterReplace(input, 'https://javadoc.io/static/com.diffplug.spotless/spotless-plugin-gradle/', '/', versionLast)
-->

Spotless is a general-purpose formatting plugin used by [3,500 projects on GitHub (May 2020)](https://github.com/search?l=gradle&q=spotless&type=Code).  It is completely à la carte, but also includes powerful "batteries-included" if you opt-in.

To people who use your build, it looks like this ([IDE support also available]()):

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

### Table of Contents

- [**Quickstart**](#quickstart)
  - [Requirements](#requirements)
- **Languages**
  - [Java](#java) ([android](#android), [google-java-format](#google-java-format), [eclipse jdt](#eclipse-jdt), [prettier](#prettier))
  - [Groovy](#groovy) ([eclipse groovy](#eclipse-groovy))
  - [Kotlin](#kotlin) ([ktlint](#ktlint), [ktfmt](#ktmt), [prettier](#prettier))
  - [Scala](#scala) ([scalafmt](https://github.com/scalameta/scalafmt/releases))
  - [C/C++](#c-c++) ([eclipse cdt](https://www.eclipse.org/cdt/))
  - Markdown ([freshmark](#freshmark))
  - [SQL](#SQL) ([dbeaver](#dbeaver), [prettier](#prettier))
  - [Typescript](#typescript) ([tsfmt](#tsfmt), [prettier](#prettier))
  - Multiple filetypes
    - [Prettier](#prettier) (and [plugins](#prettier-plugins))
    - [eclipse web tools platform](#eclipse-web-tools-platform)
- **Platform**
  - [License header](#license-header) ([slurp year from git](#retroactively-slurp-years-from-git-history))
  - [How can I enforce formatting gradually? (aka "ratchet")](#ratchet)
  - [Custom rules](#custom-rules)
  - [Line endings and encodings (invisible stuff)](#line-endings-and-encodings-invisible-stuff)
  - [Disabling warnings and error messages](#disabling-warnings-and-error-messages)
  - [How do I preview what `spotlessApply` will do?](#how-do-i-preview-what-spotlessapply-will-do)
  - [Example configurations (from real-world projects)](#example-configurations-from-real-world-projects)

***Contributions are welcome, see [the contributing guide](../CONTRIBUTING.md) for development info.***

## Quickstart

To use it in your buildscript, just [add the Spotless dependency](https://plugins.gradle.org/plugin/com.diffplug.gradle.spotless), and configure it like so:

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
    googleJavaFormat('1.8').aosp()
    // make sure every file has the following copyright header.
    // optionally, Spotless can set copyright years by digging
    // through git history (see "license" section below)
    licenseHeader '/* (C)$YEAR */'
  }
}
```

Spotless consists of a list of formats (in the example above, `misc` and `java`), and each format has:
- a `target` (the files to format), which you set with [`target`](https://javadoc.io/static/com.diffplug.spotless/spotless-plugin-gradle/4.4.0/com/diffplug/gradle/spotless/FormatExtension.html#target-java.lang.Object...-) and [`targetExclude`](https://javadoc.io/static/com.diffplug.spotless/spotless-plugin-gradle/4.4.0/com/diffplug/gradle/spotless/FormatExtension.html#targetExclude-java.lang.Object...-)
- a list of `FormatterStep`, which are just `String -> String` functions, such as [`replace`](https://javadoc.io/static/com.diffplug.spotless/spotless-plugin-gradle/4.4.0/com/diffplug/gradle/spotless/FormatExtension.html#replace-java.lang.String-java.lang.CharSequence-java.lang.CharSequence-), [`replaceRegex`](https://javadoc.io/static/com.diffplug.spotless/spotless-plugin-gradle/4.4.0/com/diffplug/gradle/spotless/FormatExtension.html#replaceRegex-java.lang.String-java.lang.String-java.lang.String-), [`trimTrailingWhitespace`](https://javadoc.io/static/com.diffplug.spotless/spotless-plugin-gradle/4.4.0/com/diffplug/gradle/spotless/FormatExtension.html#replace-java.lang.String-java.lang.CharSequence-java.lang.CharSequence-), [`custom`](https://javadoc.io/static/com.diffplug.spotless/spotless-plugin-gradle/4.4.0/com/diffplug/gradle/spotless/FormatExtension.html#custom-java.lang.String-groovy.lang.Closure-), [`prettier`](https://javadoc.io/static/com.diffplug.spotless/spotless-plugin-gradle/4.4.0/com/diffplug/gradle/spotless/FormatExtension.html#prettier--), [`eclipseWtp`](https://javadoc.io/static/com.diffplug.spotless/spotless-plugin-gradle/4.4.0/com/diffplug/gradle/spotless/FormatExtension.html#eclipseWtp-com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep-), [`licenseHeader`](https://javadoc.io/static/com.diffplug.spotless/spotless-plugin-gradle/4.4.0/com/diffplug/gradle/spotless/FormatExtension.html#licenseHeader-java.lang.String-java.lang.String-) etc.

All the generic steps live in [`FormatExtension`](https://javadoc.io/static/com.diffplug.spotless/spotless-plugin-gradle/4.4.0/com/diffplug/gradle/spotless/FormatExtension.html), and there are many language-specific steps which live in its language-specific subclasses, which are described below.

Spotless supports all of Gradle's built-in performance features (incremental build, buildcache, lazy configuration, etc), and also automatically fixes [idempotence issues](https://github.com/diffplug/spotless/blob/main/PADDEDCELL.md), infers [line-endings from git](#line-endings-and-encodings-invisible-stuff), is cautious about [misconfigured encoding](https://github.com/diffplug/spotless/blob/08340a11566cdf56ecf50dbd4d557ed84a70a502/testlib/src/test/java/com/diffplug/spotless/EncodingErrorMsgTest.java#L34-L38) bugs, and can use git to [ratchet formatting](#ratchet) without "format-everything" commits.

### Requirements

Spotless requires JRE 8+, and Gradle 2.14+.  Some steps require JRE 11+, `Unsupported major.minor version` means you're using a step that needs a newer JRE.

<a name="applying-to-java-source"></a>

## Java

`com.diffplug.gradle.spotless.JavaExtension` [javadoc](https://javadoc.io/static/com.diffplug.spotless/spotless-plugin-gradle/4.4.0/com/diffplug/gradle/spotless/JavaExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/JavaExtension.java)

```gradle
spotless {
  java {
    importOrder() // standard import order
    importOrder('java', 'javax', 'com.acme', '') // or importOrderFile
    // You probably want an empty string at the end - all of the
    // imports you didn't specify explicitly will go there.

    removeUnusedImports()

    googleJavaFormat() // has its own section below
    eclipse()          // has its own section below
    prettier()         // has its own section below

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
    //
    googleJavaFormat('1.7').aosp()
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


<a name="applying-to-groovy-source"></a>

## Groovy

- `com.diffplug.gradle.spotless.GroovyExtension` [javadoc](https://javadoc.io/static/com.diffplug.spotless/spotless-plugin-gradle/4.4.0/com/diffplug/gradle/spotless/GroovyExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/GroovyExtension.java)
- `com.diffplug.gradle.spotless.GroovyGradleExtension` [javadoc](https://javadoc.io/static/com.diffplug.spotless/spotless-plugin-gradle/4.4.0/com/diffplug/gradle/spotless/GroovyGradleExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/GroovyGradleExtension.java)

Configuration for Groovy is similar to [Java](#java), in that it also supports `licenseHeader` and `importOrder`.

The groovy formatter's default behavior is to format all `.groovy` and `.java` files found in the Java and Groovy source sets.  If you would like to exclude the `.java` files, set the parameter `excludeJava`, or you can set the `target` parameter as described in the [Custom rules](#custom) section.

```gradle
apply plugin: 'groovy'
spotless {
  groovy {
    importOrder() // standard import order
    importOrder('java', 'javax', 'com.acme', '') // or importOrderFile

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

[homepage](https://github.com/groovy/groovy-eclipse/wiki). [changelog](https://github.com/groovy/groovy-eclipse/releases). [compatible versions](https://github.com/diffplug/spotless/tree/main/lib-extra/src/main/resources/com/diffplug/spotless/extra/groovy_eclipse_formatter). The Groovy formatter uses some of the [eclipse jdt](#eclipse-jdt) configuration parameters in addition to groovy-specific ones. All parameters can be configured within a single file, like the Java properties file [greclipse.properties](../testlib/src/main/resources/groovy/greclipse/format/greclipse.properties) in the previous example. The formatter step can also load the [exported Eclipse properties](../ECLIPSE_SCREENSHOTS.md) and augment it with the `.metadata/.plugins/org.eclipse.core.runtime/.settings/org.codehaus.groovy.eclipse.ui.prefs` from your Eclipse workspace as shown below.

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

- `com.diffplug.gradle.spotless.KotlinExtension` [javadoc](https://javadoc.io/static/com.diffplug.spotless/spotless-plugin-gradle/4.4.0/com/diffplug/gradle/spotless/KotlinExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/KotlinExtension.java)
- `com.diffplug.gradle.spotless.KotlinGradleExtension` [javadoc](https://javadoc.io/static/com.diffplug.spotless/spotless-plugin-gradle/4.4.0/com/diffplug/gradle/spotless/KotlinGradleExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/KotlinGradleExtension.java)

```kotlin
configure<com.diffplug.gradle.spotless.SpotlessExtension> {
  kotlin {
    // by default the target is every '.kt' and '.kts` file in the java sourcesets
    ktlint()   // has its own section below
    ktfmt()    // has its own section below
    prettier() // has its own section below
    licenseHeader '/* (C)$YEAR */' // or licenseHeaderFile
  }
  kotlinGradle {
    target '*.gradle.kts' // default target for kotlinGradle
    ktlint() // or ktfmt() or prettier()
  }
}
```

<a name="applying-ktlint-to-kotlin-files"></a>

### ktlint

[homepage](https://github.com/pinterest/ktlint). [changelog](https://github.com/pinterest/ktlint/releases).  Spotless does not ([yet](https://github.com/diffplug/spotless/issues/142)) respect the `.editorconfig` settings ([ktlint docs](https://github.com/pinterest/ktlint#editorconfig)), but you can provide them manually as `userData`.

```kotlin
configure<com.diffplug.gradle.spotless.SpotlessExtension> {
  kotlin {
    // version and userData are both optional
    ktlint('0.37.2').userData(mapOf('indent_size' to '2', 'continuation_indent_size' to '2'])
```

<a name="applying-ktfmt-to-kotlin-files"></a>

### ktfmt

[homepage](https://github.com/facebookincubator/ktfmt). [changelog](https://github.com/facebookincubator/ktfmt/releases).

```kotlin
configure<com.diffplug.gradle.spotless.SpotlessExtension> {
  kotlin {
    ktfmt('0.13') // version is optional
```

<a name="applying-scalafmt-to-scala-files"></a>

## Scala

`com.diffplug.gradle.spotless.ScalaExtension` [javadoc](https://javadoc.io/static/com.diffplug.spotless/spotless-plugin-gradle/4.4.0/com/diffplug/gradle/spotless/ScalaExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/ScalaExtension.java)

```gradle
spotless {
  scala {
    // by default, all `.scala` and `.sc` files in the java sourcesets will be formatted

    scalafmt() // has its own section below

    licenseHeader '/* (C) $YEAR */', 'package ' // or licenseHeaderFile
    // note the 'package ' argument - this is a regex which identifies the top
    // of the file, be careful that all of your source has a package declaration
    // or pick a regex which works better for your code
  }
}
```

### scalafmt

[homepage](https://scalameta.org/scalafmt/). [changelog](https://github.com/scalameta/scalafmt/releases). [config docs](https://scalameta.org/scalafmt/docs/configuration.html).

```gradle
spotless {
  scala {
    // version and configFile are both optional
    scalafmt('2.6.1').configFile('scalafmt.conf')
```

<a name="applying-to-cc-sources"></a>

## C/C++

`com.diffplug.gradle.spotless.CppExtension` [javadoc](https://javadoc.io/static/com.diffplug.spotless/spotless-plugin-gradle/4.4.0/com/diffplug/gradle/spotless/CppExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/CppExtension.java)

```gradle
spotless {
  cpp {
    target 'src/native/**' // you have to set the target manually

    eclipseCdt() // has its own section below

    licenseHeader '/* (C) $YEAR */' // or licenseHeaderFile
  }
}
```

<a name="eclipse-cdt-formatter"></a>

### eclipse cdt

[homepage](https://www.eclipse.org/cdt/). [compatible versions](https://github.com/diffplug/spotless/tree/main/lib-extra/src/main/resources/com/diffplug/spotless/extra/eclipse_cdt_formatter).

Use Eclipse to define the *Code Style preferences* (see [Eclipse documentation](https://www.eclipse.org/documentation/)). Within the preferences *Edit...* dialog, you can export your configuration as XML file, which can be used as a `configFile`. If no `configFile` is provided, the CDT default configuration is used.

<a name="applying-freshmark-to-markdown-files"></a>

## FreshMark

`com.diffplug.gradle.spotless.FreshMarkExtension` [javadoc](https://javadoc.io/static/com.diffplug.spotless/spotless-plugin-gradle/4.4.0/com/diffplug/gradle/spotless/FreshMarkExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/FreshMarkExtension.java)

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

<a name="sql-dbeaver"></a>
<a name="applying-dbeaver-to-sql-scripts"></a>

## SQL

`com.diffplug.gradle.spotless.SqlExtension` [javadoc](https://javadoc.io/static/com.diffplug.spotless/spotless-plugin-gradle/4.4.0/com/diffplug/gradle/spotless/SqlExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/SqlExtension.java)

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
    dbeaver().configFile('dbeaver.props') // configFile is optional
```

Default configuration file, other options [available here](https://github.com/diffplug/spotless/blob/main/lib/src/main/java/com/diffplug/spotless/sql/dbeaver/DBeaverSQLFormatterConfiguration.java).

```properties
# case of the keywords (UPPER, LOWER or ORIGINAL)
sql.formatter.keyword.case=UPPER
# Statement delimiter
sql.formatter.statement.delimiter=;
# Indentation style (space or tab)
sql.formatter.indent.type=space
# Number of identation characters
sql.formatter.indent.size=4
```

<a name="applying-to-typescript-source"></a>

## Typescript

- `com.diffplug.gradle.spotless.TypescriptExtension` [javadoc](https://javadoc.io/static/com.diffplug.spotless/spotless-plugin-gradle/4.4.0/com/diffplug/gradle/spotless/TypescriptExtension.html), [code](https://github.com/diffplug/spotless/blob/main/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/TypescriptExtension.java)

```gradle
spotless {
  typescript {
    target 'src/**/*.ts' // you have to set the target manually

    tsfmt()    // has its own section below
    prettier() // has its own section below

    licenseHeader '/* (C) $YEAR */', '(import|const|declare|export|var) ' // or licenseHeaderFile
    // note the '(import|const|...' argument - this is a regex which identifies the top
    // of the file, be careful that all of your source has a suitable top-level declaration
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
      // provide config inline
      .config(['indentSize': 1, 'convertTabsToSpaces': true])
      // or according to tsfmt-parameters: https://github.com/vvakame/typescript-formatter/blob/7764258ad42ac65071399840d1b8701868510ca7/lib/index.ts#L27L34
      .tsconfigFile('tsconfig.json')
      .tslintFile('tslint.json')
      .vscodeFile('vscode.json')
      .tsfmtFile('tsfmt.json')
```

**Prerequisite: tsfmt requires a working NodeJS version**

tsfmt is based on NodeJS, so to use it, a working NodeJS installation (especially npm) is required on the host running spotless.
Spotless will try to auto-discover an npm installation. If that is not working for you, it is possible to directly configure the npm binary to use.

```gradle
spotless {
  typescript {
    tsfmt().npmExecutable('/usr/bin/npm').config(...)
```

Spotless uses npm to install necessary packages and run the `typescript-formatter` (`tsfmt`) package.

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
    prettier().config(['tabWidth: 4'])

    // you can also slurp from a file or even provide both (inline always takes precedence over file)
    prettier().config(['tabWidth: 4']).configFile('path-to/.prettierrc.yml')
  }
}
```

**Limitations:**
- The auto-discovery of config files (up the file tree) will not work when using prettier within spotless.
- Prettier's override syntax is not supported when using prettier within spotless.

To apply prettier to more kinds of files, just add more formats




<a name="Using plugins for prettier"></a>
### Prettier plugins

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

### NPM detection

Prettier is based on NodeJS, so a working NodeJS installation (especially npm) is required on the host running spotless.
Spotless will try to auto-discover an npm installation. If that is not working for you, it is possible to directly configure the npm binary to use.

```gradle
spotless {
  format 'javascript', {
    prettier().npmExecutable('/usr/bin/npm').config(...)
```

<a name="applying-eclipse-wtp-to-css--html--etc"></a>

## Eclipse web tools platform

[changelog](https://www.eclipse.org/webtools/). [compatible versions](https://github.com/diffplug/spotless/tree/main/lib-extra/src/main/resources/com/diffplug/spotless/extra/eclipse_wtp_formatters).

```gradle
spotless {
  format 'xml', {
    target 'src/**/*/xml' // must specify target
    eclipseWtp('xml')     // must specify a type (table below)
    eclipseWtp('xml', '4.7.3a') // optional version
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

<a name="license-header-options"></a>

## License header

If the license header (specified with `licenseHeader` or `licenseHeaderFile`) contains `$YEAR` or `$today.year`, then that token will be replaced with the current 4-digit year.  For example, if Spotless is launched in 2020, then `/* Licensed under Apache-2.0 $YEAR. */` will produce `/* Licensed under Apache-2.0 2020. */`

Once a file's license header has a valid year, whether it is a year (`2020`) or a year range (`2017-2020`), it will not be changed.  If you want the date to be updated when it changes, enable the [`ratchetFrom` functionality](#ratchet), and the year will be automatically set to today's year according to the following table (assuming the current year is 2020):

* No license header -> `2020`
* `2017` -> `2017-2020`
* `2017-2019` -> `2017-2020`

See the [javadoc](https://javadoc.io/static/com.diffplug.spotless/spotless-plugin-gradle/4.4.0/com/diffplug/gradle/spotless/FormatExtension.LicenseHeaderConfig.html) for a complete listing of options.

<a name="retroactively-populating-year-range-from-git-history">

### Retroactively slurp years from git history

If your project has not been rigorous with copyright headers, and you'd like to use git history to repair this retroactively, you can do so with `-PspotlessSetLicenseHeaderYearsFromGitHistory=true`.  When run in this mode, Spotless will do an expensive search through git history for each file, and set the copyright header based on the oldest and youngest commits for that file.  This is intended to be a one-off sort of thing.

<a name="ratchet">

## How can I enforce formatting gradually? (aka "ratchet")

If your project is not currently enforcing formatting, then it can be a noisy transition.  Having a giant commit where every single file gets changed makes the history harder to read.  To address this, you can use the `ratchet` feature:

```gradle
spotless {
  ratchetFrom 'origin/main' // only format files which have changed since origin/main
```

In this mode, Spotless will apply only to files which have changed since `origin/main`.  You can ratchet from [any point you want](https://javadoc.io/static/org.eclipse.jgit/org.eclipse.jgit/5.6.1.202002131546-r/org/eclipse/jgit/lib/Repository.html#resolve-java.lang.String-), even `HEAD`.  You can also set `ratchetFrom` per-format if you prefer (e.g. `spotless { java { ratchetFrom ...`).

However, we strongly recommend that you use a non-local branch, such as a tag or `origin/main`.  The problem with `HEAD` or any local branch is that as soon as you commit a file, that is now the canonical formatting, even if it was formatted incorrectly.  By instead specifying `origin/main` or a tag, your CI server will fail unless every changed file is at least as good or better than it was before the change.

This is especially helpful for injecting accurate copyright dates using the [license step](#license-header).

<a name="custom"></a>

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

    targetExclude 'src/main/codegen/**', 'src/test/codegen/**'
    // the files to be formatted = (target - targetExclude)
    // NOTE: if target or targetExclude is called multiple times, only the
    // last call is effective

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

If you use `custom` or `customLazy`, you might want to take a look at [this javadoc](https://javadoc.io/static/com.diffplug.spotless/spotless-plugin-gradle/4.4.0/com/diffplug/gradle/spotless/FormatExtension.html#bumpThisNumberIfACustomStepChanges-int-) for a big performance win.

See [`JavaExtension.java`](src/main/java/com/diffplug/gradle/spotless/JavaExtension.java) if you'd like to see how a language-specific set of custom rules is implemented.  We'd love PR's which add support for other languages.

If you'd like to create a one-off Spotless task outside of the `check`/`apply` framework, see [`FormatExtension.createIndependentTask`](https://github.com/diffplug/spotless/blob/91ed7203994e52058ea6c2e0f88d023ed290e433/plugin-gradle/src/main/java/com/diffplug/gradle/spotless/FormatExtension.java#L613-L639).

<a name="invisible"></a>

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

You can fix (1) by excluding the file from formatting using the `targetExclude` method, see the [custom rules](#custom) section for details.  You can fix (2) and turn these exceptions into warnings like this:

```gradle
spotless {
  java {
    googleJavaFormat()
    custom 'my-glitchy-step', { }

    ignoreErrorForStep('my-glitchy-step')   // ignore errors on all files thrown by a specific step
    ignoreErrorForPath('path/to/file.java') // ignore errors by all steps on this specific file
  }
}
```

<a name="preview"></a>

## How do I preview what `spotlessApply` will do?

- Save your working tree with `git add -A`, then `git commit -m "Checkpoint before spotless."`
- Run `gradlew spotlessApply`
- View the changes with `git diff`
- If you don't like what spotless did, `git reset --hard`
- If you'd like to remove the "checkpoint" commit, `git reset --soft head~1` will make the checkpoint commit "disappear" from history, but keeps the changes in your working directory.

<a name="examples"></a>

## Example configurations (from real-world projects)

Spotless is hosted on jcenter and at plugins.gradle.org. [Go here](https://plugins.gradle.org/plugin/com.diffplug.gradle.spotless) if you're not sure how to import the plugin.

* [One thousand github projects](https://github.com/search?l=gradle&q=spotless&type=Code)
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
