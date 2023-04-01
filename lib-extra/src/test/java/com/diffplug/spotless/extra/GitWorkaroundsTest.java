/*
 * Copyright 2022 DiffPlug
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

import org.assertj.core.api.Assertions;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.extra.GitWorkarounds.RepositorySpecificResolver;

class GitWorkaroundsTest extends ResourceHarness {
	@Test
	void inline() throws IOException, GitAPIException {
		File projectFolder = newFolder("project");
		Git.init().setDirectory(projectFolder).call();

		RepositorySpecificResolver repositorySpecificResolver = GitWorkarounds.fileRepositoryResolverForProject(projectFolder);
		Assertions.assertThat(repositorySpecificResolver.getGitDir()).isEqualTo(new File(projectFolder, ".git"));
	}

	@Test
	void external() throws IOException, GitAPIException {
		File projectFolder = newFolder("project");
		File gitDir = newFolder("project.git");
		Git.init().setDirectory(projectFolder).setGitDir(gitDir).call();

		RepositorySpecificResolver repositorySpecificResolver = GitWorkarounds.fileRepositoryResolverForProject(projectFolder);
		Assertions.assertThat(repositorySpecificResolver.getGitDir()).isEqualTo(gitDir);
	}

	@Nested
	@DisplayName("Worktrees")
	class Worktrees {
		private File project1Tree;
		private File project1GitDir;
		private File project2Tree;
		private File project2GitDir;
		private File commonGitDir;

		@BeforeEach
		void setUp() throws IOException, GitAPIException {
			project1Tree = newFolder("project-w1");
			project2Tree = newFolder("project-w2");
			commonGitDir = newFolder("project.git");
			Git.init().setDirectory(project1Tree).setGitDir(commonGitDir).call();

			// Setup worktrees manually since JGit does not support it
			newFolder("project.git/worktrees/");

			project1GitDir = newFolder("project.git/worktrees/project-w1/");
			setFile("project.git/worktrees/project-w1/gitdir").toContent(project1Tree.getAbsolutePath() + "/.git");
			setFile("project.git/worktrees/project-w1/commondir").toContent("../.."); // Relative path
			setFile("project-w1/.git").toContent("gitdir: " + project1GitDir.getAbsolutePath());

			project2GitDir = newFolder("project.git/worktrees/project-w2/");
			setFile("project.git/worktrees/project-w2/gitdir").toContent(project2Tree.getAbsolutePath() + "/.git");
			setFile("project.git/worktrees/project-w2/commondir").toContent(commonGitDir.getAbsolutePath()); // Absolute path
			setFile("project-w2/.git").toContent("gitdir: " + project2GitDir.getAbsolutePath());
		}

		@Test
		void resolveGitDir() {
			// Test worktree 1
			{
				RepositorySpecificResolver repositorySpecificResolver = GitWorkarounds.fileRepositoryResolverForProject(project1Tree);
				Assertions.assertThat(repositorySpecificResolver.getGitDir()).isEqualTo(project1GitDir);
				Assertions.assertThat(repositorySpecificResolver.resolveWithCommonDir(Constants.CONFIG)).isEqualTo(new File(commonGitDir, Constants.CONFIG));
			}

			// Test worktree 2
			{
				RepositorySpecificResolver repositorySpecificResolver = GitWorkarounds.fileRepositoryResolverForProject(project2Tree);
				Assertions.assertThat(repositorySpecificResolver.getGitDir()).isEqualTo(project2GitDir);
				Assertions.assertThat(repositorySpecificResolver.resolveWithCommonDir(Constants.CONFIG)).isEqualTo(new File(commonGitDir, Constants.CONFIG));
			}
		}

		@Test
		void perWorktreeConfig() throws IOException {
			setFile("project.git/config").toLines("[core]", "mySetting = true");

			Assertions.assertThat(getMySetting(project1Tree)).isTrue();
			Assertions.assertThat(getMySetting(project2Tree)).isTrue();

			// Override setting for project 1, but don't enable extension yet
			setFile("project.git/worktrees/project-w1/config.worktree").toLines("[core]", "mySetting = false");

			Assertions.assertThat(getMySetting(project1Tree)).isTrue();
			Assertions.assertThat(getMySetting(project2Tree)).isTrue();

			// Enable extension
			setFile("project.git/config").toLines("[core]", "mySetting = true", "[extensions]", "worktreeConfig = true");

			Assertions.assertThat(getMySetting(project1Tree)).isFalse(); // Should now be overridden by config.worktree
			Assertions.assertThat(getMySetting(project2Tree)).isTrue();
		}

		private boolean getMySetting(File projectDir) {
			return GitWorkarounds.fileRepositoryResolverForProject(projectDir).getRepositoryConfig().getBoolean("core", "mySetting", false);
		}
	}
}
