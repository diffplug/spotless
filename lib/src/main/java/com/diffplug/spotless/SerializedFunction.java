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

import java.io.Serializable;

@FunctionalInterface
public interface SerializedFunction<T, R> extends Serializable, ThrowingEx.Function<T, R> {
	static <T> SerializedFunction<T, T> identity() {
		return t -> t;
	}

	static <T, R> SerializedFunction<T, R> alwaysReturns(R value) {
		return new AlwaysReturns(value);
	}

	class AlwaysReturns<T, R> implements SerializedFunction<T, R> {
		private static final long serialVersionUID = 1L;
		private final R value;

		AlwaysReturns(R value) {
			this.value = value;
		}

		@Override
		public R apply(T t) {
			return value;
		}
	}
}
