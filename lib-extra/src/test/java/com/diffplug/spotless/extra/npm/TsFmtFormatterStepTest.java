/*
 * Copyright 2016 DiffPlug
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
package com.diffplug.spotless.extra.npm;

import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.diffplug.common.collect.ImmutableMap;
import com.diffplug.spotless.*;
import com.diffplug.spotless.category.NpmTest;

@Category(NpmTest.class)
@RunWith(Enclosed.class)
public class TsFmtFormatterStepTest {

	@Category(NpmTest.class)
	@RunWith(Parameterized.class)
	public static class TsFmtUsingVariousFormattingFilesTest extends NpmFormatterStepCommonTests {
		@Parameterized.Parameter
		public String formattingConfigFile;

		@Parameterized.Parameters(name = "{index}: formatting using {0} is working")
		public static Iterable<String> formattingConfigFiles() {
			return Arrays.asList("vscode/vscode.json", "tslint/tslint.json", "tsfmt/tsfmt.json", "tsconfig/tsconfig.json");
		}

		@Test
		public void formattingUsingConfigFile() throws Exception {
			String configFileName = formattingConfigFile.substring(formattingConfigFile.lastIndexOf('/') >= 0 ? formattingConfigFile.lastIndexOf('/') + 1 : 0);
			String configFileNameWithoutExtension = configFileName.substring(0, configFileName.lastIndexOf('.'));
			String filedir = "npm/tsfmt/" + configFileNameWithoutExtension + "/";

			final File configFile = createTestFile(filedir + configFileName);
			final String dirtyFile = filedir + configFileNameWithoutExtension + ".dirty";
			final String cleanFile = filedir + configFileNameWithoutExtension + ".clean";

			// some config options expect to see at least one file in the baseDir, so let's write one there
			Files.write(new File(configFile.getParentFile(), configFileNameWithoutExtension + ".ts").toPath(), getTestResource(dirtyFile).getBytes(StandardCharsets.UTF_8));

			final FormatterStep formatterStep = TsFmtFormatterStep.create(
					TestProvisioner.mavenCentral(),
					buildDir(),
					npmExecutable(),
					ImmutableMap.<String, Object> builder()
							.put("basedir", configFile.getParent())
							.put(configFileNameWithoutExtension, Boolean.TRUE)
							.put(configFileNameWithoutExtension + "File", configFile.getPath())
							.build(),
					Collections.emptyMap());

			try (StepHarness stepHarness = StepHarness.forStep(formatterStep)) {
				stepHarness.testResource(dirtyFile, cleanFile);
			}
		}
	}

	@Category(NpmTest.class)
	public static class TsFmtUsingInlineConfigTest extends NpmFormatterStepCommonTests {
		@Test
		public void formattingUsingInlineConfigWorks() throws Exception {

			final ImmutableMap<String, Object> inlineConfig = ImmutableMap.of("indentSize", 1, "convertTabsToSpaces", true);

			final FormatterStep formatterStep = TsFmtFormatterStep.create(
					TestProvisioner.mavenCentral(),
					buildDir(),
					npmExecutable(),
					ImmutableMap.<String, Object> builder()
							.put("basedir", buildDir().getAbsolutePath())
							.build(),
					inlineConfig);

			try (StepHarness stepHarness = StepHarness.forStep(formatterStep)) {
				stepHarness.testResource("npm/tsfmt/tsfmt/tsfmt.dirty", "npm/tsfmt/tsfmt/tsfmt.clean");
			}
		}
	}

	@Category(NpmTest.class)
	@RunWith(Parameterized.class)
	public static class TsFmtBlacklistedOptionsTest extends NpmFormatterStepCommonTests {
		@Parameterized.Parameter
		public String blackListedOption;

		@Parameterized.Parameters(name = "{index}: config option '{0}' is blacklisted")
		public static Iterable<String> blacklistedOption() {
			return Arrays.asList("dryRun", "replace", "verify");
		}

		@Test(expected = BlacklistedOptionException.class)
		public void blacklistedOptionIsThrown() throws Exception {
			TsFmtFormatterStep.create(
					TestProvisioner.mavenCentral(),
					buildDir(),
					npmExecutable(),
					ImmutableMap.<String, Object> builder()
							.put(blackListedOption, Boolean.TRUE)
							.build(),
					Collections.emptyMap());

			fail("should never be reached!");

		}

	}

}
