/*
 * Copyright 2021-2023 DiffPlug
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
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.json.JacksonConfig;

/**
 * A {@link FormatterFunc} based on Jackson library
 */
// https://github.com/FasterXML/jackson-dataformats-text/issues/372
public class JacksonJsonFormatterFunc implements FormatterFunc {
	private JacksonConfig jacksonConfig;

	public JacksonJsonFormatterFunc(JacksonConfig jacksonConfig) {
		this.jacksonConfig = jacksonConfig;
	}

	@Override
	public String apply(String input) throws Exception {
		ObjectMapper objectMapper = makeObjectMapper();

		return format(objectMapper, input);
	}

	/**
	 * @return a {@link JsonFactory}. May be overridden to handle alternative formats.
	 * @see <a href="https://github.com/FasterXML/jackson-dataformats-text">jackson-dataformats-text</a>
	 */
	protected JsonFactory makeJsonFactory() {
		return new JsonFactory();
	}

	protected ObjectMapper makeObjectMapper() {
		JsonFactory jsonFactory = makeJsonFactory();
		ObjectMapper objectMapper = new ObjectMapper(jsonFactory);

		objectMapper.setDefaultPrettyPrinter(makePrinter());

		// Configure the ObjectMapper
		// https://github.com/FasterXML/jackson-databind#commonly-used-features
		jacksonConfig.getFeatureToToggle().forEach((rawFeature, toggle) -> {
			// https://stackoverflow.com/questions/3735927/java-instantiating-an-enum-using-reflection
			SerializationFeature feature = SerializationFeature.valueOf(rawFeature);

			objectMapper.configure(feature, toggle);
		});

		return objectMapper;
	}

	protected DefaultPrettyPrinter makePrinter() {
		boolean spaceBeforeSeparator = jacksonConfig.isSpaceBeforeSeparator();

		DefaultPrettyPrinter.Indenter indenter = new DefaultIndenter(getIndentation(), "\n");
		DefaultPrettyPrinter printer = new SpotlessDefaultPrettyPrinter(spaceBeforeSeparator);

		printer.indentObjectsWith(indenter);
		printer.indentArraysWith(indenter);
		return printer;
	}

	protected String getIndentation() {
		// DefaultIndenter default constructor relies on this
		return "  ";
		//		int indentSpaces = jacksonConfig.getIndentSpaces();
		//		if (indentSpaces < 0) {
		//			return "\t";
		//		} else {
		//			return IntStream.range(0, indentSpaces).mapToObj(i -> "").collect(Collectors.joining());
		//		}
	}

	protected String format(ObjectMapper objectMapper, String input) throws IllegalArgumentException, IOException {
		try {
			ObjectNode objectNode = objectMapper.readValue(input, ObjectNode.class);
			String outputFromjackson = objectMapper.writeValueAsString(objectNode);

			return outputFromjackson;
		} catch (JsonProcessingException e) {
			throw new AssertionError("Unable to format. input='" + input + "'", e);
		}
	}

	protected static class SpotlessDefaultPrettyPrinter extends DefaultPrettyPrinter {
		private static final long serialVersionUID = 1L;
		private final boolean spaceBeforeSeparator;

		public SpotlessDefaultPrettyPrinter(boolean spaceBeforeSeparator) {
			this.spaceBeforeSeparator = spaceBeforeSeparator;
		}

		@Override
		public DefaultPrettyPrinter createInstance() {
			return new DefaultPrettyPrinter(this);
		}

		@Override
		public DefaultPrettyPrinter withSeparators(Separators separators) {
			this._separators = separators;
			if (spaceBeforeSeparator) {
				this._objectFieldValueSeparatorWithSpaces = " " + separators.getObjectFieldValueSeparator() + " ";
			} else {
				this._objectFieldValueSeparatorWithSpaces = separators.getObjectFieldValueSeparator() + " ";
			}
			return this;
		}
	}
}
