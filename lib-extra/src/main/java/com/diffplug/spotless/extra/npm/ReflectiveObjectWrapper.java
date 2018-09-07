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
package com.diffplug.spotless.extra.npm;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.function.Function;

public abstract class ReflectiveObjectWrapper implements AutoCloseable {

	private final Object wrappedObj;
	private final Reflective reflective;

	public ReflectiveObjectWrapper(Reflective reflective, Object wrappedObj) {
		this.reflective = requireNonNull(reflective);
		this.wrappedObj = requireNonNull(wrappedObj);
	}

	public ReflectiveObjectWrapper(Reflective reflective, Function<Reflective, Object> wrappedObjSupplier) {
		this(reflective, wrappedObjSupplier.apply(reflective));
	}

	protected Reflective reflective() {
		return this.reflective;
	}

	protected Object wrappedObj() {
		return this.wrappedObj;
	}

	protected Object invoke(String methodName, Object... parameters) {
		return reflective().invokeMethod(wrappedObj(), methodName, parameters);
	}

	protected Object invoke(String methodName, Reflective.TypedValue... parameters) {
		return reflective().invokeMethod(wrappedObj(), methodName, parameters);
	}

	public void release() {
		invoke("release");
	}

	@Override
	public void close() throws Exception {
		release();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof ReflectiveObjectWrapper))
			return false;
		ReflectiveObjectWrapper that = (ReflectiveObjectWrapper) o;
		return Objects.equals(wrappedObj, that.wrappedObj) && Objects.equals(getClass(), that.getClass());
	}

	@Override
	public int hashCode() {
		return Objects.hash(wrappedObj, getClass());
	}
}
