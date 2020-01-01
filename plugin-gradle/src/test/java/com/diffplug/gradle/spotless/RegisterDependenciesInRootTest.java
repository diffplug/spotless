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
package com.diffplug.gradle.spotless;

import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class RegisterDependenciesInRootTest extends GradleIntegrationTest {
	@Test
	public void registerDependencies() throws IOException {
		setFile("settings.gradle")
				.toLines("include 'sub'");
		setFile("build.gradle").toLines(
				"buildscript { repositories { mavenCentral() } }",
				"plugins { id 'com.diffplug.gradle.spotless' }");
		setFile("sub/build.gradle").toLines(
				"apply plugin: 'com.diffplug.gradle.spotless'",
				"",
				"spotless {",
				"  java {",
				"    target 'src/main/java/**/*.java'",
				"    googleJavaFormat('1.2')",
				"  }",
				"}");

		String oldestSupported = gradleRunner()
				.withArguments("spotlessCheck").build().getOutput();
		Assertions.assertThat(oldestSupported.replace("\r", "")).startsWith(
				":spotlessCheck UP-TO-DATE\n" +
						":spotlessInternalRegisterDependencies\n" +
						":sub:spotlessJava\n" +
						":sub:spotlessJavaCheck\n" +
						":sub:spotlessCheck\n" +
						"\n" +
						"BUILD SUCCESSFUL");

		setFile("gradle.properties").toLines();
		String newestSupported = gradleRunner().withGradleVersion("6.0")
				.withArguments("spotlessCheck").build().getOutput();
		Assertions.assertThat(newestSupported.replace("\r", "")).startsWith(
				"> Task :spotlessCheck UP-TO-DATE\n" +
						"> Task :spotlessInternalRegisterDependencies\n" +
						"> Task :sub:spotlessJava\n" +
						"> Task :sub:spotlessJavaCheck\n" +
						"> Task :sub:spotlessCheck\n" +
						"\n" +
						"BUILD SUCCESSFUL");
	}
}
