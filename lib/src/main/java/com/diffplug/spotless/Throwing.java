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

import static java.util.Objects.requireNonNull;

public class Throwing {
	private Throwing() {}

	/** A function that can throw any exception. */
	public interface Function<T, R> {
		R apply(T input) throws Throwable;
	}

	/** A supplier that can throw any exception. */
	public interface Supplier<T> {
		T get() throws Throwable;
	}

	/** A runnable that can throw any exception. */
	public interface Runnable {
		void run() throws Throwable;
	}

	/** Runs the given runnable, rethrowing any exceptions as runtime exceptions. */
	public static void run(Throwing.Runnable runnable) {
		try {
			runnable.run();
		} catch (Throwable t) {
			throw asRuntime(t);
		}
	}

	/** Gets the given value, rethrowing any exceptions as runtime exceptions. */
	public static <T> T get(Throwing.Supplier<T> supplier) {
		try {
			return supplier.get();
		} catch (Throwable t) {
			throw asRuntime(t);
		}
	}

	/** Wraps the given {@link Throwing.Function} as a standard {@link java.util.function.Function}, rethrowing any exceptions as runtime exceptions. */
	public static <T, R> java.util.function.Function<T, R> wrap(Throwing.Function<T, R> function) {
		return input -> {
			try {
				return function.apply(input);
			} catch (Throwable t) {
				throw asRuntime(t);
			}
		};
	}

	/**
	 * Casts or wraps the given exception to be a RuntimeException.
	 *
	 * If the input exception is a RuntimeException, it is simply
	 * cast and returned.  Otherwise, it wrapped in a
	 * {@link WrappedAsRuntimeException} and returned.
	 */
	public static RuntimeException asRuntime(Throwable e) {
		requireNonNull(e);
		if (e instanceof RuntimeException) {
			return (RuntimeException) e;
		} else if (e instanceof Error) {
			throw (Error) e;
		} else {
			return new WrappedAsRuntimeException(e);
		}
	}

	/** A RuntimeException specifically for the purpose of wrapping non-runtime Throwables as RuntimeExceptions. */
	public static class WrappedAsRuntimeException extends RuntimeException {
		private static final long serialVersionUID = -912202209702586994L;

		public WrappedAsRuntimeException(Throwable e) {
			super(e);
		}
	}
}
