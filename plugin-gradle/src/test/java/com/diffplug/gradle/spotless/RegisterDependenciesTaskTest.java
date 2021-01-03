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
package com.diffplug.gradle.spotless;

import java.io.File;
import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.assertj.core.util.Files;
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

	@Test
	public void reproduce_issue_747() throws IOException {
		setFile("settings.gradle").toLines(
				"plugins {",
				"	id 'com.diffplug.spotless' apply false",
				"}",
				"include 'sub'");
		setFile("build.gradle").toLines(
				"buildscript { repositories { mavenCentral() }}");
		setFile("sub/build.gradle").toLines(
				"apply plugin: 'com.diffplug.spotless'",
				"",
				"spotless {",
				"  scala {",
				"    scalafmt(\"2.0.1\")",
				"    target 'src/**'",
				"  }",
				"}");
		setFile("sub/src/a.scala").toResource("scala/scalafmt/basic.dirty");
		gradleRunner()
				.withGradleVersion("6.7.1")
				.withArguments("spotlessApply").build();
		assertFile("sub/src/a.scala").sameAsResource("scala/scalafmt/basic.clean_2.0.1");

		File build = new File(rootFolder(), "build");
		Files.delete(build);
		Assertions.assertThat(build.createNewFile()).isTrue();

		String output = gradleRunner()
				.withGradleVersion("6.7.1")
				.withArguments("spotlessApply")
				.buildAndFail()
				.getOutput();
		Assertions.assertThat(output).contains(
				"Cannot write to file '",
				"spotless-register-dependencies' specified for property 'unitOutput', as ancestor '",
				"build' is not a directory.");
	}
}
