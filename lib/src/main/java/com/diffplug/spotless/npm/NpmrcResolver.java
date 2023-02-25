/*
 * Copyright 2016-2020 DiffPlug
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Utility class to resolve a {@code .npmrc} file to use in our node config.
 * Tries to find a {@code .npmrc} config file in the following order:
 * <ol>
 *     <li>from System-Property {@code npm.npmrc}</li>
 *     <li>from 0..n projectLocations (specified by arguments)</li>
 *     <li>from the user home directory, resolved via environment variable{@code $HOME}</li>
 * </ol>
 */
class NpmrcResolver {

	private final FileFinder npmrcFileFinder;

	NpmrcResolver(File... projectLocations) {
		this(Arrays.asList(projectLocations));
	}

	NpmrcResolver(List<File> projectLocations) {
		// no instance
		final FileFinder.Builder finderBuilder = FileFinder.finderForFilename(".npmrc")
				.candidateSystemProperty("npm.npmrc");
		projectLocations.forEach(finderBuilder::candidateFileInFolder);
		npmrcFileFinder = finderBuilder
				.candidateEnvironmentPath("HOME")
				.build();
	}

	Optional<File> tryFind() {
		return npmrcFileFinder.tryFind();
	}
}
