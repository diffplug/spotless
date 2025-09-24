/*
 * Copyright 2021-2025 DiffPlug
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newBufferedReader;
import static java.nio.file.Files.newBufferedWriter;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.maven.plugin.logging.Log;

import com.diffplug.common.annotations.VisibleForTesting;

import jakarta.annotation.Nullable;

class FileIndex {

	private static final String SEPARATOR = " ";

	private final Path indexFile;
	private final PluginFingerprint pluginFingerprint;
	private final Map<Path, Instant> fileToLastModifiedTime;
	private final Path projectDir;

	private boolean modified;

	private FileIndex(Path indexFile, PluginFingerprint pluginFingerprint, Map<Path, Instant> fileToLastModifiedTime, Path projectDir, boolean needsRewrite) {
		this.indexFile = indexFile;
		this.pluginFingerprint = pluginFingerprint;
		this.fileToLastModifiedTime = fileToLastModifiedTime;
		this.projectDir = projectDir;
		this.modified = needsRewrite;
	}

	static FileIndex read(FileIndexConfig config, Log log) {
		Path indexFile = config.getIndexFile();
		if (Files.notExists(indexFile)) {
			log.info("Index file does not exist. Fallback to an empty index");
			return emptyIndexFallback(config);
		}

		try (BufferedReader reader = newBufferedReader(indexFile, UTF_8)) {
			String firstLine = reader.readLine();
			if (firstLine == null) {
				log.info("Index file is empty. Fallback to an empty index");
				return emptyIndexFallback(config);
			}

			PluginFingerprint computedFingerprint = config.getPluginFingerprint();
			PluginFingerprint storedFingerprint = PluginFingerprint.from(firstLine);
			if (!computedFingerprint.equals(storedFingerprint)) {
				log.info("Index file corresponds to a different configuration of the plugin. Either the plugin version or its configuration has changed. Fallback to an empty index");
				return emptyIndexFallback(config);
			} else {
				Content content = readIndexContent(reader, config.getProjectDir(), log);
				return new FileIndex(indexFile, computedFingerprint, content.fileToLastModifiedTime, config.getProjectDir(), content.needsRewrite);
			}
		} catch (IOException e) {
			log.warn("Error reading the index file. Fallback to an empty index", e);
			return emptyIndexFallback(config);
		}
	}

	static void delete(FileIndexConfig config, Log log) {
		Path indexFile = config.getIndexFile();
		boolean deleted = false;
		try {
			deleted = Files.deleteIfExists(indexFile);
		} catch (IOException e) {
			log.warn("Unable to delete the index file: " + indexFile, e);
		}
		if (deleted) {
			log.info("Deleted the index file: " + indexFile);
		}
	}

	@Nullable Instant getLastModifiedTime(Path file) {
		if (!file.startsWith(projectDir)) {
			return null;
		}
		Path relativeFile = projectDir.relativize(file);
		return fileToLastModifiedTime.get(relativeFile);
	}

	void setLastModifiedTime(Path file, Instant time) {
		Path relativeFile = projectDir.relativize(file);
		fileToLastModifiedTime.put(relativeFile, time);
		modified = true;
	}

	@VisibleForTesting
	int size() {
		return fileToLastModifiedTime.size();
	}

	void write() {
		if (!modified) {
			return;
		}

		ensureParentDirExists();
		try (PrintWriter writer = new PrintWriter(newBufferedWriter(indexFile, UTF_8, CREATE, TRUNCATE_EXISTING))) {
			writer.println(pluginFingerprint.value());

			for (Entry<Path, Instant> entry : fileToLastModifiedTime.entrySet()) {
				writer.println(entry.getKey() + SEPARATOR + entry.getValue());
			}
		} catch (IOException e) {
			throw new UncheckedIOException("Unable to write the index", e);
		}
	}

	private void ensureParentDirExists() {
		Path parentDir = indexFile.getParent();
		if (parentDir == null) {
			throw new IllegalStateException("Index file does not have a parent dir: " + indexFile);
		}
		try {
			if (Files.exists(parentDir, LinkOption.NOFOLLOW_LINKS)) {
				Path realPath = parentDir.toRealPath();
				if (!Files.exists(realPath)) {
					Files.createDirectories(realPath);
				}
			} else {
				Files.createDirectories(parentDir);
			}
		} catch (IOException e) {
			throw new UncheckedIOException("Unable to create parent directory for the index file: " + indexFile, e);
		}
	}

	private static Content readIndexContent(BufferedReader reader, Path projectDir, Log log) throws IOException {
		Map<Path, Instant> fileToLastModifiedTime = new TreeMap<>();
		boolean needsRewrite = false;

		String line;
		while ((line = reader.readLine()) != null) {
			int separatorIndex = line.lastIndexOf(SEPARATOR);
			if (separatorIndex == -1) {
				throw new IOException("Incorrect index file. No separator found in '" + line + "'");
			}

			Path relativeFile = Path.of(line.substring(0, separatorIndex));
			Path absoluteFile = projectDir.resolve(relativeFile);
			if (Files.notExists(absoluteFile)) {
				log.info("File stored in the index does not exist: " + relativeFile);
				needsRewrite = true;
			} else {
				Instant lastModifiedTime = parseLastModifiedTime(line, separatorIndex);
				fileToLastModifiedTime.put(relativeFile, lastModifiedTime);
			}
		}

		return new Content(fileToLastModifiedTime, needsRewrite);
	}

	private static Instant parseLastModifiedTime(String line, int separatorIndex) throws IOException {
		try {
			return Instant.parse(line.substring(separatorIndex + 1));
		} catch (DateTimeParseException e) {
			throw new IOException("Incorrect index file. Unable to parse last modified time from '" + line + "'", e);
		}
	}

	private static FileIndex emptyIndexFallback(FileIndexConfig config) {
		return new FileIndex(config.getIndexFile(), config.getPluginFingerprint(), new TreeMap<>(), config.getProjectDir(), true);
	}

	private static class Content {
		final Map<Path, Instant> fileToLastModifiedTime;
		final boolean needsRewrite;

		Content(Map<Path, Instant> fileToLastModifiedTime, boolean needsRewrite) {
			this.fileToLastModifiedTime = fileToLastModifiedTime;
			this.needsRewrite = needsRewrite;
		}
	}
}
