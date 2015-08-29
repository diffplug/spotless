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

/**
 * An implementation of this class specifies a single step in a formatting process.
 * 
 * The input is guaranteed to have unix-style newlines, and the output is required
 * to not introduce any windows-style newlines as well.
 */
public class FormatterStep {
	private final String name;

	private final FormattingOperation formatter;

	private FormatterStep(String name, FormattingOperation formatter) {
		this.name = name;
		this.formatter = formatter;
	}

	/** Creates a FormatterStep from the given function. */
	public static FormatterStep create(String name, FormattingOperation formatter) {
		return new FormatterStep(name, formatter);
	}

	/** Creates a FormatterStep lazily from the given formatterSupplier function. */
	public static FormatterStep createLazy(String name, final FormattingOperationSupplier formatterSupplier) {
		return new FormatterStep(name, new FormattingOperation() {
			@Override
			public String apply(String raw) throws Throwable {
				return formatterSupplier.get().apply(raw);
			}

		});
	}

	/** The name of the step, for debugging purposes. */
	public String getName() {
		return name;
	}

	/**
	 * Returns a formatted version of the given content.
	 *
	 * @param raw File's content, guaranteed to have unix-style newlines ('\n')
	 * @return The formatted content, guaranteed to only have unix-style newlines
	 * @throws Throwable
	 */
	public String format(String raw) throws Throwable {
		return formatter.apply(raw);
	}
}
