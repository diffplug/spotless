# <img align="left" src="../_images/spotless_logo.png"> Spotless: Keep your code spotless with Maven

<!---freshmark shields
output = [
  link(shield('MavenCentral', 'mavencentral', '{{group}}:{{artifactIdMaven}}', 'blue'), 'https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22{{group}}%22%20AND%20a%3A%22{{artifactIdMaven}}%22'),
  link(shield('Changelog', 'changelog', '{{versionLast}}', 'blue'), 'CHANGES.md'),
  link(shield('Javadoc', 'javadoc', 'here', 'blue'), 'https://javadoc.io/doc/com.diffplug.spotless/spotless-maven-plugin/{{versionLast}}/index.html')
  ].join('\n');
-->
[![MavenCentral](https://img.shields.io/badge/mavencentral-com.diffplug.spotless%3Aspotless--maven--plugin-blue.svg)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.diffplug.spotless%22%20AND%20a%3A%22spotless-maven-plugin%22)
[![Changelog](https://img.shields.io/badge/changelog-2.44.0.BETA2-blue.svg)](CHANGES.md)
[![Javadoc](https://img.shields.io/badge/javadoc-here-blue.svg)](https://javadoc.io/doc/com.diffplug.spotless/spotless-maven-plugin/2.44.0.BETA2/index.html)
<!---freshmark /shields -->

<!---freshmark javadoc
output = prefixDelimiterReplace(input, 'https://{{org}}.github.io/{{name}}/javadoc/spotless-plugin-maven/', '/', versionLast)
-->

Spotless is a general-purpose formatting plugin used by [6,000 projects on GitHub (Jan 2023)](https://github.com/search?l=Maven+POM&q=spotless&type=Code).  It is completely à la carte, but also includes powerful "batteries-included" if you opt-in. Plugin requires a version of Maven higher or equal to 3.1.0.

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
  - [Java](#java) ([google-java-format](#google-java-format), [eclipse jdt](#eclipse-jdt), [prettier](#prettier), [palantir-java-format](#palantir-java-format), [formatAnnotations](#formatAnnotations), [cleanthat](#cleanthat))
  - [Groovy](#groovy) ([eclipse groovy](#eclipse-groovy))
  - [Kotlin](#kotlin) ([ktfmt](#ktfmt), [ktlint](#ktlint), [diktat](#diktat), [prettier](#prettier))
  - [Scala](#scala) ([scalafmt](#scalafmt))
  - [C/C++](#cc) ([eclipse cdt](#eclipse-cdt))
  - [Python](#python) ([black](#black))
  - [Antlr4](#antlr4) ([antlr4formatter](#antlr4formatter))
  - [Sql](#sql) ([dbeaver](#dbeaver))
  - [Maven Pom](#maven-pom) ([sortPom](#sortpom))
  - [Markdown](#markdown) ([flexmark](#flexmark))
  - [Typescript](#typescript) ([tsfmt](#tsfmt), [prettier](#prettier), [ESLint](#eslint-typescript), [Biome](#biome))
  - [Javascript](#javascript) ([prettier](#prettier), [ESLint](#eslint-javascript), [Biome](#biome))
  - [JSON](#json) ([simple](#simple), [gson](#gson), [jackson](#jackson), [Biome](#biome), [jsonPatch](#jsonPatch))
  - [YAML](#yaml)
  - [Gherkin](#gherkin)
  - [Go](#go)
  - [RDF](#RDF)
  - Multiple languages
    - [Prettier](#prettier) ([plugins](#prettier-plugins), [npm detection](#npm-detection), [`.npmrc` detection](#npmrc-detection), [caching `npm install` results](#caching-results-of-npm-install))
    - [eclipse web tools platform](#eclipse-web-tools-platform)
    - [Biome](#biome) ([binary detection](#biome-binary), [config file](#biome-configuration-file), [input language](#biome-input-language))
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

To use it in your pom, just [add the Spotless plugin](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.diffplug.spotless%22%20AND%20a%3A%22spotless-maven-plugin%22), and configure it like so:

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
          <include>.gitattributes</include>
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
        <formatJavadoc>false</formatJavadoc>
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
- **order matters**, and this is good! (More info [here](https://github.com/diffplug/spotless/blob/main/PADDEDCELL.md) and [here](https://github.com/diffplug/spotless/blob/main/CONTRIBUTING.md#how-spotless-works))
  - For example, `googleJavaFormat` always indents with spaces, but some wish it had a tab mode
    - ```xml
      <googleJavaFormat/> // this works
      <indent><tabs>true</tabs><spacesPerTab>2</spacesPerTab></indent>
       ```
    - ```xml
      <indent><tabs>true</tabs><spacesPerTab>2</spacesPerTab></indent>
      <googleJavaFormat/> // the tab indentation gets overwritten
       ```

### Requirements

Spotless requires Maven to be running on JRE 11+. To use JRE 8, go back to [`2.30.0` or older](https://github.com/diffplug/spotless/blob/main/plugin-maven/CHANGES.md#2300---2023-01-13).

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

    <!-- Cleanthat will refactor your code, but it may break your style: apply it before your formatter -->
    <cleanthat />        <!-- has its own section below -->

    <googleJavaFormat /> <!-- has its own section below -->
    <eclipse />          <!-- has its own section below -->
    <prettier />         <!-- has its own section below -->

    <importOrder /> <!-- standard import order -->
    <importOrder>  <!-- or a custom ordering -->
      <wildcardsLast>false</wildcardsLast> <!-- Optional, default false. Sort wildcard import after specific imports -->
      <order>java|javax,org,com,com.diffplug,,\#com.diffplug,\#</order>  <!-- or use <file>${project.basedir}/eclipse.importorder</file> -->
      <!-- you can use an empty string for all the imports you didn't specify explicitly, '|' to join group without blank line, and '\#` prefix for static imports. -->
      <semanticSort>false</semanticSort> <!-- Optional, default false. Sort by package, then class, then member (for static imports). Splitting is based on common conventions (packages are lower case, classes start with upper case). Use <treatAsPackage> and <treatAsClass> for exceptions. -->
      <treatAsPackage> <!-- Packages starting with upper case letters. -->
        <package>com.example.MyPackage</package>
      </treatAsPackage>
      <treatAsClass> <!-- Classes starting with lower case letters. -->
        <class>com.example.myClass</class>
      </treatAsClass>
    </importOrder>

    <removeUnusedImports /> <!-- self-explanatory -->

    <formatAnnotations />  <!-- fixes formatting of type annotations, see below -->

    <licenseHeader>
      <content>/* (C)$YEAR */</content>  <!-- or <file>${project.basedir}/license-header</file> -->
    </licenseHeader>
  </java>
</configuration>
```

### removeUnusedImports

```xml
<removeUnusedImports>
  <engine>google-java-format</engine>    <!-- optional. Defaults to `google-java-format`. Can be switched to `cleanthat-javaparser-unnecessaryimport` (e.g. to process JDK17 source files with a JDK8+ Runtime) -->
</removeUnusedImports>
```

### google-java-format

[homepage](https://github.com/google/google-java-format). [changelog](https://github.com/google/google-java-format/releases). [code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/java/GoogleJavaFormat.java).

```xml
<googleJavaFormat>
  <version>1.8</version>                      <!-- optional, 1.8 is the minimum supported version for Java 11 -->
  <style>GOOGLE</style>                       <!-- or AOSP (optional) -->
  <reflowLongStrings>true</reflowLongStrings> <!-- optional -->
  <formatJavadoc>false</formatJavadoc>        <!-- optional -->
  <!-- optional: custom group artifact (you probably don't need this) -->
  <groupArtifact>com.google.googlejavaformat:google-java-format</groupArtifact>
</googleJavaFormat>
```

### palantir-java-format

[homepage](https://github.com/palantir/palantir-java-format). [changelog](https://github.com/palantir/palantir-java-format/releases). [code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/java/PalantirJavaFormat.java).

```xml
<palantirJavaFormat>
  <version>2.39.0</version>                     <!-- optional -->
  <style>PALANTIR</style>                       <!-- or AOSP/GOOGLE (optional) -->
  <formatJavadoc>false</formatJavadoc>          <!-- defaults to false (optional, requires at least Palantir 2.39.0) -->
</palantirJavaFormat>
```

### eclipse jdt

[homepage](https://download.eclipse.org/eclipse/downloads/). [code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/java/Eclipse.java). See [here](../ECLIPSE_SCREENSHOTS.md) for screenshots that demonstrate how to get and install the config file mentioned below.

```xml
<eclipse>
  <version>4.26</version>                     <!-- optional version of Eclipse Formatter -->
  <file>${project.basedir}/eclipse-formatter.xml</file> <!-- optional -->
</eclipse>
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

To fix the incorrect formatting, add the `formatAnnotations` rule after a Java formatter.  For example:

```XML
<googleJavaFormat />
<formatAnnotations />
```

This does not re-order annotations, it just removes incorrect newlines.

A type annotation is an annotation that is meta-annotated with `@Target({ElementType.TYPE_USE})`.
Because Spotless cannot necessarily examine the annotation definition, it uses a hard-coded
list of well-known type annotations.  You can make a pull request to add new ones.
In the future there will be mechanisms to add/remove annotations from the list.
These mechanisms already exist for the Gradle plugin.

### Cleanthat

[homepage](https://github.com/solven-eu/cleanthat). CleanThat enables automatic refactoring of Java code. [ChangeLog](https://github.com/solven-eu/cleanthat/blob/master/CHANGES.MD)

```xml
<cleanthat>
  <version>2.8</version>                          <!-- optional version of Cleanthat -->
  <sourceJdk>${maven.compiler.source}</sourceJdk> <!-- optional. Default to ${maven.compiler.source} else '1.7' -->
  <mutators>
    <mutator>SafeAndConsensual</mutator>          <!-- optional. Default to 'SafeAndConsensual' to include all mutators -->
  </mutators>
  <mutators>            <!-- List of mutators: https://github.com/solven-eu/cleanthat/blob/master/MUTATORS.generated.MD -->
    <mutator>LiteralsFirstInComparisons</mutator> <!-- You may alternatively list the requested mutators -->
  </mutators>
  <excludedMutators>
    <excludedMutator>OptionalNotEmpty</excludedMutator> <!-- You can discard specific rules -->
  </excludedMutators>
  <includeDraft>false</includeDraft>              <!-- optional. Default to false, not to include draft mutators from Composite mutators -->
</cleanthat>
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

    <importOrder/> <!-- standard import order -->
    <importOrder>  <!-- or a custom ordering -->
      <order>java|javax,org,com,com.diffplug,,\#com.diffplug,\#</order>  <!-- or use <file>${project.basedir}/eclipse.importorder</file> -->
      <!-- you can use an empty string for all the imports you didn't specify explicitly, '|' to join group without blank line, and '\#` prefix for static imports. -->
    </importOrder>
    
    <removeSemicolons/> <!-- removes semicolons at the end of lines -->
    <greclipse/>          <!-- has its own section below -->

    <excludeJava/>
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
  <version>4.26</version>  <!-- optional version of Eclipse Formatter -->
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

[homepage](https://github.com/facebook/ktfmt). [changelog](https://github.com/facebook/ktfmt/releases). [code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/kotlin/Ktfmt.java).

```xml
<ktfmt>
  <version>0.51</version> <!-- optional -->
  <style>KOTLINLANG</style> <!-- optional, options are META (default), GOOGLE and KOTLINLANG -->
  <maxWidth>120</maxWidth> <!-- optional -->
  <blockIndent>4</blockIndent> <!-- optional -->
  <continuationIndent>8</continuationIndent> <!-- optional -->
  <removeUnusedImports>false</removeUnusedImports> <!-- optional -->
  <manageTrailingCommas>true</manageTrailingCommas> <!-- optional -->
</ktfmt>
```

<a name="applying-ktlint-to-kotlin-files"></a>

### ktlint

[homepage](https://github.com/pinterest/ktlint). [changelog](https://github.com/pinterest/ktlint/releases).
[code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/kotlin/Ktlint.java).

Spotless respects the `.editorconfig` settings by providing `editorConfigPath` option.
([ktlint docs](https://github.com/pinterest/ktlint#editorconfig)).

Additionally, `editorConfigOverride` options will override what's supplied in `.editorconfig` file.

```xml
<ktlint>
  <version>1.0.0</version> <!-- optional -->
  <editorConfigOverride> <!-- optional -->
    <ij_kotlin_allow_trailing_comma>true</ij_kotlin_allow_trailing_comma>
    <ij_kotlin_allow_trailing_comma_on_call_site>true</ij_kotlin_allow_trailing_comma_on_call_site>
    <!-- intellij_idea is the default style we preset in Spotless, you can override it referring to https://pinterest.github.io/ktlint/latest/rules/code-styles. -->
    <ktlint_code_style>intellij_idea</ktlint_code_style>
  </editorConfigOverride>
  <customRuleSets> <!-- optional -->
    <value>io.nlopez.compose.rules:ktlint:0.3.3</value>
  </customRuleSets>
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
  <version>3.5.9</version>              <!-- optional -->
  <file>${project.basedir}/scalafmt.conf</file> <!-- optional -->
  <scalaMajorVersion>2.13</scalaMajorVersion> <!-- optional -->
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
      <include>src/native/**</include>
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
  <version>11.0</version> <!-- optional version of Eclipse Formatter, others at https://download.eclipse.org/tools/cdt/releases/ -->
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

    <antlr4Formatter /> <!-- has its own section below -->

    <licenseHeader>
      <content>/* (C)$YEAR */</content>  <!-- or <file>${project.basedir}/license-header</file> -->
    </licenseHeader>
  </antlr4>
</configuration>
```

### antlr4Formatter

[homepage](https://github.com/antlr/Antlr4Formatter). [available versions](https://search.maven.org/artifact/com.khubla.antlr4formatter/antlr4-formatter). [code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/antlr4/Antlr4Formatter.java).

```xml
<antlr4Formatter>
  <version>1.2.1</version> <!-- optional -->
</antlr4Formatter>
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
    <configFile>dbeaver.properties</configFile> <!-- configFile is optional -->
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

  <expandEmptyElements>true</expandEmptyElements> <!-- Should empty elements be expanded -->

  <spaceBeforeCloseEmptyElement>false</spaceBeforeCloseEmptyElement> <!-- Should a space be added inside self-closing elements -->

  <keepBlankLines>true</keepBlankLines> <!-- Keep empty lines -->

  <endWithNewline>true</endWithNewline> <!-- Whether sorted pom ends with a newline -->

  <nrOfIndentSpace>2</nrOfIndentSpace> <!-- Indentation -->

  <indentBlankLines>false</indentBlankLines> <!-- Should empty lines be indented -->

  <indentSchemaLocation>false</indentSchemaLocation> <!-- Should schema locations be indented -->

  <indentAttribute></indentAttribute> <!-- Should the xml attributes be indented -->

  <predefinedSortOrder>recommended_2008_06</predefinedSortOrder> <!-- Sort order of elements: https://github.com/Ekryd/sortpom/wiki/PredefinedSortOrderProfiles -->

  <sortOrderFile></sortOrderFile> <!-- Custom sort order of elements: https://raw.githubusercontent.com/Ekryd/sortpom/master/sorter/src/main/resources/custom_1.xml -->

  <sortDependencies></sortDependencies> <!-- Sort dependencies: https://github.com/Ekryd/sortpom/wiki/SortDependencies -->

  <sortDependencyManagement></sortDependencyManagement> <!-- Sort dependency management: https://github.com/Ekryd/sortpom/wiki/SortDependencies -->

  <sortDependencyExclusions></sortDependencyExclusions> <!-- Sort dependency exclusions: https://github.com/Ekryd/sortpom/wiki/SortDependencies -->

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
    <eslint/>   <!-- has its own section below -->
    <biome/>    <!-- has its own section below -->

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

For details, see the [npm detection](#npm-detection), [`.npmrc` detection](#npmrc-detection) and [caching results of `npm install`](#caching-results-of-npm-install) sections of prettier, which apply also to tsfmt.

### ESLint (typescript)

[npm](https://www.npmjs.com/package/eslint). [changelog](https://github.com/eslint/eslint/blob/main/CHANGELOG.md). *Please note:*
The auto-discovery of config files (up the file tree) will not work when using ESLint within spotless,
hence you are required to provide resolvable file paths for config files, or alternatively provide the configuration inline.

The configuration is very similar to the [ESLint (Javascript)](#eslint-javascript) configuration. In typescript, a
reference to a `tsconfig.json` is required.

```xml
<eslint>
  <!-- Specify at most one of the following 3 configs: either 'eslintVersion', 'devDependencies' or 'devDependencyProperties'  -->
  <eslintVersion>8.30.0</eslintVersion>
  <devDependencies>
    <myEslintFork>8.30.0</myEslintFork>
    <myEslintPlugin>1.2.1</myEslintPlugin>
  </devDependencies>
  <devDependencyProperties>
    <property>
      <name>eslint</name>
      <value>8.30.0</value>
    </property>
    <property>
      <name>@eslint/my-plugin-typescript</name> <!-- this could not be written in the simpler to write 'devDependencies' element. -->
      <value>0.14.2</value>
    </property>
  </devDependencyProperties>
  <!-- mandatory: provide either a configFile or a configJs object -->
  <configFile>${project.basedir}/.eslintrc.js</configFile>
  <configJs>
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
  </configJs>
  <!-- recommended: provide a tsconfigFile - especially when using the styleguides -->
  <tsconfigFile>${project.basedir}/tsconfig.json</tsconfigFile>
</eslint>
```

**Prerequisite: ESLint requires a working NodeJS version**

For details, see the [npm detection](#npm-detection), [`.npmrc` detection](#npmrc-detection) and [caching results of `npm install`](#caching-results-of-npm-install) sections of prettier, which apply also to ESLint.


## Javascript

[code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/javascript/Javascript.java). [available steps](https://github.com/diffplug/spotless/tree/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/javascript).

```xml
<configuration>
  <typescript>
    <includes> <!-- You have to set the target manually -->
      <include>src/**/*.js</include>
    </includes>

    <prettier/> <!-- has its own section below -->
    <eslint/>   <!-- has its own section below -->
    <biome/>    <!-- has its own section below -->

    <licenseHeader>
      <content>/* (C)$YEAR */</content>  <!-- or <file>${project.basedir}/license-header</file> -->
      <delimiter>REGEX_TO_DEFINE_TOP_OF_FILE</delimiter> <!-- set a regex to define the top of the file -->
    </licenseHeader>
  </typescript>
</configuration>
```

### ESLint (Javascript)

[npm](https://www.npmjs.com/package/eslint). [changelog](https://github.com/eslint/eslint/blob/main/CHANGELOG.md). *Please note:*
The auto-discovery of config files (up the file tree) will not work when using ESLint within spotless,
hence you are required to provide resolvable file paths for config files, or alternatively provide the configuration inline.

The configuration is very similar to the [ESLint (Typescript)](#eslint-typescript) configuration. In javascript, *no*
`tsconfig.json` is supported.

```xml
<eslint>
  <!-- Specify at most one of the following 3 configs: either 'eslintVersion', 'devDependencies' or 'devDependencyProperties'  -->
  <eslintVersion>8.30.0</eslintVersion>
  <devDependencies>
    <myEslintFork>8.30.0</myEslintFork>
    <myEslintPlugin>1.2.1</myEslintPlugin>
  </devDependencies>
  <devDependencyProperties>
    <property>
      <name>eslint</name>
      <value>8.30.0</value>
    </property>
    <property>
      <name>@eslint/my-plugin-javascript</name> <!-- this could not be written in the simpler to write 'devDependencies' element. -->
      <value>0.14.2</value>
    </property>
  </devDependencyProperties>
  <!-- mandatory: provide either a configFile or a configJs object -->
  <configFile>${project.basedir}/.eslintrc.js</configFile>
  <configJs>
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
  </configJs>
</eslint>
```

**Prerequisite: ESLint requires a working NodeJS version**

For details, see the [npm detection](#npm-detection), [`.npmrc` detection](#npmrc-detection) and [caching results of `npm install`](#caching-results-of-npm-install) sections of prettier, which apply also to ESLint.

## JSON

- `com.diffplug.spotless.maven.json.Json` [code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/json/Json.java)

```xml
<configuration>
  <json>
    <includes>    <!-- You have to set the target manually -->
      <include>src/**/*.json</include>
    </includes>

    <simple />         <!-- has its own section below -->
    <gson />           <!-- has its own section below -->
    <jackson />        <!-- has its own section below -->
    <biome />          <!-- has its own section below -->
    <jsonPatch />      <!-- has its own section below -->
  </json>
</configuration>
```

### simple

Uses a JSON pretty-printer that optionally allows configuring the number of spaces that are used to pretty print objects:

```xml
<simple>
  <indentSpaces>4</indentSpaces>    <!-- optional: specify the number of spaces to use -->
</simple>
```

### Gson

Uses Google Gson to also allow sorting by keys besides custom indentation - useful for i18n files.

```xml
<gson>
  <indentSpaces>4</indentSpaces>        <!-- optional: specify the number of spaces to use -->
  <sortByKeys>false</sortByKeys>        <!-- optional: sort JSON by its keys -->
  <escapeHtml>false</indentSpaces>      <!-- optional: escape HTML in values -->
  <version>2.8.1</version>              <!-- optional: specify version -->
</gson>
```

Notes:
* There's no option in Gson to leave HTML as-is (i.e. escaped HTML would remain escaped, raw would remain raw). Either
all HTML characters are written escaped or none. Set `escapeHtml` if you prefer the former.
* `sortByKeys` will apply lexicographic order on the keys of the input JSON. See the
[javadoc of String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html#compareTo(java.lang.String))
for details.

### Jackson

Uses Jackson for formatting.

```xml
<jackson>
  <version>2.14.1</version>    <!-- optional: The version of 'com.fasterxml.jackson.core:jackson-databind' to be used -->
  <features>                   <!-- optional: Customize the set of features (based on com.fasterxml.jackson.databind.SerializationFeature) -->
    <INDENT_OUTPUT>true</INDENT_OUTPUT>                            <!-- optional: true by default -->
    <ORDER_MAP_ENTRIES_BY_KEYS>false</ORDER_MAP_ENTRIES_BY_KEYS>   <!-- optional: false by default -->
    <ANY_OTHER_FEATURE>true|false</ANY_OTHER_FEATURE>              <!-- any SerializationFeature can be toggled on or off -->
  </features>
  <jsonFeatures>
    <QUOTE_FIELD_NAMES>false</QUOTE_FIELD_NAMES>                   <!-- false by default -->
    <ANY_OTHER_FEATURE>true|false</ANY_OTHER_FEATURE>              <!-- any JsonGenerator.Feature can be toggled on or off -->
  </jsonFeatures>
  <spaceBeforeSeparator>false</spaceBeforeSeparator>               <!-- optional: false by default -->
</jackson>
```

<a name="applying-prettier-to-javascript--flow--typescript--css--scss--less--jsx--graphql--yaml--etc"></a>

### jsonPatch

Uses [zjsonpatch](https://github.com/flipkart-incubator/zjsonpatch) to apply [JSON Patches](https://jsonpatch.com/) as per [RFC 6902](https://datatracker.ietf.org/doc/html/rfc6902/) to JSON documents.

This enables you to add, replace or remove properties at locations in the JSON document that you specify using [JSON Pointers](https://datatracker.ietf.org/doc/html/rfc6901/).

For example, to apply the patch from the [JSON Patch homepage](https://jsonpatch.com/#the-patch):

```xml
<jsonPatch>[
  { "op": "replace", "path": "/baz", "value": "boo" },
  { "op": "add", "path": "/hello", "value": ["world"] },
  { "op": "remove", "path": "/foo" }
]</jsonPatch>
```

## YAML

- `com.diffplug.spotless.maven.FormatterFactory.addStepFactory(FormatterStepFactory)` [code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/yaml/Yaml.java)

```xml
<configuration>
  <yaml>
    <includes>     <!-- You have to set the target manually -->
      <include>src/**/*.yaml</include>
    </includes>

    <jackson />    <!-- has its own section below -->
    <prettier />   <!-- has its own section below -->
  </yaml>
</configuration>
```

### jackson

Uses Jackson and YAMLFactory to pretty print objects:

```xml
<jackson>
  <version>2.14.1</version>    <!-- optional: The version of 'com.fasterxml.jackson.dataformat:jackson-dataformat-yaml' to be used -->
  <features>                   <!-- optional: Customize the set of features (based on com.fasterxml.jackson.databind.SerializationFeature) -->
    <INDENT_OUTPUT>true</INDENT_OUTPUT>                            <!-- true by default -->
    <ORDER_MAP_ENTRIES_BY_KEYS>false</ORDER_MAP_ENTRIES_BY_KEYS>   <!-- false by default -->
    <ANY_OTHER_FEATURE>true|false</ANY_OTHER_FEATURE>              <!-- any SerializationFeature can be toggled on or off -->
  </features>
  <yamlFeatures>
    <WRITE_DOC_START_MARKER>true</WRITE_DOC_START_MARKER>          <!-- false by default -->
    <MINIMIZE_QUOTES>false</MINIMIZE_QUOTES>                       <!-- false by default -->
    <ANY_OTHER_FEATURE>true|false</ANY_OTHER_FEATURE>              <!-- any YAMLGenerator.Feature can be toggled on or off -->
  </yamlFeatures>
  <spaceBeforeSeparator>false</spaceBeforeSeparator>   <!-- optional: false by default -->
</jackson>
```

## Shell

- `com.diffplug.spotless.maven.FormatterFactory.addStepFactory(FormatterStepFactory)` [code](./src/main/java/com/diffplug/spotless/maven/shell/Shell.java)

```xml
<configuration>
  <shell>
    <includes>     <!-- Not required. Defaults to **/*.sh -->
      <include>scripts/**/*.sh</include>
    </includes>
    
    <shfmt />    <!-- has its own section below -->
  </shell>
</configuration>
```

### shfmt

[homepage](https://github.com/mvdan/sh). [changelog](https://github.com/mvdan/sh/blob/master/CHANGELOG.md).

When formatting shell scripts via `shfmt`, configure `shfmt` settings via `.editorconfig`.

```xml
<shfmt>
  <version>3.8.0</version>                         <!-- optional: Custom version of 'mvdan/sh' -->
  <pathToExe>/opt/homebrew/bin/shfmt</pathToExe>   <!-- optional: if shfmt is not on your path, you must specify its location manually -->
</shfmt>
```

## Gherkin

- `com.diffplug.spotless.maven.FormatterFactory.addStepFactory(FormatterStepFactory)` [code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/gherkin/Gherkin.java)

```xml
<configuration>
  <gherkin>
    <includes>     <!-- You have to set the target manually -->
      <include>src/**/*.feature</include>
    </includes>

    <gherkinUtils />    <!-- has its own section below -->
  </gherkin>
</configuration>
```

### gherkinUtils

[homepage](https://github.com/cucumber/gherkin-utils). [changelog](https://github.com/cucumber/gherkin-utils/blob/main/CHANGELOG.md).

Uses a Gherkin pretty-printer that optionally allows configuring the number of spaces that are used to pretty print objects:

```xml
<gherkinUtils>
  <version>9.0.0</version>                 <!-- optional: Custom version of 'io.cucumber:gherkin-utils' -->
</gherkinUtils>
```

## Go

- `com.diffplug.spotless.maven.FormatterFactory.addStepFactory(FormatterStepFactory)` [code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/go/Go.java)

```xml
<configuration>
  <go>
    <includes>     <!-- You have to set the target manually -->
      <include>src/**/*.go</include>
    </includes>

    <gofmt />    <!-- has its own section below -->
  </go>
</configuration>
```

### gofmt

Standard Go formatter, part of Go distribution.

```xml
<gofmt>
  <version>go1.25.1</version>
  <goExecutablePath>/opt/sdks/go1.25.1/bin/go</goExecutablePath>
</gofmt>
```

## RDF

### Generic Options

List of generic configuration `parameters (type/default)` 

* `failOnWarning (boolean/true)`: The Jena parser produces three levels of problem reports: warning, error, and fatal. By default, 
the build fails for any of them. You can ignore warnings using this parameter. They will still be logged in the plugin's 
output.
* `verify (boolean/true)`: If `true`, the content before and after formatting is parsed to an RDF model and compared for isomorphicity.   
* `turtleFormatterVersion (string|RdfFormatterStep.LATEST_TURTLE_FORMATTER_VERSION)`: the version of turtle-formatter to use (see below).

### Supported RDF formats: only TTL (at the moment)

Formatting TTL is done using [turtle-formatter](https://github.com/atextor/turtle-formatter),
which is highly configurable (have a look at the [Style Documentation](https://github.com/atextor/turtle-formatter?tab=readme-ov-file#customizing-the-style)) 
and will handle blank nodes the way you'd hope.

The style options can be configured via spotless. Wherever the style wants a URI (for example, for the `predicateOrder`, you can 
use the abbreviated form if it is a `FormattingStyle.KnownPrefix` (currently `rdf`, `rdfs`, `xsd`, `owl`, `dcterms`)
Error messages will give you hints. To configure the TTL formatting style, pass the configuration parameters under `<turtle>`

### Examples
Minimal:
```xml
<configuration>
  <rdf>
    <includes>
      <include>**/*.ttl</include>
    </includes>
    <format/>
  </rdf>
</configuration>
```
Configuring some generic and TTL options:
```xml
<configuration>
  <rdf>
    <includes>
      <include>**/*.ttl</include>
    </includes>
    <format>
      <failOnWarning>false</failOnWarning>
      <verify>false</verify>
      <turtleFormatterVersion>1.2.13</turtleFormatterVersion>
      <turtle>
        <alignPrefixes>RIGHT</alignPrefixes>
        <enableDoubleFormatting>true</enableDoubleFormatting>
      </turtle>
    </format>
  </rdf>
</configuration>
```
### Libraries and versions

RDF parsing is done via [Apache Jena](https://jena.apache.org/) in the version that
[turtle-formatter](https://github.com/atextor/turtle-formatter) depends on (not necessarily the latest).

## CSS

[code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/css/Css.java). [available steps](https://github.com/diffplug/spotless/tree/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/css).

```xml
<configuration>
  <css>
    <!-- These are the defaults, you can override if you want -->
    <includes>
      <include>src/main/css/**/*.css</include>
      <include>src/test/css/**/*.css</include>
    </includes>

    <biome />          <!-- has its own section below -->

    <licenseHeader>
      <content>/* (C)$YEAR */</content>  <!-- or <file>${project.basedir}/license-header</file> -->
    </licenseHeader>
  </css>
</configuration>
```

Note regarding biome: Biome supports formatting CSS as of 1.8.0 (experimental, opt-in) and 1.9.0 (stable).

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
            <value>2.8.8</value>
          </property>
          <property>
            <name>@prettier/plugin-php</name> <!-- this could not be written in the simpler to write 'devDependencies' element. -->
            <value>0.19.6</value>
          </property>
        </devDependencyProperties>
        <!-- Specify config file and/or inline config, the inline always trumps file -->
        <configFile>${project.basedir}/path/to/configfile</configFile>
        <config>
            <useTabs>true</useTabs>
            <!-- Prettier v3 Only - Comma Delimited -->
            <plugins>@prettier/plugin-php</plugins>
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
            <prettier>2.8.8</prettier>
            <prettier-plugin-java>2.2.0</prettier-plugin-java>
        </devDependencies>
        <config>
            <tabWidth>4</tabWidth>
            <parser>java</parser>
            <plugins>prettier-plugin-java</plugins><!-- this is only for prettier 3.0.0 and above: an additional 'plugins' config element is required -->
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
            <value>2.8.8</value>
          </property>
          <property>
            <name>@prettier/plugin-php</name>
            <value>0.19.6</value>
          </property>
        </devDependencyProperties>
        <config>
            <tabWidth>3</tabWidth>
            <parser>php</parser>
            <plugins>@prettier/plugin-php</plugins><!-- this is only for prettier 3.0.0 and above: an additional 'plugins' config element is required -->
        </config>
      </prettier>
    </format>

  </formats>
</configuration>
```

### npm detection

Prettier is based on NodeJS, so to use it, a working NodeJS installation (especially npm) is required on the host running spotless.
Spotless will try to auto-discover an npm installation. If that is not working for you, it is possible to directly configure the npm
and/or node binary to use.

```xml
<prettier>
  <npmExecutable>/usr/bin/npm</npmExecutable>
  <nodeExecutable>/usr/bin/node</nodeExecutable>
```

If you provide both `npmExecutable` and `nodeExecutable`, spotless will use these paths. If you specify only one of the
two, spotless will assume the other one is in the same directory.

### `.npmrc` detection

Spotless picks up npm configuration stored in a `.npmrc` file either in the project directory or in your user home.
Alternatively you can supply spotless with a location of the `.npmrc` file to use. (This can be combined with
`npmExecutable` and `nodeExecutable`, of course.)

```xml
<prettier>
  <npmrc>/usr/local/shared/.npmrc</npmrc>
```

### Caching results of `npm install`

Spotless uses `npm` behind the scenes to install `prettier`. This can be a slow process, especially if you are using a slow internet connection or
if you need large plugins. You can instruct spotless to cache the results of the `npm install` calls, so that for the next installation,
it will not need to download the packages again, but instead reuse the cached version.

```xml
<prettier>
  <npmInstallCache>true</npmInstallCache> <!-- will use the default cache directory (the `target`-directory) -->
  <npmInstallCache>/usr/local/shared/.spotless-npm-install-cache</npmInstallCache> <!-- will use the specified directory (creating it if not existing) -->
```

Depending on your filesystem and the location of the cache directory, spotless will use hardlinks when caching the npm packages. If that is not
possible, it will fall back to copying the files.

<a name="applying-eclipse-wtp-to-css--html--etc"></a>

## Eclipse web tools platform

[changelog](https://www.eclipse.org/webtools/). [compatible versions](https://github.com/diffplug/spotless/tree/main/lib-extra/src/main/resources/com/diffplug/spotless/extra/eclipse_wtp_formatter).

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
        <version>4.21.0</version> <!-- optional version of Eclipse Formatter -->
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

## Biome

[homepage](https://biomejs.dev/). [changelog](https://github.com/biomejs/biome/blob/main/CHANGELOG.md). Biome is
a formatter that for the frontend written in Rust, which has a native binary, does not require Node.js and as such,
is pretty fast. It can currently format JavaScript, TypeScript, JSX, and JSON, and may support
[more frontend languages](https://biomejs.dev/internals/language-support/) such as CSS in the future.

You can use Biome in any language-specific format for supported languages, but
usually you will be creating a generic format.

Note regarding CSS: Biome supports formatting CSS as of 1.8.0 (experimental, opt-in) and 1.9.0 (stable).

```xml
<configuration>
  <formats>
    <format>
      <includes>
        <include>src/**/typescript/**/*.ts</include>
      </includes>

      <biome>
        <!-- Download Biome from the network if not already downloaded, see below for more info  -->
        <version>1.2.0</version>

        <!-- (optional) Path to the directory with the biome.json conig file -->
        <configPath>${project.basedir}/path/to/config/dir</configPath>

        <!-- (optional) Biome will auto-detect the language based on the file extension. -->
        <!-- See below for possible values. -->
        <language>ts</language>
      </prettier>
    </biome>

  </formats>
</configuration>
```

To apply Biome to more kinds of files with a different configuration, just add
more formats:

```xml
<configuration>
  <formats>
    <format><includes>src/**/*.ts</includes><biome/></format>
    <format><includes>src/**/*.js</includes><biome/></format>
</configuration>
```

**Limitations:**
- The auto-discovery of config files (up the file tree) will not work when using
  Biome within spotless.
- The `ignore` option of the `biome.json` configuration file will not be applied.
  Include and exclude patterns are configured in the spotless configuration in the
  Maven pom instead.

Note: Due to a limitation of biome, if the name of a file matches a pattern in
the `ignore` option of the specified `biome.json` configuration file, it will not be
formatted, even if included in the biome configuration section of the Maven pom.
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

```xml
<biome>
  <version>1.2.0</version>
</biome>
```

Spotless uses a default version when you do not specify a version, but this
may change at any time, so we recommend that you always set the Biome version
you want to use. Optionally, you can also specify a directory for the downloaded
Biome binaries (defaults to `~/.m2/repository/com/diffplug/spotless/spotless-data/biome`):

```xml
<biome>
  <version>1.2.0</version>
  <!-- Relative paths are resolved against the project's base directory -->
  <downloadDir>${user.home}/biome</downloadDir>
</biome>
```

To use a fixed binary, omit the `version` and specify a `pathToExe`:

```xml
<biome>
  <pathToExe>${project.basedir}/bin/biome</pathToExe>
</biome>
```

Absolute paths are used as-is. Relative paths are resolved against the project's
base directory. To use a pre-installed Biome binary on the user's path, specify
just a name without any slashes / backslashes:


```xml
<biome>
  <!-- Uses the "biome" command, which must be on the user's path. -->
  <pathToExe>biome</pathToExe>
</biome>
```

### Biome configuration file

Biome is a biased formatter and linter without many options, but there are a few
basic options. Biome uses a file named [biome.json](https://biomejs.dev/reference/configuration/)
for its configuration. When none is specified, the default configuration from
Biome is used. To use a custom configuration:

```xml
<biome>
  <!-- Must point to the directory with the "biome.json" config file -->
  <!-- Relative paths are resolved against the project's base directory -->
  <configPath>${project.basedir}</configPath>
</biome>
```

### Biome input language

By default, Biome detects the language / syntax of the files to format
automatically from the file extension. This may fail if your source code files
have unusual extensions for some reason. If you are using the generic format,
you can force a certain language like this:

```xml
<configuration>
  <formats>
    <format>
      <includes>
        <include>src/**/typescript/**/*.mjson</include>
      </includes>

      <biome>
        <version>1.2.0</version>
        <language>json</language>
      </biome>
    </format>
  </formats>
</configuration>
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

```xml
<trimTrailingWhitespace /> <!-- trim trailing whitespaces -->

<endWithNewline /> <!-- files must end with a newline -->

<indent> <!-- specify whether to use tabs or spaces for indentation -->
  <spaces>true</spaces> <!-- or <tabs>true</tabs> -->
  <spacesPerTab>4</spacesPerTab> <!-- optional, default is 4 -->
</indent>

<jsr223> <!-- specify replacements using JSR223 scripting -->
  <name>Greetings to Mars</name>
  <dependency>org.codehaus.groovy:groovy-jsr223:3.0.9</dependency> <!-- optional, maven dependency, containing the jsr223 compatible scripting engine -->
  <engine>groovy</engine> <!-- nashorn is provided by JDK 8-14, other engines can be loaded from the given dependency -->
  <script>source.replace('World','Mars');</script> <!-- the source variable contains the unformatted code, the returned value of the script is the formatted code -->
</jsr223>

<nativeCmd> <!-- run a native binary -->
  <name>Greetings to Mars from sed</name>
  <pathToExe>/usr/bin/sed</pathToExe> <!-- path to the binary, unformatted code is send via StdIn, formatted code is expected on StdOut -->
  <arguments> <!-- optional, list with arguments for the binary call -->
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

### Files with fixed header lines

Some files have fixed header lines (e.g. `<?xml version="1.0" ...` in XMLs, or `#!/bin/bash` in bash scripts). Comments cannot precede these, so the license header has to come after them, too.

To define what lines to skip at the beginning of such files, fill the `skipLinesMatching` option with a regular expression that matches them (e.g. `<skipLinesMatching>^#!.+?$</skipLinesMatching>` to skip shebangs).

<a name="invisible"></a>

<a name="ratchet"></a>

## Incremental up-to-date checking and formatting

**This feature is enabled by default starting from version 2.35.0.**

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

You can explicitly disable ratchet functionality by providing the value 'NONE':
```xml
<configuration>
  <ratchetFrom>NONE</ratchetFrom>
</configuration>
```
This is useful for disabling the ratchet functionality in child projects where the parent defines a ratchetFrom value.

### Using `ratchetFrom` on CI systems

Many popular CI systems (GitHub, GitLab, BitBucket, and Travis) use a "shallow clone". This means that `<ratchetFrom>origin/main</ratchetFrom>` will fail with `No such reference`. You can fix this by:

- calling `git fetch origin main` before you call Spotless
- disabling the shallow clone [like so](https://github.com/diffplug/spotless/issues/710)

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

Line endings can also be set globally or per-format using the `lineEndings` property.  Spotless supports four line ending modes: `UNIX`, `WINDOWS`, `MAC_CLASSIC`, `PLATFORM_NATIVE`, `GIT_ATTRIBUTES`, and `GIT_ATTRIBUTES_FAST_ALLSAME`.  The default value is `GIT_ATTRIBUTES_FAST_ALLSAME`, and *we highly recommend that you* ***do not change*** *this value*.  Git has opinions about line endings, and if Spotless and git disagree, then you're going to have a bad time. `FAST_ALLSAME` just means that Spotless can assume that every file being formatted has the same line endings ([more info](https://github.com/diffplug/spotless/pull/1838)).

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

## Does Spotless support incremental builds in Eclipse?

Spotless comes with [m2e](https://eclipse.dev/m2e/) support. However, by default its execution is skipped in incremental builds as most developers want to fix all issues in one go via explicit `mvn spotless:apply` prior to raising a PR and don't want to be bothered with Spotless issues during working on the source code in the IDE.
To enable it use the following parameter

```
<configuration>
    <m2eEnableForIncrementalBuild>true</m2eEnableForIncrementalBuild><!-- this is false by default -->
</configuration>
```

In addition Eclipse problem markers are being emitted for goal `check`. By default they have the severity `WARNING`.
You can adjust this with 

```
<configuration>
    <m2eIncrementalBuildMessageSeverity>ERROR</m2eIncrementalBuildMessageSeverity><!-- WARNING or ERROR -->
</configuration>
```

Note that for Incremental build support the goals have to be bound to a phase prior to `test`.

<a name="examples"></a>

## Example configurations (from real-world projects)

- [Apache Avro](https://github.com/apache/avro/blob/8026c8ffe4ef67ab419dba73910636bf2c1a691c/lang/java/pom.xml#L307-L334)
- [JUNG: Java Universal Network/Graph Framework](https://github.com/jrtom/jung/blob/b3a2461b97bb3ab40acc631e21feef74976489e4/pom.xml#L187-L208)

<!---freshmark /javadoc -->
