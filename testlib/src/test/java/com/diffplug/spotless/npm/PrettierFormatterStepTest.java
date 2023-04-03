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
package com.diffplug.spotless.npm;

import java.io.File;
import java.util.Collections;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.diffplug.common.collect.ImmutableMap;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.StepHarnessWithFile;
import com.diffplug.spotless.TestProvisioner;
import com.diffplug.spotless.tag.NpmTest;

@NpmTest
class PrettierFormatterStepTest extends ResourceHarness {

	@NpmTest
	@Nested
	class PrettierFormattingOfFileTypesIsWorking extends NpmFormatterStepCommonTests {

		@ParameterizedTest(name = "{index}: prettier can be applied to {0}")
		@ValueSource(strings = {"html", "typescript", "json", "javascript-es5", "javascript-es6", "css", "scss", "markdown", "yaml"})
		void formattingUsingConfigFile(String fileType) throws Exception {
			var filedir = "npm/prettier/filetypes/" + fileType + "/";

			final File prettierRc = createTestFile(filedir + ".prettierrc.yml");
			final var dirtyFile = filedir + fileType + ".dirty";
			final var cleanFile = filedir + fileType + ".clean";

			final FormatterStep formatterStep = PrettierFormatterStep.create(
					PrettierFormatterStep.defaultDevDependencies(),
					TestProvisioner.mavenCentral(),
					projectDir(),
					buildDir(),
					null,
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
			var filedir = "npm/prettier/filetypes/json/";

			final var dirtyFile = filedir + "json.dirty";
			final var cleanFile = filedir + "json.clean";

			final FormatterStep formatterStep = PrettierFormatterStep.create(
					PrettierFormatterStep.defaultDevDependencies(),
					TestProvisioner.mavenCentral(),
					projectDir(),
					buildDir(),
					null,
					npmPathResolver(),
					new PrettierConfig(null, ImmutableMap.of("filepath", "anyname.json"))); // should select parser based on this name

			try (StepHarness stepHarness = StepHarness.forStep(formatterStep)) {
				stepHarness.testResource(dirtyFile, cleanFile);
			}
		}

		@Test
		void parserInferenceBasedOnFilenameIsWorking() throws Exception {
			var filedir = "npm/prettier/filename/";

			final var dirtyFile = filedir + "dirty.json";
			final var cleanFile = filedir + "clean.json";

			final FormatterStep formatterStep = PrettierFormatterStep.create(
					PrettierFormatterStep.defaultDevDependencies(),
					TestProvisioner.mavenCentral(),
					projectDir(),
					buildDir(),
					null,
					npmPathResolver(),
					new PrettierConfig(null, Collections.emptyMap()));

			try (StepHarnessWithFile stepHarness = StepHarnessWithFile.forStep(this, formatterStep)) {
				stepHarness.testResource("test.json", dirtyFile, cleanFile);
			}
		}

		@Test
		void verifyPrettierErrorMessageIsRelayed() throws Exception {
			FormatterStep formatterStep = PrettierFormatterStep.create(
					PrettierFormatterStep.defaultDevDependenciesWithPrettier("2.0.5"),
					TestProvisioner.mavenCentral(),
					projectDir(),
					buildDir(),
					null,
					npmPathResolver(),
					new PrettierConfig(null, ImmutableMap.of("parser", "postcss")));
			try (StepHarnessWithFile stepHarness = StepHarnessWithFile.forStep(this, formatterStep)) {
				stepHarness.testResourceExceptionMsg("npm/prettier/filetypes/scss/scss.dirty").isEqualTo(
						"Unexpected response status code at /prettier/format [HTTP 500] -- (Error while formatting: Error: Couldn't resolve parser \"postcss\")");
			}
		}
	}

	@NpmTest
	@Nested
	class PrettierFormattingOptionsAreWorking extends NpmFormatterStepCommonTests {

		private static final String FILEDIR = "npm/prettier/config/";

		void runFormatTest(PrettierConfig config, String cleanFileNameSuffix) throws Exception {

			final var dirtyFile = FILEDIR + "typescript.dirty";
			final var cleanFile = FILEDIR + "typescript." + cleanFileNameSuffix + ".clean";

			final FormatterStep formatterStep = PrettierFormatterStep.create(
					PrettierFormatterStep.defaultDevDependencies(),
					TestProvisioner.mavenCentral(),
					projectDir(),
					buildDir(),
					null,
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

	}
}
