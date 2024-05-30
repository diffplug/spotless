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
class ExceptionPerStep extends AbstractList<Throwable> {
	private final int size;
	private @Nullable Throwable exception;
	private int exceptionIdx;
	private @Nullable Throwable[] multipleExceptions = null;

	ExceptionPerStep(Formatter formatter) {
		this.size = formatter.getSteps().size();
	}

	@Override
	public @Nullable Throwable set(int index, Throwable exception) {
		if (index < 0 || index >= size) {
			throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
		}
		if (this.exception == null) {
			this.exceptionIdx = index;
			this.exception = exception;
			return null;
		} else if (this.multipleExceptions != null) {
			Throwable previousValue = multipleExceptions[index];
			multipleExceptions[index] = exception;
			return previousValue;
		} else {
			if (index == exceptionIdx) {
				Throwable previousValue = this.exception;
				this.exception = exception;
				return previousValue;
			} else {
				multipleExceptions = new Throwable[size];
				multipleExceptions[exceptionIdx] = this.exception;
				multipleExceptions[index] = exception;
				return null;
			}
		}
	}

	@Override
	public Throwable get(int index) {
		if (multipleExceptions != null) {
			return multipleExceptions[index];
		} else if (exceptionIdx == index) {
			return exception;
		} else {
			return null;
		}
	}

	private int indexOfFirstException() {
		if (multipleExceptions != null) {
			for (int i = 0; i < multipleExceptions.length; i++) {
				if (multipleExceptions[i] != null) {
					return i;
				}
			}
			return -1;
		} else if (exception != null) {
			return exceptionIdx;
		} else {
			return -1;
		}
	}

	@Override
	public int size() {
		return size;
	}

	/** Rethrows the first exception in the list. */
	public void rethrowFirstIfPresent() {
		int firstException = indexOfFirstException();
		if (firstException != -1) {
			throw ThrowingEx.asRuntimeRethrowError(get(firstException));
		}
	}
}
