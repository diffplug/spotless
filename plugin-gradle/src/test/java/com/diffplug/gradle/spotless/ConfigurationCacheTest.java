/*
 * Copyright 2020-2022 DiffPlug
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

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

public class ConfigurationCacheTest extends GradleIntegrationHarness {
	@Override
	protected GradleRunner gradleRunner() throws IOException {
		setFile("gradle.properties").toContent("org.gradle.unsafe.configuration-cache=true");
		setFile("settings.gradle").toContent("enableFeaturePreview(\"STABLE_CONFIGURATION_CACHE\")");
		return super.gradleRunner().withGradleVersion(GradleVersionSupport.STABLE_CONFIGURATION_CACHE.version);
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
				"        googleJavaFormat('1.2')",
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
				"        googleJavaFormat('1.2')",
				"    }",
				"}",
				"tasks.named('spotlessJavaApply').get()");
		gradleRunner().withArguments("help").build();
	}

	@Test
	public void jvmLocalCache() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    java {",
				"        target file('test.java')",
				"        googleJavaFormat('1.2')",
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

		BuildResult failure = gradleRunner().withDebug(true).withArguments("spotlessApply", "--stacktrace").buildAndFail();
		failure.getOutput().contains("Spotless daemon-local cache is stale. Regenerate the cache with\n" +
				"  rm -rf .gradle/configuration-cache\n" +
				"For more information see #123\n");
	}
}
