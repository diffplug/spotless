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

import com.diffplug.spotless.JarState;

public class GsonWrapper extends GsonWrapperBase {

	private final Constructor<?> constructor;
	private final Method fromJsonMethod;
	private final Method toJsonMethod;

	public GsonWrapper(JarState jarState, JsonElementWrapper jsonElementWrapper, JsonWriterWrapper jsonWriterWrapper) {
		Class<?> clazz = loadClass(jarState.getClassLoader(), "com.google.gson.Gson");
		this.constructor = getConstructor(clazz);
		this.fromJsonMethod = getMethod(clazz, "fromJson", String.class, Class.class);
		this.toJsonMethod = getMethod(clazz, "toJson", jsonElementWrapper.getWrappedClass(), jsonWriterWrapper.getWrappedClass());
	}

	public Object createGson() {
		return newInstance(constructor);
	}

	public Object fromJson(Object gson, String json, Class<?> type) {
		return invoke(fromJsonMethod, gson, json, type);
	}

	public void toJson(Object gson, Object jsonElement, Object jsonWriter) {
		invoke(toJsonMethod, gson, jsonElement, jsonWriter);
	}

}
