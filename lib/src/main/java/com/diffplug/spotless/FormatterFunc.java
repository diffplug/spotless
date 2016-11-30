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

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Throwing;

/** A `Function<String, String>` which can throw an exception. */
public interface FormatterFunc extends Throwing.Function<String, String> {
	/** A `Function<String, String>` whose implementation requires a resource which should be released when the function is no longer needed. */
	interface Closeable extends FormatterFunc, AutoCloseable {
		@Override
		void close();

		/** Creates a {@link Closeable} from an AutoCloseable and a function. */
		public static Closeable of(AutoCloseable closeable, FormatterFunc function) {
			return new Closeable() {
				@Override
				public void close() {
					Errors.rethrow().run(closeable::close);
				}

				@Override
				public String apply(String input) throws Throwable {
					return function.apply(input);
				}
			};
		}
	}
}
