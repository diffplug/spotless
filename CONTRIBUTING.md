# Contributing to Spotless

Pull requests are welcome, preferably against `master`.

## Build instructions

The easiest way to work on Spotless is to run `gradlew ide`.  It will download and setup an Eclipse IDE for you, using [goomph](https://github.com/diffplug/goomph).

You can also use Spotless like any other standard Gradle build.

`gradlew eclipse`
* creates an Eclipse project file for you.

`gradlew build`
* builds the jars
* runs the tests

## Project layout

| Maven coordinate | Folder | Description |
| ---------------- | ------ | ----------- |
| `com.diffplug.spotless:spotless-lib` | lib | Contains all of Spotless' core infrastructure and most of its `FormatterStep`, no external dependencies. |
| `com.diffplug.spotless:spotless-testlib` | testlib | Contains testing infrastructure, as well as all tests for `spotless-lib`, since those tests need the testing infrastructure. |
| `com.diffplug.spotless:spotless-lib-extra` | lib-extra | Contains the optional parts of Spotless which require external dependencies.  `LineEnding.GIT_ATTRIBUTES` won't work unless `lib-extra` is available. |
| `com.diffplug.spotless:spotless-plugin-gradle` | plugin-gradle | Integrates spotless and all of its formatters into Gradle. |
| `com.diffplug.spotless:spotless-plugin-maven` | plugin-maven | Integrates spotless and all of its formatters into Maven. |
| N/A | javadoc-publish | Logic for publishing javadoc to github-pages. |
| N/A | ide | Generates and launches an IDE for developing spotless. |
| N/A | _ext | Folder for generating glue jars (specifically packaging Eclipse jars from p2 for consumption using maven) |

## License

By contributing your code, you agree to license your contribution under the terms of the APLv2: https://github.com/diffplug/spotless/blob/master/LICENSE

All files are released with the Apache 2.0 license as such:

```
Copyright 2016 DiffPlug

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

	http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
