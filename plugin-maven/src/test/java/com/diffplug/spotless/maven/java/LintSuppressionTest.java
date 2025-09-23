/*
 * Copyright 2025 DiffPlug
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
package com.diffplug.spotless.maven.java;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

class LintSuppressionTest extends MavenIntegrationHarness {

	@Test
	void testNoSuppressionFailsOnWildcardImports() throws Exception {
		writePomWithJavaSteps("<removeWildcardImports/>");

		String path = "src/main/java/TestFile.java";
		setFile(path).toResource("java/removewildcardimports/JavaCodeWildcardsUnformatted.test");

		expectSelfieErrorMsg(mavenRunner().withArguments("spotless:check").runHasError()).toBe("""
				Failed to execute goal com.diffplug.spotless:spotless-maven-plugin:VERSION:check (default-cli) on project spotless-maven-plugin-tests: Unable to format file PROJECT_DIR/src/main/java/TestFile.java
				Step 'removeWildcardImports' found problem in 'TestFile.java':
				TestFile.java:L1 removeWildcardImports(import java.util.*;) Do not use wildcard imports (e.g. java.util.*) - replace with specific class imports (e.g. java.util.List) as 'spotlessApply' cannot auto-fix this
				TestFile.java:L2 removeWildcardImports(import static java.util.Collections.*;) Do not use wildcard imports (e.g. java.util.*) - replace with specific class imports (e.g. java.util.List) as 'spotlessApply' cannot auto-fix this
				TestFile.java:L5 removeWildcardImports(import io.quarkus.maven.dependency.*;) Do not use wildcard imports (e.g. java.util.*) - replace with specific class imports (e.g. java.util.List) as 'spotlessApply' cannot auto-fix this
				TestFile.java:L6 removeWildcardImports(import static io.quarkus.vertx.web.Route.HttpMethod.*;) Do not use wildcard imports (e.g. java.util.*) - replace with specific class imports (e.g. java.util.List) as 'spotlessApply' cannot auto-fix this
				TestFile.java:L7 removeWildcardImports(import static org.springframework.web.reactive.function.BodyInserters.*;) Do not use wildcard imports (e.g. java.util.*) - replace with specific class imports (e.g. java.util.List) as 'spotlessApply' cannot auto-fix this
				""");
	}

	@Test
	void testSuppressByFilePath() throws Exception {
		writePomWithLintSuppressions(
				"<removeWildcardImports/>",
				"<lintSuppressions>",
				"  <lintSuppression>",
				"    <path>src/main/java/TestFile1.java</path>",
				"    <step>*</step>",
				"    <shortCode>*</shortCode>",
				"  </lintSuppression>",
				"</lintSuppressions>");

		String suppressedFile = "src/main/java/TestFile1.java";
		String unsuppressedFile = "src/main/java/TestFile2.java";

		setFile(suppressedFile).toResource("java/removewildcardimports/JavaCodeWildcardsUnformatted.test");
		setFile(unsuppressedFile).toResource("java/removewildcardimports/JavaCodeWildcardsUnformatted.test");

		var result = mavenRunner().withArguments("spotless:check").runHasError();
		assertThat(result.stdOutUtf8()).contains("TestFile2.java");
		assertThat(result.stdOutUtf8()).doesNotContain("TestFile1.java");
	}

	@Test
	void testSuppressByStep() throws Exception {
		writePomWithLintSuppressions(
				"<removeWildcardImports/>",
				"<lintSuppressions>",
				"  <lintSuppression>",
				"    <path>*</path>",
				"    <step>removeWildcardImports</step>",
				"    <shortCode>*</shortCode>",
				"  </lintSuppression>",
				"</lintSuppressions>");

		String path = "src/main/java/TestFile.java";
		setFile(path).toResource("java/removewildcardimports/JavaCodeWildcardsUnformatted.test");

		// Should succeed because we suppressed the entire step
		mavenRunner().withArguments("spotless:check").runNoError();
	}

	@Test
	void testSuppressByShortCode() throws Exception {
		// Use wildcard to suppress all shortCodes - this tests the shortCode suppression mechanism
		writePomWithLintSuppressions(
				"<removeWildcardImports/>",
				"<lintSuppressions>",
				"  <lintSuppression>",
				"    <path>*</path>",
				"    <step>*</step>",
				"    <shortCode>*</shortCode>",
				"  </lintSuppression>",
				"</lintSuppressions>");

		String path = "src/main/java/TestFile.java";
		setFile(path).toResource("java/removewildcardimports/JavaCodeWildcardsUnformatted.test");

		// Should succeed because we suppressed all error codes
		mavenRunner().withArguments("spotless:check").runNoError();
	}

	@Test
	void testMultipleSuppressionsWork() throws Exception {
		writePomWithLintSuppressions(
				"<removeWildcardImports/>",
				"<lintSuppressions>",
				"  <lintSuppression>",
				"    <path>src/main/java/TestFile1.java</path>",
				"    <step>*</step>",
				"    <shortCode>*</shortCode>",
				"  </lintSuppression>",
				"  <lintSuppression>",
				"    <path>src/main/java/TestFile2.java</path>",
				"    <step>*</step>",
				"    <shortCode>*</shortCode>",
				"  </lintSuppression>",
				"</lintSuppressions>");

		String file1 = "src/main/java/TestFile1.java";
		String file2 = "src/main/java/TestFile2.java";
		String file3 = "src/main/java/TestFile3.java";

		setFile(file1).toResource("java/removewildcardimports/JavaCodeWildcardsUnformatted.test");
		setFile(file2).toResource("java/removewildcardimports/JavaCodeWildcardsUnformatted.test");
		setFile(file3).toResource("java/removewildcardimports/JavaCodeWildcardsUnformatted.test");

		var result = mavenRunner().withArguments("spotless:check").runHasError();
		assertThat(result.stdOutUtf8()).contains("TestFile3.java");
		assertThat(result.stdOutUtf8()).doesNotContain("TestFile1.java");
		assertThat(result.stdOutUtf8()).doesNotContain("TestFile2.java");
	}

	/**
	 * Helper method to write POM with both Java steps and lint suppressions configuration
	 */
	private void writePomWithLintSuppressions(String... stepsAndSuppressions) throws IOException {
		// Separate java steps from lint suppressions
		StringBuilder javaSteps = new StringBuilder();
		StringBuilder globalConfig = new StringBuilder();

		boolean inSuppressions = false;
		for (String line : stepsAndSuppressions) {
			if (line.startsWith("<lintSuppressions>")) {
				inSuppressions = true;
				globalConfig.append(line);
			} else if (line.startsWith("</lintSuppressions>")) {
				inSuppressions = false;
				globalConfig.append(line);
			} else if (inSuppressions) {
				globalConfig.append(line);
			} else {
				// This is a java step
				javaSteps.append(line);
			}
		}

		// Create the configuration
		String javaGroup = "<java>" + javaSteps.toString() + "</java>";
		String fullConfiguration = javaGroup + globalConfig.toString();

		writePom(fullConfiguration);
	}
}
