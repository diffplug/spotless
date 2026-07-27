/*
 * Copyright 2016-2026 DiffPlug
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

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
				"import  com.diffplug.spotless.kotlin.KtfmtStep.TrailingCommaManagementStrategy",
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
				"            it.setTrailingCommaManagementStrategy(TrailingCommaManagementStrategy.NONE)",
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
	void issue1901CustomRuleSetSupportsProjectDependency() throws IOException {
		setFile("settings.gradle.kts").toContent("include(\"ktlint-rules\", \"rule-support\")");
		setFile("rule-support/build.gradle.kts").toContent("plugins { java }");
		setFile("rule-support/src/main/java/support/Marker.java").toContent("package support; public final class Marker { public static int version() { return 1; } }\n");
		setFile("ktlint-rules/build.gradle.kts").toContent("""
				plugins { java }
				repositories { mavenCentral() }
				dependencies {
				    implementation(project(":rule-support"))
				    compileOnly("com.pinterest.ktlint:ktlint-cli-ruleset-core:1.0.1")
				    compileOnly("com.pinterest.ktlint:ktlint-rule-engine-core:1.0.1")
				}
				""");
		setFile("ktlint-rules/src/main/java/rules/LocalRuleSetProvider.java").toContent("""
				package rules;

				import java.nio.file.Files;
				import java.nio.file.Path;
				import java.util.Collections;
				import java.util.Set;

				import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3;
				import com.pinterest.ktlint.rule.engine.core.api.RuleProvider;
				import com.pinterest.ktlint.rule.engine.core.api.RuleSetId;
				import support.Marker;

				public final class LocalRuleSetProvider extends RuleSetProviderV3 {
				    public LocalRuleSetProvider() {
				        super(new RuleSetId("local-project"));
				        try {
				            Files.writeString(Path.of(System.getProperty("spotless.test.rule.version")), Integer.toString(Marker.version()));
				        } catch (Exception e) {
				            throw new RuntimeException(e);
				        }
				    }

				    @Override
				    public Set<RuleProvider> getRuleProviders() {
				        return Collections.emptySet();
				    }
				}
				""");
		setFile("ktlint-rules/src/main/resources/META-INF/services/com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3")
				.toContent("rules.LocalRuleSetProvider\n");
		setFile("build.gradle.kts").toContent("""
				plugins {
				    id("com.diffplug.spotless")
				}
				repositories { mavenCentral() }
				spotless {
				    kotlin {
				        target("src/**/*.kt")
				        ktlint("1.0.1").customRuleSets(project(":ktlint-rules"))
				    }
				}
				""");
		setFile("src/main/kotlin/Main.kt").toContent("fun main() {}\n");
		String ruleVersionProperty = "-Dspotless.test.rule.version=" + newFile("rule-version.txt").getAbsolutePath();

		BuildResult firstRun = gradleRunner()
				.withGradleVersion("9.5.1")
				.withArguments("spotlessCheck", "--configuration-cache", "--stacktrace", ruleVersionProperty)
				.build();
		assertThat(firstRun.getOutput()).contains("Configuration cache entry stored.");
		assertThat(firstRun.task(":ktlint-rules:jar")).isNotNull();
		assertThat(firstRun.task(":rule-support:jar")).isNotNull();
		assertThat(firstRun.task(":spotlessKotlin")).isNotNull();
		assertFile("rule-version.txt").hasContent("1");

		setFile("rule-support/src/main/java/support/Marker.java").toContent("package support; public final class Marker { public static int version() { return 2; } }\n");
		BuildResult secondRun = gradleRunner()
				.withGradleVersion("9.5.1")
				.withArguments("spotlessCheck", "--configuration-cache", "--stacktrace", ruleVersionProperty)
				.build();
		assertThat(secondRun.getOutput()).contains("Reusing configuration cache.");
		assertThat(secondRun.task(":rule-support:jar").getOutcome()).isNotEqualTo(TaskOutcome.UP_TO_DATE);
		assertThat(secondRun.task(":spotlessKotlin").getOutcome()).isNotEqualTo(TaskOutcome.UP_TO_DATE);
		assertFile("rule-version.txt").hasContent("2");
	}

	@Test
	void ktlintProjectDependenciesShareConflictResolutionWithMavenRuleSets() throws IOException {
		// The Maven rule graph requests support v1, while the project rule graph requests v2 and calls a v2-only method.
		setFile("settings.gradle.kts").toContent("include(\"support-v1\", \"support-v2\", \"conflict-anchor\", \"ktlint-rules\")");
		setFile("support-v1/build.gradle").toContent(publishedSupportBuild("1"));
		setFile("support-v2/build.gradle").toContent(publishedSupportBuild("2"));
		setFile("support-v1/src/main/java/support/Api.java").toContent("""
				package support;
				public final class Api {
				    public static String existing() { return "v1"; }
				}
				""");
		setFile("support-v2/src/main/java/support/Api.java").toContent("""
				package support;
				public final class Api {
				    public static String existing() { return "v2"; }
				    public static String addedInV2() { return "v2"; }
				}
				""");
		setFile("conflict-anchor/build.gradle").toContent("""
				plugins {
				    id 'java-library'
				    id 'maven-publish'
				}
				group = 'test'
				version = '1'
				dependencies { api 'test:support:1' }
				publishing {
				    publications {
				        mavenJava(MavenPublication) {
				            from components.java
				            artifactId = 'conflict-anchor'
				        }
				    }
				    repositories {
				        maven { url = rootProject.layout.projectDirectory.dir('repo') }
				    }
				}
				""");
		setFile("ktlint-rules/build.gradle").toContent("""
				plugins { id 'java-library' }
				dependencies {
				    implementation 'test:support:2'
				    compileOnly 'com.pinterest.ktlint:ktlint-cli-ruleset-core:1.0.1'
				    compileOnly 'com.pinterest.ktlint:ktlint-rule-engine-core:1.0.1'
				}
				""");
		setFile("ktlint-rules/src/main/java/rules/LocalRuleSetProvider.java").toContent("""
				package rules;

				import java.nio.file.Files;
				import java.nio.file.Path;
				import java.util.Collections;
				import java.util.Set;

				import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3;
				import com.pinterest.ktlint.rule.engine.core.api.RuleProvider;
				import com.pinterest.ktlint.rule.engine.core.api.RuleSetId;
				import support.Api;

				public final class LocalRuleSetProvider extends RuleSetProviderV3 {
				    public LocalRuleSetProvider() {
				        super(new RuleSetId("local-project"));
				        try {
				            Files.writeString(
				                    Path.of(System.getProperty("spotless.test.rule.version")),
				                    Api.addedInV2());
				        } catch (Exception e) {
				            throw new RuntimeException(e);
				        }
				    }

				    @Override
				    public Set<RuleProvider> getRuleProviders() {
				        return Collections.emptySet();
				    }
				}
				""");
		setFile("ktlint-rules/src/main/resources/META-INF/services/com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3")
				.toContent("rules.LocalRuleSetProvider\n");
		setFile("build.gradle.kts").toContent("""
				plugins {
				    id("com.diffplug.spotless")
				}
				allprojects {
				    repositories {
				        maven { url = uri(rootProject.layout.projectDirectory.dir("repo")) }
				        mavenCentral()
				    }
				}
				spotless {
				    kotlin {
				        target("src/**/*.kt")
				        ktlint("1.0.1").customRuleSets(
				            "test:conflict-anchor:1",
				            project(":ktlint-rules"))
				    }
				}
				""");
		setFile("src/main/kotlin/Main.kt").toContent("fun main() {}\n");

		gradleRunner()
				.withGradleVersion("9.5.1")
				.withArguments(
						":support-v1:publishMavenJavaPublicationToMavenRepository",
						":support-v2:publishMavenJavaPublicationToMavenRepository",
						":conflict-anchor:publishMavenJavaPublicationToMavenRepository")
				.build();

		String ruleVersionProperty = "-Dspotless.test.rule.version=" + newFile("rule-version.txt").getAbsolutePath();
		BuildResult result = gradleRunner()
				.withGradleVersion("9.5.1")
				.withArguments("spotlessCheck", "--configuration-cache", "--stacktrace", ruleVersionProperty)
				.build();

		assertThat(result.getOutput()).contains("Configuration cache entry stored.");
		assertFile("rule-version.txt").hasContent("v2");
	}

	@ParameterizedTest(name = "rejects conflicting ktlint {0}")
	@ValueSource(strings = {"1.0.0", "1.1.0"})
	void ktlintProjectDependencyCannotOverrideRequestedVersion(String conflictingVersion) throws IOException {
		setFile("settings.gradle.kts").toContent("include(\"ktlint-rules\")");
		setFile("ktlint-rules/build.gradle.kts").toContent("""
				plugins { java }
				dependencies {
				    runtimeOnly("com.pinterest.ktlint:ktlint-cli:%s")
				}
				""".formatted(conflictingVersion));
		setFile("build.gradle.kts").toContent("""
				plugins {
				    id("com.diffplug.spotless")
				}
				repositories { mavenCentral() }
				spotless {
				    kotlin {
				        target("src/**/*.kt")
				        ktlint("1.0.1").customRuleSets(project(":ktlint-rules"))
				    }
				}
				""");
		setFile("src/main/kotlin/Main.kt").toContent("fun main() {}\n");

		BuildResult result = gradleRunner()
				.withGradleVersion("9.5.1")
				.withArguments("spotlessCheck", "--stacktrace")
				.buildAndFail();

		assertThat(result.getOutput()).contains(
				"The dependency graph requests 'com.pinterest.ktlint:ktlint-cli:" + conflictingVersion + "'",
				"Spotless is configured to use 'com.pinterest.ktlint:ktlint-cli:1.0.1'");
	}

	@Test
	void ktlintProjectDependencyPreservesGradleVariantFailure() throws IOException {
		setFile("settings.gradle.kts").toContent("include(\"ktlint-rules\")");
		setFile("ktlint-rules/build.gradle.kts").toContent("");
		setFile("build.gradle.kts").toContent("""
				plugins {
				    id("com.diffplug.spotless")
				}
				repositories { mavenCentral() }
				spotless {
				    kotlin {
				        target("src/**/*.kt")
				        ktlint("1.0.1").customRuleSets(project(":ktlint-rules"))
				    }
				}
				""");
		setFile("src/main/kotlin/Main.kt").toContent("fun main() {}\n");

		BuildResult result = gradleRunner()
				.withGradleVersion("9.5.1")
				.withArguments("spotlessCheck", "--stacktrace")
				.buildAndFail();

		assertThat(result.getOutput())
				.contains("No matching variant of project :ktlint-rules was found")
				.doesNotContain("You need to add a repository containing");
	}

	private static String publishedSupportBuild(String version) {
		return """
				plugins {
				    id 'java-library'
				    id 'maven-publish'
				}
				group = 'test'
				version = '%s'
				publishing {
				    publications {
				        mavenJava(MavenPublication) {
				            from components.java
				            artifactId = 'support'
				        }
				    }
				    repositories {
				        maven { url = rootProject.layout.projectDirectory.dir('repo') }
				    }
				}
				""".formatted(version);
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
		String path = "src/main/kotlin/Main.kt";
		setFile(path).toResource("kotlin/ktlint/experimentalEditorConfigOverride.dirty");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile(path).sameAsResource("kotlin/ktlint/experimentalEditorConfigOverride.ktlintOfficial.clean");
	}
}
