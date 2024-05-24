/*
 * Copyright 2020-2024 DiffPlug
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
package com.diffplug.spotless.npm;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public class NpmPathResolver implements Serializable {

	private final File explicitNpmExecutable;

	private final File explicitNodeExecutable;

	private final File explicitNpmrcFile;

	private final List<File> additionalNpmrcLocations;

	public NpmPathResolver(File explicitNpmExecutable, File explicitNodeExecutable, File explicitNpmrcFile, List<File> additionalNpmrcLocations) {
		this.explicitNpmExecutable = explicitNpmExecutable;
		this.explicitNodeExecutable = explicitNodeExecutable;
		this.explicitNpmrcFile = explicitNpmrcFile;
		this.additionalNpmrcLocations = List.copyOf(additionalNpmrcLocations);
	}

	/**
	 * Finds the npm executable to use.
	 * <br>
	 * Either the explicit npm executable is returned, or - if an explicit node executable is configured - tries to find
	 * the npm executable relative to the node executable.
	 * Falls back to looking for npm on the user's system using {@link NpmExecutableResolver}
	 *
	 * @return the npm executable to use
	 * @throws IllegalStateException if no npm executable could be found
	 */
	public File resolveNpmExecutable() {
		if (this.explicitNpmExecutable != null) {
			return this.explicitNpmExecutable;
		}
		if (this.explicitNodeExecutable != null) {
			File nodeExecutableCandidate = new File(this.explicitNodeExecutable.getParentFile(), NpmExecutableResolver.npmExecutableName());
			if (nodeExecutableCandidate.canExecute()) {
				return nodeExecutableCandidate;
			}
		}
		return NpmExecutableResolver.tryFind()
				.orElseThrow(() -> new IllegalStateException("Can't automatically determine npm executable and none was specifically supplied!\n\n" + NpmExecutableResolver.explainMessage()));
	}

	/**
	 * Finds the node executable to use.
	 * <br>
	 * Either the explicit node executable is returned, or tries to find the node executable relative to the npm executable
	 * found by {@link #resolveNpmExecutable()}.
	 * @return the node executable to use
	 * @throws IllegalStateException if no node executable could be found
	 */
	public File resolveNodeExecutable() {
		if (this.explicitNodeExecutable != null) {
			return this.explicitNodeExecutable;
		}
		File npmExecutable = resolveNpmExecutable();
		return NodeExecutableResolver.tryFindNextTo(npmExecutable)
				.orElseThrow(() -> new IllegalStateException("Can't automatically determine node executable and none was specifically supplied!\n\n" + NodeExecutableResolver.explainMessage()));
	}

	public String resolveNpmrcContent() {
		File npmrcFile = Optional.ofNullable(this.explicitNpmrcFile)
				.orElseGet(() -> new NpmrcResolver(additionalNpmrcLocations).tryFind()
						.orElse(null));
		if (npmrcFile != null) {
			return NpmResourceHelper.readUtf8StringFromFile(npmrcFile);
		}
		return null;
	}

}
