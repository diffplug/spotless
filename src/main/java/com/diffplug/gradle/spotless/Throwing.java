/**
 * Copyright 2015 DiffPlug
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
package com.diffplug.gradle.spotless;

/**
 * Versions of the standard functional interfaces which throw Throwable.
 *
 * Ripped from Durian (https://github.com/diffplug/durian/blob/master/src/com/diffplug/common/base/Throwing.java).
 */
public interface Throwing {
	/** Versions of the standard functional interfaces which throw a specific exception type. */
	public interface Specific {
		@FunctionalInterface
		public interface Runnable<E extends Throwable> {
			void run() throws E;
		}

		@FunctionalInterface
		public interface Supplier<T, E extends Throwable> {
			T get() throws E;
		}

		@FunctionalInterface
		public interface Consumer<T, E extends Throwable> {
			void accept(T t) throws E;
		}

		@FunctionalInterface
		public interface Function<T, R, E extends Throwable> {
			R apply(T t) throws E;
		}
	}

	@FunctionalInterface
	public interface Runnable extends Specific.Runnable<Throwable> {}

	@FunctionalInterface
	public interface Supplier<T> extends Specific.Supplier<T, Throwable> {}

	@FunctionalInterface
	public interface Consumer<T> extends Specific.Consumer<T, Throwable> {}

	@FunctionalInterface
	public interface Function<T, R> extends Specific.Function<T, R, Throwable> {}
}
