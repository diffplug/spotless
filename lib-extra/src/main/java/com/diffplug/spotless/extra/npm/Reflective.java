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

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;

class Reflective {
	private final ClassLoader classLoader;

	private Reflective(ClassLoader classLoader) {
		this.classLoader = requireNonNull(classLoader);
	}

	static Reflective withClassLoader(ClassLoader classLoader) {
		return new Reflective(classLoader);
	}

	Class<?> clazz(String className) {
		try {
			return this.classLoader.loadClass(className);
		} catch (ClassNotFoundException e) {
			throw new ReflectiveException(e);
		}
	}

	private Method staticMethod(String className, String methodName, Object... parameters) {
		try {
			final Class<?> clazz = clazz(className);
			return clazz.getDeclaredMethod(methodName, types(parameters));
		} catch (NoSuchMethodException e) {
			throw new ReflectiveException(e);
		}
	}

	Object invokeStaticMethod(String className, String methodName, Object... parameters) {
		try {
			Method m = staticMethod(className, methodName, parameters);
			return m.invoke(m.getDeclaringClass(), parameters);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new ReflectiveException(e);
		}
	}

	private Class<?>[] types(TypedValue[] typedValues) {
		return Arrays.stream(typedValues)
				.map(TypedValue::getClazz)
				.toArray(Class[]::new);
	}

	Class<?>[] types(Object[] arguments) {
		return Arrays.stream(arguments)
				.map(Object::getClass)
				.toArray(Class[]::new);
	}

	Object invokeMethod(Object target, String methodName, Object... parameters) {
		Method m = method(target, clazz(target), methodName, parameters);
		try {
			return m.invoke(target, parameters);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new ReflectiveException(e);
		}
	}

	Object invokeMethod(Object target, String methodName, TypedValue... parameters) {
		Method m = method(target, clazz(target), methodName, parameters);
		try {
			return m.invoke(target, objects(parameters));
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new ReflectiveException(e);
		}
	}

	private Method method(Object target, Class<?> clazz, String methodName, Object[] parameters) {
		try {
			final Method method = findMatchingMethod(clazz, methodName, parameters);
			return method;
		} catch (NoSuchMethodException e) {
			if (clazz.getSuperclass() != null) {
				return method(target, clazz.getSuperclass(), methodName, parameters);
			} else {
				// try with primitives

				throw new ReflectiveException("Could not find method " + methodName + " with parameters " + Arrays.toString(parameters) + " on object " + target, e);
			}
		}
	}

	private Method method(Object target, Class<?> clazz, String methodName, TypedValue[] parameters) {
		try {
			final Method method = findMatchingMethod(clazz, methodName, parameters);
			return method;
		} catch (NoSuchMethodException e) {
			if (clazz.getSuperclass() != null) {
				return method(target, clazz.getSuperclass(), methodName, parameters);
			} else {
				// try with primitives

				throw new ReflectiveException("Could not find method " + methodName + " with parameters " + Arrays.toString(parameters) + " on object " + target, e);
			}
		}
	}

	private Method findMatchingMethod(Class<?> clazz, String methodName, Object[] parameters) throws NoSuchMethodException {
		final Class<?>[] origTypes = types(parameters);
		try {
			return clazz.getDeclaredMethod(methodName, origTypes);
		} catch (NoSuchMethodException e) {
			// try with primitives
			final Class<?>[] primitives = autoUnbox(origTypes);
			try {
				return clazz.getDeclaredMethod(methodName, primitives);
			} catch (NoSuchMethodException e1) {
				// didn't work either
				throw e;
			}
		}
	}

	private Method findMatchingMethod(Class<?> clazz, String methodName, TypedValue[] parameters) throws NoSuchMethodException {
		return clazz.getDeclaredMethod(methodName, types(parameters));
	}

	private Class<?>[] autoUnbox(Class<?>[] possiblyBoxed) {
		return Arrays.stream(possiblyBoxed)
				.map(clazz -> {
					try {
						return (Class<?>) this.staticField(clazz, "TYPE");
					} catch (ReflectiveException e) {
						// no primitive type, just keeping current clazz
						return clazz;
					}
				}).toArray(Class[]::new);
	}

	private Class<?> clazz(Object target) {
		return target.getClass();
	}

	Object invokeConstructor(String className, TypedValue... parameters) {
		try {
			final Constructor<?> constructor = constructor(className, parameters);
			return constructor.newInstance(objects(parameters));
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new ReflectiveException(e);
		}
	}

	private Object[] objects(TypedValue[] parameters) {
		return Arrays.stream(parameters)
				.map(TypedValue::getObj)
				.toArray();
	}

	Object invokeConstructor(String className, Object... parameters) {
		try {
			final Constructor<?> constructor = constructor(className, parameters);
			return constructor.newInstance(parameters);
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new ReflectiveException(e);
		}
	}

	private Constructor<?> constructor(String className, TypedValue[] parameters) {
		try {
			final Class<?> clazz = clazz(className);
			final Constructor<?> constructor = clazz.getDeclaredConstructor(types(parameters));
			return constructor;
		} catch (NoSuchMethodException e) {
			throw new ReflectiveException(e);
		}
	}

	private Constructor<?> constructor(String className, Object[] parameters) {
		try {
			final Class<?> clazz = clazz(className);
			final Constructor<?> constructor = clazz.getDeclaredConstructor(types(parameters));
			return constructor;
		} catch (NoSuchMethodException e) {
			throw new ReflectiveException(e);
		}
	}

	Object createDynamicProxy(InvocationHandler invocationHandler, String... interfaceNames) {
		Class<?>[] clazzes = Arrays.stream(interfaceNames)
				.map(this::clazz)
				.toArray(Class[]::new);
		return Proxy.newProxyInstance(this.classLoader, clazzes, invocationHandler);
	}

	Object staticField(String className, String fieldName) {
		final Class<?> clazz = clazz(className);
		return staticField(clazz, fieldName);
	}

	private Object staticField(Class<?> clazz, String fieldName) {
		try {
			return clazz.getDeclaredField(fieldName).get(clazz);
		} catch (IllegalAccessException | NoSuchFieldException e) {
			throw new ReflectiveException(e);
		}
	}

	TypedValue typed(String className, Object obj) {
		return new TypedValue(clazz(className), obj);
	}

	public static class TypedValue {
		private final Class<?> clazz;
		private final Object obj;

		public TypedValue(Class<?> clazz, Object obj) {
			this.clazz = requireNonNull(clazz);
			this.obj = requireNonNull(obj);
		}

		public Class<?> getClazz() {
			return clazz;
		}

		public Object getObj() {
			return obj;
		}

		@Override
		public String toString() {
			return new StringJoiner(", ", TypedValue.class.getSimpleName() + "[", "]")
					.add("clazz=" + clazz)
					.add("obj=" + obj)
					.toString();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			TypedValue that = (TypedValue) o;
			return Objects.equals(clazz, that.clazz) &&
					Objects.equals(obj, that.obj);
		}

		@Override
		public int hashCode() {
			return Objects.hash(clazz, obj);
		}
	}

	public static class ReflectiveException extends RuntimeException {
		private static final long serialVersionUID = -5764607170953013791L;

		public ReflectiveException(String message, Throwable cause) {
			super(message, cause);
		}

		public ReflectiveException(Throwable cause) {
			super(cause);
		}
	}
}
