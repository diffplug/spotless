/*
 * Copyright 2016-2025 DiffPlug
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

class JavaDefaultTargetTest extends GradleIntegrationHarness {
	@Test
	void integration() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"",
				"apply plugin: 'groovy'",
				"",
				"spotless {",
				"    java {",
				"        googleJavaFormat()",
				"    }",
				"}");
		setFile("src/main/java/test.java").toResource("java/googlejavaformat/JavaCodeUnformatted.test");
		setFile("src/main/groovy/test.java").toResource("java/googlejavaformat/JavaCodeUnformatted.test");
		setFile("src/main/groovy/test.groovy").toResource("java/googlejavaformat/JavaCodeUnformatted.test");

		gradleRunner().withArguments("spotlessApply").build();

		assertFile("src/main/java/test.java").sameAsResource("java/googlejavaformat/JavaCodeFormatted.test");
		assertFile("src/main/groovy/test.java").sameAsResource("java/googlejavaformat/JavaCodeFormatted.test");
		assertFile("src/main/groovy/test.groovy").sameAsResource("java/googlejavaformat/JavaCodeUnformatted.test");
	}

	@Test
	void multipleBlocksShouldWork() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"  id 'com.diffplug.spotless'",
				"  id 'java'",
				"}",
				"repositories { mavenCentral() }",
				"",
				"spotless {",
				"  java {  googleJavaFormat()  }",
				"  java {  eclipse()  }",
				"}");
		gradleRunner().withArguments("spotlessApply").build();
		gradleRunner().withArguments("spotlessApply").build();
	}

	@Test
	void removeUnusedImportsWithCleanthat() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"  id 'com.diffplug.spotless'",
				"  id 'java'",
				"}",
				"repositories { mavenCentral() }",
				"",
				"spotless {",
				"  java {  removeUnusedImports('cleanthat-javaparser-unnecessaryimport')  }",
				"}");

		setFile("src/main/java/test.java").toResource("java/removeunusedimports/Jdk17TextBlockUnformatted.test");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/java/test.java").sameAsResource("java/removeunusedimports/Jdk17TextBlockFormatted.test");
	}

	@Test
	void removeWildCardImports() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"",
				"spotless {",
				"    java {",
				"        target file('test.java')",
				"        removeWildcardImports()",
				"    }",
				"}");

		setFile("test.java").toResource("java/removewildcardimports/JavaCodeWildcardsUnformatted.test");
		gradleRunner().withArguments("spotlessApply").buildAndFail();
		assertFile("test.java").sameAsResource("java/removewildcardimports/JavaCodeWildcardsFormatted.test");
	}

	/**
	 * Triggers the special case in {@link FormatExtension#setupTask(SpotlessTask)} with {@code toggleFence} and
	 * {@code targetExcludeContentPattern} both being not {@code null}.
	 */
	@Test
	void fenceWithTargetExcludeNoMatch() throws Exception {
		setFile("build.gradle").toLines(
				"plugins {",
				"  id 'com.diffplug.spotless'",
				"  id 'java'",
				"}",
				"repositories { mavenCentral() }",
				"",
				"spotless {",
				"  java {",
				"    licenseHeader('// my-copyright')",
				"    toggleOffOn()",
				"    targetExcludeIfContentContains('excludeMe')",
				"  }",
				"}");

		setFile("src/main/java/test.java").toResource("java/targetExclude/TargetExcludeNoMatchUnformatted.test");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/java/test.java").sameAsResource("java/targetExclude/TargetExcludeNoMatchFormatted.test");
	}

	/**
	 * Triggers the special case in {@link FormatExtension#setupTask(SpotlessTask)} with {@code toggleFence} and
	 * {@code targetExcludeContentPattern} both being not {@code null}.
	 */
	@Test
	void fenceWithTargetExcludeMatch() throws Exception {
		setFile("build.gradle").toLines(
				"plugins {",
				"  id 'com.diffplug.spotless'",
				"  id 'java'",
				"}",
				"repositories { mavenCentral() }",
				"",
				"spotless {",
				"  java {",
				"    licenseHeader('// my-copyright')",
				"    toggleOffOn()",
				"    targetExcludeIfContentContains('excludeMe')",
				"  }",
				"}");

		setFile("src/main/java/test.java").toResource("java/targetExclude/TargetExcludeMatch.test");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/java/test.java").sameAsResource("java/targetExclude/TargetExcludeMatch.test");
	}
}
