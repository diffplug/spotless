/*
 * Copyright 2020 DiffPlug
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
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

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

	Iterable<String> getDirtyFiles(File baseDir, String ratchetFrom)
			throws IOException, GitAPIException {
		Repository repository = repositoryFor(baseDir);
		ObjectId sha = rootTreeShaOf(baseDir, ratchetFrom);

		ObjectReader oldReader = repository.newObjectReader();
		CanonicalTreeParser oldTree = new CanonicalTreeParser();
		oldTree.reset(oldReader, sha);

		Git git = new Git(repository);
		List<DiffEntry> diffs = git.diff()
				.setShowNameAndStatusOnly(true)
				.setOldTree(oldTree)
				.call();

		String workTreePath = repository.getWorkTree().getPath();
		Path baseDirPath = Paths.get(baseDir.getPath());

		return diffs.stream()
				.map(DiffEntry::getNewPath)
				.map(path -> Paths.get(workTreePath, path))
				.map(path -> baseDirPath.relativize(path).toString())
				.collect(Collectors.toList());
	}
}
