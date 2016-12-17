/*
 * Copyright 2016 DiffPlug
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

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Objects;

public final class NonSerializableList extends AbstractList<String> {
	private final String[] elements;

	public static NonSerializableList of(String... elements) {
		Objects.requireNonNull(elements);
		return new NonSerializableList(elements);
	}

	private NonSerializableList(String[] elements) {
		this.elements = Arrays.copyOf(elements, elements.length);
	}

	@Override
	public String get(int index) {
		if (index < 0 || index >= elements.length) {
			throw new IndexOutOfBoundsException(String.format("index: %s, size: %s", index, elements.length));
		}
		return elements[index];
	}

	@Override
	public int size() {
		return elements.length;
	}
}
