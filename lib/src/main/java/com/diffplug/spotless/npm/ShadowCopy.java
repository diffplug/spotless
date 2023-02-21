/*
 * Copyright 2023 DiffPlug
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
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diffplug.spotless.ThrowingEx;

class ShadowCopy {

	private static final Logger logger = LoggerFactory.getLogger(ShadowCopy.class);

	private final File shadowCopyRoot;

	public ShadowCopy(@Nonnull File shadowCopyRoot) {
		this.shadowCopyRoot = shadowCopyRoot;
		if (!shadowCopyRoot.isDirectory()) {
			throw new IllegalArgumentException("Shadow copy root must be a directory: " + shadowCopyRoot);
		}
	}

	public void addEntry(String key, File orig) {
		if (!reserveSubFolder(key)) {
			logger.debug("Shadow copy entry already on the way: {}. Awaiting finalization.", key);
			try {
				// maybe make the duration configurable?
				NpmResourceHelper.awaitFileDeleted(markerFilePath(key).toFile(), Duration.ofSeconds(120));
			} catch (TimeoutException e) {
				throw new RuntimeException(e);
			}
		}
		try {
			storeEntry(key, orig);
		} finally {
			cleanupReservation(key);
		}
	}

	public File getEntry(String key, String fileName) {
		return entry(key, fileName);
	}

	private void storeEntry(String key, File orig) {
		File target = entry(key, orig.getName());
		if (target.exists()) {
			logger.debug("Shadow copy entry already exists: {}", key);
			// delete directory "target" recursively
			// https://stackoverflow.com/questions/3775694/deleting-folder-from-java
			ThrowingEx.run(() -> Files.walkFileTree(target.toPath(), new DeleteDirectoryRecursively()));
		}
		// copy directory "orig" to "target" using hard links if possible or a plain copy otherwise
		ThrowingEx.run(() -> Files.walkFileTree(orig.toPath(), new CopyDirectoryRecursively(target, orig)));
	}

	private void cleanupReservation(String key) {
		ThrowingEx.run(() -> Files.delete(markerFilePath(key)));
	}

	private Path markerFilePath(String key) {
		return Paths.get(shadowCopyRoot.getAbsolutePath(), key + ".marker");
	}

	private File entry(String key, String origName) {
		return Paths.get(shadowCopyRoot.getAbsolutePath(), key, origName).toFile();
	}

	private boolean reserveSubFolder(String key) {
		// put a marker file named "key".marker in "shadowCopyRoot" to make sure no other process is using it or return false if it already exists
		try {
			Files.createFile(Paths.get(shadowCopyRoot.getAbsolutePath(), key + ".marker"));
			return true;
		} catch (FileAlreadyExistsException e) {
			return false;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public File copyEntryInto(String key, String origName, File targetParentFolder) {
		File target = Paths.get(targetParentFolder.getAbsolutePath(), origName).toFile();
		if (target.exists()) {
			logger.warn("Shadow copy destination already exists, deleting! {}: {}", key, target);
			ThrowingEx.run(() -> Files.walkFileTree(target.toPath(), new DeleteDirectoryRecursively()));
		}
		// copy directory "orig" to "target" using hard links if possible or a plain copy otherwise
		ThrowingEx.run(() -> Files.walkFileTree(entry(key, origName).toPath(), new CopyDirectoryRecursively(target, entry(key, origName))));
		return target;
	}

	private static class CopyDirectoryRecursively extends SimpleFileVisitor<Path> {
		private final File target;
		private final File orig;

		private boolean tryHardLink = true;

		public CopyDirectoryRecursively(File target, File orig) {
			this.target = target;
			this.orig = orig;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			// create directory on target
			Files.createDirectories(target.toPath().resolve(orig.toPath().relativize(dir)));
			return super.preVisitDirectory(dir, attrs);
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			// first try to hardlink, if that fails, copy
			if (tryHardLink) {
				try {
					Files.createLink(target.toPath().resolve(orig.toPath().relativize(file)), file);
					return super.visitFile(file, attrs);
				} catch (UnsupportedOperationException | SecurityException | FileSystemException e) {
					logger.debug("Shadow copy entry does not support hard links: {}", file, e);
					tryHardLink = false; // remember that hard links are not supported
				} catch (IOException e) {
					logger.debug("Shadow copy entry failed to create hard link: {}", file, e);
					tryHardLink = false; // remember that hard links are not supported
				}
			}
			// copy file to target
			Files.copy(file, target.toPath().resolve(orig.toPath().relativize(file)));
			return super.visitFile(file, attrs);
		}
	}

	private static class DeleteDirectoryRecursively extends SimpleFileVisitor<Path> {
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			Files.delete(file);
			return super.visitFile(file, attrs);
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			Files.delete(dir);
			return super.postVisitDirectory(dir, exc);
		}
	}
}
