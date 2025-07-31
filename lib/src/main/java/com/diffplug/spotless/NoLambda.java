/*
 * Copyright 2016-2023 DiffPlug
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
import java.util.Arrays;

/**
 * Marker interface to prevent lambda implementations of
 * single-method interfaces that require serializability.
 * <p>
 * In order for Spotless to support up-to-date checks, all
 * of its parameters must be {@link Serializable} so that
 * entries can be written to file, and they must implement
 * equals and hashCode correctly.
 * <p>
 * This interface and its standard implementation,
 * {@link EqualityBasedOnSerialization}, are a quick way
 * to accomplish these goals.
 */
public interface NoLambda extends Serializable {
	/**
	 * Returns a byte array representation of everything inside this {@code SerializableFileFilter}.
	 * <p>
	 * The main purpose of this method is to ensure one can't instantiate this class with lambda
	 * expressions, which are notoriously difficult to serialize and deserialize properly. (See
	 * {@code SerializableFileFilterImpl.SkipFilesNamed} for an example of how to make a serializable
	 * subclass.)
	 */
	public byte[] toBytes();

	/** An implementation of NoLambda in which equality is based on the serialized representation of itself. */
	public abstract static class EqualityBasedOnSerialization implements NoLambda {
		private static final long serialVersionUID = 1733798699224768949L;

		@Override
		public byte[] toBytes() {
			return LazyForwardingEquality.toBytes(this);
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(toBytes());
		}

		@Override
		public boolean equals(Object otherObj) {
			if (otherObj == null) {
				return false;
			} else if (otherObj.getClass().equals(this.getClass())) {
				EqualityBasedOnSerialization other = (EqualityBasedOnSerialization) otherObj;
				return Arrays.equals(toBytes(), other.toBytes());
			} else {
				return false;
			}
		}
	}
}
