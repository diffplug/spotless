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

import java.io.IOException;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.diffplug.spotless.npm.EslintFormatterStep;
import com.diffplug.spotless.npm.EslintStyleGuide;
import com.diffplug.spotless.tag.NpmTest;

@NpmTest
class JavascriptExtensionTest extends GradleIntegrationHarness {

	private static String styleGuideMapString(String styleGuideName) {
		return EslintStyleGuide.fromNameOrNull(styleGuideName).asGradleMapStringMergedWith(EslintFormatterStep.defaultDevDependencies());
	}

	@NpmTest
	@Nested
	class EslintGeneralJavascriptTests extends GradleIntegrationHarness {
		@Test
		void supportsEslintFormattingForJavascript() throws IOException {
			setFile(".eslintrc.js").toResource("npm/eslint/javascript/styleguide/standard/.eslintrc.js");
			setFile("build.gradle").toLines(
					"plugins {",
					"    id 'com.diffplug.spotless'",
					"}",
					"repositories { mavenCentral() }",
					"spotless {",
					"    javascript {",
					"        target 'test.js'",
					"        eslint(" + styleGuideMapString("standard") + ").configFile('.eslintrc.js')",
					"    }",
					"}");
			setFile("test.js").toResource("npm/eslint/javascript/styleguide/standard/javascript-es6.dirty");
			gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
			assertFile("test.js").sameAsResource("npm/eslint/javascript/styleguide/standard/javascript-es6.clean");
		}

		@Test
		void eslintAllowsToSpecifyEslintVersionForJavascript() throws IOException {
			setFile(".eslintrc.js").toResource("npm/eslint/javascript/custom_rules/.eslintrc.js");
			setFile("build.gradle").toLines(
					"plugins {",
					"    id 'com.diffplug.spotless'",
					"}",
					"repositories { mavenCentral() }",
					"spotless {",
					"    javascript {",
					"        target 'test.js'",
					"        eslint('8.28.0').configFile('.eslintrc.js')",
					"    }",
					"}");
			setFile("test.js").toResource("npm/eslint/javascript/custom_rules/javascript-es6.dirty");
			gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
			assertFile("test.js").sameAsResource("npm/eslint/javascript/custom_rules/javascript-es6.clean");
		}

		@Test
		void esllintAllowsToSpecifyInlineConfig() throws IOException {
			final String eslintConfigJs = String.join("\n",
					"{",
					"	env: {",
					"		browser: true,",
					"		es2021: true",
					"	},",
					"	extends: 'standard',",
					"	overrides: [",
					"	],",
					"	parserOptions: {",
					"		ecmaVersion: 'latest',",
					"		sourceType: 'module'",
					"	},",
					"	rules: {",
					"	}",
					"}");
			setFile("build.gradle").toLines(
					"plugins {",
					"    id 'com.diffplug.spotless'",
					"}",
					"repositories { mavenCentral() }",
					"spotless {",
					"    javascript {",
					"        target 'test.js'",
					"        eslint(" + styleGuideMapString("standard") + ").configJs('''" + eslintConfigJs + "''')",
					"    }",
					"}");
			setFile("test.js").toResource("npm/eslint/javascript/styleguide/standard/javascript-es6.dirty");
			gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
			assertFile("test.js").sameAsResource("npm/eslint/javascript/styleguide/standard/javascript-es6.clean");
		}

		@Test
		void eslintRequiresAnExplicitEslintConfig() throws IOException {
			setFile(".eslintrc.js").toResource("npm/eslint/javascript/styleguide/standard/.eslintrc.js");
			setFile("build.gradle").toLines(
					"plugins {",
					"    id 'com.diffplug.spotless'",
					"}",
					"repositories { mavenCentral() }",
					"spotless {",
					"    javascript {",
					"        target 'test.js'",
					"        eslint(" + styleGuideMapString("standard") + ")",
					"    }",
					"}");
			setFile("test.js").toResource("npm/eslint/javascript/styleguide/standard/javascript-es6.dirty");
			BuildResult spotlessApply = gradleRunner().withArguments("--stacktrace", "spotlessApply").buildAndFail();
			assertThat(spotlessApply.getOutput()).contains("ESLint must be configured");
		}

		@Test
		void eslintAllowsSpecifyingCustomLibraryVersions() throws IOException {
			setFile(".eslintrc.js").toResource("npm/eslint/javascript/styleguide/standard/.eslintrc.js");
			setFile("build.gradle").toLines(
					"plugins {",
					"    id 'com.diffplug.spotless'",
					"}",
					"repositories { mavenCentral() }",
					"spotless {",
					"    javascript {",
					"        target 'test.js'",
					"        eslint([",
					"            'eslint': '8.28.0',",
					"            'eslint-config-standard': '17.0.0',",
					"            'eslint-plugin-import': '2.26.0',",
					"            'eslint-plugin-n': '15.6.0',",
					"            'eslint-plugin-promise': '6.1.1'",
					"        ]).configFile('.eslintrc.js')",
					"    }",
					"}");
			setFile("test.js").toResource("npm/eslint/javascript/styleguide/standard/javascript-es6.dirty");
			gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
			assertFile("test.js").sameAsResource("npm/eslint/javascript/styleguide/standard/javascript-es6.clean");
		}

	}

	@NpmTest
	@Nested
	class EslintPopularJsStyleGuideTests extends GradleIntegrationHarness {
		@ParameterizedTest(name = "{index}: eslint can be applied using styleguide {0}")
		@ValueSource(strings = {"airbnb", "google", "standard", "xo"})
		void formattingUsingStyleguide(String styleguide) throws Exception {

			final String styleguidePath = "npm/eslint/javascript/styleguide/" + styleguide + "/";

			setFile(".eslintrc.js").toResource(styleguidePath + ".eslintrc.js");
			setFile("build.gradle").toLines(
					"plugins {",
					"    id 'com.diffplug.spotless'",
					"}",
					"repositories { mavenCentral() }",
					"spotless {",
					"    javascript {",
					"        target 'test.js'",
					"        eslint(" + styleGuideMapString(styleguide) + ").configFile('.eslintrc.js')",
					"    }",
					"}");
			setFile("test.js").toResource(styleguidePath + "javascript-es6.dirty");
			gradleRunner().forwardOutput().withArguments("--info", "--stacktrace", "spotlessApply").build();
			assertFile("test.js").sameAsResource(styleguidePath + "javascript-es6.clean");
		}
	}

	@NpmTest
	@Nested
	class JavascriptPrettierTests extends GradleIntegrationHarness {
		@Test
		void supportsPrettierFormattingForJavascript() throws IOException {
			setFile("build.gradle").toLines(
					"plugins {",
					"    id 'com.diffplug.spotless'",
					"}",
					"repositories { mavenCentral() }",
					"spotless {",
					"    javascript {",
					"        target 'test.js'",
					"        prettier()",
					"    }",
					"}");
			setFile("test.js").toResource("npm/prettier/filetypes/javascript-es6/javascript-es6.dirty");
			gradleRunner().withArguments("--stacktrace", "spotlessApply").build();
			assertFile("test.js").sameAsResource("npm/prettier/filetypes/javascript-es6/javascript-es6.clean");
		}
	}

}
