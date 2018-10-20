# <img align="left" src="../_images/spotless_logo.png"> Spotless: Keep your code spotless with Maven

<!---freshmark shields
output = [
  link(shield('Maven central', 'mavencentral', '{{group}}:{{artifactIdMaven}}', 'blue'), 'http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22{{group}}%22%20AND%20a%3A%22{{artifactIdMaven}}%22'),
  link(shield('Javadoc', 'javadoc', '{{stableMaven}}', 'blue'), 'https://{{org}}.github.io/{{name}}/javadoc/{{artifactIdMaven}}/{{stableMaven}}/'),
  '',
  link(shield('Changelog', 'changelog', '{{stableMaven}}', 'brightgreen'), 'CHANGES.md'),
  link(image('Travis CI', 'https://travis-ci.org/{{org}}/{{name}}.svg?branch=master'), 'https://travis-ci.org/{{org}}/{{name}}'),
  link(shield('Live chat', 'gitter', 'chat', 'brightgreen'), 'https://gitter.im/{{org}}/{{name}}'),
  link(shield('License Apache', 'license', 'apache', 'brightgreen'), 'https://tldrlegal.com/license/apache-license-2.0-(apache-2.0)')
  ].join('\n');
-->
[![Maven central](https://img.shields.io/badge/mavencentral-com.diffplug.spotless%3Aspotless--maven--plugin-blue.svg)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.diffplug.spotless%22%20AND%20a%3A%22spotless-maven-plugin%22)
[![Javadoc](https://img.shields.io/badge/javadoc-1.15.0-blue.svg)](https://diffplug.github.io/spotless/javadoc/spotless-maven-plugin/1.15.0/)

[![Changelog](https://img.shields.io/badge/changelog-1.15.0-brightgreen.svg)](CHANGES.md)
[![Travis CI](https://travis-ci.org/diffplug/spotless.svg?branch=master)](https://travis-ci.org/diffplug/spotless)
[![Live chat](https://img.shields.io/badge/gitter-chat-brightgreen.svg)](https://gitter.im/diffplug/spotless)
[![License Apache](https://img.shields.io/badge/license-apache-brightgreen.svg)](https://tldrlegal.com/license/apache-license-2.0-(apache-2.0))
<!---freshmark /shields -->

<!---freshmark javadoc
output = prefixDelimiterReplace(input, 'https://{{org}}.github.io/{{name}}/javadoc/spotless-plugin-maven/', '/', stableMaven)
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

To use it in your pom, just [add the Spotless dependency](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.diffplug.spotless%22%20AND%20a%3A%22spotless-maven-plugin%22), and configure it like so:

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
* Eclipse's [WTP](https://www.eclipse.org/webtools/) CSS and XML code formatter
* Google's [google-java-format](https://github.com/google/google-java-format)
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
       <file>${basedir}/eclipse-fmt.xml</file>
       <!-- Optional, available versions: https://github.com/diffplug/spotless/tree/master/lib-extra/src/main/resources/com/diffplug/spotless/extra/config/eclipse_jdt_formatter -->
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
       <file>${basedir}/importOrder</file>
     </importOrder>
  </java>
</configuration>
```

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
     <endWithNewLine/>
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

```xml
<configuration>
  <kotlin>
     <licenseHeader>
       <!-- Specify either content or file, but not both -->
       <content>/* Licensed under Apache-2.0 */</content>
       <file>${basedir}/license-header</file>
     </licenseHeader>
     <endWithNewLine/>
     <trimTrailingWhitespace/>
     <ktlint>
       <!-- Optional, available versions: https://github.com/shyiko/ktlint/releases -->
       <version>0.14.0</version>
     </ktlint>
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
       <!-- Optional, available versions: https://github.com/diffplug/spotless/tree/master/lib-extra/src/main/resources/com/diffplug/spotless/extra/config/eclipse_cdt_formatter -->
       <version>4.7.3a</version>
     </eclipse>
  </cpp>
</configuration>
```
Use the Eclipse to define the *Code Style preferences* (see [Eclipse documentation](https://www.eclipse.org/documentation/)). Within the preferences *Edit...* dialog, you can export your configuration as XML file, which can be used as a configuration `<file>`. If no `<file>` is provided, the CDT default configuration is used.

<a name="xml"></a>

## Applying to CSS source

By default, all files matching `src/**/*.css` Ant style pattern will be formatted.  Each element under `<css>` is a step, and they will be applied in the order specified.  Every step is optional, and they will be applied in the order specified.

```xml
<configuration>
  <css>
     <licenseHeader>
       <!-- Specify either content or file, but not both -->
       <content>/* Licensed under Apache-2.0 */</content>
       <file>${basedir}/license-header</file>
     </licenseHeader>
     <eclipse>
       <file>${basedir}/eclipse-fmt.pref</file>
       <!-- Optional, available versions: https://github.com/diffplug/spotless/tree/master/lib-extra/src/main/resources/com/diffplug/spotless/extra/eclipse_wtp_formatters -->
       <version>4.7.3a</version>
     </eclipse>
  </css>
</configuration>
```
Use Eclipse to define the *CSS Files* editor preferences (see [Eclipse documentation](http://help.eclipse.org/photon/topic/org.eclipse.wst.sse.doc.user/topics/tsrcedt025.html)) and the *Cleanup* preferences available in the *Source* menu (when editing a CSS file). The preferences are stored below your Eclipse workspace directory in `.metadata/.plugins/org.eclipse.core.runtime/org.eclipse.wst.css.core.prefs`. Note that only the differences to the default configuration are stored within the file. If no `<file>` is provided, the WTP default configuration is used.

<a name="xml"></a>

## Applying to XML source

By default, all files matching `src/**/*.<ext>` Ant style pattern will be formatted, whereas the file extensions `xml`, `xsl`, `xslt`, `wsdl`, `xsd`, `exsd`, `xmi` are supported.  Each element under `<xml>` is a step, and they will be applied in the order specified.  Every step is optional, and they will be applied in the order specified.

```xml
<configuration>
  <xml>
     <licenseHeader>
       <!-- Specify either content or file, but not both -->
       <content>&lt;!-- Licensed under Apache-2.0 --&gt;</content>
       <file>${basedir}/license-header</file>
     </licenseHeader>
     <eclipse>
       <file>${basedir}/eclipse-fmt.pref</file>
       <!-- Optional, available versions: https://github.com/diffplug/spotless/tree/master/lib-extra/src/main/resources/com/diffplug/spotless/extra/eclipse_wtp_formatters -->
       <version>4.7.3a</version>
     </eclipse>
  </xml>
</configuration>
```
Use Eclipse to define the *XML editor preferences* (see [Eclipse documentation](https://www.eclipse.org/documentation/)). The preferences are stored below your Eclipse workspace directory in `.metadata/.plugins/org.eclipse.core.runtime/org.eclipse.wst.xml.core.prefs`. Note that only the differences to the default configuration are stored within the file. If no `<file>` is provided, the WTP default configuration is used.

The Eclipse WTP formatter supports DTD/XSD restrictions on white spaces. For XSD/DTD lookup, relative and absolute XSD/DTD URIs are supported. Furthermore a user catalog can be configured using the `userCatalog` property key. Add the property to the preference `<file>`.

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

## File incudes and excludes

Spotless uses [Ant style patterns](https://ant.apache.org/manual/dirtasks.html) to define included and excluded files.
By default, most common compile and test source roots for the supported languages are included. They are `scr/main/java/**/*.java`, `scr/test/java/**/*.java` for Java and `scr/main/scala/**/*.scala`, `scr/main/scala/**/*.sc`, `scr/test/scala/**/*.scala`, `scr/test/scala/**/*.sc` for Scala.
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

## Example configurations (from real-world projects)

- [JUNG: Java Universal Network/Graph Framework](https://github.com/jrtom/jung/blob/b3a2461b97bb3ab40acc631e21feef74976489e4/pom.xml#L187-L208)

<!---freshmark /javadoc -->
