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
import org.junit.Assert;
import org.junit.Test;

import com.diffplug.gradle.spotless.GradleIntegrationTest;

public class GroovyDefaultTargetTest extends GradleIntegrationTest {

	private final String HEADER = "//My tests header";

	@Test
	public void includeJava() throws IOException {
		testIncludeExcludeOption(false);
	}

	@Test
	public void excludeJava() throws IOException {
		testIncludeExcludeOption(true);
	}

	private void testIncludeExcludeOption(boolean excludeJava) throws IOException {
		String excludeStatement = excludeJava ? "excludeJava" : "";
		write("build.gradle",
				"plugins {",
				"    id 'com.diffplug.gradle.spotless'",
				"}",
				"repositories { mavenLocal() }",
				"",
				"apply plugin: 'groovy'",
				"",
				"spotless {",
				"    groovy {",
				excludeStatement,
				"        licenseHeader('" + HEADER + "')",
				"    }",
				"}");

		String original = getTestResource("groovy/licenseheader/JavaCodeWithoutHeader.test");

		File javaSrcJavaFile = write("src/main/java/test.java", original);
		File groovySrcJavaFile = write("src/main/groovy/test.java", original);
		File groovySrcGroovyFile = write("src/main/groovy/test.groovy", original);

		// write appends a line ending so re-read to see what the original currently looks like
		original = read("src/main/java/test.java");

		// Run
		gradleRunner().withArguments("spotlessApply").build();

		// Common checks
		assertFileContent(original, javaSrcJavaFile);

		Assertions.assertThat(read(groovySrcGroovyFile.toPath())).contains(HEADER);

		if (excludeJava) {
			assertFileContent(original, groovySrcJavaFile);
		} else {
			Assertions.assertThat(read(groovySrcJavaFile.toPath())).contains(HEADER);
		}
	}

	@Test
	public void excludeJavaWithCustomTarget() throws IOException {
		write("build.gradle",
				"plugins {",
				"    id 'com.diffplug.gradle.spotless'",
				"}",
				"repositories { mavenLocal() }",
				"",
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

}
