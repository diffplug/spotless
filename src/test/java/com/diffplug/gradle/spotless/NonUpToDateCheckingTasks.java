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
package com.diffplug.gradle.spotless;

import java.io.File;
import java.util.function.Supplier;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Suppliers;
import com.diffplug.common.base.Throwing;
import com.diffplug.gradle.spotless.fi.SerializableThrowingFunction;

/**
 * Creates trivial tasks which are incapable of
 * performing up-to-date checks, but are useful
 * for testing.
 */
public class NonUpToDateCheckingTasks {
	/** A FormatterStep which doesn't depend on the input file. */
	static class FileIndependent implements FormatterStep {
		private final String name;
		private final SerializableThrowingFunction<String, String> formatter;

		private FileIndependent(String name, SerializableThrowingFunction<String, String> formatter) {
			this.name = name;
			this.formatter = formatter;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String format(String raw, File file) throws Throwable {
			return formatter.apply(raw);
		}

		private static final long serialVersionUID = 1L;
	}

	public static FormatterStep create(String name, SerializableThrowingFunction<String, String> formatter) {
		return new FileIndependent(name, formatter);
	}

	public static FormatterStep createLazy(String name, Throwing.Supplier<Throwing.Function<String, String>> formatterSupplier) {
		// wrap the supplier as a regular Supplier (not a Throwing.Supplier)
		Supplier<Throwing.Function<String, String>> rethrowFormatterSupplier = Errors.rethrow().wrap(formatterSupplier);
		// memoize its result
		Supplier<Throwing.Function<String, String>> memoized = Suppliers.memoize(rethrowFormatterSupplier);
		// create the step
		return new FileIndependent(name, content -> memoized.get().apply(content));
	}
}
