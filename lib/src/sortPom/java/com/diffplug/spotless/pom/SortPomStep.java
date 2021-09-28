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

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;

public class SortPomStep {

	public static final String NAME = "sortPom";

	private SortPomStep() {}

	public static FormatterStep create(String encoding, String lineSeparator, boolean expandEmptyElements, boolean spaceBeforeCloseEmptyElement, boolean keepBlankLines, int nrOfIndentSpace, boolean indentBlankLines, boolean indentSchemaLocation, String predefinedSortOrder, String sortOrderFile, String sortDependencies, String sortDependencyExclusions, String sortPlugins, boolean sortProperties, boolean sortModules, boolean sortExecutions, Provisioner provisioner) {
		return FormatterStep.createLazy(NAME, () -> new SortPomState(encoding, lineSeparator, expandEmptyElements, spaceBeforeCloseEmptyElement, keepBlankLines, nrOfIndentSpace, indentBlankLines, indentSchemaLocation, predefinedSortOrder, sortOrderFile, sortDependencies, sortDependencyExclusions, sortPlugins, sortProperties, sortModules, sortExecutions, provisioner), SortPomState::createFormat);
	}
}
