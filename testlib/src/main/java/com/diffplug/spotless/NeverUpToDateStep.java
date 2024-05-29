/*
 * Copyright 2024 DiffPlug
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
 * Formatter which is equal to itself, but not to any other Formatter.
 */
public class NeverUpToDateStep implements FormatterStep {
	/**
	 * @param name
	 *             The name of the formatter step
	 * @param functionSupplier
	 *             A supplier which will lazily generate the function
	 *             used by the formatter step
	 * @return A FormatterStep which will never report that it is up-to-date, because
	 *         it is not equal to the serialized representation of itself.
	 */
	public static FormatterStep createLazy(
			String name,
			ThrowingEx.Supplier<FormatterFunc> functionSupplier) {
		return new NeverUpToDateStep(name, functionSupplier);
	}

	/**
	 * @param name
	 *             The name of the formatter step
	 * @param function
	 *             The function used by the formatter step
	 * @return A FormatterStep which will never report that it is up-to-date, because
	 *         it is not equal to the serialized representation of itself.
	 */
	public static FormatterStep create(
			String name,
			FormatterFunc function) {
		Objects.requireNonNull(function, "function");
		return createLazy(name, () -> function);
	}

	private static final long serialVersionUID = 1L;

	private final String name;
	private final ThrowingEx.Supplier<FormatterFunc> formatterSupplier;
	private transient FormatterFunc formatter; // initialized lazily

	NeverUpToDateStep(String name, ThrowingEx.Supplier<FormatterFunc> formatterSupplier) {
		this.name = name;
		this.formatterSupplier = Objects.requireNonNull(formatterSupplier, "formatterSupplier");
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String format(String rawUnix, File file) throws Exception {
		if (formatter == null) {
			formatter = formatterSupplier.get();
			if (formatter instanceof FormatterFunc.Closeable) {
				throw new AssertionError("NeverUpToDate does not support FormatterFunc.Closeable.  See https://github.com/diffplug/spotless/pull/284");
			}
		}
		return formatter.apply(rawUnix, file);
	}

	@Override
	public void close() throws Exception {
		if (formatter instanceof FormatterFunc.Closeable) {
			((FormatterFunc.Closeable) formatter).close();
			formatter = null;
		}
	}
}
