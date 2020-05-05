# <img align="left" src="../_images/spotless_logo.png"> Spotless: Keep your code spotless with Maven

<!---freshmark shields
output = [
  link(shield('Maven central', 'mavencentral', '{{group}}:{{artifactIdMaven}}', 'blue'), 'https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22{{group}}%22%20AND%20a%3A%22{{artifactIdMaven}}%22'),
  link(shield('Javadoc', 'javadoc', '{{versionLast}}', 'blue'), 'https://javadoc.io/doc/com.diffplug.spotless/spotless-maven-plugin/{{versionLast}}/index.html'),
  '',
  link(shield('Changelog', 'changelog', '{{versionLast}}', 'brightgreen'), 'CHANGES.md'),
  link(image('Travis CI', 'https://travis-ci.org/{{org}}/{{name}}.svg?branch=master'), 'https://travis-ci.org/{{org}}/{{name}}'),
  link(shield('Live chat', 'gitter', 'chat', 'brightgreen'), 'https://gitter.im/{{org}}/{{name}}'),
  link(shield('License Apache', 'license', 'apache', 'brightgreen'), 'https://tldrlegal.com/license/apache-license-2.0-(apache-2.0)')
  ].join('\n');
-->
[![Maven central](https://img.shields.io/badge/mavencentral-com.diffplug.spotless%3Aspotless--maven--plugin-blue.svg)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.diffplug.spotless%22%20AND%20a%3A%22spotless-maven-plugin%22)
[![Javadoc](https://img.shields.io/badge/javadoc-1.30.0-blue.svg)](https://javadoc.io/doc/com.diffplug.spotless/spotless-maven-plugin/1.30.0/index.html)

[![Changelog](https://img.shields.io/badge/changelog-1.30.0-brightgreen.svg)](CHANGES.md)
[![Travis CI](https://travis-ci.org/diffplug/spotless.svg?branch=master)](https://travis-ci.org/diffplug/spotless)
[![Live chat](https://img.shields.io/badge/gitter-chat-brightgreen.svg)](https://gitter.im/diffplug/spotless)
[![License Apache](https://img.shields.io/badge/license-apache-brightgreen.svg)](https://tldrlegal.com/license/apache-license-2.0-(apache-2.0))
<!---freshmark /shields -->

<!---freshmark javadoc
output = prefixDelimiterReplace(input, 'https://{{org}}.github.io/{{name}}/javadoc/spotless-plugin-maven/', '/', versionLast)
-->

Spotless is a general-purpose formatting plugin.  It is completely à la carte, but also includes powerful "batteries-included" if you opt-in. Plugin requires a version of Maven higher or equal to 3.1.0.

To people who use your build, it looks like this:

```
cmd> mvn spotless:check
...
[ERROR] ... The following files had format violations:
[ERROR]  src\main\java\com\diffplug\gradle\spotless\FormatExtension.java
[ERROR]    @@ -109,7 +109,7 @@
[ERROR]    ...
[ERROR]    -\t\t····if·(targets.length·==·0)·{
[ERROR]    +\t\tif·(targets.length·==·0)·{
[ERROR]    ...
[ERROR]  Run 'mvn spotless:apply' to fix these violations.
...

cmd> mvn spotless:apply
...
[INFO] BUILD SUCCESS
...

cmd> mvn spotless:check
...
[INFO] BUILD SUCCESS
...
```

To use it in your pom, just [add the Spotless dependency](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.diffplug.spotless%22%20AND%20a%3A%22spotless-maven-plugin%22), and configure it like so:

```xml
<plugin>
  <groupId>com.diffplug.spotless</groupId>
  <artifactId>spotless-maven-plugin</artifactId>
  <version>${spotless.version}</version>
  <configuration>
    <java>
      <eclipse>
        <file>${basedir}/eclipse-fmt.xml</file>
        <version>4.7.1</version>
      </eclipse>
    </java>
  </configuration>
</plugin>
```

Spotless supports the following powerful formatters:

* Eclipse's java code formatter (including style and import ordering)
* Eclipse's [CDT](https://www.eclipse.org/cdt/) C/C++ code formatter
* Eclipse's [WTP](https://www.eclipse.org/webtools/) Web-Tools code formatters
* Google's [google-java-format](https://github.com/google/google-java-format)
* [Typescript Tsfmt formatter](https://github.com/vvakame/typescript-formatter)
* [Prettier formatter](https://prettier.io)
* User-defined license enforcement, regex replacement, etc.

Contributions are welcome, see [the contributing guide](../CONTRIBUTING.md) for development info.

Spotless requires Maven to be running on JRE 8+.

<a name="java"></a>

## Applying to Java source

By default, all files matching `src/main/java/**/*.java` and `src/test/java/**/*.java` Ant style pattern will be formatted.  Each element under `<java>` is a step, and they will be applied in the order specified.  Every step is optional, and they will be applied in the order specified.  It doesn't make sense to use both eclipse and google-java-format.

```xml
<configuration>
  <java>
     <licenseHeader>
       <!-- Specify either content or file, but not both -->
       <content>/* Licensed under Apache-2.0 */</content>
       <file>${basedir}/license-header</file>
     </licenseHeader>
     <eclipse>
       <!-- Optional, otherwise Eclipse defaults are used. Eclipse preference or property files are also supported. -->
       <file>${basedir}/eclipse-format.xml</file>
       <!-- Optional, available versions: https://github.com/diffplug/spotless/tree/master/lib-extra/src/main/resources/com/diffplug/spotless/extra/eclipse_jdt_formatter -->
       <version>4.7.1</version>
     </eclipse>
     <googleJavaFormat>
       <!-- Optional, available versions: https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.google.googlejavaformat%22%20AND%20a%3A%22google-java-format%22 -->
       <version>1.5</version>
       <!-- Optional, available versions: GOOGLE, AOSP  https://github.com/google/google-java-format/blob/master/core/src/main/java/com/google/googlejavaformat/java/JavaFormatterOptions.java -->
       <style>GOOGLE</style>
     </googleJavaFormat>
     <removeUnusedImports/>
     <importOrder>
       <!-- Specify either order or file, but not both -->
       <order>java,javax,org,com,com.diffplug,</order>
       <file>${basedir}/eclipse.importorder</file>
     </importOrder>
  </java>
</configuration>
```

See [ECLIPSE_SCREENSHOTS](../ECLIPSE_SCREENSHOTS.md) for screenshots that demonstrate how to get and install the Eclipse format configuration file and Eclipse import order file mentioned above.

<a name="scala"></a>

## Applying to Scala source

By default, all files matching `src/main/scala/**/*.scala`, `src/test/scala/**/*.scala`, `src/main/scala/**/*.sc` and `src/test/scala/**/*.sc` Ant style pattern will be formatted.  Each element under `<scala>` is a step, and they will be applied in the order specified.  Every step is optional.

```xml
<configuration>
  <scala>
     <licenseHeader>
       <!-- Specify either content or file, but not both -->
       <content>/* Licensed under Apache-2.0 */</content>
       <file>${basedir}/license-header</file>
     </licenseHeader>
     <endWithNewline/>
     <trimTrailingWhitespace/>
     <scalafmt>
       <file>${basedir}/scalafmt.conf</file>
       <!-- Optional, available versions: https://github.com/scalameta/scalafmt/releases -->
       <version>1.1.0</version>
     </scalafmt>
  </scala>
</configuration>
```

<a name="kotlin"></a>

## Applying to Kotlin source

By default, all files matching `src/main/kotlin/**/*.kt` and `src/test/kotlin/**/*.kt` Ant style pattern will be formatted.  Each element under `<kotlin>` is a step, and they will be applied in the order specified.  Every step is optional.

### Applying [ktlint](https://github.com/pinterest/ktlint) to Kotlin files

```xml
<configuration>
  <kotlin>
     <licenseHeader>
       <!-- Specify either content or file, but not both -->
       <content>/* Licensed under Apache-2.0 */</content>
       <file>${basedir}/license-header</file>
     </licenseHeader>
     <endWithNewline/>
     <trimTrailingWhitespace/>
     <ktlint>
       <!-- Optional, available versions: https://github.com/pinterest/ktlint/releases -->
       <version>0.14.0</version>
     </ktlint>
  </kotlin>
</configuration>
```

### Applying [ktfmt](https://github.com/facebookincubator/ktfmt) to Kotlin files

```xml
<configuration>
  <kotlin>
     <licenseHeader>
       <!-- Specify either content or file, but not both -->
       <content>/* Licensed under Apache-2.0 */</content>
       <file>${basedir}/license-header</file>
     </licenseHeader>
     <endWithNewline/>
     <trimTrailingWhitespace/>
     <ktfmt>
       <!-- Optional, available versions: https://github.com/facebookincubator/ktfmt/releases -->
       <version>0.11</version>
     </ktfmt>
  </kotlin>
</configuration>
```

<a name="cpp"></a>

## Applying to C/C++ source

By default, all files matching `src/main/cpp/**/*.<ext>` and `src/test/cpp/**/*.<ext>` Ant style pattern will be formatted, whereas the file extensions `c`, `h`, `C`, `cpp`, `cxx`, `cc`, `c++`, `h`, `hpp`, `hh`, `hxx` and `inc` are supported.  Each element under `<cpp>` is a step, and they will be applied in the order specified.  Every step is optional, and they will be applied in the order specified.

```xml
<configuration>
  <cpp>
     <licenseHeader>
       <!-- Specify either content or file, but not both -->
       <content>/* Licensed under Apache-2.0 */</content>
       <file>${basedir}/license-header</file>
     </licenseHeader>
     <eclipse>
       <file>${basedir}/eclipse-fmt.xml</file>
       <!-- Optional, available versions: https://github.com/diffplug/spotless/tree/master/lib-extra/src/main/resources/com/diffplug/spotless/extra/eclipse_cdt_formatter -->
       <version>4.7.3a</version>
     </eclipse>
  </cpp>
</configuration>
```
Use the Eclipse to define the *Code Style preferences* (see [Eclipse documentation](https://www.eclipse.org/documentation/)). Within the preferences *Edit...* dialog, you can export your configuration as XML file, which can be used as a configuration `<file>`. If no `<file>` is provided, the CDT default configuration is used.

<a name="typescript"></a>

## Applying to Typescript source

```xml
<configuration>
  <typescript>

       <!-- optionally define which files will be formatted. -->
       <includes>
         <include>src/**/*.ts</include> <!-- default value if nothing is specified -->
       </includes>

     <tsfmt>
       <!-- must specify exactly one of the following "{foo}File" or "config" elements -->
       <tslintFile>${basedir}/path/to/repo/tslint.json</tslintFile>
       <tsfmtFile>${basedir}/path/to/repo/tsfmt.json</tsfmtFile>
       <tsconfigFile>${basedir}/path/to/repo/tsconfig.json</tsconfigFile>
       <vscodeFile>${basedir}/path/to/repo/vscode.json</vscodeFile>
       <config>
         <indentSize>1</indentSize>
         <convertTabsToSpaces>true</convertTabsToSpaces>
       </config>
       <!-- optionally configure following versions to use, shown values are defaults-->
       <typescriptFormatterVersion>7.2.2</typescriptFormatterVersion>
       <typescriptVersion>3.3.3</typescriptVersion>
       <tslintVersion>5.12.1</tslintVersion>
     </tsfmt>
  </typescript>
</configuration>
```

Supported config file types are `tsconfigFile`, `tslintFile`, `vscodeFile` and `tsfmtFile`. They are corresponding to the respective
[tsfmt-parameters](https://github.com/vvakame/typescript-formatter/blob/7764258ad42ac65071399840d1b8701868510ca7/lib/index.ts#L27L34).  See [tsfmt's default config settings](https://github.com/vvakame/typescript-formatter/blob/7764258ad42ac65071399840d1b8701868510ca7/lib/utils.ts#L11L32) for what is available.

*Please note:*
The auto-discovery of config files (up the file tree) will not work when using tsfmt within spotless,
  hence you are required to provide resolvable file paths for config files.

### Prerequisite: tsfmt requires a working NodeJS version

tsfmt is based on NodeJS, so to use it, a working NodeJS installation (especially npm) is required on the host running spotless.
Spotless will try to auto-discover an npm installation. If that is not working for you, it is possible to directly configure the npm binary to use.

```xml
<configuration><typescript><tsfmt>
  ...
  <npmExecutable>/usr/bin/npm</npmExecutable>
```

Spotless uses npm to install necessary packages locally. It runs tsfmt using [J2V8](https://github.com/eclipsesource/J2V8) internally after that.

<a name="prettier"></a>

## Applying [Prettier](https://prettier.io) to javascript | flow | typeScript | css | scss | less | jsx | graphQL | yaml | etc.

Prettier is a formatter that can format [multiple file types](https://prettier.io/docs/en/language-support.html).

To use prettier, you first have to specify the files that you want it to apply to.  Then you specify prettier, and how you want to apply it.

```xml
<configuration>
  <formats>

    <format>
      <includes>
        <include>src/**/typescript/**/*.ts</include>
      </includes>

      <prettier>
        <!-- Specify either simple prettier version (1.19.0 is max supported,
             which is also default) or whole devDependencies -->
        <prettierVersion>1.19.0</prettierVersion>
        <devDependencies>
            <prettier>1.19.0</prettier>
        </devDependencies>

        <!-- Specify config file and/or inline config -->
        <configFile>${basedir}/path/to/configfile</configFile>
        <config>
            <useTabs>true</useTabs>
        </config>
      </prettier>
    </format>

  </formats>
</configuration>
```

Supported config options are documented on [prettier.io](https://prettier.io/docs/en/options.html).
Supported config file variants are documented on [prettier.io](https://prettier.io/docs/en/configuration.html).

*Please note:*
- The auto-discovery of config files (up the file tree) will not work when using prettier within spotless.
- Prettier's override syntax is not supported when using prettier within spotless.

To apply prettier to more kinds of files, just add more formats.

### Prerequisite: prettier requires a working NodeJS version

Prettier, like tsfmt, is based on NodeJS, so to use it, a working NodeJS installation (especially npm) is required on the host running spotless.
Spotless will try to auto-discover an npm installation. If that is not working for you, it is possible to directly configure the npm binary to use.

```xml
<formats><format><prettier>
  <npmExecutable>/usr/bin/npm</npmExecutable>
  ...
```

Spotless uses npm to install necessary packages locally. It runs prettier using [J2V8](https://github.com/eclipsesource/J2V8) internally after that.
Development for J2V8 for non android envs is stopped (for Windows since J2V8 4.6.0 and Unix 4.8.0), therefore Prettier is limited to <= v1.19.0 as newer versions
use ES6 feature and that needs a newer J2V8 version.

<a name="format"></a>

## Applying to custom sources

By default, no Ant-Style include patterns are defined.  Each element under `<format>` is a step, and they will be applied in the order specified.  Every step is optional, and they will be applied in the order specified. It is possible to define multiple custom formats.

```xml
<configuration>
  <formats>

    <!-- Define first formatter that operates on properties files -->
    <format>
      <includes>
        <!-- Include all property files in "resource" folders under "src" -->
        <include>src/**/resources/**/*.properties</include>
      </includes>

      <licenseHeader>
        <!-- Specify either content or file, but not both -->
        <content>/* Licensed under Apache-2.0 */</content>
        <file>${basedir}/license-header</file>
        <!-- conent until first occurrence of the delimiter regex will be interpreted as header section -->
        <delimiter>#</delimiter>
      </licenseHeader>

      <!-- Files must end with a newline -->
      <endWithNewline />

      <!-- Specify whether to use tabs or spaces for indentation -->
      <indent>
        <!-- Specify either spaces or tabs -->
        <spaces>true</spaces>
        <tabs>true</tabs>
        <!-- Specify how many spaces are used to convert one tab and vice versa. Defaults to 4 -->
        <spacesPerTab>4</spacesPerTab>
      </indent>

      <!-- Trim trailing whitespaces -->
      <trimTrailingWhitespace />

      <!-- Specify replacements using search and replace -->
      <replace>
        <name>Say Hello to Mars</name>
        <search>World</search>
        <replacement>Mars</replacement>
      </replace>

      <!-- Specify replacements using regex match and replace -->
      <replaceRegex>
        <name>Say Hello to Mars from Regex</name>
        <searchRegex>(Hello) W[a-z]{3}d</searchRegex>
        <replacement>$1 Mars</replacement>
      </replaceRegex>
    </format>

    <!-- Other formats can be defined here, they will be applied in the order specified -->

  </formats>
</configuration>
```


<a name="eclipse-wtp"></a>

## Applying [Eclipse WTP](https://www.eclipse.org/webtools/) to css | html | etc.

The Eclipse [WTP](https://www.eclipse.org/webtools/) formatter can be applied as follows:

```xml
<configuration>
  <formats>

    <format>
      <includes>
        <include>src/**/resources/**/*.xml</include>
        <include>src/**/resources/**/*.xsd</include>
      </includes>

      <eclipseWtp>
        <!-- Specify the WTP formatter type (XML, JS, ...) -->
        <type>XML</type>
        <!-- Specify the configuration for the selected type -->
        <files>
          <file>${basedir}/xml.prefs</file>
          <file>${basedir}/additional.properties</file>
        </files>
        <!-- Optional, available versions: https://github.com/diffplug/spotless/tree/master/lib-extra/src/main/resources/com/diffplug/spotless/extra/eclipse_wtp_formatters -->
        <version>4.7.3a</version>
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

<a name="invisible"></a>

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

<a name="includeExclude"></a>

## File includes and excludes

Spotless uses [Ant style patterns](https://ant.apache.org/manual/dirtasks.html) to define included and excluded files.
By default, most common compile and test source roots for the supported languages are included. They are `src/main/java/**/*.java`, `src/test/java/**/*.java` for Java and `src/main/scala/**/*.scala`, `src/main/scala/**/*.sc`, `src/test/scala/**/*.scala`, `src/test/scala/**/*.sc` for Scala.
Includes can be completely overriden using `<includes>...</includes>` configuration section.

Default excludes only contain output directory (usually `target/`) and various temporary and VCS-related files. Additional excludes can also be configured.

Includes and excludes can be configured the same way for all supported languages. Excample for Java:

```xml
<java>
  <includes>
    <!-- include all java files in "java" folders under "src" -->
    <include>src/**/java/**/*.java</include>

    <!-- include all java files in "java" folders under "other" -->
    <include>other/java/**/*.java</include>
  </includes>

  <excludes>
    <!-- exclude examples from formatting -->
    <exclude>src/test/java/**/*Example.java</exclude>
  </excludes>
</java>
```

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

<a name="examples"></a>

## Can I apply Spotless to specific files?

You can target specific files by setting the `spotlessFiles` project property to a comma-separated list of file patterns:

```
cmd> mvn spotless:apply -DspotlessFiles=my/file/pattern.java,more/generic/.*-pattern.java
```

The patterns are matched using `String#matches(String)` against the absolute file path.

## Example configurations (from real-world projects)

- [Apache Avro](https://github.com/apache/avro/blob/8026c8ffe4ef67ab419dba73910636bf2c1a691c/lang/java/pom.xml#L307-L334)
- [JUNG: Java Universal Network/Graph Framework](https://github.com/jrtom/jung/blob/b3a2461b97bb3ab40acc631e21feef74976489e4/pom.xml#L187-L208)

<!---freshmark /javadoc -->
