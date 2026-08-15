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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class ConfigurationCacheHackListTest {

	/** Step whose equality/hashCode/serialization forces state evaluation. */
	private static FormatterStep lazyStep(String name, AtomicInteger stateEvals, Serializable state) {
		return FormatterStep.createLazy(name,
				() -> {
					stateEvals.incrementAndGet();
					return state;
				},
				SerializedFunction.identity(),
				eq -> (FormatterFunc) (s -> s));
	}

	/** Step whose state evaluation always fails, like an unresolvable P2/Maven dependency. */
	private static FormatterStep explodingStep(String name, AtomicInteger stateEvals, String message) {
		return FormatterStep.createLazy(name,
				() -> {
					stateEvals.incrementAndGet();
					throw new RuntimeException(message);
				},
				SerializedFunction.identity(),
				eq -> (FormatterFunc) (s -> s));
	}

	@Test
	void toStringReportsSerializationFailureWithoutReEvaluating() throws Exception {
		AtomicInteger evals = new AtomicInteger();
		ConfigurationCacheHackList list = ConfigurationCacheHackList.forEquality();
		list.addAll(List.of(explodingStep("unresolvable", evals, "P2 dependencies not predeclared")));

		try (ObjectOutputStream out = new ObjectOutputStream(new ByteArrayOutputStream())) {
			assertThatThrownBy(() -> out.writeObject(list)).isNotNull();
		}
		int evalsAfterSerialize = evals.get();
		assertThat(evalsAfterSerialize).as("serialization evaluates state").isPositive();

		// Gradle renders this value into "cannot be serialized" and drops the cause, so the
		// actionable message has to survive here or the user never sees it (#3004).
		assertThat(list.toString()).contains("P2 dependencies not predeclared");
		assertThat(evals.get()).as("toString must not re-evaluate step state").isEqualTo(evalsAfterSerialize);
	}

	@Test
	void toStringDoesNotEvaluateStepState() {
		AtomicInteger evals = new AtomicInteger();
		ConfigurationCacheHackList list = ConfigurationCacheHackList.forEquality();
		list.addAll(List.of(lazyStep("expensive", evals, "state")));

		// Gradle includes this value in "cannot be serialized" error messages.
		// Default Object.toString() calls hashCode(), which fingerprints steps and
		// may provision P2 deps — re-triggering the failure being reported (#3004).
		String text = list.toString();
		assertThat(text).contains("ConfigurationCacheHackList");
		assertThat(text).contains("optimizeForEquality=true");
		assertThat(text).contains("size=1");
		assertThat(evals.get()).as("toString must not evaluate step state").isZero();
	}

	@Test
	void equalityListRoundtripsThroughJavaSerialization() throws Exception {
		AtomicInteger evals = new AtomicInteger();
		ConfigurationCacheHackList original = ConfigurationCacheHackList.forEquality();
		original.addAll(List.of(lazyStep("plain", evals, "eq-state")));

		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
			out.writeObject(original);
		}
		assertThat(evals.get()).as("serializing equality list evaluates state once").isEqualTo(1);

		ConfigurationCacheHackList restored;
		try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
			restored = (ConfigurationCacheHackList) in.readObject();
		}
		assertThat(restored.getSteps()).hasSize(1);
		assertThat(restored.getSteps().get(0).getName()).isEqualTo("plain");
		// toString after restore must still be side-effect free
		assertThatCode(restored::toString).doesNotThrowAnyException();
	}
}
