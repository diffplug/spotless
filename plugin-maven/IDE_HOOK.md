# Spotless Maven IDE integrations

## How to add an IDE

The Spotless plugin for Maven accepts a command-line argument `-DspotlessIdeHook=${ABSOLUTE_PATH_TO_FILE}`.  In this mode, `spotless:check` is disabled, and `spotless:apply` will apply only to that one file.  Because it already knows the absolute path of the only file you are asking about, it is able to run much faster than a normal invocation of `spotless:apply`.

For extra flexibility, you can add `-DspotlessIdeHookUseStdIn`, and Spotless will read the file content from `stdin`.  This allows you to send the content of a dirty editor buffer without writing to a file.  You can also add `-DspotlessIdeHookUseStdOut`, and Spotless will return the formatted content on `stdout` rather than writing it to a file (you should also add `--quiet` to make sure Maven doesn't dump logging info into `stdout`).

In all of these cases, Spotless will send useful status information on `stderr`:

- if `stderr` starts with `IS DIRTY`, then the file was dirty, and `stdout` contains its full formatted contents
  - in every other case, `stdout` will be empty / the file will be unchanged because there is nothing to change
- if `stderr` starts with `IS CLEAN`, then the file is already clean
- if `stderr` starts with `DID NOT CONVERGE`, then the formatter is misbehaving, and the rest of `stderr` has useful diagnostic info (e.g. `spotless:diagnose` for [padded cell](../PADDEDCELL.md))
- if `stderr` is empty, then the file is not being formatted by spotless (not included in any target)
- if `stderr` is anything else, then it is the stacktrace of whatever went wrong
