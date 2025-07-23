/*
 * Copyright 2021-2025 DiffPlug
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
package com.diffplug.spotless.maven.kotlin;

import java.io.File;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

class DiktatTest extends MavenIntegrationHarness {

	@Test
	void testDiktat() throws Exception {

		writePomWithKotlinSteps("<diktat/>");

		testPath = "src/main/kotlin/Main.kt";
		setFile(testPath).toResource("kotlin/diktat/main.dirty");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(testPath).sameAsResource("kotlin/diktat/main.clean");

	}

	@Test
	void testDiktatWithVersion() throws Exception {

		writePomWithKotlinSteps(
				"<diktat>",
				"  <version>1.2.1</version>",
				"</diktat>");

		testPath = "src/main/kotlin/Main.kt";
		setFile(testPath).toResource("kotlin/diktat/main.dirty");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(testPath).sameAsResource("kotlin/diktat/main.clean");
	}

	@Test
	void testDiktatConfig() throws Exception {

		String configPath = "src/main/kotlin/diktat-analysis.yml";
		File conf = setFile(configPath).toResource("kotlin/diktat/diktat-analysis.yml");
		writePomWithKotlinSteps(
				"<diktat>",
				"  <version>1.2.1</version>",
				"  <configFile>" + conf.getAbsolutePath() + "</configFile>",
				"</diktat>");

		testPath = "src/main/kotlin/Main.kt";
		setFile(testPath).toResource("kotlin/diktat/main.dirty");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(testPath).sameAsResource("kotlin/diktat/main.clean");
	}

}
