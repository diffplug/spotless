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

class GsonBuilderWrapper extends GsonWrapperBase {

	private final Constructor<?> constructor;
	private final Method serializeNullsMethod;
	private final Method disableHtmlEscapingMethod;
	private final Method createMethod;

	GsonBuilderWrapper(JarState jarState) {
		Class<?> clazz = loadClass(jarState.getClassLoader(), "com.google.gson.GsonBuilder");
		this.constructor = getConstructor(clazz);
		this.serializeNullsMethod = getMethod(clazz, "serializeNulls");
		this.disableHtmlEscapingMethod = getMethod(clazz, "disableHtmlEscaping");
		this.createMethod = getMethod(clazz, "create");
	}

	Object createGsonBuilder() {
		return newInstance(constructor);
	}

	Object serializeNulls(Object gsonBuilder) {
		return invoke(serializeNullsMethod, gsonBuilder);
	}

	Object disableHtmlEscaping(Object gsonBuilder) {
		return invoke(disableHtmlEscapingMethod, gsonBuilder);
	}

	Object create(Object gsonBuilder) {
		return invoke(createMethod, gsonBuilder);
	}

}
