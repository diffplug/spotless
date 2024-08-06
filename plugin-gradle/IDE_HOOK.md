# Spotless Gradle IDE integrations

Thanks to `spotlessApply`, it is not necessary for Spotless and your IDE to agree on formatting - you can always run spotless at the end to fix things up.  But if you want them to agree, we have plugins for:

- [VS Code](https://marketplace.visualstudio.com/items?itemName=richardwillis.vscode-spotless-gradle)
- [IntelliJ](https://plugins.jetbrains.com/plugin/18321-spotless-gradle)
- (add your IDE here!)

## How to add an IDE

The Spotless plugin for Gradle accepts a command-line argument `-PspotlessIdeHook=${ABSOLUTE_PATH_TO_FILE}`.  In this mode, `spotlessCheck` is disabled, and `spotlessApply` will apply only to that one file.  Because it already knows the absolute path of the only file you are asking about, it is able to run much faster than a normal invocation of `spotlessApply`.

For extra flexibility, you can add `-PspotlessIdeHookUseStdIn`, and Spotless will read the file content from `stdin`.  This allows you to send the content of a dirty editor buffer without writing to a file.  You can also add `-PspotlessIdeHookUseStdOut`, and Spotless will return the formatted content on `stdout` rather than writing it to a file (you should also add `--quiet` to make sure Gradle doesn't dump logging info into `stdout`).

In all of these cases, Spotless will send useful status information on `stderr`:

- if `stderr` starts with `IS DIRTY`, then the file was dirty, and `stdout` contains its full formatted contents
  - in every other case, `stdout` will be empty / the file will be unchanged because there is nothing to change
- if `stderr` starts with `IS CLEAN`, then the file is already clean
- if `stderr` starts with `DID NOT CONVERGE`, then the formatter is misbehaving, and the rest of `stderr` has useful diagnostic info (e.g. `spotlessDiagnose` for [padded cell](../PADDEDCELL.md))
- if `stderr` is empty, then the file is not being formatted by spotless (not included in any target)
- if `stderr` is anything else, then it is the stacktrace of whatever went wrong

See the VS Code extension above for a working example, or [the PR](https://github.com/diffplug/spotless/pull/568) where this feature was added for more context.

## Compatibility considerations and future plans

### Configuration cache

- Gradle 6.6 introduced the [configuration-cache](https://docs.gradle.org/6.6/release-notes.html#performance-improvements)
- The Spotless IDE hook does not work with the configuration-cache, but this might change in a future version of Spotless ([#1082](https://github.com/diffplug/spotless/issues/1082))
- A user might have enabled configuration-cache with `org.gradle.unsafe.configuration-cache=true` in a `gradle.properties` file
- In that case, the IDE plugin must pass `--no-configuration-cache` at the command line or the user will get an error.
- You could pass that flag all the time, but the flag will generate "unrecognized flag" errors in Gradle < 6.6.

### Configure on demand

This is not yet supported, but in a future version of Spotless you might be able to improve speed dramatically by calling `:spotlessApply --configure-on-demand` rather than `spotlessApply` (note the leading colon). This is being tracked in [#1101](https://github.com/diffplug/spotless/issues/1101).

### Lint as well as format

Spotless is in the process of adding support for linters ([#1097](https://github.com/diffplug/spotless/pull/1097)). When this gets added, Spotless will be able to provide a list of things like `L5: NPE on blah blah blah` and `L8-14: leaking resource` etc.

We will provide those lint errors over stderr like so `IS DIRTY\nLINTS` or `IS CLEAN\nLINTS`, and then provide the list of lints in a TBD format. The key thing is that if you are correctly using `startsWith("IS DIRTY")` then older versions of your plugin are future-compatible.
