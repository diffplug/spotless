/*
 * Copyright 2025-2026 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.gradle.spotless;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

/**
 * End-to-end regression for {@code HackClone.writeObject} with Gradle task input
 * fingerprinting.
 *
 * <p>The original failure used:
 *
 * <pre><code>
 * groovyGradle {
 *     toggleOffOn()                // adds a FenceStep
 *     target '**&#47;*.gradle'     // no files matching this target
 *     greclipse()                  // lazy slow step
 * }
 * </code></pre>
 *
 * <p>{@code toggleOffOn()} creates a wrapper step that preserves text between the off/on markers and
 * stores {@code greclipse()} in a lazy {@code FormatterStep} containing a nested
 * {@code ConfigurationCacheHackList}. As no files match the target, the slow {@code greclipse} state
 * is first initialized while Gradle fingerprints task inputs, before task actions format any files.
 *
 * <p>The race is not specific to {@code greclipse} formatting. It only needs a lazy {@code FormatterStep}
 * whose state supplier stays inside {@code HackClone.writeObject} long enough for another Gradle
 * worker to serialize the same half-built clone. {@code greclipse()} provided that delay by resolving
 * Eclipse state.
 *
 * <p>The real build configures a separate {@code greclipse()} step for each subproject. This test
 * deliberately reuses one local slow no-op wrapper step across subprojects so parallel Gradle workers
 * serialize the same nested {@code HackClone}. That makes the concurrency window deterministic while
 * avoiding Eclipse provisioning and still using Gradle's real input serialization path.
 *
 * <p>That shared-object setup is intentionally not an isolated-projects-compatible build pattern:
 * isolated projects remove this kind of cross-project shared configuration state, while this
 * reproducer needs it to force concurrent serialization of one {@code HackClone}.
 */
class HackCloneRaceIntegrationTest extends GradleIntegrationHarness {
	private static final int SUBPROJECTS = 12;
	private static final int ATTEMPTS = 3;
	private static final int STATE_INIT_SLEEP_MS = 500;

	@Test
	void realGradleParallelFingerprintingOfNestedLazyStepIsThreadSafe() throws IOException {
		StringBuilder settings = new StringBuilder("rootProject.name = 'race'\n");
		for (int i = 0; i < SUBPROJECTS; i++) {
			settings.append("include 'sub").append(i).append("'\n");
			setFile("sub" + i + "/.keep").toContent("");
		}
		setFile("settings.gradle").toContent(settings.toString());
		setFile("gradle.properties").toContent("org.gradle.jvmargs=-Xmx1g\n");

		setFile("build.gradle").toContent(String.join("\n",
				"plugins { id 'com.diffplug.spotless' }",
				"",
				"class SlowStateSupplier implements com.diffplug.spotless.ThrowingEx.Supplier<String>, Serializable {",
				"    private final long sleepMillis",
				"    SlowStateSupplier(long sleepMillis) { this.sleepMillis = sleepMillis }",
				"    String get() { Thread.sleep(sleepMillis); return 'state' }",
				"}",
				"",
				"class NoopFormatter implements com.diffplug.spotless.FormatterFunc, Serializable {",
				"    String apply(String input) { return input }",
				"}",
				"",
				"def hackCloneRaceStep",
				"",
				"subprojects {",
				"    apply plugin: 'com.diffplug.spotless'",
				"    spotless {",
				"        if (hackCloneRaceStep == null) {",
				"            def slowStep = com.diffplug.spotless.FormatterStep.createLazy(",
				"                'slowLazy',",
				"                new SlowStateSupplier(" + STATE_INIT_SLEEP_MS + "L),",
				"                com.diffplug.spotless.SerializedFunction.identity(),",
				"                com.diffplug.spotless.SerializedFunction.alwaysReturns(new NoopFormatter()))",
				"            hackCloneRaceStep = com.diffplug.spotless.generic.FenceStep",
				"                .named('toggle')",
				"                .openClose('spotless:off', 'spotless:on')",
				"                .preserveWithin([slowStep])",
				"        }",
				"        format 'race', {",
				"            target 'no-such-dir/**/*.txt'",
				"            addStep(hackCloneRaceStep)",
				"        }",
				"    }",
				"}"));

		for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
			BuildResult result = gradleRunner()
					.withGradleVersion("9.6.1")
					.withArguments("spotlessRace", "--parallel", "--build-cache", "--rerun-tasks", "--max-workers=" + SUBPROJECTS, "--stacktrace")
					.build();
			assertThat(result.getOutput())
					.as("attempt %d must not trip the HackClone.writeObject guard", attempt)
					.doesNotContain("roundtripStateInternal or equalityStateInternal should be non-null");
		}
	}
}
