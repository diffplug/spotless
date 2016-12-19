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

import java.io.Serializable;

import javax.annotation.Nonnull;

/**
 * Implements equality, hashcode, and serialization entirely in terms
 * of a lazily-computed key.  The key's serialized form is used to implement
 * equals() and hashCode(), so you don't have to.
 */
public abstract class LazyForwardingEquality<T extends Serializable> extends ForwardingEquality<T> {
	protected LazyForwardingEquality() {}

	private static final long serialVersionUID = 1L;

	/**
	 * This function is guaranteed to be called at most once.
	 * If the key is never required, then it will never be called at all.
	 *
	 * Throws exception because it's likely that there will be some IO going on.
	 */
	@Nonnull
	protected abstract T calculateKey() throws Exception;

	/** Returns the underlying key, possibly triggering a call to {{@link #calculateKey()}. */
	@Nonnull
	protected final T key() {
		// double-checked locking for lazy evaluation of calculateKey
		if (key == null) {
			synchronized (this) {
				if (key == null) {
					try {
						key = calculateKey();
					} catch (Exception e) {
						throw ThrowingEx.asRuntime(e);
					}
				}
			}
		}
		return key;
	}
}
