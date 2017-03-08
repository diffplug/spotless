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
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/** Computes a signature for any needed files. */
public final class FileSignature implements Serializable {
	private static final long serialVersionUID = 1L;

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

	/** Creates file signature whereas order of the files remains unchanged. */
	public static FileSignature fromList(File... files) throws IOException {
		return fromList(Arrays.asList(files));
	}

	/** Creates file signature whereas order of the files remains unchanged. */
	public static FileSignature fromList(Collection<File> files) throws IOException {
		return new FileSignature(files);
	}

	/** Creates file signature insensitive to the order of the files. */
	public static FileSignature fromSet(File... files) throws IOException {
		return fromSet(Arrays.asList(files));
	}

	/** Creates file signature insensitive to the order of the files. */
	public static FileSignature fromSet(Collection<File> files) throws IOException {
		return new FileSignature(toSortedSet(Objects.requireNonNull(files)));
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

	/** Returns a "sortedSet" by copying the input to an ArrayList, sorting, and removing duplicates. */
	private static <T extends Comparable<T>> List<T> toSortedSet(Collection<T> unsorted) {
		// copy the whole unsorted into a list
		List<T> result = new ArrayList<>((Collection<T>) unsorted);
		// sort it
		Collections.sort(result);
		// remove any duplicates (normally there won't be any)
		if (result.size() > 1) {
			Iterator<T> iter = result.iterator();
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
		return result;
	}
}
