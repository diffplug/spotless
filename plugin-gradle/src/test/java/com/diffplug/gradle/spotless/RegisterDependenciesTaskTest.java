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
package com.diffplug.gradle.spotless;

import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class RegisterDependenciesTaskTest extends GradleIntegrationHarness {
	@Test
	public void registerDependencies() throws IOException {
		setFile("settings.gradle")
				.toLines("include 'sub'");
		setFile("build.gradle").toLines(
				"buildscript { repositories { mavenCentral() } }",
				"plugins { id 'com.diffplug.spotless' }");
		setFile("sub/build.gradle").toLines(
				"apply plugin: 'com.diffplug.spotless'",
				"",
				"spotless {",
				"  java {",
				"    target 'src/main/java/**/*.java'",
				"    googleJavaFormat('1.2')",
				"  }",
				"}");

		setFile("gradle.properties").toLines();
		String newestSupported = gradleRunner().withArguments("spotlessCheck").build().getOutput();
		Assertions.assertThat(newestSupported.replace("\r", ""))
				.startsWith("> Task :spotlessCheck UP-TO-DATE\n" +
						"> Task :spotlessInternalRegisterDependencies\n")
				.contains("> Task :sub:spotlessJava\n" +
						"> Task :sub:spotlessJavaCheck\n" +
						"> Task :sub:spotlessCheck\n" +
						"\n" +
						"BUILD SUCCESSFUL");
	}
}
