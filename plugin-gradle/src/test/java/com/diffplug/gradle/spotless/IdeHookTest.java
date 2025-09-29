/*
 * Copyright 2016-2025 DiffPlug
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

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.common.io.Files;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdeHookTest extends GradleIntegrationHarness {
	private String output;
	private String error;
	private File dirty;
	private File clean;
	private File diverge;
	private File outofbounds;

	@BeforeEach
	void before() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"  id 'com.diffplug.spotless'",
				"}",
				"spotless {",
				"  format 'misc', {",
				"    target 'DIRTY.md', 'CLEAN.md'",
				"    addStep com.diffplug.spotless.TestingOnly.lowercase()",
				"  }",
				"  format 'diverge', {",
				"    target 'DIVERGE.md'",
				"    addStep com.diffplug.spotless.TestingOnly.diverge()",
				"  }",
				"}");
		dirty = new File(rootFolder(), "DIRTY.md");
		Files.write("ABC".getBytes(StandardCharsets.UTF_8), dirty);
		clean = new File(rootFolder(), "CLEAN.md");
		Files.write("abc".getBytes(StandardCharsets.UTF_8), clean);
		diverge = new File(rootFolder(), "DIVERGE.md");
		Files.write("ABC".getBytes(StandardCharsets.UTF_8), diverge);
		outofbounds = new File(rootFolder(), "OUTOFBOUNDS.md");
		Files.write("ABC".getBytes(StandardCharsets.UTF_8), outofbounds);
	}

	private static Stream<Boolean> configurationCacheProvider() {
		return Stream.of(false, true);
	}

	private void runWith(boolean configurationCache, String... arguments) throws IOException {
		StringBuilder output = new StringBuilder();
		StringBuilder error = new StringBuilder();
		try (Writer outputWriter = new StringPrinter(output::append).toWriter();
				Writer errorWriter = new StringPrinter(error::append).toWriter()) {
			gradleRunner(configurationCache)
					.withArguments(arguments)
					.forwardStdOutput(outputWriter)
					.forwardStdError(errorWriter)
					.build();
		}
		this.output = output.toString();
		this.error = error.toString();
	}

	protected GradleRunner gradleRunner(boolean configurationCache) throws IOException {
		if (configurationCache) {
			setFile("gradle.properties").toContent("org.gradle.unsafe.configuration-cache=true");
			setFile("settings.gradle").toContent("enableFeaturePreview(\"STABLE_CONFIGURATION_CACHE\")");
			return super.gradleRunner().withGradleVersion(GradleVersionSupport.STABLE_CONFIGURATION_CACHE.version);
		} else {
			File gradleProps = new File(rootFolder(), "gradle.properties");
			if (gradleProps.exists()) {
				gradleProps.delete();
			}
			File settingsGradle = new File(rootFolder(), "settings.gradle");
			if (settingsGradle.exists()) {
				settingsGradle.delete();
			}
			return super.gradleRunner();
		}
	}

	@ParameterizedTest
	@MethodSource("configurationCacheProvider")
	void dirty(boolean configurationCache) throws IOException {
		runWith(configurationCache, "spotlessApply", "--quiet", "-PspotlessIdeHook=" + dirty.getAbsolutePath(), "-PspotlessIdeHookUseStdOut");
		Assertions.assertThat(output).isEqualTo("abc");
		Assertions.assertThat(error).startsWith("IS DIRTY");
	}

	@ParameterizedTest
	@MethodSource("configurationCacheProvider")
	void clean(boolean configurationCache) throws IOException {
		runWith(configurationCache, "spotlessApply", "--quiet", "-PspotlessIdeHook=" + clean.getAbsolutePath(), "-PspotlessIdeHookUseStdOut");
		Assertions.assertThat(output).isEmpty();
		Assertions.assertThat(error).startsWith("IS CLEAN");
	}

	@ParameterizedTest
	@MethodSource("configurationCacheProvider")
	void diverge(boolean configurationCache) throws IOException {
		runWith(configurationCache, "spotlessApply", "--quiet", "-PspotlessIdeHook=" + diverge.getAbsolutePath(), "-PspotlessIdeHookUseStdOut");
		Assertions.assertThat(output).isEmpty();
		Assertions.assertThat(error).startsWith("DID NOT CONVERGE");
	}

	@ParameterizedTest
	@MethodSource("configurationCacheProvider")
	void outofbounds(boolean configurationCache) throws IOException {
		runWith(configurationCache, "spotlessApply", "--quiet", "-PspotlessIdeHook=" + outofbounds.getAbsolutePath(), "-PspotlessIdeHookUseStdOut");
		Assertions.assertThat(output).isEmpty();
		Assertions.assertThat(error).isEmpty();
	}

	@ParameterizedTest
	@MethodSource("configurationCacheProvider")
	void notAbsolute(boolean configurationCache) throws IOException {
		runWith(configurationCache, "spotlessApply", "--quiet", "-PspotlessIdeHook=build.gradle", "-PspotlessIdeHookUseStdOut");
		Assertions.assertThat(output).isEmpty();
		Assertions.assertThat(error).contains("Argument passed to spotlessIdeHook must be an absolute path");
	}
}
