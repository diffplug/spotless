/*
 * Copyright 2021-2022 DiffPlug
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
package com.diffplug.spotless.maven.pom;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

public class SortPomMavenTest extends MavenIntegrationHarness {
	@Test
	public void testSortPomWithDefaultConfig() throws Exception {
		writePomWithPomSteps("<sortPom/>");

		setFile("pom_test.xml").toResource("pom/pom_dirty.xml");
		mavenRunner().withArguments("spotless:apply").runNoError().error();
		assertFile("pom_test.xml").sameAsResource("pom/pom_clean_default.xml");
	}
}
