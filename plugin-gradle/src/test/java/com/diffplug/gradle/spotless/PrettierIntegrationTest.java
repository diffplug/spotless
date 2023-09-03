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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.diffplug.spotless.npm.PrettierFormatterStep;
import com.diffplug.spotless.tag.NpmTest;

@NpmTest
class PrettierIntegrationTest extends GradleIntegrationHarness {

	private static final String PRETTIER_VERSION_2 = PrettierFormatterStep.DEFAULT_VERSION;

	private static final String PRETTIER_VERSION_3 = "3.0.3";

	@ParameterizedTest(name = "{index}: useInlineConfig with prettier {0}")
	@ValueSource(strings = {PRETTIER_VERSION_2, PRETTIER_VERSION_3})
	void useInlineConfig(String prettierVersion) throws IOException {
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
				"        prettier('" + prettierVersion + "').config(prettierConfig)",
				"    }",
				"}");
		setFile("test.ts").toResource("npm/prettier/config/typescript.dirty");
		final BuildResult spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		Assertions.assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		switch (prettierVersion) {
		case PRETTIER_VERSION_2:
			assertFile("test.ts").sameAsResource("npm/prettier/config/typescript.configfile_prettier_2.clean");
			break;
		case PRETTIER_VERSION_3:
			assertFile("test.ts").sameAsResource("npm/prettier/config/typescript.configfile_prettier_3.clean");
			break;
		}
	}

	@ParameterizedTest(name = "{index}: verifyCleanSpotlessCheckWorks with prettier {0}")
	@ValueSource(strings = {PRETTIER_VERSION_2, PRETTIER_VERSION_3})
	void verifyCleanSpotlessCheckWorks(String prettierVersion) throws IOException {
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
				"        prettier('" + prettierVersion + "').config(prettierConfig)",
				"    }",
				"}");
		setFile("test.ts").toResource("npm/prettier/config/typescript.dirty");
		final BuildResult spotlessCheckFailsGracefully = gradleRunner().withArguments("--stacktrace", "spotlessCheck").buildAndFail();
		Assertions.assertThat(spotlessCheckFailsGracefully.getOutput()).contains("> The following files had format violations:");

		gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		gradleRunner().withArguments("--stacktrace", "spotlessCheck").build();
	}

	@ParameterizedTest(name = "{index}: useFileConfig with prettier {0}")
	@ValueSource(strings = {PRETTIER_VERSION_2, PRETTIER_VERSION_3})
	void useFileConfig(String prettierVersion) throws IOException {
		setFile(".prettierrc.yml").toResource("npm/prettier/config/.prettierrc.yml");
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    format 'mytypescript', {",
				"        target 'test.ts'",
				"        prettier('" + prettierVersion + "').configFile('.prettierrc.yml')",
				"    }",
				"}");
		setFile("test.ts").toResource("npm/prettier/config/typescript.dirty");
		final BuildResult spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		Assertions.assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		switch (prettierVersion) {
		case PRETTIER_VERSION_2:
			assertFile("test.ts").sameAsResource("npm/prettier/config/typescript.configfile_prettier_2.clean");
			break;
		case PRETTIER_VERSION_3:
			assertFile("test.ts").sameAsResource("npm/prettier/config/typescript.configfile_prettier_3.clean");
			break;
		}
	}

	@ParameterizedTest(name = "{index}: chooseParserBasedOnFilename with prettier {0}")
	@ValueSource(strings = {PRETTIER_VERSION_2, PRETTIER_VERSION_3})
	void chooseParserBasedOnFilename(String prettierVersion) throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    format 'webResources', {",
				"        target 'dirty.*'",
				"        prettier('" + prettierVersion + "')",
				"    }",
				"}");
		setFile("dirty.json").toResource("npm/prettier/filename/dirty.json");
		final BuildResult spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		Assertions.assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("dirty.json").sameAsResource("npm/prettier/filename/clean.json");
	}

	@ParameterizedTest(name = "{index}: useJavaCommunityPlugin with prettier {0}")
	@ValueSource(strings = {PRETTIER_VERSION_2, PRETTIER_VERSION_3})
	void useJavaCommunityPlugin(String prettierVersion) throws IOException {
		var prettierPluginJava = "";
		var prettierConfigPluginsStr = "";
		switch (prettierVersion) {
		case PRETTIER_VERSION_2:
			prettierPluginJava = "2.1.0"; // last version to support v2
			break;
		case PRETTIER_VERSION_3:
			prettierPluginJava = "2.3.0"; // latest to support v3
			prettierConfigPluginsStr = "prettierConfig['plugins'] = ['prettier-plugin-java']";
			break;
		}
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"def prettierConfig = [:]",
				"prettierConfig['tabWidth'] = 4",
				"prettierConfig['parser'] = 'java'",
				prettierConfigPluginsStr,
				"def prettierPackages = [:]",
				"prettierPackages['prettier'] = '" + prettierVersion + "'",
				"prettierPackages['prettier-plugin-java'] = '" + prettierPluginJava + "'",
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

	@ParameterizedTest(name = "{index}: useJavaCommunityPluginFileConfig with prettier {0}")
	@ValueSource(strings = {PRETTIER_VERSION_2, PRETTIER_VERSION_3})
	void useJavaCommunityPluginFileConfig(String prettierVersion) throws IOException {
		var prettierPluginJava = "";
		switch (prettierVersion) {
		case PRETTIER_VERSION_2:
			prettierPluginJava = "2.1.0"; // last version to support v2
			break;
		case PRETTIER_VERSION_3:
			prettierPluginJava = "2.3.0"; // latest to support v3
			break;
		}
		setFile(".prettierrc.yml").toResource("npm/prettier/config/.prettierrc_java_plugin.yml");
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"def prettierPackages = [:]",
				"prettierPackages['prettier'] = '" + prettierVersion + "'",
				"prettierPackages['prettier-plugin-java'] = '" + prettierPluginJava + "'",
				"spotless {",
				"    format 'java', {",
				"        target 'JavaTest.java'",
				"        prettier(prettierPackages).configFile('.prettierrc.yml')",
				"    }",
				"}");
		setFile("JavaTest.java").toResource("npm/prettier/plugins/java-test.dirty");
		final BuildResult spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		Assertions.assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("JavaTest.java").sameAsResource("npm/prettier/plugins/java-test.clean");
	}

	@ParameterizedTest(name = "{index}: suggestsMissingJavaCommunityPlugin with prettier {0}")
	@ValueSource(strings = {PRETTIER_VERSION_2, PRETTIER_VERSION_3})
	void suggestsMissingJavaCommunityPlugin(String prettierVersion) throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"def prettierConfig = [:]",
				"prettierConfig['tabWidth'] = 4",
				"def prettierPackages = [:]",
				"prettierPackages['prettier'] = '" + prettierVersion + "'",
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

	@ParameterizedTest(name = "{index}: usePhpCommunityPlugin with prettier {0}")
	@ValueSource(strings = {PRETTIER_VERSION_2, PRETTIER_VERSION_3})
	void usePhpCommunityPlugin(String prettierVersion) throws IOException {
		var prettierPluginPhp = "";
		var prettierConfigPluginsStr = "";
		switch (prettierVersion) {
		case PRETTIER_VERSION_2:
			prettierPluginPhp = "0.19.7"; // last version to support v2
			break;
		case PRETTIER_VERSION_3:
			prettierPluginPhp = "0.20.1"; // latest to support v3
			prettierConfigPluginsStr = "prettierConfig['plugins'] = ['@prettier/plugin-php']";
			break;
		}
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"def prettierConfig = [:]",
				"prettierConfig['tabWidth'] = 3",
				"prettierConfig['parser'] = 'php'",
				prettierConfigPluginsStr,
				"def prettierPackages = [:]",
				"prettierPackages['prettier'] = '" + prettierVersion + "'",
				"prettierPackages['@prettier/plugin-php'] = '" + prettierPluginPhp + "'",
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
	@ParameterizedTest(name = "{index}: usePhpAndJavaCommunityPlugin with prettier {0}")
	@ValueSource(strings = {PRETTIER_VERSION_2, PRETTIER_VERSION_3})
	void usePhpAndJavaCommunityPlugin(String prettierVersion) throws IOException {
		var prettierPluginJava = "";
		var prettierPluginPhp = "";
		var prettierConfigPluginsJavaStr = "";
		var prettierConfigPluginsPhpStr = "";
		switch (prettierVersion) {
		case PRETTIER_VERSION_2:
			prettierPluginJava = "2.1.0"; // last version to support v2
			prettierPluginPhp = "0.19.7"; // last version to support v2
			break;
		case PRETTIER_VERSION_3:
			prettierPluginJava = "2.3.0"; // latest to support v3
			prettierPluginPhp = "0.20.1"; // latest to support v3
			prettierConfigPluginsJavaStr = "prettierConfigJava['plugins'] = ['prettier-plugin-java']";
			prettierConfigPluginsPhpStr = "prettierConfigPhp['plugins'] = ['@prettier/plugin-php']";
			break;
		}
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"def prettierConfigPhp = [:]",
				"prettierConfigPhp['tabWidth'] = 3",
				"prettierConfigPhp['parser'] = 'php'",
				prettierConfigPluginsPhpStr,
				"def prettierPackagesPhp = [:]",
				"prettierPackagesPhp['prettier'] = '" + prettierVersion + "'",
				"prettierPackagesPhp['@prettier/plugin-php'] = '" + prettierPluginPhp + "'",
				"def prettierConfigJava = [:]",
				"prettierConfigJava['tabWidth'] = 4",
				"prettierConfigJava['parser'] = 'java'",
				prettierConfigPluginsJavaStr,
				"def prettierPackagesJava = [:]",
				"prettierPackagesJava['prettier'] = '" + prettierVersion + "'",
				"prettierPackagesJava['prettier-plugin-java'] = '" + prettierPluginJava + "'",
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

	@ParameterizedTest(name = "{index}: autodetectNpmrcFileConfig with prettier {0}")
	@ValueSource(strings = {PRETTIER_VERSION_2, PRETTIER_VERSION_3})
	void autodetectNpmrcFileConfig(String prettierVersion) throws IOException {
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
				"        prettier('" + prettierVersion + "').config(prettierConfig)",
				"    }",
				"}");
		setFile("test.ts").toResource("npm/prettier/config/typescript.dirty");
		final BuildResult spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").buildAndFail();
		Assertions.assertThat(spotlessApply.getOutput()).containsPattern("Running npm command.*npm install.* failed with exit code: 1");
	}

	@ParameterizedTest(name = "{index}: autodetectNpmrcFileConfig with prettier {0}")
	@ValueSource(strings = {PRETTIER_VERSION_2, PRETTIER_VERSION_3})
	void pickupNpmrcFileConfig(String prettierVersion) throws IOException {
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
				"        prettier('" + prettierVersion + "').npmrc('.custom_npmrc').config(prettierConfig)",
				"    }",
				"}");
		setFile("test.ts").toResource("npm/prettier/config/typescript.dirty");
		final BuildResult spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").buildAndFail();
		Assertions.assertThat(spotlessApply.getOutput()).containsPattern("Running npm command.*npm install.* failed with exit code: 1");
	}
}
