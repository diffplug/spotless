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

import java.lang.reflect.Method;

import com.diffplug.spotless.JarState;

public class JsonElementWrapper extends GsonWrapperBase {

	private final Class<?> clazz;
	private final Method isJsonObjectMethod;

	public JsonElementWrapper(JarState jarState) {
		this.clazz = loadClass(jarState.getClassLoader(), "com.google.gson.JsonElement");
		this.isJsonObjectMethod = getMethod(clazz, "isJsonObject");
	}

	public boolean isJsonObject(Object jsonElement) {
		return (boolean) invoke(isJsonObjectMethod, jsonElement);
	}

	public Class<?> getWrappedClass() {
		return clazz;
	}

}
