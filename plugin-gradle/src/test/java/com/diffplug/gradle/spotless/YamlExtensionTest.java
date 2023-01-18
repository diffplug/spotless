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
package com.diffplug.gradle.spotless;

import java.io.IOException;

import org.junit.jupiter.api.Test;

class YamlExtensionTest extends GradleIntegrationHarness {
	@Test
	void testFormatYaml_WithJackson_defaultConfig_separatorComments() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'java'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    yaml {",
				"        target 'src/**/*.yaml'",
				"        jackson()",
				"    }",
				"}");
		setFile("src/main/resources/example.yaml").toResource("yaml/separator_comments.yaml");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/resources/example.yaml").sameAsResource("yaml/separator_comments.clean.yaml");
	}

	// see YAMLGenerator.Feature.WRITE_DOC_START_MARKER
	@Test
	void testFormatYaml_WithJackson_skipDocStartMarker() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'java'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    yaml {",
				"        target 'src/**/*.yaml'",
				"        jackson()",
				"	        .yamlFeature('WRITE_DOC_START_MARKER', false)",
				"	        .yamlFeature('MINIMIZE_QUOTES', true)",
				"    }",
				"}");
		setFile("src/main/resources/example.yaml").toResource("yaml/array_with_bracket.yaml");
		gradleRunner().withArguments("spotlessApply", "--stacktrace").build();
		assertFile("src/main/resources/example.yaml").sameAsResource("yaml/array_with_bracket.clean.no_start_marker.no_quotes.yaml");
	}

	@Test
	void testFormatYaml_WithJackson_multipleDocuments() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'java'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    yaml {",
				"        target 'src/**/*.yaml'",
				"        jackson()",
				"    }",
				"}");
		setFile("src/main/resources/example.yaml").toResource("yaml/multiple_documents.yaml");
		gradleRunner().withArguments("spotlessApply", "--stacktrace").build();
		assertFile("src/main/resources/example.yaml").sameAsResource("yaml/multiple_documents.clean.jackson.yaml");
	}

	@Test
	void testFormatYaml_WithJackson_arrayAtRoot() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'java'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    yaml {",
				"        target 'src/**/*.yaml'",
				"        jackson()",
				"    }",
				"}");
		setFile("src/main/resources/example.yaml").toResource("yaml/array_at_root.yaml");
		gradleRunner().withArguments("spotlessApply", "--stacktrace").build();
		assertFile("src/main/resources/example.yaml").sameAsResource("yaml/array_at_root.clean.yaml");
	}

}
