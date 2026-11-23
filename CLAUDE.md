# CLAUDE.md — Spotless

Spotless is a multi-language code formatter. Core formatting logic lives in `lib` / `lib-extra` as
`FormatterStep`s; `plugin-gradle` and `plugin-maven` expose them via build-tool DSLs. Spotless formats
itself with Spotless. Contributor docs: `CONTRIBUTING.md`, `PULL_REQUEST_TEMPLATE.md`.

## Build & test

Gradle wrapper (9.x), JDK 17+ to run (CI builds on 21, tests on 17/21/24). All Java compiles with
`--release 17` (`VER_JAVA` in `gradle.properties`). Parallel build, build cache and configuration cache
are on by default (`gradle.properties`).

```bash
./gradlew spotlessApply                     # format everything (Java, *.gradle, dotfiles, *.md via freshmark)
./gradlew spotlessCheck                     # what CI runs first ("Sanity Check")
./gradlew assemble testClasses              # CI sanity step 2
./gradlew spotbugsMain                      # spotbugs is fail-on-error; lib/lib-extra use LOW threshold
./gradlew test                              # all default tests (slow)
./gradlew :testlib:test --tests com.diffplug.spotless.generic.IndentStepTest
./gradlew :lib-extra:test --tests '<fqcn>'
./gradlew :plugin-gradle:test --tests com.diffplug.gradle.spotless.FreshMarkExtensionTest
./gradlew :plugin-maven:test --tests com.diffplug.spotless.maven.pom.SortPomMavenTest
./gradlew build -x spotlessCheck -PSPOTLESS_EXCLUDE_MAVEN=true   # CI "gradle" job (skips plugin-maven)
./gradlew :plugin-maven:build -x spotlessCheck                    # CI "maven" job
./gradlew publishToMavenLocal               # try a snapshot in another project
```

- `-PSPOTLESS_EXCLUDE_MAVEN=true` (or env var) drops `plugin-maven` from `settings.gradle` → faster config.
- `plugin-maven` tests automatically depend on `publishToMavenLocal` of `lib`, `lib-extra`, `plugin-maven`.
- Error Prone only runs when env var `error-prone=true` is set.
- OpenRewrite is configured (`gradle/rewrite.gradle`, recipe in `rewrite.yml`) but is not part of CI.

### Special (tagged) tests — excluded from `test`

Defined in `gradle/special-tests.gradle`; tags are annotations in `testlib/src/main/java/com/diffplug/spotless/tag/`
(`@NpmTest`, `@ClangTest`, `@BlackTest`, `@BufTest`, `@GofmtTest`, `@IdeaTest`, `@ShfmtTest`).
Run with `./gradlew testNpm | testClang | testBlack | testBuf | testGofmt | testIdea | testShfmt`
(per module, e.g. `:testlib:testNpm`). They need the external tool on `PATH` (node/npm, clang-format,
black, buf, gofmt, idea, shfmt). Optional `testenv.properties` at repo root (gitignored) is passed via
`testenv.properties.path`. CI runs only `testNpm`, `testShfmt`, `testIdea`.

## Module map

| Module | Role |
|---|---|
| `lib` | Core API + most steps. **Zero runtime dependencies** (hard rule). |
| `lib-extra` | Steps needing real deps: Eclipse (JDT/CDT/Groovy via P2 + `solstice`), JGit (`GitAttributesLineEndings`). |
| `testlib` | Test harnesses (`src/main`), **tests for `lib`** (`src/test`), all test resources (`src/main/resources`). |
| `plugin-gradle` | Gradle DSL: `SpotlessExtension`, `FormatExtension`, per-language `*Extension` classes. |
| `plugin-maven` | Maven DSL: one `FormatterStepFactory` class per step under `maven/<lang>/`, registered via `addXxx(...)` in the language class (e.g. `maven/java/Java.java`). |

`lib/src/test` exists but only for a few lib-internal unit tests; step tests go in `testlib/src/test`.

## Architecture rules (MUST / MUST NOT)

- MUST NOT add runtime deps to `lib`. Third-party formatters are loaded at runtime via `JarState` +
  `Provisioner`, called either by reflection or from a **glue source set**:
  `lib/src/<glue>/java` with `<glue>CompileOnly` deps, listed in `NEEDS_GLUE` in `lib/build.gradle`
  (alphabetical) and merged into the `lib` jar. Eclipse-based glue in `lib-extra` uses `NEEDS_P2_DEPS`.
- Multi-version adapters (ktlint, diktat, cleanthat) use the `version-compatibility` plugin
  (`compat<Name><Version>` source sets); `check` runs `testCompatibility*`.
