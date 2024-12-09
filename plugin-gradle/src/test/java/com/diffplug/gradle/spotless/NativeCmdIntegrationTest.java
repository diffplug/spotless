/*
 * Copyright 2024 DiffPlug
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

import static org.assertj.core.api.Assumptions.assumeThat;

import java.io.File;
import java.io.IOException;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

interface NativeCmdIntegrationTest {
	@Test
	default void nativeCmd() throws IOException {
		// This will only work if /usr/bin/sed is available
		assumeThat(new File("/usr/bin/sed")).exists();

		GradleIntegrationHarness harness = (GradleIntegrationHarness) this;
		harness.setFile("build.gradle").toLines(
				"plugins {",
				"  id 'com.diffplug.spotless'",
				"}",
				"spotless {",
				"  lineEndings 'UNIX'",
				"  format 'test', {",
				"    target '*.txt'",
				"    nativeCmd('sed', '/usr/bin/sed', ['s/placeholder/replaced/g'])",
				"  }",
				"}");
		harness.setFile("test.txt").toResource("native_cmd/dirty.txt");
		harness.gradleRunner().withArguments("spotlessApply", "--stacktrace").build();
		harness.assertFile("test.txt").sameAsResource("native_cmd/clean.txt");
	}

	class NativeCmdWithoutConfigCacheTest extends GradleIntegrationHarness implements NativeCmdIntegrationTest {}

	class NativeCmdWithConfigCacheTest extends GradleIntegrationHarness implements NativeCmdIntegrationTest {
		@Override
		public GradleRunner gradleRunner() throws IOException {
			setFile("gradle.properties").toContent("org.gradle.unsafe.configuration-cache=true");
			setFile("gradle.properties").toContent("org.gradle.configuration-cache=true");
			return super.gradleRunner().withGradleVersion(GradleVersionSupport.CONFIGURATION_CACHE.version);
		}
	}
}
