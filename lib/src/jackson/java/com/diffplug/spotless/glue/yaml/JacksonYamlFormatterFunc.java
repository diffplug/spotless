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
package com.diffplug.spotless.glue.yaml;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import com.diffplug.spotless.glue.json.JacksonJsonFormatterFunc;
import com.diffplug.spotless.json.JacksonConfig;

public class JacksonYamlFormatterFunc extends JacksonJsonFormatterFunc {

	public JacksonYamlFormatterFunc(JacksonConfig jacksonConfig) {
		super(jacksonConfig);
	}

	@Override
	protected YAMLFactory makeJsonFactory() {
		return new YAMLFactory();
	}

	@Override
	protected String format(ObjectMapper objectMapper, String input) throws IllegalArgumentException, IOException {
		// We may consider adding manually an initial '---' prefix to help management of multiple documents
		// if (!input.trim().startsWith("---")) {
		// 	input = "---" + "\n" + input;
		// }

		try {
			// https://stackoverflow.com/questions/25222327/deserialize-pojos-from-multiple-yaml-documents-in-a-single-file-in-jackson
			// https://github.com/FasterXML/jackson-dataformats-text/issues/66#issuecomment-375328648
			// 2023-01: For now, we get 'Cannot deserialize value of type `com.fasterxml.jackson.databind.node.ObjectNode` from Array value'
			//			JsonParser yamlParser = objectMapper.getFactory().createParser(input);
			//			List<ObjectNode> docs = objectMapper.readValues(yamlParser, ObjectNode.class).readAll();
			//			return objectMapper.writeValueAsString(docs);

			// A JsonNode may keep the comments from the input node
			// JsonNode jsonNode = objectMapper.readTree(input);
			//Not 'toPrettyString' as one could require no INDENT_OUTPUT
			// return jsonNode.toPrettyString();
			ObjectNode objectNode = objectMapper.readValue(input, ObjectNode.class);
			String outputFromjackson = objectMapper.writeValueAsString(objectNode);

			return outputFromjackson;
		} catch (JsonProcessingException e) {
			throw new AssertionError("Unable to format. input='" + input + "'", e);
		}
	}
}
