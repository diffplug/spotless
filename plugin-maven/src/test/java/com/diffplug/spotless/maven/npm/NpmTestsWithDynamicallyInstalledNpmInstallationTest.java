/*
 * Copyright 2023-2025 DiffPlug
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
package com.diffplug.spotless.maven.npm;

import static com.diffplug.spotless.maven.npm.NpmFrontendMavenPlugin.installNpmMavenGoal;
import static com.diffplug.spotless.maven.npm.NpmFrontendMavenPlugin.installedNpmPath;
import static com.diffplug.spotless.maven.npm.NpmFrontendMavenPlugin.pomPluginLines;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

public class NpmTestsWithDynamicallyInstalledNpmInstallationTest extends MavenIntegrationHarness {

	public static final String TS = "typescript";

	@Test
	void useDownloadedNpmInstallation() throws Exception {
		writePomWithPrettierSteps(
				pomPluginLines("v18.13.0", null),
				"src/main/typescript/test.ts",
				"<prettier>",
				"    <npmExecutable>" + installedNpmPath() + "</npmExecutable>",
				"</prettier>");

		String path = "src/main/" + TS + "/test.ts";
		setFile(path).toResource("npm/prettier/filetypes/" + TS + "/" + TS + ".dirty");
		setFile(".prettierrc.yml").toResource("npm/prettier/filetypes/" + TS + "/" + ".prettierrc.yml");
		mavenRunner().withArguments(installNpmMavenGoal(), "spotless:apply").runNoError();
		assertFile(path).sameAsResource("npm/prettier/filetypes/" + TS + "/" + TS + ".clean");
	}

}
