/*
 * Copyright 2024-2026 DiffPlug
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

import java.io.Serializable;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.generic.FenceStep;

class ConfigurationCacheHackListTest {
	private static FormatterStep roundtripStep(String name, Serializable equalityState) {
		// mirrors FormatterStep.createLazy(name, roundtripInit, equalityFunc, formatterFunc)
		return FormatterStep.createLazy(name,
				() -> "roundtrip-" + name,
				rt -> equalityState,
				eq -> (FormatterFunc) (s -> s));
	}

	private static FormatterStep toggleOffOn(FormatterStep... within) {
		return FenceStep.named(FenceStep.defaultToggleName())
				.openClose(FenceStep.defaultToggleOff(), FenceStep.defaultToggleOn())
				.preserveWithin(List.of(within));
	}

	/** Gradle fingerprints the task's input (a {@link ConfigurationCacheHackList}) by calling hashCode(). */
	private static int fingerprint(FormatterStep... steps) {
		ConfigurationCacheHackList list = ConfigurationCacheHackList.forEquality();
		list.addAll(List.of(steps));
		return list.hashCode();
	}

	@Test
	void plainStepCanBeFingerprinted() {
		fingerprint(roundtripStep("plain", "eq-state"));
	}

	@Test
	void fenceWrappingStepCanBeFingerprinted() {
		fingerprint(toggleOffOn(roundtripStep("inner", "eq-state")));
	}

	/**
	 * A sub-step whose equality state is null (e.g. a not-yet-provisioned promised state, as with
	 * greclipse in a clean CI environment) must not break Gradle's input fingerprinting. See #2950.
	 */
	@Test
	void fenceWrappingStepWithNullEqualityStateCanBeFingerprinted() {
		fingerprint(toggleOffOn(roundtripStep("inner-null-eq", null)));
	}
}
