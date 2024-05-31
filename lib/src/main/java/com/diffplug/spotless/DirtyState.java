/*
 * Copyright 2022-2024 DiffPlug
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
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Arrays;

import javax.annotation.Nullable;

/**
 * The clean/dirty state of a single file.  Intended use:
 * - {@link #isClean()} means that the file is is clean, and there's nothing else to say
 * - {@link #didNotConverge()} means that we were unable to determine a clean state
 * - once you've tested the above conditions and you know that it's a dirty file with a converged state,
 * then you can call {@link #writeCanonicalTo(OutputStream)} to get the canonical form of the given file.
 */
public class DirtyState {
	@Nullable
	private final byte[] canonicalBytes;

	DirtyState(@Nullable byte[] canonicalBytes) {
		this.canonicalBytes = canonicalBytes;
	}

	public boolean isClean() {
		return this == isClean;
	}

	public boolean didNotConverge() {
		return this == didNotConverge;
	}

	byte[] canonicalBytes() {
		if (canonicalBytes == null) {
			throw new IllegalStateException("First make sure that {@code !isClean()} and {@code !didNotConverge()}");
		}
		return canonicalBytes;
	}

	public void writeCanonicalTo(File file) throws IOException {
		Files.write(file.toPath(), canonicalBytes());
	}

	public void writeCanonicalTo(OutputStream out) throws IOException {
		out.write(canonicalBytes());
	}

	/** Returns the DirtyState which corresponds to {@code isClean()}. */
	public static DirtyState clean() {
		return isClean;
	}

	static final DirtyState didNotConverge = new DirtyState(null);
	static final DirtyState isClean = new DirtyState(null);

	public static Calculation of(Formatter formatter, File file) throws IOException {
		return of(formatter, file, Files.readAllBytes(file.toPath()));
	}

	public static Calculation of(Formatter formatter, File file, byte[] rawBytes) {
		return new Calculation(formatter, file, rawBytes);
	}

	public static class Calculation {
		private final Formatter formatter;
		private final File file;
		private final byte[] rawBytes;
		final String raw;

		private Calculation(Formatter formatter, File file, byte[] rawBytes) {
			this.formatter = formatter;
			this.file = file;
			this.rawBytes = rawBytes;
			this.raw = new String(rawBytes, formatter.getEncoding());
			// check that all characters were encodable
			String encodingError = EncodingErrorMsg.msg(raw, rawBytes, formatter.getEncoding());
			if (encodingError != null) {
				throw new IllegalArgumentException(encodingError);
			}
		}

		/**
		 * Calculates whether the given file is dirty according to a PaddedCell invocation of the given formatter.
		 * DirtyState includes the clean state of the file, as well as a warning if we were not able to apply the formatter
		 * due to diverging idempotence.
		 */
		public DirtyState calculateDirtyState() {
			return calculateDirtyState(new ValuePerStep<>(formatter));
		}

		/**
		 * Calculates whether the given file is dirty according to a PaddedCell invocation of the given formatter.
		 * DirtyState includes the clean state of the file, as well as a warning if we were not able to apply the formatter
		 * due to diverging idempotence.
		 */
		DirtyState calculateDirtyState(ValuePerStep<Throwable> exceptionPerStep) {
			String rawUnix = LineEnding.toUnix(raw);

			// enforce the format
			String formattedUnix = formatter.computeWithLint(rawUnix, file, exceptionPerStep);
			// convert the line endings if necessary
			String formatted = formatter.computeLineEndings(formattedUnix, file);

			// if F(input) == input, then the formatter is well-behaving and the input is clean
			byte[] formattedBytes = formatted.getBytes(formatter.getEncoding());
			if (Arrays.equals(rawBytes, formattedBytes)) {
				return isClean;
			}

			// F(input) != input, so we'll do a padded check
			String doubleFormattedUnix = formatter.computeWithLint(formattedUnix, file, exceptionPerStep);
			if (doubleFormattedUnix.equals(formattedUnix)) {
				// most dirty files are idempotent-dirty, so this is a quick-short circuit for that common case
				return new DirtyState(formattedBytes);
			}

			PaddedCell cell = PaddedCell.check(formatter, file, rawUnix, exceptionPerStep);
			if (!cell.isResolvable()) {
				return didNotConverge;
			}

			// get the canonical bytes
			String canonicalUnix = cell.canonical();
			String canonical = formatter.computeLineEndings(canonicalUnix, file);
			byte[] canonicalBytes = canonical.getBytes(formatter.getEncoding());
			if (!Arrays.equals(rawBytes, canonicalBytes)) {
				// and write them to disk if needed
				return new DirtyState(canonicalBytes);
			} else {
				return isClean;
			}
		}
	}
}
