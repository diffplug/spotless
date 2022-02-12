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
