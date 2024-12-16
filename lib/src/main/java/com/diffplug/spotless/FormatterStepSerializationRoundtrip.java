/*
 * Copyright 2023-2024 DiffPlug
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
import java.io.Serializable;
import java.util.Objects;

import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

final class FormatterStepSerializationRoundtrip<RoundtripState extends Serializable, EqualityState extends Serializable> extends FormatterStepEqualityOnStateSerialization<EqualityState> {
	private static final long serialVersionUID = 1L;
	private final String name;
	@SuppressFBWarnings(value = "SE_TRANSIENT_FIELD_NOT_RESTORED", justification = "HackClone")
	private final transient ThrowingEx.Supplier<RoundtripState> initializer;
	private @Nullable RoundtripState roundtripStateInternal;
	private @Nullable EqualityState equalityStateInternal;
	private final SerializedFunction<RoundtripState, EqualityState> equalityStateExtractor;
	private final SerializedFunction<EqualityState, FormatterFunc> equalityStateToFormatter;

	FormatterStepSerializationRoundtrip(String name, ThrowingEx.Supplier<RoundtripState> initializer, SerializedFunction<RoundtripState, EqualityState> equalityStateExtractor, SerializedFunction<EqualityState, FormatterFunc> equalityStateToFormatter) {
		this.name = name;
		this.initializer = initializer;
		this.equalityStateExtractor = equalityStateExtractor;
		this.equalityStateToFormatter = equalityStateToFormatter;
	}

	@Override
	public String getName() {
		return name;
	}

	private RoundtripState roundtripStateSupplier() throws Exception {
		if (roundtripStateInternal == null) {
			roundtripStateInternal = initializer.get();
		}
		return roundtripStateInternal;
	}

	@Override
	protected EqualityState stateSupplier() throws Exception {
		if (equalityStateInternal == null) {
			equalityStateInternal = equalityStateExtractor.apply(roundtripStateSupplier());
		}
		return equalityStateInternal;
	}

	@Override
	protected FormatterFunc stateToFormatter(EqualityState equalityState) throws Exception {
		return equalityStateToFormatter.apply(equalityState);
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		if (initializer == null) {
			// then this instance was created by Gradle's ConfigurationCacheHackList and the following will hold true
			if (roundtripStateInternal == null && equalityStateInternal == null) {
				throw new IllegalStateException("If the initializer was null, then one of roundtripStateInternal or equalityStateInternal should be non-null, and neither was");
			}
		} else {
			// this was a normal instance, which means we need to encode to roundtripStateInternal (since the initializer might not be serializable)
			// and there's no reason to keep equalityStateInternal since we can always recompute it
			if (roundtripStateInternal == null) {
				roundtripStateInternal = ThrowingEx.get(this::roundtripStateSupplier);
			}
			equalityStateInternal = null;
		}
		out.defaultWriteObject();
	}

	HackClone<?, ?> hackClone(boolean optimizeForEquality) {
		return new HackClone<>(this, optimizeForEquality);
	}

	/**
	 * This class has one setting (optimizeForEquality) and two pieces of data
	 * - the original step, which is marked transient so it gets discarded during serialization
	 * - the cleaned step, which is lazily created during serialization, and the serialized form is optimized for either equality or roundtrip integrity
	 *
	 * It works in conjunction with ConfigurationCacheHackList to allow Spotless to work with all of Gradle's cache systems.
	 */
	static class HackClone<RoundtripState extends Serializable, EqualityState extends Serializable> implements Serializable {
		private static final long serialVersionUID = 1L;
		@SuppressFBWarnings(value = "SE_TRANSIENT_FIELD_NOT_RESTORED", justification = "HackClone")
		transient FormatterStepSerializationRoundtrip<?, ?> original;
		boolean optimizeForEquality;
		@Nullable
		FormatterStepSerializationRoundtrip cleaned;

		HackClone(@Nullable FormatterStepSerializationRoundtrip<RoundtripState, EqualityState> original, boolean optimizeForEquality) {
			this.original = original;
			this.optimizeForEquality = optimizeForEquality;
		}

		@SuppressFBWarnings(value = "NP_NONNULL_PARAM_VIOLATION", justification = "HackClone")
		private void writeObject(java.io.ObjectOutputStream out) throws IOException {
			if (cleaned == null) {
				cleaned = new FormatterStepSerializationRoundtrip(original.name, null, original.equalityStateExtractor, original.equalityStateToFormatter);
				if (optimizeForEquality) {
					cleaned.equalityStateInternal = ThrowingEx.get(original::stateSupplier);
				} else {
					cleaned.roundtripStateInternal = ThrowingEx.get(original::roundtripStateSupplier);
				}
			}
			out.defaultWriteObject();
		}

		public FormatterStep rehydrate() {
			return original != null ? original : Objects.requireNonNull(cleaned, "how is clean null if this has been serialized?");
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			HackClone<?, ?> that = (HackClone<?, ?>) o;
			return optimizeForEquality == that.optimizeForEquality && rehydrate().equals(that.rehydrate());
		}

		@Override
		public int hashCode() {
			return rehydrate().hashCode() ^ Boolean.hashCode(optimizeForEquality);
		}
	}
}
