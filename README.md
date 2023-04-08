# <img align="left" src="_images/spotless_logo.png"> Spotless: Keep your code spotless

[![Gradle Plugin](https://img.shields.io/gradle-plugin-portal/v/com.diffplug.spotless?color=blue&label=gradle%20plugin)](plugin-gradle)
[![Maven Plugin](https://img.shields.io/maven-central/v/com.diffplug.spotless/spotless-maven-plugin?color=blue&label=maven%20plugin)](plugin-maven)
[![SBT Plugin](https://img.shields.io/badge/sbt%20plugin-0.1.3-blue)](https://github.com/moznion/sbt-spotless)

Spotless can format &lt;antlr | c | c# | c++ | css | flow | graphql | groovy | html | java | javascript | json | jsx | kotlin | less | license headers | markdown | objective-c | protobuf | python | scala | scss | sql | typeScript | vue | yaml | anything> using &lt;gradle | maven | sbt | anything>.

You probably want one of the links below:

## [❇️ Spotless for Gradle](plugin-gradle) (with integrations for [VS Code](https://marketplace.visualstudio.com/items?itemName=richardwillis.vscode-spotless-gradle) and [IntelliJ](https://plugins.jetbrains.com/plugin/18321-spotless-gradle))

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

## [❇️ Spotless for Maven](plugin-maven)

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

## [❇️ Spotless for SBT (external for now)](https://github.com/moznion/sbt-spotless)
## [Other build systems](CONTRIBUTING.md#how-to-add-a-new-plugin-for-a-build-system)

## How it works (for potential contributors)

Ideally, a code formatter can do more than just find formatting errors - it should fix them as well. Such a formatter is just a `Function<String, String>`, which returns a formatted version of its potentially unformatted input.

It's easy to build such a function, but there are some gotchas and lots of integration work ([newlines](https://github.com/diffplug/spotless/tree/main/plugin-gradle#line-endings-and-encodings-invisible-stuff), character [encodings](https://github.com/diffplug/spotless/blob/08340a11566cdf56ecf50dbd4d557ed84a70a502/testlib/src/test/java/com/diffplug/spotless/EncodingErrorMsgTest.java#L34-L38), [idempotency](https://github.com/diffplug/spotless/blob/main/PADDEDCELL.md), git [ratcheting](https://github.com/diffplug/spotless/tree/main/plugin-gradle#ratchet), and build-system integration). Spotless tackles those for you **so you can focus on just a simple `Function<String, String>` which can compose with any of the other formatters and build tools in Spotless' arsenal**.

## Current feature matrix

<!---freshmark matrix
function lib(className)   { return '| [`' + className + '`](lib/src/main/java/com/diffplug/spotless/' + className.replaceAll('\\.', '/') + '.java) | ' }
function extra(className) { return '| [`' + className + '`](lib-extra/src/main/java/com/diffplug/spotless/extra/' + className.replaceAll('\\.', '/') + '.java) | ' }

//                                               | GRADLE        | MAVEN        | SBT          | (new)   |
output = [
'| Feature / FormatterStep                       | [gradle](plugin-gradle/README.md) | [maven](plugin-maven/README.md) | [sbt](https://github.com/moznion/sbt-spotless) | [(Your build tool here)](CONTRIBUTING.md#how-to-add-a-new-plugin-for-a-build-system) |',
'| --------------------------------------------- | ------------- | ------------ | ------------ | --------|',
'| Automatic [idempotency safeguard](PADDEDCELL.md) | {{yes}}    | {{yes}}      | {{yes}}      | {{no}}  |',
'| Misconfigured [encoding safeguard](https://github.com/diffplug/spotless/blob/08340a11566cdf56ecf50dbd4d557ed84a70a502/testlib/src/test/java/com/diffplug/spotless/EncodingErrorMsgTest.java#L34-L38) | {{yes}}    | {{yes}}      | {{yes}}       | {{no}}  |',
'| Toggle with [`spotless:off` and `spotless:on`](plugin-gradle/#spotlessoff-and-spotlesson) | {{yes}}    | {{yes}}      | {{no}}       | {{no}}  |',
'| [Ratchet from](https://github.com/diffplug/spotless/tree/main/plugin-gradle#ratchet) `origin/main` or other git ref | {{yes}}    | {{yes}}      | {{no}}       | {{no}}  |',
'| Define [line endings using git](https://github.com/diffplug/spotless/tree/main/plugin-gradle#line-endings-and-encodings-invisible-stuff) | {{yes}}    | {{yes}}      | {{yes}}       | {{no}}  |',
'| Fast incremental format and up-to-date check   | {{yes}}      | {{yes}}      | {{no}}       | {{no}}  |',
'| Fast format on fresh checkout using buildcache | {{yes}}      | {{no}}       | {{no}}       | {{no}}  |',
lib('generic.EndWithNewlineStep')                +'{{yes}}       | {{yes}}      | {{no}}       | {{no}}  |',
lib('generic.IndentStep')                        +'{{yes}}       | {{yes}}      | {{no}}       | {{no}}  |',
lib('generic.Jsr223Step')                        +'{{no}}        | {{yes}}      | {{no}}       | {{no}}  |',
lib('generic.LicenseHeaderStep')                 +'{{yes}}       | {{yes}}      | {{yes}}      | {{no}}  |',
lib('generic.NativeCmdStep')                     +'{{yes}}       | {{yes}}      | {{no}}       | {{no}}  |',
lib('generic.ReplaceRegexStep')                  +'{{yes}}       | {{yes}}      | {{no}}       | {{no}}  |',
lib('generic.ReplaceStep')                       +'{{yes}}       | {{yes}}      | {{no}}       | {{no}}  |',
lib('generic.TrimTrailingWhitespaceStep')        +'{{yes}}       | {{yes}}      | {{no}}       | {{no}}  |',
lib('antlr4.Antlr4FormatterStep')                +'{{yes}}       | {{yes}}      | {{no}}       | {{no}}  |',
lib('cpp.ClangFormatStep')                       +'{{yes}}       | {{no}}       | {{no}}       | {{no}}  |',
extra('cpp.EclipseFormatterStep')                +'{{yes}}       | {{yes}}      | {{yes}}      | {{no}}  |',
lib('gherkin.GherkinUtilsStep')                  +'{{yes}}       | {{yes}}      | {{no}}       | {{no}}  |',
extra('groovy.GrEclipseFormatterStep')           +'{{yes}}       | {{yes}}      | {{yes}}      | {{no}}  |',
lib('java.GoogleJavaFormatStep')                 +'{{yes}}       | {{yes}}      | {{yes}}      | {{no}}  |',
lib('java.ImportOrderStep')                      +'{{yes}}       | {{yes}}      | {{yes}}      | {{no}}  |',
lib('java.PalantirJavaFormatStep')               +'{{yes}}       | {{yes}}      | {{no}}       | {{no}}  |',
lib('java.RemoveUnusedImportsStep')              +'{{yes}}       | {{yes}}      | {{yes}}      | {{no}}  |',
extra('java.EclipseJdtFormatterStep')            +'{{yes}}       | {{yes}}      | {{yes}}      | {{no}}  |',
lib('java.FormatAnnotationsStep')                +'{{yes}}       | {{yes}}      | {{no}}       | {{no}}  |',
lib('java.CleanthatJavaStep')                    +'{{yes}}       | {{yes}}      | {{no}}       | {{no}}  |',
lib('json.gson.GsonStep')                        +'{{yes}}       | {{yes}}      | {{no}}       | {{no}}  |',
lib('json.JacksonJsonStep')                      +'{{yes}}       | {{yes}}      | {{no}}       | {{no}}  |',
lib('json.JsonSimpleStep')                       +'{{yes}}       | {{yes}}      | {{no}}       | {{no}}  |',
lib('kotlin.KtLintStep')                         +'{{yes}}       | {{yes}}      | {{yes}}      | {{no}}  |',
lib('kotlin.KtfmtStep')                          +'{{yes}}       | {{yes}}      | {{no}}       | {{no}}  |',
lib('kotlin.DiktatStep')                         +'{{yes}}       | {{yes}}      | {{no}}       | {{no}}  |',
lib('markdown.FreshMarkStep')                    +'{{yes}}       | {{no}}       | {{no}}       | {{no}}  |',
lib('markdown.FlexmarkStep')                     +'{{no}}        | {{yes}}      | {{no}}       | {{no}}  |',
lib('npm.EslintFormatterStep')                   +'{{yes}}       | {{yes}}      | {{no}}       | {{no}}  |',
lib('npm.PrettierFormatterStep')                 +'{{yes}}       | {{yes}}      | {{no}}       | {{no}}  |',
lib('npm.TsFmtFormatterStep')                    +'{{yes}}       | {{yes}}      | {{no}}       | {{no}}  |',
lib('pom.SortPomStep')                           +'{{no}}        | {{yes}}      | {{no}}       | {{no}}  |',
lib('python.BlackStep')                          +'{{yes}}       | {{no}}       | {{no}}       | {{no}}  |',
lib('rome.RomeStep')                             +'{{yes}}       | {{yes}}      | {{no}}       | {{no}}  |',
lib('scala.ScalaFmtStep')                        +'{{yes}}       | {{yes}}      | {{yes}}      | {{no}}  |',
lib('sql.DBeaverSQLFormatterStep')               +'{{yes}}       | {{yes}}      | {{yes}}      | {{no}}  |',
extra('wtp.EclipseWtpFormatterStep')             +'{{yes}}       | {{yes}}      | {{no}}       | {{no}}  |',
lib('yaml.JacksonYamlStep')                      +'{{yes}}       | {{yes}}      | {{no}}       | {{no}}  |',
'| [(Your FormatterStep here)](CONTRIBUTING.md#how-to-add-a-new-formatterstep) | {{no}} | {{no}} | {{no}} | {{no}} |',
].join('\n');
-->
| Feature / FormatterStep                       | [gradle](plugin-gradle/README.md) | [maven](plugin-maven/README.md) | [sbt](https://github.com/moznion/sbt-spotless) | [(Your build tool here)](CONTRIBUTING.md#how-to-add-a-new-plugin-for-a-build-system) |
| --------------------------------------------- | ------------- | ------------ | ------------ | --------|
| Automatic [idempotency safeguard](PADDEDCELL.md) | :+1:    | :+1:      | :+1:      | :white_large_square:  |
| Misconfigured [encoding safeguard](https://github.com/diffplug/spotless/blob/08340a11566cdf56ecf50dbd4d557ed84a70a502/testlib/src/test/java/com/diffplug/spotless/EncodingErrorMsgTest.java#L34-L38) | :+1:    | :+1:      | :+1:       | :white_large_square:  |
| Toggle with [`spotless:off` and `spotless:on`](plugin-gradle/#spotlessoff-and-spotlesson) | :+1:    | :+1:      | :white_large_square:       | :white_large_square:  |
| [Ratchet from](https://github.com/diffplug/spotless/tree/main/plugin-gradle#ratchet) `origin/main` or other git ref | :+1:    | :+1:      | :white_large_square:       | :white_large_square:  |
| Define [line endings using git](https://github.com/diffplug/spotless/tree/main/plugin-gradle#line-endings-and-encodings-invisible-stuff) | :+1:    | :+1:      | :+1:       | :white_large_square:  |
| Fast incremental format and up-to-date check   | :+1:      | :+1:      | :white_large_square:       | :white_large_square:  |
| Fast format on fresh checkout using buildcache | :+1:      | :white_large_square:       | :white_large_square:       | :white_large_square:  |
| [`generic.EndWithNewlineStep`](lib/src/main/java/com/diffplug/spotless/generic/EndWithNewlineStep.java) | :+1:       | :+1:      | :white_large_square:       | :white_large_square:  |
| [`generic.IndentStep`](lib/src/main/java/com/diffplug/spotless/generic/IndentStep.java) | :+1:       | :+1:      | :white_large_square:       | :white_large_square:  |
| [`generic.Jsr223Step`](lib/src/main/java/com/diffplug/spotless/generic/Jsr223Step.java) | :white_large_square:        | :+1:      | :white_large_square:       | :white_large_square:  |
| [`generic.LicenseHeaderStep`](lib/src/main/java/com/diffplug/spotless/generic/LicenseHeaderStep.java) | :+1:       | :+1:      | :+1:      | :white_large_square:  |
| [`generic.NativeCmdStep`](lib/src/main/java/com/diffplug/spotless/generic/NativeCmdStep.java) | :+1:       | :+1:      | :white_large_square:       | :white_large_square:  |
| [`generic.ReplaceRegexStep`](lib/src/main/java/com/diffplug/spotless/generic/ReplaceRegexStep.java) | :+1:       | :+1:      | :white_large_square:       | :white_large_square:  |
| [`generic.ReplaceStep`](lib/src/main/java/com/diffplug/spotless/generic/ReplaceStep.java) | :+1:       | :+1:      | :white_large_square:       | :white_large_square:  |
| [`generic.TrimTrailingWhitespaceStep`](lib/src/main/java/com/diffplug/spotless/generic/TrimTrailingWhitespaceStep.java) | :+1:       | :+1:      | :white_large_square:       | :white_large_square:  |
| [`antlr4.Antlr4FormatterStep`](lib/src/main/java/com/diffplug/spotless/antlr4/Antlr4FormatterStep.java) | :+1:       | :+1:      | :white_large_square:       | :white_large_square:  |
| [`cpp.ClangFormatStep`](lib/src/main/java/com/diffplug/spotless/cpp/ClangFormatStep.java) | :+1:       | :white_large_square:       | :white_large_square:       | :white_large_square:  |
| [`cpp.EclipseFormatterStep`](lib-extra/src/main/java/com/diffplug/spotless/extra/cpp/EclipseFormatterStep.java) | :+1:       | :+1:      | :+1:      | :white_large_square:  |
| [`gherkin.GherkinUtilsStep`](lib/src/main/java/com/diffplug/spotless/gherkin/GherkinUtilsStep.java) | :+1:       | :+1:      | :white_large_square:       | :white_large_square:  |
| [`groovy.GrEclipseFormatterStep`](lib-extra/src/main/java/com/diffplug/spotless/extra/groovy/GrEclipseFormatterStep.java) | :+1:       | :+1:      | :+1:      | :white_large_square:  |
| [`java.GoogleJavaFormatStep`](lib/src/main/java/com/diffplug/spotless/java/GoogleJavaFormatStep.java) | :+1:       | :+1:      | :+1:      | :white_large_square:  |
| [`java.ImportOrderStep`](lib/src/main/java/com/diffplug/spotless/java/ImportOrderStep.java) | :+1:       | :+1:      | :+1:      | :white_large_square:  |
| [`java.PalantirJavaFormatStep`](lib/src/main/java/com/diffplug/spotless/java/PalantirJavaFormatStep.java) | :+1:       | :+1:      | :white_large_square:       | :white_large_square:  |
| [`java.RemoveUnusedImportsStep`](lib/src/main/java/com/diffplug/spotless/java/RemoveUnusedImportsStep.java) | :+1:       | :+1:      | :+1:      | :white_large_square:  |
| [`java.EclipseJdtFormatterStep`](lib-extra/src/main/java/com/diffplug/spotless/extra/java/EclipseJdtFormatterStep.java) | :+1:       | :+1:      | :+1:      | :white_large_square:  |
| [`java.FormatAnnotationsStep`](lib/src/main/java/com/diffplug/spotless/java/FormatAnnotationsStep.java) | :+1:       | :+1:      | :white_large_square:       | :white_large_square:  |
| [`java.CleanthatJavaStep`](lib/src/main/java/com/diffplug/spotless/java/CleanthatJavaStep.java) | :+1:       | :+1:      | :white_large_square:       | :white_large_square:  |
| [`json.gson.GsonStep`](lib/src/main/java/com/diffplug/spotless/json/gson/GsonStep.java) | :+1:       | :+1:      | :white_large_square:       | :white_large_square:  |
| [`json.JacksonJsonStep`](lib/src/main/java/com/diffplug/spotless/json/JacksonJsonStep.java) | :+1:       | :+1:      | :white_large_square:       | :white_large_square:  |
| [`json.JsonSimpleStep`](lib/src/main/java/com/diffplug/spotless/json/JsonSimpleStep.java) | :+1:       | :+1:      | :white_large_square:       | :white_large_square:  |
| [`kotlin.KtLintStep`](lib/src/main/java/com/diffplug/spotless/kotlin/KtLintStep.java) | :+1:       | :+1:      | :+1:      | :white_large_square:  |
| [`kotlin.KtfmtStep`](lib/src/main/java/com/diffplug/spotless/kotlin/KtfmtStep.java) | :+1:       | :+1:      | :white_large_square:       | :white_large_square:  |
| [`kotlin.DiktatStep`](lib/src/main/java/com/diffplug/spotless/kotlin/DiktatStep.java) | :+1:       | :+1:      | :white_large_square:       | :white_large_square:  |
| [`markdown.FreshMarkStep`](lib/src/main/java/com/diffplug/spotless/markdown/FreshMarkStep.java) | :+1:       | :white_large_square:       | :white_large_square:       | :white_large_square:  |
| [`markdown.FlexmarkStep`](lib/src/main/java/com/diffplug/spotless/markdown/FlexmarkStep.java) | :white_large_square:        | :+1:      | :white_large_square:       | :white_large_square:  |
| [`npm.EslintFormatterStep`](lib/src/main/java/com/diffplug/spotless/npm/EslintFormatterStep.java) | :+1:       | :+1:      | :white_large_square:       | :white_large_square:  |
| [`npm.PrettierFormatterStep`](lib/src/main/java/com/diffplug/spotless/npm/PrettierFormatterStep.java) | :+1:       | :+1:      | :white_large_square:       | :white_large_square:  |
| [`npm.TsFmtFormatterStep`](lib/src/main/java/com/diffplug/spotless/npm/TsFmtFormatterStep.java) | :+1:       | :+1:      | :white_large_square:       | :white_large_square:  |
| [`pom.SortPomStep`](lib/src/main/java/com/diffplug/spotless/pom/SortPomStep.java) | :white_large_square:        | :+1:      | :white_large_square:       | :white_large_square:  |
| [`python.BlackStep`](lib/src/main/java/com/diffplug/spotless/python/BlackStep.java) | :+1:       | :white_large_square:       | :white_large_square:       | :white_large_square:  |
| [`rome.RomeStep`](lib/src/main/java/com/diffplug/spotless/rome/RomeStep.java) | :+1:       | :+1:       | :white_large_square:       | :white_large_square:  |
| [`scala.ScalaFmtStep`](lib/src/main/java/com/diffplug/spotless/scala/ScalaFmtStep.java) | :+1:       | :+1:      | :+1:      | :white_large_square:  |
| [`sql.DBeaverSQLFormatterStep`](lib/src/main/java/com/diffplug/spotless/sql/DBeaverSQLFormatterStep.java) | :+1:       | :+1:      | :+1:      | :white_large_square:  |
| [`wtp.EclipseWtpFormatterStep`](lib-extra/src/main/java/com/diffplug/spotless/extra/wtp/EclipseWtpFormatterStep.java) | :+1:       | :+1:      | :white_large_square:       | :white_large_square:  |
| [`yaml.JacksonYamlStep`](lib/src/main/java/com/diffplug/spotless/yaml/JacksonYamlStep.java) | :+1:       | :+1:      | :white_large_square:       | :white_large_square:  |
| [(Your FormatterStep here)](CONTRIBUTING.md#how-to-add-a-new-formatterstep) | :white_large_square: | :white_large_square: | :white_large_square: | :white_large_square: |
<!---freshmark /matrix -->

### Why are there empty squares?

Many projects get harder to work on as they get bigger. Spotless is easier to work on than ever, and one of the reasons why is that we don't require contributors to "fill the matrix". If you want to [add Bazel support](https://github.com/diffplug/spotless/issues/76), we'd happily accept the PR even if it only supports the one formatter you use. And if you want to add FooFormatter support, we'll happily accept the PR even if it only supports the one build system you use.

Once someone has filled in one square of the formatter/build system matrix, it's easy for interested parties to fill in any empty squares, since you'll now have a working example for every piece needed.

## Acknowledgements

- Thanks to [Konstantin Lutovich](https://github.com/lutovich) for [implementing and maintaining the maven plugin](https://github.com/diffplug/spotless/pull/188), as well as fixing [remote-build cache support for Gradle](https://github.com/diffplug/spotless/pull/571).
- Thanks to [Frank Vennemeyer](https://github.com/fvgh) for [Groovy support via greclipse](https://github.com/diffplug/spotless/issues/13), [C++ support via CDT](https://github.com/diffplug/spotless/issues/232), [XML support via WTP](https://github.com/diffplug/spotless/pull/241) and a huge body of work with other eclipse-based formatters.
- Thanks to [Jonathan Bluett-Duncan](https://github.com/jbduncan) for
  - implementing up-to-date checking [#31](https://github.com/diffplug/spotless/issues/31)
  - breaking spotless into libraries [#56](https://github.com/diffplug/spotless/issues/56)
  - lots of other things, but especially the diff support in `spotlessCheck`
  - constant improvements on a variety of topics with high-quality code reviews
- Thanks to [Daz DeBoer](https://github.com/bigdaz) for the reworking the guts of our gradle plugin to support [buildcache](https://github.com/diffplug/spotless/pull/576), [InputChanges](https://github.com/diffplug/spotless/pull/607), and [lazy configuration](https://github.com/diffplug/spotless/pull/617).
- Thanks to [Richard Willis](https://github.com/badsyntax) for creating the [VS Code extension for Spotless Gradle](https://marketplace.visualstudio.com/items?itemName=richardwillis.vscode-spotless-gradle).
- Thanks to [Ryan Gurney](https://github.com/ragurney) for creating the [IntelliJ plugin for Spotless Gradle](https://plugins.jetbrains.com/plugin/18321-spotless-gradle).
- Thanks to [Markus Heberling](https://github.com/tisoft) for adding [generic native formatters](https://github.com/diffplug/spotless/pull/949), [jsr-223 formatters](https://github.com/diffplug/spotless/pull/945), and [maven pom sorting](https://github.com/diffplug/spotless/pull/946).
- Thanks to [Matthias Balke](https://github.com/matthiasbalke) for [adding support for Antlr](https://github.com/diffplug/spotless/pull/328).
- Thanks to [Matthias Andreas Benkard](https://github.com/benkard) for adding support for google-java-format 1.8+ ([#563](https://github.com/diffplug/spotless/pull/563))
- Thanks to [Thomas Broyer](https://github.com/tbroyer) for adding support for google-java-format's [skip-reflowing-long-strings option](https://github.com/diffplug/spotless/pull/929).
- Thanks to [Ranadeep Polavarapu](https://github.com/RanadeepPolavarapu) for adding support for ktfmt ([#569](https://github.com/diffplug/spotless/pull/569))
- Thanks to [Simon Gamma](https://github.com/simschla) for [adding support for npm-based formatters](https://github.com/diffplug/spotless/pull/283), [twice](https://github.com/diffplug/spotless/pull/606) including `prettier` and `tsfmt`.
- Thanks to [Hakanai](https://github.com/hakanai) for adding [wildcards last support to the import sorter](https://github.com/diffplug/spotless/pull/956).
- Thanks to [Kevin Brooks](https://github.com/k-brooks) for [updating all eclipse-based formatters to 4.13](https://github.com/diffplug/spotless/pull/482) and [fixing Groovy for multiproject](https://github.com/diffplug/spotless/issues/877).
- Thanks to [Dylan Baroody](https://github.com/dylanbaroody) for fixing [sql formatting support for JDBI bind list params](https://github.com/diffplug/spotless/pull/955).
- Thanks to [figroc](https://github.com/figroc) for adding [custom mavenCoordinate support to google-java-format](https://github.com/diffplug/spotless/pull/944#issuecomment-927565149).
- Thanks to [Thomas Glaeser](https://github.com/tglaeser) for [finding](https://github.com/diffplug/spotless/issues/654) and [fixing](https://github.com/diffplug/spotless/pull/656) a file-permissions-clobbering bug.
- Thanks to [Joan Goyeau](https://github.com/joan38) for [fixing scalafmt integration](https://github.com/diffplug/spotless/pull/260).
- Thanks to [Nick Sutcliffe](https://github.com/nsutcliffe) for [fixing scalafmt post-2.0](https://github.com/diffplug/spotless/pull/416).
- Thanks to [Baptiste Mesta](https://github.com/baptistemesta) for
  - porting the DBeaver formatter to Spotless, and thanks to [DBeaver](https://dbeaver.jkiss.org/) and [its authors](https://github.com/serge-rider/dbeaver/graphs/contributors) for their excellent SQL formatter.
  - making license headers date-aware [#179](https://github.com/diffplug/spotless/pull/179)
- Thanks to [vmdominguez](https://github.com/vmdominguez) and [Luis Fors](https://github.com/luis-fors-cb) for adding the ability to limit formatting to specific files in gradle ([#322](https://github.com/diffplug/spotless/pull/322)) and maven ([#392](https://github.com/diffplug/spotless/pull/392)), respectively.
- Thanks to [bender316](https://github.com/bender316) for fixing classloading on Java 9 ([#426](https://github.com/diffplug/spotless/pull/426)).
- Thanks to [Stefan Oehme](https://github.com/oehme) for tons of help on the internal mechanics of Gradle.
- Thanks to [eyalkaspi](https://github.com/eyalkaspi) for adding configurable date ranges to the date-aware license headers.
- Thanks to [Andrew Parmet](https://github.com/andrewparmet) for adding [ktfmt support for kotlin gradle](https://github.com/diffplug/spotless/pull/583).
- Thanks to [Oliver Horn](https://github.com/ohorn) for adding AOSP support for Spotless' google-java-format integration.
- Formatting by Eclipse
  - Special thanks to [Mateusz Matela](https://waynebeaton.wordpress.com/2015/03/15/great-fixes-for-mars-winners-part-i/) for huge improvements to the eclipse code formatter!
- Thanks to [Zac Sweers](https://github.com/ZacSweers) for fixing the highly requested ktlint 0.34+ support ([#469](https://github.com/diffplug/spotless/pull/469)), multiple build updates and fixing a gradle deprecation warning ([#434](https://github.com/diffplug/spotless/pull/434) and others).
- Thanks to [Stephen Panaro](https://github.com/smpanaro) for adding support for ktlint FilenameRule ([#974](https://github.com/diffplug/spotless/pull/974)).
- Thanks to [Nelson Osacky](https://github.com/runningcode) for android doc improvements, versions bump, and a build improvement.
- Thanks to [Stanley Shyiko](https://github.com/shyiko) for his help integrating [ktlint](https://github.com/shyiko/ktlint).
- Thanks to [Jonathan Leitschuh](https://github.com/JLLeitschuh) for adding [ktlint](https://github.com/shyiko/ktlint) support for [Gradle Kotlin DSL](https://github.com/gradle/kotlin-dsl) files.
- Originally forked from [gradle-format-plugin](https://github.com/youribonnaffe/gradle-format-plugin) by Youri Bonnaffé.
- Thanks to Ismaël Mejía for bumping eclipse-jdt deps to 4.11. [PR #60](https://github.com/diffplug/spotless/issues/381).
- Thanks to Gábor Bernát for improvements to logging and multi-project support.
- Thanks to [Oliver Szymanski](https://github.com/source-knights) for porting [tsfmt](https://github.com/diffplug/spotless/pull/553) and [prettier](https://github.com/diffplug/spotless/pull/555) to maven.
- Thanks to Andrew Oberstar for improvements to formatting java source in non-java source sets. [PR #60](https://github.com/diffplug/spotless/pull/60).
- Thanks to [Sameer Balasubrahmanyam](https://github.com/sameer-b) for [adding support for IntelliJ-style year placeholders](https://github.com/diffplug/spotless/pull/542).
- Thanks to [Jamie Tanna](https://github.com/jamietanna) for [adding a simple JSON formatter](https://github.com/diffplug/spotless/pull/853).
- Thanks to [Adib Saikali](https://github.com/asaikali) and [Paul Merlin](https://github.com/eskatos) for tracking down the tricky cause of [#506](https://github.com/diffplug/spotless/issues/506).
- Import ordering from [EclipseCodeFormatter](https://github.com/krasa/EclipseCodeFormatter).
- Built by [gradle](https://gradle.org/).
- Tested by [junit](https://junit.org/).
- Maintained by [DiffPlug](https://www.diffplug.com/).
