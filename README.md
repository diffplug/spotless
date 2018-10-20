# <img align="left" src="_images/spotless_logo.png"> Spotless: Keep your code spotless

<!---freshmark shields
output = [
  link(image('Travis CI', 'https://travis-ci.org/{{org}}/{{name}}.svg?branch=master'), 'https://travis-ci.org/{{org}}/{{name}}'),
  link(shield('Live chat', 'gitter', 'chat', 'brightgreen'), 'https://gitter.im/{{org}}/{{name}}'),
  link(shield('License Apache', 'license', 'apache', 'brightgreen'), 'https://tldrlegal.com/license/apache-license-2.0-(apache-2.0)')
  ].join('\n');
-->
[![Travis CI](https://travis-ci.org/diffplug/spotless.svg?branch=master)](https://travis-ci.org/diffplug/spotless)
[![Live chat](https://img.shields.io/badge/gitter-chat-brightgreen.svg)](https://gitter.im/diffplug/spotless)
[![License Apache](https://img.shields.io/badge/license-apache-brightgreen.svg)](https://tldrlegal.com/license/apache-license-2.0-(apache-2.0))
<!---freshmark /shields -->

Spotless can format &lt;java | kotlin | scala | sql | groovy | javascript | flow | typeScript | css | scss | less | jsx | vue | graphql | json | yaml | markdown | license headers | anything> using &lt;gradle | maven | anything>.

- [Spotless for Gradle](plugin-gradle)
- [Spotless for Maven](plugin-maven)
- [Other build systems](CONTRIBUTING.md#how-to-add-a-new-plugin-for-a-build-system)

Ideally, a code formatter can do more than just find formatting errors - it should fix them as well. Such a formatter is really just a `Function<String, String>`, which returns a formatted version of its potentially unformatted input.

It's easy to build such a function, but there are some gotchas and lots of integration work (newlines, character encodings, idempotency, and build-system integration). Spotless tackles those for you so you can focus on just a simple `Function<String, String>` which can compose with any of the other formatters and build tools in Spotless' arsenal.

## Current feature matrix

<!---freshmark matrix
function lib(className)   { return '| [`' + className + '`](lib/src/main/java/com/diffplug/spotless/' + className.replace('.', '/') + '.java) | ' }
function extra(className) { return '| [`' + className + '`](lib-extra/src/main/java/com/diffplug/spotless/extra/' + className.replace('.', '/') + '.java) | ' }

//                                               | GRADLE        | MAVEN        | (new)   |
output = [
'| Feature / FormatterStep                       | [plugin-gradle](plugin-gradle/README.md) | [plugin-maven](plugin-maven/README.md) | [(Your build tool here)](CONTRIBUTING.md#how-to-add-a-new-plugin-for-a-build-system) |',
'| --------------------------------------------- | ------------- | ------------ | --------|',
lib('generic.EndWithNewlineStep')                +'{{yes}}       | {{yes}}       | {{no}}  |',
lib('generic.IndentStep')                        +'{{yes}}       | {{yes}}       | {{no}}  |',
lib('generic.LicenseHeaderStep')                 +'{{yes}}       | {{yes}}      | {{no}}  |',
lib('generic.ReplaceRegexStep')                  +'{{yes}}       | {{yes}}       | {{no}}  |',
lib('generic.ReplaceStep')                       +'{{yes}}       | {{yes}}       | {{no}}  |',
lib('generic.TrimTrailingWhitespaceStep')        +'{{yes}}       | {{yes}}       | {{no}}  |',
extra('cpp.EclipseFormatterStep')                +'{{yes}}       | {{yes}}       | {{no}}  |',
extra('groovy.GrEclipseFormatterStep')           +'{{yes}}       | {{no}}       | {{no}}  |',
lib('java.GoogleJavaFormatStep')                 +'{{yes}}       | {{yes}}      | {{no}}  |',
lib('java.ImportOrderStep')                      +'{{yes}}       | {{yes}}      | {{no}}  |',
lib('java.RemoveUnusedImportsStep')              +'{{yes}}       | {{yes}}      | {{no}}  |',
extra('java.EclipseFormatterStep')               +'{{yes}}       | {{yes}}      | {{no}}  |',
lib('kotlin.KtLintStep')                         +'{{yes}}       | {{yes}}      | {{no}}  |',
lib('markdown.FreshMarkStep')                    +'{{yes}}       | {{no}}       | {{no}}  |',
lib('npm.PrettierFormatterStep')                 +'{{yes}}       | {{no}}       | {{no}}  |',
lib('npm.TsFmtFormatterStep')                    +'{{yes}}       | {{no}}       | {{no}}  |',
lib('scala.ScalaFmtStep')                        +'{{yes}}       | {{yes}}       | {{no}}  |',
lib('sql.DBeaverSQLFormatterStep')               +'{{yes}}       | {{no}}       | {{no}}  |',
extra('wtp.WtpEclipseFormatterStep')             +'{{yes}}       | {{yes}}      | {{no}}  |',
'| [(Your FormatterStep here)](CONTRIBUTING.md#how-to-add-a-new-formatterstep) | {{no}}        | {{no}}       | {{no}}  |',
'| Fast up-to-date checking                      | {{yes}}       | {{no}}       | {{no}}  |',
'| Automatic idempotency safeguard               | {{yes}}       | {{no}}       | {{no}}  |',
''
].join('\n');
-->
| Feature / FormatterStep                       | [plugin-gradle](plugin-gradle/README.md) | [plugin-maven](plugin-maven/README.md) | [(Your build tool here)](CONTRIBUTING.md#how-to-add-a-new-plugin-for-a-build-system) |
| --------------------------------------------- | ------------- | ------------ | --------|
| [`generic.EndWithNewlineStep`](lib/src/main/java/com/diffplug/spotless/generic/EndWithNewlineStep.java) | :+1:       | :+1:       | :white_large_square:  |
| [`generic.IndentStep`](lib/src/main/java/com/diffplug/spotless/generic/IndentStep.java) | :+1:       | :+1:       | :white_large_square:  |
| [`generic.LicenseHeaderStep`](lib/src/main/java/com/diffplug/spotless/generic/LicenseHeaderStep.java) | :+1:       | :+1:      | :white_large_square:  |
| [`generic.ReplaceRegexStep`](lib/src/main/java/com/diffplug/spotless/generic/ReplaceRegexStep.java) | :+1:       | :+1:       | :white_large_square:  |
| [`generic.ReplaceStep`](lib/src/main/java/com/diffplug/spotless/generic/ReplaceStep.java) | :+1:       | :+1:       | :white_large_square:  |
| [`generic.TrimTrailingWhitespaceStep`](lib/src/main/java/com/diffplug/spotless/generic/TrimTrailingWhitespaceStep.java) | :+1:       | :+1:       | :white_large_square:  |
| [`cpp.EclipseFormatterStep`](lib-extra/src/main/java/com/diffplug/spotless/extra/cpp/EclipseFormatterStep.java) | :+1:       | :+1:       | :white_large_square:  |
| [`groovy.GrEclipseFormatterStep`](lib-extra/src/main/java/com/diffplug/spotless/extra/groovy/GrEclipseFormatterStep.java) | :+1:       | :white_large_square:       | :white_large_square:  |
| [`java.GoogleJavaFormatStep`](lib/src/main/java/com/diffplug/spotless/java/GoogleJavaFormatStep.java) | :+1:       | :+1:      | :white_large_square:  |
| [`java.ImportOrderStep`](lib/src/main/java/com/diffplug/spotless/java/ImportOrderStep.java) | :+1:       | :+1:      | :white_large_square:  |
| [`java.RemoveUnusedImportsStep`](lib/src/main/java/com/diffplug/spotless/java/RemoveUnusedImportsStep.java) | :+1:       | :+1:      | :white_large_square:  |
| [`java.EclipseFormatterStep`](lib-extra/src/main/java/com/diffplug/spotless/extra/java/EclipseFormatterStep.java) | :+1:       | :+1:      | :white_large_square:  |
| [`kotlin.KtLintStep`](lib/src/main/java/com/diffplug/spotless/kotlin/KtLintStep.java) | :+1:       | :+1:      | :white_large_square:  |
| [`markdown.FreshMarkStep`](lib/src/main/java/com/diffplug/spotless/markdown/FreshMarkStep.java) | :+1:       | :white_large_square:       | :white_large_square:  |
| [`npm.PrettierFormatterStep`](lib/src/main/java/com/diffplug/spotless/npm/PrettierFormatterStep.java) | :+1:       | :white_large_square:       | :white_large_square:  |
| [`npm.TsFmtFormatterStep`](lib/src/main/java/com/diffplug/spotless/npm/TsFmtFormatterStep.java) | :+1:       | :white_large_square:       | :white_large_square:  |
| [`scala.ScalaFmtStep`](lib/src/main/java/com/diffplug/spotless/scala/ScalaFmtStep.java) | :+1:       | :+1:       | :white_large_square:  |
| [`sql.DBeaverSQLFormatterStep`](lib/src/main/java/com/diffplug/spotless/sql/DBeaverSQLFormatterStep.java) | :+1:       | :white_large_square:       | :white_large_square:  |
| [`wtp.WtpEclipseFormatterStep`](lib-extra/src/main/java/com/diffplug/spotless/extra/wtp/WtpEclipseFormatterStep.java) | :+1:       | :+1:      | :white_large_square:  |
| [(Your FormatterStep here)](CONTRIBUTING.md#how-to-add-a-new-formatterstep) | :white_large_square:        | :white_large_square:       | :white_large_square:  |
| Fast up-to-date checking                      | :+1:       | :white_large_square:       | :white_large_square:  |
| Automatic idempotency safeguard               | :+1:       | :white_large_square:       | :white_large_square:  |
<!---freshmark /matrix -->

## Acknowledgements

- Thanks to [Simon Gamma](https://github.com/simschla) for [adding support for npm-based formatters](https://github.com/diffplug/spotless/pull/283), including `prettier` and `tsfmt`.
- Thanks to [Frank Vennemeyer](https://github.com/fvgh) for [Groovy support via greclipse](https://github.com/diffplug/spotless/issues/13), [C++ support via CDT](https://github.com/diffplug/spotless/issues/232), [XML support via WTP](https://github.com/diffplug/spotless/pull/241) and a huge body of work with other eclipse-based formatters.
- Thanks to [Konstantin Lutovich](https://github.com/lutovich) for [implementing the maven plugin](https://github.com/diffplug/spotless/pull/188).
- Thanks to [Joan Goyeau](https://github.com/joan38) for [fixing scalafmt integration](https://github.com/diffplug/spotless/pull/260).
- Thanks to [Baptiste Mesta](https://github.com/baptistemesta) for
  - porting the DBeaver formatter to Spotless, and thanks to [DBeaver](https://dbeaver.jkiss.org/) and [its authors](https://github.com/serge-rider/dbeaver/graphs/contributors) for their excellent SQL formatter.
  - making license headers date-aware [#179](https://github.com/diffplug/spotless/pull/179)
- Thanks to [Jonathan Bluett-Duncan](https://github.com/jbduncan) for
  - implementing up-to-date checking [#31](https://github.com/diffplug/spotless/issues/31)
  - breaking spotless into libraries [#56](https://github.com/diffplug/spotless/issues/56)
  - lots of other things, but especially the diff support in `spotlessCheck`
- Thanks to [Stefan Oehme](https://github.com/oehme) for tons of help on the internal mechanics of Gradle.
- Thanks to [eyalkaspi](https://github.com/eyalkaspi) for adding configurable date ranges to the date-aware license headers.
- Thanks to [Oliver Horn](https://github.com/ohorn) for adding AOSP support for Spotless' google-java-format integration.
- Formatting by Eclipse
  - Special thanks to [Mateusz Matela](https://waynebeaton.wordpress.com/2015/03/15/great-fixes-for-mars-winners-part-i/) for huge improvements to the eclipse code formatter!
- Thanks to [Nelson Osacky](https://github.com/runningcode) for android doc improvements, versions bump, and a build improvement.
- Thanks to [Stanley Shyiko](https://github.com/shyiko) for his help integrating [ktlint](https://github.com/shyiko/ktlint).
- Thanks to [Jonathan Leitschuh](https://github.com/JLLeitschuh) for adding [ktlint](https://github.com/shyiko/ktlint) support for [Gradle Kotlin DSL](https://github.com/gradle/kotlin-dsl) files.
- Originally forked from [gradle-format-plugin](https://github.com/youribonnaffe/gradle-format-plugin) by Youri Bonnaffé.
- Thanks to Gábor Bernát for improvements to logging and multi-project support.
- Thanks to Andrew Oberstar for improvements to formatting java source in non-java source sets. [PR #60](https://github.com/diffplug/spotless/pull/60).
- Import ordering from [EclipseCodeFormatter](https://github.com/krasa/EclipseCodeFormatter).
- Built by [gradle](http://gradle.org/).
- Tested by [junit](http://junit.org/).
- Maintained by [DiffPlug](http://www.diffplug.com/).
