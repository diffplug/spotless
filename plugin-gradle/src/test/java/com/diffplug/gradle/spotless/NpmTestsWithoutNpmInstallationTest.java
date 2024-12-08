/*
 * Copyright 2016-2024 DiffPlug
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

import com.diffplug.common.base.Predicates;

@Disabled("https://status.npmjs.org/ shows npm services down on 12/8/2024, should undisable this later")
class NpmTestsWithoutNpmInstallationTest extends GradleIntegrationHarness {

	@Test
	void useNodeAndNpmFromNodeGradlePlugin() throws Exception {
		try {
			setFile("build.gradle").toLines(
					"plugins {",
					"    id 'com.diffplug.spotless'",
					"    id 'com.github.node-gradle.node' version '3.5.1'",
					"}",
					"repositories { mavenCentral() }",
					"node {",
					"    download = true",
					"    version = '18.16.1'",
					"    npmVersion = '9.5.1'",
					"    workDir = file(\"${buildDir}/nodejs\")",
					"    npmWorkDir = file(\"${buildDir}/npm\")",
					"}",
					"def prettierConfig = [:]",
					"prettierConfig['printWidth'] = 20",
					"prettierConfig['parser'] = 'typescript'",
					"def npmExec = System.getProperty('os.name').toLowerCase().contains('windows') ? '/npm.cmd' : '/bin/npm'",
					"def nodeExec = System.getProperty('os.name').toLowerCase().contains('windows') ? '/node.exe' : '/bin/node'",
					"spotless {",
					"    format 'mytypescript', {",
					"        target 'test.ts'",
					"        prettier()",
					"            .npmExecutable(\"${tasks.named('npmSetup').get().npmDir.get()}${npmExec}\")",
					"            .nodeExecutable(\"${tasks.named('nodeSetup').get().nodeDir.get()}${nodeExec}\")",
					"            .config(prettierConfig)",
					"    }",
					"}");
			setFile("test.ts").toResource("npm/prettier/config/typescript.dirty");
			// make sure node binary is there
			gradleRunner().withArguments("nodeSetup", "npmSetup").build();
			// then run spotless using that node installation
			final BuildResult spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
			Assertions.assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
			assertFile("test.ts").sameAsResource("npm/prettier/config/typescript.configfile_prettier_2.clean");
		} catch (Exception e) {
			printContents();
			throw e;
		}
	}

	private void printContents() {
		System.out.println("************* Folder contents **************************");
		System.out.println(listFiles(Predicates.and(path -> !path.startsWith(".gradle"), path -> !path.contains("/node_modules/"), path -> !path.contains("/include/"))));
		System.out.println("********************************************************");
	}

	@Test
	void useNodeAndNpmFromNodeGradlePlugin_example1() throws Exception {
		try {
			setFile("build.gradle").toResource("com/diffplug/gradle/spotless/NpmTestsWithoutNpmInstallationTest_gradle_node_plugin_example_1.gradle");
			setFile("test.ts").toResource("npm/prettier/config/typescript.dirty");
			final BuildResult spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
			Assertions.assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
			assertFile("test.ts").sameAsResource("npm/prettier/config/typescript.configfile_prettier_2.clean");
		} catch (Exception e) {
			printContents();
			throw e;
		}
	}

	@Test
	void useNpmFromNodeGradlePlugin_example2() throws Exception {
		try {
			setFile("build.gradle").toResource("com/diffplug/gradle/spotless/NpmTestsWithoutNpmInstallationTest_gradle_node_plugin_example_2.gradle");
			setFile("test.ts").toResource("npm/prettier/config/typescript.dirty");
			final BuildResult spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
			Assertions.assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
			assertFile("test.ts").sameAsResource("npm/prettier/config/typescript.configfile_prettier_2.clean");
		} catch (Exception e) {
			printContents();
			throw e;
		}
	}

	@Test
	void useNpmFromNodeGradlePlugin() throws Exception {
		try {
			setFile("build.gradle").toLines(
					"plugins {",
					"    id 'com.diffplug.spotless'",
					"    id 'com.github.node-gradle.node' version '3.5.1'",
					"}",
					"repositories { mavenCentral() }",
					"node {",
					"    download = true",
					"    version = '18.16.1'",
					"    workDir = file(\"${buildDir}/nodejs\")",
					"}",
					"def prettierConfig = [:]",
					"prettierConfig['printWidth'] = 20",
					"prettierConfig['parser'] = 'typescript'",
					"def npmExec = System.getProperty('os.name').toLowerCase().contains('windows') ? '/npm.cmd' : '/bin/npm'",
					"spotless {",
					"    format 'mytypescript', {",
					"        target 'test.ts'",
					"        prettier()",
					"            .npmExecutable(\"${tasks.named('npmSetup').get().npmDir.get()}${npmExec}\")",
					"            .config(prettierConfig)",
					"    }",
					"}");
			setFile("test.ts").toResource("npm/prettier/config/typescript.dirty");
			// make sure node binary is there
			gradleRunner().withArguments("nodeSetup", "npmSetup").build();
			// then run spotless using that node installation
			final BuildResult spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
			Assertions.assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
			assertFile("test.ts").sameAsResource("npm/prettier/config/typescript.configfile_prettier_2.clean");
		} catch (Exception e) {
			printContents();
			throw e;
		}
	}

	@Test
	void useNpmNextToConfiguredNodePluginFromNodeGradlePlugin() throws Exception {
		try {
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
					"prettierConfig['printWidth'] = 20",
					"prettierConfig['parser'] = 'typescript'",
					"def nodeExec = System.getProperty('os.name').toLowerCase().contains('windows') ? '/node.exe' : '/bin/node'",
					"spotless {",
					"    format 'mytypescript', {",
					"        target 'test.ts'",
					"        prettier()",
					"            .nodeExecutable(\"${tasks.named('nodeSetup').get().nodeDir.get()}${nodeExec}\")",
					"            .config(prettierConfig)",
					"    }",
					"}");
			setFile("test.ts").toResource("npm/prettier/config/typescript.dirty");
			// make sure node binary is there
			gradleRunner().withArguments("nodeSetup", "npmSetup").build();
			// then run spotless using that node installation
			final BuildResult spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
			Assertions.assertThat(spotlessApply.getOutput()).contains("BUILD SUCCESSFUL");
			assertFile("test.ts").sameAsResource("npm/prettier/config/typescript.configfile_prettier_2.clean");
		} catch (Exception e) {
			printContents();
			throw e;
		}
	}
}
