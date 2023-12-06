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

import java.util.Locale;
import java.util.Objects;

import org.assertj.core.api.Assertions;

class StepHarnessBase<T extends StepHarnessBase<?>> implements AutoCloseable {
	private final Formatter formatter;
	private Formatter roundTripped;
	private boolean supportsRoundTrip = false;

	protected StepHarnessBase(Formatter formatter) {
		this.formatter = Objects.requireNonNull(formatter);
		if (formatter.getSteps().size() == 1) {
			// our goal is for everything to be roundtrip serializable
			// the steps to get there are
			// - make every individual step round-trippable
			// - make the other machinery (Formatter, LineEnding, etc) round-trippable
			// - done!
			//
			// Right now, we're still trying to get each and every single step to be round-trippable.
			// You can help by add a test below, make sure that the test for that step fails, and then
			// make the test pass. `FormatterStepEqualityOnStateSerialization` is a good base class for
			// easily converting a step to round-trip serialization while maintaining easy and concise
			// equality code.
			String onlyStepName = formatter.getSteps().get(0).getName();
			if (onlyStepName.startsWith("indentWith")) {
				supportsRoundTrip = true;
			} else if (onlyStepName.equals("diktat")) {
				supportsRoundTrip = true;
			} else if (onlyStepName.toLowerCase(Locale.ROOT).contains("eclipse")) {
				supportsRoundTrip = true;
			} else if (onlyStepName.equals("fence")) {
				supportsRoundTrip = true;
			} else if (onlyStepName.equals("buf")) {
				supportsRoundTrip = true;
			}
		}
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
