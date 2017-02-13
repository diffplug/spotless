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
package com.diffplug.spotless;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.TreeSet;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/** Computes a signature for any needed files. */
public final class FileSignature implements Serializable {
	private static final long serialVersionUID = 1L;

	/** Ignore certain changes of the file list for signature calculation */
	public static enum Ignore {
		/** Signature does not depend on the order of files (if multiple are specified) changes. */
		ORDER,
		/** Signature does not change in case duplicates of files are added/removed (duplicates after first occurrence are ignored). */
		NEXT_DUPLICATES,
		/** Signature does not change in case duplicates of files are added/removed (duplicates before last occurrence are ignored). */
		PREVIOUS_DUPLICATES;
		/** Signature does not depend on the order of files or whether duplicates of files are removed/added. */
		public static final EnumSet<Ignore> ORDER_AND_DUPLICATES = EnumSet.of(ORDER, NEXT_DUPLICATES);
	}

	/*
	 * Transient because not needed to uniquely identify a FileSignature instance, and also because
	 * Gradle only needs this class to be Serializable so it can compare FileSignature instances for
	 * incremental builds.
	 */
	@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
	private final transient Collection<File> files;

	private final String[] filenames;
	private final long[] filesizes;
	private final long[] lastModified;

	public static FileSignature from(File... files) throws IOException {
		return from(Arrays.asList(files));
	}

	private FileSignature(final Collection<File> files) throws IOException {
		this.files = files;

		filenames = new String[this.files.size()];
		filesizes = new long[this.files.size()];
		lastModified = new long[this.files.size()];

		int i = 0;
		for (File file : this.files) {
			filenames[i] = file.getCanonicalPath();
			filesizes[i] = file.length();
			lastModified[i] = file.lastModified();
			++i;
		}
	}

	/** Returns all of the files in this signature, throwing an exception if there are more or less than 1 file. */
	public Collection<File> files() {
		return Collections.unmodifiableCollection(files);
	}

	/** Returns the only file in this signature, throwing an exception if there are more or less than 1 file. */
	public File getOnlyFile() {
		if (files.size() == 1) {
			return files.iterator().next();
		} else {
			throw new IllegalArgumentException("Expected one file, but was " + files.size());
		}
	}

	/**
	 * Creates file signature for a file.
	 *
	 * @param file
	 * 			File used for signature generation.
	 * @return Signature for file
	 */
	public static FileSignature from(final File file) throws IOException {
		return from(Arrays.asList(file), EnumSet.noneOf(Ignore.class));
	}

	/**
	 * Creates file signature for a list of files.
	 *
	 * @param files
	 * 			Files used for signature generation.
	 * @return Signature for files
	 */
	public static FileSignature from(final Collection<File> files) throws IOException {
		return from(files, EnumSet.noneOf(Ignore.class));
	}

	/**
	 * Creates file signature for none or multiple files.
	 *
	 * @param files
	 * 			Files used for signature generation.
	 * @param ignore
	 * 			Ignore a feature of the provided files when generating signature
	 * @return Signature for files
	 */
	public static FileSignature from(final Collection<File> files, final Ignore ignore) throws IOException {
		return from(files, EnumSet.of(ignore));
	}

	/**
	 * Creates file signature for none or multiple files.
	 *
	 * @param files
	 * 			Files used for signature generation.
	 * @param ignores
	 * 			Ignore features of the provided files when generating signature
	 * @return Signature for files
	 */
	public static FileSignature from(Collection<File> files, final EnumSet<Ignore> ignores) throws IOException {
		if (ignores.contains(Ignore.ORDER)) {
			/* The content of the input argument is not touched. We make a copy. */
			List<File> fileList = new ArrayList<File>(files);
			Collections.sort(fileList);
			files = fileList;
			if (ignores.contains(Ignore.NEXT_DUPLICATES)
					|| ignores.contains(Ignore.PREVIOUS_DUPLICATES)) {
				toSortedSet(files);
			}
		} else if (ignores.contains(Ignore.NEXT_DUPLICATES)) {
			/* Duplicates of sorted collections had been handled by optimized function above. */
			files = new LinkedHashSet<File>(files);
		} else if (ignores.contains(Ignore.PREVIOUS_DUPLICATES)) {
			/* PREVIOUS_DUPLICATES can be ignored if NEXT_DUPLICATES had already removed the duplicates. */
			List<File> fileList = new LinkedList<File>(files);
			toSetRemovingPrevious(fileList);
			files = fileList;
		}
		return new FileSignature(files);
	}

	/** Converts unsorted list into set by removing previous occurrences */
	private static <T extends Comparable<T>> void toSetRemovingPrevious(final List<T> unsortedList) {
		Set<T> set = new TreeSet<T>();
		ListIterator<T> current = unsortedList.listIterator(unsortedList.size());
		ListIterator<T> previous = null;
		while (current.hasPrevious()) {
			previous = current;
			if (!set.add(current.previous())) {
				previous.remove();
			}
		}
	}

	/** Converts a sorted collection into a sorted set by removing all duplicates. */
	private static <T extends Comparable<T>> void toSortedSet(final Collection<T> sortedCollection) {
		// remove any duplicates (normally there won't be any)
		if (sortedCollection.size() > 1) {
			Iterator<T> iter = sortedCollection.iterator();
			T last = iter.next();
			while (iter.hasNext()) {
				T next = iter.next();
				if (next.compareTo(last) == 0) {
					iter.remove();
				} else {
					last = next;
				}
			}
		}
	}
}
