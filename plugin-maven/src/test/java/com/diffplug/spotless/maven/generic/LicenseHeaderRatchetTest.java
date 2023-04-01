/*
 * Copyright 2020-2021 DiffPlug
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
package com.diffplug.spotless.maven.generic;

import java.io.IOException;
import java.time.YearMonth;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

class LicenseHeaderRatchetTest extends MavenIntegrationHarness {
	private static final String NOW = String.valueOf(YearMonth.now().getYear());

	private static final String TEST_JAVA = "src/main/java/pkg/Test.java";
	private static final String CONTENT = "package pkg;\npublic class Test {}";

	private void setRatchetFrom(String ratchetFrom) throws IOException {
		writePomWithJavaSteps(
				"<licenseHeader>",
				"  <content>/** $YEAR */</content>",
				"</licenseHeader>",
				ratchetFrom);
	}

	private void assertUnchanged(String year) throws Exception {
		assertTransform(year, year);
	}

	private void assertTransform(String yearBefore, String yearAfter) throws Exception {
		setFile(TEST_JAVA).toContent("/** " + yearBefore + " */\n" + CONTENT);
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(TEST_JAVA).hasContent("/** " + yearAfter + " */\n" + CONTENT);
	}

	private void testSuiteUpdateWithLatest(boolean update) throws Exception {
		if (update) {
			assertTransform("2003", "2003-" + NOW);
			assertTransform("2003-2005", "2003-" + NOW);
		} else {
			assertUnchanged("2003");
			assertUnchanged("2003-2005");
		}
		assertUnchanged(NOW);
		assertTransform("", NOW);
	}

	@Test
	void normal() throws Exception {
		setRatchetFrom("");
		testSuiteUpdateWithLatest(false);
	}

	@Test
	void ratchetFrom() throws Exception {
		try (Git git = Git.init().setDirectory(rootFolder()).call()) {
			git.commit().setMessage("First commit").call();
		}
		setRatchetFrom("<ratchetFrom>HEAD</ratchetFrom>");
		testSuiteUpdateWithLatest(true);
	}
}
