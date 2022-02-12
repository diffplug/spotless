package com.diffplug.spotless.json.gson;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import com.diffplug.spotless.JarState;

public class GsonBuilderWrapper extends GsonWrapperBase {

	private final Constructor<?> constructor;
	private final Method serializeNullsMethod;
	private final Method disableHtmlEscapingMethod;
	private final Method createMethod;

	public GsonBuilderWrapper(JarState jarState) {
		Class<?> clazz = loadClass(jarState.getClassLoader(), "com.google.gson.GsonBuilder");
		this.constructor = getConstructor(clazz);
		this.serializeNullsMethod = getMethod(clazz, "serializeNulls");
		this.disableHtmlEscapingMethod = getMethod(clazz, "disableHtmlEscaping");
		this.createMethod = getMethod(clazz, "create");
	}

	public Object createGsonBuilder() {
		return newInstance(constructor);
	}

	public Object serializeNulls(Object gsonBuilder) {
		return invoke(serializeNullsMethod, gsonBuilder);
	}

	public Object disableHtmlEscaping(Object gsonBuilder) {
		return invoke(disableHtmlEscapingMethod, gsonBuilder);
	}

	public Object create(Object gsonBuilder) {
		return invoke(createMethod, gsonBuilder);
	}

}
