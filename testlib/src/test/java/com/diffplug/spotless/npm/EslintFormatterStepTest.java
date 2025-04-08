/*
 * Copyright 2016-2024 DiffPlug
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
package com.diffplug.spotless.npm;

import java.io.File;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.diffplug.common.collect.ImmutableMap;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.StepHarnessWithFile;
import com.diffplug.spotless.TestProvisioner;
import com.diffplug.spotless.tag.NpmTest;

@NpmTest
class EslintFormatterStepTest {

	@NpmTest
	@Nested
	class EslintJavascriptFormattingStepTest extends NpmFormatterStepCommonTests {

		private final Map<String, Map<String, String>> devDependenciesForRuleset = ImmutableMap.of(
				"custom_rules", EslintFormatterStep.defaultDevDependenciesForTypescript(),
				"styleguide/airbnb", EslintStyleGuide.JS_AIRBNB.mergedWith(EslintFormatterStep.defaultDevDependencies()),
				"styleguide/google", EslintStyleGuide.JS_GOOGLE.mergedWith(EslintFormatterStep.defaultDevDependencies()),
				"styleguide/standard", EslintStyleGuide.JS_STANDARD.mergedWith(EslintFormatterStep.defaultDevDependencies()));

		@ParameterizedTest(name = "{index}: eslint can be applied using ruleset {0}")
		@ValueSource(strings = {"custom_rules", "styleguide/airbnb", "styleguide/google", "styleguide/standard"})
		void formattingUsingRulesetsFile(String ruleSetName) throws Exception {
			String filedir = "npm/eslint/javascript/" + ruleSetName + "/";

			String testDir = "formatting_ruleset_" + ruleSetName.replace('/', '_') + "/";
			//			File testDirFile = newFolder(testDir);

			final File eslintRc = createTestFile(filedir + ".eslintrc.js");
			//			final File eslintRc = setFile(buildDir().getPath() + "/.eslintrc.js").toResource(filedir + ".eslintrc.js");

			final String dirtyFile = filedir + "javascript-es6.dirty";
			final String cleanFile = filedir + "javascript-es6.clean";

			final FormatterStep formatterStep = EslintFormatterStep.create(
					devDependenciesForRuleset.get(ruleSetName),
					TestProvisioner.mavenCentral(),
					projectDir(),
					buildDir(),
					null,
					npmPathResolver(),
					new EslintConfig(eslintRc, null));

			try (StepHarnessWithFile stepHarness = StepHarnessWithFile.forStep(this, formatterStep)) {
				stepHarness.test("test.js", ResourceHarness.getTestResource(dirtyFile), ResourceHarness.getTestResource(cleanFile));
			}
		}
	}

	@NpmTest
	@Nested
	class EslintTypescriptFormattingStepTest extends NpmFormatterStepCommonTests {

		private final Map<String, Map<String, String>> devDependenciesForRuleset = ImmutableMap.of(
				"custom_rules", EslintFormatterStep.defaultDevDependenciesForTypescript(),
				"styleguide/standard_with_typescript", EslintStyleGuide.TS_STANDARD_WITH_TYPESCRIPT.mergedWith(EslintFormatterStep.defaultDevDependenciesForTypescript()));

		@ParameterizedTest(name = "{index}: eslint can be applied using ruleset {0}")
		@ValueSource(strings = {"custom_rules", "styleguide/standard_with_typescript"})
		void formattingUsingRulesetsFile(String ruleSetName) throws Exception {
			String filedir = "npm/eslint/typescript/" + ruleSetName + "/";

			String testDir = "formatting_ruleset_" + ruleSetName.replace('/', '_') + "/";
			//			File testDirFile = newFolder(testDir);

			final File eslintRc = createTestFile(filedir + ".eslintrc.js");
			//			final File eslintRc = setFile(buildDir().getPath() + "/.eslintrc.js").toResource(filedir + ".eslintrc.js");

			//setFile(testDir + "/test.ts").toResource(filedir + "typescript.dirty");
			File tsconfigFile = null;
			if (existsTestResource(filedir + "tsconfig.json")) {
				tsconfigFile = setFile(testDir + "tsconfig.json").toResource(filedir + "tsconfig.json");
			}
			final String dirtyFile = filedir + "typescript.dirty";
			final String cleanFile = filedir + "typescript.clean";

			final FormatterStep formatterStep = EslintFormatterStep.create(
					devDependenciesForRuleset.get(ruleSetName),
					TestProvisioner.mavenCentral(),
					projectDir(),
					buildDir(),
					null,
					npmPathResolver(),
					new EslintTypescriptConfig(eslintRc, null, tsconfigFile));

			try (StepHarnessWithFile stepHarness = StepHarnessWithFile.forStep(this, formatterStep)) {
				stepHarness.test(testDir + "test.ts", ResourceHarness.getTestResource(dirtyFile), ResourceHarness.getTestResource(cleanFile));
			}
		}
	}

	@NpmTest
	@Nested
	class EslintInlineConfigTypescriptFormattingStepTest extends NpmFormatterStepCommonTests {

		@Test
		void formattingUsingInlineXoConfig() throws Exception {
			String filedir = "npm/eslint/typescript/styleguide/xo/";

			String testDir = "formatting_ruleset_xo_inline_config/";

			final String esLintConfig = String.join("\n",
					"{",
					"	env: {",
					"		browser: true,",
					"		es2021: true,",
					"	},",
					"	extends: 'xo/browser',",
					"	overrides: [",
					"		{",
					"			extends: [",
					"				'xo-typescript',",
					"			],",
					"			files: [",
					"				'*.ts',",
					"				'*.tsx',",
					"			],",
					"		},",
					"	],",
					"	parser: '@typescript-eslint/parser',",
					"	parserOptions: {",
					"		ecmaVersion: 'latest',",
					"		sourceType: 'module',",
					"		project: './tsconfig.json',",
					"	},",
					"	rules: {",
					"	},",
					"}");

			File tsconfigFile = setFile(testDir + "tsconfig.json").toResource(filedir + "tsconfig.json");
			final String dirtyFile = filedir + "typescript.dirty";
			final String cleanFile = filedir + "typescript.clean";

			final FormatterStep formatterStep = EslintFormatterStep.create(
					EslintStyleGuide.TS_XO_TYPESCRIPT.mergedWith(EslintFormatterStep.defaultDevDependenciesForTypescript()),
					TestProvisioner.mavenCentral(),
					projectDir(),
					buildDir(),
					null,
					npmPathResolver(),
					new EslintTypescriptConfig(null, esLintConfig, tsconfigFile));

			try (StepHarnessWithFile stepHarness = StepHarnessWithFile.forStep(this, formatterStep)) {
				stepHarness.test(testDir + "test.ts", ResourceHarness.getTestResource(dirtyFile), ResourceHarness.getTestResource(cleanFile));
			}
		}
	}
}
