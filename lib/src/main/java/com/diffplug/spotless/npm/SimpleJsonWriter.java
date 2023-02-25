/*
 * Copyright 2016-2020 DiffPlug
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
package com.diffplug.spotless.npm;

import static com.diffplug.spotless.npm.JsonEscaper.jsonEscape;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.diffplug.spotless.ThrowingEx;

public class SimpleJsonWriter {

	private final LinkedHashMap<String, Object> valueMap = new LinkedHashMap<>();

	public static SimpleJsonWriter of(Map<String, ?> values) {
		SimpleJsonWriter writer = new SimpleJsonWriter();
		writer.putAll(values);
		return writer;
	}

	SimpleJsonWriter putAll(Map<String, ?> values) {
		verifyValues(values);
		this.valueMap.putAll(values);
		return this;
	}

	SimpleJsonWriter put(String name, Object value) {
		verifyValues(Collections.singletonMap(name, value));
		this.valueMap.put(name, value);
		return this;
	}

	private void verifyValues(Map<String, ?> values) {
		if (values.values()
				.stream()
				.anyMatch(val -> !(val instanceof String || val instanceof JsonRawValue || val instanceof Number || val instanceof Boolean))) {
			throw new IllegalArgumentException("Only values of type 'String', 'JsonRawValue', 'Number' and 'Boolean' are supported. You provided: " + values.values());
		}
	}

	String toJsonString() {
		final String valueString = valueMap.entrySet()
				.stream()
				.map(entry -> "    " + jsonEscape(entry.getKey()) + ": " + jsonEscape(entry.getValue()))
				.collect(Collectors.joining(",\n"));
		return "{\n" + valueString + "\n}";
	}

	JsonRawValue toJsonRawValue() {
		return JsonRawValue.wrap(toJsonString());
	}

	void toJsonFile(File file) {
		if (!file.getParentFile().exists()) {
			if (!file.getParentFile().mkdirs()) {
				throw new RuntimeException("Cannot write to file");
			}
		}
		try {
			Files.write(file.toPath(), toJsonString().getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw ThrowingEx.asRuntime(e);
		}
	}

	@Override
	public String toString() {
		return this.toJsonString();
	}

}
