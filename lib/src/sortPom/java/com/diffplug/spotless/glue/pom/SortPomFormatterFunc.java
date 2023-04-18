/*
 * Copyright 2021-2023 DiffPlug
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
package com.diffplug.spotless.glue.pom;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.pom.SortPomCfg;

import sortpom.SortPomImpl;
import sortpom.logger.SortPomLogger;
import sortpom.parameter.PluginParameters;

public class SortPomFormatterFunc implements FormatterFunc {
	private static final Logger logger = LoggerFactory.getLogger(SortPomFormatterFunc.class);
	private final SortPomCfg cfg;

	public SortPomFormatterFunc(SortPomCfg cfg) {
		this.cfg = cfg;
	}

	@Override
	public String apply(String input) throws Exception {
		// SortPom expects a file to sort, so we write the input into a temporary file
		File pom = File.createTempFile("pom", ".xml");
		pom.deleteOnExit();
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(pom, Charset.forName(cfg.encoding)))) {
			writer.write(input);
		}
		SortPomImpl sortPom = new SortPomImpl();
		sortPom.setup(new MySortPomLogger(), PluginParameters.builder()
				.setPomFile(pom)
				.setFileOutput(false, null, null, false)
				.setEncoding(cfg.encoding)
				.setFormatting(cfg.lineSeparator, cfg.expandEmptyElements, cfg.spaceBeforeCloseEmptyElement, cfg.keepBlankLines)
				.setIndent(cfg.nrOfIndentSpace, cfg.indentBlankLines, cfg.indentSchemaLocation)
				.setSortOrder(cfg.sortOrderFile, cfg.predefinedSortOrder)
				.setSortEntities(cfg.sortDependencies, cfg.sortDependencyExclusions, cfg.sortDependencyManagement,
						cfg.sortPlugins, cfg.sortProperties, cfg.sortModules, cfg.sortExecutions)
				.setIgnoreLineSeparators(false)
				.build());
		sortPom.sortPom();
		return Files.readString(pom.toPath(), Charset.forName(cfg.encoding));
	}

	private static class MySortPomLogger implements SortPomLogger {
		@Override
		public void warn(String content) {
			logger.warn(content);
		}

		@Override
		public void info(String content) {
			logger.info(content);
		}

		@Override
		public void error(String content) {
			logger.error(content);
		}
	}
}
