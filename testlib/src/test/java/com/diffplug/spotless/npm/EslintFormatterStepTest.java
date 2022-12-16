/*
 * Copyright 2016-2021 DiffPlug
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

import com.diffplug.common.collect.ImmutableMap;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.StepHarnessWithFile;
import com.diffplug.spotless.TestProvisioner;
import com.diffplug.spotless.tag.NpmTest;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

@NpmTest
class EslintFormatterStepTest {

	private final Map<String, String> combine(Map<String, String> m1, Map<String, String> m2) {
		Map<String, String> combined = new TreeMap<>(m1);
		combined.putAll(m2);
		return combined;
	}

	@NpmTest
	@Nested
	class EslintTypescriptFormattingStepTest extends NpmFormatterStepCommonTests {

		private final Map<String, Map<String, String>> devDependenciesForRuleset = ImmutableMap.of(
			"custom_rules", EslintFormatterStep.defaultDevDependenciesForTypescript(),
			"standard_rules_standard_with_typescript", combine(EslintFormatterStep.defaultDevDependenciesForTypescript(), EslintFormatterStep.PopularStyleGuide.STANDARD_WITH_TYPESCRIPT.devDependencies()),
				"standard_rules_xo", combine(EslintFormatterStep.defaultDevDependenciesForTypescript(), EslintFormatterStep.PopularStyleGuide.XO_TYPESCRIPT.devDependencies())
				);



		@ParameterizedTest(name = "{index}: eslint can be applied using ruleset {0}")
		@ValueSource(strings = {"custom_rules", "standard_rules_standard_with_typescript", "standard_rules_xo"})
		void formattingUsingRulesetsFile(String ruleSetName) throws Exception {
			String filedir = "npm/eslint/typescript/" + ruleSetName + "/";

			String testDir = "formatting_ruleset_" + ruleSetName + "/";
//			File testDirFile = newFolder(testDir);

			final File eslintRc = createTestFile(filedir + ".eslintrc.js");
//			final File eslintRc = setFile(buildDir().getPath() + "/.eslintrc.js").toResource(filedir + ".eslintrc.js");

			//setFile(testDir + "/test.ts").toResource(filedir + "typescript.dirty");
			File tsconfigFile = null;
			if (existsTestResource(filedir + "tsconfig.json")) {
				tsconfigFile = setFile(testDir + "tsconfig.json").toResource(filedir + "tsconfig.json");
			}
			final String dirtyFile = filedir + "typescript.dirty";
			File dirtyFileFile = setFile(testDir + "test.ts").toResource(dirtyFile);
			final String cleanFile = filedir + "typescript.clean";

			final FormatterStep formatterStep = EslintFormatterStep.create(
				devDependenciesForRuleset.get(ruleSetName),
				TestProvisioner.mavenCentral(),
				projectDir(),
				buildDir(),
				npmPathResolver(),
				new EslintTypescriptConfig(eslintRc, null, tsconfigFile));

			try (StepHarnessWithFile stepHarness = StepHarnessWithFile.forStep(formatterStep)) {
				stepHarness.testResource(dirtyFileFile, dirtyFile, cleanFile);
			}
		}
	}
/*
	@NpmTest
	@Nested
	class PrettierFormattingOfFileTypesIsWorking extends NpmFormatterStepCommonTests {

		@ParameterizedTest(name = "{index}: prettier can be applied to {0}")
		@ValueSource(strings = {"html", "typescript", "json", "javascript-es5", "javascript-es6", "css", "scss", "markdown", "yaml"})
		void formattingUsingConfigFile(String fileType) throws Exception {
			String filedir = "npm/prettier/filetypes/" + fileType + "/";

			final File prettierRc = createTestFile(filedir + ".prettierrc.yml");
			final String dirtyFile = filedir + fileType + ".dirty";
			final String cleanFile = filedir + fileType + ".clean";

			final FormatterStep formatterStep = PrettierFormatterStep.create(
					PrettierFormatterStep.defaultDevDependencies(),
					TestProvisioner.mavenCentral(),
					buildDir(),
					npmPathResolver(),
					new PrettierConfig(prettierRc, null));

			try (StepHarness stepHarness = StepHarness.forStep(formatterStep)) {
				stepHarness.testResource(dirtyFile, cleanFile);
			}
		}
	}

	@NpmTest
	@Nested
	class SpecificPrettierFormatterStepTests extends NpmFormatterStepCommonTests {

		@Test
		void parserInferenceBasedOnExplicitFilepathIsWorking() throws Exception {
			String filedir = "npm/prettier/filetypes/json/";

			final String dirtyFile = filedir + "json.dirty";
			final String cleanFile = filedir + "json.clean";

			final FormatterStep formatterStep = PrettierFormatterStep.create(
					PrettierFormatterStep.defaultDevDependencies(),
					TestProvisioner.mavenCentral(),
					buildDir(),
					npmPathResolver(),
					new PrettierConfig(null, ImmutableMap.of("filepath", "anyname.json"))); // should select parser based on this name

			try (StepHarness stepHarness = StepHarness.forStep(formatterStep)) {
				stepHarness.testResource(dirtyFile, cleanFile);
			}
		}

		@Test
		void parserInferenceBasedOnFilenameIsWorking() throws Exception {
			String filedir = "npm/prettier/filename/";

			final String dirtyFile = filedir + "dirty.json";
			final String cleanFile = filedir + "clean.json";

			final FormatterStep formatterStep = PrettierFormatterStep.create(
					PrettierFormatterStep.defaultDevDependencies(),
					TestProvisioner.mavenCentral(),
					buildDir(),
					npmPathResolver(),
					new PrettierConfig(null, Collections.emptyMap()));

			try (StepHarnessWithFile stepHarness = StepHarnessWithFile.forStep(formatterStep)) {
				stepHarness.testResource(new File("test.json"), dirtyFile, cleanFile);
			}
		}

		@Test
		void verifyPrettierErrorMessageIsRelayed() throws Exception {
			FormatterStep formatterStep = PrettierFormatterStep.create(
					PrettierFormatterStep.defaultDevDependenciesWithPrettier("2.0.5"),
					TestProvisioner.mavenCentral(),
					buildDir(),
					npmPathResolver(),
					new PrettierConfig(null, ImmutableMap.of("parser", "postcss")));
			try (StepHarness stepHarness = StepHarness.forStep(formatterStep)) {
				stepHarness.testResourceException("npm/prettier/filetypes/scss/scss.dirty", exception -> {
					exception.hasMessageContaining("HTTP 501");
					exception.hasMessageContaining("Couldn't resolve parser \"postcss\"");
				});
			}
		}
	}

	@NpmTest
	@Nested
	class PrettierFormattingOptionsAreWorking extends NpmFormatterStepCommonTests {

		private static final String FILEDIR = "npm/prettier/config/";

		void runFormatTest(PrettierConfig config, String cleanFileNameSuffix) throws Exception {

			final String dirtyFile = FILEDIR + "typescript.dirty";
			final String cleanFile = FILEDIR + "typescript." + cleanFileNameSuffix + ".clean";

			final FormatterStep formatterStep = PrettierFormatterStep.create(
					PrettierFormatterStep.defaultDevDependencies(),
					TestProvisioner.mavenCentral(),
					buildDir(),
					npmPathResolver(),
					config); // should select parser based on this name

			try (StepHarness stepHarness = StepHarness.forStep(formatterStep)) {
				stepHarness.testResource(dirtyFile, cleanFile);
			}
		}

		@Test
		void defaultsAreApplied() throws Exception {
			runFormatTest(new PrettierConfig(null, ImmutableMap.of("parser", "typescript")), "defaults");
		}

		@Test
		void configFileOptionsAreApplied() throws Exception {
			runFormatTest(new PrettierConfig(createTestFile(FILEDIR + ".prettierrc.yml"), null), "configfile");
		}

		@Test
		void configFileOptionsCanBeOverriden() throws Exception {
			runFormatTest(new PrettierConfig(createTestFile(FILEDIR + ".prettierrc.yml"), ImmutableMap.of("printWidth", 300)), "override");
		}

	}*/
}
