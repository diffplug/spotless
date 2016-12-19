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

import java.io.File;
import java.io.Serializable;
import java.util.Objects;
import java.util.Random;

import com.diffplug.spotless.FormatterStep.Strict;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Standard implementation of FormatExtension which cleanly enforces
 * separation of serializable configuration and a pure format function.
 *
 * Not an inner-class of FormatterStep so that it can stay entirely private
 * from the API.
 */
@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
abstract class FormatterStepImpl<Key extends Serializable> extends Strict<Key> {
	private static final long serialVersionUID = 1L;

	/** Transient because only the key matters. */
	final transient String name;

	FormatterStepImpl(String name, Key key) {
		super(key);
		this.name = Objects.requireNonNull(name);
	}

	@Override
	public String getName() {
		return name;
	}

	static final class Standard<Key extends Serializable> extends FormatterStepImpl<Key> {
		private static final long serialVersionUID = 1L;

		final transient ThrowingEx.Function<Key, FormatterFunc> keyToFormatter;
		transient FormatterFunc formatter; // initialized lazily

		Standard(String name, Key key, ThrowingEx.Function<Key, FormatterFunc> keyToFormatter) {
			super(name, key);
			this.keyToFormatter = Objects.requireNonNull(keyToFormatter);
		}

		@Override
		protected String format(Key key, String rawUnix, File file) throws Exception {
			if (formatter == null) {
				formatter = keyToFormatter.apply(key());
			}
			return formatter.apply(rawUnix);
		}
	}

	static class Closeable<Key extends Serializable> extends FormatterStepImpl<Key> {
		private static final long serialVersionUID = 1L;

		final transient ThrowingEx.Function<Key, FormatterFunc.Closeable> keyToFormatter;
		transient FormatterFunc.Closeable formatter; // initialized lazily

		Closeable(String name, Key key, ThrowingEx.Function<Key, FormatterFunc.Closeable> keyToFormatter) {
			super(name, key);
			this.keyToFormatter = Objects.requireNonNull(keyToFormatter);
		}

		@Override
		protected String format(Key key, String rawUnix, File file) throws Exception {
			if (formatter == null) {
				formatter = keyToFormatter.apply(key());
			}
			return formatter.apply(rawUnix);
		}

		@Override
		public void finish() {
			if (formatter != null) {
				formatter.close();
				formatter = null;
			}
		}
	}

	/** Formatter which is equal to itself, but not to any other Formatter. */
	static class LazyForwardingStep extends LazyForwardingEquality<FormatterStep> implements FormatterStep {
		private static final long serialVersionUID = 1L;

		final transient String name;
		final transient ThrowingEx.Supplier<FormatterStep> supplier;

		public LazyForwardingStep(String name, ThrowingEx.Supplier<FormatterStep> supplier) {
			this.name = Objects.requireNonNull(name);
			this.supplier = Objects.requireNonNull(supplier);
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String format(String rawUnix, File file) throws Exception {
			return key().format(rawUnix, file);
		}

		@Override
		protected FormatterStep calculateKey() throws Exception {
			return supplier.get();
		}
	}

	/** Formatter which is equal to itself, but not to any other Formatter. */
	static class NeverUpToDate extends LazyForwardingEquality<Integer> implements FormatterStep {
		private static final long serialVersionUID = 1L;

		private static final Random RANDOM = new Random();

		final transient String name;
		final transient ThrowingEx.Supplier<FormatterFunc> formatterSupplier;
		transient FormatterFunc formatter; // initialized lazily

		NeverUpToDate(String name, ThrowingEx.Supplier<FormatterFunc> formatterSupplier) {
			this.name = name;
			this.formatterSupplier = formatterSupplier;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String format(String rawUnix, File file) throws Exception {
			if (formatter == null) {
				formatter = formatterSupplier.get();
			}
			return formatter.apply(rawUnix);
		}

		@Override
		protected Integer calculateKey() throws Exception {
			return RANDOM.nextInt();
		}
	}
}
