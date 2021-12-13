/*
 * Copyright 2021 DiffPlug
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
package com.diffplug.spotless.maven.incremental;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import com.diffplug.common.annotations.VisibleForTesting;
import com.diffplug.spotless.Formatter;

class IndexBasedChecker implements UpToDateChecker {

	private final FileIndex index;

	@VisibleForTesting
	IndexBasedChecker(FileIndex index) {
		this.index = index;
	}

	static IndexBasedChecker create(MavenProject project, Iterable<Formatter> formatters, Log log) {
		PluginFingerprint pluginFingerprint = PluginFingerprint.from(project, formatters);
		FileIndexConfig indexConfig = new FileIndexConfig(project, pluginFingerprint);
		FileIndex fileIndex = FileIndex.read(indexConfig, log);
		return new IndexBasedChecker(fileIndex);
	}

	@Override
	public boolean isUpToDate(File file) {
		Path path = file.toPath();

		Optional<Instant> storedLastModifiedTimeOptional = index.getLastModifiedTime(path);
		if (!storedLastModifiedTimeOptional.isPresent()) {
			return false;
		}

		Instant storedLastModifiedTime = storedLastModifiedTimeOptional.get();
		return storedLastModifiedTime.equals(lastModifiedTime(path));
	}

	@Override
	public void setUpToDate(File file) {
		Path path = file.toPath();
		index.setLastModifiedTime(path, lastModifiedTime(path));
	}

	@Override
	public void close() {
		index.write();
	}

	private static Instant lastModifiedTime(Path path) {
		try {
			return Files.getLastModifiedTime(path).toInstant();
		} catch (IOException e) {
			throw new UncheckedIOException("Unable to get last modified date for " + path, e);
		}
	}
}
