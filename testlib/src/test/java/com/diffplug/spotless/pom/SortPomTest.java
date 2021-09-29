/*
 * Copyright 2021 DiffPlug
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
package com.diffplug.spotless.pom;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.TestProvisioner;

public class SortPomTest {
	@Test
	public void testSortPomWithDefaultConfig() throws Exception {
		String encoding = "UTF-8";
		String lineSeparator = System.getProperty("line.separator");
		boolean expandEmptyElements = true;
		boolean spaceBeforeCloseEmptyElement = false;
		boolean keepBlankLines = true;
		int nrOfIndentSpace = 2;
		boolean indentBlankLines = false;
		boolean indentSchemaLocation = false;
		String predefinedSortOrder = "recommended_2008_06";
		String sortOrderFile = null;
		String sortDependencies = null;
		String sortDependencyExclusions = null;
		String sortPlugins = null;
		boolean sortProperties = false;
		boolean sortModules = false;
		boolean sortExecutions = false;
		Provisioner provisioner = TestProvisioner.mavenCentral();
		StepHarness harness = StepHarness.forStep(SortPomStep.create(encoding, lineSeparator, expandEmptyElements, spaceBeforeCloseEmptyElement,
				keepBlankLines, nrOfIndentSpace, indentBlankLines, indentSchemaLocation,
				predefinedSortOrder, sortOrderFile, sortDependencies, sortDependencyExclusions, sortPlugins,
				sortProperties, sortModules, sortExecutions,
				provisioner));
		harness.testResource("pom/pom_dirty.xml", "pom/pom_clean_default.xml");
	}
}
