/*
 * Copyright 2022 DiffPlug
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
package com.diffplug.spotless.json.gson;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Set;

import com.diffplug.spotless.JarState;

public class JsonObjectWrapper extends GsonWrapperBase {

	private final Constructor<?> constructor;
	private final Method keySetMethod;
	private final Method getMethod;
	private final Method addMethod;

	public JsonObjectWrapper(JarState jarState, JsonElementWrapper jsonElementWrapper) {
		Class<?> clazz = loadClass(jarState.getClassLoader(), "com.google.gson.JsonObject");
		this.constructor = getConstructor(clazz);
		this.keySetMethod = getMethod(clazz, "keySet");
		this.getMethod = getMethod(clazz, "get", String.class);
		this.addMethod = getMethod(clazz, "add", String.class, jsonElementWrapper.getWrappedClass());
	}

	public Object createJsonObject() {
		return newInstance(constructor);
	}

	@SuppressWarnings("unchecked")
	public Set<String> keySet(Object jsonObject) {
		return (Set<String>) invoke(keySetMethod, jsonObject);
	}

	public Object get(Object jsonObject, String key) {
		return invoke(getMethod, jsonObject, key);
	}

	public void add(Object jsonObject, String key, Object element) {
		invoke(addMethod, jsonObject, key, element);
	}

}
