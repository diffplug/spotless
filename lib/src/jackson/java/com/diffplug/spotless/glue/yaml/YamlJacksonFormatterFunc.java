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
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import com.diffplug.spotless.FormatterFunc;

public class YamlJacksonFormatterFunc implements FormatterFunc {
	private List<String> enabledFeatures;
	private List<String> disabledFeatures;

	// private static final Logger logger = LoggerFactory.getLogger(YamlJacksonFormatterFunc.class);

	public YamlJacksonFormatterFunc(List<String> enabledFeatures, List<String> disabledFeatures) {
		this.enabledFeatures = enabledFeatures;
		this.disabledFeatures = disabledFeatures;
	}

	@Override
	public String apply(String input) throws Exception {
		ObjectMapper objectMapper = makeObjectMapper();

		return format(objectMapper, input);
	}

	protected ObjectMapper makeObjectMapper() {
		YAMLFactory yamlFactory = new YAMLFactory();
		ObjectMapper objectMapper = new ObjectMapper(yamlFactory);

		// Configure the ObjectMapper
		// https://github.com/FasterXML/jackson-databind#commonly-used-features
		for (String rawFeature : enabledFeatures) {
			// https://stackoverflow.com/questions/3735927/java-instantiating-an-enum-using-reflection
			SerializationFeature feature = SerializationFeature.valueOf(rawFeature);

			objectMapper.enable(feature);
		}

		for (String rawFeature : disabledFeatures) {
			// https://stackoverflow.com/questions/3735927/java-instantiating-an-enum-using-reflection
			SerializationFeature feature = SerializationFeature.valueOf(rawFeature);

			objectMapper.disable(feature);
		}
		return objectMapper;
	}

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

			// 2023-01: This returns JSON instead of YAML
			// This will transit with a JsonNode
			// A JsonNode may keep the comments from the input node
			// JsonNode jsonNode = objectMapper.readTree(input);
			//Not 'toPrettyString' as one could require no INDENT_OUTPUT
			// return jsonNode.toPrettyString();
			ObjectNode objectNode = objectMapper.readValue(input, ObjectNode.class);
			return objectMapper.writeValueAsString(objectNode);
		} catch (JsonProcessingException e) {
			throw new AssertionError("Unable to format YAML. input='" + input + "'", e);
		}
	}

	// Spotbugs
	private static class ObjectNodeTypeReference extends TypeReference<ObjectNode> {}
}
