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
package com.diffplug.gradle.spotless;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.diffplug.common.base.Errors;

/**
 * Implements equality, hashcode, and serialization entirely in terms
 * of a lazily-computed key.  The key's serialized form is used to implement
 * equals() and hashCode(), so you don't have to.
 */
public abstract class LazyForwardingEquality<T extends Serializable> implements Serializable {
	private static final long serialVersionUID = 1L;
  
	/** Null indicates that the key has not yet been set. */
	@Nullable
	private transient volatile T key;

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
						throw Errors.asRuntime(e);
					}
				}
			}
		}
		return key;
	}

	// override serialize output
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeObject(key());
	}

	// override serialize input
	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		key = (T) Objects.requireNonNull(in.readObject());
	}

	// override serialize input
	@SuppressWarnings("unused")
	private void readObjectNoData() throws ObjectStreamException {
		throw new UnsupportedOperationException();
	}

	@Override
	public final boolean equals(Object other) {
		if (other == null) {
			return false;
		} else if (getClass().equals(other.getClass())) {
			Serializable otherKey = ((LazyForwardingEquality<?>) other).key();
			return Arrays.equals(toBytes(otherKey), toBytes(key()));
		} else {
			return false;
		}
	}

	@Override
	public final int hashCode() {
		return Arrays.hashCode(toBytes(key()));
	}

	private static byte[] toBytes(Serializable obj) {
		ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
		try (ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput)) {
			objectOutput.writeObject(obj);
		} catch (IOException e) {
			throw Errors.asRuntime(e);
		}
		return byteOutput.toByteArray();
	}
}
