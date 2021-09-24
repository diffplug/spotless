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
package com.diffplug.spotless.extra.pom;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Serializable;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;

import sortpom.SortPomImpl;
import sortpom.logger.SortPomLogger;
import sortpom.parameter.PluginParameters;

public class SortPomStep {

	public static final String NAME = "sortPom";
	private static final Logger logger = Logger.getLogger(SortPomStep.class.getName());

	private SortPomStep() {}

	public static FormatterStep create(String encoding, String lineSeparator, boolean expandEmptyElements, boolean spaceBeforeCloseEmptyElement, boolean keepBlankLines, int nrOfIndentSpace, boolean indentBlankLines, boolean indentSchemaLocation, String predefinedSortOrder, String sortOrderFile, String sortDependencies, String sortDependencyExclusions, String sortPlugins, boolean sortProperties, boolean sortModules, boolean sortExecutions) {
		return FormatterStep.createLazy(NAME, () -> new State(encoding, lineSeparator, expandEmptyElements, spaceBeforeCloseEmptyElement, keepBlankLines, nrOfIndentSpace, indentBlankLines, indentSchemaLocation, predefinedSortOrder, sortOrderFile, sortDependencies, sortDependencyExclusions, sortPlugins, sortProperties, sortModules, sortExecutions), State::createFormat);
	}

	static final class State implements Serializable {
		private static final long serialVersionUID = 1L;
		final String encoding;

		final String lineSeparator;

		final boolean expandEmptyElements;

		final boolean spaceBeforeCloseEmptyElement;

		final boolean keepBlankLines;

		final int nrOfIndentSpace;

		final boolean indentBlankLines;

		final boolean indentSchemaLocation;

		final String predefinedSortOrder;

		final String sortOrderFile;

		final String sortDependencies;

		final String sortDependencyExclusions;

		final String sortPlugins;

		final boolean sortProperties;

		final boolean sortModules;

		final boolean sortExecutions;

		State(String encoding, String lineSeparator, boolean expandEmptyElements, boolean spaceBeforeCloseEmptyElement, boolean keepBlankLines, int nrOfIndentSpace, boolean indentBlankLines, boolean indentSchemaLocation, String predefinedSortOrder, String sortOrderFile, String sortDependencies, String sortDependencyExclusions, String sortPlugins, boolean sortProperties, boolean sortModules, boolean sortExecutions) {
			this.encoding = encoding;
			this.lineSeparator = lineSeparator;
			this.expandEmptyElements = expandEmptyElements;
			this.spaceBeforeCloseEmptyElement = spaceBeforeCloseEmptyElement;
			this.keepBlankLines = keepBlankLines;
			this.nrOfIndentSpace = nrOfIndentSpace;
			this.indentBlankLines = indentBlankLines;
			this.indentSchemaLocation = indentSchemaLocation;
			this.predefinedSortOrder = predefinedSortOrder;
			this.sortOrderFile = sortOrderFile;
			this.sortDependencies = sortDependencies;
			this.sortDependencyExclusions = sortDependencyExclusions;
			this.sortPlugins = sortPlugins;
			this.sortProperties = sortProperties;
			this.sortModules = sortModules;
			this.sortExecutions = sortExecutions;
		}

		FormatterFunc createFormat() {
			return input -> {
				// SortPom expects a file to sort, so we write the inpout into a temporary file
				File pom = File.createTempFile("pom", ".xml");
				pom.deleteOnExit();
				try (FileWriter fw = new FileWriter(pom)) {
					fw.write(input);
				}
				SortPomImpl sortPom = new SortPomImpl();
				sortPom.setup(new SortPomLogger() {
					@Override
					public void warn(String content) {
						logger.warning(content);
					}

					@Override
					public void info(String content) {
						logger.info(content);
					}

					@Override
					public void error(String content) {
						logger.severe(content);
					}
				}, PluginParameters.builder()
						.setPomFile(pom)
						.setFileOutput(false, null, null, false)
						.setEncoding(encoding)
						.setFormatting(lineSeparator, expandEmptyElements, spaceBeforeCloseEmptyElement, keepBlankLines)
						.setIndent(nrOfIndentSpace, indentBlankLines, indentSchemaLocation)
						.setSortOrder(sortOrderFile, predefinedSortOrder)
						.setSortEntities(sortDependencies, sortDependencyExclusions, sortPlugins, sortProperties, sortModules, sortExecutions)
						.setTriggers(false)
						.build());
				sortPom.sortPom();
				return IOUtils.toString(new FileReader(pom));
			};
		}
	}
}
