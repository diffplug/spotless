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
package com.diffplug.spotless.maven.prettier;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.diffplug.spotless.category.NpmTest;
import com.diffplug.spotless.maven.MavenIntegrationHarness;
import com.diffplug.spotless.maven.MavenRunner;

@Category(NpmTest.class)
public class PrettierFormatStepTest extends MavenIntegrationHarness {

	private void run(String kind, String suffix) throws IOException, InterruptedException {
		String configPath = ".prettierrc.yml";
		setFile(configPath).toResource("npm/prettier/filetypes/" + kind + "/" + ".prettierrc.yml");
		String path = "src/main/" + kind + "/test." + suffix;
		setFile(path).toResource("npm/prettier/filetypes/" + kind + "/" + kind + ".dirty");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).sameAsResource("npm/prettier/filetypes/" + kind + "/" + kind + ".clean");
	}

	@Test
	public void prettier_typescript() throws Exception {
		String suffix = "ts";
		writePomWithPrettierSteps("**/*." + suffix,
				"<prettier>",
				"  <prettierVersion>1.16.4</prettierVersion>",
				"  <configFile>.prettierrc.yml</configFile>",
				"</prettier>");
		run("typescript", suffix);
	}

	@Test
	public void prettier_html() throws Exception {
		String suffix = "html";
		writePomWithPrettierSteps("**/*." + suffix,
				"<prettier>",
				"  <prettierVersion>1.16.4</prettierVersion>",
				"  <configFile>.prettierrc.yml</configFile>",
				"</prettier>");
		run("html", suffix);
	}

	@Test
	public void prettier_tsx() throws Exception {
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
	public void prettier_tsx_inline_config() throws Exception {
		String suffix = "tsx";
		writePomWithPrettierSteps("src/main/**/*." + suffix,
				"<prettier>",
				"  <prettierVersion>1.16.4</prettierVersion>",
				"  <config><parser>typescript</parser></config>",
				"</prettier>");
		run("tsx", suffix);
	}

	@Test
	public void unique_dependency_config() throws Exception {
		writePomWithFormatSteps(
				"<includes><include>**/*.ts</include></includes>",
				"<prettier>",
				"  <prettierVersion>1.16.4</prettierVersion>",
				"  <devDependencies><prettier>1.16.4</prettier></devDependencies>",
				"</prettier>");

		MavenRunner.Result result = mavenRunner().withArguments("spotless:apply").runHasError();
		assertThat(result.output()).contains("must specify exactly one configFile or config");
	}
}
