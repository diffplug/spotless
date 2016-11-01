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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Objects;

/**
 * Implements equality, hashcode, and serialization entirely in terms
 * of the given key.  It is appropriate for subclasses of this class
 * to use `@SuppressWarnings("serial")`.
 */
@SuppressWarnings("serial")
public abstract class ForwardingEquality<T extends Serializable> implements Serializable {
	private T key;

	ForwardingEquality(T key) {
		this.key = Objects.requireNonNull(key);
	}

	protected T key() {
		return key;
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeObject(key);
	}

	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		key = (T) Objects.requireNonNull(in.readObject());
	}

	@SuppressWarnings("unused")
	private void readObjectNoData() throws ObjectStreamException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean equals(Object other) {
		if (other == null) {
			return false;
		} else if (getClass().equals(other.getClass())) {
			return key.equals(((ForwardingEquality<?>) other).key);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return key.hashCode();
	}
}
