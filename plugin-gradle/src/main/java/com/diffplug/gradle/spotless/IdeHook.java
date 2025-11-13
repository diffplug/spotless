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
package com.diffplug.gradle.spotless;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.annotation.Nullable;

import org.gradle.api.Project;
import org.gradle.api.provider.ProviderFactory;

import com.diffplug.common.base.Errors;
import com.diffplug.common.io.ByteStreams;
import com.diffplug.spotless.DirtyState;
import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.NoLambda;

final class IdeHook {
	static class State extends NoLambda.EqualityBasedOnSerialization {
		final @Nullable String path;
		final boolean useStdIn;
		final boolean useStdOut;

		State(Project project) {
			ProviderFactory providers = project.getProviders();
			path = providers.gradleProperty(PROPERTY).getOrNull();
			if (path != null) {
				useStdIn = providers.gradleProperty(USE_STD_IN).isPresent();
				useStdOut = providers.gradleProperty(USE_STD_OUT).isPresent();
			} else {
				useStdIn = false;
				useStdOut = false;
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
		File file = new File(state.path);
		if (!file.isAbsolute()) {
			System.err.println("Argument passed to " + PROPERTY + " must be an absolute path");
			return;
		}
		if (spotlessTask.getTarget().contains(file)) {
			GitRatchetGradle ratchet = spotlessTask.getRatchet();
			try (Formatter formatter = spotlessTask.buildFormatter()) {
				if (ratchet != null) {
					if (ratchet.isClean(spotlessTask.getProjectDir().get().getAsFile(), spotlessTask.getRootTreeSha(), file)) {
						dumpIsClean();
						return;
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
