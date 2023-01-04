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

import com.diffplug.spotless.maven.MavenIntegrationHarness;

public class JsonTest extends MavenIntegrationHarness {
	@Test
	public void testFormatJson_WithSimple_defaultConfig() throws Exception {
		writePomWithJsonSteps("<json><simple/></json>");

		setFile("json_test.json").toResource("json/sortByKeysBefore.json");
		mavenRunner().withArguments("spotless:apply").runNoError().error();
		assertFile("json_test.json").sameAsResource("json/sortByKeysAfterDisabled.json");
	}

	@Test
	public void testFormatJson_WithGson_defaultConfig() throws Exception {
		writePomWithJsonSteps("<json><gson/></json>");

		setFile("json_test.json").toResource("json/sortByKeysBefore.json");
		mavenRunner().withArguments("spotless:apply").runNoError().error();
		assertFile("json_test.json").sameAsResource("json/sortByKeysAfterDisabled.json");
	}
}
