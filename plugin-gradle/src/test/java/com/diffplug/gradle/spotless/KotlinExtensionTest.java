/*
 * Copyright 2016-2020 DiffPlug
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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.gradle.testkit.runner.BuildResult;
import org.junit.Test;

import com.diffplug.spotless.JreVersion;

public class KotlinExtensionTest extends GradleIntegrationHarness {
	private static final String HEADER = "// License Header";
	private static final String HEADER_WITH_YEAR = "// License Header $YEAR";

	@Test
	public void integration() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'nebula.kotlin' version '1.3.72'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        ktlint()",
				"    }",
				"}");
		setFile("src/main/kotlin/basic.kt").toResource("kotlin/ktlint/basic.dirty");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/kotlin/basic.kt").sameAsResource("kotlin/ktlint/basic.clean");
	}

	@Test
	public void integrationKtfmt() throws IOException {
		// ktfmt's dependency, google-java-format 1.8 requires a minimum of JRE 11+.
		JreVersion.assume11OrGreater();
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'nebula.kotlin' version '1.3.72'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        ktfmt()",
				"    }",
				"}");
		setFile("src/main/kotlin/basic.kt").toResource("kotlin/ktfmt/basic.dirty");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/kotlin/basic.kt").sameAsResource("kotlin/ktfmt/basic.clean");
	}

	@Test
	public void integrationKtfmt_dropboxStyle_0_18() throws IOException {
		// ktfmt's dependency, google-java-format 1.8 requires a minimum of JRE 11+.
		JreVersion.assume11OrGreater();
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'nebula.kotlin' version '1.3.72'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        ktfmt('0.18').dropboxStyle()",
				"    }",
				"}");
		setFile("src/main/kotlin/basic.kt").toResource("kotlin/ktfmt/basic.dirty");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/kotlin/basic.kt").sameAsResource("kotlin/ktfmt/basic-dropboxstyle.clean");
	}

	@Test
	public void integrationKtfmt_dropboxStyle_0_19() throws IOException {
		// ktfmt's dependency, google-java-format 1.8 requires a minimum of JRE 11+.
		JreVersion.assume11OrGreater();
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'nebula.kotlin' version '1.3.72'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        ktfmt('0.19').dropboxStyle()",
				"    }",
				"}");
		setFile("src/main/kotlin/basic.kt").toResource("kotlin/ktfmt/basic.dirty");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/kotlin/basic.kt").sameAsResource("kotlin/ktfmt/basic-dropboxstyle.clean");
	}

	@Test
	public void testWithIndentation() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'nebula.kotlin' version '1.3.72'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        ktlint('0.21.0').userData(['indent_size': '6'])",
				"    }",
				"}");
		setFile("src/main/kotlin/basic.kt").toResource("kotlin/ktlint/basic.dirty");
		BuildResult result = gradleRunner().withArguments("spotlessApply").buildAndFail();
		assertThat(result.getOutput()).contains("Unexpected indentation (4) (it should be 6)");
	}

	@Test
	public void testWithHeader() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'nebula.kotlin' version '1.3.72'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        licenseHeader('" + HEADER + "')",
				"        ktlint()",
				"    }",
				"}");
		setFile("src/main/kotlin/test.kt").toResource("kotlin/licenseheader/KotlinCodeWithoutHeader.test");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/kotlin/test.kt").hasContent(HEADER + "\n" + getTestResource("kotlin/licenseheader/KotlinCodeWithoutHeader.test"));
	}

	@Test
	public void testWithHeaderKtfmt() throws IOException {
		// ktfmt's dependency, google-java-format 1.8 requires a minimum of JRE 11+.
		JreVersion.assume11OrGreater();
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'nebula.kotlin' version '1.3.72'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        licenseHeader('" + HEADER + "')",
				"        ktfmt()",
				"    }",
				"}");
		setFile("src/main/kotlin/test.kt").toResource("kotlin/licenseheader/KotlinCodeWithoutHeader.test");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/kotlin/test.kt").hasContent(HEADER + "\n" + getTestResource("kotlin/licenseheader/KotlinCodeWithoutHeaderKtfmt.test"));
	}

	@Test
	public void testWithCustomHeaderSeparator() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'nebula.kotlin' version '1.3.72'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        licenseHeader ('" + HEADER + "', '@file')",
				"        ktlint()",
				"    }",
				"}");
		setFile("src/main/kotlin/test.kt").toResource("kotlin/licenseheader/KotlinCodeWithoutHeader.test");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/kotlin/test.kt").hasContent(HEADER + "\n" + getTestResource("kotlin/licenseheader/KotlinCodeWithoutHeader.test"));
	}

	@Test
	public void testWithCustomHeaderSeparatorKtfmt() throws IOException {
		// ktfmt's dependency, google-java-format 1.8 requires a minimum of JRE 11+.
		JreVersion.assume11OrGreater();
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'nebula.kotlin' version '1.3.72'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        licenseHeader ('" + HEADER + "', '@file')",
				"        ktfmt()",
				"    }",
				"}");
		setFile("src/main/kotlin/test.kt").toResource("kotlin/licenseheader/KotlinCodeWithoutHeader.test");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/kotlin/test.kt").hasContent(HEADER + "\n" + getTestResource("kotlin/licenseheader/KotlinCodeWithoutHeaderKtfmt.test"));
	}

	@Test
	public void testWithNonStandardYearSeparator() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'nebula.kotlin' version '1.3.72'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        licenseHeader('" + HEADER_WITH_YEAR + "').yearSeparator(', ')",
				"        ktlint()",
				"    }",
				"}");

		setFile("src/main/kotlin/test.kt").toResource("kotlin/licenseheader/KotlinCodeWithMultiYearHeader.test");
		setFile("src/main/kotlin/test2.kt").toResource("kotlin/licenseheader/KotlinCodeWithMultiYearHeader2.test");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/kotlin/test.kt").matches(matcher -> {
			matcher.startsWith("// License Header 2012, 2014");
		});
		assertFile("src/main/kotlin/test2.kt").matches(matcher -> {
			matcher.startsWith("// License Header 2012, 2014");
		});
	}

	@Test
	public void testWithNonStandardYearSeparatorKtfmt() throws IOException {
		// ktfmt's dependency, google-java-format 1.8 requires a minimum of JRE 11+.
		JreVersion.assume11OrGreater();
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'nebula.kotlin' version '1.3.72'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        licenseHeader('" + HEADER_WITH_YEAR + "').yearSeparator(', ')",
				"        ktfmt()",
				"    }",
				"}");

		setFile("src/main/kotlin/test.kt").toResource("kotlin/licenseheader/KotlinCodeWithMultiYearHeader.test");
		setFile("src/main/kotlin/test2.kt").toResource("kotlin/licenseheader/KotlinCodeWithMultiYearHeader2.test");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/kotlin/test.kt").matches(matcher -> {
			matcher.startsWith("// License Header 2012, 2014");
		});
		assertFile("src/main/kotlin/test2.kt").matches(matcher -> {
			matcher.startsWith("// License Header 2012, 2014");
		});
	}
}
