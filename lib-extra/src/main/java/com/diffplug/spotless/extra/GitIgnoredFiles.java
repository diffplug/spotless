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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;

import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import com.diffplug.spotless.FileSignature;

/**
 * Answers whether git considers a file to be ignored, using the same rules as {@code git status}:
 * a file is ignored if it matches a pattern from any {@code .gitignore}, from {@code .git/info/exclude}
 * or from the {@code core.excludesFile} configured in git, <b>and</b> it is not tracked in the index.
 * Tracked files are never ignored, even when they match an ignore pattern.
 * <p>
 * The ignored paths below the root directory are computed once, lazily, on the first call to
 * {@link #isIgnored(File)} and are cached for the lifetime of this instance. Ignored directories are not
 * descended into, so large ignored folders such as {@code node_modules} or {@code build} are cheap.
 * <p>
 * Instances are thread-safe. They are {@link Serializable} so that build tools can cache their configuration,
 * the computed state is not serialized and is recomputed after deserialization.
 */
public final class GitIgnoredFiles implements Serializable {
	private static final long serialVersionUID = 1L;

	private final File rootDir;
	private transient volatile @Nullable State state;

	private GitIgnoredFiles(File rootDir) {
		this.rootDir = Objects.requireNonNull(rootDir).getAbsoluteFile();
	}

	/**
	 * Creates an instance which answers questions about files below {@code rootDir}, which must be inside
	 * a git working tree. The repository is not read until {@link #isIgnored(File)} is first called.
	 */
	public static GitIgnoredFiles under(File rootDir) {
		return new GitIgnoredFiles(rootDir);
	}

	/** The directory whose files this instance answers questions about. */
	public File getRootDir() {
		return rootDir;
	}

	/**
	 * Returns true if git ignores the given file. Files outside of the git working tree, or outside of
	 * {@link #getRootDir()}, are never considered ignored.
	 *
	 * @throws IllegalArgumentException if {@link #getRootDir()} is not inside a git working tree
	 */
	public boolean isIgnored(File file) {
		return state().isIgnored(file);
	}

	/** Negation of {@link #isIgnored(File)}, handy as a method reference for filtering. */
	public boolean isNotIgnored(File file) {
		return !isIgnored(file);
	}

	private State state() {
		State result = state;
		if (result == null) {
			synchronized (this) {
				result = state;
				if (result == null) {
					try {
						result = State.compute(rootDir);
					} catch (IOException e) {
						throw new IllegalStateException("Unable to determine git-ignored files below " + rootDir, e);
					}
					state = result;
				}
			}
		}
		return result;
	}

	private static final class State {
		private final Path workTree;
		/** Unix-style paths relative to the work tree, which are either ignored files or fully-ignored directories. */
		private final Set<String> ignoredPaths;

		private State(Path workTree, Set<String> ignoredPaths) {
			this.workTree = workTree;
			this.ignoredPaths = ignoredPaths;
		}

		static State compute(File rootDir) throws IOException {
			GitWorkarounds.RepositorySpecificResolver resolver = GitWorkarounds.fileRepositoryResolverForProject(rootDir);
			if (resolver.getGitDir() == null) {
				throw new IllegalArgumentException("Respecting .gitignore requires a git repository, but none was found in "
						+ rootDir + " or any of its parent directories");
			}
			try (Repository repo = resolver.build()) {
				if (repo.isBare()) {
					throw new IllegalArgumentException("Respecting .gitignore requires a git working tree, but " + repo.getDirectory() + " is bare");
				}
				Path workTree = repo.getWorkTree().toPath().toAbsolutePath().normalize();
				// ObjectId == null: compare index to working tree only, we don't need HEAD
				IndexDiff indexDiff = new IndexDiff(repo, (ObjectId) null, new FileTreeIterator(repo));
				String subPath = toUnixRelative(workTree, rootDir.toPath());
				if (subPath != null && !subPath.isEmpty()) {
					indexDiff.setFilter(PathFilter.create(subPath));
				}
				indexDiff.diff();
				return new State(workTree, Set.copyOf(indexDiff.getIgnoredNotInIndex()));
			}
		}

		boolean isIgnored(File file) {
			if (ignoredPaths.isEmpty()) {
				return false;
			}
			String path = toUnixRelative(workTree, file.toPath());
			if (path == null || path.isEmpty()) {
				return false;
			}
			// the file itself, or any of its parent directories, may be the ignored entry
			while (true) {
				if (ignoredPaths.contains(path)) {
					return true;
				}
				int lastSlash = path.lastIndexOf('/');
				if (lastSlash == -1) {
					return false;
				}
				path = path.substring(0, lastSlash);
			}
		}

		/** Returns the unix-style relative path of {@code file} within {@code workTree}, or null if it is outside. */
		private static @Nullable String toUnixRelative(Path workTree, Path file) {
			Path absolute = file.toAbsolutePath().normalize();
			if (!absolute.startsWith(workTree)) {
				return null;
			}
			return FileSignature.pathNativeToUnix(workTree.relativize(absolute).toString());
		}
	}
}
