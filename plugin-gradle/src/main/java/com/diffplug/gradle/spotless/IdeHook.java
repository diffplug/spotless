/*
 * Copyright 2016-2026 DiffPlug
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
package com.diffplug.gradle.spotless;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.gradle.api.Project;

import com.diffplug.common.base.Errors;
import com.diffplug.common.io.ByteStreams;
import com.diffplug.spotless.DirtyState;
import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.NoLambda;

final class IdeHook {
	static class State extends NoLambda.EqualityBasedOnSerialization {
		final @Nullable List<String> paths;
		final boolean useStdIn;
		final boolean useStdOut;

		State(Project project) {
			var pathsString = project.getProviders().gradleProperty(PROPERTY).getOrNull();
			if (pathsString != null) {
				useStdIn = project.getProviders().gradleProperty(USE_STD_IN).isPresent();
				useStdOut = project.getProviders().gradleProperty(USE_STD_OUT).isPresent();
				paths = Arrays.stream(pathsString.split(","))
						.map(String::trim)
						.filter(s -> !s.isEmpty())
						.collect(Collectors.toList());
			} else {
				useStdIn = false;
				useStdOut = false;
				paths = null;
			}
		}
	}

	static final String PROPERTY = "spotlessIdeHook";
	static final String USE_STD_IN = "spotlessIdeHookUseStdIn";
	static final String USE_STD_OUT = "spotlessIdeHookUseStdOut";

	private static void dumpIsClean() {
		System.err.println("IS CLEAN");
	}

	static void performHook(SpotlessTaskImpl spotlessTask, IdeHook.State state) {
		if (state.paths == null) {
			return;
		}
		if (state.paths.size() > 1 && (state.useStdIn || state.useStdOut)) {
			System.err.println("Using " + USE_STD_IN + " or " + USE_STD_OUT + " with multiple files is not supported");
			return;
		}
		List<File> files = state.paths.stream().map(File::new).toList();
		for (File file : files) {
			if (!file.isAbsolute()) {
				System.err.println("Argument passed to " + PROPERTY + " must be one or multiple absolute paths");
				return;
			}
		}

		var matchedFiles = files.stream().filter(file -> spotlessTask.getTarget().contains(file)).toList();
		for (File file : matchedFiles) {
			GitRatchetGradle ratchet = spotlessTask.getRatchet();
			try (Formatter formatter = spotlessTask.buildFormatter()) {
				if (ratchet != null) {
					if (ratchet.isClean(spotlessTask.getProjectDir().get().getAsFile(), spotlessTask.getRootTreeSha(), file)) {
						dumpIsClean();
						continue;
					}
				}
				byte[] bytes;
				if (state.useStdIn) {
					bytes = ByteStreams.toByteArray(System.in);
				} else {
					bytes = Files.readAllBytes(file.toPath());
				}
				DirtyState dirty = DirtyState.of(formatter, file, bytes);
				if (dirty.isClean()) {
					dumpIsClean();
				} else if (dirty.didNotConverge()) {
					System.err.println("DID NOT CONVERGE");
					System.err.println("Run 'spotlessDiagnose' for details https://github.com/diffplug/spotless/blob/main/PADDEDCELL.md");
				} else {
					System.err.println("IS DIRTY");
					if (state.useStdOut) {
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
	}

	private IdeHook() {}
}
