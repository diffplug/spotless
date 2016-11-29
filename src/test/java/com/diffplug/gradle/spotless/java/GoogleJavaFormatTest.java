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
package com.diffplug.gradle.spotless.java;

import java.io.IOException;
import java.util.function.Function;

import org.assertj.core.api.Assertions;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Assert;
import org.junit.Test;

import com.diffplug.gradle.spotless.CheckFormatTask;
import com.diffplug.gradle.spotless.FormatterStep;
import com.diffplug.gradle.spotless.GradleIntegrationTest;

public class GoogleJavaFormatTest extends GradleIntegrationTest {
	@Test
	public void integration() throws IOException {
		write("build.gradle",
				"plugins {",
				"    id 'com.diffplug.gradle.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"",
				"spotless {",
				"    java {",
				"        target file('test.java')",
				"        googleJavaFormat('1.1')",
				"    }",
				"}");
		String input = getTestResource("java/googlejavaformat/JavaCodeUnformatted.test");
		write("test.java", input);
		gradleRunner().withArguments("spotlessApply").build();

		String result = read("test.java");
		String output = getTestResource("java/googlejavaformat/JavaCodeFormatted.test");
		Assert.assertEquals(output, result);

		checkRunsThenUpToDate();

		// if we change the version of google-java format, then check should need to run again
		replace("build.gradle",
				"googleJavaFormat('1.1')",
				"googleJavaFormat('1.0')");
		// but this fails, because we don't.  Now we gotta figure out why...
		checkRunsThenUpToDate();
	}

	/** This passes, and confirms that the steps we're creating have proper equality. */
	@Test
	public void testStepEquality() {
		Project project = ProjectBuilder.builder().withProjectDir(folder.getRoot()).build();
		project.getRepositories().mavenCentral();
		// copied from JavaExtension.googleJavaFormat
		Function<String, FormatterStep> create = version -> GoogleJavaFormat.createStep(version, project);

		Assertions.assertThat(create.apply("1.0"))
				.isEqualTo(create.apply("1.0"))
				.isNotEqualTo(create.apply("1.1"));
		Assertions.assertThat(create.apply("1.1"))
				.isEqualTo(create.apply("1.1"))
				.isNotEqualTo(create.apply("1.0"));
	}

	/** This passes, and confirms that the list of steps we're creating has proper equality. */
	@Test
	public void testTaskEquality() throws Exception {
		CheckFormatTask checks1_0a = createCheckTask(extension -> {
			extension.java(java -> {
				java.googleJavaFormat("1.0");
			});
		});
		CheckFormatTask checks1_0b = createCheckTask(extension -> {
			extension.java(java -> {
				java.googleJavaFormat("1.0");
			});
		});
		CheckFormatTask checks1_1 = createCheckTask(extension -> {
			extension.java(java -> {
				java.googleJavaFormat("1.1");
			});
		});
		Assertions.assertThat(checks1_0a.getSteps()).isEqualTo(checks1_0b.getSteps());
		Assertions.assertThat(checks1_0a.getSteps()).isNotEqualTo(checks1_1.getSteps());
	}
}
