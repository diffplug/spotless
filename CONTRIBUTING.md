# Contributing to Spotless

Pull requests are welcome, preferably against `master`.  Feel free to develop spotless any way you like, but the easiest way to look at the code is to clone the repo and run `gradlew ide`, which will download, setup, and start an Eclipse IDE for you.

## How Spotless works

Spotless' most basic element is the `FormatterStep`, which has one method that really matters: `String format(String rawUnix, File file)`.  Each step is guaranteed that its input string will contain only unix newlines, and the step's output should also contain only unix newlines.  The file argument is provided only to allow path-dependent formatting (e.g. special formatting for `package-info.java`), but most formatters are path-independent and won't use that argument.

In order to use and combine `FormatterStep`, you first create a `Formatter`, which has the following parameters:

- an encoding
- a list of `FormatterStep`
- a line endings policy (`LineEnding.GIT_ATTRIBUTES` is almost always the best choice)

Once you have an instance of `Formatter`, you can call `boolean isClean(File)`, or `void applyTo(File)` to either check or apply formatting to a file.  Spotless will then:

- parse the raw bytes into a String according to the encoding
- normalize its line endings to `\n`
- pass the unix string to each `FormatterStep` one after the other
- apply line endings according to the policy

You can also use lower-level methods like `String compute(String unix, File file)` if you'd like to do lower-level processing.

All `FormatterStep` implement `Serializable`, `equals`, and `hashCode`, so build systems that support up-to-date checks can easily and correctly determine if any actions need to be taken.

Spotless also provides `PaddedCell`, which makes it easy to diagnose and correct idempotence problems.

## Project layout

For the folders below in monospace text, they are published on maven central at the coordinate `com.diffplug.spotless:spotless-${FOLDER_NAME}`.  The other folders are dev infrastructure.

| Folder | Description |
| ------ | ----------- |
| `lib` | Contains all of Spotless' core infrastructure and most of its `FormatterStep` - has no external dependencies. |
| `testlib` | Contains testing infrastructure, as well as all tests for `spotless-lib`, since those tests need the testing infrastructure in `testlib`. |
| `lib-extra` | Contains the optional parts of Spotless which require external dependencies.  `LineEnding.GIT_ATTRIBUTES` won't work unless `lib-extra` is available. |
| `plugin-gradle` | Integrates spotless and all of its formatters into Gradle. |
| `plugin-maven` | Integrates spotless and all of its formatters into Maven. |
| javadoc-publish | Logic for publishing javadoc to github-pages. |
| ide | Generates and launches an IDE for developing spotless. |
| _ext | Folder for generating glue jars (specifically packaging Eclipse jars from p2 for consumption using maven).

## How to add a new FormatterStep

The easiest way to create a FormatterStep is `FormatterStep createNeverUpToDate(String name, FormatterFunc function)`, which you can use like this:

```java
FormatterStep identityStep = FormatterStep.createNeverUpToDate("identity", unix -> unix)
```

This creates a step which will fail up-to-date checks (it is equal only to itself), and will use the function you passed in to do the formatting pass.

To create a step which can handle up-to-date checks properly, use the method `<State extends Serializable> FormatterStep create(String name, State state, Function<State, FormatterFunc> stateToFormatter)`.  Here's an example:

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

		private final CharSequence target;
		private final CharSequence replacement;

		State(CharSequence target, CharSequence replacement) {
			this.target = target;
			this.replacement = replacement;
		}

		FormatterFunc toFormatter() {
			return raw -> raw.replace(target, replacement);
		}
	}
}
```

The `FormatterStep` created above implements `equals` and `hashCode` based on the serialized representation of its `State`.  This trick makes it quick and easy to write steps which properly support up-to-date checks.

Oftentimes, a rule's state will be expensive to compute. `EclipseFormatterStep`, for example, depends on a formatting file.  Ideally, we would like to only pay the cost of the I/O needed to load that file if we have to - we'd like to create the FormatterStep now but load its state lazily at the last possible moment.  For this purpose, each of the `FormatterStep.create` methods has a lazy counterpart.  Here are their signatures:

```java
FormatterStep createNeverUpToDate    (String name, FormatterFunc function                  )
FormatterStep createNeverUpToDateLazy(String name, Supplier<FormatterFunc> functionSupplier)
FormatterStep create    (String name, State state                  , Function<State, FormatterFunc> stateToFormatter)
FormatterStep createLazy(String name, Supplier<State> stateSupplier, Function<State, FormatterFunc> stateToFormatter)
```

If your formatting step only needs to call one or two methods of the external dependency, you can pull it in at runtime and call it via reflection.  See the logic for [`EclipseFormatterStep`](lib-extra/src/main/java/com/diffplug/spotless/extra/java/EclipseFormatterStep.java) or [`GoogleJavaFormatStep`](lib/src/main/java/com/diffplug/spotless/java/GoogleJavaFormatStep.java).

## How to add a new plugin for a build system

The gist of it is that you will have to:

- Use the build system's user-interface to define a "format" as a set of files, and the list of `FormatterStep` the user would like enforced on those files.
- Use the build system's execution logic to create a `Formatter` with the appropriate `FormatterStep`, and pass it the files to be formatted and/or checked.
- To use the good `FormatterStep` like `EclipseFormatterStep` or `GoogleJavaFormatStep`, you'll need to implement `Provisioner`, which is a generic API for the build system's native mechanism for resolving dependencies from maven: `Set<File> provisionWithDependencies(Collection<String> mavenCoordinates)`.
- (Optional) Tie into the build system's native up-to-date mechanism.
- (Optional) Use `PaddedCell` to proactively catch and resolve idempotence issues.

`plugin-gradle` is the canonical example which uses everything that Spotless has to offer.  It's only ~700 lines.

If you get something running, we'd love to host your plugin within this repo as a peer to `plugin-gradle` and `plugin-maven`.

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
