# <img align="left" src="../_images/spotless_logo.png"> Spotless: Keep your code spotless with Gradle

<!---freshmark shields
output = [
  link(shield('Maven central', 'mavencentral', '{{group}}:{{artifactIdMaven}}', 'blue'), 'http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22{{group}}%22%20AND%20a%3A%22{{artifactIdMaven}}%22'),
  link(shield('Javadoc', 'javadoc', '{{stableMaven}}', 'blue'), 'https://{{org}}.github.io/{{name}}/javadoc/spotless-plugin-maven/{{stableMaven}}/'),
  '',
  link(shield('Changelog', 'changelog', '{{stableMaven}}', 'brightgreen'), 'CHANGES.md'),
  link(image('Travis CI', 'https://travis-ci.org/{{org}}/{{name}}.svg?branch=master'), 'https://travis-ci.org/{{org}}/{{name}}'),
  link(shield('Live chat', 'gitter', 'chat', 'brightgreen'), 'https://gitter.im/{{org}}/{{name}}'),
  link(shield('License Apache', 'license', 'apache', 'brightgreen'), 'https://tldrlegal.com/license/apache-license-2.0-(apache-2.0)')
  ].join('\n');
-->
[![Maven central](https://img.shields.io/badge/mavencentral-com.diffplug.spotless%3Aspotless--maven--plugin-blue.svg)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.diffplug.spotless%22%20AND%20a%3A%22spotless-maven-plugin%22)
[![Javadoc](https://img.shields.io/badge/javadoc-unreleased-blue.svg)](https://diffplug.github.io/spotless/javadoc/spotless-plugin-maven/unreleased/)

[![Changelog](https://img.shields.io/badge/changelog-unreleased-brightgreen.svg)](CHANGES.md)
[![Travis CI](https://travis-ci.org/diffplug/spotless.svg?branch=master)](https://travis-ci.org/diffplug/spotless)
[![Live chat](https://img.shields.io/badge/gitter-chat-brightgreen.svg)](https://gitter.im/diffplug/spotless)
[![License Apache](https://img.shields.io/badge/license-apache-brightgreen.svg)](https://tldrlegal.com/license/apache-license-2.0-(apache-2.0))
<!---freshmark /shields -->

<!---freshmark javadoc
output = prefixDelimiterReplace(input, 'https://{{org}}.github.io/{{name}}/javadoc/spotless-plugin-maven/', '/', stableMaven)
-->

Spotless is a general-purpose formatting plugin.  It is completely à la carte, but also includes powerful "batteries-included" if you opt-in.

To people who use your build, it looks like this:

```
cmd> mvn spotless:check
...
:spotlessJavaCheck FAILED
> The following files had format violations:
  src\main\java\com\diffplug\gradle\spotless\FormatExtension.java
    @@ -109,7 +109,7 @@
    ...
    -\t\t····if·(targets.length·==·0)·{
    +\t\tif·(targets.length·==·0)·{
    ...
  Run 'mvn spotless:apply' to fix these violations.

cmd> mvn spotless:apply
:spotlessApply
BUILD SUCCESSFUL

cmd> mvn spotless:check
BUILD SUCCESSFUL
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
* Google's [google-java-format](https://github.com/google/google-java-format)
* User-defined license enforcement, regex replacement, etc.

Contributions are welcome, see [the contributing guide](../CONTRIBUTING.md) for development info.

Spotless requires Maven to be running on JRE 8+.

<a name="java"></a>

## Applying to Java source

By default, all compileSourceRoots will be formatted.  Each element under `<java>` is a step, and they will be applied in the order specified.  Every step is optional, and they will be applied in the order specified.  It doesn't make sense to use both eclipse and google-java-format.

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
       <!-- Optional, available versions: https://bintray.com/diffplug/opensource/spotless-ext-eclipse-jdt -->
       <version>4.7.1</version>
     </eclipse>
     <googleJavaFormat>
       <!-- Optional, available versions: https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.google.googlejavaformat%22%20AND%20a%3A%22google-java-format%22 -->
       <version>1.5</version>
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

## Disabling warnings and error messages

The `test` phase is Maven's built-in task for grouping all verification tasks - unit tests, static analysis, etc.  By default, `spotless-check` is added as a dependency to `test`.

You might want to disable this behavior.  We [recommend against this](https://github.com/diffplug/spotless/issues/79#issuecomment-290844602), but it's easy to do if you'd like:

```xml
<configuration>
  <enforceCheck>false</enforceCheck>
</configuration>
```

<a name="preview"></a>

## How do I preview what `mvn spotless:apply` will do?

- Save your working tree with `git add -A`, then `git commit -m "Checkpoint before spotless."`
- Run `mvn spotless:apply`
- View the changes with `git diff`
- If you don't like what spotless did, `git reset --hard`
- If you'd like to remove the "checkpoint" commit, `git reset --soft head~1` will make the checkpoint commit "disappear" from history, but keeps the changes in your working directory.

<a name="examples"></a>

## Example configurations (from real-world projects)

TODO

<!---freshmark /javadoc -->
