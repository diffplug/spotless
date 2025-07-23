/*
 * Copyright 2016-2025 DiffPlug
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

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

class KotlinExtensionTest extends GradleIntegrationHarness {
	private static final String HEADER = "// License Header";
	private static final String HEADER_WITH_YEAR = "// License Header $YEAR";

	@Test
	void integrationDiktat() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.6.21'",
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
	void integrationKtfmtDropboxStyleWithPublicApi() throws IOException {
		setFile("build.gradle.kts").toLines(
				"plugins {",
				"    id(\"org.jetbrains.kotlin.jvm\") version \"1.6.21\"",
				"    id(\"com.diffplug.spotless\")",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        ktfmt(\"0.50\").dropboxStyle().configure {",
				"            it.setMaxWidth(4)",
				"            it.setBlockIndent(4)",
				"            it.setContinuationIndent(4)",
				"            it.setRemoveUnusedImports(false)",
				"            it.setManageTrailingCommas(false)",
				"        }",
				"    }",
				"}");
		setFile("src/main/kotlin/basic.kt").toResource("kotlin/ktfmt/basic.dirty");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/kotlin/basic.kt").sameAsResource("kotlin/ktfmt/basic-dropbox-style.clean");
	}

	@Test
	void withExperimentalEditorConfigOverride() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.6.21'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        ktlint().editorConfigOverride([",
				"            ktlint_experimental: \"enabled\",",
				"            ij_kotlin_allow_trailing_comma: true,",
				"            ij_kotlin_allow_trailing_comma_on_call_site: true",
				"        ])",
				"    }",
				"}");
		setFile("src/main/kotlin/Main.kt").toResource("kotlin/ktlint/experimentalEditorConfigOverride.dirty");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/kotlin/Main.kt").sameAsResource("kotlin/ktlint/experimentalEditorConfigOverride.clean");
	}

	@Test
	void testWithInvalidEditorConfigFile() throws IOException {
		String invalidPath = "invalid/path/to/.editorconfig".replace('/', File.separatorChar);

		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.6.21'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        ktlint().setEditorConfigPath('" + invalidPath.replace("\\", "\\\\") + "')",
				"    }",
				"}");
		setFile("src/main/kotlin/Main.kt").toResource("kotlin/ktlint/experimentalEditorConfigOverride.dirty");
		String buildOutput = gradleRunner().withArguments("spotlessApply").buildAndFail().getOutput();
		assertThat(buildOutput).contains("EditorConfig file does not exist: ");
		assertThat(buildOutput).contains(invalidPath);
	}

	@Test
	void testReadCodeStyleFromEditorConfigFile() throws IOException {
		setFile(".editorconfig").toResource("kotlin/ktlint/ktlint_official/.editorconfig");
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.6.21'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        ktlint()",
				"    }",
				"}");
		checkKtlintOfficialStyle();
	}

	@Test
	void testEditorConfigOverrideWithUnsetCodeStyleDoesNotOverrideEditorConfigCodeStyleWithDefault() throws IOException {
		setFile(".editorconfig").toResource("kotlin/ktlint/ktlint_official/.editorconfig");
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.6.21'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        ktlint().editorConfigOverride([",
				"	         ktlint_test_key: true,",
				"        ])",
				"    }",
				"}");
		checkKtlintOfficialStyle();
	}

	@Test
	void testSetEditorConfigCanOverrideEditorConfigFile() throws IOException {
		setFile(".editorconfig").toResource("kotlin/ktlint/intellij_idea/.editorconfig");
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.6.21'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        ktlint().editorConfigOverride([",
				"            ktlint_code_style: \"ktlint_official\",",
				"        ])",
				"    }",
				"}");
		checkKtlintOfficialStyle();
	}

	@Test
	void withCustomRuleSetApply() throws IOException {
		setFile("build.gradle.kts").toLines(
				"plugins {",
				"    id(\"org.jetbrains.kotlin.jvm\") version \"1.6.21\"",
				"    id(\"com.diffplug.spotless\")",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        ktlint(\"1.0.1\")",
				"        .customRuleSets(listOf(",
				"            \"io.nlopez.compose.rules:ktlint:0.4.25\"",
				"        ))",
				"        .editorConfigOverride(mapOf(",
				"            \"ktlint_function_naming_ignore_when_annotated_with\" to \"Composable\"",
				"        ))",
				"    }",
				"}");
		setFile("src/main/kotlin/Main.kt").toResource("kotlin/ktlint/listScreen.dirty");
		String buildOutput = gradleRunner().withArguments("spotlessCheck").buildAndFail().getOutput();
		assertThat(buildOutput).contains("Composable functions that return Unit should start with an uppercase letter.");
	}

	@Test
	void testWithHeader() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.6.21'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"        ktlint()",
				"        licenseHeader('" + HEADER + "')",
				"    }",
				"}");
		setFile("src/main/kotlin/AnObject.kt").toResource("kotlin/licenseheader/KotlinCodeWithoutHeader.test");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/kotlin/AnObject.kt").hasContent(HEADER + "\n" + getTestResource("kotlin/licenseheader/KotlinCodeWithoutHeader.test"));
	}

	@Test
	void testWithCustomMaxWidthDefaultStyleKtfmt() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.6.21'",
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

	private void checkKtlintOfficialStyle() throws IOException {
		testPath = "src/main/kotlin/Main.kt";
		setFile(testPath).toResource("kotlin/ktlint/experimentalEditorConfigOverride.dirty");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile(testPath).sameAsResource("kotlin/ktlint/experimentalEditorConfigOverride.ktlintOfficial.clean");
	}
}
