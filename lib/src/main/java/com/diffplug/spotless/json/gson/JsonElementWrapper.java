package com.diffplug.spotless.json.gson;

import com.diffplug.spotless.JarState;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Set;

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
