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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class GsonWrapperBase {

	public static final String INCOMPATIBLE_ERROR_MESSAGE = "There was a problem interacting with Gson; maybe you set an incompatible version?";
	public static final String FAILED_TO_PARSE_ERROR_MESSAGE = "Unable to format JSON";

	protected final Class<?> loadClass(ClassLoader classLoader, String className) {
		try {
			return classLoader.loadClass(className);
		} catch (ClassNotFoundException cause) {
			throw new IllegalStateException(INCOMPATIBLE_ERROR_MESSAGE, cause);
		}
	}

	protected final Constructor<?> getConstructor(Class<?> clazz, Class<?>... argumentTypes) {
		try {
			return clazz.getConstructor(argumentTypes);
		} catch (NoSuchMethodException cause) {
			throw new IllegalStateException(INCOMPATIBLE_ERROR_MESSAGE, cause);
		}
	}

	protected final Method getMethod(Class<?> clazz, String name, Class<?>... argumentTypes) {
		try {
			return clazz.getMethod(name, argumentTypes);
		} catch (NoSuchMethodException cause) {
			throw new IllegalStateException(INCOMPATIBLE_ERROR_MESSAGE, cause);
		}
	}

	protected final <T> T newInstance(Constructor<T> constructor, Object... args) {
		try {
			return constructor.newInstance(args);
		} catch (InstantiationException | IllegalAccessException cause) {
			throw new IllegalStateException(INCOMPATIBLE_ERROR_MESSAGE, cause);
		} catch (InvocationTargetException cause) {
			throw new AssertionError(FAILED_TO_PARSE_ERROR_MESSAGE, cause.getCause());
		}
	}

	protected Object invoke(Method method, Object targetObject, Object... args) {
		try {
			return method.invoke(targetObject, args);
		} catch (IllegalAccessException cause) {
			throw new IllegalStateException(INCOMPATIBLE_ERROR_MESSAGE, cause);
		} catch (InvocationTargetException cause) {
			throw new AssertionError(FAILED_TO_PARSE_ERROR_MESSAGE, cause.getCause());
		}
	}

}
