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

import static java.util.Collections.emptyMap;

import com.diffplug.common.collect.ImmutableMap;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.SerializableEqualityTester;
import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.StepHarnessWithFile;
import com.diffplug.spotless.TestProvisioner;
import com.diffplug.spotless.tag.NpmTest;
import java.io.File;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@NpmTest
class PrettierFormatterStepTest extends NpmFormatterStepCommonTests {

	private static final String PRETTIER_VERSION_2 = PrettierFormatterStep.DEFAULT_VERSION;

	private static final String PRETTIER_VERSION_3 = "3.0.0";

	@NpmTest
	@Nested
	class PrettierFormattingOfFileTypesIsWorking extends NpmFormatterStepCommonTests {

		@ParameterizedTest(name = "{index}: prettier 2.x can be applied to {0}")
		@ValueSource(strings = {"html", "typescript", "json", "javascript-es5", "javascript-es6", "css", "scss", "markdown", "yaml"})
		void formattingUsingPrettier2WithConfigFile(String fileType) throws Exception {
			runTestUsingPrettier(fileType, PrettierFormatterStep.defaultDevDependencies());
		}

		@ParameterizedTest(name = "{index}: prettier 3.x can be applied to {0}")
		@ValueSource(strings = {"html_prettier3", "typescript", "json", "javascript-es5", "javascript-es6", "css", "scss", "markdown", "yaml"})
		void formattingUsingPrettier3WithConfigFile(String fileType) throws Exception {
			runTestUsingPrettier(fileType, ImmutableMap.of("prettier", "3.0.0"));
		}

		private void runTestUsingPrettier(String fileType, Map<String, String> dependencies) throws Exception {
			String filedir = "npm/prettier/filetypes/" + fileType + "/";

			final File prettierRc = createTestFile(filedir + ".prettierrc.yml");
			final String dirtyFile = filedir + fileType + ".dirty";
			final String cleanFile = filedir + fileType + ".clean";

			final FormatterStep formatterStep = PrettierFormatterStep.create(
					dependencies,
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

		@ParameterizedTest(name = "{index}: parser inference based on explicit filepath is working with prettier {0}")
		@ValueSource(strings = {PRETTIER_VERSION_2, PRETTIER_VERSION_3})
		void parserInferenceBasedOnExplicitFilepathIsWorking(String prettierVersion) throws Exception {
			String filedir = "npm/prettier/filetypes/json/";

			final String dirtyFile = filedir + "json.dirty";
			final String cleanFile = filedir + "json.clean";

			final FormatterStep formatterStep = PrettierFormatterStep.create(
					ImmutableMap.of("prettier", prettierVersion),
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

		@ParameterizedTest(name = "{index}: parser inference based on filename is working with prettier {0}")
		@ValueSource(strings = {PRETTIER_VERSION_2, PRETTIER_VERSION_3})
		void parserInferenceBasedOnFilenameIsWorking(String prettierVersion) throws Exception {
			String filedir = "npm/prettier/filename/";

			final String dirtyFile = filedir + "dirty.json";
			final String cleanFile = filedir + "clean.json";

			final FormatterStep formatterStep = PrettierFormatterStep.create(
					ImmutableMap.of("prettier", prettierVersion),
					TestProvisioner.mavenCentral(),
					projectDir(),
					buildDir(),
					null,
					npmPathResolver(),
					new PrettierConfig(null, emptyMap()));

			try (StepHarnessWithFile stepHarness = StepHarnessWithFile.forStep(this, formatterStep)) {
				stepHarness.testResource("test.json", dirtyFile, cleanFile);
			}
		}

		@Test
		void verifyPrettierErrorMessageIsRelayed() throws Exception {
			FormatterStep formatterStep = PrettierFormatterStep.create(
					PrettierFormatterStep.defaultDevDependenciesWithPrettier("2.8.8"),
					TestProvisioner.mavenCentral(),
					projectDir(),
					buildDir(),
					null,
					npmPathResolver(),
					new PrettierConfig(null, ImmutableMap.of("parser", "postcss")));
			try (StepHarnessWithFile stepHarness = StepHarnessWithFile.forStep(this, formatterStep)) {
				stepHarness.expectLintsOfResource("npm/prettier/filetypes/scss/scss.dirty")
						.toBe("LINE_UNDEFINED prettier-format(com.diffplug.spotless.npm.SimpleRestClient$SimpleRestResponseException) Unexpected response status code at /prettier/format [HTTP 500] -- (Error while formatting: Error: Couldn't resolve parser \"postcss\") (...)");
			}
		}
	}

	@NpmTest
	@Nested
	class PrettierFormattingOptionsAreWorking extends NpmFormatterStepCommonTests {

		private static final String FILEDIR = "npm/prettier/config/";

		void runFormatTest(String prettierVersion, PrettierConfig config, String cleanFileNameSuffix) throws Exception {

			final String dirtyFile = FILEDIR + "typescript.dirty";
			final String cleanFile = FILEDIR + "typescript." + cleanFileNameSuffix + ".clean";

			final FormatterStep formatterStep = PrettierFormatterStep.create(
					ImmutableMap.of("prettier", prettierVersion),
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

		@ParameterizedTest(name = "{index}: defaults are applied with prettier {0}")
		@ValueSource(strings = {PRETTIER_VERSION_2, PRETTIER_VERSION_3})
		void defaultsAreApplied(String prettierVersion) throws Exception {
			runFormatTest(prettierVersion, new PrettierConfig(null, ImmutableMap.of("parser", "typescript")), "defaults_prettier_" + major(prettierVersion));
		}

		@ParameterizedTest(name = "{index}: config file options are applied with prettier {0}")
		@ValueSource(strings = {PRETTIER_VERSION_2, PRETTIER_VERSION_3})
		void configFileOptionsAreApplied(String prettierVersion) throws Exception {
			runFormatTest(prettierVersion, new PrettierConfig(createTestFile(FILEDIR + ".prettierrc.yml"), null), "configfile_prettier_" + major(prettierVersion));
		}

		@ParameterizedTest(name = "{index}: config file options can be overriden with prettier {0}")
		@ValueSource(strings = {PRETTIER_VERSION_2, PRETTIER_VERSION_3})
		void configFileOptionsCanBeOverriden(String prettierVersion) throws Exception {
			runFormatTest(prettierVersion, new PrettierConfig(createTestFile(FILEDIR + ".prettierrc.yml"), ImmutableMap.of("printWidth", 300)), "override_prettier_" + major(prettierVersion));
		}

		private String major(String semVer) {
			return semVer.split("\\.")[0];
		}
	}

	@Test
	void equality() {
		new SerializableEqualityTester() {
			String prettierVersion = "3.0.0";
			PrettierConfig config = new PrettierConfig(null, Map.of("parser", "typescript"));

			@Override
			protected void setupTest(API api) {
				// same version == same
				api.areDifferentThan();
				// change the groupArtifact, and it's different
				prettierVersion = "2.8.8";
				api.areDifferentThan();
				config = new PrettierConfig(null, Map.of("parser", "css"));
				api.areDifferentThan();
			}

			@Override
			protected FormatterStep create() {
				return PrettierFormatterStep.create(
						ImmutableMap.of("prettier", prettierVersion),
						TestProvisioner.mavenCentral(),
						projectDir(),
						buildDir(),
						null,
						npmPathResolver(),
						config); // should select parser based on this name
			}
		}.testEquals();
	}
}
