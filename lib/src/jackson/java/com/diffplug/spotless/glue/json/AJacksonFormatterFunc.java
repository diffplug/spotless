/*
 * Copyright 2021-2025 DiffPlug
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
package com.diffplug.spotless.glue.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.json.JacksonConfig;

/**
 * A {@link FormatterFunc} based on Jackson library
 */
// https://github.com/FasterXML/jackson-dataformats-text/issues/372
public abstract class AJacksonFormatterFunc implements FormatterFunc {
	private final JacksonConfig jacksonConfig;

	protected AJacksonFormatterFunc(JacksonConfig jacksonConfig) {
		this.jacksonConfig = jacksonConfig;
	}

	@Override
	public String apply(String input) throws Exception {
		ObjectMapper objectMapper = makeObjectMapper();

		return format(objectMapper, input);
	}

	protected String format(ObjectMapper objectMapper, String input) throws IllegalArgumentException, IOException {
		try {
			// ObjectNode is not compatible with SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS
			Object objectNode = objectMapper.readValue(input, inferType(input));
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectNode);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Unable to format. input='" + input + "'", e);
		}
	}

	/**
	 *
	 * @param input
	 * @return the {@link Class} into which the String has to be deserialized
	 */
	protected abstract Class<?> inferType(String input);

	/**
	 * @return a {@link JsonFactory}. May be overridden to handle alternative formats.
	 * @see <a href="https://github.com/FasterXML/jackson-dataformats-text">jackson-dataformats-text</a>
	 */
	protected abstract JsonFactory makeJsonFactory();

	protected ObjectMapper makeObjectMapper() {
		JsonFactory jsonFactory = makeJsonFactory();
		ObjectMapper objectMapper = new ObjectMapper(jsonFactory);

		objectMapper.setDefaultPrettyPrinter(makePrettyPrinter());

		// Configure the ObjectMapper
		// https://github.com/FasterXML/jackson-databind#commonly-used-features
		jacksonConfig.getFeatureToToggle().forEach((rawFeature, toggle) -> {
			// https://stackoverflow.com/questions/3735927/java-instantiating-an-enum-using-reflection
			SerializationFeature feature = SerializationFeature.valueOf(rawFeature);

			objectMapper.configure(feature, toggle);
		});

		return objectMapper;
	}

	protected PrettyPrinter makePrettyPrinter() {
		return new DefaultPrettyPrinter();
	}
}
