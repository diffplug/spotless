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
package com.diffplug.spotless.maven.pom;

import org.apache.maven.plugins.annotations.Parameter;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.FormatterStepFactory;
import com.diffplug.spotless.pom.SortPomStep;

public class SortPom implements FormatterStepFactory {
	@Parameter
	String encoding = "UTF-8";

	@Parameter
	String lineSeparator = System.getProperty("line.separator");

	@Parameter
	boolean expandEmptyElements = true;

	@Parameter
	boolean spaceBeforeCloseEmptyElement = false;

	@Parameter
	boolean keepBlankLines = true;

	@Parameter
	int nrOfIndentSpace = 2;

	@Parameter
	boolean indentBlankLines = false;

	@Parameter
	boolean indentSchemaLocation = false;

	@Parameter
	String predefinedSortOrder = "recommended_2008_06";

	@Parameter
	String sortOrderFile;

	@Parameter
	String sortDependencies;

	@Parameter
	String sortDependencyExclusions;

	@Parameter
	String sortPlugins;

	@Parameter
	boolean sortProperties = false;

	@Parameter
	boolean sortModules = false;

	@Parameter
	boolean sortExecutions = false;

	@Override
	public FormatterStep newFormatterStep(FormatterStepConfig stepConfig) {
		return SortPomStep.create(encoding, lineSeparator, expandEmptyElements, spaceBeforeCloseEmptyElement, keepBlankLines, nrOfIndentSpace, indentBlankLines, indentSchemaLocation, predefinedSortOrder, sortOrderFile, sortDependencies, sortDependencyExclusions, sortPlugins, sortProperties, sortModules, sortExecutions, stepConfig.getProvisioner());
	}
}
