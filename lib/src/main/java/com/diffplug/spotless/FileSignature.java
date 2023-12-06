/*
 * Copyright 2016-2023 DiffPlug
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
package com.diffplug.spotless;

import static com.diffplug.spotless.MoreIterables.toNullHostileList;
import static com.diffplug.spotless.MoreIterables.toSortedSet;
import static java.util.Comparator.comparing;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/** Computes a signature for any needed files. */
public final class FileSignature implements Serializable {
	private static final long serialVersionUID = 2L;

	/*
	 * Transient because not needed to uniquely identify a FileSignature instance, and also because
	 * Gradle only needs this class to be Serializable so it can compare FileSignature instances for
	 * incremental builds.
	 *
	 * We don't want these absolute paths to screw up buildcache keys.
	 */
	@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
	private final transient List<File> files;
	private final Sig[] signatures;

	/** Creates file signature whereas order of the files remains unchanged. */
	public static FileSignature signAsList(File... files) throws IOException {
		return signAsList(Arrays.asList(files));
	}

	/** Creates file signature whereas order of the files remains unchanged. */
	public static FileSignature signAsList(Iterable<File> files) throws IOException {
		return new FileSignature(toNullHostileList(files));
	}

	/** Creates file signature whereas order of the files remains unchanged. */
	public static FileSignature signAsSet(File... files) throws IOException {
		return signAsSet(Arrays.asList(files));
	}

	/** Creates file signature insensitive to the order of the files. */
	public static FileSignature signAsSet(Iterable<File> files) throws IOException {
		List<File> natural = toSortedSet(files);
		List<File> onNameOnly = toSortedSet(files, comparing(File::getName));
		if (natural.size() != onNameOnly.size()) {
			StringBuilder builder = new StringBuilder();
			builder.append("For these files:\n");
			for (File file : files) {
				builder.append("  " + file.getAbsolutePath() + "\n");
			}
			builder.append("a caching signature is being generated, which will be based only on their\n");
			builder.append("names, not their full path (foo.txt, not C:\folder\foo.txt). Unexpectedly,\n");
			builder.append("you have two files with different paths, but the same names.  You must\n");
			builder.append("rename one of them so that all files have unique names.");
			throw new IllegalArgumentException(builder.toString());
		}
		return new FileSignature(onNameOnly);
	}

	private FileSignature(final List<File> files) throws IOException {
		this.files = validateInputFiles(files);
		this.signatures = new Sig[this.files.size()];

		int i = 0;
		for (File file : this.files) {
			signatures[i] = cache.sign(file);
			++i;
		}
	}

	/** A view of `FileSignature` which can be safely roundtripped. */
	public static class Promised implements Serializable {
		private static final long serialVersionUID = 1L;
		private final List<File> files;
		@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
		private transient @Nullable FileSignature cached;

		private Promised(List<File> files, @Nullable FileSignature cached) {
			this.files = files;
			this.cached = cached;
		}

		public FileSignature stripAbsolutePaths() throws IOException {
			if (cached == null) {
				// null when restored via serialization
				cached = new FileSignature(files);
			}
			return cached;
		}
	}

	public static Promised promise(Iterable<File> files) {
		return new Promised(MoreIterables.toNullHostileList(files), null);
	}

	public Promised roundTrippable() {
		return new Promised(files, this);
	}

	public static @Nullable Promised roundTrippableNullable(@Nullable FileSignature signature) {
		if (signature != null) {
			return signature.roundTrippable();
		} else {
			return null;
		}
	}

	public static @Nullable FileSignature stripAbsolutePathsNullable(@Nullable Promised roundTrippable) throws IOException {
		if (roundTrippable != null) {
			return roundTrippable.stripAbsolutePaths();
		} else {
			return null;
		}
	}

	/** Returns all of the files in this signature, throwing an exception if there are more or less than 1 file. */
	public Collection<File> files() {
		return Collections.unmodifiableList(files);
	}

	/** Returns the only file in this signature, throwing an exception if there are more or less than 1 file. */
	public File getOnlyFile() {
		if (files.size() == 1) {
			return files.iterator().next();
		} else {
			throw new IllegalArgumentException("Expected one file, but was " + files.size());
		}
	}

	private static boolean machineIsWin = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");

	/** Returns true if this JVM is running on a windows machine. */
	public static boolean machineIsWin() {
		return machineIsWin;
	}

	/** Transforms a native path to a unix one. */
	public static String pathNativeToUnix(String pathNative) {
		return pathNative.replace(File.separatorChar, '/');
	}

	/** Transforms a unix path to a native one. */
	public static String pathUnixToNative(String pathUnix) {
		return pathUnix.replace('/', File.separatorChar);
	}

	private static List<File> validateInputFiles(List<File> files) {
		for (File file : files) {
			if (!file.isFile()) {
				throw new IllegalArgumentException(
						"File signature can only be created for existing regular files, given: "
								+ file);
			}
		}
		return files;
	}

	/**
	 * It is very common for a given set of files to be "signed" many times.  For example,
	 * the jars which constitute any given formatter live in a central cache, but will be signed
	 * over and over.  To save this I/O, we maintain a cache, invalidated by lastModified time.
	 */
	static final Cache cache = new Cache();

	private static final class Cache {
		Map<String, Sig> cache = new HashMap<>();

		synchronized Sig sign(File fileInput) throws IOException {
			String canonicalPath = fileInput.getCanonicalPath();
			Sig sig = cache.computeIfAbsent(canonicalPath, ThrowingEx.<String, Sig> wrap(p -> {
				MessageDigest digest = MessageDigest.getInstance("SHA-256");
				File file = new File(p);
				// calculate the size and content hash of the file
				long size = 0;
				byte[] buf = new byte[1024];
				long lastModified;
				try (InputStream input = new FileInputStream(file)) {
					lastModified = file.lastModified();
					int numRead;
					while ((numRead = input.read(buf)) != -1) {
						size += numRead;
						digest.update(buf, 0, numRead);
					}
				}
				return new Sig(file.getName(), size, digest.digest(), lastModified);
			}));
			long lastModified = fileInput.lastModified();
			if (sig.lastModified != lastModified) {
				cache.remove(canonicalPath);
				return sign(fileInput);
			} else {
				return sig;
			}
		}
	}

	@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
	private static final class Sig implements Serializable {
		private static final long serialVersionUID = 6727302747168655222L;

		@SuppressWarnings("unused")
		final String name;
		@SuppressWarnings("unused")
		final long size;
		@SuppressWarnings("unused")
		final byte[] hash;
		/** transient because state should be transferable from machine to machine. */
		final transient long lastModified;

		Sig(String name, long size, byte[] hash, long lastModified) {
			this.name = name;
			this.size = size;
			this.hash = hash;
			this.lastModified = lastModified;
		}
	}

	/** Asserts that child is a subpath of root. and returns the subpath. */
	public static String subpath(String root, String child) {
		if (child.startsWith(root)) {
			return child.substring(root.length());
		} else {
			if (machineIsWin() && root.endsWith("://") && child.startsWith(root.substring(0, root.length() - 1))) {
				return child.substring(root.length() - 1);
			}
			throw new IllegalArgumentException("Expected '" + child + "' to start with '" + root + "'");
		}
	}
}
