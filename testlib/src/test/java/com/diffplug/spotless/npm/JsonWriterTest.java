/*
 * Copyright 2016-2023 DiffPlug
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.diffplug.common.collect.ImmutableMap;
import com.diffplug.spotless.ResourceHarness;

class JsonWriterTest extends ResourceHarness {

	private JsonWriter jsonWriter = new JsonWriter();

	@Test
	void itWritesAValidEmptyObject() {
		assertThat(jsonWriter.toJsonString().replaceAll("\\s", "")).isEqualTo("{}");
	}

	@Test
	void itWritesABooleanProperty() {
		jsonWriter.put("mybool", true);
		assertThat(jsonWriter.toJsonString()).isEqualTo("{\n    \"mybool\": true\n}");
	}

	@Test
	void itWritesAStringProperty() {
		jsonWriter.put("mystring", "stringvalue");
		assertThat(jsonWriter.toJsonString()).isEqualTo("{\n    \"mystring\": \"stringvalue\"\n}");
	}

	@Test
	void itWritesAnInteger() {
		jsonWriter.put("myint", 7);
		assertThat(jsonWriter.toJsonString()).isEqualTo("{\n    \"myint\": 7\n}");
	}

	@Test
	void itFailsOnUnsupportedObject() {
		assertThatThrownBy(() -> jsonWriter.put("anyobj", new Object())).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void itHandlesSeveralOptionsSimultaneously() {
		jsonWriter.putAll(ImmutableMap.of("mystring", "stringvalue", "intvalue", 1));
		assertThat(jsonWriter.toJsonString()).isEqualTo("{\n    \"mystring\": \"stringvalue\",\n    \"intvalue\": 1\n}");
	}

	@Test
	void itWritesToFile() throws IOException {
		jsonWriter.put("mystring", "stringvalue");
		final File file = newFile("target.json");
		jsonWriter.toJsonFile(file);
		assertFile(file).hasContent("{\n    \"mystring\": \"stringvalue\"\n}");
	}
}
