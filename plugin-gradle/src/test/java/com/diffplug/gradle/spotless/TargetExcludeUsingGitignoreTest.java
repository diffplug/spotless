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
package com.diffplug.gradle.spotless;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.diffplug.spotless.ClearGitConfig;

/** Integration tests for {@code targetExcludeUsingGitignore()}, see https://github.com/diffplug/spotless/issues/2443 */
@ClearGitConfig
class TargetExcludeUsingGitignoreTest extends GradleIntegrationHarness {
	private static final String IGNORED = "src/generated/Ignored.md";
	private static final String TRACKED = "src/main/Tracked.md";
	private static final String OPT_IN = "targetExcludeUsingGitignore()";

	private void givenBuild(boolean configCache, String... miscFormatLines) throws IOException {
		givenBuildWithFormats(configCache, format("misc", miscFormatLines));
	}

	private void givenBuildWithFormats(boolean configCache, String... formats) throws IOException {
		if (configCache) {
			setFile("gradle.properties").toContent("org.gradle.configuration-cache=true");
		}
		setFile("build.gradle").toLines(
				"plugins {",
				"  id 'com.diffplug.spotless'",
				"}",
				"spotless {",
				String.join("\n", formats),
				"}");
	}

	private static String format(String name, String... extraLines) {
		return String.join("\n",
				"  format '" + name + "', {",
				"    target 'src/**/*.md'",
				"    " + String.join("\n    ", extraLines),
				"    replace 'lowercase hello', 'HELLO', 'hello'",
				"  }");
	}

	/** Repo with a committed .gitignore for src/generated/, a clean tracked file, and a dirty ignored file. */
	private Git givenRepoWithDirtyIgnoredFile() throws GitAPIException, IOException {
		Git git = Git.init().setDirectory(rootFolder()).call();
		setFile(".gitignore").toContent("src/generated/\n.gradle/\nbuild/\n");
		setFile(TRACKED).toContent("hello");
		git.add().addFilepattern(".gitignore").addFilepattern(TRACKED).call();
		git.commit().setSign(false).setMessage("init").call();
		setFile(IGNORED).toContent("HELLO");
		return git;
	}

	private static String nativePath(String unixPath) {
		// the violation message lists paths with native separators
		return unixPath.replace('/', File.separatorChar);
	}

	@ParameterizedTest(name = "configCache={0}")
	@ValueSource(booleans = {false, true})
	@DisplayName("given targetExcludeUsingGitignore is not configured, when checking, then ignored files are still formatted (unchanged default)")
	void defaultStillFormatsIgnoredFiles(boolean configCache) throws Exception {
		try (Git git = givenRepoWithDirtyIgnoredFile()) {
			givenBuild(configCache);
			BuildResult result = gradleRunner().withArguments("spotlessCheck").buildAndFail();
			assertThat(result.getOutput()).contains(nativePath(IGNORED));
		}
	}

	@ParameterizedTest(name = "configCache={0}")
	@ValueSource(booleans = {false, true})
	@DisplayName("given targetExcludeUsingGitignore(), when checking and applying, then ignored files are skipped but others are fixed")
	void skipsIgnoredFiles(boolean configCache) throws Exception {
		try (Git git = givenRepoWithDirtyIgnoredFile()) {
			givenBuild(configCache, OPT_IN);
			gradleRunner().withArguments("spotlessCheck").build();

			setFile(TRACKED).toContent("HELLO");
			gradleRunner().withArguments("spotlessCheck").buildAndFail();
			gradleRunner().withArguments("spotlessApply").build();
			assertFile(TRACKED).hasContent("hello");
			assertFile(IGNORED).hasContent("HELLO");
		}
	}

	@ParameterizedTest(name = "configCache={0}")
	@ValueSource(booleans = {false, true})
	@DisplayName("given two formats and only one opts in, when checking, then only the other format still sees ignored files")
	void onlyAffectsTheFormatWhichOptsIn(boolean configCache) throws Exception {
		try (Git git = givenRepoWithDirtyIgnoredFile()) {
			givenBuildWithFormats(configCache, format("optedIn", OPT_IN), format("notOptedIn"));
			gradleRunner().withArguments("spotlessOptedInCheck").build();
			BuildResult result = gradleRunner().withArguments("spotlessNotOptedInCheck").buildAndFail();
			assertThat(result.getOutput()).contains(nativePath(IGNORED));
		}
	}

	@ParameterizedTest(name = "configCache={0}")
	@ValueSource(booleans = {false, true})
	@DisplayName("given targetExcludeUsingGitignore() together with targetExclude, when checking, then both exclusions apply")
	void combinesWithTargetExclude(boolean configCache) throws Exception {
		try (Git git = givenRepoWithDirtyIgnoredFile()) {
			givenBuild(configCache, OPT_IN, "targetExclude 'src/vendor/**'");
			setFile("src/vendor/Vendored.md").toContent("HELLO");
			gradleRunner().withArguments("spotlessCheck").build();
		}
	}

	@ParameterizedTest(name = "configCache={0}")
	@ValueSource(booleans = {false, true})
	@DisplayName("given targetExcludeUsingGitignore(), when .gitignore changes between builds, then the next build honors the new rules")
	void gitignoreChangeIsPickedUp(boolean configCache) throws Exception {
		try (Git git = givenRepoWithDirtyIgnoredFile()) {
			givenBuild(configCache, OPT_IN);
			setFile("src/other/Dirty.md").toContent("HELLO");
			gradleRunner().withArguments("spotlessCheck").buildAndFail();

			setFile(".gitignore").toContent("src/generated/\nsrc/other/\n.gradle/\nbuild/\n");
			gradleRunner().withArguments("spotlessCheck").build();

			setFile(".gitignore").toContent(".gradle/\nbuild/\n");
			gradleRunner().withArguments("spotlessCheck").buildAndFail();
		}
	}

	@Test
	@DisplayName("given targetExcludeUsingGitignore() outside of a git repository, when checking, then the build fails with a clear message")
	void notAGitRepositoryFails() throws Exception {
		givenBuild(false, OPT_IN);
		setFile(TRACKED).toContent("hello");
		BuildResult result = gradleRunner().withArguments("spotlessCheck").buildAndFail();
		assertThat(result.getOutput()).contains("requires a git repository");
	}

	@Test
	@DisplayName("given targetExcludeUsingGitignore() at the top level of the spotless block, when configuring, then the build fails because it is format-level only")
	void isNotAvailableGlobally() throws Exception {
		setFile("build.gradle").toLines(
				"plugins {",
				"  id 'com.diffplug.spotless'",
				"}",
				"spotless {",
				"  " + OPT_IN,
				"}");
		BuildResult result = gradleRunner().withArguments("spotlessCheck").buildAndFail();
		assertThat(result.getOutput()).contains("targetExcludeUsingGitignore");
	}
}
