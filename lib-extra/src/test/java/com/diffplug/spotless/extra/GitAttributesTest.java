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
package com.diffplug.spotless.extra;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Test;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.ResourceHarness;

class GitAttributesTest extends ResourceHarness {
	private List<File> testFiles(String prefix) {
		List<File> result = new ArrayList<>();
		for (String path : TEST_PATHS) {
			var prefixedPath = prefix + path;
			setFile(prefixedPath).toContent("");
			result.add(newFile(prefixedPath));
		}
		return result;
	}

	private List<File> testFiles() {
		return testFiles("");
	}

	private static final List<String> TEST_PATHS = Arrays.asList("someFile", "subfolder/someFile", "MANIFEST.MF", "subfolder/MANIFEST.MF");

	@Test
	void cacheTest() {
		setFile(".gitattributes").toContent(StringPrinter.buildStringFromLines(
				"* eol=lf",
				"*.MF eol=crlf"));
		{
			GitAttributesLineEndings.AttributesCache cache = new GitAttributesLineEndings.AttributesCache();
			Assertions.assertThat(cache.valueFor(newFile("someFile"), "eol")).isEqualTo("lf");
			Assertions.assertThat(cache.valueFor(newFile("subfolder/someFile"), "eol")).isEqualTo("lf");
			Assertions.assertThat(cache.valueFor(newFile("MANIFEST.MF"), "eol")).isEqualTo("crlf");
			Assertions.assertThat(cache.valueFor(newFile("subfolder/MANIFEST.MF"), "eol")).isEqualTo("crlf");

			// write out a .gitattributes for the subfolder
			setFile("subfolder/.gitattributes").toContent("* eol=lf");

			// it shouldn't change anything, because it's cached
			Assertions.assertThat(cache.valueFor(newFile("someFile"), "eol")).isEqualTo("lf");
			Assertions.assertThat(cache.valueFor(newFile("subfolder/someFile"), "eol")).isEqualTo("lf");
			Assertions.assertThat(cache.valueFor(newFile("MANIFEST.MF"), "eol")).isEqualTo("crlf");
			Assertions.assertThat(cache.valueFor(newFile("subfolder/MANIFEST.MF"), "eol")).isEqualTo("crlf");
		}
		{
			// but if we make a new cache, it should change
			GitAttributesLineEndings.AttributesCache cache = new GitAttributesLineEndings.AttributesCache();
			Assertions.assertThat(cache.valueFor(newFile("someFile"), "eol")).isEqualTo("lf");
			Assertions.assertThat(cache.valueFor(newFile("subfolder/someFile"), "eol")).isEqualTo("lf");
			Assertions.assertThat(cache.valueFor(newFile("MANIFEST.MF"), "eol")).isEqualTo("crlf");
			Assertions.assertThat(cache.valueFor(newFile("subfolder/MANIFEST.MF"), "eol")).isEqualTo("lf");
		}
	}

	@Test
	void policyTest() {
		setFile(".gitattributes").toContent(StringPrinter.buildStringFromLines(
				"* eol=lf",
				"*.MF eol=crlf"));
		LineEnding.Policy policy = LineEnding.GIT_ATTRIBUTES.createPolicy(rootFolder(), () -> testFiles());
		Assertions.assertThat(policy.getEndingFor(newFile("someFile"))).isEqualTo("\n");
		Assertions.assertThat(policy.getEndingFor(newFile("subfolder/someFile"))).isEqualTo("\n");
		Assertions.assertThat(policy.getEndingFor(newFile("MANIFEST.MF"))).isEqualTo("\r\n");
		Assertions.assertThat(policy.getEndingFor(newFile("subfolder/MANIFEST.MF"))).isEqualTo("\r\n");
	}

	@Test
	void policyDefaultLineEndingTest() throws GitAPIException {
		Git git = Git.init().setDirectory(rootFolder()).call();
		git.close();
		setFile(".git/config").toContent(StringPrinter.buildStringFromLines(
				"[core]",
				"autocrlf=true",
				"eol=lf"));
		LineEnding.Policy policy = LineEnding.GIT_ATTRIBUTES.createPolicy(rootFolder(), () -> testFiles());
		Assertions.assertThat(policy.getEndingFor(newFile("someFile"))).isEqualTo("\r\n");
	}

	@Test
	void policyTestWithExternalGitDir() throws IOException, GitAPIException {
		File projectFolder = newFolder("project");
		File gitDir = newFolder("project.git");
		Git.init().setDirectory(projectFolder).setGitDir(gitDir).call();

		setFile("project.git/info/attributes").toContent(StringPrinter.buildStringFromLines(
				"* eol=lf",
				"*.MF eol=crlf"));
		LineEnding.Policy policy = LineEnding.GIT_ATTRIBUTES.createPolicy(projectFolder, () -> testFiles("project/"));
		Assertions.assertThat(policy.getEndingFor(newFile("project/someFile"))).isEqualTo("\n");
		Assertions.assertThat(policy.getEndingFor(newFile("project/subfolder/someFile"))).isEqualTo("\n");
		Assertions.assertThat(policy.getEndingFor(newFile("project/MANIFEST.MF"))).isEqualTo("\r\n");
		Assertions.assertThat(policy.getEndingFor(newFile("project/subfolder/MANIFEST.MF"))).isEqualTo("\r\n");
	}

	@Test
	void policyTestWithCommonDir() throws IOException, GitAPIException {
		File projectFolder = newFolder("project");
		File commonGitDir = newFolder("project.git");
		Git.init().setDirectory(projectFolder).setGitDir(commonGitDir).call();
		newFolder("project.git/worktrees/");

		File projectGitDir = newFolder("project.git/worktrees/project/");
		setFile("project.git/worktrees/project/gitdir").toContent(projectFolder.getAbsolutePath() + "/.git");
		setFile("project.git/worktrees/project/commondir").toContent("../..");
		setFile("project/.git").toContent("gitdir: " + projectGitDir.getAbsolutePath());

		setFile("project.git/info/attributes").toContent(StringPrinter.buildStringFromLines(
				"* eol=lf",
				"*.MF eol=crlf"));
		LineEnding.Policy policy = LineEnding.GIT_ATTRIBUTES.createPolicy(projectFolder, () -> testFiles("project/"));
		Assertions.assertThat(policy.getEndingFor(newFile("project/someFile"))).isEqualTo("\n");
		Assertions.assertThat(policy.getEndingFor(newFile("project/subfolder/someFile"))).isEqualTo("\n");
		Assertions.assertThat(policy.getEndingFor(newFile("project/MANIFEST.MF"))).isEqualTo("\r\n");
		Assertions.assertThat(policy.getEndingFor(newFile("project/subfolder/MANIFEST.MF"))).isEqualTo("\r\n");
	}
}
