package com.diffplug.spotless.json.gson;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Objects;

public class GsonStep {
	private static final String MAVEN_COORDINATES = "com.google.code.gson:gson";

	public static FormatterStep create(int indentSpaces, boolean sortByKeys, boolean escapeHtml, String version, Provisioner provisioner) {
		Objects.requireNonNull(provisioner, "provisioner cannot be null");
		return FormatterStep.createLazy("gson", () -> new State(indentSpaces, sortByKeys, escapeHtml, version, provisioner), State::toFormatter);
	}

	private static final class State implements Serializable {
		private static final long serialVersionUID = -1493479043249379485L;

		private final int indentSpaces;
		private final boolean sortByKeys;
		private final boolean escapeHtml;
		private final JarState jarState;

		private State(int indentSpaces, boolean sortByKeys, boolean escapeHtml, String version, Provisioner provisioner) throws IOException {
			this.indentSpaces = indentSpaces;
			this.sortByKeys = sortByKeys;
			this.escapeHtml = escapeHtml;
			this.jarState = JarState.from(MAVEN_COORDINATES + ":" + version, provisioner);
		}

		FormatterFunc toFormatter() {
			JsonWriterWrapper jsonWriterWrapper = new JsonWriterWrapper(jarState);
			JsonElementWrapper jsonElementWrapper = new JsonElementWrapper(jarState);
			JsonObjectWrapper jsonObjectWrapper = new JsonObjectWrapper(jarState, jsonElementWrapper);
			GsonBuilderWrapper gsonBuilderWrapper = new GsonBuilderWrapper(jarState);
			GsonWrapper gsonWrapper = new GsonWrapper(jarState, jsonElementWrapper, jsonWriterWrapper);

			Object gsonBuilder = gsonBuilderWrapper.serializeNulls(gsonBuilderWrapper.createGsonBuilder());
			if (!escapeHtml) {
				gsonBuilder = gsonBuilderWrapper.disableHtmlEscaping(gsonBuilder);
			}
			Object gson = gsonBuilderWrapper.create(gsonBuilder);

			return inputString -> {
				String result;
				if (inputString.isEmpty()) {
					result = "";
				} else {
					Object jsonElement = gsonWrapper.fromJson(gson, inputString, jsonElementWrapper.getWrappedClass());
					if (jsonElement == null) {
						throw new AssertionError(GsonWrapperBase.FAILED_TO_PARSE_ERROR_MESSAGE);
					}
					if (sortByKeys && jsonElementWrapper.isJsonObject(jsonElement)) {
						jsonElement = sortByKeys(jsonObjectWrapper, jsonElementWrapper, jsonElement);
					}
					try (StringWriter stringWriter = new StringWriter()) {
						Object jsonWriter = jsonWriterWrapper.createJsonWriter(stringWriter);
						jsonWriterWrapper.setIndent(jsonWriter, generateIndent(indentSpaces));
						gsonWrapper.toJson(gson, jsonElement, jsonWriter);
						result = stringWriter + "\n";
					}
				}
				return result;
			};
		}

		private Object sortByKeys(JsonObjectWrapper jsonObjectWrapper, JsonElementWrapper jsonElementWrapper, Object jsonObject) {
			Object result = jsonObjectWrapper.createJsonObject();
			jsonObjectWrapper.keySet(jsonObject).stream().sorted()
				.forEach(key -> {
					Object element = jsonObjectWrapper.get(jsonObject, key);
					if (jsonElementWrapper.isJsonObject(element)) {
						element = sortByKeys(jsonObjectWrapper, jsonElementWrapper, element);
					}
					jsonObjectWrapper.add(result, key, element);
				});
			return result;
		}

		private String generateIndent(int indentSpaces) {
			return String.join("", Collections.nCopies(indentSpaces, " "));
		}
	}

}
