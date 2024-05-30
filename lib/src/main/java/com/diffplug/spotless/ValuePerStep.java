/*
 * Copyright 2024 DiffPlug
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

import javax.annotation.Nullable;

/**
 * Fixed-size list which maintains a list of exceptions, one per step of the formatter.
 * Usually this list will be empty or have only a single value, so it is optimized for stack allocation in those cases.
 */
class ValuePerStep<T> extends AbstractList<T> {
	private final int size;
	private @Nullable T value;
	private int valueIdx;
	private @Nullable Object[] multipleValues = null;

	ValuePerStep(Formatter formatter) {
		this.size = formatter.getSteps().size();
	}

	@Override
	public @Nullable T set(int index, T exception) {
		if (index < 0 || index >= size) {
			throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
		}
		if (this.value == null) {
			this.valueIdx = index;
			this.value = exception;
			return null;
		} else if (this.multipleValues != null) {
			T previousValue = (T) multipleValues[index];
			multipleValues[index] = exception;
			return previousValue;
		} else {
			if (index == valueIdx) {
				T previousValue = this.value;
				this.value = exception;
				return previousValue;
			} else {
				multipleValues = new Object[size];
				multipleValues[valueIdx] = this.value;
				multipleValues[index] = exception;
				return null;
			}
		}
	}

	@Override
	public T get(int index) {
		if (multipleValues != null) {
			return (T) multipleValues[index];
		} else if (valueIdx == index) {
			return value;
		} else {
			return null;
		}
	}

	public int indexOfFirstValue() {
		if (multipleValues != null) {
			for (int i = 0; i < multipleValues.length; i++) {
				if (multipleValues[i] != null) {
					return i;
				}
			}
			return -1;
		} else if (value != null) {
			return valueIdx;
		} else {
			return -1;
		}
	}

	@Override
	public int size() {
		return size;
	}
}
