package com.diffplug.spotless.glue.gson;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.ThrowingEx;
import com.diffplug.spotless.json.gson.GsonConfig;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

public class GsonFormatterFunc implements FormatterFunc {

	private static final String FAILED_TO_PARSE_ERROR_MESSAGE = "Unable to format JSON";

	private final Gson gson;
	private final GsonConfig gsonConfig;
	private final String generatedIndent;

	public GsonFormatterFunc(GsonConfig gsonConfig) {
		GsonBuilder gsonBuilder = new GsonBuilder().serializeNulls();
		if (!gsonConfig.isEscapeHtml()) {
			gsonBuilder = gsonBuilder.disableHtmlEscaping();
		}
		this.gson = gsonBuilder.create();
		this.gsonConfig = gsonConfig;
		this.generatedIndent = generateIndent(gsonConfig.getIndentSpaces());
	}

	@Override
	public String apply(String inputString) {
		String result;
		if (inputString.isEmpty()) {
			result = "";
		} else {
			JsonElement jsonElement = gson.fromJson(inputString, JsonElement.class);
			if (jsonElement == null) {
				throw new AssertionError(FAILED_TO_PARSE_ERROR_MESSAGE);
			}
			if (gsonConfig.isSortByKeys() && jsonElement.isJsonObject()) {
				jsonElement = sortByKeys(jsonElement.getAsJsonObject());
			}
			try (StringWriter stringWriter = new StringWriter()) {
				JsonWriter jsonWriter = new JsonWriter(stringWriter);
				jsonWriter.setIndent(this.generatedIndent);
				gson.toJson(jsonElement, jsonWriter);
				result = stringWriter + "\n";
			} catch (IOException ioException) {
				throw ThrowingEx.asRuntime(ioException);
			}
		}
		return result;
	}

	private JsonElement sortByKeys(JsonObject jsonObject) {
		JsonObject result = new JsonObject();
		result.keySet().stream().sorted()
			.forEach(key -> {
				JsonElement element = jsonObject.get(key);
				if (element.isJsonObject()) {
					element = sortByKeys(element.getAsJsonObject());
				}
				result.add(key, element);
			});
		return result;
	}

	private String generateIndent(int indentSpaces) {
		return String.join("", Collections.nCopies(indentSpaces, " "));
	}

}
