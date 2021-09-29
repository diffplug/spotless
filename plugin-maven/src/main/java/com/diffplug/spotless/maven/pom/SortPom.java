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
import com.diffplug.spotless.pom.SortPomCfg;
import com.diffplug.spotless.pom.SortPomStep;

public class SortPom implements FormatterStepFactory {
	private final SortPomCfg defaultValues = new SortPomCfg();

	@Parameter
	String encoding = defaultValues.encoding;

	@Parameter
	String lineSeparator = defaultValues.lineSeparator;

	@Parameter
	boolean expandEmptyElements = defaultValues.expandEmptyElements;

	@Parameter
	boolean spaceBeforeCloseEmptyElement = defaultValues.spaceBeforeCloseEmptyElement;

	@Parameter
	boolean keepBlankLines = defaultValues.keepBlankLines;

	@Parameter
	int nrOfIndentSpace = defaultValues.nrOfIndentSpace;

	@Parameter
	boolean indentBlankLines = defaultValues.indentBlankLines;

	@Parameter
	boolean indentSchemaLocation = defaultValues.indentSchemaLocation;

	@Parameter
	String predefinedSortOrder = defaultValues.predefinedSortOrder;

	@Parameter
	String sortOrderFile = defaultValues.sortOrderFile;

	@Parameter
	String sortDependencies = defaultValues.sortDependencies;

	@Parameter
	String sortDependencyExclusions = defaultValues.sortDependencyExclusions;

	@Parameter
	String sortPlugins = defaultValues.sortPlugins;

	@Parameter
	boolean sortProperties = defaultValues.sortProperties;

	@Parameter
	boolean sortModules = defaultValues.sortModules;

	@Parameter
	boolean sortExecutions = defaultValues.sortExecutions;

	@Override
	public FormatterStep newFormatterStep(FormatterStepConfig stepConfig) {
		SortPomCfg cfg = new SortPomCfg();
		cfg.encoding = encoding;
		cfg.lineSeparator = lineSeparator;
		cfg.expandEmptyElements = expandEmptyElements;
		cfg.spaceBeforeCloseEmptyElement = spaceBeforeCloseEmptyElement;
		cfg.keepBlankLines = keepBlankLines;
		cfg.nrOfIndentSpace = nrOfIndentSpace;
		cfg.indentBlankLines = indentBlankLines;
		cfg.indentSchemaLocation = indentSchemaLocation;
		cfg.predefinedSortOrder = predefinedSortOrder;
		cfg.sortOrderFile = sortOrderFile;
		cfg.sortDependencies = sortDependencies;
		cfg.sortDependencyExclusions = sortDependencyExclusions;
		cfg.sortPlugins = sortPlugins;
		cfg.sortProperties = sortProperties;
		cfg.sortModules = sortModules;
		cfg.sortExecutions = sortExecutions;
		return SortPomStep.create(cfg, stepConfig.getProvisioner());
	}
}
