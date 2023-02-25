/*
 * Copyright 2016-2021 DiffPlug
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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/** Reproduces https://github.com/diffplug/spotless/issues/506 */
class MultiProjectAfterEvaluate extends GradleIntegrationHarness {
	@Test
	void failureDoesntTriggerAll() throws IOException {
		setFile("settings.gradle").toLines("include 'sub'");
		setFile("sub/build.gradle")
				.toLines(
						"plugins {",
						"  id 'com.diffplug.spotless'",
						"  id 'java'",
						"}",
						"repositories { mavenCentral() }",
						"spotless { java { googleJavaFormat() } }");
		String output = gradleRunner().withArguments("spotlessApply", "--warning-mode", "all").build().getOutput().replace("\r\n", "\n");
		Assertions.assertThat(output).doesNotContain("Using method Project#afterEvaluate(Action) when the project is already evaluated has been deprecated.");
	}
}
