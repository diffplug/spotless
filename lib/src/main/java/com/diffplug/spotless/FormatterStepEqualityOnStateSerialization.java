/*
 * Copyright 2016-2024 DiffPlug
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

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Standard implementation of FormatterStep which cleanly enforces
 * separation of a lazily computed "state" object whose serialized form
 * is used as the basis for equality and hashCode, which is separate
 * from the serialized form of the step itself, which can include absolute paths
 * and such without interfering with buildcache keys.
 */
@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
abstract class FormatterStepEqualityOnStateSerialization<State extends Serializable> implements FormatterStep, Serializable {
	private static final long serialVersionUID = 1L;

	protected abstract State stateSupplier() throws Exception;

	protected abstract FormatterFunc stateToFormatter(State state) throws Exception;

	private transient FormatterFunc formatter;
	private transient State stateInternal;
	private transient byte[] serializedStateInternal;

	@Override
	public String format(String rawUnix, File file) throws Exception {
		if (formatter == null) {
			formatter = stateToFormatter(state());
		}
		return formatter.apply(rawUnix, file);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		} else if (getClass() != o.getClass()) {
			return false;
		} else {
			return Arrays.equals(serializedState(), ((FormatterStepEqualityOnStateSerialization<?>) o).serializedState());
		}
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(serializedState());
	}

	@Override
	public void close() {
		if (formatter instanceof FormatterFunc.Closeable) {
			((FormatterFunc.Closeable) formatter).close();
			formatter = null;
		}
	}

	private State state() throws Exception {
		if (stateInternal == null) {
			stateInternal = stateSupplier();
		}
		return stateInternal;
	}

	private byte[] serializedState() {
		if (serializedStateInternal == null) {
			serializedStateInternal = ThrowingEx.get(() -> LazyForwardingEquality.toBytes(state()));
		}
		return serializedStateInternal;
	}
}
