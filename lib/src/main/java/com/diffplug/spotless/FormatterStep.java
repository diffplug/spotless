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

/**
 * An implementation of this class specifies a single step in a formatting process.
 *
 * The input is guaranteed to have unix-style newlines, and the output is required
 * to not introduce any windows-style newlines as well.
 */
public interface FormatterStep extends Serializable {
	/** The name of the step, for debugging purposes. */
	public String getName();

	/**
	 * Returns a formatted version of the given content.
	 *
	 * @param rawUnix
	 *            File's content, guaranteed to have unix-style newlines ('\n')
	 * @param file
	 *            the File which is being formatted
	 * @return The formatted content, guaranteed to only have unix-style newlines
	 * @throws Exception when the formatter steps experiences a problem
	 */
	public String format(String rawUnix, File file) throws Exception;

	/**
	 * Hint to the FormatterStep that {@link #format(String, File)} will not
	 * be called anytime soon, so clean up any resources that are being used.
	 * Does NOT guarantee that format() won't be called ever again, but does
	 * guarantee to be the best possible time to clean that you're going to get.
	 */
	public default void finish() {}

	/**
	 * Returns a new FormatterStep which will only apply its changes
	 * to files which pass the given filter.
	 *
	 * The provided filter must be serializable.
	 */
	public default FormatterStep filterByFile(SerializableFileFilter filter) {
		return new FilterByFileFormatterStep(this, filter);
	}

	/**
	 * Implements a FormatterStep in a strict way which guarantees correct and lazy implementation
	 * of up-to-date checks.  This maximizes performance for cases where the FormatterStep is not
	 * actually needed (e.g. don't load eclipse setting file unless this step is actually running)
	 * while also ensuring that gradle can detect changes in a step's settings to determine that
	 * it needs to rerun a format.
	 */
	abstract class Strict<Key extends Serializable> extends LazyForwardingEquality<Key> implements FormatterStep {
		private static final long serialVersionUID = 1L;

		/**
		 * Implements the formatting function strictly in terms
		 * of the input data and the result of {@link #calculateKey()}.
		 */
		protected abstract String format(Key key, String rawUnix, File file) throws Exception;

		@Override
		public final String format(String rawUnix, File file) throws Exception {
			return format(key(), rawUnix, file);
		}
	}

	/**
	 * @param name
	 *             The name of the formatter step
	 * @param keySupplier
	 *             If the rule has any state, this supplier will calculate it lazily, and the result
	 *             will be passed to keyToFormatter
	 * @param keyToFormatter
	 *             A pure function which generates a formatting function using
	 *             only the state supplied by key and nowhere else.
	 * @return A FormatterStep
	 */
	public static <Key extends Serializable> FormatterStep createLazy(
			String name,
			ThrowingEx.Supplier<Key> keySupplier,
			ThrowingEx.Function<Key, FormatterFunc> keyToFormatter) {
		return new FormatterStepImpl.Standard<>(name, keySupplier, keyToFormatter);
	}

	/**
	 * @param name
	 *             The name of the formatter step
	 * @param key
	 *             If the rule has any state, this key must contain all of it
	 * @param keyToFormatter
	 *             A pure function which generates a formatting function using
	 *             only the state supplied by key and nowhere else.
	 * @return A FormatterStep
	 */
	public static <Key extends Serializable> FormatterStep create(
			String name,
			Key key,
			ThrowingEx.Function<Key, FormatterFunc> keyToFormatter) {
		return createLazy(name, () -> key, keyToFormatter);
	}

	/**
	 * @param name
	 *             The name of the formatter step
	 * @param keySupplier
	 *             If the rule has any state, this supplier will calculate it lazily, and the result
	 *             will be passed to keyToFormatter
	 * @param keyToFormatter
	 *             A pure function which generates a closeable formatting function using
	 *             only the state supplied by key and nowhere else.
	 * @return A FormatterStep
	 */
	public static <Key extends Serializable> FormatterStep createCloseableLazy(
			String name,
			ThrowingEx.Supplier<Key> keySupplier,
			ThrowingEx.Function<Key, FormatterFunc.Closeable> keyToFormatter) {
		return new FormatterStepImpl.Closeable<>(name, keySupplier, keyToFormatter);
	}

	/**
	 * @param name
	 *             The name of the formatter step
	 * @param key
	 *             If the rule has any state, this key must contain all of it
	 * @param keyToFormatter
	 *             A pure function which generates a formatting function using
	 *             only the state supplied by key and nowhere else.
	 * @return A FormatterStep
	 */
	public static <Key extends Serializable> FormatterStep createCloseable(
			String name,
			Key key,
			ThrowingEx.Function<Key, FormatterFunc.Closeable> keyToFormatter) {
		return createCloseableLazy(name, () -> key, keyToFormatter);
	}

	/**
	 * @param name
	 *             The name of the formatter step
	 * @param functionSupplier
	 *             A supplier which will lazily generate the function
	 *             used by the formatter step
	 * @return A FormatterStep which will never report that it is up-to-date, because
	 *         it is not equal to the serialized representation of itself.
	 */
	public static FormatterStep createNeverUpToDateLazy(
			String name,
			ThrowingEx.Supplier<FormatterFunc> functionSupplier) {
		return new FormatterStepImpl.NeverUpToDate(name, functionSupplier);
	}

	/**
	 * @param name
	 *             The name of the formatter step
	 * @param function
	 *             The function used by the formatter step
	 * @return A FormatterStep which will never report that it is up-to-date, because
	 *         it is not equal to the serialized representation of itself.
	 */
	public static FormatterStep createNeverUpToDate(
			String name,
			FormatterFunc function) {
		return createNeverUpToDateLazy(name, () -> function);
	}
}
