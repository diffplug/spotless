/*
 * Copyright 2016-2021 DiffPlug
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
package com.diffplug.spotless.maven.generic;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

class ToggleOffOnTest extends MavenIntegrationHarness {
	@Test
	void toggleOffOn() throws Exception {
		writePomWithJavaSteps(
				"<indent>",
				"  <tabs>true</tabs>",
				"  <spacesPerTab>1</spacesPerTab>",
				"</indent>",
				"<toggleOffOn />");
		setFile("src/main/java/Main.java").toLines(
				" Here is some stuff",
				" No matter",
				"//spotless:off",
				" This won't get tabbed",
				"//spotless:on",
				" But this will get tabbed.");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile("src/main/java/Main.java").hasLines(
				"	Here is some stuff",
				"	No matter",
				"//spotless:off",
				" This won't get tabbed",
				"//spotless:on",
				"	But this will get tabbed.");
	}

	@Test
	void toggleOffOnCustom() throws Exception {
		writePomWithJavaSteps(
				"<indent>",
				"  <tabs>true</tabs>",
				"  <spacesPerTab>1</spacesPerTab>",
				"</indent>",
				"<toggleOffOn>",
				"  <off>//off</off>",
				"  <on>//on</on>",
				"</toggleOffOn>");
		setFile("src/main/java/Main.java").toLines(
				" Here is some stuff",
				" No matter",
				"//off",
				" This won't get tabbed",
				"//on",
				" But this will get tabbed.");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile("src/main/java/Main.java").hasLines(
				"	Here is some stuff",
				"	No matter",
				"//off",
				" This won't get tabbed",
				"//on",
				"	But this will get tabbed.");
	}
}
