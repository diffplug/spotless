/*
 * Copyright 2016-2023 DiffPlug
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

import com.diffplug.common.base.StringPrinter;

class GroovyGradleExtensionTest extends GradleIntegrationHarness {
	private static final String HEADER = "//My tests header";

	@Test
	void defaultTarget() throws IOException {
		testTarget(true);
	}

	@Test
	void customTarget() throws IOException {
		testTarget(false);
	}

	private void testTarget(boolean useDefaultTarget) throws IOException {
		var target = useDefaultTarget ? "" : "target 'other.gradle'";
		String buildContent = StringPrinter.buildStringFromLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"spotless {",
				"    groovyGradle {",
				target,
				"        licenseHeader('" + HEADER + "', 'plugins')",
				"    }",
				"}");
		setFile("build.gradle").toContent(buildContent);

		gradleRunner().withArguments("spotlessApply").build();

		if (useDefaultTarget) {
			assertFile("build.gradle").hasContent(HEADER + "\n" + buildContent);
		} else {
			assertFile("build.gradle").hasContent(buildContent);
		}
	}
}
