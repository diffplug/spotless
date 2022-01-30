package com.diffplug.spotless.json.gson;

import com.diffplug.spotless.JarState;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

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
