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
package com.diffplug.gradle.spotless.freshmark;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.diffplug.gradle.spotless.GradleIntegrationTest;

public class FreshmarkTest extends GradleIntegrationTest {
	@Test
	public void integration() throws IOException {
		write("build.gradle",
				"plugins {",
				"    id 'java'",
				"    id 'com.diffplug.gradle.spotless'",
				"}",
				"spotless {",
				"    freshmark {",
				"        properties([lib: 'MyLib', author: 'Me'])",
				"    }",
				"}");
		String unformatted = getTestResource("freshmark/FreshMarkUnformatted.test");
		write("README.md", unformatted);
		gradleRunner().withArguments("spotlessApply").build();
		String result = read("README.md");
		String formatted = getTestResource("freshmark/FreshMarkFormatted.test");
		Assert.assertEquals(formatted, result);
	}
}
