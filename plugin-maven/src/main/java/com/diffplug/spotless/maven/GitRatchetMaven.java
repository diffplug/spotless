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
package com.diffplug.spotless.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.FileTreeIterator;

import com.diffplug.spotless.extra.GitRatchet;

final class GitRatchetMaven extends GitRatchet<File> {

	private GitRatchetMaven() {}

	@Override
	protected File getDir(File project) {
		return project;
	}

	@Override
	protected File getParent(File project) {
		return project.getParentFile();
	}

	private static volatile GitRatchetMaven instance = new GitRatchetMaven();

	static GitRatchetMaven instance() {
		if (instance == null) {
			synchronized (GitRatchetMaven.class) {
				if (instance == null) {
					instance = new GitRatchetMaven();
				}
			}
		}
		return instance;
	}

	List<String> getDirtyFiles(File baseDir, String ratchetFrom) throws IOException {
		Repository repository = repositoryFor(baseDir);
		ObjectId sha = rootTreeShaOf(baseDir, ratchetFrom);

		IndexDiff indexDiff = new IndexDiff(repository, sha, new FileTreeIterator(repository));
		indexDiff.diff();

		String workTreePath = repository.getWorkTree().getPath();
		Path baseDirPath = Path.of(baseDir.getPath());

		Set<String> dirtyPaths = new HashSet<>(indexDiff.getChanged());
		dirtyPaths.addAll(indexDiff.getAdded());
		dirtyPaths.addAll(indexDiff.getConflicting());
		dirtyPaths.addAll(indexDiff.getUntracked());

		for (String path : indexDiff.getModified()) {
			if (!dirtyPaths.add(path)) {
				// File differs to index both in working tree and local repository,
				// which means the working tree and local repository versions may be equal
				if (isClean(baseDir, sha, path)) {
					dirtyPaths.remove(path);
				}
			}
		}
		for (String path : indexDiff.getRemoved()) {
			if (dirtyPaths.contains(path)) {
				// A removed file can also be untracked, if a new file with the same name has been created.
				// This file may be identical to the one in the local repository.
				if (isClean(baseDir, sha, path)) {
					dirtyPaths.remove(path);
				}
			}
		}
		// A file can be modified in the index but removed in the tree
		dirtyPaths.removeAll(indexDiff.getMissing());

		return dirtyPaths.stream()
				.map(path -> baseDirPath.relativize(Path.of(workTreePath, path)).toString())
				.collect(Collectors.toList());
	}
}
