/*
 * Copyright 2023-2024 DiffPlug
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
package com.diffplug.spotless.maven.biome;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.owasp.encoder.Encode.forXml;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

/**
 * Tests for the Biome formatter used via the Maven spotless plugin.
 */
class BiomeMavenTest extends MavenIntegrationHarness {
	/**
	 * Tests that biome can be used as a CSS formatting step, using biome 1.8.3
	 * which requires opt-in.
	 *
	 * @throws Exception When a test failure occurs.
	 */
	@Test
	void asCssStepExperimental() throws Exception {
		writePomWithCssSteps("**/*.css", "<biome><version>1.8.3</version><configPath>configs</configPath></biome>");
		setFile("biome_test.css").toResource("biome/css/fileBefore.css");
		setFile("configs/biome.json").toResource("biome/config/css-enabled.json");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile("biome_test.css").sameAsResource("biome/css/fileAfter.css");
	}

	/**
	 * Tests that biome can be used as a CSS formatting step, with biome 1.9.0
	 * which does not require opt-in.
	 *
	 * @throws Exception When a test failure occurs.
	 */
	@Test
	void asCssStepStable() throws Exception {
		writePomWithCssSteps("**/*.js", "<biome><version>1.9.0</version></biome>");
		setFile("biome_test.css").toResource("biome/css/fileBefore.css");
		var res = mavenRunner().withArguments("spotless:apply").runNoError();
		System.out.println(res.stdOutUtf8());
		assertFile("biome_test.css").sameAsResource("biome/css/fileAfter.css");
	}

	/**
	 * Tests that Biome can be used as a generic formatting step.
	 *
	 * @throws Exception When a test failure occurs.
	 */
	@Test
	void asGenericStep() throws Exception {
		writePomWithBiomeSteps("**/*.js", "<biome><version>1.2.0</version></biome>");
		setFile("biome_test.js").toResource("biome/js/fileBefore.js");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile("biome_test.js").sameAsResource("biome/js/fileAfter.js");
	}

	/**
	 * Tests that Biome can be used as a JavaScript formatting step.
	 *
	 * @throws Exception When a test failure occurs.
	 */
	@Test
	void asJavaScriptStep() throws Exception {
		writePomWithJavascriptSteps("**/*.js", "<biome><version>1.2.0</version></biome>");
		setFile("biome_test.js").toResource("biome/js/fileBefore.js");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile("biome_test.js").sameAsResource("biome/js/fileAfter.js");
	}

	/**
	 * Tests that biome can be used as a JSON formatting step.
	 *
	 * @throws Exception When a test failure occurs.
	 */
	@Test
	void asJsonStep() throws Exception {
		writePomWithJsonSteps("**/*.json", "<biome><version>1.2.0</version></biome>");
		setFile("biome_test.json").toResource("biome/json/fileBefore.json");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile("biome_test.json").sameAsResource("biome/json/fileAfter.json");
	}

	/**
	 * Tests that biome can be used as a TypeScript formatting step.
	 *
	 * @throws Exception When a test failure occurs.
	 */
	@Test
	void asTypeScriptStep() throws Exception {
		writePomWithTypescriptSteps("**/*.ts", "<biome><version>1.2.0</version></biome>");
		setFile("biome_test.ts").toResource("biome/ts/fileBefore.ts");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile("biome_test.ts").sameAsResource("biome/ts/fileAfter.ts");
	}

	/**
	 * Tests that the language can be specified for the generic format step.
	 *
	 * @throws Exception When a test failure occurs.
	 */
	@Test
	void canSetLanguageForGenericStep() throws Exception {
		writePomWithBiomeSteps("**/*.nosj", "<biome><version>1.2.0</version><language>json</language></biome>");
		setFile("biome_test.nosj").toResource("biome/json/fileBefore.json");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile("biome_test.nosj").sameAsResource("biome/json/fileAfter.json");
	}

	/**
	 * Tests that an absolute config path can be specified.
	 *
	 * @throws Exception When a test failure occurs.
	 */
	@Test
	void configPathAbsolute() throws Exception {
		var path = newFile("configs").getAbsolutePath();
		writePomWithBiomeSteps("**/*.js",
				"<biome><version>1.2.0</version><configPath>" + forXml(path) + "</configPath></biome>");
		setFile("biome_test.js").toResource("biome/js/longLineBefore.js");
		setFile("configs/biome.json").toResource("biome/config/line-width-120.json");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile("biome_test.js").sameAsResource("biome/js/longLineAfter120.js");
	}

	/**
	 * Tests that a path to the directory with the biome.json config file can be
	 * specified. Uses a config file with a line width of 120.
	 *
	 * @throws Exception When a test failure occurs.
	 */
	@Test
	void configPathLineWidth120() throws Exception {
		writePomWithBiomeSteps("**/*.js", "<biome><version>1.2.0</version><configPath>configs</configPath></biome>");
		setFile("biome_test.js").toResource("biome/js/longLineBefore.js");
		setFile("configs/biome.json").toResource("biome/config/line-width-120.json");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile("biome_test.js").sameAsResource("biome/js/longLineAfter120.js");
	}

