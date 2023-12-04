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

import java.util.Objects;

import org.assertj.core.api.Assertions;

class StepHarnessBase<T extends StepHarnessBase<?>> implements AutoCloseable {
	private final Formatter formatter;
	private Formatter roundTripped;
	private boolean supportsRoundTrip = false;

	protected StepHarnessBase(Formatter formatter) {
		this.formatter = Objects.requireNonNull(formatter);
	}

	public T supportsRoundTrip(boolean supportsRoundTrip) {
		this.supportsRoundTrip = supportsRoundTrip;
		return (T) this;
	}

	protected Formatter formatter() {
		if (!supportsRoundTrip) {
			return formatter;
		} else {
			if (roundTripped == null) {
				roundTripped = SerializableEqualityTester.reserialize(formatter);
				Assertions.assertThat(roundTripped).isEqualTo(formatter);
			}
			return roundTripped;
		}
	}

	@Override
	public void close() {
		formatter.close();
		if (roundTripped != null) {
			roundTripped.close();
		}
	}
}
