/*
 * Copyright 2016-2021 DiffPlug
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
import java.util.function.Consumer;

import org.assertj.core.api.Assertions;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.common.io.Files;

class ApplyFileTest extends GradleIntegrationHarness {
	private String output, error;
	private File dirty, clean, diverge, outOfBounds;

	@BeforeEach
	void before() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"  id 'com.diffplug.spotless'",
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
				"}",
				"import com.diffplug.gradle.spotless.SpotlessApplyResult",
				"task customTask {",
				"  doLast {",
				"    SpotlessApplyResult result = spotless.applyFile(new File(project.properties.customTaskFile))",
				"    System.err << result",
				"  }",
				"}");
		dirty = new File(rootFolder(), "DIRTY.md");
		Files.write("ABC".getBytes(StandardCharsets.UTF_8), dirty);
		clean = new File(rootFolder(), "CLEAN.md");
		Files.write("abc".getBytes(StandardCharsets.UTF_8), clean);
		diverge = new File(rootFolder(), "DIVERGE.md");
		Files.write("ABC".getBytes(StandardCharsets.UTF_8), diverge);
		outOfBounds = new File(rootFolder(), "OUT_OF_BOUNDS.md");
		Files.write("ABC".getBytes(StandardCharsets.UTF_8), outOfBounds);
	}

	private void runWith(String... arguments) throws IOException {
		runWith(GradleRunner::build, arguments);
	}

	private void runToFail(String... arguments) throws IOException {
		runWith(GradleRunner::buildAndFail, arguments);
	}

	private void runWith(Consumer<GradleRunner> runner, String... arguments) throws IOException {
		StringBuilder output = new StringBuilder();
		StringBuilder error = new StringBuilder();
		try (Writer outputWriter = new StringPrinter(output::append).toWriter();
				Writer errorWriter = new StringPrinter(error::append).toWriter()) {
			runner.accept(
					gradleRunner()
							.withArguments(arguments)
							.forwardStdOutput(outputWriter)
							.forwardStdError(errorWriter));
		} finally {
			this.output = output.toString();
			this.error = error.toString();
		}
	}

	@Test
	void dirty() throws IOException {
		runWith("customTask", "--quiet", "-PcustomTaskFile=" + dirty.getAbsolutePath());
		Assertions.assertThat(output).isEmpty();
		Assertions.assertThat(error).isEqualTo(SpotlessApplyResult.DIRTY.name());
		Assertions.assertThat(Files.toString(dirty, StandardCharsets.UTF_8)).isEqualTo("abc");
	}

	@Test
	void clean() throws IOException {
		runWith("customTask", "--quiet", "-PcustomTaskFile=" + clean.getAbsolutePath());
		Assertions.assertThat(output).isEmpty();
		Assertions.assertThat(error).isEqualTo(SpotlessApplyResult.CLEAN.name());
	}

	@Test
	void diverge() throws IOException {
		runWith("customTask", "--quiet", "-PcustomTaskFile=" + diverge.getAbsolutePath());
		Assertions.assertThat(output).isEmpty();
		Assertions.assertThat(error).isEqualTo(SpotlessApplyResult.DID_NOT_CONVERGE.name());
	}

	@Test
	void outOfBounds() throws IOException {
		runWith("customTask", "--quiet", "-PcustomTaskFile=" + outOfBounds.getAbsolutePath());
		Assertions.assertThat(output).isEmpty();
		Assertions.assertThat(error).isEqualTo(SpotlessApplyResult.OUT_OF_BOUNDS.name());
	}

	@Test
	void notAbsolute() throws IOException {
		runToFail("customTask", "--quiet", "--stacktrace", "-PcustomTaskFile=build.gradle");
		Assertions.assertThat(output).isEmpty();
		Assertions.assertThat(error).contains("java.lang.IllegalArgumentException: File must be an absolute path: build.gradle");
	}
}
