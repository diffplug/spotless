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

import java.io.IOException;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

abstract class ToggleOffOnTest extends GradleIntegrationHarness {
	private final boolean useConfigCache;

	ToggleOffOnTest(boolean useConfigCache) {
		this.useConfigCache = useConfigCache;
	}

	static class WithConfigCache extends ToggleOffOnTest {
		WithConfigCache() {
			super(true);
		}
	}

	static class WithoutConfigCache extends ToggleOffOnTest {
		WithoutConfigCache() {
			super(false);
		}
	}

	@Override
	public GradleRunner gradleRunner() throws IOException {
		if (useConfigCache) {
			setFile("gradle.properties").toLines("org.gradle.unsafe.configuration-cache=true",
					"org.gradle.configuration-cache=true");
			return super.gradleRunner().withGradleVersion(GradleVersionSupport.STABLE_CONFIGURATION_CACHE.version);
		} else {
			return super.gradleRunner();
		}
	}

	@Test
	void lowercase() throws IOException {
		setFile("build.gradle").toLines(
				"plugins { id 'com.diffplug.spotless' }",
				"spotless {",
				"  lineEndings 'UNIX'",
				"  format 'toLower', {",
				"    target '**/*.md'",
				"    addStep(com.diffplug.spotless.TestingOnly.lowercase())",
				"    toggleOffOn()",
				"  }",
				"}");
		setFile("test.md").toLines(
				"A B C",
				"spotless:off",
				"D E F",
				"spotless:on",
				"G H I");
		gradleRunner().withArguments("spotlessApply", "--stacktrace").build();
		assertFile("test.md").hasLines(
				"a b c",
				"spotless:off",
				"D E F",
				"spotless:on",
				"g h i");
		gradleRunner().withArguments("spotlessApply", "--stacktrace").build();
		gradleRunner().withArguments("spotlessApply", "--stacktrace").build();
		gradleRunner().withArguments("spotlessApply", "--stacktrace").build();
		setFile("test.md").toLines(
				"A B C",
				"spotless:off",
				"D E F",
				"spotless:on",
				"G H I");
		gradleRunner().withArguments("spotlessApply", "--stacktrace").build();
		assertFile("test.md").hasLines(
				"a b c",
				"spotless:off",
				"D E F",
				"spotless:on",
				"g h i");
	}

	@Test
	void gjf() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"",
				"spotless {",
				"    java {",
				"        target file('test.java')",
				"        googleJavaFormat('1.17.0')",
				"        toggleOffOn()",
				"    }",
				"}");

		setFile("test.java").toResource("java/googlejavaformat/JavaCodeUnformatted.test");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("test.java").sameAsResource("java/googlejavaformat/JavaCodeFormatted.test");
		gradleRunner().withArguments("spotlessCheck").build();
	}
}
