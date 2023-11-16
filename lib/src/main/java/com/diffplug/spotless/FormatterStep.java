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

import javax.annotation.Nullable;

/**
 * An implementation of this class specifies a single step in a formatting process.
 * <p>
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
	 *            the content to format, guaranteed to have unix-style newlines ('\n'); never null
	 * @param file
	 *            the file which {@code rawUnix} was obtained from; never null. Pass the reference
	 *            {@link Formatter#NO_FILE_SENTINEL} if and only if no file is actually associated with {@code rawUnix}
	 * @return the formatted content, guaranteed to only have unix-style newlines; may return null
	 *         if the formatter step doesn't have any changes to make
	 * @throws Exception if the formatter step experiences a problem
	 */
	public @Nullable String format(String rawUnix, File file) throws Exception;

	/**
	 * Returns a new FormatterStep which will only apply its changes
	 * to files which pass the given filter.
	 *
	 * @param contentPattern
	 *            java regular expression used to filter out files which content doesn't contain pattern
	 * @return FormatterStep
	 */
	@Deprecated
	public default FormatterStep filterByContentPattern(String contentPattern) {
		return filterByContent(OnMatch.INCLUDE, contentPattern);
	}

	/**
	 * Returns a new {@code FormatterStep} which, observing the value of {@code formatIfMatches},
	 * will only apply, or not, its changes to files which pass the given filter.
	 *
	 * @param onMatch
	 *            determines if matches are included or excluded
	 * @param contentPattern
	 *            java regular expression used to filter in or out files which content contain pattern
	 * @return FormatterStep
	 */
	public default FormatterStep filterByContent(OnMatch onMatch, String contentPattern) {
		return new FilterByContentPatternFormatterStep(this, onMatch, contentPattern);
	}

	/**
	 * Returns a new FormatterStep which will only apply its changes
	 * to files which pass the given filter.
	 * <p>
	 * The provided filter must be serializable.
	 */
	public default FormatterStep filterByFile(SerializableFileFilter filter) {
		return new FilterByFileFormatterStep(this, filter);
	}

	/**
	 * Implements a FormatterStep in a strict way which guarantees correct and lazy implementation
	 * of up-to-date checks.  This maximizes performance for cases where the FormatterStep is not
	 * actually needed (e.g. don't load eclipse setting file unless this step is actually running)
	 * while also ensuring that Gradle can detect changes in a step's settings to determine that
	 * it needs to rerun a format.
	 */
	abstract class Strict<State extends Serializable> extends LazyForwardingEquality<State> implements FormatterStep {
		private static final long serialVersionUID = 1L;

		/**
		 * Implements the formatting function strictly in terms
		 * of the input data and the result of {@link #calculateState()}.
		 */
		protected abstract String format(State state, String rawUnix, File file) throws Exception;

		@Override
		public final String format(String rawUnix, File file) throws Exception {
			return format(state(), rawUnix, file);
		}
	}

	/**
	 * @param name
	 *             The name of the formatter step
	 * @param stateSupplier
	 *             If the rule has any state, this supplier will calculate it lazily, and the result
	 *             will be passed to stateToFormatter
	 * @param stateToFormatter
	 *             A pure function which generates a formatting function using
	 *             only the state supplied by state and nowhere else.
	 * @return A FormatterStep
	 */
	public static <State extends Serializable> FormatterStep createLazy(
			String name,
			ThrowingEx.Supplier<State> stateSupplier,
			ThrowingEx.Function<State, FormatterFunc> stateToFormatter) {
		return new FormatterStepImpl.Standard<>(name, stateSupplier, stateToFormatter);
	}

	/**
	 * @param name
	 *             The name of the formatter step
	 * @param state
	 *             If the rule has any state, this state must contain all of it
	 * @param stateToFormatter
	 *             A pure function which generates a formatting function using
	 *             only the state supplied by state and nowhere else.
	 * @return A FormatterStep
	 */
	public static <State extends Serializable> FormatterStep create(
			String name,
			State state,
			ThrowingEx.Function<State, FormatterFunc> stateToFormatter) {
		Objects.requireNonNull(state, "state");
		return createLazy(name, () -> state, stateToFormatter);
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
		Objects.requireNonNull(function, "function");
		return createNeverUpToDateLazy(name, () -> function);
	}
}
