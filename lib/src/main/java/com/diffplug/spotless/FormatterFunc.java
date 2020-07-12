/*
 * Copyright 2016-2020 DiffPlug
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
import java.util.Objects;

/**
 * A `Function<String, String>` which can throw an exception.  Technically, there
 * is also a `File` argument which gets passed around as well, but that is invisible
 * to formatters.  If you need the File, see {@link NeedsFile}.
 */
@FunctionalInterface
public interface FormatterFunc {

	String apply(String input) throws Exception;

	default String apply(String unix, File file) throws Exception {
		return apply(unix);
	}

	/**
	 * `Function<String, String>` and `BiFunction<String, File, String>` whose implementation
	 * requires a resource which should be released when the function is no longer needed.
	 */
	interface Closeable extends FormatterFunc, AutoCloseable {
		@Override
		void close();

		/** Creates a {@link Closeable} from an AutoCloseable and a function. */
		public static Closeable of(AutoCloseable closeable, FormatterFunc function) {
			Objects.requireNonNull(closeable, "closeable");
			Objects.requireNonNull(function, "function");
			return new Closeable() {
				@Override
				public void close() {
					ThrowingEx.run(closeable::close);
				}

				@Override
				public String apply(String unix, File file) throws Exception {
					return function.apply(Objects.requireNonNull(unix), Objects.requireNonNull(file));
				}

				@Override
				public String apply(String unix) throws Exception {
					return function.apply(Objects.requireNonNull(unix));
				}
			};
		}
	}

	/**
	 * Ideally, formatters don't need the underlying file. But in case they do, they should only use it's path,
	 * and should never read the content inside the file, because that breaks the `Function<String, String>` composition
	 * that Spotless is based on.  For the rare case that you need access to the file, use this method
	 * or {@link NeedsFile} to create a {@link FormatterFunc} which needs the File.
	 */
	static FormatterFunc needsFile(NeedsFile needsFile) {
		return needsFile;
	}

	/** @see FormatterFunc#needsFile(NeedsFile) */
	@FunctionalInterface
	interface NeedsFile extends FormatterFunc {
		String applyWithFile(String unix, File file) throws Exception;

		@Override
		default String apply(String unix, File file) throws Exception {
			if (file == FormatterStepImpl.SENTINEL) {
				throw new IllegalArgumentException("This step requires the underlying file. If this is a test, use StepHarnessWithFile");
			}
			return applyWithFile(unix, file);
		}

		@Override
		default String apply(String unix) throws Exception {
			return apply(unix, FormatterStepImpl.SENTINEL);
		}
	}
}
