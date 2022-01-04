/*
 * Copyright 2020-2022 DiffPlug
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

import javax.annotation.Nullable;

import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

/**
 * Utility methods for Git workarounds.
 */
public class GitWorkarounds {
	private GitWorkarounds() {}

	/**
	 * Finds the .git directory for the given project directory.
	 *
	 * Ordinarily one would just use JGit for this, but it doesn't support worktrees properly.
	 * So this applies an additional workaround for that.
	 *
	 * @param projectDir the project directory.
	 * @return the path to the .git directory.
	 */
	static @Nullable File getDotGitDir(File projectDir) {
		return fileRepositoryBuilderForProject(projectDir).getGitDir();
	}

	/**
	 * Creates a {@link FileRepositoryBuilder} for the given project directory.
	 *
	 * This applies a workaround for JGit not supporting worktrees properly.
	 *
	 * @param projectDir the project directory.
	 * @return the builder.
	 */
	static FileRepositoryBuilder fileRepositoryBuilderForProject(File projectDir) {
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		builder.findGitDir(projectDir);
		File gitDir = builder.getGitDir();
		if (gitDir != null) {
			builder.setGitDir(resolveRealGitDirIfWorktreeDir(gitDir));
		}
		return builder;
	}

	/**
	 * If the dir is a worktree directory (typically .git/worktrees/something) then
	 * returns the actual .git directory.
	 *
	 * @param dir the directory which may be a worktree directory or may be a .git directory.
	 * @return the .git directory.
	 */
	private static File resolveRealGitDirIfWorktreeDir(File dir) {
		File pointerFile = new File(dir, "gitdir");
		if (pointerFile.isFile()) {
			try {
				String content = new String(Files.readAllBytes(pointerFile.toPath()), StandardCharsets.UTF_8).trim();
				return new File(content);
			} catch (IOException e) {
				System.err.println("failed to parse git meta: " + e.getMessage());
				return dir;
			}
		} else {
			return dir;
		}
	}
}
