/*
 * Copyright 2026 DiffPlug
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

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.ProcessRunner;
import com.diffplug.spotless.maven.MavenIntegrationHarness;

class ForbidWildcardImportsMultiFileStepTest extends MavenIntegrationHarness {

	/**
	 * Regression test: in apply mode a single linting file must not abort processing of the
	 * remaining files. Lints across all files are collected and the build fails once at the end,
	 * so every file's lints are reported.
	 */
	@Test
	void testApplyAggregatesLintsAcrossAllFiles() throws Exception {
		writePomWithJavaSteps("<forbidWildcardImports/>");

		String first = "src/main/java/test1.java";
		String second = "src/main/java/test2.java";
		setFile(first).toResource("java/forbidwildcardimports/JavaCodeWildcardsUnformatted.test");
		setFile(second).toResource("java/forbidwildcardimports/JavaCodeWildcardsUnformatted.test");

		ProcessRunner.Result result = mavenRunner().withArguments("spotless:apply").runHasError();
		String output = result.stdOutUtf8();

		// 5 wildcard imports per file across 2 files = 10, aggregated into a single failure
		assertThat(output).contains("There were 10 lint error(s), they must be fixed or suppressed.");
		// both files reported -> the loop did NOT abort on the first linting file
		assertThat(output).contains(first + ":");
		assertThat(output).contains(second + ":");
		assertThat(output).contains("Resolve these lints or suppress with `<lintSuppressions>`");

		// forbidWildcardImports cannot auto-fix, so both files are left untouched
		assertFile(first).sameAsResource("java/forbidwildcardimports/JavaCodeWildcardsUnformatted.test");
		assertFile(second).sameAsResource("java/forbidwildcardimports/JavaCodeWildcardsUnformatted.test");
	}
}
