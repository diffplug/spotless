/*
 * Copyright 2016-2020 DiffPlug
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

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.common.io.Files;

public class IdeHookTest extends GradleIntegrationHarness {
	private String output, error;
	private File dirty, clean, diverge, outofbounds;

	@Before
	public void before() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"  id 'com.diffplug.gradle.spotless'",
				"}",
				"spotless {",
				"  format 'misc', {",
				"    target 'DIRTY.md', 'CLEAN.md'",
				"    custom 'lowercase', { str -> str.toLowerCase(Locale.ROOT) }",
				"  }",
				"  format 'diverge', {",
				"    target 'DIVERGE.md'",
				"    custom 'diverge', { str -> str + ' ' }",
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

	private void runWith(String... arguments) throws IOException {
		StringBuilder output = new StringBuilder();
		StringBuilder error = new StringBuilder();
		try (Writer outputWriter = new StringPrinter(output::append).toWriter();
				Writer errorWriter = new StringPrinter(error::append).toWriter();) {
			// gradle 2.14 -> 4.6 confirmed to work
			// gradle 4.7 -> 5.1 don't work in tooling API because of https://github.com/gradle/gradle/issues/7617
			// gradle 5.1 -> current confirmed to work
			gradleRunner()
					.withGradleVersion(GradleVersionSupport.MODERN.version)
					.withProjectDir(rootFolder())
					.withPluginClasspath()
					.withArguments(arguments)
					.forwardStdOutput(outputWriter)
					.forwardStdError(errorWriter)
					.build();
		}
		this.output = output.toString();
		this.error = error.toString();
	}

	@Test
	public void dirty() throws IOException {
		runWith("spotlessApply", "--quiet", "-PspotlessIdeHook=" + dirty.getAbsolutePath(), "-PspotlessIdeHookUseStdOut");
		Assertions.assertThat(output).isEqualTo("abc");
		Assertions.assertThat(error).startsWith("IS DIRTY");
	}

	@Test
	public void clean() throws IOException {
		runWith("spotlessApply", "--quiet", "-PspotlessIdeHook=" + clean.getAbsolutePath(), "-PspotlessIdeHookUseStdOut");
		Assertions.assertThat(output).isEmpty();
		Assertions.assertThat(error).startsWith("IS CLEAN");
	}

	@Test
	public void diverge() throws IOException {
		runWith("spotlessApply", "--quiet", "-PspotlessIdeHook=" + diverge.getAbsolutePath(), "-PspotlessIdeHookUseStdOut");
		Assertions.assertThat(output).isEmpty();
		Assertions.assertThat(error).startsWith("DID NOT CONVERGE");
	}

	@Test
	public void outofbounds() throws IOException {
		runWith("spotlessApply", "--quiet", "-PspotlessIdeHook=" + outofbounds.getAbsolutePath(), "-PspotlessIdeHookUseStdOut");
		Assertions.assertThat(output).isEmpty();
		Assertions.assertThat(error).isEmpty();
	}

	@Test
	public void notAbsolute() throws IOException {
		runWith("spotlessApply", "--quiet", "-PspotlessIdeHook=build.gradle", "-PspotlessIdeHookUseStdOut");
		Assertions.assertThat(output).isEmpty();
		Assertions.assertThat(error).contains("Argument passed to spotlessIdeHook must be an absolute path");
	}
}
