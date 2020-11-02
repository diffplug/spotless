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
package com.diffplug.spotless.maven.typescript;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.diffplug.spotless.category.NpmTest;
import com.diffplug.spotless.maven.MavenIntegrationHarness;
import com.diffplug.spotless.maven.MavenRunner.Result;

@Category(NpmTest.class)
public class TypescriptFormatStepTest extends MavenIntegrationHarness {
	private void run(String kind) throws IOException, InterruptedException {
		String path = prepareRun(kind);
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).sameAsResource("npm/tsfmt/" + kind + "/" + kind + ".clean");
	}

	private String prepareRun(String kind) throws IOException {
		String path = "src/main/typescript/test.ts";
		setFile(path).toResource("npm/tsfmt/" + kind + "/" + kind + ".dirty");
		return path;
	}

	private Result runExpectingError(String kind) throws IOException, InterruptedException {
		prepareRun(kind);
		return mavenRunner().withArguments("spotless:apply").runHasError();
	}

	@Test
	public void tslint() throws Exception {
		writePomWithTypescriptSteps(
				"<tsfmt>",
				"  <tslintFile>${basedir}/tslint.json</tslintFile>",
				"</tsfmt>");
		setFile("tslint.json").toResource("npm/tsfmt/tslint/tslint.json");
		run("tslint");
	}

	@Test
	public void vscode() throws Exception {
		writePomWithTypescriptSteps(
				"<tsfmt>",
				"  <vscodeFile>${basedir}/vscode.json</vscodeFile>",
				"</tsfmt>");
		setFile("vscode.json").toResource("npm/tsfmt/vscode/vscode.json");
		run("vscode");
	}

	@Test
	public void tsfmt() throws Exception {
		writePomWithTypescriptSteps(
				"<tsfmt>",
				"  <tsfmtFile>${basedir}/tsfmt.json</tsfmtFile>",
				"</tsfmt>");
		setFile("tsfmt.json").toResource("npm/tsfmt/tsfmt/tsfmt.json");
		run("tsfmt");
	}

	@Test
	public void tsfmtInline() throws Exception {
		writePomWithTypescriptSteps(
				"<tsfmt>",
				"  <config>",
				"    <indentSize>1</indentSize>",
				"    <convertTabsToSpaces>true</convertTabsToSpaces>",
				"  </config>",
				"</tsfmt>");
		run("tsfmt");
	}

	@Test
	public void tsconfig() throws Exception {
		writePomWithTypescriptSteps(
				"<tsfmt>",
				"  <tsconfigFile>${project.basedir}/tsconfig.json</tsconfigFile>",
				"</tsfmt>");
		setFile("tsconfig.json").toResource("npm/tsfmt/tsconfig/tsconfig.json");
		run("tsconfig");
	}

	@Test
	public void testTypescript_2_Configs() throws Exception {
		writePomWithTypescriptSteps(
				"<tsfmt>",
				"  <vscodeFile>${basedir}/tslint.json</vscodeFile>",
				"  <tsfmtFile>${basedir}/tslint.json</tsfmtFile>",
				"</tsfmt>");
		setFile("vscode.json").toResource("npm/tsfmt/vscode/vscode.json");
		setFile("tsfmt.json").toResource("npm/tsfmt/tsfmt/tsfmt.json");

		String path = "src/main/typescript/test.ts";
		setFile(path).toResource("npm/tsfmt/tsfmt/tsfmt.dirty");
		Result result = mavenRunner().withArguments("spotless:apply").runHasError();
		assertThat(result.output()).contains("must specify exactly one configFile or config");
	}

	@Test
	public void testNpmrcIsAutoPickedUp() throws Exception {
		setFile(".npmrc").toLines("registry=https://i.do.no.exist.com");
		writePomWithTypescriptSteps(
				"<tsfmt>",
				"  <tslintFile>${basedir}/tslint.json</tslintFile>",
				"</tsfmt>");
		setFile("tslint.json").toResource("npm/tsfmt/tslint/tslint.json");
		Result result = runExpectingError("tslint");
		assertThat(result.output()).containsPattern("Running npm command.*npm install.* failed with exit code: 1");
	}

	@Test
	public void testNpmrcIsConfigurativelyPickedUp() throws Exception {
		setFile(".custom_npmrc").toLines("registry=https://i.do.no.exist.com");
		writePomWithTypescriptSteps(
				"<tsfmt>",
				"  <tslintFile>${basedir}/tslint.json</tslintFile>",
				"  <npmrc>${basedir}/.custom_npmrc</npmrc>",
				"</tsfmt>");
		setFile("tslint.json").toResource("npm/tsfmt/tslint/tslint.json");
		Result result = runExpectingError("tslint");
		assertThat(result.output()).containsPattern("Running npm command.*npm install.* failed with exit code: 1");
	}
}
