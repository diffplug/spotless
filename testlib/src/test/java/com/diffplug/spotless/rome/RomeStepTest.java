/*
 * Copyright 2023 DiffPlug
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
package com.diffplug.spotless.rome;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.diffplug.common.base.StandardSystemProperty;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.StepHarnessWithFile;
import com.diffplug.spotless.ThrowingEx;

class RomeStepTest extends ResourceHarness {
	private static String downloadDir;

	@BeforeAll
	static void createDownloadDir() throws IOException {
		// We do not want to download Rome each time we execute a test
		var userHome = Paths.get(StandardSystemProperty.USER_HOME.value());
		downloadDir = userHome.resolve(".gradle").resolve("rome-dl-test").toAbsolutePath().normalize().toString();
	}

	@Nested
	@Deprecated
	class Rome {
		/**
		 * Tests that files can be formatted without setting the input language
		 * explicitly.
		 */
		@Nested
		class AutoDetectLanguage {
			/**
			 * Tests that a *.cjs file can be formatted without setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectCjs() {
				var step = RomeStep.withExeDownload(BiomeFlavor.ROME, "12.0.0", downloadDir.toString()).create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("rome/js/fileBefore.cjs", "rome/js/fileAfter.cjs");
			}

			/**
			 * Tests that a *.cts file can be formatted without setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectCts() {
				var step = RomeStep.withExeDownload(BiomeFlavor.ROME, "12.0.0", downloadDir.toString()).create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("rome/ts/fileBefore.cts", "rome/ts/fileAfter.cts");
			}

			/**
			 * Tests that a *.js file can be formatted without setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectJs() {
				var step = RomeStep.withExeDownload(BiomeFlavor.ROME, "12.0.0", downloadDir.toString()).create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("rome/js/fileBefore.js", "rome/js/fileAfter.js");
			}

			/**
			 * Tests that a *.js file can be formatted without setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectJson() {
				var step = RomeStep.withExeDownload(BiomeFlavor.ROME, "12.0.0", downloadDir.toString()).create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("rome/json/fileBefore.json", "rome/json/fileAfter.json");
			}

			/**
			 * Tests that a *.jsx file can be formatted without setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectJsx() {
				var step = RomeStep.withExeDownload(BiomeFlavor.ROME, "12.0.0", downloadDir.toString()).create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("rome/js/fileBefore.jsx", "rome/js/fileAfter.jsx");
			}

			/**
			 * Tests that a *.mjs file can be formatted without setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectMjs() {
				var step = RomeStep.withExeDownload(BiomeFlavor.ROME, "12.0.0", downloadDir.toString()).create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("rome/js/fileBefore.mjs", "rome/js/fileAfter.mjs");
			}

			/**
			 * Tests that a *.mts file can be formatted without setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectMts() {
				var step = RomeStep.withExeDownload(BiomeFlavor.ROME, "12.0.0", downloadDir.toString()).create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("rome/ts/fileBefore.mts", "rome/ts/fileAfter.mts");
			}

			/**
			 * Tests that a *.ts file can be formatted without setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectTs() {
				var step = RomeStep.withExeDownload(BiomeFlavor.ROME, "12.0.0", downloadDir.toString()).create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("rome/ts/fileBefore.ts", "rome/ts/fileAfter.ts");
			}

			/**
			 * Tests that a *.tsx file can be formatted without setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectTsx() {
				var step = RomeStep.withExeDownload(BiomeFlavor.ROME, "12.0.0", downloadDir.toString()).create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("rome/ts/fileBefore.tsx", "rome/ts/fileAfter.tsx");
			}
		}

		@Nested
		class ConfigFile {
			/**
			 * Test formatting with the line width in the config file set to 120.
			 */
			@Test
			void testLineWidth120() {
				var path = createRomeConfig("rome/config/line-width-120.json");
				var step = RomeStep.withExeDownload(BiomeFlavor.ROME, "12.0.0", downloadDir.toString()).withConfigPath(path).create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("rome/js/longLineBefore.js", "rome/js/longLineAfter120.js");
			}

			/**
			 * Test formatting with the line width in the config file set to 120.
			 */
			@Test
			void testLineWidth80() {
				var path = createRomeConfig("rome/config/line-width-80.json");
				var step = RomeStep.withExeDownload(BiomeFlavor.ROME, "12.0.0", downloadDir.toString()).withConfigPath(path).create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("rome/js/longLineBefore.js", "rome/js/longLineAfter80.js");
			}

			private String createRomeConfig(String name) {
				var config = createTestFile(name).toPath();
				var dir = config.getParent();
				var rome = dir.resolve("rome.json");
				ThrowingEx.run(() -> Files.copy(config, rome));
				return dir.toString();
			}
		}

		/**
		 * Tests that files can be formatted when setting the input language explicitly.
		 */
		@Nested
		class ExplicitLanguage {
			/**
			 * Tests that a *.cjs file can be formatted when setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectCjs() {
				var step = RomeStep.withExeDownload(BiomeFlavor.ROME, "12.0.0", downloadDir.toString()).withLanguage("js").create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("rome/js/fileBefore.cjs", "rome/js/fileAfter.cjs");
			}

			/**
			 * Tests that a *.cts file can be formatted when setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectCts() {
				var step = RomeStep.withExeDownload(BiomeFlavor.ROME, "12.0.0", downloadDir.toString()).withLanguage("ts").create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("rome/ts/fileBefore.cts", "rome/ts/fileAfter.cts");
			}

			/**
			 * Tests that a *.js file can be formatted when setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectJs() {
				var step = RomeStep.withExeDownload(BiomeFlavor.ROME, "12.0.0", downloadDir.toString()).withLanguage("js").create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("rome/js/fileBefore.js", "rome/js/fileAfter.js");
			}

			/**
			 * Tests that a *.json file can be formatted when setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectJson() {
				var step = RomeStep.withExeDownload(BiomeFlavor.ROME, "12.0.0", downloadDir.toString()).withLanguage("json").create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("rome/json/fileBefore.json", "rome/json/fileAfter.json");
			}

			/**
			 * Tests that a *.jsx file can be formatted when setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectJsx() {
				var step = RomeStep.withExeDownload(BiomeFlavor.ROME, "12.0.0", downloadDir.toString()).withLanguage("jsx").create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("rome/js/fileBefore.jsx", "rome/js/fileAfter.jsx");
			}

			/**
			 * Tests that a *.mjs file can be formatted without setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectMjs() {
				var step = RomeStep.withExeDownload(BiomeFlavor.ROME, "12.0.0", downloadDir.toString()).withLanguage("js").create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("rome/js/fileBefore.mjs", "rome/js/fileAfter.mjs");
			}

			/**
			 * Tests that a *.mts file can be formatted when setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectMts() {
				var step = RomeStep.withExeDownload(BiomeFlavor.ROME, "12.0.0", downloadDir.toString()).withLanguage("ts").create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("rome/ts/fileBefore.mts", "rome/ts/fileAfter.mts");
			}

			/**
			 * Tests that a *.ts file can be formatted when setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectTs() {
				var step = RomeStep.withExeDownload(BiomeFlavor.ROME, "12.0.0", downloadDir.toString()).withLanguage("ts").create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("rome/ts/fileBefore.ts", "rome/ts/fileAfter.ts");
			}

			/**
			 * Tests that a *.tsx file can be formatted when setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectTsx() {
				var step = RomeStep.withExeDownload(BiomeFlavor.ROME, "12.0.0", downloadDir.toString()).withLanguage("tsx").create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("rome/ts/fileBefore.tsx", "rome/ts/fileAfter.tsx");
			}
		}
	}

	@Nested
	class Biome {
		/**
		 * Tests that files can be formatted without setting the input language
		 * explicitly.
		 */
		@Nested
		class AutoDetectLanguage {
			/**
			 * Tests that a *.cjs file can be formatted without setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectCjs() {
				var step = RomeStep.withExeDownload(BiomeFlavor.BIOME, "1.2.0", downloadDir.toString()).create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("biome/js/fileBefore.cjs", "biome/js/fileAfter.cjs");
			}

			/**
			 * Tests that a *.cts file can be formatted without setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectCts() {
				var step = RomeStep.withExeDownload(BiomeFlavor.BIOME, "1.2.0", downloadDir.toString()).create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("biome/ts/fileBefore.cts", "biome/ts/fileAfter.cts");
			}

			/**
			 * Tests that a *.js file can be formatted without setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectJs() {
				var step = RomeStep.withExeDownload(BiomeFlavor.BIOME, "1.2.0", downloadDir.toString()).create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("biome/js/fileBefore.js", "biome/js/fileAfter.js");
			}

			/**
			 * Tests that a *.js file can be formatted without setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectJson() {
				var step = RomeStep.withExeDownload(BiomeFlavor.BIOME, "1.2.0", downloadDir.toString()).create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("biome/json/fileBefore.json", "biome/json/fileAfter.json");
			}

			/**
			 * Tests that a *.jsx file can be formatted without setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectJsx() {
				var step = RomeStep.withExeDownload(BiomeFlavor.BIOME, "1.2.0", downloadDir.toString()).create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("biome/js/fileBefore.jsx", "biome/js/fileAfter.jsx");
			}

			/**
			 * Tests that a *.mjs file can be formatted without setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectMjs() {
				var step = RomeStep.withExeDownload(BiomeFlavor.BIOME, "1.2.0", downloadDir.toString()).create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("biome/js/fileBefore.mjs", "biome/js/fileAfter.mjs");
			}

			/**
			 * Tests that a *.mts file can be formatted without setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectMts() {
				var step = RomeStep.withExeDownload(BiomeFlavor.BIOME, "1.2.0", downloadDir.toString()).create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("biome/ts/fileBefore.mts", "biome/ts/fileAfter.mts");
			}

			/**
			 * Tests that a *.ts file can be formatted without setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectTs() {
				var step = RomeStep.withExeDownload(BiomeFlavor.BIOME, "1.2.0", downloadDir.toString()).create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("biome/ts/fileBefore.ts", "biome/ts/fileAfter.ts");
			}

			/**
			 * Tests that a *.tsx file can be formatted without setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectTsx() {
				var step = RomeStep.withExeDownload(BiomeFlavor.BIOME, "1.2.0", downloadDir.toString()).create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("biome/ts/fileBefore.tsx", "biome/ts/fileAfter.tsx");
			}

			/**
			 * Biome is hard-coded to ignore certain files, such as package.json. Since version 1.5.0,
			 * the biome CLI does not output any formatted code anymore, whereas previously it printed
			 * the input as-is. This tests checks that when the biome formatter outputs an empty string,
			 * the contents of the file to format are used instead.
			 */
			@Test
			void preservesIgnoredFiles() {
				var step = RomeStep.withExeDownload(BiomeFlavor.BIOME, "1.5.0", downloadDir.toString()).create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("biome/json/package.json", "biome/json/packageAfter.json");
			}
		}

		@Nested
		class ConfigFile {
			/**
			 * Test formatting with the line width in the config file set to 120.
			 */
			@Test
			void testLineWidth120() {
				var path = createBiomeConfig("biome/config/line-width-120.json");
				var step = RomeStep.withExeDownload(BiomeFlavor.BIOME, "1.2.0", downloadDir.toString()).withConfigPath(path).create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("biome/js/longLineBefore.js", "biome/js/longLineAfter120.js");
			}

			/**
			 * Test formatting with the line width in the config file set to 120.
			 */
			@Test
			void testLineWidth80() {
				var path = createBiomeConfig("biome/config/line-width-80.json");
				var step = RomeStep.withExeDownload(BiomeFlavor.BIOME, "1.2.0", downloadDir.toString()).withConfigPath(path).create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("biome/js/longLineBefore.js", "biome/js/longLineAfter80.js");
			}

			private String createBiomeConfig(String name) {
				var config = createTestFile(name).toPath();
				var dir = config.getParent();
				var rome = dir.resolve("biome.json");
				ThrowingEx.run(() -> Files.copy(config, rome));
				return dir.toString();
			}
		}

		/**
		 * Tests that files can be formatted when setting the input language explicitly.
		 */
		@Nested
		class ExplicitLanguage {
			/**
			 * Tests that a *.cjs file can be formatted when setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectCjs() {
				var step = RomeStep.withExeDownload(BiomeFlavor.BIOME, "1.2.0", downloadDir.toString()).withLanguage("js").create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("biome/js/fileBefore.cjs", "biome/js/fileAfter.cjs");
			}

			/**
			 * Tests that a *.cts file can be formatted when setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectCts() {
				var step = RomeStep.withExeDownload(BiomeFlavor.BIOME, "1.2.0", downloadDir.toString()).withLanguage("ts").create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("biome/ts/fileBefore.cts", "biome/ts/fileAfter.cts");
			}

			/**
			 * Tests that a *.js file can be formatted when setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectJs() {
				var step = RomeStep.withExeDownload(BiomeFlavor.BIOME, "1.2.0", downloadDir.toString()).withLanguage("js").create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("biome/js/fileBefore.js", "biome/js/fileAfter.js");
			}

			/**
			 * Tests that a *.json file can be formatted when setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectJson() {
				var step = RomeStep.withExeDownload(BiomeFlavor.BIOME, "1.2.0", downloadDir.toString()).withLanguage("json").create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("biome/json/fileBefore.json", "biome/json/fileAfter.json");
			}

			/**
			 * Tests that a *.jsx file can be formatted when setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectJsx() {
				var step = RomeStep.withExeDownload(BiomeFlavor.BIOME, "1.2.0", downloadDir.toString()).withLanguage("jsx").create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("biome/js/fileBefore.jsx", "biome/js/fileAfter.jsx");
			}

			/**
			 * Tests that a *.mjs file can be formatted without setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectMjs() {
				var step = RomeStep.withExeDownload(BiomeFlavor.BIOME, "1.2.0", downloadDir.toString()).withLanguage("js").create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("biome/js/fileBefore.mjs", "biome/js/fileAfter.mjs");
			}

			/**
			 * Tests that a *.mts file can be formatted when setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectMts() {
				var step = RomeStep.withExeDownload(BiomeFlavor.BIOME, "1.2.0", downloadDir.toString()).withLanguage("ts").create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("biome/ts/fileBefore.mts", "biome/ts/fileAfter.mts");
			}

			/**
			 * Tests that a *.ts file can be formatted when setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectTs() {
				var step = RomeStep.withExeDownload(BiomeFlavor.BIOME, "1.2.0", downloadDir.toString()).withLanguage("ts").create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("biome/ts/fileBefore.ts", "biome/ts/fileAfter.ts");
			}

			/**
			 * Tests that a *.tsx file can be formatted when setting the input language
			 * explicitly.
			 */
			@Test
			void testAutoDetectTsx() {
				var step = RomeStep.withExeDownload(BiomeFlavor.BIOME, "1.2.0", downloadDir.toString()).withLanguage("tsx").create();
				var stepHarness = StepHarnessWithFile.forStep(RomeStepTest.this, step);
				stepHarness.testResource("biome/ts/fileBefore.tsx", "biome/ts/fileAfter.tsx");
			}
		}
	}
}
