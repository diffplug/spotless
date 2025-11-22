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
package com.diffplug.spotless.generic;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;

public final class ReplaceStep {
	// prevent direct instantiation
	private ReplaceStep() {}

	public static FormatterStep create(String name, CharSequence target, CharSequence replacement) {
		requireNonNull(name, "name");
		requireNonNull(target, "target");
		requireNonNull(replacement, "replacement");
		return FormatterStep.createLazy(name,
				() -> new State(target, replacement),
				State::toFormatter);
	}

	private static final class State implements Serializable {
		private static final long serialVersionUID = 1L;

		private final String target;
		private final String replacement;

		State(CharSequence target, CharSequence replacement) {
			this.target = target.toString();
			this.replacement = replacement.toString();
		}

		FormatterFunc toFormatter() {
			return raw -> raw.replace(target, replacement);
		}
	}
}
