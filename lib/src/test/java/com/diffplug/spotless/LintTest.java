/*
 * Copyright 2026 DiffPlug
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.Test;

class LintTest {
	@Test
	void stepNameIsNullByDefault() {
		assertThat(Lint.atLine(1, "code", "detail").getStepName()).isNull();
	}

	@Test
	void withStepNameReturnsCopyWithStepName() {
		Lint original = Lint.atLineRange(1, 3, "code", "detail");
		Lint named = original.withStepName("inner");

		assertThat(named).isNotSameAs(original);
		assertThat(named.getStepName()).isEqualTo("inner");
		assertThat(named.getLineStart()).isEqualTo(1);
		assertThat(named.getLineEnd()).isEqualTo(3);
		assertThat(named.getShortCode()).isEqualTo("code");
		assertThat(named.getDetail()).isEqualTo("detail");
		assertThat(original.getStepName()).isNull();
	}

	@Test
	void withStepNameKeepsInnermostStepName() {
		Lint inner = Lint.atLine(1, "code", "detail").withStepName("inner");

		assertThat(inner.withStepName("outer")).isSameAs(inner);
	}

	@Test
	void stepNameIsNotPartOfIdentity() {
		Lint plain = Lint.atLine(1, "code", "detail");
		Lint named = plain.withStepName("inner");

		assertThat(named).isEqualTo(plain);
		assertThat(named.hashCode()).isEqualTo(plain.hashCode());
		assertThat(named.toString()).isEqualTo(plain.toString());
	}

	@Test
	void stepNameSurvivesSerialization() throws Exception {
		Lint plain = roundtrip(Lint.atLine(1, "code", "detail"));
		Lint named = roundtrip(Lint.atLine(1, "code", "detail").withStepName("inner"));

		assertThat(plain.getStepName()).isNull();
		assertThat(named.getStepName()).isEqualTo("inner");
		assertThat(named).isEqualTo(plain);
	}

	private static Lint roundtrip(Lint lint) throws IOException, ClassNotFoundException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
			out.writeObject(lint);
		}
		try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
			return (Lint) in.readObject();
		}
	}
}