	/**
	 * Tests that a path to the directory with the biome.json config file can be
	 * specified. Uses a config file with a line width of 80.
	 *
	 * @throws Exception When a test failure occurs.
	 */
	@Test
	void configPathLineWidth80() throws Exception {
		writePomWithBiomeSteps("**/*.js", "<biome><version>1.2.0</version><configPath>configs</configPath></biome>");
		setFile("biome_test.js").toResource("biome/js/longLineBefore.js");
		setFile("configs/biome.json").toResource("biome/config/line-width-80.json");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile("biome_test.js").sameAsResource("biome/js/longLineAfter80.js");
	}

	/**
	 * Tests that the download directory can be an absolute path.
	 *
	 * @throws Exception When a test failure occurs.
	 */
	@Test
	void downloadDirAbsolute() throws Exception {
		var path = newFile("target/bin/biome").getAbsoluteFile().toString();
		writePomWithBiomeSteps("**/*.js",
				"<biome><version>1.2.0</version><downloadDir>" + forXml(path) + "</downloadDir></biome>");
		setFile("biome_test.js").toResource("biome/js/fileBefore.js");
		assertTrue(!newFile("target/bin/biome").exists() || newFile("target/bin/biome").list().length == 0);
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile("biome_test.js").sameAsResource("biome/js/fileAfter.js");
		assertEquals(2, newFile("target/bin/biome").list().length);
	}

	/**
	 * Tests that the download directory can be changed to a path relative to the
	 * project's base directory.
	 *
	 * @throws Exception When a test failure occurs.
	 */
	@Test
	void downloadDirRelative() throws Exception {
		writePomWithBiomeSteps("**/*.js",
				"<biome><version>1.2.0</version><downloadDir>target/bin/biome</downloadDir></biome>");
		setFile("biome_test.js").toResource("biome/js/fileBefore.js");
		assertTrue(!newFile("target/bin/biome").exists() || newFile("target/bin/biome").list().length == 0);
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile("biome_test.js").sameAsResource("biome/js/fileAfter.js");
		assertEquals(2, newFile("target/bin/biome").list().length);
	}

	/**
	 * Tests that the build fails when the input file could not be parsed.
	 *
	 * @throws Exception When a test failure occurs.
	 */
	@Test
	void failureWhenExeNotFound() throws Exception {
		writePomWithBiomeSteps("**/*.js", "<biome><version>1.2.0</version><pathToExe>biome/is/missing</pathToExe></biome>");
		setFile("biome_test.js").toResource("biome/js/fileBefore.js");
		var result = mavenRunner().withArguments("spotless:apply").runHasError();
		assertFile("biome_test.js").sameAsResource("biome/js/fileBefore.js");
		assertThat(result.stdOutUtf8()).contains("Biome executable does not exist");
	}

	/**
	 * Tests that the build fails when the input file could not be parsed.
	 *
	 * @throws Exception When a test failure occurs.
	 */
	@Test
	void failureWhenNotParseable() throws Exception {
		writePomWithBiomeSteps("**/*.js", "<biome><version>1.2.0</version><language>json</language></biome>");
		setFile("biome_test.js").toResource("biome/js/fileBefore.js");
		var result = mavenRunner().withArguments("spotless:apply").runHasError();
		assertFile("biome_test.js").sameAsResource("biome/js/fileBefore.js");
		assertThat(result.stdOutUtf8()).contains("Format with errors is disabled.");
		assertThat(result.stdOutUtf8()).contains("Unable to format file");
		assertThat(result.stdOutUtf8()).contains("Step 'biome' found problem in 'biome_test.js'");
	}

	/**
	 * Biome is hard-coded to ignore certain files, such as package.json. Since version 1.5.0,
	 * the biome CLI does not output any formatted code anymore, whereas previously it printed
	 * the input as-is. This tests checks that when the biome formatter outputs an empty string,
	 * the contents of the file to format are used instead.
	 *
	 * @throws Exception When a test failure occurs.
	 */
	@Test
	void preservesIgnoredFiles() throws Exception {
		writePomWithJsonSteps("**/*.json", "<biome><version>1.5.0</version></biome>");
		setFile("package.json").toResource("biome/json/packageBefore.json");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile("package.json").sameAsResource("biome/json/packageAfter.json");
	}

	/**
	 * Tests that the Maven plugin works with version 2.x of biome.
	 *
	 * @throws Exception When a test failure occurs.
	 */
	@Test
	void version2X() throws Exception {
		writePomWithBiomeSteps("**/*.js", "<biome><version>2.0.6</version></biome>");
		setFile("biome_test.js").toResource("biome/js/fileBefore.js");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile("biome_test.js").sameAsResource("biome/js/fileAfter.js");
	}
}
