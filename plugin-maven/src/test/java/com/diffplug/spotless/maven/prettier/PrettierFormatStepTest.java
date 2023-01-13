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
package com.diffplug.spotless.maven.prettier;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;
import com.diffplug.spotless.maven.MavenRunner.Result;
import com.diffplug.spotless.maven.generic.Prettier;
import com.diffplug.spotless.tag.NpmTest;

@NpmTest
class PrettierFormatStepTest extends MavenIntegrationHarness {

	private void run(String kind, String suffix) throws IOException, InterruptedException {
		String path = prepareRun(kind, suffix);
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).sameAsResource("npm/prettier/filetypes/" + kind + "/" + kind + ".clean");
	}

	private String prepareRun(String kind, String suffix) throws IOException {
		String configPath = ".prettierrc.yml";
		setFile(configPath).toResource("npm/prettier/filetypes/" + kind + "/" + ".prettierrc.yml");
		String path = "src/main/" + kind + "/test." + suffix;
		setFile(path).toResource("npm/prettier/filetypes/" + kind + "/" + kind + ".dirty");
		return path;
	}

	private Result runExpectingError(String kind, String suffix) throws IOException, InterruptedException {
		String path = prepareRun(kind, suffix);
		return mavenRunner().withArguments("spotless:apply").runHasError();
	}

	@Test
	void prettier_typescript() throws Exception {
		String suffix = "ts";
		writePomWithPrettierSteps("**/*." + suffix,
				"<prettier>",
				"  <prettierVersion>1.16.4</prettierVersion>",
				"  <configFile>.prettierrc.yml</configFile>",
				"</prettier>");
		run("typescript", suffix);
	}

	@Test
	void prettier_html() throws Exception {
		String suffix = "html";
		writePomWithPrettierSteps("**/*." + suffix,
				"<prettier>",
				"  <prettierVersion>1.16.4</prettierVersion>",
				"  <configFile>.prettierrc.yml</configFile>",
				"</prettier>");
		run("html", suffix);
	}

	@Test
	void prettier_tsx() throws Exception {
		String suffix = "tsx";
		writePomWithPrettierSteps("src/main/**/*." + suffix,
				"<includes><include>src/**/*.tsx</include></includes>",
				"<prettier>",
				"  <prettierVersion>1.16.4</prettierVersion>",
				"  <configFile>.prettierrc.yml</configFile>",
				"</prettier>");
		run("tsx", suffix);
	}

	@Test
	void prettier_tsx_inline_config() throws Exception {
		String suffix = "tsx";
		writePomWithPrettierSteps("src/main/**/*." + suffix,
				"<prettier>",
				"  <prettierVersion>1.16.4</prettierVersion>",
				"  <config><parser>typescript</parser></config>",
				"</prettier>");
		run("tsx", suffix);
	}

	@Test
	void unique_dependency_config() throws Exception {
		writePomWithFormatSteps(
				"<includes><include>**/*.ts</include></includes>",
				"<prettier>",
				"  <prettierVersion>1.16.4</prettierVersion>",
				"  <devDependencies><prettier>1.16.4</prettier></devDependencies>",
				"</prettier>");

		Result result = mavenRunner().withArguments("spotless:apply").runHasError();
		assertThat(result.output()).contains(Prettier.ERROR_MESSAGE_ONLY_ONE_CONFIG);
	}

	@Test
	void custom_plugin() throws Exception {
		writePomWithFormatSteps(
				"<includes><include>php-example.php</include></includes>",
				"<prettier>",
				"  <devDependencyProperties>",
				"    <property>",
				"      <name>prettier</name>",
				"      <value>2.0.5</value>",
				"    </property>",
				"    <property>",
				"      <name>@prettier/plugin-php</name>",
				"      <value>0.14.2</value>",
				"    </property>",
				"  </devDependencyProperties>",
				"  <config>",
				"    <tabWidth>3</tabWidth>",
				"    <parser>php</parser>",
				"  </config>",
				"</prettier>");

		setFile("php-example.php").toResource("npm/prettier/plugins/php.dirty");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile("php-example.php").sameAsResource("npm/prettier/plugins/php.clean");
	}

	@Test
	void autodetect_parser_based_on_filename() throws Exception {
		writePomWithFormatSteps(
				"<includes><include>dirty.json</include></includes>",
				"<prettier/>");

		setFile("dirty.json").toResource("npm/prettier/filename/dirty.json");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile("dirty.json").sameAsResource("npm/prettier/filename/clean.json");
	}

	@Test
	void autodetect_npmrc_file() throws Exception {
		setFile(".npmrc").toLines(
				"registry=https://i.do.not.exist.com",
				"fetch-timeout=250",
				"fetch-retry-mintimeout=250",
				"fetch-retry-maxtimeout=250");
		String suffix = "ts";
		writePomWithPrettierSteps("**/*." + suffix,
				"<prettier>",
				"  <prettierVersion>1.16.4</prettierVersion>",
				"  <configFile>.prettierrc.yml</configFile>",
				"</prettier>");
		Result result = runExpectingError("typescript", suffix);
		assertThat(result.output()).containsPattern("Running npm command.*npm install.* failed with exit code: 1");
	}

	@Test
	void select_configured_npmrc_file() throws Exception {
		setFile(".custom_npmrc").toLines(
				"registry=https://i.do.not.exist.com",
				"fetch-timeout=250",
				"fetch-retry-mintimeout=250",
				"fetch-retry-maxtimeout=250");
		String suffix = "ts";
		writePomWithPrettierSteps("**/*." + suffix,
				"<prettier>",
				"  <prettierVersion>1.16.4</prettierVersion>",
				"  <configFile>.prettierrc.yml</configFile>",
				"  <npmrc>${basedir}/.custom_npmrc</npmrc>",
				"</prettier>");
		Result result = runExpectingError("typescript", suffix);
		assertThat(result.output()).containsPattern("Running npm command.*npm install.* failed with exit code: 1");
	}
}
