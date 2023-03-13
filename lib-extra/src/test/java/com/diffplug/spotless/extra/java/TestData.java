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
package com.diffplug.spotless.extra.eclipse.java;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class TestData {
	private static final String EXTENSION_INPUT = ".input";
	private static final String EXTENSION_ORGANIZED_IMPORTS = ".organized";
	private static final String EXTENSION_CLEAN_UP = ".cleanup";

	public static TestData getTestDataOnFileSystem() {
		final String userDir = System.getProperty("user.dir", ".");
		Path dataPath = Paths.get(userDir, "src", "test", "resources");
		if (Files.isDirectory(dataPath)) {
			return new TestData(dataPath);
		}
		throw new IllegalArgumentException("Test data not found:" + dataPath.toString());
	}

	private final Path resourcesPath;

	private TestData(Path resourcesPath) {
		this.resourcesPath = resourcesPath.toAbsolutePath();
		if (!Files.isDirectory(resourcesPath)) {
			throw new IllegalArgumentException(String.format("'%1$s' is not a directory.", resourcesPath));
		}
	}

	public String input(final String fileName) throws Exception {
		Path filePath = resourcesPath.resolve(fileName + EXTENSION_INPUT);
		return read(filePath);
	}

	public String afterOrganizedImports(final String fileName) {
		Path filePath = resourcesPath.resolve(fileName + EXTENSION_ORGANIZED_IMPORTS);
		return read(filePath);
	}

	public String afterCleanUp(final String fileName) {
		Path filePath = resourcesPath.resolve(fileName + EXTENSION_CLEAN_UP);
		return read(filePath);
	}

	private String read(final Path filePath) {
		if (!Files.isRegularFile(filePath)) {
			throw new IllegalArgumentException(String.format("'%1$s' is not a regular file.", filePath));
		}
		try {
			String checkedOutFileContent = new String(java.nio.file.Files.readAllBytes(filePath), "ASCII");
			return checkedOutFileContent.replace("\r", ""); //Align GIT end-of-line normalization
		} catch (IOException e) {
			throw new IllegalArgumentException(String.format("Failed to read '%1$s'.", filePath), e);
		}
	}

	public static String toString(Properties properties) {
		StringBuilder result = new StringBuilder();
		result.append('[');
		properties.forEach((k, v) -> {
			result.append(k.toString());
			result.append('=');
			result.append(v.toString());
			result.append(';');
		});
		result.append(']');
		return result.toString();
	}
}
