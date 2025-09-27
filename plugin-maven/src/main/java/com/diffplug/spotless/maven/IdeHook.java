/*
 * Copyright 2016-2025 DiffPlug
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
import java.nio.file.Files;

import com.diffplug.common.base.Errors;
import com.diffplug.common.io.ByteStreams;
import com.diffplug.spotless.DirtyState;
import com.diffplug.spotless.Formatter;

final class IdeHook {

	private static void dumpIsClean() {
		System.err.println("IS CLEAN");
	}

	//No need to check ratchet (using isClean()) as it is performed in Gradle's IDE hook, since we have already gathered the available git files from ratchet.
	static void performHook(Iterable<File> projectFiles, Formatter formatter, String path, boolean spotlessIdeHookUseStdIn, boolean spotlessIdeHookUseStdOut) {
		File file = new File(path);
		if (!file.isAbsolute()) {
			System.err.println("Argument passed to spotlessIdeHook must be an absolute path");
			return;
		}

		if (!projectContainsFile(projectFiles, file)) {
			return;
		}

		try {
			byte[] bytes;
			if (spotlessIdeHookUseStdIn) {
				bytes = ByteStreams.toByteArray(System.in);
			} else {
				bytes = Files.readAllBytes(file.toPath());
			}
			DirtyState dirty = DirtyState.of(formatter, file, bytes);
			if (dirty.isClean()) {
				dumpIsClean();
			} else if (dirty.didNotConverge()) {
				System.err.println("DID NOT CONVERGE");
				System.err.println("See details https://github.com/diffplug/spotless/blob/main/PADDEDCELL.md");
			} else {
				System.err.println("IS DIRTY");
				if (spotlessIdeHookUseStdOut) {
					dirty.writeCanonicalTo(System.out);
				} else {
					dirty.writeCanonicalTo(file);
				}
			}
		} catch (IOException e) {
			e.printStackTrace(System.err);
			throw Errors.asRuntime(e);
		} finally {
			System.err.close();
			System.out.close();
		}
	}

	private static boolean projectContainsFile(Iterable<File> projectFiles, File file) {
		for (File projectFile : projectFiles) {
			if (projectFile.equals(file)) {
				return true;
			}
		}
		return false;
	}

	private IdeHook() {}
}
