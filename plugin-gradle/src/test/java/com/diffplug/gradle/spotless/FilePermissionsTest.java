/*
 * Copyright 2020-2023 DiffPlug
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

import static org.junit.jupiter.api.condition.OS.WINDOWS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;

class FilePermissionsTest extends GradleIntegrationHarness {
	@Test
	@DisabledOnOs(WINDOWS)
	void spotlessApplyShouldPreservePermissions() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"",
				"spotless {",
				"    java {",
				"        target file('test.java')",
				"        googleJavaFormat()",
				"    }",
				"}");
		setFile("test.java").toResource("java/googlejavaformat/JavaCodeUnformatted.test");

		Path path = rootFolder().toPath().resolve("test.java");
		Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxr--r--"));
		assertPermissions(path).isEqualTo("rwxr--r--");

		gradleRunner().withArguments("spotlessApply").build();
		assertFile("test.java").sameAsResource("java/googlejavaformat/JavaCodeFormatted.test");
		assertPermissions(path).isEqualTo("rwxr--r--");
	}

	private AbstractStringAssert<?> assertPermissions(Path path) throws IOException {
		return Assertions.assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(path)));
	}
}
