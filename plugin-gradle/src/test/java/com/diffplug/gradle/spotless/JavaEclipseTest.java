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

import java.io.IOException;

import org.junit.jupiter.api.Test;

class JavaEclipseTest extends GradleIntegrationHarness {
	@Test
	void settingsWithContentWithoutFile() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"  id 'com.diffplug.spotless'",
				"  id 'java'",
				"}",
				"repositories { mavenCentral() }",
				"",
				"spotless {",
				"  java {  eclipse().config(\"\"\"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>",
				"<profiles version=\"12\">",
				"  <profile kind=\"CodeFormatterProfile\" name=\"Spotless\" version=\"12\">",
				"    <setting id=\"org.eclipse.jdt.core.formatter.tabulation.size\" value=\"4\" />",
				"  </profile>",
				"</profiles>",
				"\"\"\")  }",
				"}");

		gradleRunner().withArguments("spotlessApply").build();
	}
}
