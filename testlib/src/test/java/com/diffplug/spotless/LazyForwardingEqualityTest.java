/*
 * Copyright 2016-2026 DiffPlug
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

import static com.diffplug.common.testing.SerializableTester.reserializeAndAssert;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.diffplug.common.testing.EqualsTester;
import com.diffplug.spotless.generic.FenceStep;

@SuppressWarnings("serial")
class LazyForwardingEqualityTest {
	static Str s(String state) {
		return new Str(state);
	}

	static Other o(String state) {
		return new Other(state);
	}

	static class Str extends LazyForwardingEquality<String> {
		private String state;

		Str(String state) {
			this.state = state;
		}

		@Override
		protected String calculateState() {
			return state;
		}
	}

	static class Other extends Str {
		Other(String state) {
			super(state);
		}
	}

	@Test
	void unlazyEvaluatesRoundtripStepState() {
		AtomicInteger stateCalculations = new AtomicInteger();
		FormatterStep step = countingStep(stateCalculations);

		LazyForwardingEquality.unlazy(step);

		assertThat(stateCalculations).hasValue(1);
	}

	@Test
	void unlazyEvaluatesFilteredSteps() {
		AtomicInteger stateCalculations = new AtomicInteger();
		FormatterStep step = countingStep(stateCalculations)
				.filterByFile(SerializableFileFilter.skipFilesNamed("skip"))
				.filterByContent(OnMatch.INCLUDE, ".*");

		LazyForwardingEquality.unlazy(List.of(step));

		assertThat(stateCalculations).hasValue(1);
	}

	@Test
	void unlazyEvaluatesFencedSteps() {
		AtomicInteger stateCalculations = new AtomicInteger();
		FormatterStep step = FenceStep.named("nested").openClose("begin", "end")
				.applyWithin(List.of(countingStep(stateCalculations)));

		LazyForwardingEquality.unlazy(List.of(step));

		assertThat(stateCalculations).hasValue(1);
	}

	private static FormatterStep countingStep(AtomicInteger stateCalculations) {
		return FormatterStep.create("test", "state", state -> {
			stateCalculations.incrementAndGet();
			return state;
		}, state -> input -> input);
	}

	@Test
	void testEquality() {
		new EqualsTester()
				.addEqualityGroup(s("hello"), reserializeAndAssert(s("hello")))
				.addEqualityGroup(s("world"), reserializeAndAssert(s("world")))
				.addEqualityGroup(o("hello"), reserializeAndAssert(o("hello")))
				.addEqualityGroup(o("world"), reserializeAndAssert(o("world")))
				.testEquals();
	}
}
