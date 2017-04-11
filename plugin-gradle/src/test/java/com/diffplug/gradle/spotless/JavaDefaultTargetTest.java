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

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.diffplug.gradle.spotless.GradleIntegrationTest;

public class JavaDefaultTargetTest extends GradleIntegrationTest {
	@Test
	public void integration() throws IOException {
		write("build.gradle",
				"buildscript { repositories { mavenCentral() } }",
				"plugins {",
				"    id 'com.diffplug.gradle.spotless'",
				"}",
				"",
				"apply plugin: 'groovy'",
				"",
				"spotless {",
				"    java {",
				"        googleJavaFormat()",
				"    }",
				"}");
		String input = getTestResource("java/googlejavaformat/JavaCodeUnformatted.test");
		write("src/main/java/test.java", input);
		write("src/main/groovy/test.java", input);
		write("src/main/groovy/test.groovy", input);

		// write appends a line ending so re-read to see what groovy currently looks like
		String groovyInput = read("src/main/groovy/test.groovy");

		gradleRunner().withArguments("spotlessApply").build();

		String result = read("src/main/java/test.java");
		String output = getTestResource("java/googlejavaformat/JavaCodeFormatted.test");
		Assert.assertEquals("Java code in the java directory should be formatted.", output, result);

		result = read("src/main/groovy/test.java");
		Assert.assertEquals("Java code in the groovy directory should be formatted.", output, result);

		result = read("src/main/groovy/test.groovy");
		Assert.assertEquals("Groovy code in the groovy directory should not be formatted.", groovyInput, result);
	}
}
