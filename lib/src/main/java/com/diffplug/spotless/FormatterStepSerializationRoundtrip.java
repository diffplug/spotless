/*
 * Copyright 2023 DiffPlug
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

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;

import edu.umd.cs.findbugs.annotations.Nullable;

class FormatterStepSerializationRoundtrip<RoundtripState extends Serializable, EqualityState extends Serializable> extends FormatterStepEqualityOnStateSerialization<EqualityState> {
	private static final long serialVersionUID = 1L;
	private final String name;
	private final transient ThrowingEx.Supplier<RoundtripState> initializer;
	private @Nullable RoundtripState roundtripStateInternal;
	private final SerializedFunction<RoundtripState, EqualityState> equalityStateExtractor;
	private final SerializedFunction<EqualityState, FormatterFunc> equalityStateToFormatter;

	public FormatterStepSerializationRoundtrip(String name, ThrowingEx.Supplier<RoundtripState> initializer, SerializedFunction<RoundtripState, EqualityState> equalityStateExtractor, SerializedFunction<EqualityState, FormatterFunc> equalityStateToFormatter) {
		this.name = name;
		this.initializer = initializer;
		this.equalityStateExtractor = equalityStateExtractor;
		this.equalityStateToFormatter = equalityStateToFormatter;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	protected EqualityState stateSupplier() throws Exception {
		if (roundtripStateInternal == null) {
			roundtripStateInternal = initializer.get();
		}
		return equalityStateExtractor.apply(roundtripStateInternal);
	}

	@Override
	protected FormatterFunc stateToFormatter(EqualityState equalityState) throws Exception {
		return equalityStateToFormatter.apply(equalityState);
	}

	// override serialize output
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		if (roundtripStateInternal == null) {
			roundtripStateInternal = ThrowingEx.get(initializer::get);
		}
		out.defaultWriteObject();
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
	}

	private void readObjectNoData() throws ObjectStreamException {
		throw new UnsupportedOperationException();
	}
}
