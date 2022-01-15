/*
 * Copyright 2016-2022 DiffPlug
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
import static org.junit.jupiter.api.condition.JRE.JAVA_11;

import java.io.IOException;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;

class KotlinExtensionTest extends GradleIntegrationHarness {
	private static final String HEADER = "// License Header";
	private static final String HEADER_WITH_YEAR = "// License Header $YEAR";

	@Test
	void integration() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.5.31'",
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
	void integrationDiktat() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.4.30'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        diktat()",
				"    }",
				"}");
		setFile("src/main/kotlin/com/example/Main.kt").toResource("kotlin/diktat/main.dirty");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/kotlin/com/example/Main.kt").sameAsResource("kotlin/diktat/main.clean");
	}

	@Test
	@EnabledForJreRange(min = JAVA_11) // ktfmt's dependency, google-java-format 1.8 requires a minimum of JRE 11+.
	void integrationKtfmt() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.5.31'",
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
	@EnabledForJreRange(min = JAVA_11) // ktfmt's dependency, google-java-format 1.8 requires a minimum of JRE 11+.
	void integrationKtfmt_dropboxStyle_0_18() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.5.31'",
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
	@EnabledForJreRange(min = JAVA_11) // ktfmt's dependency, google-java-format 1.8 requires a minimum of JRE 11+.
	void integrationKtfmt_dropboxStyle_0_19() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.5.31'",
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
	void testWithIndentation() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.5.31'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        ktlint('0.32.0').userData(['indent_size': '6'])",
				"    }",
				"}");
		setFile("src/main/kotlin/basic.kt").toResource("kotlin/ktlint/basic.dirty");
		BuildResult result = gradleRunner().withArguments("spotlessCheck", "--stacktrace").buildAndFail();
		assertThat(result.getOutput()).contains("Unexpected indentation (4) (it should be 6)");
	}

	@Test
	void testWithHeader() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.5.31'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        licenseHeader('" + HEADER + "')",
				"        ktlint()",
				"    }",
				"}");
		setFile("src/main/kotlin/AnObject.kt").toResource("kotlin/licenseheader/KotlinCodeWithoutHeader.test");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/kotlin/AnObject.kt").hasContent(HEADER + "\n" + getTestResource("kotlin/licenseheader/KotlinCodeWithoutHeader.test"));
	}

	@Test
	@EnabledForJreRange(min = JAVA_11) // ktfmt's dependency, google-java-format 1.8 requires a minimum of JRE 11+.
	void testWithHeaderKtfmt() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.5.31'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        licenseHeader('" + HEADER + "')",
				"        ktfmt()",
				"    }",
				"}");
		setFile("src/main/kotlin/AnObject.kt").toResource("kotlin/licenseheader/KotlinCodeWithoutHeader.test");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/kotlin/AnObject.kt").hasContent(HEADER + "\n" + getTestResource("kotlin/licenseheader/KotlinCodeWithoutHeaderKtfmt.test"));
	}

	@Test
	void testWithCustomHeaderSeparator() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.5.31'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        licenseHeader ('" + HEADER + "', '@file')",
				"        ktlint()",
				"    }",
				"}");
		setFile("src/main/kotlin/AnObject.kt").toResource("kotlin/licenseheader/KotlinCodeWithoutHeader.test");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/kotlin/AnObject.kt").hasContent(HEADER + "\n" + getTestResource("kotlin/licenseheader/KotlinCodeWithoutHeader.test"));
	}

	@Test
	@EnabledForJreRange(min = JAVA_11) // ktfmt's dependency, google-java-format 1.8 requires a minimum of JRE 11+.
	void testWithCustomHeaderSeparatorKtfmt() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.5.31'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        licenseHeader ('" + HEADER + "', '@file')",
				"        ktfmt()",
				"    }",
				"}");
		setFile("src/main/kotlin/AnObject.kt").toResource("kotlin/licenseheader/KotlinCodeWithoutHeader.test");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/kotlin/AnObject.kt").hasContent(HEADER + "\n" + getTestResource("kotlin/licenseheader/KotlinCodeWithoutHeaderKtfmt.test"));
	}

	@Test
	void testWithNonStandardYearSeparator() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.5.31'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        licenseHeader('" + HEADER_WITH_YEAR + "').yearSeparator(', ')",
				"        ktlint()",
				"    }",
				"}");

		setFile("src/main/kotlin/AnObject.kt").toResource("kotlin/licenseheader/KotlinCodeWithMultiYearHeader.test");
		setFile("src/main/kotlin/AnObject2.kt").toResource("kotlin/licenseheader/KotlinCodeWithMultiYearHeader2.test");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/kotlin/AnObject.kt").matches(matcher -> {
			matcher.startsWith("// License Header 2012, 2014");
		});
		assertFile("src/main/kotlin/AnObject2.kt").matches(matcher -> {
			matcher.startsWith("// License Header 2012, 2014");
		});
	}

	@Test
	@EnabledForJreRange(min = JAVA_11) // ktfmt's dependency, google-java-format 1.8 requires a minimum of JRE 11+.
	void testWithNonStandardYearSeparatorKtfmt() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.5.31'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        licenseHeader('" + HEADER_WITH_YEAR + "').yearSeparator(', ')",
				"        ktfmt()",
				"    }",
				"}");

		setFile("src/main/kotlin/AnObject.kt").toResource("kotlin/licenseheader/KotlinCodeWithMultiYearHeader.test");
		setFile("src/main/kotlin/AnObject2.kt").toResource("kotlin/licenseheader/KotlinCodeWithMultiYearHeader2.test");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/kotlin/AnObject.kt").matches(matcher -> {
			matcher.startsWith("// License Header 2012, 2014");
		});
		assertFile("src/main/kotlin/AnObject2.kt").matches(matcher -> {
			matcher.startsWith("// License Header 2012, 2014");
		});
	}
}
