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

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.diffplug.spotless.category.BlackTest;

@Category(BlackTest.class)
public class PythonGradleTest extends GradleIntegrationHarness {
	@Test
	public void black() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"  id 'com.diffplug.spotless'",
				"}",
				"spotless {",
				"  python {",
				"    target 'src/**/*.py'",
				"    black()",
				"  }",
				"}");
		setFile("src/test.py").toResource("python/black/black.dirty");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/test.py").sameAsResource("python/black/black.clean");
	}
}
