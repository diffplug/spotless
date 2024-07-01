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
package com.diffplug.spotless.npm;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.diffplug.spotless.ThrowingEx;

/**
 * Wrapper for a file that conforms to the following idea:
 * 1. Equals and hashcode should not include absolute paths and such - optimize for buildcache keys
 * 2. Serialized/deserialized state can include absolute paths and such and should recreate a valid/runnable state
 */
class RoundtrippableFile implements Serializable {

	private static final long serialVersionUID = 8667124674782397566L;

	private final File file;

	public RoundtrippableFile(@Nonnull File file) {
		this.file = requireNonNull(file);
	}

	public File file() {
		return file;
	}

	public boolean isFile() {
		return file.isFile();
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		} else if (getClass() != o.getClass()) {
			return false;
		} else {
			return sig().equals(((RoundtrippableFile) o).sig());
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(sig());
	}

	@Override
	public String toString() {
		return "RoundtrippableFile[" + file.toString() + "]";
	}

	private FileSig sig() {
		return FileSigCache.INSTANCE.sign(file);
	}

	/**
	 * A file signature which includes the file name, size, hash and last modified.
	 * Equals and Hashcode are based on these three values only.
	 */
	private static class FileSig {
		final String name;
		final long size;
		final byte[] hash;
		final long lastModified;

		FileSig(@Nonnull File file) {
			Objects.requireNonNull(file);
			this.name = file.getName();
			this.size = safeSize(file);
			this.hash = ThrowingEx.get(() -> hash(file));
			this.lastModified = safeLastModified(file);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof FileSig))
				return false;
			FileSig fileSig = (FileSig) o;
			return size == fileSig.size && Objects.equals(name, fileSig.name) && Objects.deepEquals(hash, fileSig.hash) && lastModified == fileSig.lastModified;
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, size, Arrays.hashCode(hash), lastModified);
		}

		private static byte[] hash(@Nonnull File file) throws NoSuchAlgorithmException, IOException {
			if (!file.isFile()) {
				return new byte[0];
			}
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] buf = new byte[1024];
			try (InputStream input = new FileInputStream(file)) {
				int numRead;
				while ((numRead = input.read(buf)) != -1) {
					digest.update(buf, 0, numRead);
				}
			}
			return digest.digest();
		}
	}

	private static long safeLastModified(File file) {
		return file.isFile() ? file.lastModified() : -1;
	}

	private static long safeSize(File file) {
		return file.isFile() ? file.length() : 0;
	}

	// heavily inspired by FileSignature.Cache
	private enum FileSigCache {
		INSTANCE;

		private final Map<String, FileSig> cache = new HashMap<>();

		synchronized FileSig sign(File file) {
			String canonicalPath = ThrowingEx.get(file::getCanonicalPath);
			FileSig cached = cache.computeIfAbsent(canonicalPath, ThrowingEx.wrap(p -> new FileSig(file)));
			if (cached.lastModified != safeLastModified(file)) {
				cache.remove(canonicalPath); // might have been changed, so re-calculate
				return sign(file);
			} else {
				return cached;
			}
		}
	}
}
