/*
 * Copyright 2021-2022 DiffPlug
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import com.diffplug.common.annotations.VisibleForTesting;
import com.diffplug.spotless.Formatter;

class IndexBasedChecker implements UpToDateChecker {

	private final FileIndex index;
	private final Log log;

	@VisibleForTesting
	IndexBasedChecker(FileIndex index, Log log) {
		this.index = index;
		this.log = log;
	}

	static IndexBasedChecker create(MavenProject project, Path indexFile, Iterable<Formatter> formatters, Log log) {
		PluginFingerprint pluginFingerprint = PluginFingerprint.from(project, formatters);
		FileIndexConfig indexConfig = new FileIndexConfig(project, indexFile, pluginFingerprint);
		FileIndex fileIndex = FileIndex.read(indexConfig, log);
		return new IndexBasedChecker(fileIndex, log);
	}

	@Override
	public boolean isUpToDate(Path file) {
		Instant storedLastModifiedTime = index.getLastModifiedTime(file);
		return Objects.equals(storedLastModifiedTime, lastModifiedTime(file));
	}

	@Override
	public void setUpToDate(Path file) {
		Instant lastModified = lastModifiedTime(file);
		if (Instant.MIN.equals(lastModified) || Instant.MAX.equals(lastModified)) {
			// FileTime can store timestamps further in the past/future than Instant.
			// Such timestamps are saturated to Instant.MIN/Instant.MAX.
			// Do not store such timestamps in the index because they are imprecise.
			log.warn("File " + file + " has an approximated last modified time of " + lastModified + ". "
					+ "It will not be recorded in the up-to-date index.");
			return;
		}
		index.setLastModifiedTime(file, lastModified);
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
