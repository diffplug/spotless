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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.spotless.ClearGitConfig;

/** Integration tests for {@code <excludeUsingGitignore>}, see https://github.com/diffplug/spotless/issues/2443 */
@ClearGitConfig
class ExcludeUsingGitignoreTest extends MavenIntegrationHarness {
	private static final String IGNORED = "src/generated/Ignored.md";
	private static final String TRACKED = "src/main/Tracked.md";
	private static final String OPT_IN = "<excludeUsingGitignore>true</excludeUsingGitignore>";

	private static String withFormats(String... formats) {
		return StringPrinter.buildStringFromLines(
				"<formats>",
				String.join("\n", formats),
				"</formats>");
	}

	/** A format which lowercases HELLO in the given files, with extra configuration lines. */
	private static String format(String include, String... extraLines) {
		return StringPrinter.buildStringFromLines(
				"  <format>",
				String.join("\n", extraLines),
				"    <includes>",
				"      <include>" + include + "</include>",
				"    </includes>",
				"    <replace>",
				"      <name>Lowercase hello</name>",
				"      <search>HELLO</search>",
				"      <replacement>hello</replacement>",
				"    </replace>",
				"  </format>");
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
	@DisplayName("given excludeUsingGitignore is not configured, when checking, then ignored files are still formatted (unchanged default)")
	void defaultStillFormatsIgnoredFiles() throws Exception {
		try (Git git = givenRepoWithDirtyIgnoredFile()) {
			writePom(withFormats(format("src/**/*.md")));
			mavenRunner().withArguments("spotless:check").runHasError();
		}
	}

	@Test
	@DisplayName("given excludeUsingGitignore on the format, when checking and applying, then ignored files are skipped")
	void skipsIgnoredFiles() throws Exception {
		try (Git git = givenRepoWithDirtyIgnoredFile()) {
			writePom(withFormats(format("src/**/*.md", OPT_IN)));
			mavenRunner().withArguments("spotless:check").runNoError();
			mavenRunner().withArguments("spotless:apply").runNoError();
			assertFile(IGNORED).hasContent("HELLO");
		}
	}

	@Test
	@DisplayName("given excludeUsingGitignore on the format, when a non-ignored file is dirty, then it is still reported and fixed")
	void stillFormatsNonIgnoredFiles() throws Exception {
		try (Git git = givenRepoWithDirtyIgnoredFile()) {
			writePom(withFormats(format("src/**/*.md", OPT_IN)));
			setFile(TRACKED).toContent("HELLO");
			mavenRunner().withArguments("spotless:check").runHasError();
			mavenRunner().withArguments("spotless:apply").runNoError();
			assertFile(TRACKED).hasContent("hello");
			assertFile(IGNORED).hasContent("HELLO");
		}
	}

	@Test
	@DisplayName("given two formats and only one opts in, when applying, then only the other format still formats ignored files")
	void onlyAffectsTheFormatWhichOptsIn() throws Exception {
		try (Git git = givenRepoWithDirtyIgnoredFile()) {
			setFile("src/generated/Ignored.txt").toContent("HELLO");
			writePom(withFormats(format("src/**/*.md", OPT_IN), format("src/**/*.txt")));
			mavenRunner().withArguments("spotless:apply").runNoError();
			assertFile(IGNORED).hasContent("HELLO");
			assertFile("src/generated/Ignored.txt").hasContent("hello");
		}
	}

	@Test
	@DisplayName("given excludeUsingGitignore together with excludes, when checking, then both exclusions apply")
	void combinesWithExcludes() throws Exception {
		try (Git git = givenRepoWithDirtyIgnoredFile()) {
			setFile("src/vendor/Vendored.md").toContent("HELLO");
			writePom(withFormats(format("src/**/*.md", OPT_IN, "<excludes><exclude>src/vendor/**</exclude></excludes>")));
			mavenRunner().withArguments("spotless:check").runNoError();
		}
	}

	@Test
	@DisplayName("given excludeUsingGitignore with ratchetFrom, when an ignored file is new, then it is skipped")
	void combinesWithRatchetFrom() throws Exception {
		try (Git git = givenRepoWithDirtyIgnoredFile()) {
			writePom("<ratchetFrom>baseline</ratchetFrom>\n" + withFormats(format("src/**/*.md", OPT_IN)));
			mavenRunner().withArguments("spotless:check").runNoError();
		}
	}

	@Test
	@DisplayName("given excludeUsingGitignore outside of a git repository, when checking, then the build fails with a clear message")
	void notAGitRepositoryFails() throws Exception {
		setFile(TRACKED).toContent("hello");
		writePom(withFormats(format("src/**/*.md", OPT_IN)));
		assertThat(mavenRunner().withArguments("spotless:check").runHasError().stdOutUtf8()).contains("requires a git repository");
	}
}
