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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;

import com.diffplug.spotless.FormatterFunc;

import sortpom.SortPomImpl;
import sortpom.logger.SortPomLogger;
import sortpom.parameter.PluginParameters;

class SortPomFormatterFunc implements FormatterFunc {
	private static final Logger logger = Logger.getLogger(SortPomStep.class.getName());
	private final SortPomStep.InternalState state;

	public SortPomFormatterFunc(SortPomStep.InternalState state) {
		this.state = state;
	}

	@Override
	public String apply(String input) throws Exception {
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
				.setEncoding(state.encoding)
				.setFormatting(state.lineSeparator, state.expandEmptyElements, state.spaceBeforeCloseEmptyElement, state.keepBlankLines)
				.setIndent(state.nrOfIndentSpace, state.indentBlankLines, state.indentSchemaLocation)
				.setSortOrder(state.sortOrderFile, state.predefinedSortOrder)
				.setSortEntities(state.sortDependencies, state.sortDependencyExclusions, state.sortPlugins, state.sortProperties, state.sortModules, state.sortExecutions)
				.setTriggers(false)
				.build());
		sortPom.sortPom();
		return IOUtils.toString(new FileReader(pom));
	}
}
