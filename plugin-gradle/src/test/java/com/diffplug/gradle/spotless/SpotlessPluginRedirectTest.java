/*
 * Copyright 2020 DiffPlug
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
import org.junit.Test;

import com.diffplug.common.base.StringPrinter;

public class SpotlessPluginRedirectTest extends GradleIntegrationHarness {
	@Test
	public void test() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.gradle.spotless'",
				"}");
		Assertions.assertThat(gradleRunner().buildAndFail().getOutput().replace("\r", ""))
				.contains(StringPrinter.buildStringFromLines(
						"> Failed to apply plugin [id 'com.diffplug.gradle.spotless']",
						"   > We have moved from 'com.diffplug.gradle.spotless'",
						"                     to 'com.diffplug.spotless'",
						"     To migrate:"));
	}
}
