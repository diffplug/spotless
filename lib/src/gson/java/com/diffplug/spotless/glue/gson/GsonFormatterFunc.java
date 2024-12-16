/*
 * Copyright 2023-2024 DiffPlug
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
package com.diffplug.spotless.glue.gson;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.ThrowingEx;
import com.diffplug.spotless.json.gson.GsonConfig;

public class GsonFormatterFunc implements FormatterFunc {

	private static final String FAILED_TO_PARSE_ERROR_MESSAGE = "Unable to parse JSON";

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
				throw new IllegalArgumentException(FAILED_TO_PARSE_ERROR_MESSAGE);
			}
			if (gsonConfig.isSortByKeys()) {
				jsonElement = sortByKeys(jsonElement);
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

	private JsonElement sortByKeys(JsonElement jsonElement) {
		if (jsonElement.isJsonArray()) {
			return sortByKeys(jsonElement.getAsJsonArray());
		} else if (jsonElement.isJsonObject()) {
			return sortByKeys(jsonElement.getAsJsonObject());
		} else {
			return jsonElement;
		}
	}

	private JsonElement sortByKeys(JsonObject jsonObject) {
		JsonObject result = new JsonObject();
		jsonObject.keySet().stream().sorted()
				.forEach(key -> {
					JsonElement sorted = sortByKeys(jsonObject.get(key));
					result.add(key, sorted);
				});
		return result;
	}

	private JsonElement sortByKeys(JsonArray jsonArray) {
		var result = new JsonArray();
		for (JsonElement element : jsonArray) {
			JsonElement sorted = sortByKeys(element);
			result.add(sorted);
		}

		return result;
	}

	private String generateIndent(int indentSpaces) {
		return String.join("", Collections.nCopies(indentSpaces, " "));
	}

}
