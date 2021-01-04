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
package com.diffplug.spotless.extra;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.IndexDiffFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.FS;

import com.diffplug.common.base.Errors;
import com.diffplug.common.collect.HashBasedTable;
import com.diffplug.common.collect.Table;
import com.diffplug.spotless.FileSignature;

/**
 * How to use:
 * - For best performance, you should have one instance of GitRatchet, shared by all projects.
 * - Use {@link #rootTreeShaOf(Object, String)} to turn `origin/master` into the SHA of the tree object at that reference
 * - Use {@link #isClean(Object, ObjectId, File)} to see if the given file is "git clean" relative to that tree
 * - If you have up-to-date checking and want the best possible performance, use {@link #subtreeShaOf(Object, ObjectId)} to optimize up-to-date checks on a per-project basis.
 */
public abstract class GitRatchet<Project> implements AutoCloseable {

	public boolean isClean(Project project, ObjectId treeSha, File file) throws IOException {
		Repository repo = repositoryFor(project);
		String relativePath = FileSignature.pathNativeToUnix(repo.getWorkTree().toPath().relativize(file.toPath()).toString());
		return isClean(project, treeSha, relativePath);
	}

	/**
	 * This is the highest-level method, which all the others serve.  Given the sha
	 * of a git tree (not a commit!), and the file in question, this method returns
	 * true if that file is clean relative to that tree.  A naive implementation of this
	 * could be verrrry slow, so the rest of this is about speeding this up.
	 */
	public boolean isClean(Project project, ObjectId treeSha, String relativePathUnix) throws IOException {
		Repository repo = repositoryFor(project);

		// TODO: should be cached-per-repo if it is thread-safe, or per-repo-per-thread if it is not
		DirCache dirCache = repo.readDirCache();

		try (TreeWalk treeWalk = new TreeWalk(repo)) {
			treeWalk.setRecursive(true);
			treeWalk.addTree(treeSha);
			treeWalk.addTree(new DirCacheIterator(dirCache));
			treeWalk.addTree(new FileTreeIterator(repo));
			treeWalk.setFilter(AndTreeFilter.create(
					PathFilter.create(relativePathUnix),
					new IndexDiffFilter(INDEX, WORKDIR)));

			if (!treeWalk.next()) {
				// the file we care about is git clean
				return true;
			} else {
				AbstractTreeIterator treeIterator = treeWalk.getTree(TREE, AbstractTreeIterator.class);
				DirCacheIterator dirCacheIterator = treeWalk.getTree(INDEX, DirCacheIterator.class);
				WorkingTreeIterator workingTreeIterator = treeWalk.getTree(WORKDIR, WorkingTreeIterator.class);

				boolean hasTree = treeIterator != null;
				boolean hasDirCache = dirCacheIterator != null;

				if (!hasTree) {
					// it's not in the tree, so it was added
					return false;
				} else {
					if (hasDirCache) {
						boolean treeEqualsIndex = treeIterator.idEqual(dirCacheIterator) && treeIterator.getEntryRawMode() == dirCacheIterator.getEntryRawMode();
						boolean indexEqualsWC = !workingTreeIterator.isModified(dirCacheIterator.getDirCacheEntry(), true, treeWalk.getObjectReader());
						if (treeEqualsIndex != indexEqualsWC) {
							// if one is equal and the other isn't, then it has definitely changed
							return false;
						} else if (treeEqualsIndex) {
							// this means they are all equal to each other, which should never happen
							// the IndexDiffFilter should keep those out of the TreeWalk entirely
							throw new IllegalStateException("Index status for " + relativePathUnix + " against treeSha " + treeSha + " is invalid.");
						} else {
							// they are all unique
							// we have to check manually
							return worktreeIsCleanCheckout(treeWalk);
						}
					} else {
						// no dirCache, so we will compare the tree to the workdir manually
						return worktreeIsCleanCheckout(treeWalk);
					}
				}
			}
		}
	}

	/** Returns true if the worktree file is a clean checkout of head (possibly smudged). */
	private static boolean worktreeIsCleanCheckout(TreeWalk treeWalk) {
		return treeWalk.idEqual(TREE, WORKDIR);
	}

	private final static int TREE = 0;
	private final static int INDEX = 1;
	private final static int WORKDIR = 2;

	Map<Project, Repository> gitRoots = new HashMap<>();
	Table<Repository, String, ObjectId> rootTreeShaCache = HashBasedTable.create();
	Map<Project, ObjectId> subtreeShaCache = new HashMap<>();

