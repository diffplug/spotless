/*
 * Copyright 2016-2025 DiffPlug
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
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

/**
 * An implementation of this class specifies a single step in a formatting process.
 * <p>
 * The input is guaranteed to have unix-style newlines, and the output is required
 * to not introduce any windows-style newlines as well.
 */
public interface FormatterStep extends Serializable, AutoCloseable {
	/** The name of the step, for debugging purposes. */
	String getName();

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
	@Nullable
	String format(String rawUnix, File file) throws Exception;

	/**
	 * Returns a list of lints against the given file content
	 *
	 * @param content
	 *            the content to check
	 * @param file
	 *            the file which {@code content} was obtained from; never null. Pass an empty file using
	 *            {@code new File("")} if and only if no file is actually associated with {@code content}
	 * @return a list of lints
	 * @throws Exception if the formatter step experiences a problem
	 */
	@Nullable
	default List<Lint> lint(String content, File file) throws Exception {
		return List.of();
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
	default FormatterStep filterByContent(OnMatch onMatch, String contentPattern) {
		return new FilterByContentPatternFormatterStep(this, onMatch, contentPattern);
	}

	/**
	 * Returns a new FormatterStep which will only apply its changes
	 * to files which pass the given filter.
	 * <p>
	 * The provided filter must be serializable.
	 */
	default FormatterStep filterByFile(SerializableFileFilter filter) {
		return new FilterByFileFormatterStep(this, filter);
	}

	/**
	 * @param name
	 *             The name of the formatter step.
	 * @param roundtripInit
	 *             If the step has any state, this supplier will calculate it lazily. The supplier doesn't
	 *             have to be serializable, but the result it calculates needs to be serializable.
	 * @param equalityFunc
	 * 		       A pure serializable function (method reference recommended) which takes the result of `roundtripInit`,
	 * 		       and returns a serializable object whose serialized representation will be used for `.equals` and
	 * 		       `.hashCode` of the FormatterStep.
	 * @param formatterFunc
	 * 		       A pure serializable function (method reference recommended) which takes the result of `equalityFunc`,
	 * 		       and returns a `FormatterFunc` which will be used for the actual formatting.
	 * @return A FormatterStep which can be losslessly roundtripped through the java serialization machinery.
	 */
	static <RoundtripState extends Serializable, EqualityState extends Serializable> FormatterStep createLazy(
			String name,
			ThrowingEx.Supplier<RoundtripState> roundtripInit,
			SerializedFunction<RoundtripState, EqualityState> equalityFunc,
			SerializedFunction<EqualityState, ? extends FormatterFunc> formatterFunc) {
		return new FormatterStepSerializationRoundtrip<>(name, roundtripInit, equalityFunc, formatterFunc);
	}

	/**
	 * @param name
	 *             The name of the formatter step.
	 * @param roundTrip
	 *             The roundtrip serializable state of the step.
	 * @param equalityFunc
	 * 		       A pure serializable function (method reference recommended) which takes the result of `roundTrip`,
	 * 		       and returns a serializable object whose serialized representation will be used for `.equals` and
	 * 		       `.hashCode` of the FormatterStep.
	 * @param formatterFunc
	 * 		       A pure serializable function (method reference recommended) which takes the result of `equalityFunc`,
	 * 		       and returns a `FormatterFunc` which will be used for the actual formatting.
	 * @return A FormatterStep which can be losslessly roundtripped through the java serialization machinery.
	 */
	static <RoundtripState extends Serializable, EqualityState extends Serializable> FormatterStep create(
			String name,
			RoundtripState roundTrip,
			SerializedFunction<RoundtripState, EqualityState> equalityFunc,
			SerializedFunction<EqualityState, ? extends FormatterFunc> formatterFunc) {
		return createLazy(name, () -> roundTrip, equalityFunc, formatterFunc);
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
	static <State extends Serializable> FormatterStep createLazy(
			String name,
			ThrowingEx.Supplier<State> stateSupplier,
			SerializedFunction<State, FormatterFunc> stateToFormatter) {
		return createLazy(name, stateSupplier, SerializedFunction.identity(), stateToFormatter);
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
	static <State extends Serializable> FormatterStep create(
			String name,
			State state,
			SerializedFunction<State, FormatterFunc> stateToFormatter) {
		Objects.requireNonNull(state, "state");
		return createLazy(name, () -> state, stateToFormatter);
	}
}
