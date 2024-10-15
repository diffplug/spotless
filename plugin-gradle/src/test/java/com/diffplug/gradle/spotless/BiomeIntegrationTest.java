/*
 * Copyright 2023-2024 DiffPlug
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

import org.junit.jupiter.api.Test;
import org.owasp.encoder.Encode;

class BiomeIntegrationTest extends GradleIntegrationHarness {
	/**
	 * Tests that biome can be used as a JSON formatting step, using biome 1.8.3
	 * which
	 * requires opt-in.
	 *
	 * @throws Exception When a test failure occurs.
	 */
	@Test
	void asCssStepExperimental() throws Exception {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    css {",
				"        target '**/*.css'",
				"        biome('1.8.3').configPath('configs')",
				"    }",
				"}");
		setFile("biome_test.css").toResource("biome/css/fileBefore.css");
		setFile("configs/biome.json").toResource("biome/config/css-enabled.json");

		var spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("biome_test.css").sameAsResource("biome/css/fileAfter.css");
	}

	/**
	 * Tests that biome can be used as a JSON formatting step, using biome 1.9.0
	 * which
	 * does not require opt-in.
	 *
	 * @throws Exception When a test failure occurs.
	 */
	@Test
	void asCssStepStable() throws Exception {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    css {",
				"        target '**/*.css'",
				"        biome('1.9.0')",
				"    }",
				"}");
		setFile("biome_test.css").toResource("biome/css/fileBefore.css");

		var spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("biome_test.css").sameAsResource("biome/css/fileAfter.css");
	}

	/**
	 * Tests that biome can be used as a generic formatting step.
	 *
	 * @throws Exception When a test failure occurs.
	 */
	@Test
	void asGenericStep() throws Exception {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    format 'mybiome', {",
				"        target '**/*.js'",
				"        biome('1.2.0')",
				"    }",
				"}");
		setFile("biome_test.js").toResource("biome/js/fileBefore.js");

		var spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("biome_test.js").sameAsResource("biome/js/fileAfter.js");
	}

	/**
	 * Tests that biome can be used as a JavaScript formatting step.
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
				"        biome('1.2.0')",
				"    }",
				"}");
		setFile("biome_test.js").toResource("biome/js/fileBefore.js");

		var spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("biome_test.js").sameAsResource("biome/js/fileAfter.js");
	}

	/**
	 * Tests that biome can be used as a JSON formatting step.
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
				"        biome('1.2.0')",
				"    }",
				"}");
		setFile("biome_test.json").toResource("biome/json/fileBefore.json");

		var spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("biome_test.json").sameAsResource("biome/json/fileAfter.json");
	}

	/**
	 * Tests that biome can be used as a TypeScript formatting step.
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
				"        biome('1.2.0')",
				"    }",
				"}");
		setFile("biome_test.ts").toResource("biome/ts/fileBefore.ts");

		var spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("biome_test.ts").sameAsResource("biome/ts/fileAfter.ts");
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
				"    format 'mybiome', {",
				"        target '**/*.nosj'",
				"        biome('1.2.0').language('json')",
				"    }",
				"}");
		setFile("biome_test.nosj").toResource("biome/json/fileBefore.json");

		var spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("biome_test.nosj").sameAsResource("biome/json/fileAfter.json");
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
				"    format 'mybiome', {",
				"        target '**/*.js'",
				"        biome('1.2.0').configPath('" + Encode.forJava(path) + "')",
				"    }",
				"}");
		setFile("biome_test.js").toResource("biome/js/longLineBefore.js");
		setFile("configs/biome.json").toResource("biome/config/line-width-120.json");

		var spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("biome_test.js").sameAsResource("biome/js/longLineAfter120.js");
	}

	/**
	 * Tests that a path to the directory with the biome.json config file can be
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
				"    format 'mybiome', {",
				"        target '**/*.js'",
				"        biome('1.2.0').configPath('configs')",
				"    }",
				"}");
		setFile("biome_test.js").toResource("biome/js/longLineBefore.js");
		setFile("configs/biome.json").toResource("biome/config/line-width-120.json");

		var spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("biome_test.js").sameAsResource("biome/js/longLineAfter120.js");
	}

	/**
	 * Tests that a path to the directory with the biome.json config file can be
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
				"    format 'mybiome', {",
				"        target '**/*.js'",
				"        biome('1.2.0').configPath('configs')",
				"    }",
				"}");
		setFile("biome_test.js").toResource("biome/js/longLineBefore.js");
		setFile("configs/biome.json").toResource("biome/config/line-width-80.json");

		var spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("biome_test.js").sameAsResource("biome/js/longLineAfter80.js");
	}

	/**
	 * Tests that the download directory can be an absolute path.
	 *
	 * @throws Exception When a test failure occurs.
	 */
	@Test
	void downloadDirAbsolute() throws Exception {
		var path = newFile("target/bin/biome").getAbsoluteFile().toString();
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    format 'mybiome', {",
				"        target '**/*.js'",
				"        biome('1.2.0').downloadDir('" + Encode.forJava(path) + "')",
				"    }",
				"}");
		setFile("biome_test.js").toResource("biome/js/fileBefore.js");
		assertTrue(!newFile("target/bin/biome").exists() || newFile("target/bin/biome").list().length == 0);

		var spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("biome_test.js").sameAsResource("biome/js/fileAfter.js");
		assertEquals(2, newFile("target/bin/biome").list().length);
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
				"    format 'mybiome', {",
				"        target '**/*.js'",
				"        biome('1.2.0').downloadDir('target/bin/biome')",
				"    }",
				"}");
		setFile("biome_test.js").toResource("biome/js/fileBefore.js");
		assertTrue(!newFile("target/bin/biome").exists() || newFile("target/bin/biome").list().length == 0);

		var spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("biome_test.js").sameAsResource("biome/js/fileAfter.js");
		assertEquals(2, newFile("target/bin/biome").list().length);
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
				"    format 'mybiome', {",
				"        target '**/*.js'",
				"        biome('1.2.0').pathToExe('biome/is/missing')",
				"    }",
				"}");
		setFile("biome_test.js").toResource("biome/js/fileBefore.js");

		var spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").buildAndFail();
		assertThat(spotlessApply.getOutput()).contains("Build failed with an exception");
		assertFile("biome_test.js").sameAsResource("biome/js/fileBefore.js");
		assertThat(spotlessApply.getOutput()).contains("Execution failed for task ':spotlessMybiome'");
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
				"    format 'mybiome', {",
				"        target '**/*.js'",
				"        biome('1.2.0').language('json')",
				"    }",
				"}");
		setFile("biome_test.js").toResource("biome/js/fileBefore.js");

		var spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").buildAndFail();
		assertThat(spotlessApply.getOutput()).contains("spotlessMybiome FAILED");
		assertFile("biome_test.js").sameAsResource("biome/js/fileBefore.js");
		assertThat(spotlessApply.getOutput()).contains("Format with errors is disabled.");
		assertThat(spotlessApply.getOutput()).contains("Step 'biome' found problem in 'biome_test.js'");
	}

	/**
	 * Biome is hard-coded to ignore certain files, such as package.json. Since
	 * version 1.5.0,
	 * the biome CLI does not output any formatted code anymore, whereas previously
	 * it printed
	 * the input as-is. This tests checks that when the biome formatter outputs an
	 * empty string,
	 * the contents of the file to format are used instead.
	 *
	 * @throws Exception When a test failure occurs.
	 */
	@Test
	void preservesIgnoredFiles() throws Exception {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    json {",
				"        target '**/*.json'",
				"        biome('1.5.0')",
				"    }",
				"}");
		setFile("package.json").toResource("biome/json/packageBefore.json");

		var spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("package.json").sameAsResource("biome/json/packageAfter.json");
	}
}
