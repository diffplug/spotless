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

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

public class GroovyExtensionTest extends GradleIntegrationTest {

	private static final String HEADER = "//My tests header";

	@Test
	public void includeJava() throws IOException {
		testIncludeExcludeOption(false);
	}

	@Test
	public void excludeJava() throws IOException {
		testIncludeExcludeOption(true);
	}

	private void testIncludeExcludeOption(boolean excludeJava) throws IOException {
		String excludeStatement = excludeJava ? "excludeJava()" : "";
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.gradle.spotless'",
				"}",
				"apply plugin: 'groovy'",
				"",
				"spotless {",
				"    groovy {",
				excludeStatement,
				"        licenseHeader('" + HEADER + "')",
				"    }",
				"}");

		String withoutHeader = getTestResource("groovy/licenseheader/JavaCodeWithoutHeader.test");

		setFile("src/main/java/test.java").toContent(withoutHeader);
		setFile("src/main/groovy/test.java").toContent(withoutHeader);
		setFile("src/main/groovy/test.groovy").toContent(withoutHeader);

		gradleRunner().withArguments("spotlessApply").build();

		assertFile("src/main/java/test.java").hasContent(withoutHeader);
		assertFile("src/main/groovy/test.groovy").hasContent(HEADER + "\n" + withoutHeader);
		if (excludeJava) {
			assertFile("src/main/groovy/test.java").hasContent(withoutHeader);
		} else {
			assertFile("src/main/groovy/test.java").hasContent(HEADER + "\n" + withoutHeader);
		}
	}

	@Test
	public void excludeJavaWithCustomTarget() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.gradle.spotless'",
				"}",
				"apply plugin: 'groovy'",
				"",
				"spotless {",
				"    groovy {",
				"        excludeJava()",
				"        target '**/*.java', '**/*.groovy'",
				"    }",
				"}");

		try {
			gradleRunner().withArguments("spotlessApply").build();
			Assert.fail("Exception expected when running 'excludeJava' in combination with 'target'.");
		} catch (Throwable t) {
			Assertions.assertThat(t).hasMessageContaining("'excludeJava' is not supported");
		}
	}

	@Test
	public void groovyPluginMissingCheck() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.gradle.spotless'",
				"}",
				"apply plugin: 'java'",
				"",
				"spotless {",
				"    groovy {",
				"    }",
				"}");

		try {
			gradleRunner().withArguments("spotlessApply").build();
			Assert.fail("Exception expected when using 'groovy' without 'target' if groovy-plugin is not applied.");
		} catch (Throwable t) {
			Assertions.assertThat(t).hasMessageContaining("must apply the groovy plugin before");
		}
	}

}
