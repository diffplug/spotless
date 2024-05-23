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

import java.util.Objects;

import org.assertj.core.api.Assertions;

class StepHarnessBase implements AutoCloseable {
	enum RoundTrip {
		ASSERT_EQUAL, DONT_ASSERT_EQUAL, DONT_ROUNDTRIP
	}

	private final Formatter formatter;

	protected StepHarnessBase(Formatter formatter, RoundTrip roundTrip) {
		if (roundTrip == RoundTrip.DONT_ROUNDTRIP) {
			this.formatter = Objects.requireNonNull(formatter);
			return;
		}
		Formatter roundTripped = SerializableEqualityTester.reserialize(formatter);
		if (roundTrip == RoundTrip.ASSERT_EQUAL) {
			Assertions.assertThat(roundTripped).isEqualTo(formatter);
		}
		this.formatter = roundTripped;
	}

	protected Formatter formatter() {
		return formatter;
	}

	@Override
	public void close() {
		formatter.close();
	}
}
