# <img align="left" src="../_images/spotless_logo.png"> Spotless: Keep your code spotless with Maven

<!---freshmark shields
output = [
  link(shield('Maven central', 'mavencentral', '{{group}}:{{artifactIdMaven}}', 'blue'), 'https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22{{group}}%22%20AND%20a%3A%22{{artifactIdMaven}}%22'),
  link(shield('Javadoc', 'javadoc', 'yes', 'blue'), 'https://javadoc.io/doc/com.diffplug.spotless/spotless-maven-plugin/{{versionLast}}/index.html'),
  link(shield('Changelog', 'changelog', '{{versionLast}}', 'brightgreen'), 'CHANGES.md'),
  '',
  link(image('Circle CI', 'https://circleci.com/gh/diffplug/spotless/tree/main.svg?style=shield'), 'https://circleci.com/gh/diffplug/spotless/tree/main'),
  link(shield('Live chat', 'gitter', 'chat', 'brightgreen'), 'https://gitter.im/{{org}}/{{name}}'),
  link(shield('License Apache', 'license', 'apache', 'brightgreen'), 'https://tldrlegal.com/license/apache-license-2.0-(apache-2.0)')
  ].join('\n');
-->
[![Maven central](https://img.shields.io/badge/mavencentral-com.diffplug.spotless%3Aspotless--maven--plugin-blue.svg)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.diffplug.spotless%22%20AND%20a%3A%22spotless-maven-plugin%22)
[![Javadoc](https://img.shields.io/badge/javadoc-yes-blue.svg)](https://javadoc.io/doc/com.diffplug.spotless/spotless-maven-plugin/2.20.0/index.html)
[![Changelog](https://img.shields.io/badge/changelog-2.20.0-brightgreen.svg)](CHANGES.md)

[![Circle CI](https://circleci.com/gh/diffplug/spotless/tree/main.svg?style=shield)](https://circleci.com/gh/diffplug/spotless/tree/main)
[![Live chat](https://img.shields.io/badge/gitter-chat-brightgreen.svg)](https://gitter.im/diffplug/spotless)
[![License Apache](https://img.shields.io/badge/license-apache-brightgreen.svg)](https://tldrlegal.com/license/apache-license-2.0-(apache-2.0))
<!---freshmark /shields -->

<!---freshmark javadoc
output = prefixDelimiterReplace(input, 'https://{{org}}.github.io/{{name}}/javadoc/spotless-plugin-maven/', '/', versionLast)
-->

Spotless is a general-purpose formatting plugin.  It is completely à la carte, but also includes powerful "batteries-included" if you opt-in. Plugin requires a version of Maven higher or equal to 3.1.0.

To people who use your build, it looks like this:

```console
user@machine repo % mvn spotless:check
[ERROR]  > The following files had format violations:
[ERROR]  src\main\java\com\diffplug\gradle\spotless\FormatExtension.java
[ERROR]    -\t\t····if·(targets.length·==·0)·{
[ERROR]    +\t\tif·(targets.length·==·0)·{
[ERROR]  Run 'mvn spotless:apply' to fix these violations.
user@machine repo % mvn spotless:apply
[INFO] BUILD SUCCESS
user@machine repo % mvn spotless:check
[INFO] BUILD SUCCESS
```

### Table of Contents

- [**Quickstart**](#quickstart)
  - [Requirements](#requirements)
  - [Binding to maven phase](#binding-to-maven-phase)
- **Languages**
  - [Java](#java) ([google-java-format](#google-java-format), [eclipse jdt](#eclipse-jdt), [prettier](#prettier), [palantir-java-format](#palantir-java-format))
  - [Groovy](#groovy) ([eclipse groovy](#eclipse-groovy))
  - [Kotlin](#kotlin) ([ktfmt](#ktfmt), [ktlint](#ktlint), [diktat](#diktat), [prettier](#prettier))
  - [Scala](#scala) ([scalafmt](#scalafmt))
  - [C/C++](#cc) ([eclipse cdt](#eclipse-cdt))
  - [Python](#python) ([black](#black))
  - [Antlr4](#antlr4) ([antlr4formatter](#antlr4formatter))
  - [Sql](#sql) ([dbeaver](#dbeaver))
  - [Maven Pom](#maven-pom) ([sortPom](#sortpom))
  - [Markdown](#markdown) ([flexmark](#flexmark))
  - [Typescript](#typescript) ([tsfmt](#tsfmt), [prettier](#prettier))
  - Multiple languages
    - [Prettier](#prettier) ([plugins](#prettier-plugins), [npm detection](#npm-detection), [`.npmrc` detection](#npmrc-detection))
    - [eclipse web tools platform](#eclipse-web-tools-platform)
- **Language independent**
  - [Generic steps](#generic-steps)
  - [License header](#license-header) ([slurp year from git](#retroactively-slurp-years-from-git-history))
  - [How can I enforce formatting gradually? (aka "ratchet")](#ratchet)
  - [`spotless:off` and `spotless:on`](#spotlessoff-and-spotlesson)
  - [Line endings and encodings (invisible stuff)](#line-endings-and-encodings-invisible-stuff)
  - [Disabling warnings and error messages](#disabling-warnings-and-error-messages)
  - [How do I preview what `mvn spotless:apply` will do?](#how-do-i-preview-what-mvn-spotlessapply-will-do)
  - [Can I apply Spotless to specific files?](#can-i-apply-spotless-to-specific-files)
  - [Example configurations (from real-world projects)](#examples)

***Contributions are welcome, see [the contributing guide](../CONTRIBUTING.md) for development info.***

## Quickstart

To use it in your pom, just [add the Spotless dependency](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.diffplug.spotless%22%20AND%20a%3A%22spotless-maven-plugin%22), and configure it like so:

```xml
<plugin>
  <groupId>com.diffplug.spotless</groupId>
  <artifactId>spotless-maven-plugin</artifactId>
  <version>${spotless.version}</version>
  <configuration>
    <!-- optional: limit format enforcement to just the files changed by this feature branch -->
    <ratchetFrom>origin/main</ratchetFrom>
    <formats>
      <!-- you can define as many formats as you want, each is independent -->
      <format>
        <!-- define the files to apply to -->
        <includes>
          <include>*.md</include>
          <include>.gitignore</include>
        </includes>
        <!-- define the steps to apply to those files -->
        <trimTrailingWhitespace/>
        <endWithNewline/>
        <indent>
          <tabs>true</tabs>
          <spacesPerTab>4</spacesPerTab>
        </indent>
      </format>
    </formats>
    <!-- define a language-specific format -->
    <java>
      <!-- no need to specify files, inferred automatically, but you can if you want -->

      <!-- apply a specific flavor of google-java-format and reflow long strings -->
      <googleJavaFormat>
        <version>1.8</version>
        <style>AOSP</style>
        <reflowLongStrings>true</reflowLongStrings>
      </googleJavaFormat>

      <!-- make sure every file has the following copyright header.
        optionally, Spotless can set copyright years by digging
        through git history (see "license" section below) -->
      <licenseHeader>
        <content>/* (C)$YEAR */</content>  <!-- or <file>${project.basedir}/license-header</file> -->
      </licenseHeader>
    </java>
  </configuration>
</plugin>
```

Spotless consists of a list of formats (in the example above, `misc` and `java`), and each format has:
- a `target` (the files to format), which you set with [`includes` and `excludes`](https://github.com/diffplug/spotless/blob/989abbecff4d8373c6111c1a98f359eadc532429/plugin-maven/src/main/java/com/diffplug/spotless/maven/FormatterFactory.java#L51-L55)
- a list of `FormatterStep`, which are just `String -> String` functions, such as [`replace`](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/generic/Replace.java), [`replaceRegex`](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/generic/ReplaceRegex.java), [`trimTrailingWhitespace`](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/generic/TrimTrailingWhitespace.java), [`indent`](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/generic/Indent.java), [`prettier`](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/generic/Prettier.java), [`eclipseWtp`](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/generic/EclipseWtp.java), and [`licenseHeader`](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/generic/LicenseHeader.java).


### Requirements

Spotless requires Maven to be running on JRE 8+.

<a name="applying-to-java-source"></a>

### Binding to maven phase

By default, spotless:check is bound to verify maven phase. This means it is not required to
explicitly bind the plugin execution, and the following will suffice;

```xml
<executions>
  <execution>
    <goals>
      <goal>check</goal>
    </goals>
  </execution>
</executions>
```

with this `mvn verify` will run `spotless:check`. If you require the check goal to be run with
any other maven phase (i.e. compile) then it can be configured as below;

```xml
<executions>
  <execution>
    <goals>
      <goal>check</goal>
    </goals>
    <phase>compile</phase>
  </execution>
</executions>
```

## Java

[code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/java/Java.java). [available steps](https://github.com/diffplug/spotless/tree/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/java).

```xml
<configuration>
  <java>
    <!-- These are the defaults, you can override if you want -->
    <includes>
      <include>src/main/java/**/*.java</include>
      <include>src/test/java/**/*.java</include>
    </includes>

    <importOrder /> <!-- standard import order -->
    <importOrder>  <!-- or a custom ordering -->
      <wildcardsLast>false</wildcardsLast> <!-- Optional, default false. Sort wildcard import after specific imports -->
      <order>java,javax,org,com,com.diffplug,,\\#com.diffplug,\\#</order>  <!-- or use <file>${project.basedir}/eclipse.importorder</file> -->
      <!-- you can use an empty string for all the imports you didn't specify explicitly, and '\\#` prefix for static imports. -->
    </importOrder>

    <removeUnusedImports /> <!-- self-explanatory -->

    <googleJavaFormat /> <!-- has its own section below -->
    <eclipse />          <!-- has its own section below -->
    <prettier />         <!-- has its own section below -->

    <licenseHeader>
      <content>/* (C)$YEAR */</content>  <!-- or <file>${project.basedir}/license-header</file> -->
    </licenseHeader>
  </java>
</configuration>
```

### google-java-format

[homepage](https://github.com/google/google-java-format). [changelog](https://github.com/google/google-java-format/releases). [code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/java/GoogleJavaFormat.java).

```xml
<googleJavaFormat>
  <version>1.8</version>                      <!-- optional -->
  <style>GOOGLE</style>                       <!-- or AOSP (optional) -->
  <reflowLongStrings>true</reflowLongStrings> <!-- optional (requires at least 1.8) -->
  <!-- optional: custom group artifact (you probably don't need this) -->
  <groupArtifact>com.google.googlejavaformat:google-java-format</groupArtifact>
</googleJavaFormat>
```

**⚠️ Note on using Google Java Format with Java 16+**

Using Java 16+ with Google Java Format 1.10.0 [requires additional flags](https://github.com/google/google-java-format/releases/tag/v1.10.0) to the running JDK.
These Flags can be provided using `MAVEN_OPTS` environment variable or using the `./mvn/jvm.config` file (See [documentation](https://maven.apache.org/configure.html#mvn-jvm-config-file)).

For example the following file under `.mvn/jvm.config` will run maven with the required flags:
```
--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED --add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED --add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED --add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED --add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
```
This is a workaround to a [pending issue](https://github.com/diffplug/spotless/issues/834).

### palantir-java-format

[homepage](https://github.com/palantir/palantir-java-format). [changelog](https://github.com/palantir/palantir-java-format/releases). [code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/java/PalantirJavaFormat.java).

```xml
<palantirJavaFormat>
  <version>2.10.0</version>                     <!-- optional -->
</palantirJavaFormat>
```

**⚠️ Note on using Palantir Java Format with Java 16+**

Using Java 16+ with Palantir Java Format [requires additional flags](https://github.com/google/google-java-format/releases/tag/v1.10.0) on the running JDK.
These Flags can be provided using `MAVEN_OPTS` environment variable or using the `./mvn/jvm.config` file (See [documentation](https://maven.apache.org/configure.html#mvn-jvm-config-file)).

For example the following file under `.mvn/jvm.config` will run maven with the required flags:
```
--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED --add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED --add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED --add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED --add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
```
This is a workaround to a [pending issue](https://github.com/diffplug/spotless/issues/834).

### eclipse jdt

[homepage](https://www.eclipse.org/downloads/packages/). [compatible versions](https://github.com/diffplug/spotless/tree/main/lib-extra/src/main/resources/com/diffplug/spotless/extra/eclipse_jdt_formatter). [code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/java/Eclipse.java). See [here](../ECLIPSE_SCREENSHOTS.md) for screenshots that demonstrate how to get and install the config file mentioned below.

```xml
<eclipse>
  <version>4.13.0</version>                     <!-- optional -->
  <file>${project.basedir}/eclipse-formatter.xml</file> <!-- optional -->
</eclipse>
```

## Groovy

[code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/groovy/Groovy.java). [available steps](https://github.com/diffplug/spotless/tree/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/groovy).

```xml
<configuration>
  <groovy>
    <!-- These are the defaults, you can override if you want -->
    <includes>
      <include>src/main/groovy/**/*.groovy</include>
      <include>src/test/groovy/**/*.groovy</include>
    </includes>

    <importOrder /> <!-- standard import order -->
    <importOrder>  <!-- or a custom ordering -->
      <order>java,javax,org,com,com.diffplug,,\\#com.diffplug,\\#</order>  <!-- or use <file>${project.basedir}/eclipse.importorder</file> -->
      <!-- you can use an empty string for all the imports you didn't specify explicitly, and '\\#` prefix for static imports -->
    </importOrder>

    <greclipse />          <!-- has its own section below -->

    <licenseHeader>
      <content>/* (C)$YEAR */</content>  <!-- or <file>${project.basedir}/license-header</file> -->
    </licenseHeader>
  </groovy>
</configuration>
```

### eclipse groovy

[homepage](https://github.com/groovy/groovy-eclipse/wiki). [changelog](https://github.com/groovy/groovy-eclipse/releases). [compatible versions](https://github.com/diffplug/spotless/tree/main/lib-extra/src/main/resources/com/diffplug/spotless/extra/groovy_eclipse_formatter). The Groovy formatter uses some of the [eclipse jdt](#eclipse-jdt) configuration parameters in addition to groovy-specific ones. All parameters can be configured within a single file, like the Java properties file [greclipse.properties](../testlib/src/main/resources/groovy/greclipse/format/greclipse.properties) in the previous example. The formatter step can also load the [exported Eclipse properties](../ECLIPSE_SCREENSHOTS.md) and augment it with the `.metadata/.plugins/org.eclipse.core.runtime/.settings/org.codehaus.groovy.eclipse.ui.prefs` from your Eclipse workspace as shown below.

```xml
<greclipse>
  <version>4.13.0</version>                     <!-- optional -->
  <file>${project.basedir}/greclipse.properties</file> <!-- optional -->
</greclipse>
```

Groovy-Eclipse formatting errors/warnings lead per default to a build failure. This behavior can be changed by adding the property/key value `ignoreFormatterProblems=true` to a configuration file. In this scenario, files causing problems, will not be modified by this formatter step.

<a name="applying-to-kotlin-source"></a>

## Kotlin

[code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/kotlin/Kotlin.java). [available steps](https://github.com/diffplug/spotless/tree/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/kotlin).

```xml
<configuration>
  <kotlin>
    <!-- These are the defaults, you can override if you want -->
    <includes>
      <include>src/main/kotlin/**/*.kt</include>
      <include>src/test/kotlin/**/*.kt</include>
    </includes>

    <ktfmt />    <!-- has its own section below -->
    <ktlint />   <!-- has its own section below -->
    <diktat />   <!-- has its own section below -->
    <prettier /> <!-- has its own section below -->

    <licenseHeader>
      <content>/* (C)$YEAR */</content>  <!-- or <file>${project.basedir}/license-header</file> -->
    </licenseHeader>
  </kotlin>
</configuration>
```

### ktfmt

[homepage](https://github.com/facebookincubator/ktfmt). [changelog](https://github.com/facebookincubator/ktfmt/releases). [code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/kotlin/Ktfmt.java).

```xml
<ktfmt>
  <version>0.30</version> <!-- optional -->
  <style>DEFAULT</style> <!-- optional, other option is DROPBOX -->
</ktfmt>
```

<a name="applying-ktlint-to-kotlin-files"></a>

### ktlint

[homepage](https://github.com/pinterest/ktlint). [changelog](https://github.com/pinterest/ktlint/releases). [code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/kotlin/Ktlint.java).  Spotless does not ([yet](https://github.com/diffplug/spotless/issues/142)) respect the `.editorconfig` settings.

```xml
<ktlint>
  <version>0.43.2</version> <!-- optional -->
</ktlint>
```

### diktat

[homepage](https://github.com/cqfn/diKTat). [changelog](https://github.com/cqfn/diKTat/releases). [code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/kotlin/Diktat.java). You can provide configuration path manually as `configFile`.

```xml
<diktat>
  <version>1.0.1</version> <!-- optional -->
  <configFile>full/path/to/diktat-analysis.yml</configFile> <!-- optional, configuration file path -->
</diktat>
```

<a name="applying-to-scala-source"></a>

## Scala

[code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/scala/Scala.java). [available steps](https://github.com/diffplug/spotless/tree/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/scala).

```xml
<configuration>
  <scala>
    <!-- These are the defaults, you can override if you want -->
    <includes>
      <include>src/main/scala/**/*.scala</include>
      <include>src/test/scala/**/*.scala</include>
      <include>src/main/scala/**/*.sc</include>
      <include>src/test/scala/**/*.sc</include>
    </includes>

    <scalafmt /> <!-- has its own section below -->

    <licenseHeader>
      <content>/* (C)$YEAR */</content>  <!-- or <file>${project.basedir}/license-header</file> -->
      <delimiter>package </delimiter> <!--
        note the 'package ' argument - this is a regex which identifies the top
        of the file, be careful that all of your sources have a package declaration,
        or pick a regex which works better for your code -->
    </licenseHeader>
  </scala>
</configuration>
```

### scalafmt

[homepage](https://scalameta.org/scalafmt/). [changelog](https://github.com/scalameta/scalafmt/releases). [config docs](https://scalameta.org/scalafmt/docs/configuration.html). [code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/scala/Scalafmt.java).

```xml
<scalafmt>
  <version>2.0.1</version>              <!-- optional -->
  <file>${project.basedir}/scalafmt.conf</file> <!-- optional -->
</scalafmt>
```

<a name="cpp"></a>

<a name="applying-to-cc-source"></a>

## C/C++

[code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/cpp/Cpp.java). [available steps](https://github.com/diffplug/spotless/tree/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/cpp).

```xml
<configuration>
  <cpp>
    <includes> <!-- You have to set the target manually -->
      <include>src/native/**</inclue>
    </includes>

    <eclipseCdt /> <!-- has its own section below -->

    <licenseHeader>
      <content>/* (C)$YEAR */</content>  <!-- or <file>${project.basedir}/license-header</file> -->
    </licenseHeader>
  </cpp>
</configuration>
```

### eclipse cdt

[homepage](https://www.eclipse.org/cdt/). [compatible versions](https://github.com/diffplug/spotless/tree/main/lib-extra/src/main/resources/com/diffplug/spotless/extra/eclipse_cdt_formatter). [code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/cpp/EclipseCdt.java).

```xml
<eclipseCdt>
  <version>4.13.0</version>               <!-- optional -->
  <file>${project.basedir}/eclipse-cdt.xml</file> <!-- optional -->
</eclipseCdt>
```

## Python

[code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/python/Python.java). [available steps](https://github.com/diffplug/spotless/tree/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/python/Black.java).

```xml
<configuration>
  <python>
    <!-- You have to set the target manually -->
    <includes>
      <include>src/main/**/*.py</include>
    </includes>

    <black />  <!-- has its own section below -->
  </python>
</configuration>
```

### black

[homepage](https://github.com/psf/black). [changelog](https://github.com/psf/black/blob/master/CHANGES.md).

```xml
<black>
  <version>19.10b0</version> <!-- optional -->
  <!-- if black is not on your path, you must specify its location manually -->
  <pathToExe>C:/myuser/.pyenv/versions/3.8.0/scripts/black.exe</pathToExe>
  <!--
    Spotless always checks the version of the black it is using
    and will fail with an error if it does not match the expected version
    (whether manually specified or default). If there is a problem, Spotless
    will suggest commands to help install the correct version.
    TODO: handle installation & packaging automatically - https://github.com/diffplug/spotless/issues/674
  -->
</black>
```

## Antlr4

[code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/antlr4/Antlr4.java). [available steps](https://github.com/diffplug/spotless/tree/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/antlr4).

```xml
<configuration>
  <antlr4>
    <!-- These are the defaults, you can override if you want -->
    <includes>
      <include>src/*/antlr4/**/*.g4</include>
    </includes>

    <antlr4formatter /> <!-- has its own section below -->

    <licenseHeader>
      <content>/* (C)$YEAR */</content>  <!-- or <file>${project.basedir}/license-header</file> -->
    </licenseHeader>
  </antlr4>
</configuration>
```

### antlr4formatter

[homepage](https://github.com/antlr/Antlr4Formatter). [available versions](https://search.maven.org/artifact/com.khubla.antlr4formatter/antlr4-formatter). [code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/antlr4/Antlr4Formatter.java).

```xml
<antlr4formatter>
  <version>1.2.1</version> <!-- optional -->
</antlr4formatter>
```

## SQL

[code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/sql/Sql.java). [available steps](https://github.com/diffplug/spotless/tree/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/sql).

```xml
<configuration>
  <sql>
    <!-- You have to set the target manually -->
    <includes>
      <include>src/main/resources/**/*.sql</include>
    </includes>

    <dbeaver />  <!-- has its own section below -->
    <prettier /> <!-- has its own section below -->
  </sql>
</configuration>
```

### dbeaver

[homepage](https://dbeaver.io/). DBeaver is only distributed as a monolithic jar, so the formatter used here was copy-pasted into Spotless, and thus there is no version to change.

```xml
<dbeaver>
    <configFile>dbeaver.props</configFile> <!-- configFile is optional -->
</dbeaver>
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

## Maven POM

[code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/pom/Pom.java). [available steps](https://github.com/diffplug/spotless/tree/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/pom/SortPom.java).

```xml
<configuration>
  <pom>
    <!-- These are the defaults, you can override if you want -->
    <includes>
      <include>pom.xml</include>
    </includes>

    <sortPom /> <!-- has its own section below -->

  </pom>
</configuration>
```

### sortPom

[homepage](https://github.com/Ekryd/sortpom). [code](https://github.com/diffplug/spotless/tree/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/pom/SortPom.java).

All configuration settings are optional, they are described in detail [here](https://github.com/Ekryd/sortpom/wiki/Parameters).

```xml
<sortPom>

  <encoding>UTF-8</encoding> <!-- The encoding of the pom files -->

  <lineSeparator>${line.separator}</lineSeparator> <!-- line separator to use -->

  <expandEmptyElements>true</expandEmptyElements> <!-- Should empty elements be expanded-->

  <spaceBeforeCloseEmptyElement>false</spaceBeforeCloseEmptyElement> <!-- Should a space be added inside self-closing elements-->

  <keepBlankLines>true</keepBlankLines> <!-- Keep empty lines -->

  <nrOfIndentSpace>2</nrOfIndentSpace> <!-- Indentation -->

  <indentBlankLines>false</indentBlankLines> <!-- Should empty lines be indented -->

  <indentSchemaLocation>false</indentSchemaLocation> <!-- Should schema locations be indended -->

  <predefinedSortOrder>recommended_2008_06</predefinedSortOrder> <!-- Sort order of elements: https://github.com/Ekryd/sortpom/wiki/PredefinedSortOrderProfiles-->

  <sortOrderFile></sortOrderFile> <!-- Custom sort order of elements: https://raw.githubusercontent.com/Ekryd/sortpom/master/sorter/src/main/resources/custom_1.xml -->

  <sortDependencies></sortDependencies> <!-- Sort dependencies: https://github.com/Ekryd/sortpom/wiki/SortDependencies-->

  <sortDependencyExclusions></sortDependencyExclusions> <!-- Sort dependency exclusions: https://github.com/Ekryd/sortpom/wiki/SortDependencies-->

  <sortPlugins></sortPlugins> <!-- Sort plugins: https://github.com/Ekryd/sortpom/wiki/SortPlugins -->

  <sortProperties>false</sortProperties> <!-- Sort properties -->

  <sortModules>false</sortModules> <!-- Sort modules -->

  <sortExecutions>false</sortExecutions> <!-- Sort plugin executions -->
</sortPom>
```

## Markdown

[code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/markdown/Markdown.java). [available steps](https://github.com/diffplug/spotless/tree/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/markdown).

```xml
<configuration>
  <markdown>
    <includes> <!-- You have to set the target manually -->
      <include>**/*.md</include>
    </includes>

    <flexmark/> <!-- has its own section below -->
  </markdown>
</configuration>
```

### Flexmark

[homepage](https://github.com/vsch/flexmark-java). [code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/markdown/Flexmark.java). Flexmark is a flexible Commonmark/Markdown parser that can be used to format Markdown files. It supports different [flavors of Markdown](https://github.com/vsch/flexmark-java#markdown-processor-emulation) and [many formatting options](https://github.com/vsch/flexmark-java/wiki/Markdown-Formatter#options).

Currently, none of the available options can be configured yet. It uses only the default options together with `COMMONMARK` as `FORMATTER_EMULATION_PROFILE`.

<a name="applying-to-typescript-source"></a>

## Typescript

[code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/typescript/Typescript.java). [available steps](https://github.com/diffplug/spotless/tree/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/typescript).

```xml
<configuration>
  <typescript>
    <includes> <!-- You have to set the target manually -->
      <include>src/**/*.ts</include>
    </includes>

    <tsfmt/>    <!-- has its own section below -->
    <prettier/> <!-- has its own section below -->

    <licenseHeader>
      <content>/* (C)$YEAR */</content>  <!-- or <file>${project.basedir}/license-header</file> -->
      <delimiter>(import|const|declare|export|var) </delimiter> <!--
        note the '(import|const|...' argument - this is a regex which identifies the top
        of the file, be careful that all of your sources have a suitable top-level declaration,
        or pick a regex which works better for your code -->
    </licenseHeader>
  </typescript>
</configuration>
```

### tsfmt

[npm](https://www.npmjs.com/package/typescript-formatter). [changelog](https://github.com/vvakame/typescript-formatter/blob/master/CHANGELOG.md). [code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/typescript/Tsfmt.java). *Please note:*
The auto-discovery of config files (up the file tree) will not work when using tsfmt within spotless,
  hence you are required to provide resolvable file paths for config files, or alternatively provide the configuration inline. See [tsfmt's default config settings](https://github.com/vvakame/typescript-formatter/blob/7764258ad42ac65071399840d1b8701868510ca7/lib/utils.ts#L11L32) for what is available.

```xml
<tsfmt>
  <typescriptFormatterVersion>7.2.2</typescriptFormatterVersion> <!-- optional https://www.npmjs.com/package/typescript-formatter -->
  <typescriptVersion>3.9.5</typescriptVersion> <!-- optional https://www.npmjs.com/package/typescript -->
  <tslintVersion>6.1.2</tslintVersion> <!-- optional https://www.npmjs.com/package/tslint -->

  <config> <!-- https://github.com/vvakame/typescript-formatter/blob/7764258ad42ac65071399840d1b8701868510ca7/lib/utils.ts#L11L32 -->
    <indentSize>1</indentSize>
    <convertTabsToSpaces>true</convertTabsToSpaces>
  </config>
  <!-- // or according to tsfmt-parameters: https://github.com/vvakame/typescript-formatter/blob/7764258ad42ac65071399840d1b8701868510ca7/lib/index.ts#L27L34 -->
  <tsconfigFile>${project.basedir}/tsconfig.json</tsconfigFile>
  <tslintFile>${project.basedir}/tslint.json</tslintFile>
  <vscodeFile>${project.basedir}/vscode.json</vscodeFile>
  <tsfmtFile>${project.basedir}/tsfmt.json</tsfmtFile>
</tsfmt>
```

**Prerequisite: tsfmt requires a working NodeJS version**

For details, see the [npm detection](#npm-detection) and [`.npmrc` detection](#npmrc-detection) sections of prettier, which apply also to tsfmt.

<a name="applying-prettier-to-javascript--flow--typescript--css--scss--less--jsx--graphql--yaml--etc"></a>

## Prettier

[homepage](https://prettier.io/). [changelog](https://github.com/prettier/prettier/blob/master/CHANGELOG.md). [official plugins](https://prettier.io/docs/en/plugins.html#official-plugins). [community plugins](https://prettier.io/docs/en/plugins.html#community-plugins). Prettier is a formatter that can format almost every anything - JavaScript, JSX, Angular, Vue, Flow, TypeScript, CSS, Less, SCSS, HTML, JSON, GraphQL, Markdown (including GFM and MDX), and YAML.  It can format even more [using plugins](https://prettier.io/docs/en/plugins.html) (PHP, Ruby, Swift, XML, Apex, Elm, Java (!!), Kotlin, pgSQL, .properties, solidity, svelte, toml, shellscript, ...).

You can use prettier in any language-specific format, but usually you will be creating a generic format.

```xml
<configuration>
  <formats>
    <format>
      <includes>
        <include>src/**/typescript/**/*.ts</include>
      </includes>

      <prettier>
        <!-- Specify at most one of the following 3 configs: either 'prettierVersion' (2.0.5 is default), 'devDependencies' or 'devDependencyProperties'  -->
        <prettierVersion>1.19.0</prettierVersion>
        <devDependencies>
            <prettier>1.19.0</prettier>
        </devDependencies>
        <devDependencyProperties>
          <property>
            <name>prettier</name>
            <value>2.0.5</value>
          </property>
          <property>
            <name>@prettier/plugin-php</name> <!-- this could not be written in the simpler to write 'devDependencies' element. -->
            <value>0.14.2</value>
          </property>
        </devDependencyProperties>
        <!-- Specify config file and/or inline config, the inline always trumps file -->
        <configFile>${project.basedir}/path/to/configfile</configFile>
        <config>
            <useTabs>true</useTabs>
        </config>
      </prettier>
    </format>

  </formats>
</configuration>
```

**Limitations:**
- The auto-discovery of config files (up the file tree) will not work when using prettier within spotless.
- Prettier's override syntax is not supported when using prettier within spotless.

To apply prettier to more kinds of files, just add more formats

```xml
<configuration>
  <formats>
    <format><includes>src/**/*.ts</includes><prettier/></format>
    <format><includes>src/**/*.css</includes><prettier/></format>
```

<a name="using-plugins-for-prettier"></a>

### prettier plugins

Since spotless uses the actual npm prettier package behind the scenes, it is possible to use prettier with
[plugins](https://prettier.io/docs/en/plugins.html#official-plugins) or [community-plugins](https://www.npmjs.com/search?q=prettier-plugin) in order to support even more file types.

```xml
<configuration>
  <formats>
    <!-- prettier with java-plugin -->
    <format>
      <includes>
        <include>src/*/java/**/*.java</include>
      </includes>

      <prettier>
        <devDependencies>
            <prettier>2.0.5</prettier>
            <prettier-plugin-java>0.8.0</prettier-plugin-java>
        </devDependencies>
        <config>
            <tabWidth>4</tabWidth>
            <parser>java</parser>
        </config>
      </prettier>
    </format>

    <!-- prettier with php-plugin -->
    <format>
      <includes>
        <include>src/**/*.php</include>
      </includes>

      <prettier>
        <!-- use the devDependencyProperties writing style when the property-names are not well-formed such as @prettier/plugin-php -->
        <devDependencyProperties>
          <property>
            <name>prettier</name>
            <value>2.0.5</value>
          </property>
          <property>
            <name>@prettier/plugin-php</name>
            <value>0.14.2</value>
          </property>
        </devDependencyProperties>
        <config>
            <tabWidth>3</tabWidth>
            <parser>php</parser>
        </config>
      </prettier>
    </format>

  </formats>
</configuration>
```

### npm detection

Prettier is based on NodeJS, so to use it, a working NodeJS installation (especially npm) is required on the host running spotless.
Spotless will try to auto-discover an npm installation. If that is not working for you, it is possible to directly configure the npm binary to use.

```xml
<prettier>
  <npmExecutable>/usr/bin/npm</npmExecutable>
```

### `.npmrc` detection

Spotless picks up npm configuration stored in a `.npmrc` file either in the project directory or in your user home.
Alternatively you can supply spotless with a location of the `.npmrc` file to use. (This can be combined with `npmExecutable`, of course.)

```xml
<prettier>
  <npmrc>/usr/local/shared/.npmrc</npmrc>
```

<a name="applying-eclipse-wtp-to-css--html--etc"></a>

## Eclipse web tools platform

[changelog](https://www.eclipse.org/webtools/). [compatible versions](https://github.com/diffplug/spotless/tree/main/lib-extra/src/main/resources/com/diffplug/spotless/extra/eclipse_wtp_formatters).

```xml
<configuration>
  <formats>
    <format>
      <includes>
        <include>src/**/resources/**/*.xml</include>
        <include>src/**/resources/**/*.xsd</include>
      </includes>

      <eclipseWtp>
        <type>XML</type> <!-- specify the WTP formatter type (XML, JS, ...) -->
        <files> <!-- specify the configuration for the selected type -->
          <file>${project.basedir}/xml.prefs</file>
          <file>${project.basedir}/additional.properties</file>
        </files>
        <version>4.13.0</version> <!-- optional -->
      </eclipseWtp>
    </format>
  </formats>
</configuration>
```

The WTP formatter accept multiple configuration files. All Eclipse configuration file formats are accepted as well as simple Java property files. Omit the `<files>` entirely to use the default Eclipse configuration. The following formatters and configurations are supported:

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

<a name="format"></a>
<a name="custom rules"></a>

## Generic steps

[Prettier](#prettier), [eclipse wtp](#eclipse-web-tools-platform), and [license header](#license-header) are available in every format, and they each have their own section. As mentioned in the [quickstart](#quickstart), there are a variety of simple generic steps which are also available in every format, here are examples of these:

```xml
<trimTrailingWhitespace /> <!-- trim trailing whitespaces -->

<endWithNewline /> <!-- files must end with a newline -->

<indent> <!-- specify whether to use tabs or spaces for indentation -->
  <spaces>true</spaces> <!-- or <tabs>true</tabs> -->
  <spacesPerTab>4</spacesPerTab> <!-- optional, default is 4 -->
</indent>

<jsr223> <!-- specify replacements using JSR223 scripting -->
  <name>Greetings to Mars</name>
  <dependency>org.codehaus.groovy:groovy-jsr223:3.0.9</dependency> <!-- optional, maven dependency, containing the jsr223 compatible scripting engine-->
  <engine>groovy</engine> <!-- nashorn is provided by JDK 8-14, other engines can be loaded from the given dependency -->
  <script>source.replace('World','Mars');</script> <!-- the source variable contains the unformatted code, the returned value of the script is the formatted code -->
</jsr223>

<nativeCmd> <!-- run a native binary -->
  <name>Greetings to Mars from sed</name>
  <pathToExe>/usr/bin/sed</pathToExe> <!-- path to the binary, unformatted code is send via StdIn, formatted code is expected on StdOut -->
  <arguments> <!-- optional, list with arguments for the binary call-->
    <argument>s/World/Mars/g</argument>
  </arguments>
</nativeCmd>

<replace> <!-- specify replacements using search and replace -->
  <name>Say Hello to Mars</name>
  <search>World</search>
  <replacement>Mars</replacement>
</replace>

<replaceRegex> <!-- specify replacements using regex match and replace -->
  <name>Say Hello to Mars from Regex</name>
  <searchRegex>(Hello) W[a-z]{3}d</searchRegex>
  <replacement>$1 Mars</replacement>
</replaceRegex>
```

<a name="license-header-options"></a>

## License header

Spotless can inject a license header into your files, including populating an accurate copyright header from today's date or from git history.

```xml
<licenseHeader> <!-- specify either content or file, but not both -->
  <content>/* (C)$YEAR */</content>  <!-- or <file>${project.basedir}/license-header</file> -->
  <delimiter>#</delimiter>  <!-- content until first occurrence of the delimiter regex will be interpreted as header section -->
</licenseHeader>
```

If the license header (specified with `content` or `file`) contains `$YEAR` or `$today.year`, then that token will be replaced with the current 4-digit year.  For example, if Spotless is launched in 2020, then `/* Licensed under Apache-2.0 $YEAR. */` will produce `/* Licensed under Apache-2.0 2020. */`

Once a file's license header has a valid year, whether it is a year (`2020`) or a year range (`2017-2020`), it will not be changed.  If you want the date to be updated when it changes, enable the [`ratchetFrom` functionality](#ratchet), and the year will be automatically set to today's year according to the following table (assuming the current year is 2020):

* No license header -> `2020`
* `2017` -> `2017-2020`
* `2017-2019` -> `2017-2020`

### Retroactively slurp years from git history

If your project has not been rigorous with copyright headers, and you'd like to use git history to repair this retroactively, you can do so with `-DspotlessSetLicenseHeaderYearsFromGitHistory=true`.  When run in this mode, Spotless will do an expensive search through git history for each file, and set the copyright header based on the oldest and youngest commits for that file.  This is intended to be a one-off sort of thing.

<a name="invisible"></a>

<a name="ratchet"></a>

## Incremental up-to-date checking and formatting

**This feature is turned off by default.**

Execution of `spotless:check` and `spotless:apply` for large projects can take time.
By default, Spotless Maven plugin needs to read and format each source file.
Repeated executions of `spotless:check` or `spotless:apply` are completely independent.

If your project has many source files managed by Spotless and formatting takes a long time, you can
enable incremental up-to-date checking with the following configuration:

```xml
<configuration>
  <upToDateChecking>
    <enabled>true</enabled>
    <indexFile>${project.basedir}/custom-index-file</indexFile> <!-- optional, default is ${project.build.directory}/spotless-index -->
  </upToDateChecking>
  <!-- ... define formats ... -->
</configuration>
```

With up-to-date checking enabled, Spotless creates an index file in the `target` directory.
The index file contains source file paths and corresponding last modified timestamps.
It allows Spotless to skip already formatted files that have not changed.

**Note:** the index file is located in the `target` directory. Executing `mvn clean` will delete
the index file, and Spotless will need to check/format all the source files. You can override the default index file location with the `indexFile` configuration parameter.

Spotless will remove the index file when up-to-date checking is explicitly turned off with the
following configuration:

```xml
<configuration>
  <upToDateChecking>
    <enabled>false</enabled>
    <indexFile>${project.basedir}/custom-index-file</indexFile> <!-- optional, default is ${project.build.directory}/spotless-index -->
  </upToDateChecking>
  <!-- ... define formats ... -->
</configuration>
```

Consider using this configuration if you experience issues with up-to-date checking.

## How can I enforce formatting gradually? (aka "ratchet")

If your project is not currently enforcing formatting, then it can be a noisy transition.  Having a giant commit where every single file gets changed makes the history harder to read.  To address this, you can use the `ratchet` feature:

```xml
<configuration>
  <ratchetFrom>origin/main</ratchetFrom> <!-- only format files which have changed since origin/main -->
  <!-- ... define formats ... -->
</configuration>
```

In this mode, Spotless will apply only to files which have changed since `origin/main`.  You can ratchet from [any point you want](https://javadoc.io/doc/org.eclipse.jgit/org.eclipse.jgit/5.6.1.202002131546-r/org/eclipse/jgit/lib/Repository.html#resolve-java.lang.String-), even `HEAD`.  You can also set `ratchetFrom` per-format if you prefer (e.g. `<configuration><java><ratchetFrom>...`).

However, we strongly recommend that you use a non-local branch, such as a tag or `origin/main`.  The problem with `HEAD` or any local branch is that as soon as you commit a file, that is now the canonical formatting, even if it was formatted incorrectly.  By instead specifying `origin/main` or a tag, your CI server will fail unless every changed file is at least as good or better than it was before the change.

This is especially helpful for injecting accurate copyright dates using the [license step](#license-header).

## `spotless:off` and `spotless:on`

Sometimes there is a chunk of code  which you have carefully handcrafted, and you would like to exclude just this one little part from getting clobbered by the autoformat. Some formatters have a way to do this, many don't, but who cares. If you setup your spotless like this:

```xml
<configuration>
  <java> <!-- or scala, or c, or whatever -->
    <toggleOffOn />
    ...
```

Then whenever Spotless encounters a pair of `spotless:off` / `spotless:on`, it will exclude that subsection of code from formatting. If you want, you can change the tags to be whatever you want, e.g. `<toggleOffOn><off>fmt:off</off><on>fmt:on</on></toggleOffOn>')`. If you change the default, [read this](https://github.com/diffplug/spotless/pull/691) for some gotchas.

## Line endings and encodings (invisible stuff)

Spotless uses UTF-8 by default, but you can use [any encoding which Java supports](https://docs.oracle.com/javase/8/docs/technotes/guides/intl/encoding.doc.html).  You can set it globally, and you can also set it per-format.

```xml
<configuration>
  <java>
    <encoding>Cp1252</encoding>
    <!-- ... other steps ... -->
  </java>
  <encoding>US-ASCII</encoding>
</configuration>
```

Line endings can also be set globally or per-format using the `lineEndings` property.  Spotless supports four line ending modes: `UNIX`, `WINDOWS`, `PLATFORM_NATIVE`, and `GIT_ATTRIBUTES`.  The default value is `GIT_ATTRIBUTES`, and *we highly recommend that you* ***do not change*** *this value*.  Git has opinions about line endings, and if Spotless and git disagree, then you're going to have a bad time.

You can easily set the line endings of different files using [a `.gitattributes` file](https://help.github.com/articles/dealing-with-line-endings/).  Here's an example `.gitattributes` which sets all files to unix newlines: `* text eol=lf`.

<a name="enforceCheck"></a>

## Disabling warnings and error messages

By default, `spotless:check` is bound to the `verify` phase.  You might want to disable this behavior.  We [recommend against this](https://github.com/diffplug/spotless/issues/79#issuecomment-290844602), but it's easy to do if you'd like:

- set `-Dspotless.check.skip=true` at the command line
- set `spotless.check.skip` to `true` in the `<properties>` section of the `pom.xml`

<a name="preview"></a>

## How do I preview what `mvn spotless:apply` will do?

- Save your working tree with `git add -A`, then `git commit -m "Checkpoint before spotless."`
- Run `mvn spotless:apply`
- View the changes with `git diff`
- If you don't like what spotless did, `git reset --hard`
- If you'd like to remove the "checkpoint" commit, `git reset --soft head~1` will make the checkpoint commit "disappear" from history, but keeps the changes in your working directory.

## Can I apply Spotless to specific files?

You can target specific files by setting the `spotlessFiles` project property to a comma-separated list of file patterns:

```
cmd> mvn spotless:apply -DspotlessFiles=my/file/pattern.java,more/generic/.*-pattern.java
```

The patterns are matched using `String#matches(String)` against the absolute file path.

<a name="examples"></a>

## Example configurations (from real-world projects)

- [Apache Avro](https://github.com/apache/avro/blob/8026c8ffe4ef67ab419dba73910636bf2c1a691c/lang/java/pom.xml#L307-L334)
- [JUNG: Java Universal Network/Graph Framework](https://github.com/jrtom/jung/blob/b3a2461b97bb3ab40acc631e21feef74976489e4/pom.xml#L187-L208)

<!---freshmark /javadoc -->
