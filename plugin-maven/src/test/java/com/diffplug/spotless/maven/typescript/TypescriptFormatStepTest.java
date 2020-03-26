/*
 * Copyright 2016 DiffPlug
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

import org.junit.Ignore;
import org.junit.Test;

import com.diffplug.spotless.maven.MavenIntegrationTest;
import com.diffplug.spotless.maven.MavenRunner;

public class TypescriptFormatStepTest extends MavenIntegrationTest {

	@Test
	public void testTypescriptTsfmtTslintFile() throws Exception {
		writePomWithTypescriptSteps(
				"<tsfmt>",
				"  <tslintFile>${basedir}/tslint.json</tslintFile>",
				"  <typescriptFormatterVersion>7.2.2</typescriptFormatterVersion>",
				"</tsfmt>");
		setFile("tslint.json").toResource("typescript/tsfmt/tslint.json");

		String path = "src/main/typescript/test.ts";
		setFile(path).toResource("typescript/tsfmt/TypescriptCodeUnformatted.test");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).sameAsResource("typescript/tsfmt/TypescriptCodeFormatted.test");
	}
	
	@Test
	@Ignore
	public void testTypescriptTsfmtTsConfigFile() throws Exception {
		writePomWithTypescriptSteps(
				"<tsfmt>",
				"  <buildDir>${basedir}/</buildDir>",
				"  <tsconfigFile>${basedir}/tsconfig.json</tsconfigFile>",
				"  <tsfmtFile>${basedir}/tsfmt.json</tsfmtFile>",
				"  <typescriptFormatterVersion>7.2.2</typescriptFormatterVersion>",
				"</tsfmt>");
		setFile("tsconfig.json").toResource("typescript/tsfmt/tsconfig.json");

		String path = "src/main/typescript/test.ts";
		setFile(path).toResource("typescript/tsfmt/TypescriptCodeUnformatted.test");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).sameAsResource("typescript/tsfmt/TypescriptCodeFormatted.test");
	}
	
	@Test
	public void testTypescriptTsfmtTsFmtFile() throws Exception {
		writePomWithTypescriptSteps(
				"<tsfmt>",
				"  <tsfmtFile>${basedir}/tsfmt.json</tsfmtFile>",
				"  <typescriptFormatterVersion>7.2.2</typescriptFormatterVersion>",
				"</tsfmt>");
		setFile("tsfmt.json").toResource("typescript/tsfmt/tsfmt.json");

		String path = "src/main/typescript/test.ts";
		setFile(path).toResource("typescript/tsfmt/TypescriptCodeUnformatted.test");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).sameAsResource("typescript/tsfmt/TypescriptCodeFormatted.test");
	}
	
	@Test
	public void testTypescriptTsfmtVsCodeFile() throws Exception {
		writePomWithTypescriptSteps(
				"<tsfmt>",
				"  <vscodeFile>${basedir}/vscode.json</vscodeFile>", 
				"  <typescriptFormatterVersion>7.2.2</typescriptFormatterVersion>",
				"</tsfmt>");
		setFile("vscode.json").toResource("typescript/tsfmt/vscode.json");

		String path = "src/main/typescript/test.ts";
		setFile(path).toResource("typescript/tsfmt/TypescriptCodeUnformatted.test");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).sameAsResource("typescript/tsfmt/TypescriptCodeFormatted.test");
	}
	
	@Test
	public void testTypescriptTsfmtInlineConfig() throws Exception {
		writePomWithTypescriptSteps(
				"<tsfmt>",
				"  <config>",
				"    <indentSize>1</indentSize>",
				"    <convertTabsToSpaces>true</indentSize>",
				"  </config>", 
				"  <typescriptFormatterVersion>7.2.2</typescriptFormatterVersion>",
				"</tsfmt>");
		setFile("vscode.json").toResource("typescript/tsfmt/vscode.json");

		String path = "src/main/typescript/test.ts";
		setFile(path).toResource("typescript/tsfmt/TypescriptCodeUnformatted.test");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).sameAsResource("typescript/tsfmt/TypescriptCodeFormatted.test");
	}
	
	@Test
	public void testTypescript_2_Configs() throws Exception {
		writePomWithTypescriptSteps(
				"<tsfmt>",
				"  <vscodeFile>${basedir}/tslint.json</vscodeFile>",
				"  <tsfmtFile>${basedir}/tslint.json</tsfmtFile>",
				"  <typescriptFormatterVersion>7.2.2</typescriptFormatterVersion>",
				"</tsfmt>");
		setFile("vscode.json").toResource("typescript/tsfmt/vscode.json");
		setFile("tsfmt.json").toResource("typescript/tsfmt/tsfmt.json");

		String path = "src/main/typescript/test.ts";
		setFile(path).toResource("typescript/tsfmt/TypescriptCodeUnformatted.test");
		MavenRunner.Result result = mavenRunner().withArguments("spotless:apply").runHasError();
		assertThat(result.output()).contains("must specify exactly one configFile or config");
	}
}
