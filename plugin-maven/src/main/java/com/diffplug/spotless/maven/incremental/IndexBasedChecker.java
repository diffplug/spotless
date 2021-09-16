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

// todo: add unit tests
class IndexBasedChecker implements UpToDateChecker {

	private final FileIndex index;

	private IndexBasedChecker(FileIndex index) {
		this.index = index;
	}

	static IndexBasedChecker create(MavenProject project, Log log) {
		PluginFingerprint pluginFingerprint = PluginFingerprint.from(project);
		// todo: does this produce the correct dir?
		Path buildDir = project.getBasedir().toPath().resolve(project.getBuild().getDirectory());
		FileIndex fileIndex = FileIndex.read(buildDir, pluginFingerprint, log);
		return new IndexBasedChecker(fileIndex);
	}

	@Override
	public boolean isUpToDate(File file) {
		Path path = file.toPath();

		Optional<Instant> storedLastModifiedTimeOptional = index.getLastModifiedTime(path);
		if (!storedLastModifiedTimeOptional.isPresent()) {
			return false;
		}

		Instant currentLastModifiedTime = lastModifiedTime(path);
		Instant storedLastModifiedTime = storedLastModifiedTimeOptional.get();
		return currentLastModifiedTime.isAfter(storedLastModifiedTime);
	}

	@Override
	public void setUpToDate(File file) {
		Path path = file.toPath();
		Instant currentLastModifiedTime = lastModifiedTime(path);
		index.setLastModifiedTime(path, currentLastModifiedTime);
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
