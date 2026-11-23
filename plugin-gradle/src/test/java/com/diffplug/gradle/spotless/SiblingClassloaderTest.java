/*
 * Copyright 2026 DiffPlug
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
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

/**
 * When the plugin is applied to sibling projects but not to a common parent, then
 * Gradle loads it in a separate buildscript classloader per project.
 * <a href="https://github.com/diffplug/spotless/issues/2877">#2877</a>
 */
class SiblingClassloaderTest extends GradleIntegrationHarness {
	private static final List<String> SUBPROJECTS = List.of("a", "b");

	/**
	 * TestKit's injected plugin classpath is shared by every project of the build,
	 * so we put the plugin on each subproject's own buildscript classpath instead.
	 */
	@Override
	public GradleRunner gradleRunner() throws IOException {
		return super.gradleRunner().withPluginClasspath(List.of());
	}

	private void createSiblingsWithOwnClassloader() throws IOException {
		String classpath = GradleRunner.create().withPluginClasspath().getPluginClasspath().stream()
				.map(File::getAbsolutePath)
				.map(path -> "'" + path.replace('\\', '/') + "'")
				.collect(Collectors.joining(", "));
		setFile("settings.gradle").toLines(SUBPROJECTS.stream().map(name -> "include '" + name + "'").toArray(String[]::new));
		setFile("build.gradle").toLines();
		for (String name : SUBPROJECTS) {
			// Gradle shares one classloader between identical buildscript classpaths, so give each
			// project an extra entry, as happens when projects apply different sets of plugins
			setFile(name + "/classpath-" + name + "/marker").toContent(name);
			setFile(name + "/build.gradle").toLines(
					"buildscript {",
					"    dependencies {",
					"        classpath files(" + classpath + ", 'classpath-" + name + "')",
					"    }",
					"}",
					"apply plugin: 'com.diffplug.spotless'",
					"spotless {",
					"    format 'misc', {",
					"        target file('test.txt')",
					"        trimTrailingWhitespace()",
					"    }",
					"}");
			setFile(name + "/test.txt").toContent("dirty   \n");
		}
	}

	@Test
	void givenSiblingClassloaders_whenApplyThenCheck_thenBothProjectsAreFormatted() throws IOException {
		// given
		createSiblingsWithOwnClassloader();

		// when
		gradleRunner().withArguments("spotlessApply").build();
		gradleRunner().withArguments("spotlessCheck").build();

		// then
		for (String name : SUBPROJECTS) {
			assertFile(name + "/test.txt").hasContent("dirty\n");
		}
	}

	@Test
	void givenSiblingClassloaders_whenCheckOnDirtyFiles_thenBothProjectsFail() throws IOException {
		// given
		createSiblingsWithOwnClassloader();

		// when
		BuildResult result = gradleRunner().withArguments("spotlessCheck", "--continue").buildAndFail();

		// then
		for (String name : SUBPROJECTS) {
			Assertions.assertThat(result.getOutput()).contains("Execution failed for task ':" + name + ":spotlessMiscCheck'");
		}
	}

	@Test
	void givenSiblingClassloadersAndConfigurationCache_whenRunTwice_thenCacheIsStoredAndReused() throws IOException {
		// given
		createSiblingsWithOwnClassloader();
		setFile("gradle.properties").toContent("org.gradle.configuration-cache=true");

		// when
		BuildResult store = gradleRunner().withArguments("spotlessApply").build();
		setFile("a/test.txt").toContent("dirty again   \n");
		BuildResult reuse = gradleRunner().withArguments("spotlessApply").build();

		// then
		Assertions.assertThat(store.getOutput()).contains("Configuration cache entry stored");
		Assertions.assertThat(reuse.getOutput()).contains("Configuration cache entry reused");
		assertFile("a/test.txt").hasContent("dirty again\n");
		assertFile("b/test.txt").hasContent("dirty\n");
	}
}
