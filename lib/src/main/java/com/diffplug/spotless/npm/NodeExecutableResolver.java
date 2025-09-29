/*
 * Copyright 2023-2025 DiffPlug
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
import java.util.Optional;

final class NodeExecutableResolver {

	private NodeExecutableResolver() {
		// no instance
	}

	static String nodeExecutableName() {
		String nodeName = "node";
		if (PlatformInfo.normalizedOS() == PlatformInfo.OS.WINDOWS) {
			nodeName += ".exe";
		}
		return nodeName;
	}

	static Optional<File> tryFindNextTo(File npmExecutable) {
		if (npmExecutable == null) {
			return Optional.empty();
		}
		File nodeExecutable = new File(npmExecutable.getParentFile(), nodeExecutableName());
		if (nodeExecutable.exists() && nodeExecutable.isFile() && nodeExecutable.canExecute()) {
			return Optional.of(nodeExecutable);
		}
		return Optional.empty();
	}

	public static String explainMessage() {
		return """
				Spotless was unable to find a node executable.
				Either specify the node executable explicitly or make sure it can be found next to the npm executable.""";
	}
}
