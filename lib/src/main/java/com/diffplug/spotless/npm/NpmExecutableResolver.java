/*
 * Copyright 2016 DiffPlug
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

import static com.diffplug.spotless.npm.PlatformInfo.OS.WINDOWS;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Utility class to resolve an npm binary to be used by npm-based steps.
 * Tries to find an npm executable in the following order:
 * <ol>
 *     <li>from System-Property {@code npm.exec} (unverified)</li>
 *     <li>from Environment-Properties in the following order:</li>
 *     	<ol>
 *     	    <li> from NVM_BIN environment variable, if available </li>
 *     	    <li> from NODE_PATH environment variable, if available </li>
 *     	    <li>fallback: PATH environment variable</li>
 *     	</ol>
 * </ol>
 */
class NpmExecutableResolver {

	private NpmExecutableResolver() {
		// no instance
	}

	static String npmExecutableName() {
		String npmName = "npm";
		if (PlatformInfo.normalizedOS() == WINDOWS) {
			npmName += ".exe";
		}
		return npmName;
	}

	static Supplier<Optional<File>> systemProperty() {
		return () -> Optional.ofNullable(System.getProperty("npm.exec"))
				.map(File::new);
	}

	static Supplier<Optional<File>> environmentNvm() {
		return () -> Optional.ofNullable(System.getenv("NVM_BIN"))
				.map(File::new)
				.map(binDir -> new File(binDir, npmExecutableName()))
				.filter(File::exists)
				.filter(File::canExecute);
	}

	static Supplier<Optional<File>> environmentNodepath() {
		return pathListFromEnvironment("NODE_PATH");
	}

	static Supplier<Optional<File>> environmentPath() {
		return pathListFromEnvironment("PATH");
	}

	static Optional<File> tryFind() {
		return Stream.of(systemProperty(), environmentNvm(), environmentNodepath(), environmentPath())
				.map(Supplier::get)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.findFirst();
	}

	private static Supplier<Optional<File>> pathListFromEnvironment(String environmentPathListName) {
		return () -> {
			String pathList = System.getenv(environmentPathListName);
			if (pathList != null) {
				return Arrays.stream(pathList.split(System.getProperty("path.separator", ":")))
						.map(File::new)
						.map(dir -> dir.getName().equalsIgnoreCase("node_modules") ? dir.getParentFile() : dir)
						.map(dir -> new File(dir, npmExecutableName()))
						.filter(File::exists)
						.filter(File::canExecute)
						.findFirst();

			}
			return Optional.empty();
		};
	}

}
