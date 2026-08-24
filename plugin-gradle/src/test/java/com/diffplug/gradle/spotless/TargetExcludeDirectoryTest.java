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

import java.io.IOException;

import org.junit.jupiter.api.Test;

class TargetExcludeDirectoryTest extends GradleIntegrationHarness {
	@Test
	void targetExcludeWithDirectory() throws IOException {
		setFile("build.gradle").toLines(
				"plugins { id 'com.diffplug.spotless' }",
				"spotless {",
				"  format 'toLower', {",
				"    target '**/*.md'",
				"    targetExclude layout.projectDirectory.dir('generated')",
				"    custom 'lowercase', { str -> str.toLowerCase() }",
				"  }",
				"}");
		setFile("generated/excluded.md").toLines("A B C");
		setFile("generated/nested/excluded.md").toLines("A B C");
		setFile("manual.md").toLines("A B C");

		gradleRunner().withArguments("spotlessApply").build();

		assertFile("generated/excluded.md").hasLines("A B C");
		assertFile("generated/nested/excluded.md").hasLines("A B C");
		assertFile("manual.md").hasLines("a b c");
	}

	@Test
	void targetExcludeWithDirectoryProperty() throws IOException {
		setFile("build.gradle").toLines(
				"plugins { id 'com.diffplug.spotless' }",
				"def excluded = objects.directoryProperty()",
				"excluded.set(layout.projectDirectory.dir('generated'))",
				"spotless {",
				"  format 'toLower', {",
				"    target '**/*.md'",
				"    targetExclude excluded",
				"    custom 'lowercase', { str -> str.toLowerCase() }",
				"  }",
				"}");
		setFile("generated/excluded.md").toLines("A B C");
		setFile("manual.md").toLines("A B C");

		gradleRunner().withArguments("spotlessApply").build();

		assertFile("generated/excluded.md").hasLines("A B C");
		assertFile("manual.md").hasLines("a b c");
	}

	@Test
	void targetExcludeWithDirectoryProvider() throws IOException {
		setFile("build.gradle").toLines(
				"plugins { id 'com.diffplug.spotless' }",
				"spotless {",
				"  format 'toLower', {",
				"    target '**/*.md'",
				"    targetExclude providers.provider { layout.projectDirectory.dir('generated') }",
				"    custom 'lowercase', { str -> str.toLowerCase() }",
				"  }",
				"}");
		setFile("generated/excluded.md").toLines("A B C");
		setFile("manual.md").toLines("A B C");

		gradleRunner().withArguments("spotlessApply").build();

		assertFile("generated/excluded.md").hasLines("A B C");
		assertFile("manual.md").hasLines("a b c");
	}

	@Test
	void targetExcludeWithDirectoryAndConfigurationCache() throws IOException {
		setFile("gradle.properties").toLines("org.gradle.configuration-cache=true");
		setFile("build.gradle").toLines(
				"plugins { id 'com.diffplug.spotless' }",
				"spotless {",
				"  format 'toLower', {",
				"    target '**/*.md'",
				"    targetExclude layout.projectDirectory.dir('generated')",
				"    custom 'lowercase', { str -> str.toLowerCase() }",
				"  }",
				"}");
		setFile("generated/excluded.md").toLines("A B C");
		setFile("manual.md").toLines("A B C");

		gradleRunner().withArguments("spotlessApply").build();
		assertFile("generated/excluded.md").hasLines("A B C");
		assertFile("manual.md").hasLines("a b c");

		// the exclusion still applies on a second run with the cache enabled
		setFile("manual.md").toLines("D E F");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("generated/excluded.md").hasLines("A B C");
		assertFile("manual.md").hasLines("d e f");
	}

}
