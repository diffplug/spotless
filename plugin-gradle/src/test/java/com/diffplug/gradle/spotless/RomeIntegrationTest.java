/*
 * Copyright 2023 DiffPlug
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.owasp.encoder.Encode;

class RomeIntegrationTest extends GradleIntegrationHarness {
	/**
	 * Tests that rome can be used as a generic formatting step.
	 *
	 * @throws Exception When a test failure occurs.
	 */
	@Test
	void asGenericStep() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    format 'myrome', {",
				"        target '**/*.js'",
				"        rome('12.0.0')",
				"    }",
				"}");
		setFile("rome_test.js").toResource("rome/js/fileBefore.js");

		var spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("rome_test.js").sameAsResource("rome/js/fileAfter.js");
	}

	/**
	 * Tests that rome can be used as a JavaScript formatting step.
	 *
	 * @throws Exception When a test failure occurs.
	 */
	@Test
	void asJavaScriptStep() throws Exception {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    javascript {",
				"        target '**/*.js'",
				"        rome('12.0.0')",
				"    }",
				"}");
		setFile("rome_test.js").toResource("rome/js/fileBefore.js");

		var spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("rome_test.js").sameAsResource("rome/js/fileAfter.js");
	}

	/**
	 * Tests that rome can be used as a JSON formatting step.
	 *
	 * @throws Exception When a test failure occurs.
	 */
	@Test
	void asJsonStep() throws Exception {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    json {",
				"        target '**/*.json'",
				"        rome('12.0.0')",
				"    }",
				"}");
		setFile("rome_test.json").toResource("rome/json/fileBefore.json");

		var spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("rome_test.json").sameAsResource("rome/json/fileAfter.json");
	}

	/**
	 * Tests that rome can be used as a TypeScript formatting step.
	 *
	 * @throws Exception When a test failure occurs.
	 */
	@Test
	void asTypeScriptStep() throws Exception {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    typescript {",
				"        target '**/*.ts'",
				"        rome('12.0.0')",
				"    }",
				"}");
		setFile("rome_test.ts").toResource("rome/ts/fileBefore.ts");

		var spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("rome_test.ts").sameAsResource("rome/ts/fileAfter.ts");
	}

	/**
	 * Tests that the language can be specified for the generic format step.
	 *
	 * @throws Exception When a test failure occurs.
	 */
	@Test
	void canSetLanguageForGenericStep() throws Exception {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    format 'myrome', {",
				"        target '**/*.nosj'",
				"        rome('12.0.0').language('json')",
				"    }",
				"}");
		setFile("rome_test.nosj").toResource("rome/json/fileBefore.json");

		var spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("rome_test.nosj").sameAsResource("rome/json/fileAfter.json");
	}

	/**
	 * Tests that an absolute config path can be specified.
	 *
	 * @throws Exception When a test failure occurs.
	 */
	@Test
	void configPathAbsolute() throws Exception {
		var path = newFile("configs").getAbsolutePath();
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    format 'myrome', {",
				"        target '**/*.js'",
				"        rome('12.0.0').configPath('" + Encode.forJava(path) + "')",
				"    }",
				"}");
		setFile("rome_test.js").toResource("rome/js/longLineBefore.js");
		setFile("configs/rome.json").toResource("rome/config/line-width-120.json");

		var spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("rome_test.js").sameAsResource("rome/js/longLineAfter120.js");
	}

	/**
	 * Tests that a path to the directory with the rome.json config file can be
	 * specified. Uses a config file with a line width of 120.
	 *
	 * @throws Exception When a test failure occurs.
	 */
	@Test
	void configPathLineWidth120() throws Exception {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    format 'myrome', {",
				"        target '**/*.js'",
				"        rome('12.0.0').configPath('configs')",
				"    }",
				"}");
		setFile("rome_test.js").toResource("rome/js/longLineBefore.js");
		setFile("configs/rome.json").toResource("rome/config/line-width-120.json");

		var spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("rome_test.js").sameAsResource("rome/js/longLineAfter120.js");
	}

	/**
	 * Tests that a path to the directory with the rome.json config file can be
	 * specified. Uses a config file with a line width of 80.
	 *
	 * @throws Exception When a test failure occurs.
	 */
	@Test
	void configPathLineWidth80() throws Exception {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    format 'myrome', {",
				"        target '**/*.js'",
				"        rome('12.0.0').configPath('configs')",
				"    }",
				"}");
		setFile("rome_test.js").toResource("rome/js/longLineBefore.js");
		setFile("configs/rome.json").toResource("rome/config/line-width-80.json");

		var spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("rome_test.js").sameAsResource("rome/js/longLineAfter80.js");
	}

	/**
	 * Tests that the download directory can be an absolute path.
	 *
	 * @throws Exception When a test failure occurs.
	 */
	@Test
	void downloadDirAbsolute() throws Exception {
		var path = newFile("target/bin/rome").getAbsoluteFile().toString();
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    format 'myrome', {",
				"        target '**/*.js'",
				"        rome('12.0.0').downloadDir('" + Encode.forJava(path) + "')",
				"    }",
				"}");
		setFile("rome_test.js").toResource("rome/js/fileBefore.js");
		assertTrue(!newFile("target/bin/rome").exists() || newFile("target/bin/rome").list().length == 0);

		var spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("rome_test.js").sameAsResource("rome/js/fileAfter.js");
		assertEquals(2, newFile("target/bin/rome").list().length);
	}

	/**
	 * Tests that the download directory can be changed to a path relative to the
	 * project's base directory.
	 *
	 * @throws Exception When a test failure occurs.
	 */
	@Test
	void downloadDirRelative() throws Exception {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    format 'myrome', {",
				"        target '**/*.js'",
				"        rome('12.0.0').downloadDir('target/bin/rome')",
				"    }",
				"}");
		setFile("rome_test.js").toResource("rome/js/fileBefore.js");
		assertTrue(!newFile("target/bin/rome").exists() || newFile("target/bin/rome").list().length == 0);

		var spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("rome_test.js").sameAsResource("rome/js/fileAfter.js");
		assertEquals(2, newFile("target/bin/rome").list().length);
	}

	/**
	 * Tests that the build fails when given Biome executable does not exist.
	 *
	 * @throws Exception When a test failure occurs.
	 */
	@Test
	void failureWhenExeNotFound() throws Exception {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    format 'myrome', {",
				"        target '**/*.js'",
				"        rome('12.0.0').pathToExe('rome/is/missing')",
				"    }",
				"}");
		setFile("rome_test.js").toResource("rome/js/fileBefore.js");

		var spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").buildAndFail();
		assertThat(spotlessApply.getOutput()).contains("Build failed with an exception");
		assertFile("rome_test.js").sameAsResource("rome/js/fileBefore.js");
		assertThat(spotlessApply.getOutput()).contains("Could not create task ':spotlessMyromeApply'");
		assertThat(spotlessApply.getOutput()).contains("Biome executable does not exist");
	}

	/**
	 * Tests that the build fails when the input file could not be parsed.
	 *
	 * @throws Exception When a test failure occurs.
	 */
	@Test
	void failureWhenNotParseable() throws Exception {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    format 'myrome', {",
				"        target '**/*.js'",
				"        rome('12.0.0').language('json')",
				"    }",
				"}");
		setFile("rome_test.js").toResource("rome/js/fileBefore.js");

		var spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").buildAndFail();
		assertThat(spotlessApply.getOutput()).contains("spotlessMyrome FAILED");
		assertFile("rome_test.js").sameAsResource("rome/js/fileBefore.js");
		assertThat(spotlessApply.getOutput()).contains("Format with errors is disabled.");
		assertThat(spotlessApply.getOutput()).contains("Step 'rome' found problem in 'rome_test.js'");
	}
}
