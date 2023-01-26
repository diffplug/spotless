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
package com.diffplug.spotless.maven.javascript;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.diffplug.spotless.ProcessRunner;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.maven.MavenIntegrationHarness;
import com.diffplug.spotless.npm.EslintFormatterStep;
import com.diffplug.spotless.npm.EslintStyleGuide;
import com.diffplug.spotless.tag.NpmTest;

@NpmTest
class JavascriptFormatStepTest extends MavenIntegrationHarness {

	private static final String TEST_FILE_PATH = "src/main/javascript/test.js";

	private static String styleGuideDevDependenciesString(String styleGuideName) {
		return EslintStyleGuide.fromNameOrNull(styleGuideName).asMavenXmlStringMergedWith(EslintFormatterStep.defaultDevDependencies());
	}

	@NpmTest
	@Nested
	class EslintCustomRulesTest extends MavenIntegrationHarness {

		@Test
		void eslintConfigFile() throws Exception {
			writePomWithJavascriptSteps(
					TEST_FILE_PATH,
					"<eslint>",
					"  <configFile>.eslintrc.js</configFile>",
					"</eslint>");
			setFile(".eslintrc.js").toResource("npm/eslint/javascript/custom_rules/.eslintrc.js");
			setFile(TEST_FILE_PATH).toResource("npm/eslint/javascript/custom_rules/javascript-es6.dirty");

			ProcessRunner.Result result = mavenRunner().withArguments("spotless:apply").runNoError();
			assertFile(TEST_FILE_PATH).sameAsResource("npm/eslint/javascript/custom_rules/javascript-es6.clean");
		}

		@Test
		void eslintConfigJs() throws Exception {
			final String configJs = ResourceHarness.getTestResource("npm/eslint/javascript/custom_rules/.eslintrc.js")
					.replace("module.exports = ", "");
			writePomWithJavascriptSteps(
					TEST_FILE_PATH,
					"<eslint>",
					"  <configJs>" + configJs + "</configJs>",
					"</eslint>");
			setFile(TEST_FILE_PATH).toResource("npm/eslint/javascript/custom_rules/javascript-es6.dirty");

			mavenRunner().withArguments("spotless:apply").runNoError();
			assertFile(TEST_FILE_PATH).sameAsResource("npm/eslint/javascript/custom_rules/javascript-es6.clean");
		}

	}

	@NpmTest
	@Nested
	class EslintStyleguidesTest extends MavenIntegrationHarness {

		@ParameterizedTest(name = "{index}: eslint js formatting with configFile using styleguide {0}")
		@ValueSource(strings = {"airbnb", "google", "standard", "xo"})
		void eslintJsStyleguideUsingConfigFile(String styleGuide) throws Exception {
			final String styleGuidePath = "npm/eslint/javascript/styleguide/" + styleGuide;

			writePomWithJavascriptSteps(
					TEST_FILE_PATH,
					"<eslint>",
					"  <configFile>.eslintrc.js</configFile>",
					"  " + styleGuideDevDependenciesString(styleGuide),
					"</eslint>");
			setFile(".eslintrc.js").toResource(styleGuidePath + "/.eslintrc.js");
			setFile(TEST_FILE_PATH).toResource(styleGuidePath + "/javascript-es6.dirty");

			mavenRunner().withArguments("spotless:apply").runNoError();
			assertFile(TEST_FILE_PATH).sameAsResource(styleGuidePath + "/javascript-es6.clean");
		}

		@ParameterizedTest(name = "{index}: eslint js formatting with inline config using styleguide {0}")
		@ValueSource(strings = {"airbnb", "google", "standard", "xo"})
		void eslintJsStyleguideUsingInlineConfig(String styleGuide) throws Exception {
			final String styleGuidePath = "npm/eslint/javascript/styleguide/" + styleGuide;

			final String escapedInlineConfig = ResourceHarness.getTestResource(styleGuidePath + "/.eslintrc.js")
					.replace("<", "&lt;")
					.replace(">", "&gt;");
			writePomWithJavascriptSteps(
					TEST_FILE_PATH,
					"<eslint>",
					"  <configJs>" + escapedInlineConfig + "</configJs>",
					"  " + styleGuideDevDependenciesString(styleGuide),
					"</eslint>");
			setFile(TEST_FILE_PATH).toResource(styleGuidePath + "/javascript-es6.dirty");

			mavenRunner().withArguments("spotless:apply").runNoError();
			assertFile(TEST_FILE_PATH).sameAsResource(styleGuidePath + "/javascript-es6.clean");
		}

		@Test
		void provideCustomDependenciesForStyleguideStandard() throws Exception {
			final String styleGuidePath = "npm/eslint/javascript/styleguide/standard";

			writePomWithJavascriptSteps(
					TEST_FILE_PATH,
					"<eslint>",
					"  <configFile>.eslintrc.js</configFile>",
					"  <devDependencies>",
					"    <eslint>8.28.0</eslint>",
					"    <eslint-config-standard>17.0.0</eslint-config-standard>",
					"    <eslint-plugin-import>2.26.0</eslint-plugin-import>",
					"    <eslint-plugin-n>15.6.0</eslint-plugin-n>",
					"    <eslint-plugin-promise>6.1.1</eslint-plugin-promise>",
					"  </devDependencies>",
					"</eslint>");
			setFile(".eslintrc.js").toResource(styleGuidePath + "/.eslintrc.js");

			setFile(TEST_FILE_PATH).toResource(styleGuidePath + "/javascript-es6.dirty");

			mavenRunner().withArguments("spotless:apply").runNoError();
			assertFile(TEST_FILE_PATH).sameAsResource(styleGuidePath + "/javascript-es6.clean");
		}
	}
}
