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

import java.io.File;
import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class GroovyGradleExtensionTest extends GradleIntegrationTest {

	private final String HEADER = "//My tests header";

	@Test
	public void defaultTarget() throws IOException {
		testTarget(true);
	}

	@Test
	public void customTarget() throws IOException {
		testTarget(false);
	}

	private void testTarget(boolean useDefaultTarget) throws IOException {
		String target = useDefaultTarget ? "" : "target 'other.gradle'";
		File projectFile = write("build.gradle",
				"plugins {",
				"    id 'com.diffplug.gradle.spotless'",
				"}",
				"spotless {",
				"    groovyGradle {",
				target,
				"        licenseHeader('" + HEADER + "', 'plugins')",
				"    }",
				"}");

		// write appends a line ending so re-read to see what the original currently looks like
		String unModified = read(projectFile.toPath());

		// Run
		gradleRunner().withArguments("spotlessApply").build();

		if (useDefaultTarget) {
			Assertions.assertThat(read(projectFile.toPath())).contains(HEADER);
		} else {
			assertFileContent(unModified, projectFile);
		}
	}

}
