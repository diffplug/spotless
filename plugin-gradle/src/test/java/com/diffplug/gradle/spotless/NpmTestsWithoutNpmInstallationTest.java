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

import org.assertj.core.api.Assertions;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class NpmTestsWithoutNpmInstallationTest extends GradleIntegrationHarness {

	@Test
	void useNodeAndNpmFromNodeGradlePlugin() throws Exception {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"    id 'com.github.node-gradle.node' version '3.5.1'",
				"}",
				"repositories { mavenCentral() }",
				"node {",
				"    download = true",
				"    version = '18.13.0'",
				"    npmVersion = '8.19.2'",
				"    workDir = file(\"${buildDir}/nodejs\")",
				"    npmWorkDir = file(\"${buildDir}/npm\")",
				"}",
				"def prettierConfig = [:]",
				"prettierConfig['printWidth'] = 50",
				"prettierConfig['parser'] = 'typescript'",
				"def npmExecExtension = System.getProperty('os.name').toLowerCase().contains('windows') ? '.cmd' : ''",
				"def nodeExecExtension = System.getProperty('os.name').toLowerCase().contains('windows') ? '.exe' : ''",
				"spotless {",
				"    format 'mytypescript', {",
				"        target 'test.ts'",
				"        prettier()",
				"            .npmExecutable(\"${tasks.named('npmSetup').get().npmDir.get()}/bin/npm${npmExecExtension}\")",
				"            .nodeExecutable(\"${tasks.named('nodeSetup').get().nodeDir.get()}/bin/node${nodeExecExtension}\")",
				"            .config(prettierConfig)",
				"    }",
				"}");
		setFile("test.ts").toResource("npm/prettier/config/typescript.dirty");
		// make sure node binary is there
		gradleRunner().withArguments("nodeSetup", "npmSetup").build();
		// then run spotless using that node installation
		final BuildResult spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		Assertions.assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("test.ts").sameAsResource("npm/prettier/config/typescript.configfile.clean");
	}

	@Test
	@Disabled("This test is disabled because we currently don't support using npm/node installed by the" +
			"node-gradle-plugin as the installation takes place in the *execution* phase, but spotless needs it in the *configuration* phase.")
	void useNodeAndNpmFromNodeGradlePluginInOneSweep() throws Exception {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"    id 'com.github.node-gradle.node' version '3.5.1'",
				"}",
				"repositories { mavenCentral() }",
				"node {",
				"    download = true",
				"    version = '18.13.0'",
				"    npmVersion = '8.19.2'",
				"    workDir = file(\"${buildDir}/nodejs\")",
				"    npmWorkDir = file(\"${buildDir}/npm\")",
				"}",
				"def prettierConfig = [:]",
				"prettierConfig['printWidth'] = 50",
				"prettierConfig['parser'] = 'typescript'",
				"def npmExecExtension = System.getProperty('os.name').toLowerCase().contains('windows') ? '.cmd' : ''",
				"def nodeExecExtension = System.getProperty('os.name').toLowerCase().contains('windows') ? '.exe' : ''",
				"spotless {",
				"    format 'mytypescript', {",
				"        target 'test.ts'",
				"        prettier()",
				"            .npmExecutable(\"${tasks.named('npmSetup').get().npmDir.get()}/bin/npm${npmExecExtension}\")",
				"            .nodeExecutable(\"${tasks.named('nodeSetup').get().nodeDir.get()}/bin/node${nodeExecExtension}\")",
				"            .config(prettierConfig)",
				"    }",
				"}",
				"tasks.named('spotlessMytypescript').configure {",
				"    it.dependsOn('nodeSetup', 'npmSetup')",
				"}");
		setFile("test.ts").toResource("npm/prettier/config/typescript.dirty");
		final BuildResult spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		Assertions.assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("test.ts").sameAsResource("npm/prettier/config/typescript.configfile.clean");
	}

	@Test
	void useNpmFromNodeGradlePlugin() throws Exception {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"    id 'com.github.node-gradle.node' version '3.5.1'",
				"}",
				"repositories { mavenCentral() }",
				"node {",
				"    download = true",
				"    version = '18.13.0'",
				"    workDir = file(\"${buildDir}/nodejs\")",
				"}",
				"def prettierConfig = [:]",
				"prettierConfig['printWidth'] = 50",
				"prettierConfig['parser'] = 'typescript'",
				"def npmExecExtension = System.getProperty('os.name').toLowerCase().contains('windows') ? '.cmd' : ''",
				"spotless {",
				"    format 'mytypescript', {",
				"        target 'test.ts'",
				"        prettier()",
				"            .npmExecutable(\"${tasks.named('npmSetup').get().npmDir.get()}/bin/npm${npmExecExtension}\")",
				"            .config(prettierConfig)",
				"    }",
				"}");
		setFile("test.ts").toResource("npm/prettier/config/typescript.dirty");
		// make sure node binary is there
		gradleRunner().withArguments("nodeSetup", "npmSetup").build();
		// then run spotless using that node installation
		final BuildResult spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		Assertions.assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("test.ts").sameAsResource("npm/prettier/config/typescript.configfile.clean");
	}

	@Test
	void useNpmNextToConfiguredNodePluginFromNodeGradlePlugin() throws Exception {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"    id 'com.github.node-gradle.node' version '3.5.1'",
				"}",
				"repositories { mavenCentral() }",
				"node {",
				"    download = true",
				"    version = '18.13.0'",
				"    workDir = file(\"${buildDir}/nodejs\")",
				"}",
				"def prettierConfig = [:]",
				"prettierConfig['printWidth'] = 50",
				"prettierConfig['parser'] = 'typescript'",
				"def nodeExecExtension = System.getProperty('os.name').toLowerCase().contains('windows') ? '.exe' : ''",
				"spotless {",
				"    format 'mytypescript', {",
				"        target 'test.ts'",
				"        prettier()",
				"            .nodeExecutable(\"${tasks.named('nodeSetup').get().nodeDir.get()}/bin/node${nodeExecExtension}\")",
				"            .config(prettierConfig)",
				"    }",
				"}");
		setFile("test.ts").toResource("npm/prettier/config/typescript.dirty");
		// make sure node binary is there
		gradleRunner().withArguments("nodeSetup", "npmSetup").build();
		// then run spotless using that node installation
		final BuildResult spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
		Assertions.assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
		assertFile("test.ts").sameAsResource("npm/prettier/config/typescript.configfile.clean");
	}
}
