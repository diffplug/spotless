/*
 * Copyright 2026 DiffPlug
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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests the behavior requested in https://github.com/diffplug/spotless/issues/2715. */
class SpotlessApplyChangedFilesTest extends GradleIntegrationHarness {
	private static final String DEFAULT_REPORT = "build/spotless/spotlessMiscApply-changed-files.txt";

	/** Lowercases every targeted file, and fails with a lint on any file containing "fubar". */
	private void writeBuildFile(String... extraLines) throws IOException {
		StringBuilder build = new StringBuilder();
		build.append("plugins {\n")
				.append("    id 'com.diffplug.spotless'\n")
				.append("}\n")
				.append("spotless {\n")
				.append("    format 'misc', {\n")
				.append("        target 'src/*.md'\n")
				.append("        custom 'lowercase', { str -> str.toLowerCase(Locale.ROOT) }\n")
				.append("        custom 'noSwearingStep', {\n")
				.append("            if (it.contains('fubar')) {\n")
				.append("                throw com.diffplug.spotless.Lint.atUndefinedLine('swearing', 'No swearing!').shortcut()\n")
				.append("            }\n")
				.append("        }\n")
				.append("        bumpThisNumberIfACustomStepChanges(1)\n")
				.append("    }\n")
				.append("}\n");
		for (String line : extraLines) {
			build.append(line).append('\n');
		}
		setFile("build.gradle").toContent(build.toString());
	}

	private String report(String path) throws IOException {
		File report = rootFolder().toPath().resolve(path).toFile();
		assertThat(report).as("report " + path).exists();
		return new String(Files.readAllBytes(report.toPath()), StandardCharsets.UTF_8);
	}

	@Test
	@DisplayName("given unformatted files, when spotlessApply runs, then it does work and the report lists exactly the rewritten files")
	void reportsRewrittenFiles() throws IOException {
		// given
		writeBuildFile();
		setFile("src/b.md").toContent("BBB");
		setFile("src/a.md").toContent("AAA");
		setFile("src/clean.md").toContent("already clean");

		// when
		BuildResult result = gradleRunner().withArguments("spotlessApply").build();

		// then
		assertThat(result.task(":spotlessMiscApply").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertFile("src/a.md").hasContent("aaa");
		assertFile("src/b.md").hasContent("bbb");
		assertThat(report(DEFAULT_REPORT)).isEqualTo("src/a.md\nsrc/b.md\n");
	}

	@Test
	@DisplayName("given only clean files, when spotlessApply runs, then it reports UP-TO-DATE and writes an empty report")
	void noChangesMeansNoWork() throws IOException {
		// given
		writeBuildFile();
		setFile("src/clean.md").toContent("already clean");

		// when
		BuildResult result = gradleRunner().withArguments("spotlessApply").build();

		// then the format task did run, but apply wrote nothing
		assertThat(result.task(":spotlessMisc").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.task(":spotlessMiscApply").getOutcome()).isEqualTo(TaskOutcome.UP_TO_DATE);
		assertThat(report(DEFAULT_REPORT)).isEmpty();
	}

	@Test
	@DisplayName("given a previous run rewrote files, when spotlessApply runs again with nothing to fix, then the report is reset to empty")
	void reportReflectsMostRecentRunOnly() throws IOException {
		// given
		writeBuildFile();
		setFile("src/a.md").toContent("AAA");
		gradleRunner().withArguments("spotlessApply").build();
		assertThat(report(DEFAULT_REPORT)).isEqualTo("src/a.md\n");

		// when
		pauseForFilesystem();
		BuildResult result = gradleRunner().withArguments("spotlessApply").build();

		// then
		assertThat(result.task(":spotlessMiscApply").getOutcome()).isEqualTo(TaskOutcome.UP_TO_DATE);
		assertThat(report(DEFAULT_REPORT)).isEmpty();
	}

	@Test
	@DisplayName("given one fixable file and one file with a lint, when spotlessApply fails on the lint, then the report still lists the fixed file")
	void reportIsWrittenEvenWhenLintsFail() throws IOException {
		// given
		writeBuildFile();
		setFile("src/a.md").toContent("AAA");
		setFile("src/swear.md").toContent("fubar");

		// when
		gradleRunner().withArguments("spotlessApply").buildAndFail();

		// then
		assertFile("src/a.md").hasContent("aaa");
		assertThat(report(DEFAULT_REPORT)).isEqualTo("src/a.md\n");
	}

	@Test
	@DisplayName("given a build script that reads a custom report location in doLast, when spotlessApply rewrites files, then the build can print them")
	void customReportLocationIsReadableFromBuildScript() throws IOException {
		// given
		writeBuildFile(
				"tasks.named('spotlessMiscApply') {",
				"    changedFilesReport = layout.buildDirectory.file('custom/changed.txt')",
				"    def report = changedFilesReport",
				"    doLast {",
				"        if (didWork) {",
				"            report.get().asFile.readLines().each { println \"spotless fixed: $it\" }",
				"        }",
				"    }",
				"}");
		setFile("src/a.md").toContent("AAA");

		// when
		BuildResult result = gradleRunner().withArguments("spotlessApply").build();

		// then
		assertThat(report("build/custom/changed.txt")).isEqualTo("src/a.md\n");
		assertThat(result.getOutput()).contains("spotless fixed: src/a.md");
		assertThat(rootFolder().toPath().resolve(DEFAULT_REPORT).toFile()).doesNotExist();
	}
}
