# <img align="left" src="../_images/spotless_logo.png"> Spotless: Keep your code spotless with Maven

<!---freshmark shields
output = [
  link(shield('Maven central', 'mavencentral', '{{group}}:{{artifactIdMaven}}', 'blue'), 'https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22{{group}}%22%20AND%20a%3A%22{{artifactIdMaven}}%22'),
  link(shield('Javadoc', 'javadoc', '{{versionLast}}', 'blue'), 'https://javadoc.io/doc/com.diffplug.spotless/spotless-maven-plugin/{{versionLast}}/index.html'),
  '',
  link(shield('Changelog', 'changelog', '{{versionLast}}', 'brightgreen'), 'CHANGES.md'),
  link(image('Travis CI', 'https://travis-ci.org/{{org}}/{{name}}.svg?branch=main'), 'https://travis-ci.org/{{org}}/{{name}}'),
  link(shield('Live chat', 'gitter', 'chat', 'brightgreen'), 'https://gitter.im/{{org}}/{{name}}'),
  link(shield('License Apache', 'license', 'apache', 'brightgreen'), 'https://tldrlegal.com/license/apache-license-2.0-(apache-2.0)')
  ].join('\n');
-->
[![Maven central](https://img.shields.io/badge/mavencentral-com.diffplug.spotless%3Aspotless--maven--plugin-blue.svg)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.diffplug.spotless%22%20AND%20a%3A%22spotless-maven-plugin%22)
[![Javadoc](https://img.shields.io/badge/javadoc-2.0.1-blue.svg)](https://javadoc.io/doc/com.diffplug.spotless/spotless-maven-plugin/2.0.1/index.html)

[![Changelog](https://img.shields.io/badge/changelog-2.0.1-brightgreen.svg)](CHANGES.md)
[![Travis CI](https://travis-ci.org/diffplug/spotless.svg?branch=main)](https://travis-ci.org/diffplug/spotless)
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
- **Languages**
  - [Java](#java) ([google-java-format](#google-java-format), [eclipse jdt](#eclipse-jdt), [prettier](#prettier))
  - [Kotlin](#kotlin) ([ktlint](#ktlint), [ktfmt](#ktfmt), [prettier](#prettier))
  - [Scala](#scala) ([scalafmt](#scalafmt))
  - [C/C++](#cc) ([eclipse cdt](#eclipse-cdt))
  - [Antlr4](#antlr4) ([antlr4formatter](#antlr4formatter))
  - [Typescript](#typescript) ([tsfmt](#tsfmt), [prettier](#prettier))
  - Multiple languages
    - [Prettier](#prettier) ([plugins](#prettier-plugins), [npm detection](#npm-detection))
    - [eclipse web tools platform](#eclipse-web-tools-platform)
- **Language independent**
  - [Generic steps](#generic-steps)
  - [License header](#license-header) ([slurp year from git](#retroactively-slurp-years-from-git-history))
  - [How can I enforce formatting gradually? (aka "ratchet")](#ratchet)
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

      <!-- apply a specific flavor of google-java-format -->
      <googleJavaFormat>
        <version>1.8</version>
        <style>AOSP</style>
      </googleJavaFormat>

      <!-- make sure every file has the following copyright header.
        optionally, Spotless can set copyright years by digging
        through git history (see "license" section below) -->
      <licenseHeader>
        <content>/* (C)$YEAR */</content>  <!-- or <file>${basedir}/license-header</file> -->
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
      <order>java,javax,org,com,com.diffplug,</order>  <!-- or use <file>${basedir}/eclipse.importorder</file> -->
      <!-- You probably want an empty string at the end - all of the
           imports you didn't specify explicitly will go there. -->
    </importOrder>

    <removeUnusedImports /> <!-- self-explanatory -->

    <googleJavaFormat /> <!-- has its own section below -->
    <eclipse />          <!-- has its own section below -->
    <prettier />         <!-- has its own section below -->

    <licenseHeader>
      <content>/* (C)$YEAR */</content>  <!-- or <file>${basedir}/license-header</file> -->
    </licenseHeader>
  </java>
</configuration>
```

### google-java-format

[homepage](https://github.com/google/google-java-format). [changelog](https://github.com/google/google-java-format/releases). [code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/java/GoogleJavaFormat.java).

```xml
<googleJavaFormat>
  <version>1.7</version> <!-- optional -->
  <style>GOOGLE</style>  <!-- or AOSP (optional) -->
</googleJavaFormat>
```

### eclipse jdt

[homepage](https://www.eclipse.org/downloads/packages/). [compatible versions](https://github.com/diffplug/spotless/tree/main/lib-extra/src/main/resources/com/diffplug/spotless/extra/eclipse_jdt_formatter). [code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/java/Eclipse.java). See [here](../ECLIPSE_SCREENSHOTS.md) for screenshots that demonstrate how to get and install the config file mentioned below.

```xml
<eclipse>
  <version>4.13.0</version>                     <!-- optional -->
  <file>${basedir}/eclipse-formatter.xml</file> <!-- optional -->
</eclipse>
```

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

    <importOrder/> <!-- standard import order -->
    <importOrder>  <!-- or a custom ordering -->
      <order>java,javax,org,com,com.diffplug,</order>  <!-- or use <file>${basedir}/eclipse.importorder</file> -->
      <!-- You probably want an empty string at the end - all of the
           imports you didn't specify explicitly will go there. -->
    </importOrder>

    <removeUnusedImports /> <!-- self-explanatory -->

    <ktlint />   <!-- has its own section below -->
    <ktfmt />    <!-- has its own section below -->
    <prettierv/> <!-- has its own section below -->

    <licenseHeader>
      <content>/* (C)$YEAR */</content>  <!-- or <file>${basedir}/license-header</file> -->
    </licenseHeader>
  </kotlin>
</configuration>
```

<a name="applying-ktlint-to-kotlin-files"></a>

### ktlint

[homepage](https://github.com/pinterest/ktlint). [changelog](https://github.com/pinterest/ktlint/releases). [code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/kotlin/Ktlint.java).  Spotless does not ([yet](https://github.com/diffplug/spotless/issues/142)) respect the `.editorconfig` settings.

```xml
<ktlint>
  <version>0.37.2</version> <!-- optional -->
</ktlint>
```

<a name="applying-ktfmt-to-kotlin-files"></a>

### ktfmt

[homepage](https://github.com/facebookincubator/ktfmt). [changelog](https://github.com/facebookincubator/ktfmt/releases). [code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/kotlin/Ktfmt.java).

```xml
<ktfmt>
  <version>0.13</version> <!-- optional -->
</ktfmt>
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
      <content>/* (C)$YEAR */</content>  <!-- or <file>${basedir}/license-header</file> -->
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
  <file>${basedir}/scalafmt.conf</file> <!-- optional -->
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
      <content>/* (C)$YEAR */</content>  <!-- or <file>${basedir}/license-header</file> -->
    </licenseHeader>
  </cpp>
</configuration>
```

### eclipse cdt

[homepage](https://www.eclipse.org/cdt/). [compatible versions](https://github.com/diffplug/spotless/tree/main/lib-extra/src/main/resources/com/diffplug/spotless/extra/eclipse_cdt_formatter). [code](https://github.com/diffplug/spotless/blob/main/plugin-maven/src/main/java/com/diffplug/spotless/maven/cpp/EclipseCdt.java).

```xml
<eclipseCdt>
  <version>4.13.0</version>               <!-- optional -->
  <file>${basedir}/eclipse-cdt.xml</file> <!-- optional -->
</eclipseCdt>
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
      <content>/* (C)$YEAR */</content>  <!-- or <file>${basedir}/license-header</file> -->
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
      <content>/* (C)$YEAR */</content>  <!-- or <file>${basedir}/license-header</file> -->
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
  <tsconfigFile>${basedir}/tsconfig.json</tsconfigFile>
  <tslintFile>${basedir}/tslint.json</tslintFile>
  <vscodeFile>${basedir}/vscode.json</vscodeFile>
  <tsfmtFile>${basedir}/tsfmt.json</tsfmtFile>
</tsfmt>
```

**Prerequisite: tsfmt requires a working NodeJS version**

tsfmt is based on NodeJS, so to use it, a working NodeJS installation (especially npm) is required on the host running spotless.
Spotless will try to auto-discover an npm installation. If that is not working for you, it is possible to directly configure the npm binary to use.

```xml
<tsfmt>
  <npmExecutable>/usr/bin/npm</npmExecutable>
</tsfmt>
```

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
        <configFile>${basedir}/path/to/configfile</configFile>
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
          <file>${basedir}/xml.prefs</file>
          <file>${basedir}/additional.properties</file>
        </files>
        <version>4.7.3a</version> <!-- optional -->
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
  <content>/* (C)$YEAR */</content>  <!-- or <file>${basedir}/license-header</file> -->
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

## How can I enforce formatting gradually? (aka "ratchet")

If your project is not currently enforcing formatting, then it can be a noisy transition.  Having a giant commit where every single file gets changed makes the history harder to read.  To address this, you can use the `ratchet` feature:

```xml
<configuration>
  <ratchetFrom>origin/main</ratchetFrom> <!-- only format files which have changed since origin/main -->
  <!-- ... define formats ... -->
</configuration>
```

In this mode, Spotless will apply only to files which have changed since `origin/main`.  You can ratchet from [any point you want](https://javadoc.io/static/org.eclipse.jgit/org.eclipse.jgit/5.6.1.202002131546-r/org/eclipse/jgit/lib/Repository.html#resolve-java.lang.String-), even `HEAD`.  You can also set `ratchetFrom` per-format if you prefer (e.g. `<configuration><java><ratchetFrom>...`).

However, we strongly recommend that you use a non-local branch, such as a tag or `origin/main`.  The problem with `HEAD` or any local branch is that as soon as you commit a file, that is now the canonical formatting, even if it was formatted incorrectly.  By instead specifying `origin/main` or a tag, your CI server will fail unless every changed file is at least as good or better than it was before the change.

This is especially helpful for injecting accurate copyright dates using the [license step](#license-header).


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
