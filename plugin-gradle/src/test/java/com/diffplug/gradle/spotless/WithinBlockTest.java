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

import java.io.IOException;

import org.junit.jupiter.api.Test;

class WithinBlockTest extends GradleIntegrationHarness {
	@Test
	void genericFormatTest() throws IOException {
		// make sure that the "typed-generic" closure works
		// it does, and it doesn't need `it`
		setFile("build.gradle").toLines(
				"plugins { id 'com.diffplug.spotless' }",
				"repositories { mavenCentral() }",
				"import com.diffplug.gradle.spotless.JavaExtension",
				"spotless {",
				"  format 'customJava', JavaExtension, {",
				"    target '*.java'",
				"    googleJavaFormat()",
				"  }",
				"}");
		setFile("test.java").toResource("java/googlejavaformat/JavaCodeUnformatted.test");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("test.java").sameAsResource("java/googlejavaformat/JavaCodeFormatted.test");
	}

	@Test
	void withinBlocksTourDeForce() throws IOException {
		// but down here, we need `it`, or it will bind to the parent context, why?
		setFile("build.gradle").toLines(
				"plugins { id 'com.diffplug.spotless' }",
				"repositories { mavenCentral() }",
				"import com.diffplug.gradle.spotless.JavaExtension",
				"spotless {",
				"  format 'docs', {",
				"    target '*.md'",
				"    withinBlocks 'toLower', '\\n```lower\\n', '\\n```\\n', {",
				"      custom 'lowercase', { str -> str.toLowerCase() }",
				"    }",
				"    withinBlocks 'java only', '\\n```java\\n', '\\n```\\n', JavaExtension, {",
				"      googleJavaFormat()",
				"    }",
				"  }",
				"}");
		setFile("test.md").toLines(
				"Some stuff",
				"```lower",
				" A B C",
				"D E F",
				"```",
				"And some java stuff",
				"```java",
				" public class SomeClass { public void method() {}}",
				"```",
				"And MORE!");
		gradleRunner().forwardOutput().withArguments("spotlessApply", "--stacktrace").build();
		assertFile("test.md").hasLines(
				"Some stuff",
				"```lower",
				" a b c",
				"d e f",
				"```",
				"And some java stuff",
				"```java",
				"public class SomeClass {",
				"  public void method() {}",
				"}",
				"",
				"```",
				"And MORE!");
	}
}
