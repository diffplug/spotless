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
import java.util.function.Predicate;

import org.eclipse.jgit.lib.ObjectId;

import com.diffplug.common.base.Errors;
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

	/** A predicate which returns only the "git dirty" files. */
	Predicate<File> isGitDirty(File baseDir, String ratchetFrom) {
		ObjectId sha = rootTreeShaOf(baseDir, ratchetFrom);
		return file -> {
			try {
				return !isClean(baseDir, sha, file);
			} catch (IOException e) {
				throw Errors.asRuntime(e);
			}
		};
	}
}
