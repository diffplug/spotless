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
package com.diffplug.spotless.maven;

import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.spotless.ClearGitConfig;

/** Integration tests for {@code <respectGitignore>}, see https://github.com/diffplug/spotless/issues/2443 */
@ClearGitConfig
class RespectGitignoreTest extends MavenIntegrationHarness {
	private static final String IGNORED = "src/generated/Ignored.md";
	private static final String TRACKED = "src/main/Tracked.md";

	private static String pom(String globalFlag, String formatFlag, String ratchet) {
		return StringPrinter.buildStringFromLines(
				globalFlag,
				ratchet,
				"<formats>",
				"  <format>",
				formatFlag,
				"    <includes>",
				"      <include>src/**/*.md</include>",
				"    </includes>",
				"    <replace>",
				"      <name>Lowercase hello</name>",
				"      <search>HELLO</search>",
				"      <replacement>hello</replacement>",
				"    </replace>",
				"  </format>",
				"</formats>");
	}

	/** Repo with a committed .gitignore for src/generated/, a clean tracked file, and a dirty ignored file. */
	private Git givenRepoWithDirtyIgnoredFile() throws GitAPIException, IOException {
		Git git = Git.init().setDirectory(rootFolder()).call();
		setFile(".gitignore").toContent("src/generated/\n");
		setFile(TRACKED).toContent("hello");
		git.add().addFilepattern(".gitignore").addFilepattern(TRACKED).call();
		git.commit().setSign(false).setMessage("init").call();
		git.tag().setName("baseline").call();
		setFile(IGNORED).toContent("HELLO");
		return git;
	}

	@Test
	@DisplayName("given respectGitignore is not configured, when checking, then ignored files are still formatted (unchanged default)")
	void defaultStillFormatsIgnoredFiles() throws Exception {
		try (Git git = givenRepoWithDirtyIgnoredFile()) {
			writePom(pom("", "", ""));
			mavenRunner().withArguments("spotless:check").runHasError();
		}
	}

	@Test
	@DisplayName("given global respectGitignore, when checking and applying, then ignored files are skipped")
	void globalFlagSkipsIgnoredFiles() throws Exception {
		try (Git git = givenRepoWithDirtyIgnoredFile()) {
			writePom(pom("<respectGitignore>true</respectGitignore>", "", ""));
			mavenRunner().withArguments("spotless:check").runNoError();
			mavenRunner().withArguments("spotless:apply").runNoError();
			assertFile(IGNORED).hasContent("HELLO");
		}
	}

	@Test
	@DisplayName("given global respectGitignore, when a non-ignored file is dirty, then it is still reported and fixed")
	void globalFlagStillFormatsNonIgnoredFiles() throws Exception {
		try (Git git = givenRepoWithDirtyIgnoredFile()) {
			writePom(pom("<respectGitignore>true</respectGitignore>", "", ""));
			setFile(TRACKED).toContent("HELLO");
			mavenRunner().withArguments("spotless:check").runHasError();
			mavenRunner().withArguments("spotless:apply").runNoError();
			assertFile(TRACKED).hasContent("hello");
			assertFile(IGNORED).hasContent("HELLO");
		}
	}

	@Test
	@DisplayName("given respectGitignore only on the format, when checking, then ignored files are skipped")
	void formatFlagSkipsIgnoredFiles() throws Exception {
		try (Git git = givenRepoWithDirtyIgnoredFile()) {
			writePom(pom("", "<respectGitignore>true</respectGitignore>", ""));
			mavenRunner().withArguments("spotless:check").runNoError();
		}
	}

	@Test
	@DisplayName("given global true but format false, when checking, then the format-level value wins")
	void formatFlagOverridesGlobal() throws Exception {
		try (Git git = givenRepoWithDirtyIgnoredFile()) {
			writePom(pom("<respectGitignore>true</respectGitignore>", "<respectGitignore>false</respectGitignore>", ""));
			mavenRunner().withArguments("spotless:check").runHasError();
		}
	}

	@Test
	@DisplayName("given respectGitignore with ratchetFrom, when an ignored file is new, then it is skipped")
	void combinesWithRatchetFrom() throws Exception {
		try (Git git = givenRepoWithDirtyIgnoredFile()) {
			writePom(pom("<respectGitignore>true</respectGitignore>", "", "<ratchetFrom>baseline</ratchetFrom>"));
			mavenRunner().withArguments("spotless:check").runNoError();
		}
	}
}
