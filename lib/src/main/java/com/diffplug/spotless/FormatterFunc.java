/*
 * Copyright 2016-2018 DiffPlug
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
 * A `Function<String, String>` which can throw an exception.
 * Also the `BiFunction<String, File, String>` is supported, whereas the default
 * implementation only requires the `Function<String, String>` implementation.
 */
public interface FormatterFunc
		extends ThrowingEx.Function<String, String>, ThrowingEx.BiFunction<String, File, String> {

	@Override
	default String apply(String input, File source) throws Exception {
		return apply(input);
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
				public String apply(String input, File source) throws Exception {
					return function.apply(Objects.requireNonNull(input), Objects.requireNonNull(source));
				}

				@Override
				public String apply(String input) throws Exception {
					return function.apply(Objects.requireNonNull(input));
				}
			};
		}
	}

}
