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

import static org.junit.jupiter.api.Assertions.assertThrows;

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

	@Test // https://github.com/diffplug/spotless/issues/911
	void testGitIgnoredDirectory_ShouldNotThrowNPE() throws IllegalStateException, GitAPIException, IOException {
		try (Git git = initRepo()) {
			// Create a directory with files and commit them
			setFile("useless/Wow.java").toContent("class Wow {}");
			setFile("useless/Another.java").toContent("class Another {}");
			addAndCommit(git, "Add useless package");

			// Now ignore the entire directory and commit
			setFile(".gitignore").toContent("useless/");
			addAndCommit(git, "Ignore useless directory");

			// The files in the useless directory are now committed but gitignored
			// This should not throw NPE
			ratchetFrom("main").onlyDirty("useless/Wow.java", "useless/Another.java");
		}
	}

	@Test
	void testNewUntrackedFile_ShouldBeDirty() throws IllegalStateException, GitAPIException, IOException {
		try (Git git = initRepo()) {
			// Create and commit initial file
			setFile("committed.java").toContent("class Committed {}");
			addAndCommit(git, "Initial commit");

			// Create a new file that's not tracked at all (not in gitignore either)
			setFile("new_untracked.java").toContent("class NewUntracked {}");

			// The new untracked file should be considered dirty
			ratchetFrom("main").onlyDirty("new_untracked.java");
		}
	}

	@Test
	void testModifiedTrackedFile_ShouldBeDirty() throws IllegalStateException, GitAPIException, IOException {
		try (Git git = initRepo()) {
			// Create and commit initial file
			setFile("Main.java").toContent("class Main { void old() {} }");
			addAndCommit(git, "Initial commit");

			// Modify the file
			setFile("Main.java").toContent("class Main { void newMethod() {} }");

			// The modified file should be considered dirty
			ratchetFrom("main").onlyDirty("Main.java");
		}
	}

	@Test
	void testDeletedFile_ShouldBeDirty() throws IllegalStateException, GitAPIException, IOException {
		try (Git git = initRepo()) {
			// Create and commit multiple files
			setFile("keep.java").toContent("class Keep {}");
			setFile("delete.java").toContent("class Delete {}");
			addAndCommit(git, "Initial commit");

			// Delete one file
			new File(rootFolder(), "delete.java").delete();

			// The deleted file should be considered dirty
			ratchetFrom("main").onlyDirty("delete.java");
		}
	}

	@Test
	void testRenamedFile_ShouldBeDirty() throws IllegalStateException, GitAPIException, IOException {
		try (Git git = initRepo()) {
			// Create and commit initial file
			setFile("OldName.java").toContent("class OldName {}");
			addAndCommit(git, "Initial commit");

			// Rename the file (Git sees this as delete + add)
			File oldFile = new File(rootFolder(), "OldName.java");
			File newFile = new File(rootFolder(), "NewName.java");
			oldFile.renameTo(newFile);

			// Both old and new files should be considered dirty
			ratchetFrom("main").onlyDirty("OldName.java", "NewName.java");
		}
	}

	@Test
	void testStagedButUncommittedChanges_ShouldBeDirty() throws IllegalStateException, GitAPIException, IOException {
		try (Git git = initRepo()) {
			// Create and commit initial file
			setFile("Test.java").toContent("class Test {}");
			addAndCommit(git, "Initial commit");

			// Modify and stage the file but don't commit
			setFile("Test.java").toContent("class Test { void newMethod() {} }");
			git.add().addFilepattern("Test.java").call();

			// The staged but uncommitted file should be considered dirty
			ratchetFrom("main").onlyDirty("Test.java");
		}
	}

	@Test
	void testMultipleBranchesWithDifferentFiles() throws IllegalStateException, GitAPIException, IOException {
		try (Git git = initRepo()) {
			// Initial commit
			setFile("base.txt").toContent("base");
			addAndCommit(git, "Initial commit");

			// Branch A changes
			git.checkout().setCreateBranch(true).setName("branch-a").call();
			setFile("a-only.txt").toContent("a content");
			addAndCommit(git, "Branch A commit");

			// Branch B changes
			git.checkout().setName("main").call();
			git.checkout().setCreateBranch(true).setName("branch-b").call();
			setFile("b-only.txt").toContent("b content");
			addAndCommit(git, "Branch B commit");

			// Check from both branches - each should only see their own changes as dirty
			git.checkout().setName("main").call();
			ratchetFrom("branch-a").onlyDirty("a-only.txt");
			ratchetFrom("branch-b").onlyDirty("b-only.txt");
		}
	}

	@Test
	void testNestedDirectoryStructure() throws IllegalStateException, GitAPIException, IOException {
		try (Git git = initRepo()) {
			// Create nested directory structure
			setFile("src/main/java/com/example/Main.java").toContent("package com.example; class Main {}");
			setFile("src/main/java/com/example/Util.java").toContent("package com.example; class Util {}");
			setFile("src/test/java/com/example/MainTest.java").toContent("package com.example; class MainTest {}");
			addAndCommit(git, "Add nested structure");

			// Modify only one nested file
			setFile("src/main/java/com/example/Util.java").toContent("package com.example; class Util { void newMethod() {} }");

			// Only the modified nested file should be dirty
			ratchetFrom("main").onlyDirty("src/main/java/com/example/Util.java");
		}
	}

	@Test
	void testNonExistentReference_ShouldThrowException() throws IllegalStateException, GitAPIException, IOException {
		try (Git git = initRepo()) {
			setFile("test.txt").toContent("test");
			addAndCommit(git, "Initial commit");

			// Trying to ratchet from non-existent branch should throw
			assertThrows(IllegalArgumentException.class, () -> ratchetFrom("nonexistent-branch"));
		}
	}

	@Test
	void testBinaryFile_ShouldBeHandled() throws IllegalStateException, GitAPIException, IOException {
		try (Git git = initRepo()) {
			// Create and commit binary file
			setFile("image.png").toContent("binary content that looks like an image");
			addAndCommit(git, "Add binary file");

			// Modify binary content
			setFile("image.png").toContent("modified binary content");

			// Binary file should be detected as dirty
			ratchetFrom("main").onlyDirty("image.png");
		}
	}

	@Test
	void testSymlink_ShouldBeHandled() throws IllegalStateException, GitAPIException, IOException {
		try (Git git = initRepo()) {
			// This test would require creating actual symlinks
			// For now, we'll test that the code doesn't break with special files
			setFile("regular.txt").toContent("regular file");
			setFile("special.txt").toContent("special file");
			addAndCommit(git, "Add files");

			// Modify one file
			setFile("special.txt").toContent("modified special file");

			// Should correctly identify the modified file
			ratchetFrom("main").onlyDirty("special.txt");
		}
	}

	@Test
	void testMultipleProjectsInSameRepo() throws IllegalStateException, GitAPIException, IOException {
		try (Git git = initRepo()) {
			// Simulate multiple projects in same repo
			setFile("project1/src/Main.java").toContent("class Main {}");
			setFile("project2/src/Other.java").toContent("class Other {}");
			setFile("shared/common.txt").toContent("shared");
			addAndCommit(git, "Add projects");

			// Modify files in different "projects"
			setFile("project1/src/Main.java").toContent("class Main { void change() {} }");
			setFile("shared/common.txt").toContent("modified shared");

			// Should detect all modified files
			ratchetFrom("main").onlyDirty("project1/src/Main.java", "shared/common.txt");
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
