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
import java.util.Random;

/**
 * Formatter which is equal to itself, but not to any other Formatter.
 */
class NeverUpToDateStep extends FormatterStepImpl<Integer> {
	private static final long serialVersionUID = 1L;

	private static final Random RANDOM = new Random();

	final transient ThrowingEx.Supplier<FormatterFunc> formatterSupplier;
	transient FormatterFunc formatter; // initialized lazily

	NeverUpToDateStep(String name, ThrowingEx.Supplier<FormatterFunc> formatterSupplier) {
		super(name, RANDOM::nextInt);
		this.formatterSupplier = Objects.requireNonNull(formatterSupplier, "formatterSupplier");
	}

	@Override
	protected String format(Integer state, String rawUnix, File file) throws Exception {
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
