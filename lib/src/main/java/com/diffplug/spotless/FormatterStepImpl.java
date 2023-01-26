/*
 * Copyright 2016-2023 DiffPlug
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
abstract class FormatterStepImpl<State extends Serializable> extends Strict<State> {
	private static final long serialVersionUID = 1L;

	/** Transient because only the state matters. */
	final transient String name;

	/** Transient because only the state matters. */
	transient ThrowingEx.Supplier<State> stateSupplier;

	FormatterStepImpl(String name, ThrowingEx.Supplier<State> stateSupplier) {
		this.name = Objects.requireNonNull(name);
		this.stateSupplier = Objects.requireNonNull(stateSupplier);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	protected State calculateState() throws Exception {
		// LazyForwardingEquality guarantees that this will only be called once, and keeping toFormat
		// causes a memory leak, see https://github.com/diffplug/spotless/issues/1194
		State state = stateSupplier.get();
		stateSupplier = null;
		return state;
	}

	static final class Standard<State extends Serializable> extends FormatterStepImpl<State> {
		private static final long serialVersionUID = 1L;

		final transient ThrowingEx.Function<State, FormatterFunc> stateToFormatter;
		transient FormatterFunc formatter; // initialized lazily

		Standard(String name, ThrowingEx.Supplier<State> stateSupplier, ThrowingEx.Function<State, FormatterFunc> stateToFormatter) {
			super(name, stateSupplier);
			this.stateToFormatter = Objects.requireNonNull(stateToFormatter);
		}

		@Override
		protected String format(State state, String rawUnix, File file) throws Exception {
			Objects.requireNonNull(state, "state");
			Objects.requireNonNull(rawUnix, "rawUnix");
			Objects.requireNonNull(file, "file");
			if (formatter == null) {
				formatter = stateToFormatter.apply(state());
			}
			return formatter.apply(rawUnix, file);
		}

		void cleanupFormatterFunc() {
			if (formatter instanceof FormatterFunc.Closeable) {
				((FormatterFunc.Closeable) formatter).close();
				formatter = null;
			}
		}
	}

	/** Formatter which is equal to itself, but not to any other Formatter. */
	static class NeverUpToDate extends FormatterStepImpl<Integer> {
		private static final long serialVersionUID = 1L;

		private static final Random RANDOM = new Random();

		final transient ThrowingEx.Supplier<FormatterFunc> formatterSupplier;
		transient FormatterFunc formatter; // initialized lazily

		NeverUpToDate(String name, ThrowingEx.Supplier<FormatterFunc> formatterSupplier) {
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
	}

	static void checkNotSentinel(File file) {
		if (file == Formatter.NO_FILE_SENTINEL) {
			throw new IllegalArgumentException("This step requires the underlying file. If this is a test, use StepHarnessWithFile");
		}
	}
}
