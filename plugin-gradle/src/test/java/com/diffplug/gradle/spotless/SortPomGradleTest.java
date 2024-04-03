/*
 * Copyright 2024 DiffPlug
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

import org.junit.jupiter.api.Test;

class SortPomGradleTest extends GradleIntegrationHarness {
	@Test
	void sortPom() throws Exception {
		// given
		setFile("build.gradle").toLines(
				"plugins {",
				"  id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"  pom {",
				"    sortPom()",
				"  }",
				"}");
		setFile("pom.xml").toResource("pom/pom_dirty.xml");

		// when
		gradleRunner().withArguments("spotlessApply").build();

		// then
		assertFile("pom.xml").sameAsResource("pom/pom_clean_default.xml");
	}

	@Test
	void sortPomWithTarget() throws Exception {
		// given
		setFile("build.gradle").toLines(
				"plugins {",
				"  id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"  pom {",
				"    target('test.xml')",
				"    sortPom()",
				"  }",
				"}");
		setFile("test.xml").toResource("pom/pom_dirty.xml");

		// when
		gradleRunner().withArguments("spotlessApply").build();

		// then
		assertFile("test.xml").sameAsResource("pom/pom_clean_default.xml");
	}

	@Test
	void sortPomWithVersion() throws Exception {
		// given
		setFile("build.gradle").toLines(
				"plugins {",
				"  id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"  pom {",
				"    sortPom '3.4.0'",
				"  }",
				"}");
		setFile("pom.xml").toResource("pom/pom_dirty.xml");

		// when
		gradleRunner().withArguments("spotlessApply").build();

		// then
		assertFile("pom.xml").sameAsResource("pom/pom_clean_default.xml");
	}

	@Test
	void sortPomWithParameters() throws Exception {
		// given
		setFile("build.gradle").toLines(
				"plugins {",
				"  id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"  pom {",
				"    sortPom()",
				"      .encoding('UTF-8')",
				"      .lineSeparator(System.getProperty('line.separator'))",
				"      .expandEmptyElements(true)",
				"      .spaceBeforeCloseEmptyElement(false)",
				"      .keepBlankLines(true)",
				"      .endWithNewline(true)",
				"      .nrOfIndentSpace(2)",
				"      .indentBlankLines(false)",
				"      .indentSchemaLocation(false)",
				"      .predefinedSortOrder('recommended_2008_06')",
				"      .sortOrderFile(null)",
				"      .sortDependencies(null)",
				"      .sortDependencyManagement(null)",
				"      .sortDependencyExclusions(null)",
				"      .sortPlugins(null)",
				"      .sortProperties(false)",
				"      .sortModules(false)",
				"      .sortExecutions(false)",
				"  }",
				"}");
		setFile("pom.xml").toResource("pom/pom_dirty.xml");

		// when
		gradleRunner().withArguments("spotlessApply").build();

		// then
		assertFile("pom.xml").sameAsResource("pom/pom_clean_default.xml");
	}
}
