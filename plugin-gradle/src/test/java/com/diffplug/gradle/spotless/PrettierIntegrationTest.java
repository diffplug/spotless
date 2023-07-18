/*
 * Copyright 2016-2023 DiffPlug
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

import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import com.diffplug.spotless.tag.NpmTest;

@NpmTest
class PrettierIntegrationTest extends GradleIntegrationHarness {
	@Test
	void useInlineConfig() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"def prettierConfig = [:]",
				"prettierConfig['printWidth'] = 20",
				"prettierConfig['parser'] = 'typescript'",
				"spotless {",
				"    format 'mytypescript', {",
				"        target 'test.ts'",
				"        prettier().config(prettierConfig)",
				"    }",
				"}");
		setFile("test.ts").toResource("npm/prettier/config/typescript.dirty");
		final BuildResult spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		Assertions.assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("test.ts").sameAsResource("npm/prettier/config/typescript.configfile_prettier_2.clean");
	}

	@Test
	void verifyCleanSpotlessCheckWorks() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"def prettierConfig = [:]",
				"prettierConfig['printWidth'] = 50",
				"prettierConfig['parser'] = 'typescript'",
				"spotless {",
				"    format 'mytypescript', {",
				"        target 'test.ts'",
				"        prettier().config(prettierConfig)",
				"    }",
				"}");
		setFile("test.ts").toResource("npm/prettier/config/typescript.dirty");
		BuildResult spotlessCheckFailsGracefully = gradleRunner().withArguments("--stacktrace", "spotlessCheck").buildAndFail();
		Assertions.assertThat(spotlessCheckFailsGracefully.getOutput()).contains("> The following files had format violations:");

		gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		gradleRunner().withArguments("--stacktrace", "spotlessCheck").build();
	}

	@Test
	void useFileConfig() throws IOException {
		setFile(".prettierrc.yml").toResource("npm/prettier/config/.prettierrc.yml");
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    format 'mytypescript', {",
				"        target 'test.ts'",
				"        prettier().configFile('.prettierrc.yml')",
				"    }",
				"}");
		setFile("test.ts").toResource("npm/prettier/config/typescript.dirty");
		final BuildResult spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		Assertions.assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("test.ts").sameAsResource("npm/prettier/config/typescript.configfile_prettier_2.clean");
	}

	@Test
	void chooseParserBasedOnFilename() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    format 'webResources', {",
				"        target 'dirty.*'",
				"        prettier()",
				"    }",
				"}");
		setFile("dirty.json").toResource("npm/prettier/filename/dirty.json");
		final BuildResult spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		Assertions.assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("dirty.json").sameAsResource("npm/prettier/filename/clean.json");
	}

	@Test
	void useJavaCommunityPlugin() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"def prettierConfig = [:]",
				"prettierConfig['tabWidth'] = 4",
				"prettierConfig['parser'] = 'java'",
				"def prettierPackages = [:]",
				"prettierPackages['prettier'] = '2.8.8'",
				"prettierPackages['prettier-plugin-java'] = '2.2.0'",
				"spotless {",
				"    format 'java', {",
				"        target 'JavaTest.java'",
				"        prettier(prettierPackages).config(prettierConfig)",
				"    }",
				"}");
		setFile("JavaTest.java").toResource("npm/prettier/plugins/java-test.dirty");
		final BuildResult spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		Assertions.assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("JavaTest.java").sameAsResource("npm/prettier/plugins/java-test.clean");
	}

	@Test
	void suggestsMissingJavaCommunityPlugin() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"def prettierConfig = [:]",
				"prettierConfig['tabWidth'] = 4",
				"def prettierPackages = [:]",
				"prettierPackages['prettier'] = '2.8.8'",
				"spotless {",
				"    format 'java', {",
				"        target 'JavaTest.java'",
				"        prettier(prettierPackages).config(prettierConfig)",
				"    }",
				"}");
		setFile("JavaTest.java").toResource("npm/prettier/plugins/java-test.dirty");
		final BuildResult spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").buildAndFail();
		Assertions.assertThat(spotlessApply.getOutput()).contains("could not infer a parser");
		Assertions.assertThat(spotlessApply.getOutput()).contains("prettier-plugin-java");
	}

	@Test
	void usePhpCommunityPlugin() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"def prettierConfig = [:]",
				"prettierConfig['tabWidth'] = 3",
				"prettierConfig['parser'] = 'php'",
				"def prettierPackages = [:]",
				"prettierPackages['prettier'] = '2.8.8'",
				"prettierPackages['@prettier/plugin-php'] = '0.19.6'",
				"spotless {",
				"    format 'php', {",
				"        target 'php-example.php'",
				"        prettier(prettierPackages).config(prettierConfig)",
				"    }",
				"}");
		setFile("php-example.php").toResource("npm/prettier/plugins/php.dirty");
		final BuildResult spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		Assertions.assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("php-example.php").sameAsResource("npm/prettier/plugins/php.clean");
	}

	/**
	 * This test is to ensure that we can have multiple prettier instances in one spotless config.
	 *
	 * @see <a href="https://github.com/diffplug/spotless/issues/1162">Issue #1162 on github</a>
	 */
	@Test
	void usePhpAndJavaCommunityPlugin() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"def prettierConfigPhp = [:]",
				"prettierConfigPhp['tabWidth'] = 3",
				"prettierConfigPhp['parser'] = 'php'",
				"def prettierPackagesPhp = [:]",
				"prettierPackagesPhp['prettier'] = '2.8.8'",
				"prettierPackagesPhp['@prettier/plugin-php'] = '0.19.6'",
				"def prettierConfigJava = [:]",
				"prettierConfigJava['tabWidth'] = 4",
				"prettierConfigJava['parser'] = 'java'",
				"def prettierPackagesJava = [:]",
				"prettierPackagesJava['prettier'] = '2.8.8'",
				"prettierPackagesJava['prettier-plugin-java'] = '2.2.0'",
				"spotless {",
				"    format 'php', {",
				"        target 'php-example.php'",
				"        prettier(prettierPackagesPhp).config(prettierConfigPhp)",
				"    }",
				"    java {",
				"        target 'JavaTest.java'",
				"        prettier(prettierPackagesJava).config(prettierConfigJava)",
				"    }",
				"}");
		setFile("php-example.php").toResource("npm/prettier/plugins/php.dirty");
		setFile("JavaTest.java").toResource("npm/prettier/plugins/java-test.dirty");
		final BuildResult spotlessApply = gradleRunner().forwardOutput().withArguments("--stacktrace", "--info", "spotlessApply").build();
		Assertions.assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		final BuildResult spotlessApply2 = gradleRunner().forwardOutput().withArguments("--stacktrace", "--info", "spotlessApply").build();
		Assertions.assertThat(spotlessApply2.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("php-example.php").sameAsResource("npm/prettier/plugins/php.clean");
		assertFile("JavaTest.java").sameAsResource("npm/prettier/plugins/java-test.clean");
	}

	@Test
	void autodetectNpmrcFileConfig() throws IOException {
		setFile(".npmrc").toLines(
				"registry=https://i.do.not.exist.com",
				"fetch-timeout=250",
				"fetch-retry-mintimeout=250",
				"fetch-retry-maxtimeout=250");
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"def prettierConfig = [:]",
				"prettierConfig['printWidth'] = 50",
				"prettierConfig['parser'] = 'typescript'",
				"spotless {",
				"    format 'mytypescript', {",
				"        target 'test.ts'",
				"        prettier().config(prettierConfig)",
				"    }",
				"}");
		setFile("test.ts").toResource("npm/prettier/config/typescript.dirty");
		final BuildResult spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").buildAndFail();
		Assertions.assertThat(spotlessApply.getOutput()).containsPattern("Running npm command.*npm install.* failed with exit code: 1");
	}

	@Test
	void pickupNpmrcFileConfig() throws IOException {
		setFile(".custom_npmrc").toLines(
				"registry=https://i.do.not.exist.com",
				"fetch-timeout=250",
				"fetch-retry-mintimeout=250",
				"fetch-retry-maxtimeout=250");
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"def prettierConfig = [:]",
				"prettierConfig['printWidth'] = 50",
				"prettierConfig['parser'] = 'typescript'",
				"spotless {",
				"    format 'mytypescript', {",
				"        target 'test.ts'",
				"        prettier().npmrc('.custom_npmrc').config(prettierConfig)",
				"    }",
				"}");
		setFile("test.ts").toResource("npm/prettier/config/typescript.dirty");
		final BuildResult spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").buildAndFail();
		Assertions.assertThat(spotlessApply.getOutput()).containsPattern("Running npm command.*npm install.* failed with exit code: 1");
	}
}
