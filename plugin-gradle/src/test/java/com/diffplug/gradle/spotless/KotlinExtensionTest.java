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
import java.time.YearMonth;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

public class KotlinExtensionTest extends GradleIntegrationTest {
	private static final String HEADER = "// License Header";
	private static final String HEADER_WITH_YEAR = "// License Header $YEAR";

	@Test
	public void integration() throws IOException {
		write("build.gradle",
				"plugins {",
				"    id 'nebula.kotlin' version '1.0.6'",
				"    id 'com.diffplug.gradle.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        ktlint()",
				"    }",
				"}");
		write("src/main/kotlin/basic.kt", getTestResource("kotlin/ktlint/basic.dirty"));
		gradleRunner().withArguments("spotlessApply").build();
		String result = read("src/main/kotlin/basic.kt");
		String formatted = getTestResource("kotlin/ktlint/basic.clean");
		Assert.assertEquals(formatted, result);
	}

	@Test
	public void testWithHeader() throws IOException {
		write("build.gradle",
				"plugins {",
				"    id 'nebula.kotlin' version '1.0.6'",
				"    id 'com.diffplug.gradle.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        licenseHeader('" + HEADER + "')",
				"        ktlint()",
				"    }",
				"}");
		final File testFile = write("src/main/kotlin/test.kt", getTestResource("kotlin/licenseheader/KotlinCodeWithoutHeader.test"));
		final String original = read(testFile.toPath());
		gradleRunner().withArguments("spotlessApply").build();
		final String result = read(testFile.toPath());
		Assertions
				.assertThat(result)
				// Make sure the header gets added.
				.startsWith(HEADER)
				// Make sure that the rest of the file is still there with nothing removed.
				.endsWith(original)
				// Make sure that no additional stuff got added to the file.
				.contains(HEADER + '\n' + original);
	}

	@Test
	public void testWithCustomHeaderSeparator() throws IOException {
		write("build.gradle",
				"plugins {",
				"    id 'nebula.kotlin' version '1.0.6'",
				"    id 'com.diffplug.gradle.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        licenseHeader ('" + HEADER + "', '@file')",
				"        ktlint()",
				"    }",
				"}");
		final File testFile = write("src/main/kotlin/test.kt", getTestResource("kotlin/licenseheader/KotlinCodeWithoutHeader.test"));
		final String original = read(testFile.toPath());
		gradleRunner().withArguments("spotlessApply").build();
		final String result = read(testFile.toPath());
		Assertions
				.assertThat(result)
				// Make sure the header gets added.
				.startsWith(HEADER)
				// Make sure that the rest of the file is still there with nothing removed.
				.endsWith(original)
				// Make sure that no additional stuff got added to the file.
				.contains(HEADER + '\n' + original);
	}

	@Test
	public void testWithNonStandardYearSeparator() throws IOException {
		write("build.gradle",
				"plugins {",
				"    id 'nebula.kotlin' version '1.0.6'",
				"    id 'com.diffplug.gradle.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        licenseHeader('" + HEADER_WITH_YEAR + "').yearSeparator(', ')",
				"        ktlint()",
				"    }",
				"}");

		final File testFile = write("src/main/kotlin/test.kt", getTestResource("kotlin/licenseheader/KotlinCodeWithMultiYearHeader.test"));
		final String original = read(testFile.toPath());
		final File testFile2 = write("src/main/kotlin/test2.kt", getTestResource("kotlin/licenseheader/KotlinCodeWithMultiYearHeader2.test"));
		final String original2 = read(testFile.toPath());
		gradleRunner().withArguments("spotlessApply").build();
		final String result = read(testFile.toPath());
		final String result2 = read(testFile2.toPath());

		Assertions
				.assertThat(result)
				// Make sure the a "valid" header isn't changed
				.contains("// License Header 2012, 2014");

		Assertions
				.assertThat(result2)
				// Make sure that an "invalid" header is rewritten
				.startsWith(HEADER_WITH_YEAR.replace("$YEAR", String.valueOf(YearMonth.now().getYear())));
	}
}
