/*
 * Copyright 2024 DiffPlug
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
package com.diffplug.spotless.cli.core;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class FilePathUtil {

	private FilePathUtil() {
		// no instance
	}

	public static File asFile(Path path) {
		return path == null ? null : path.toFile();
	}

	public static List<File> asFiles(List<Path> paths) {
		return paths == null ? null : paths.stream().map(Path::toFile).collect(Collectors.toList());
	}

	public static List<Boolean> assertDirectoryExists(File... files) {
		return assertDirectoryExists(Arrays.asList(files));
	}

	public static List<Boolean> assertDirectoryExists(List<File> files) {
		return files.stream().map(f -> f != null && f.mkdirs()).collect(Collectors.toList());
	}
}
