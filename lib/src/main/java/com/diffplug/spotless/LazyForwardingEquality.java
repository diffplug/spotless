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
package com.diffplug.spotless;

import static java.util.Objects.requireNonNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Implements equality, hashcode, and serialization entirely in terms
 * of lazily-computed state.  The state's serialized form is used to implement
 * equals() and hashCode(), so you don't have to.
 */
public abstract class LazyForwardingEquality<T extends Serializable> implements Serializable, NoLambda {
	@Serial
	private static final long serialVersionUID = 1L;

	/** Lazily initialized - null indicates that the state has not yet been set. */
	@Nullable private transient volatile T state;

	/**
	 * This function is guaranteed to be called at most once.
	 * If the state is never required, then it will never be called at all.
	 * <p>
	 * Throws exception because it's likely that there will be some IO going on.
	 */
	protected abstract T calculateState() throws Exception;

	/** Returns the underlying state, possibly triggering a call to {{@link #calculateState()}. */
	protected final T state() {
		// double-checked locking for lazy evaluation of calculateState
		if (state == null) {
			synchronized (this) {
				if (state == null) {
					try {
						state = calculateState();
					} catch (Exception e) {
						throw ThrowingEx.asRuntime(e);
					}
				}
			}
		}
		return state; // will always be nonnull at this point
	}

	// override serialize output
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeObject(state());
	}

	// override serialize input
	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		state = (T) requireNonNull(in.readObject());
	}

	// override serialize input
	@SuppressWarnings("unused")
	private void readObjectNoData() throws ObjectStreamException {
		throw new UnsupportedOperationException();
	}

	@Override
	public byte[] toBytes() {
		return toBytes(state());
	}

	@Override
	public final boolean equals(Object other) {
		if (other == null) {
			return false;
		} else if (getClass().equals(other.getClass())) {
			LazyForwardingEquality<?> otherCast = (LazyForwardingEquality<?>) other;
			return Arrays.equals(otherCast.toBytes(), toBytes());
		} else {
			return false;
		}
	}

	@Override
	public final int hashCode() {
		return Arrays.hashCode(toBytes());
	}

	static byte[] toBytes(Serializable obj) {
		ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
		try (ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput)) {
			objectOutput.writeObject(obj);
		} catch (IOException e) {
			throw ThrowingEx.asRuntime(e);
		}
		return byteOutput.toByteArray();
	}

	static Object fromBytes(byte[] bytes) {
		ByteArrayInputStream byteOutput = new ByteArrayInputStream(bytes);
		try (ObjectInputStream objectOutput = new ObjectInputStream(byteOutput)) {
			return objectOutput.readObject();
		} catch (IOException | ClassNotFoundException e) {
			throw ThrowingEx.asRuntime(e);
		}
	}

	/** Ensures that the lazy state has been evaluated. */
	public static void unlazy(Object in) {
		if (in instanceof LazyForwardingEquality<?> equality) {
			equality.state();
		} else if (in instanceof DelegateFormatterStep step) {
			unlazy(step.delegateStep);
		} else if (in instanceof Iterable) {
			Iterable<Object> cast = (Iterable<Object>) in;
			for (Object c : cast) {
				unlazy(c);
			}
		}
	}
}
