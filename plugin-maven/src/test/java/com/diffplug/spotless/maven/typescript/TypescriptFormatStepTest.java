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
package com.diffplug.spotless.maven.typescript;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.ProcessRunner;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.maven.MavenIntegrationHarness;
import com.diffplug.spotless.npm.EslintFormatterStep;
import com.diffplug.spotless.npm.EslintStyleGuide;
import com.diffplug.spotless.tag.NpmTest;

@NpmTest
class TypescriptFormatStepTest extends MavenIntegrationHarness {

	private static final String TEST_FILE_PATH = "src/main/typescript/test.ts";

	private static String styleGuideDevDependenciesString(String styleGuideName) {
		return EslintStyleGuide.fromNameOrNull(styleGuideName).asMavenXmlStringMergedWith(EslintFormatterStep.defaultDevDependencies());
	}

	private void runTsfmt(String kind) throws IOException, InterruptedException {
		var path = prepareRunTsfmt(kind);
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).sameAsResource("npm/tsfmt/" + kind + "/" + kind + ".clean");
	}

	private String prepareRunTsfmt(String kind) throws IOException {
		setFile(TEST_FILE_PATH).toResource("npm/tsfmt/" + kind + "/" + kind + ".dirty");
		return TEST_FILE_PATH;
	}

	private ProcessRunner.Result runExpectingErrorTsfmt(String kind) throws IOException, InterruptedException {
		prepareRunTsfmt(kind);
		return mavenRunner().withArguments("spotless:apply").runHasError();
	}

	@Test
	void tslint() throws Exception {
		writePomWithTypescriptSteps(
				TEST_FILE_PATH,
				"<tsfmt>",
				"  <tslintFile>${basedir}/tslint.json</tslintFile>",
				"</tsfmt>");
		setFile("tslint.json").toResource("npm/tsfmt/tslint/tslint.json");
		runTsfmt("tslint");
	}

	@Test
	void vscode() throws Exception {
		writePomWithTypescriptSteps(
				TEST_FILE_PATH,
				"<tsfmt>",
				"  <vscodeFile>${basedir}/vscode.json</vscodeFile>",
				"</tsfmt>");
		setFile("vscode.json").toResource("npm/tsfmt/vscode/vscode.json");
		runTsfmt("vscode");
	}

	@Test
	void tsfmt() throws Exception {
		writePomWithTypescriptSteps(
				TEST_FILE_PATH,
				"<tsfmt>",
				"  <tsfmtFile>${basedir}/tsfmt.json</tsfmtFile>",
				"</tsfmt>");
		setFile("tsfmt.json").toResource("npm/tsfmt/tsfmt/tsfmt.json");
		runTsfmt("tsfmt");
	}

	@Test
	void tsfmtInline() throws Exception {
		writePomWithTypescriptSteps(
				TEST_FILE_PATH,
				"<tsfmt>",
				"  <config>",
				"    <indentSize>1</indentSize>",
				"    <convertTabsToSpaces>true</convertTabsToSpaces>",
				"  </config>",
				"</tsfmt>");
		runTsfmt("tsfmt");
	}

	@Test
	void tsconfig() throws Exception {
		writePomWithTypescriptSteps(
				TEST_FILE_PATH,
				"<tsfmt>",
				"  <tsconfigFile>${project.basedir}/tsconfig.json</tsconfigFile>",
				"</tsfmt>");
		setFile("tsconfig.json").toResource("npm/tsfmt/tsconfig/tsconfig.json");
		runTsfmt("tsconfig");
	}

	@Test
	void testTypescript_2_Configs() throws Exception {
		var path = "src/main/typescript/test.ts";

		writePomWithTypescriptSteps(
				path,
				"<tsfmt>",
				"  <vscodeFile>${basedir}/tslint.json</vscodeFile>",
				"  <tsfmtFile>${basedir}/tslint.json</tsfmtFile>",
				"</tsfmt>");
		setFile("vscode.json").toResource("npm/tsfmt/vscode/vscode.json");
		setFile("tsfmt.json").toResource("npm/tsfmt/tsfmt/tsfmt.json");

		setFile(path).toResource("npm/tsfmt/tsfmt/tsfmt.dirty");
		ProcessRunner.Result result = mavenRunner().withArguments("spotless:apply").runHasError();
		assertThat(result.stdOutUtf8()).contains("must specify exactly one configFile or config");
	}

	@Test
	void testNpmrcIsAutoPickedUp() throws Exception {
		setFile(".npmrc").toLines(
				"registry=https://i.do.not.exist.com",
				"fetch-timeout=250",
				"fetch-retry-mintimeout=250",
				"fetch-retry-maxtimeout=250");
		writePomWithTypescriptSteps(
				TEST_FILE_PATH,
				"<tsfmt>",
				"  <tslintFile>${basedir}/tslint.json</tslintFile>",
				"</tsfmt>");
		setFile("tslint.json").toResource("npm/tsfmt/tslint/tslint.json");
		ProcessRunner.Result result = runExpectingErrorTsfmt("tslint");
		assertThat(result.stdOutUtf8()).containsPattern("Running npm command.*npm install.* failed with exit code: 1");
	}

	@Test
	void testNpmrcIsConfigurativelyPickedUp() throws Exception {
		setFile(".custom_npmrc").toLines(
				"registry=https://i.do.not.exist.com",
				"fetch-timeout=250",
				"fetch-retry-mintimeout=250",
				"fetch-retry-maxtimeout=250");
		writePomWithTypescriptSteps(
				TEST_FILE_PATH,
				"<tsfmt>",
				"  <tslintFile>${basedir}/tslint.json</tslintFile>",
				"  <npmrc>${basedir}/.custom_npmrc</npmrc>",
				"</tsfmt>");
		setFile("tslint.json").toResource("npm/tsfmt/tslint/tslint.json");
		ProcessRunner.Result result = runExpectingErrorTsfmt("tslint");
		assertThat(result.stdOutUtf8()).containsPattern("Running npm command.*npm install.* failed with exit code: 1");
	}

	@Test
	void eslintConfigFile() throws Exception {
		writePomWithTypescriptSteps(
				TEST_FILE_PATH,
				"<eslint>",
				"  <configFile>.eslintrc.js</configFile>",
				"</eslint>");
		setFile(".eslintrc.js").toResource("npm/eslint/typescript/custom_rules/.eslintrc.js");
		setFile(TEST_FILE_PATH).toResource("npm/eslint/typescript/custom_rules/typescript.dirty");

		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(TEST_FILE_PATH).sameAsResource("npm/eslint/typescript/custom_rules/typescript.clean");
	}

	@Test
	void eslintConfigJs() throws Exception {
		final String configJs = ResourceHarness.getTestResource("npm/eslint/typescript/custom_rules/.eslintrc.js")
				.replace("module.exports = ", "");
		writePomWithTypescriptSteps(
				TEST_FILE_PATH,
				"<eslint>",
				"  <configJs>" + configJs + "</configJs>",
				"</eslint>");
		setFile(TEST_FILE_PATH).toResource("npm/eslint/typescript/custom_rules/typescript.dirty");

		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(TEST_FILE_PATH).sameAsResource("npm/eslint/typescript/custom_rules/typescript.clean");
	}

	@Test
	void eslintStyleguideStandardWithTypescript() throws Exception {
		writePomWithTypescriptSteps(
				TEST_FILE_PATH,
				"<eslint>",
				"  <configFile>.eslintrc.js</configFile>",
				"  " + styleGuideDevDependenciesString("standard-with-typescript"),
				"  <tsconfigFile>${basedir}/tsconfig.json</tsconfigFile>",
				"</eslint>");
		setFile(".eslintrc.js").toResource("npm/eslint/typescript/styleguide/standard_with_typescript/.eslintrc.js");
		setFile("tsconfig.json").toResource("npm/eslint/typescript/styleguide/standard_with_typescript/tsconfig.json");
		setFile(TEST_FILE_PATH).toResource("npm/eslint/typescript/styleguide/standard_with_typescript/typescript.dirty");

		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(TEST_FILE_PATH).sameAsResource("npm/eslint/typescript/styleguide/standard_with_typescript/typescript.clean");
	}

	@Test
	void eslintStyleguideXo() throws Exception {
		writePomWithTypescriptSteps(
				TEST_FILE_PATH,
				"<eslint>",
				"  <configFile>.eslintrc.js</configFile>",
				"  " + styleGuideDevDependenciesString("xo-typescript"),
				"  <tsconfigFile>${basedir}/tsconfig.json</tsconfigFile>",
				"</eslint>");
		setFile(".eslintrc.js").toResource("npm/eslint/typescript/styleguide/xo/.eslintrc.js");
		setFile("tsconfig.json").toResource("npm/eslint/typescript/styleguide/xo/tsconfig.json");
		setFile(TEST_FILE_PATH).toResource("npm/eslint/typescript/styleguide/xo/typescript.dirty");

		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(TEST_FILE_PATH).sameAsResource("npm/eslint/typescript/styleguide/xo/typescript.clean");
	}
}
