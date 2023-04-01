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

class JsonExtensionTest extends GradleIntegrationHarness {
	@Test
	void simpleDefaultFormatting() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'java'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    json {",
				"        target 'examples/**/*.json'",
				"        simple()",
				"    }",
				"}");
		setFile("src/main/resources/example.json").toResource("json/nestedObjectBefore.json");
		setFile("examples/main/resources/example.json").toResource("json/nestedObjectBefore.json");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/resources/example.json").sameAsResource("json/nestedObjectBefore.json");
		assertFile("examples/main/resources/example.json").sameAsResource("json/nestedObjectAfter.json");
	}

	@Test
	void simpleFormattingWithCustomNumberOfSpaces() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'java'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    json {",
				"        target 'src/**/*.json'",
				"        simple().indentWithSpaces(6)",
				"    }",
				"}");
		setFile("src/main/resources/example.json").toResource("json/singletonArrayBefore.json");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/resources/example.json").sameAsResource("json/singletonArrayAfter6Spaces.json");
	}

	@Test
	void gsonDefaultFormatting() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'java'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    json {",
				"        target 'examples/**/*.json'",
				"        gson()",
				"    }",
				"}");
		setFile("src/main/resources/example.json").toResource("json/nestedObjectBefore.json");
		setFile("examples/main/resources/example.json").toResource("json/nestedObjectBefore.json");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/resources/example.json").sameAsResource("json/nestedObjectBefore.json");
		assertFile("examples/main/resources/example.json").sameAsResource("json/nestedObjectAfter.json");
	}

	@Test
	void gsonFormattingWithCustomNumberOfSpaces() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'java'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    json {",
				"        target 'src/**/*.json'",
				"        gson().indentWithSpaces(6)",
				"    }",
				"}");
		setFile("src/main/resources/example.json").toResource("json/singletonArrayBefore.json");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/resources/example.json").sameAsResource("json/singletonArrayAfter6Spaces.json");
	}

	@Test
	void gsonFormattingWithSortingByKeys() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'java'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    json {",
				"        target 'src/**/*.json'",
				"        gson().sortByKeys()",
				"    }",
				"}");
		setFile("src/main/resources/example.json").toResource("json/sortByKeysBefore.json");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/resources/example.json").sameAsResource("json/sortByKeysAfter.json");
	}

	@Test
	void gsonFormattingWithHtmlEscape() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'java'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    json {",
				"        target 'src/**/*.json'",
				"        gson().escapeHtml()",
				"    }",
				"}");
		setFile("src/main/resources/example.json").toResource("json/escapeHtmlGsonBefore.json");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/resources/example.json").sameAsResource("json/escapeHtmlGsonAfter.json");
	}

	@Test
	void jacksonFormattingWithSortingByKeys() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'java'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    json {",
				"        target 'src/**/*.json'",
				"        jackson().feature('ORDER_MAP_ENTRIES_BY_KEYS', true)",
				"    }",
				"}");
		setFile("src/main/resources/example.json").toResource("json/sortByKeysBefore.json");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/resources/example.json").sameAsResource("json/sortByKeysAfter_Jackson.json");
	}
}