- `FormatterStep` input/output is unix-newline only. Use `FormatterFunc.NeedsFile` only if the step truly
  needs the path (never read the file's bytes).
- Steps MUST be `Serializable` with equality from serialized state, and roundtrip-serializable
  (Gradle configuration cache). Use `FormatterStep.create/createLazy(name, state, State::toFormatter)`, or
  the RoundtripState → EqualityState variant with `FileSignature.Promised` / `JarState.Promised` for
  files/classpaths. Put absolute paths in `transient` fields so they don't affect equality.
- Formatting problems that aren't fixable → `Lint` (`Lint.atLine(...)` or override `lint(...)`), not raw
  exceptions. Lints fail the build by default; don't add them lightly.
- plugin-gradle: MUST NOT import `org.gradle.api.internal.*` (enforced by a `forbidRegex` spotless step).
- Glue / `NEEDS_GLUE` / dependency lists: keep alphabetical order (comments in build files demand it).

## Adding or changing a FormatterStep — checklist

1. `lib/src/main/java/com/diffplug/spotless/<lang>/FooStep.java` (or `lib-extra/...`), name ends in `Step`,
   `public static FormatterStep create(...)`; third-party API in a glue source set if needed.
2. Test `testlib/src/test/java/com/diffplug/spotless/<lang>/FooStepTest.java`:
   `StepHarness.forStep(step).testResource("<lang>/before.test", "<lang>/after.test")`
   (`StepHarnessWithFile` if path matters; `expectLintsOf(...)` + selfie for lints) plus an `equality()` test
   using `SerializableEqualityTester` (CONTRIBUTING.md says `StepEqualityTester` — that class no longer exists).
   Resources go in `testlib/src/main/resources/<lang>/`; that dir is exempt from editorconfig/formatting.
   Third-party deps in tests: `TestProvisioner.mavenCentral()`.
3. Gradle DSL: method on the matching `plugin-gradle/.../<Lang>Extension.java` (calls `addStep(...)`);
   integration test extends `GradleIntegrationHarness`.
4. Maven DSL: `plugin-maven/.../maven/<lang>/Foo.java implements FormatterStepFactory` + `addFoo(Foo)` in
   the language class; integration test extends `MavenIntegrationHarness` (`writePomWithJavaSteps(...)` etc.).
5. Docs: `plugin-gradle/README.md`, `plugin-maven/README.md`, and the feature matrix in root `README.md` —
   edit the `lib('...')` / `extra('...')` source lines inside `<!---freshmark matrix ... -->`, then run
   `./gradlew spotlessApply` to regenerate the table. Never hand-edit the generated table.
6. Changelogs (below), then `./gradlew spotlessApply`.

## Changelogs (keepachangelog, required for every PR)

Files: `CHANGES.md` (lib + lib-extra), `plugin-gradle/CHANGES.md`, `plugin-maven/CHANGES.md`.
Add a bullet under `## [Unreleased]` → `### Added` / `### Fixed` / `### Changes`.
- Change only affects one plugin → only that plugin's `CHANGES.md`.
- End-user-visible lib change (bug fix, default version bump, new step) → **all three** files.
- Format: `- Summary sentence. ([#1234](https://github.com/diffplug/spotless/pull/1234))` — link the issue
  for small changes, the PR for larger ones. Version bumps: ``Bump default `ktfmt` version to latest `0.59` -> `0.61`.``
- Never edit released sections or version numbers (managed by `spotless-changelog` at release).

## Code style (enforced by `spotlessApply`)

Tabs (4), UTF-8, final newline (`.editorconfig`). Java: Eclipse formatter `gradle/spotless.eclipseformat.xml`,
import order `gradle/spotless.importorder` (java, javax, org, com, com.diffplug, then rest), no wildcard
imports, no unused imports, license header from `gradle/spotless.license` (new files get it automatically).
Java formatting uses `ratchetFrom 'origin/main'`: only files changed vs `origin/main` are formatted/checked,
so `origin/main` must exist locally (`git fetch origin main`). In a fork, `origin` is the fork — keep its
`main` synced with upstream or the ratchet base is stale.

## Definition of done (before PR)

- `./gradlew spotlessApply` leaves no diff; `./gradlew spotlessCheck` green.
- Relevant module tests green (`:testlib:test --tests ...`, plugin integration tests for DSL changes);
  `spotbugsMain` green for touched modules.
- Changelog entries in the right file(s); READMEs/matrix updated for new or changed DSL.
- No unrelated reformatting, no `testenv.properties` committed.
- PR against `diffplug/spotless:main`, "Allow edits from maintainers" on, and **never force-push** a PR branch
  (maintainers review incrementally).

## Gotchas

- Every module's test classpath pulls in the `lib` jar, which bundles all glue source sets → even a single
  `:testlib:test` run resolves all glue `compileOnly` deps (ktlint, diktat, scalafmt, …). First build is slow.
- `plugin-gradle` depends on project `lib`/`lib-extra` only when its version is `-SNAPSHOT` (or lib has no
  unreleased changes); otherwise on the last *published* lib version (see `plugin-gradle/build.gradle`).
- Some `testlib` tests need `--add-opens` (already configured in `testlib/build.gradle`); don't remove them.
- Snapshot tests use selfie; to overwrite snapshots run tests with `-Pselfie=overwrite`.
- Cloud sessions behind a rate-limited proxy: dependency downloads can fail with HTTP 429
  ("Could not resolve ..."). Re-run with `--no-parallel --max-workers=1`; each run caches more artifacts.
