package com.diffplug.spotless.json.gson;

import com.diffplug.spotless.JarState;

import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class JsonWriterWrapper extends GsonWrapperBase {

	private final Class<?> clazz;
	private final Constructor<?> constructor;
	private final Method setIndentMethod;

	public JsonWriterWrapper(JarState jarState) {
		this.clazz = loadClass(jarState.getClassLoader(), "com.google.gson.stream.JsonWriter");
		this.constructor = getConstructor(clazz, Writer.class);
		this.setIndentMethod = getMethod(clazz, "setIndent", String.class);
	}

	public Object createJsonWriter(Writer writer) {
		return newInstance(constructor, writer);
	}

	public void setIndent(Object jsonWriter, String indent) {
		invoke(setIndentMethod, jsonWriter, indent);
	}

	public Class<?> getWrappedClass() {
		return clazz;
	}

}
