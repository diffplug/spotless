/*
 * Copyright 2023 DiffPlug
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
package com.diffplug.spotless.maven.json;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

public class JsonTest extends MavenIntegrationHarness {
	private static final Logger LOGGER = LoggerFactory.getLogger(JsonTest.class);

	@Test
	public void testFormatJson_WithSimple_defaultConfig_sortByKeys() throws Exception {
		writePomWithJsonSteps("<simple/>");

		setFile("json_test.json").toResource("json/sortByKeysBefore.json");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile("json_test.json").sameAsResource("json/sortByKeysAfterDisabled_Simple.json");
	}

	@Test
	public void testFormatJson_WithSimple_defaultConfig_nestedObject() throws Exception {
		writePomWithJsonSteps("<simple/>");

		setFile("json_test.json").toResource("json/nestedObjectBefore.json");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile("json_test.json").sameAsResource("json/nestedObjectAfter.json");
	}

	@Test
	public void testFormatJson_WithGson_defaultConfig_sortByKeys() throws Exception {
		writePomWithJsonSteps("<gson/>");

		setFile("json_test.json").toResource("json/sortByKeysBefore.json");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile("json_test.json").sameAsResource("json/sortByKeysAfterDisabled.json");
	}

	@Test
	public void testFormatJson_WithGson_sortByKeys() throws Exception {
		writePomWithJsonSteps("<gson><sortByKeys>true</sortByKeys></gson>");

		setFile("json_test.json").toResource("json/sortByKeysBefore.json");

		String output = mavenRunner().withArguments("spotless:apply").runNoError().output();
		LOGGER.error(output);
		System.err.println(output);
		assertFile("json_test.json").sameAsResource("json/sortByKeysAfter.json");
	}

	@Test
	public void testFormatJson_WithGson_defaultConfig_nestedObject() throws Exception {
		writePomWithJsonSteps("<gson/>");

		setFile("json_test.json").toResource("json/nestedObjectBefore.json");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile("json_test.json").sameAsResource("json/nestedObjectAfter.json");
	}

}
