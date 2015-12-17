/*
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

import java.io.File;
import java.util.function.Supplier;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Suppliers;
import com.diffplug.common.base.Throwing;

/**
 * An implementation of this class specifies a single step in a formatting process.
 * 
 * The input is guaranteed to have unix-style newlines, and the output is required
 * to not introduce any windows-style newlines as well.
 */
public class FormatterStep {
	private final String name;
	private final Throwing.Function<String, String> formatter;

	private FormatterStep(String name, Throwing.Function<String, String> formatter) {
		this.name = name;
		this.formatter = formatter;
	}

	/** The name of the step, for debugging purposes. */
	public String getName() {
		return name;
	}

	/**
	 * Returns a formatted version of the given content.
	 * 
	 * @param raw File's content, guaranteed to have unix-style newlines ('\n')
	 * @param file the File which is being formatted
	 * @return The formatted content, guaranteed to only have unix-style newlines 
	 * @throws Throwable 
	 */
	public String format(String raw, File file) throws Throwable {
		return formatter.apply(raw);
	}

	/** Creates a FormatterStep from the given function. */
	public static FormatterStep create(String name, Throwing.Function<String, String> formatter) {
		return new FormatterStep(name, formatter);
	}

	/** Creates a FormatterStep lazily from the given formatterSupplier function. */
	public static FormatterStep createLazy(String name, Throwing.Supplier<Throwing.Function<String, String>> formatterSupplier) {
		// wrap the supplier as a regular Supplier (not a Throwing.Supplier)
		Supplier<Throwing.Function<String, String>> rethrowFormatterSupplier = Errors.rethrow().wrap(formatterSupplier);
		// memoize its result
		Supplier<Throwing.Function<String, String>> memoized = Suppliers.memoize(rethrowFormatterSupplier);
		// create the step
		return new FormatterStep(name, content -> memoized.get().apply(content));
	}
}
