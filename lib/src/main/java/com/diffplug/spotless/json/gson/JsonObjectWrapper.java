package com.diffplug.spotless.json.gson;

import com.diffplug.spotless.JarState;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Set;

public class JsonObjectWrapper extends GsonWrapperBase {

	private final Class<?> clazz;
	private final Constructor<?> constructor;
	private final Method keySetMethod;
	private final Method getMethod;
	private final Method addMethod;

	public JsonObjectWrapper(JarState jarState, JsonElementWrapper jsonElementWrapper) {
		this.clazz = loadClass(jarState.getClassLoader(), "com.google.gson.JsonObject");
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

	public Class<?> getWrappedClass() {
		return clazz;
	}

}