	/**
	 * The first part of making this fast is finding the appropriate git repository quickly.  Because of composite
	 * builds and submodules, it's quite possible that a single Gradle project will span across multiple git repositories.
	 * We cache the Repository for every Project in `gitRoots`, and use dynamic programming to populate it.
	 */
	protected Repository repositoryFor(Project project) throws IOException {
		Repository repo = gitRoots.get(project);
		if (repo == null) {
			if (isGitRoot(getDir(project))) {
				repo = createRepo(getDir(project));
			} else {
				Project parentProj = getParent(project);
				if (parentProj == null) {
					repo = traverseParentsUntil(getDir(project).getParentFile(), null);
					if (repo == null) {
						throw new IllegalArgumentException("Cannot find git repository in any parent directory");
					}
				} else {
					repo = traverseParentsUntil(getDir(project).getParentFile(), getDir(parentProj));
					if (repo == null) {
						repo = repositoryFor(parentProj);
					}
				}
			}
			gitRoots.put(project, repo);
		}
		return repo;
	}

	protected abstract File getDir(Project project);

	protected abstract @Nullable Project getParent(Project project);

	private static @Nullable Repository traverseParentsUntil(File startWith, File file) throws IOException {
		while (startWith != null && !Objects.equals(startWith, file)) {
			if (isGitRoot(startWith)) {
				return createRepo(startWith);
			} else {
				startWith = startWith.getParentFile();
			}
		}
		return null;
	}

	/**
	 * When populating a new submodule directory with "git submodule init", the $GIT_DIR meta-information directory
	 * for submodules is created inside $GIT_DIR/modules// directory of the super-project
	 * and referenced via the git-file mechanism.
	 */
	private static @Nullable File getDotGitDir(File dir, String dotGit) {
		File dotGitPath = new File(dir, dotGit);

		if (dotGitPath.isDirectory()) {
			return dotGitPath;
		} else if (dotGitPath.isFile()) {
			try {
				String relativePath = new String(Files.readAllBytes(dotGitPath.toPath()), StandardCharsets.UTF_8)
						.split(":")[1].trim();
				return getDotGitDir(dir, relativePath);
			} catch (IOException e) {
				System.err.println("failed to parse git meta: " + e.getMessage());
				return null;
			}
		} else {
			return null;
		}
	}

	private static boolean isGitRoot(File dir) {
		File dotGit = getDotGitDir(dir, Constants.DOT_GIT);
		return dotGit != null && RepositoryCache.FileKey.isGitRepository(dotGit, FS.DETECTED);
	}

	static Repository createRepo(File dir) throws IOException {
		return FileRepositoryBuilder.create(getDotGitDir(dir, Constants.DOT_GIT));
	}

	/**
	 * Fast way to return treeSha of the given ref against the git repository which stores the given project.
	 * Because of parallel project evaluation, there may be races here, so we synchronize on ourselves.  However, this method
	 * is the only method which can trigger any changes, and it is only called during project evaluation.  That means our state
	 * is final/read-only during task execution, so we don't need any locks during the heavy lifting.
	 */
	public synchronized ObjectId rootTreeShaOf(Project project, String reference) {
		try {
			Repository repo = repositoryFor(project);
			ObjectId treeSha = rootTreeShaCache.get(repo, reference);
			if (treeSha == null) {
				try (RevWalk revWalk = new RevWalk(repo)) {
					ObjectId commitSha = repo.resolve(reference);
					if (commitSha == null) {
						throw new IllegalArgumentException("No such reference '" + reference + "'");
					}

					RevCommit ratchetFrom = revWalk.parseCommit(commitSha);
					RevCommit head = revWalk.parseCommit(repo.resolve(Constants.HEAD));

					revWalk.setRevFilter(RevFilter.MERGE_BASE);
					revWalk.markStart(ratchetFrom);
					revWalk.markStart(head);

					RevCommit mergeBase = revWalk.next();
					treeSha = Optional.ofNullable(mergeBase).orElse(ratchetFrom).getTree();
				}
				rootTreeShaCache.put(repo, reference, treeSha);
			}
			return treeSha;
		} catch (IOException e) {
			throw Errors.asRuntime(e);
		}
	}

	/**
	 * Returns the sha of the git subtree which represents the root of the given project, or {@link ObjectId#zeroId()}
	 * if there is no git subtree at the project root.
	 */
	public synchronized ObjectId subtreeShaOf(Project project, ObjectId rootTreeSha) {
		try {
			ObjectId subtreeSha = subtreeShaCache.get(project);
			if (subtreeSha == null) {
				Repository repo = repositoryFor(project);
				File directory = getDir(project);
				if (repo.getWorkTree().equals(directory)) {
					subtreeSha = rootTreeSha;
				} else {
					String subpath = FileSignature.pathNativeToUnix(repo.getWorkTree().toPath().relativize(directory.toPath()).toString());
					TreeWalk treeWalk = TreeWalk.forPath(repo, subpath, rootTreeSha);
					subtreeSha = treeWalk == null ? ObjectId.zeroId() : treeWalk.getObjectId(0);
				}
				subtreeShaCache.put(project, subtreeSha);
			}
			return subtreeSha;
		} catch (IOException e) {
			throw Errors.asRuntime(e);
		}
	}

	@Override
	public void close() {
		gitRoots.values().stream()
				.distinct()
				.forEach(Repository::close);
	}
}
