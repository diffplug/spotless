/*
 * Copyright 2016-2023 DiffPlug
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
import java.util.Optional;
import java.util.function.Function;

/**
 * Utility class to resolve an npm binary to be used by npm-based steps.
 * Tries to find an npm executable in the following order:
 * <ol>
 *     <li>from System-Property {@code npm.exec}</li>
 *     <li>from Environment-Properties in the following order:</li>
 *     	<ol>
 *     	    <li> from NVM_BIN environment variable, if available </li>
 *     	    <li> from NVM_SYMLINK environment variable, if available </li>
 *     	    <li> from NODE_PATH environment variable, if available </li>
 *     	    <li>fallback: PATH environment variable</li>
 *     	</ol>
 * </ol>
 */
class NpmExecutableResolver {

	private static final FileFinder NPM_EXECUTABLE_FINDER = FileFinder.finderForExecutableFilename(npmExecutableName())
			.candidateSystemProperty("npm.exec")
			.candidateEnvironmentPath("NVM_BIN")
			.candidateEnvironmentPathList("NVM_SYMLINK", resolveParentOfNodeModulesDir())
			.candidateEnvironmentPathList("NODE_PATH", resolveParentOfNodeModulesDir())
			.candidateEnvironmentPathList("PATH", resolveParentOfNodeModulesDir())
			.build();

	private NpmExecutableResolver() {
		// no instance
	}

	static String npmExecutableName() {
		var npmName = "npm";
		if (PlatformInfo.normalizedOS() == WINDOWS) {
			npmName += ".cmd";
		}
		return npmName;
	}

	private static ParentOfNodeModulesDirResolver resolveParentOfNodeModulesDir() {
		return new ParentOfNodeModulesDirResolver();
	}

	static class ParentOfNodeModulesDirResolver implements Function<File, File> {

		@Override
		public File apply(File file) {
			if (file != null && file.isDirectory() && file.getName().equalsIgnoreCase("node_modules")) {
				return file.getParentFile();
			}
			return file;
		}
	}

	static Optional<File> tryFind() {
		return NPM_EXECUTABLE_FINDER.tryFind();
	}

	static String explainMessage() {
		return "Spotless tries to find your npm executable automatically. It looks for npm in the following places:\n" +
				"- An executable referenced by the java system property 'npm.exec' - if such a system property exists.\n" +
				"- The environment variable 'NVM_BIN' - if such an environment variable exists.\n" +
				"- The environment variable 'NVM_SYMLINK' - if such an environment variable exists.\n" +
				"- The environment variable 'NODE_PATH' - if such an environment variable exists.\n" +
				"- In your 'PATH' environment variable\n" +
				"\n" +
				"If autodiscovery fails for your system, try to set one of the environment variables correctly or\n" +
				"try setting the system property 'npm.exec' in the build process to override autodiscovery.";
	}
}
