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
package com.diffplug.spotless.extra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.diffplug.spotless.ClearGitConfig;
import com.diffplug.spotless.ResourceHarness;

@ClearGitConfig
class GitIgnoredFilesTest extends ResourceHarness {

	private Git initRepo() throws GitAPIException {
		return Git.init().setDirectory(rootFolder()).call();
	}

	private static void addAndCommit(Git git) throws GitAPIException {
		git.add().addFilepattern(".").call();
		git.commit().setSign(false).setMessage("commit").call();
	}

	private GitIgnoredFiles underRoot() {
		return GitIgnoredFiles.under(rootFolder());
	}

	@Nested
	@DisplayName("given a root .gitignore")
	class RootGitignore {
		@Test
		@DisplayName("when an untracked file matches a pattern, then it is ignored")
		void untrackedMatchingFileIsIgnored() throws Exception {
			// given
			try (Git git = initRepo()) {
				setFile(".gitignore").toContent("*.log\n");
				addAndCommit(git);
				File log = setFile("debug.log").toContent("x");
				// when / then
				assertThat(underRoot().isIgnored(log)).isTrue();
				assertThat(underRoot().isNotIgnored(log)).isFalse();
			}
		}

		@Test
		@DisplayName("when an untracked file does not match any pattern, then it is not ignored")
		void untrackedNonMatchingFileIsNotIgnored() throws Exception {
			try (Git git = initRepo()) {
				setFile(".gitignore").toContent("*.log\n");
				addAndCommit(git);
				File java = setFile("src/Main.java").toContent("class Main {}");
				assertThat(underRoot().isIgnored(java)).isFalse();
			}
		}

		@Test
		@DisplayName("when a file is deep inside an ignored directory, then it is ignored")
		void fileInsideIgnoredDirectoryIsIgnored() throws Exception {
			try (Git git = initRepo()) {
				setFile(".gitignore").toContent("node_modules/\n");
				addAndCommit(git);
				File dep = setFile("web/node_modules/lib/deep/index.js").toContent("x");
				assertThat(underRoot().isIgnored(dep)).isTrue();
			}
		}

		@Test
		@DisplayName("when a negation pattern re-includes a file, then it is not ignored")
		void negatedPatternIsNotIgnored() throws Exception {
			try (Git git = initRepo()) {
				setFile(".gitignore").toContent("*.log\n!keep.log\n");
				addAndCommit(git);
				File keep = setFile("keep.log").toContent("x");
				File drop = setFile("drop.log").toContent("x");
				GitIgnoredFiles ignored = underRoot();
				assertThat(ignored.isIgnored(keep)).isFalse();
				assertThat(ignored.isIgnored(drop)).isTrue();
			}
		}

		@Test
		@DisplayName("when a file matching a pattern is tracked, then it is NOT ignored (same as git status)")
		void trackedMatchingFileIsNotIgnored() throws Exception {
			try (Git git = initRepo()) {
				setFile("generated/Tracked.java").toContent("class Tracked {}");
				addAndCommit(git);
				setFile(".gitignore").toContent("generated/\n");
				addAndCommit(git);
				File untrackedSibling = setFile("generated/Untracked.java").toContent("class Untracked {}");

				GitIgnoredFiles ignored = underRoot();
				assertThat(ignored.isIgnored(newFile("generated/Tracked.java"))).isFalse();
				assertThat(ignored.isIgnored(untrackedSibling)).isTrue();
			}
		}
	}

	@Nested
	@DisplayName("given ignore rules from other sources")
	class OtherSources {
		@Test
		@DisplayName("when a nested .gitignore has a pattern, then it applies only below its directory")
		void nestedGitignoreIsScoped() throws Exception {
			try (Git git = initRepo()) {
				setFile("sub/.gitignore").toContent("*.tmp\n");
				addAndCommit(git);
				File inSub = setFile("sub/a.tmp").toContent("x");
				File atRoot = setFile("b.tmp").toContent("x");
				GitIgnoredFiles ignored = underRoot();
				assertThat(ignored.isIgnored(inSub)).isTrue();
				assertThat(ignored.isIgnored(atRoot)).isFalse();
			}
		}

		@Test
		@DisplayName("when .git/info/exclude has a pattern, then matching files are ignored")
		void infoExcludeIsHonored() throws Exception {
			try (Git git = initRepo()) {
				setFile(".git/info/exclude").toContent("secret.txt\n");
				File secret = setFile("secret.txt").toContent("x");
				assertThat(underRoot().isIgnored(secret)).isTrue();
			}
		}

		@Test
		@DisplayName("when core.excludesFile points to a file with a pattern, then matching files are ignored")
		void coreExcludesFileIsHonored() throws Exception {
			try (Git git = initRepo()) {
				File excludes = setFile("../global-gitignore").toContent("*.bak\n");
				StoredConfig config = git.getRepository().getConfig();
				config.setString("core", null, "excludesFile", excludes.getAbsolutePath());
				config.save();
				File bak = setFile("old.bak").toContent("x");
				assertThat(underRoot().isIgnored(bak)).isTrue();
			}
		}
	}

	@Nested
	@DisplayName("given the root dir is a subproject of the repository")
	class Subproject {
		@Test
		@DisplayName("when the repository root .gitignore matches a subproject file, then it is ignored")
		void parentRulesApplyToSubproject() throws Exception {
			try (Git git = initRepo()) {
				setFile(".gitignore").toContent("build/\n");
				addAndCommit(git);
				File generated = setFile("moduleA/build/Gen.java").toContent("class Gen {}");
				File source = setFile("moduleA/src/Main.java").toContent("class Main {}");
				GitIgnoredFiles ignored = GitIgnoredFiles.under(newFile("moduleA"));
				assertThat(ignored.isIgnored(generated)).isTrue();
				assertThat(ignored.isIgnored(source)).isFalse();
			}
		}

		@Test
		@DisplayName("when a file lies outside the root dir, then it is not ignored")
		void fileOutsideRootIsNotIgnored() throws Exception {
			try (Git git = initRepo()) {
				setFile(".gitignore").toContent("*.log\n");
				addAndCommit(git);
				setFile("moduleA/keep.txt").toContent("x");
				File elsewhere = setFile("moduleB/x.log").toContent("x");
				assertThat(GitIgnoredFiles.under(newFile("moduleA")).isIgnored(elsewhere)).isFalse();
			}
		}
	}

	@Nested
	@DisplayName("given edge cases")
	class EdgeCases {
		@Test
		@DisplayName("when the root dir is not inside a git repository, then a clear error is thrown")
		void notAGitRepositoryFails() throws IOException {
			File file = setFile("a.txt").toContent("x");
			assertThatThrownBy(() -> underRoot().isIgnored(file))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("requires a git repository");
		}

		@Test
		@DisplayName("when the instance is serialized and deserialized, then it still answers correctly")
		void survivesSerialization() throws Exception {
			try (Git git = initRepo()) {
				setFile(".gitignore").toContent("*.log\n");
				addAndCommit(git);
				File log = setFile("debug.log").toContent("x");
				GitIgnoredFiles original = underRoot();
				assertThat(original.isIgnored(log)).isTrue(); // computes and caches state

				GitIgnoredFiles copy = roundTrip(original);
				assertThat(copy.getRootDir()).isEqualTo(original.getRootDir());
				assertThat(copy.isIgnored(log)).isTrue();
			}
		}
	}

	private static GitIgnoredFiles roundTrip(GitIgnoredFiles original) throws IOException, ClassNotFoundException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
			out.writeObject(original);
		}
		try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
			return (GitIgnoredFiles) in.readObject();
		}
	}
}
