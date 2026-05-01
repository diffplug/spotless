/*
 * Copyright 2020-2025 DiffPlug
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
import java.nio.file.Files;
import java.nio.file.Path;

import org.assertj.core.api.Assertions;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

public class ConfigurationCacheTest extends GradleIntegrationHarness {
	@Override
	public GradleRunner gradleRunner() throws IOException {
		setFile("gradle.properties").toContent("org.gradle.unsafe.configuration-cache=true");
		return super.gradleRunner();
	}

	@Test
	public void helpConfigures() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"apply plugin: 'java'",
				"spotless {",
				"    java {",
				"        googleJavaFormat()",
				"    }",
				"}");
		gradleRunner().withArguments("help").build();
	}

	@Test
	public void helpConfiguresIfTasksAreCreated() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"apply plugin: 'java'",
				"spotless {",
				"    java {",
				"        googleJavaFormat()",
				"    }",
				"}",
				"tasks.named('spotlessJavaApply').get()");
		gradleRunner().withArguments("help").build();
	}

	@Test
	public void configurationCacheNotInvalidatedByGitconfig() throws IOException {
		// ~/.gitconfig is read by JGit at class-load time via the SystemReader.
		// If GitRatchetGradle is loaded eagerly during configuration, Gradle's
		// configuration cache fingerprints ~/.gitconfig. When its content changes
		// (e.g. CI workers inject per-build auth tokens), the cache is invalidated.
		// This test verifies that changing ~/.gitconfig between runs does not
		// invalidate the configuration cache.
		Path gitconfig = Path.of(System.getProperty("user.home"), ".gitconfig");
		boolean existed = Files.exists(gitconfig);
		String originalContent = existed ? Files.readString(gitconfig) : null;
		try {
			Files.writeString(gitconfig, "[user]\n\tname = test\n");

			setFile("build.gradle").toLines(
					"plugins {",
					"    id 'com.diffplug.spotless'",
					"}",
					"repositories { mavenCentral() }",
					"apply plugin: 'java'",
					"spotless {",
					"    java {",
					"        googleJavaFormat()",
					"    }",
					"}");
			setFile("src/main/java/test.java").toResource("java/googlejavaformat/JavaCodeUnformatted.test");

			// first run stores the configuration cache
			gradleRunner().withArguments("help").build();

			// change ~/.gitconfig content between runs
			Files.writeString(gitconfig, "[user]\n\tname = test\n[http]\n\textraheader = changed\n");

			// second run must reuse the configuration cache despite the change
			String output = gradleRunner().withArguments("help").build().getOutput();
			Assertions.assertThat(output).contains("Reusing configuration cache.");
		} finally {
			// restore original ~/.gitconfig
			if (originalContent != null) {
				Files.writeString(gitconfig, originalContent);
			} else if (Files.exists(gitconfig)) {
				Files.delete(gitconfig);
			}
		}
	}

	@Test
	public void multipleRuns() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    java {",
				"        target file('test.java')",
				"        googleJavaFormat()",
				"    }",
				"}");

		// first run works
		setFile("test.java").toResource("java/googlejavaformat/JavaCodeUnformatted.test");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("test.java").sameAsResource("java/googlejavaformat/JavaCodeFormatted.test");

		// and it keeps working!
		gradleRunner().withArguments("spotlessApply", "--stacktrace").build();
		gradleRunner().withArguments("spotlessApply").build();
		gradleRunner().withArguments("spotlessApply").build();

		setFile("test.java").toResource("java/googlejavaformat/JavaCodeUnformatted.test");
		gradleRunner().withArguments("spotlessCheck").buildAndFail();
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("test.java").sameAsResource("java/googlejavaformat/JavaCodeFormatted.test");
	}
}
