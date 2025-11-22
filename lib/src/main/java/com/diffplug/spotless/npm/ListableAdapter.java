/*
 * Copyright 2023-2025 DiffPlug
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
package com.diffplug.spotless.npm;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;

final class ListableAdapter<T> implements Iterable<T> {

	private final List<T> delegate;

	@SuppressWarnings("unchecked")
	private ListableAdapter(Object delegate) {
		requireNonNull(delegate);
		if (!canAdapt(delegate)) {
			throw new IllegalArgumentException("Cannot create ListableAdapter from " + delegate.getClass() + ". Use canAdapt() to check first.");
		}
		if (delegate instanceof List) {
			this.delegate = (List<T>) delegate;
		} else if (delegate.getClass().isArray()) {
			this.delegate = Arrays.asList((T[]) delegate);
		} else {
			throw new IllegalArgumentException("Cannot create IterableAdapter from " + delegate.getClass());
		}
	}

	static <T> Iterable<T> adapt(Object delegate) {
		return new ListableAdapter<>(delegate);
	}

	@Override
	@Nonnull
	public Iterator<T> iterator() {
		return delegate.iterator();
	}

	static boolean canAdapt(Object delegate) {
		requireNonNull(delegate);
		return delegate instanceof List || delegate.getClass().isArray();
	}
}
