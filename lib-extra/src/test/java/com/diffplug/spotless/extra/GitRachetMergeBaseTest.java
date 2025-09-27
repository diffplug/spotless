/*
 * Copyright 2020-2025 DiffPlug
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
import java.util.Arrays;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.RefDatabase;
import org.junit.jupiter.api.Test;

import com.diffplug.spotless.ClearGitConfig;
import com.diffplug.spotless.ResourceHarness;

@ClearGitConfig
class GitRachetMergeBaseTest extends ResourceHarness {
	@Test
	void test() throws IllegalStateException, GitAPIException, IOException {
		try (Git git = initRepo()) {
			setFile("mine.txt").toContent("init");
			setFile("untouched.txt").toContent("init");
			addAndCommit(git, "init");

			git.checkout().setCreateBranch(true).setName("rem-branch").call();
			// so at the point where we start work, we are clean relative to the remote
			ratchetFrom("main", "rem-branch").allClean();

			// but when the remote changes
			setFile("untouched.txt").toContent("changed");
			addAndCommit(git, "remote");

			// it shouldn't affect files that we haven't changed
			git.checkout().setName("main").call();
			ratchetFrom("main", "rem-branch").allClean();

			// and when we work, it should continue to only affect our own work
			setFile("mine.txt").toContent("changed");
			ratchetFrom("main", "rem-branch").onlyDirty("mine.txt");
		}
	}

	static class GitRatchetSimple extends GitRatchet<File> {
		@Override
		protected File getDir(File project) {
			return project;
		}

		@Override
		protected File getParent(File project) {
			return project.getParentFile();
		}
	}

	Asserter ratchetFrom(String... ratchetFroms) {
		return new Asserter(ratchetFroms);
	}

	class Asserter {
		final GitRatchetSimple ratchet = new GitRatchetSimple();
		final String[] ratchetFroms;
		final ObjectId[] shas;

		Asserter(String... ratchetFrom) {
			this.ratchetFroms = ratchetFrom;
			this.shas = Arrays.stream(ratchetFrom)
					.map(from -> ratchet.rootTreeShaOf(rootFolder(), from))
					.toArray(ObjectId[]::new);
		}

		private void assertClean(int i, String filename, boolean expected) throws IOException {
			boolean actual = ratchet.isClean(rootFolder(), shas[i], newFile(filename));
			if (actual != expected) {
				throw new AssertionError("Expected " + filename + " to be " + (expected ? "clean" : "dirty") + " relative to " + ratchetFroms[i]);
			}
		}

		public void allClean() throws IOException {
			onlyDirty();
		}

		public void allDirty() throws IOException {
			String[] filenames = Arrays.stream(rootFolder().listFiles())
					.filter(File::isFile)
					.map(File::getName)
					.toArray(String[]::new);
			onlyDirty(filenames);
		}

		public void onlyDirty(String... filenames) throws IOException {
			List<String> dirtyFiles = Arrays.asList(filenames);
			for (File file : rootFolder().listFiles()) {
				if (!file.isFile()) {
					continue;
				}
				boolean expectedClean = !dirtyFiles.contains(file.getName());
				for (int i = 0; i < shas.length; i++) {
					assertClean(i, file.getName(), expectedClean);
				}
			}
		}
	}

	private Git initRepo() throws IllegalStateException, GitAPIException, IOException {
		Git git = Git.init().setDirectory(rootFolder()).call();
		RefDatabase refDB = git.getRepository().getRefDatabase();
		refDB.newUpdate(Constants.R_HEADS + "main", false).setNewObjectId(ObjectId.zeroId());
		refDB.newUpdate(Constants.HEAD, false).link(Constants.R_HEADS + "main");
		refDB.newUpdate(Constants.R_HEADS + Constants.MASTER, false).delete();
		return git;
	}

	private void addAndCommit(Git git, String message) throws GitAPIException {
		git.add().addFilepattern(".").call();
		git.commit().setMessage(message).call();
	}
}
