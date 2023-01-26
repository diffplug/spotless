/*
 * Copyright 2016-2023 DiffPlug
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

class KotlinExtensionTest extends GradleIntegrationHarness {
	private static final String HEADER = "// License Header";
	private static final String HEADER_WITH_YEAR = "// License Header $YEAR";

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
		setFile("src/main/kotlin/Main.kt").toResource("kotlin/ktlint/experimentalEditorConfigOverride.dirty");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/kotlin/Main.kt").sameAsResource("kotlin/ktlint/experimentalEditorConfigOverride.clean");
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
}
