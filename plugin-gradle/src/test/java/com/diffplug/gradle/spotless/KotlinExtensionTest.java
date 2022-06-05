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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
		BuildResult result = gradleRunner().withArguments("spotlessApply").buildAndFail();
		assertThat(result.getOutput()).contains("Unexpected indentation (4) (it should be 6)");
	}

	@Test
	void withExperimental() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.5.31'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        ktlint().setUseExperimental(true)",
				"    }",
				"}");
		setFile("src/main/kotlin/experimental.kt").toResource("kotlin/ktlint/experimental.dirty");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/kotlin/experimental.kt").sameAsResource("kotlin/ktlint/experimental.clean");
	}

	@Test
	void withExperimental_0_32() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.5.31'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        ktlint('0.32.0').setUseExperimental(true)",
				"    }",
				"}");
		setFile("src/main/kotlin/basic.kt").toResource("kotlin/ktlint/basic.dirty");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/kotlin/basic.kt").sameAsResource("kotlin/ktlint/basic.clean");
	}

	@Test
	void withExperimentalEditorConfigOverride() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.5.31'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        ktlint().setUseExperimental(true)",
				"            .editorConfigOverride([",
				"        	    ij_kotlin_allow_trailing_comma: true,",
				"        	    ij_kotlin_allow_trailing_comma_on_call_site: true",
				"            ])",
				"    }",
				"}");
		setFile("src/main/kotlin/experimental.kt").toResource("kotlin/ktlint/experimentalEditorConfigOverride.dirty");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/kotlin/experimental.kt").sameAsResource("kotlin/ktlint/experimentalEditorConfigOverride.clean");
	}

	@Test
	void withEditorConfigOverride_0_45_1() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.5.31'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        ktlint('0.45.1')",
				"            .editorConfigOverride([",
				"        	    indent_size: 5",
				"            ])",
				"    }",
				"}");
		setFile("src/main/kotlin/basic.kt").toResource("kotlin/ktlint/basic.dirty");
		Throwable error = assertThrows(Throwable.class,
				() -> gradleRunner().withArguments("spotlessApply").build());
		assertThat(error).hasMessageContaining("KtLint editorConfigOverride supported for version 0.45.2 and later");
	}

	/**
	 * Check that the sample used to verify the experimental ruleset is untouched by the default ruleset, to verify
	 * that enabling the experimental ruleset is actually doing something.
	 *
	 * If this test fails, it's likely that the experimental rule being used as a test graduated into the standard
	 * ruleset, and therefore a new experimental rule should be used to verify functionality.
	 */
	@Test
	void experimentalSampleUnchangedWithDefaultRuleset() throws IOException {
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
		setFile("src/main/kotlin/experimental.kt").toResource("kotlin/ktlint/experimental.dirty");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/kotlin/experimental.kt").sameAsResource("kotlin/ktlint/experimental.dirty");
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

	@Test
	@EnabledForJreRange(min = JAVA_11) // ktfmt's dependency, google-java-format 1.8 requires a minimum of JRE 11+.
	void testWithCustomMaxWidthDefaultStyleKtfmt() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.5.31'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        ktfmt().configure { options ->",
				"            options.maxWidth = 120",
				"		 }",
				"    }",
				"}");

		setFile("src/main/kotlin/max-width.kt").toResource("kotlin/ktfmt/max-width.dirty");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/kotlin/max-width.kt").sameAsResource("kotlin/ktfmt/max-width.clean");
	}

	@Test
	@EnabledForJreRange(min = JAVA_11) // ktfmt's dependency, google-java-format 1.8 requires a minimum of JRE 11+.
	void testWithCustomMaxWidthDefaultStyleKtfmtGradleKts() throws IOException {
		setFile("build.gradle.kts").toLines(
				"plugins {",
				"    id(\"org.jetbrains.kotlin.jvm\") version \"1.5.31\"",
				"    id(\"com.diffplug.spotless\")",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        ktfmt().configure { options ->",
				"            options.setMaxWidth(120)",
				"		 }",
				"    }",
				"}");

		setFile("src/main/kotlin/max-width.kt").toResource("kotlin/ktfmt/max-width.dirty");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/kotlin/max-width.kt").sameAsResource("kotlin/ktfmt/max-width.clean");
	}

	@Test
	@EnabledForJreRange(min = JAVA_11) // ktfmt's dependency, google-java-format 1.8 requires a minimum of JRE 11+.
	void testWithCustomMaxWidthDropboxStyleKtfmt() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.5.31'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        ktfmt().dropboxStyle().configure { options ->",
				"            options.maxWidth = 120",
				"		 }",
				"    }",
				"}");

		setFile("src/main/kotlin/max-width.kt").toResource("kotlin/ktfmt/max-width.dirty");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/kotlin/max-width.kt").sameAsResource("kotlin/ktfmt/max-width-dropbox.clean");
	}

	@Test
	@EnabledForJreRange(min = JAVA_11) // ktfmt's dependency, google-java-format 1.8 requires a minimum of JRE 11+.
	void testWithCustomMaxWidthDropboxStyleKtfmtGradleKts() throws IOException {
		setFile("build.gradle.kts").toLines(
				"plugins {",
				"    id(\"org.jetbrains.kotlin.jvm\") version \"1.5.31\"",
				"    id(\"com.diffplug.spotless\")",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        ktfmt().dropboxStyle().configure { options ->",
				"            options.setMaxWidth(120)",
				"		 }",
				"    }",
				"}");

		setFile("src/main/kotlin/max-width.kt").toResource("kotlin/ktfmt/max-width.dirty");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/kotlin/max-width.kt").sameAsResource("kotlin/ktfmt/max-width-dropbox.clean");
	}
}
