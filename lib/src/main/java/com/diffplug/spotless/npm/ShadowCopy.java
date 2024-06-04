/*
 * Copyright 2023-2024 DiffPlug
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
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diffplug.spotless.ThrowingEx;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

class ShadowCopy {

	private static final Logger logger = LoggerFactory.getLogger(ShadowCopy.class);

	private final Supplier<File> shadowCopyRootSupplier;

	public ShadowCopy(@Nonnull Supplier<File> shadowCopyRootSupplier) {
		this.shadowCopyRootSupplier = shadowCopyRootSupplier;
	}

	private File shadowCopyRoot() {
		File shadowCopyRoot = shadowCopyRootSupplier.get();
		if (!shadowCopyRoot.isDirectory()) {
			throw new IllegalStateException("Shadow copy root must be a directory: " + shadowCopyRoot);
		}
		return shadowCopyRoot;
	}

	public void addEntry(String key, File orig) {
		File target = entry(key, orig.getName());
		if (target.exists()) {
			logger.debug("Shadow copy entry already exists, not overwriting: {}", key);
		} else {
			try {
				storeEntry(key, orig, target);
			} catch (Throwable ex) {
				// Log but don't fail
				logger.warn("Unable to store cache entry for {}", key, ex);
			}
		}
	}

	@SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
	private void storeEntry(String key, File orig, File target) throws IOException {
		// Create a temp directory in the same directory as target
		Files.createDirectories(target.toPath().getParent());
		Path tempDirectory = Files.createTempDirectory(target.toPath().getParent(), key);
		logger.debug("Will store entry {} to temporary directory {}, which is a sibling of the ultimate target {}", orig, tempDirectory, target);

		try {
			// Copy orig to temp dir
			Files.walkFileTree(orig.toPath(), new CopyDirectoryRecursively(tempDirectory, orig.toPath()));
			try {
				logger.debug("Finished storing entry {}. Atomically moving temporary directory {} into final place {}", key, tempDirectory, target);
				// Atomically rename the completed cache entry into place
				Files.move(tempDirectory, target.toPath(), StandardCopyOption.ATOMIC_MOVE);
			} catch (FileAlreadyExistsException | DirectoryNotEmptyException e) {
				// Someone already beat us to it
				logger.debug("Shadow copy entry now exists, not overwriting: {}", key);
			} catch (AtomicMoveNotSupportedException e) {
				logger.warn("The filesystem at {} does not support atomic moves. Spotless cannot safely cache on such a system due to race conditions. Caching has been skipped.", target.toPath().getParent(), e);
			}
		} finally {
			// Best effort to clean up
			if (Files.exists(tempDirectory)) {
				try {
					Files.walkFileTree(tempDirectory, new DeleteDirectoryRecursively());
				} catch (Throwable ex) {
					logger.warn("Ignoring error while cleaning up temporary copy", ex);
				}
			}
		}
	}

	public File getEntry(String key, String fileName) {
		return entry(key, fileName);
	}

	private File entry(String key, String origName) {
		return Paths.get(shadowCopyRoot().getAbsolutePath(), key, origName).toFile();
	}

	public File copyEntryInto(String key, String origName, File targetParentFolder) {
		File target = Paths.get(targetParentFolder.getAbsolutePath(), origName).toFile();
		if (target.exists()) {
			logger.warn("Shadow copy destination already exists, deleting! {}: {}", key, target);
			ThrowingEx.run(() -> Files.walkFileTree(target.toPath(), new DeleteDirectoryRecursively()));
		}
		// copy directory "orig" to "target" using hard links if possible or a plain copy otherwise
		ThrowingEx.run(() -> Files.walkFileTree(entry(key, origName).toPath(), new CopyDirectoryRecursively(target.toPath(), entry(key, origName).toPath())));
		return target;
	}

	public boolean entryExists(String key, String origName) {
		return entry(key, origName).exists();
	}

	private static class CopyDirectoryRecursively extends SimpleFileVisitor<Path> {
		private final Path target;
		private final Path orig;

		private boolean tryHardLink = true;

		public CopyDirectoryRecursively(Path target, Path orig) {
			this.target = target;
			this.orig = orig;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			// create directory on target
			Files.createDirectories(target.resolve(orig.relativize(dir)));
			return super.preVisitDirectory(dir, attrs);
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			// first try to hardlink, if that fails, copy
			if (tryHardLink) {
				try {
					Files.createLink(target.resolve(orig.relativize(file)), file);
					return super.visitFile(file, attrs);
				} catch (UnsupportedOperationException | SecurityException | FileSystemException e) {
					logger.debug("Shadow copy entry does not support hard links: {}. Switching to 'copy'.", file, e);
					tryHardLink = false; // remember that hard links are not supported
				} catch (IOException e) {
					logger.debug("Shadow copy entry failed to create hard link: {}. Switching to 'copy'.", file, e);
					tryHardLink = false; // remember that hard links are not supported
				}
			}
			// copy file to target
			Files.copy(file, target.resolve(orig.relativize(file)));
			return super.visitFile(file, attrs);
		}
	}

	// https://stackoverflow.com/questions/3775694/deleting-folder-from-java
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
