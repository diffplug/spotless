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

import java.util.Optional;

class V8ObjectWrapper extends ReflectiveObjectWrapper {

	public static final String WRAPPED_CLASS = "com.eclipsesource.v8.V8Object";

	public V8ObjectWrapper(Reflective reflective, Object v8Object) {
		super(reflective, v8Object);
	}

	public V8ObjectWrapper add(String name, Object value) {
		invoke("add", name, value);
		return this;
	}

	public void executeVoidFunction(String functionName, V8ArrayWrapper params) {
		invoke("executeVoidFunction", functionName, params.wrappedObj());
	}

	public V8ObjectWrapper executeObjectFunction(String functionName, V8ArrayWrapper params) {
		Object returnV8Obj = invoke("executeObjectFunction", functionName, params.wrappedObj());
		return new V8ObjectWrapper(reflective(), returnV8Obj);
	}

	public String executeStringFunction(String functionName, V8ArrayWrapper params) {
		String returnValue = (String) invoke("executeStringFunction", functionName, params.wrappedObj());
		return returnValue;
	}

	public String getString(String name) {
		return (String) invoke("getString", name);
	}

	public Optional<String> getOptionalString(String name) {
		String result = null;
		try {
			result = getString(name);
		} catch (RuntimeException e) {
			// ignore
		}
		return Optional.ofNullable(result);
	}

	public boolean getBoolean(String name) {
		return (boolean) invoke("getBoolean", name);
	}

	public Optional<Boolean> getOptionalBoolean(String name) {
		Boolean result = null;
		try {
			result = getBoolean(name);
		} catch (RuntimeException e) {
			// ignore
		}
		return Optional.ofNullable(result);
	}

	public int getInteger(String name) {
		return (int) invoke("getInteger", name);
	}

	public Optional<Integer> getOptionalInteger(String name) {
		Integer result = null;
		try {
			result = getInteger(name);
		} catch (RuntimeException e) {
			// ignore
		}
		return Optional.ofNullable(result);
	}

}
