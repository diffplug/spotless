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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.diffplug.common.collect.ImmutableMap;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.TestProvisioner;
import com.diffplug.spotless.tag.NpmTest;

@NpmTest
class TsFmtFormatterStepTest {

	@NpmTest
	@Nested
	class TsFmtUsingVariousFormattingFilesTest extends NpmFormatterStepCommonTests {

		@ParameterizedTest(name = "{index}: formatting using {0} is working")
		@ValueSource(strings = {"vscode/vscode.json", "tslint/tslint.json", "tsfmt/tsfmt.json", "tsconfig/tsconfig.json"})
		void formattingUsingConfigFile(String formattingConfigFile) throws Exception {
			var configFileName = formattingConfigFile.substring(formattingConfigFile.lastIndexOf('/') >= 0 ? formattingConfigFile.lastIndexOf('/') + 1 : 0);
			var configFileNameWithoutExtension = configFileName.substring(0, configFileName.lastIndexOf('.'));
			var filedir = "npm/tsfmt/" + configFileNameWithoutExtension + "/";

			final File configFile = createTestFile(filedir + configFileName);
			final var dirtyFile = filedir + configFileNameWithoutExtension + ".dirty";
			final var cleanFile = filedir + configFileNameWithoutExtension + ".clean";

			// some config options expect to see at least one file in the baseDir, so let's write one there
			var srcDir = new File(rootFolder(), "src/main/typescript");
			Files.createDirectories(srcDir.toPath());
			Files.write(new File(srcDir, configFileNameWithoutExtension + ".ts").toPath(), getTestResource(dirtyFile).getBytes(StandardCharsets.UTF_8));

			final FormatterStep formatterStep = TsFmtFormatterStep.create(
					TsFmtFormatterStep.defaultDevDependencies(),
					TestProvisioner.mavenCentral(),
					projectDir(),
					buildDir(),
					null,
					npmPathResolver(),
					TypedTsFmtConfigFile.named(configFileNameWithoutExtension, configFile),
					Collections.emptyMap());

			try (StepHarness stepHarness = StepHarness.forStep(formatterStep)) {
				stepHarness.testResource(dirtyFile, cleanFile);
			}
		}
	}

	@NpmTest
	@Nested
	class TsFmtUsingInlineConfigTest extends NpmFormatterStepCommonTests {
		@Test
		void formattingUsingInlineConfigWorks() throws Exception {

			final ImmutableMap<String, Object> inlineConfig = ImmutableMap.of("indentSize", 1, "convertTabsToSpaces", true);

			final FormatterStep formatterStep = TsFmtFormatterStep.create(
					TsFmtFormatterStep.defaultDevDependencies(),
					TestProvisioner.mavenCentral(),
					projectDir(),
					buildDir(),
					null,
					npmPathResolver(),
					null,
					inlineConfig);

			try (StepHarness stepHarness = StepHarness.forStep(formatterStep)) {
				stepHarness.testResource("npm/tsfmt/tsfmt/tsfmt.dirty", "npm/tsfmt/tsfmt/tsfmt.clean");
			}
		}
	}
}
