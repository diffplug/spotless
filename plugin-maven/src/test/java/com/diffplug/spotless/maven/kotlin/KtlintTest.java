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
package com.diffplug.spotless.maven.kotlin;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.ProcessRunner;
import com.diffplug.spotless.maven.MavenIntegrationHarness;

/**
 * Unit test for {@link Ktlint}
 */
class KtlintTest extends MavenIntegrationHarness {
	@Test
	void testKtlint() throws Exception {
		writePomWithKotlinSteps("<ktlint/>");

		String path = "src/main/kotlin/Main.kt";
		setFile(path).toResource("kotlin/ktlint/basic.dirty");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).sameAsResource("kotlin/ktlint/basic.clean");
	}

	@Test
	void testKtlintEditorConfigOverride() throws Exception {
		writePomWithKotlinSteps("<ktlint>\n" +
				"  <editorConfigOverride>\n" +
				"    <ij_kotlin_allow_trailing_comma>true</ij_kotlin_allow_trailing_comma>\n" +
				"    <ij_kotlin_allow_trailing_comma_on_call_site>true</ij_kotlin_allow_trailing_comma_on_call_site>\n" +
				"  </editorConfigOverride>\n" +
				"</ktlint>");

		String path = "src/main/kotlin/Main.kt";
		setFile(path).toResource("kotlin/ktlint/experimentalEditorConfigOverride.dirty");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).sameAsResource("kotlin/ktlint/experimentalEditorConfigOverride.clean");
	}

	@Test
	void testReadCodeStyleFromEditorConfigFile() throws Exception {
		setFile(".editorconfig").toResource("kotlin/ktlint/ktlint_official/.editorconfig");
		writePomWithKotlinSteps("<ktlint/>");
		checkKtlintOfficialStyle();
	}

	@Test
	void testSetEditorConfigCanOverrideEditorConfigFile() throws Exception {
		setFile(".editorconfig").toResource("kotlin/ktlint/intellij_idea/.editorconfig");
		writePomWithKotlinSteps("<ktlint>\n" +
				"  <editorConfigOverride>\n" +
				"    <ktlint_code_style>ktlint_official</ktlint_code_style>\n" +
				"  </editorConfigOverride>\n" +
				"</ktlint>");
		checkKtlintOfficialStyle();
	}

	@Test
	void testWithCustomRuleSetApply() throws Exception {
		writePomWithKotlinSteps("<ktlint>\n" +
				"  <customRuleSets>\n" +
				"    <value>io.nlopez.compose.rules:ktlint:0.3.3</value>\n" +
				"  </customRuleSets>\n" +
				"  <editorConfigOverride>\n" +
				"    <ktlint_function_naming_ignore_when_annotated_with>Composable</ktlint_function_naming_ignore_when_annotated_with>\n" +
				"  </editorConfigOverride>\n" +
				"</ktlint>");
		setFile("src/main/kotlin/Main.kt").toResource("kotlin/ktlint/listScreen.dirty");
		ProcessRunner.Result result = mavenRunner().withArguments("spotless:check").runHasError();
		assertTrue(result.toString().contains("Composable functions that return Unit should start with an uppercase letter."));
	}

	private void checkKtlintOfficialStyle() throws Exception {
		String path = "src/main/kotlin/Main.kt";
		setFile(path).toResource("kotlin/ktlint/experimentalEditorConfigOverride.dirty");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).sameAsResource("kotlin/ktlint/experimentalEditorConfigOverride.ktlintOfficial.clean");
	}
}
