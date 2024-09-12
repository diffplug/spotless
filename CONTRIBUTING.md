# Contributing to Spotless

Pull requests are welcome, preferably against `main`.  Feel free to develop spotless any way you like, but if you like Eclipse and Gradle Buildship then [`gradlew equoIde` will install an IDE and set it up for you](https://github.com/equodev/equo-ide).

## How Spotless works

Spotless' most basic element is the `FormatterStep`, which has one method that really matters: `String format(String rawUnix, File file)`.  Each step is guaranteed that its input string will contain only unix newlines, and the step's output should also contain only unix newlines.  The file argument is provided only to allow path-dependent formatting (e.g. special formatting for `package-info.java`), but most formatters are path-independent and won't use that argument.

In order to use and combine `FormatterStep`, you first create a `Formatter`, which has the following parameters:

- an encoding
- a list of `FormatterStep`
- a line endings policy (`LineEnding.GIT_ATTRIBUTES_FAST_ALLSAME` is almost always the best choice)

Once you have an instance of `Formatter`, you can call `DirtyState.of(Formatter, File)`. Under the hood, Spotless will:

- parse the raw bytes into a String according to the encoding
- normalize its line endings to `\n`
- pass the unix string to each `FormatterStep` one after the other
- check for idempotence problems, and repeatedly apply the steps until the [result is stable](PADDEDCELL.md). 
- apply line endings according to the policy

You can also use lower-level methods like `String compute(String unix, File file)` if you'd like to do lower-level processing.

All `FormatterStep` implement `Serializable`, `equals`, and `hashCode`, so build systems that support up-to-date checks can easily and correctly determine if any actions need to be taken.

## Project layout

For the folders below in monospace text, they are published on MavenCentral at the coordinate `com.diffplug.spotless:spotless-${FOLDER_NAME}`.  The other folders are dev infrastructure.

| Folder | Description |
| ------ | ----------- |
| `lib` | Contains all of Spotless' core infrastructure and most of its `FormatterStep` - has no external dependencies. |
| `testlib` | Contains testing infrastructure and all test resources, so that they can be reused in plugin-specific integration tests.  Also contains tests for `lib`. |
| `lib-extra` | Contains the optional parts of Spotless which require external dependencies.  `LineEnding.GIT_ATTRIBUTES` won't work unless `lib-extra` is available. |
| `plugin-gradle` | Integrates spotless and all of its formatters into Gradle. |
| `plugin-maven` | Integrates spotless and all of its formatters into Maven. |

## How to add a new FormatterStep

The easiest way to create a FormatterStep is to just create `class FooStep implements FormatterStep`. It has one abstract method which is the formatting function, and you're ready to tinker. To work with the build plugins, this class will need to

- implement equality and hashcode
- support lossless roundtrip serialization

You can use `StepHarness` (if you don't care about the `File` argument) or `StepHarnessWithFile` to test. The harness will roundtrip serialize your step, check that it's equal to itself, and then perform all tests on the roundtripped step.

## Implementing equality in terms of serialization

Spotless has infrastructure which uses the serialized form of your step to implement equality for you. Here is an example:

```java
public final class ReplaceStep {
  private ReplaceStep() {}

  public static FormatterStep create(String name, CharSequence target, CharSequence replacement) {
    return FormatterStep.create(name,
        new State(target, replacement),
        State::toFormatter);
  }

  private static final class State implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String target;
    private final String replacement;

    State(String target, String replacement) {
      this.target = target;
      this.replacement = replacement;
    }

    FormatterFunc toFormatter() {
      return unixStr -> unixStr.replace(target, replacement);
    }
  }
}
```

The `FormatterStep` created above implements `equals` and `hashCode` based on the serialized representation of its `State`.  This trick makes it quick and easy to write steps which properly support up-to-date checks.

Oftentimes, a rule's state will be expensive to compute. `EclipseFormatterStep`, for example, depends on a formatting file.  Ideally, we would like to only pay the cost of the I/O needed to load that file if we have to - we'd like to create the FormatterStep now but load its state lazily at the last possible moment.  For this purpose, each of the `FormatterStep.create` methods has a lazy counterpart.  Here are their signatures:

```java
FormatterStep create    (String name, State state                  , Function<State, FormatterFunc> stateToFormatter)
FormatterStep createLazy(String name, Supplier<State> stateSupplier, Function<State, FormatterFunc> stateToFormatter)
```

If your formatting step only needs to call one or two methods of the external dependency, you can pull it in at runtime and call it via reflection.  See the logic for [`EclipseFormatterStep`](lib-extra/src/main/java/com/diffplug/spotless/extra/java/EclipseFormatterStep.java) or [`GoogleJavaFormatStep`](lib/src/main/java/com/diffplug/spotless/java/GoogleJavaFormatStep.java).

Here's a checklist for creating a new step for Spotless:

- [ ] Class name ends in Step, `SomeNewStep`.
- [ ] Class has a public static method named `create` that returns a `FormatterStep`.
- [ ] Has a test class named `SomeNewStepTest` that uses `StepHarness` or `StepHarnessWithFile` to test the step.
  - [ ] Start with `StepHarness.forStep(myStep).supportsRoundTrip(false)`, and then add round trip support as described in the next section. 
- [ ] Test class has test methods to verify behavior.
- [ ] Test class has a test method `equality()` which tests equality using `StepEqualityTester` (see existing methods for examples).

### Serialization roundtrip

In order to support Gradle's configuration cache, all `FormatterStep` must be round-trip serializable. This is a bit tricky because step equality is based on the serialized form of the state, and `transient` can be used to take absolute paths out of the equality check. To make this work, roundtrip compatible steps can actually have *two* states:

- `RoundtripState` which must be roundtrip serializable but has no equality constraints
  - `FileSignature.Promised` for settings files and `JarState.Promised` for the classpath 
- `EqualityState` which will never be reserialized and its serialized form is used for equality / hashCode checks
  - `FileSignature` for settings files and `JarState` for the classpath

```java
FormatterStep create(String name,
  RoundtripState roundTrip,
  SerializedFunction<RoundtripState, EqualityState> equalityFunc,
  SerializedFunction<EqualityState, FormatterFunc> formatterFunc)
FormatterStep createLazy(String name,
  Supplier<RoundtripState> roundTrip,
  SerializedFunction<RoundtripState, EqualityState> equalityFunc,
  SerializedFunction<EqualityState, FormatterFunc> formatterFunc)
```

### Third-party dependencies via reflection or compile-only source sets

Most formatters are going to use some kind of third-party jar. Spotless integrates with many formatters, some of which have incompatible transitive dependencies. To address this, we resolve third-party dependencies using [`JarState`](https://github.com/diffplug/spotless/blob/b26f0972b185995d7c6a7aefa726c146d24d9a82/lib/src/main/java/com/diffplug/spotless/kotlin/KtfmtStep.java#L118). To call methods on the classes in that `JarState`, you can either use reflection or a compile-only source set. See [#524](https://github.com/diffplug/spotless/issues/524) for examples of both approaches.

- Adding a compile-only sourceset is easier to read and probably a better approach for most cases.
- Reflection is more flexible, and might be a better approach for a very simple API.

### Accessing the underlying File

In order for Spotless' model to work, each step needs to look only at the `String` input, otherwise they cannot compose.  However, there are some cases where the source `File` is useful, such as to look at the file extension.  In this case, you can pass a `FormatterFunc.NeedsFile` instead of a `FormatterFunc`.  This should only be used in [rare circumstances](https://github.com/diffplug/spotless/pull/637), be careful that you don't accidentally depend on the bytes inside of the `File`!

### Integrating outside the JVM

There are many great formatters (prettier, clang-format, black, etc.) which live entirely outside the JVM.  We have two main strategies for these:

- [shell out to an external command](https://github.com/diffplug/spotless/pull/672) for every file (used by clang-format and black)
- open a headless server and make http calls to it from Spotless (used by our [npm-based](https://github.com/diffplug/spotless/blob/main/lib/src/main/java/com/diffplug/spotless/npm/NpmFormatterStepStateBase.java) formatters such as prettier)

Because of Spotless' up-to-date checking and [git ratcheting](https://github.com/diffplug/spotless/tree/main/plugin-gradle#ratchet), Spotless actually doesn't have to call formatters very often, so even an expensive shell call for every single invocation isn't that bad.  Anything that works is better than nothing, and we can always speed things up later if it feels too slow (but it probably won't).

## How to add a new plugin for a build system

The gist of it is that you will have to:

- Use the build system's user-interface to define a "format" as a set of files, and the list of `FormatterStep` the user would like enforced on those files.
- Use the build system's execution logic to create a `Formatter` with the appropriate `FormatterStep`, and pass it the files to be formatted and/or checked.
- To use the good `FormatterStep` like `EclipseFormatterStep` or `GoogleJavaFormatStep`, you'll need to implement `Provisioner`, which is a generic API for the build system's native mechanism for resolving dependencies from maven: `Set<File> provisionWithDependencies(Collection<String> mavenCoordinates)`.
- (Optional) Tie into the build system's native up-to-date mechanism.
- (Optional) Use `PaddedCell` to proactively catch and resolve idempotence issues.

`plugin-gradle` is the canonical example which uses everything that Spotless has to offer.  It's only ~700 lines.

If you get something running, we'd love to host your plugin within this repo as a peer to `plugin-gradle` and `plugin-maven`.

## Run tests

To run all tests, simply do

> gradlew test

Since that takes some time, you might only want to run the tests
concerning what you are working on:

```shell
# Run only from test from the "lib" project
gradlew :testlib:test --tests com.diffplug.spotless.generic.IndentStepTest

# Run only one test from the "plugin-maven" project
gradlew :plugin-maven:test --tests com.diffplug.spotless.maven.pom.SortPomMavenTest

# Run only one test from the "plugin-gradle" project
gradlew :plugin-gradle:test --tests com.diffplug.gradle.spotless.FreshMarkExtensionTest
```

## Check and format code

Before creating a pull request, you might want to format (yes, spotless is  formatted by spotless)
the code and check for possible bugs

* `./gradlew spotlessApply`
* `./gradlew spotbugsMain`

These checks are also run by the automated pipeline when you submit a pull request, if
the pipeline fails, first check if the code is formatted and no bugs were found.

## Integration testing

### Gradle - locally

First, run `./gradlew publishToMavenLocal` in your local checkout of Spotless.  Now, in any other project on your machine, you can use the following snippet in your `settings.gradle` (for Gradle 6.0+).

```
pluginManagement {
  repositories {
    mavenLocal {
      content {
        includeGroup 'com.diffplug.spotless'
      }
    }
    gradlePluginPortal()
  }
  resolutionStrategy {
    eachPlugin {
      if (requested.id.id == 'com.diffplug.spotless') {
        useModule('com.diffplug.spotless:spotless-plugin-gradle:{latest-SNAPSHOT}')
      }
    }
  }
}
```

### Gradle - any commit in a public GitHub repo (this one, or any fork)

In Gradle 6.0+, you can use the following snippet in your `settings.gradle`.


```gradle
pluginManagement {
  repositories {
    maven {
      url 'https://jitpack.io'
      content {
        includeGroup 'com.github.{{user-or-org}}.spotless'
      }
    }
    gradlePluginPortal()
  }
  resolutionStrategy {
    eachPlugin {
      if (requested.id.id == 'com.diffplug.spotless') {
        useModule('com.github.{{USER_OR_ORG}}.spotless:spotless-plugin-gradle:{{SHA_OF_COMMIT_YOU_WANT}}')
      }
    }
  }
}
```

If it doesn't work, you can check the JitPack log at `https://jitpack.io/com/github/{{USER_OR_ORG}}/spotless/{{SHA_OF_COMMIT_YOU_WANT}}/build.log`.

### Maven

Run `./gradlew publishToMavenLocal` to publish this to your local repository. You can also use the JitPack artifacts, using the same principles as Gradle above.

## License

By contributing your code, you agree to license your contribution under the terms of the APLv2: https://github.com/diffplug/spotless/blob/main/LICENSE.txt

All files are released with the Apache 2.0 license as such:

```
Copyright 2020 DiffPlug

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
