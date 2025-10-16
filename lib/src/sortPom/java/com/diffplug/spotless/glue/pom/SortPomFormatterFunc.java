/*
 * Copyright 2021-2025 DiffPlug
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;
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
	private static final Logger LOGGER = LoggerFactory.getLogger(SortPomFormatterFunc.class);
	private final SortPomCfg cfg;

	public SortPomFormatterFunc(SortPomCfg cfg) {
		this.cfg = cfg;
	}

	@Override
	public String apply(String input) throws Exception {
		// SortPom expects a file to sort, so we write the input into a temporary file
		File pom = Files.createTempFile("pom", ".xml").toFile();
		pom.deleteOnExit();
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(pom, Charset.forName(cfg.encoding)))) {
			writer.write(input);
		}
		SortPomImpl sortPom = new SortPomImpl();
		PluginParameters.Builder builder = PluginParameters.builder()
				.setPomFile(pom)
				.setFileOutput(false, null, null, false)
				.setEncoding(cfg.encoding);
		try {
			builder = builder
					.setFormatting(cfg.lineSeparator, cfg.expandEmptyElements, cfg.spaceBeforeCloseEmptyElement,
							cfg.keepBlankLines, cfg.endWithNewline);
		} catch (NoSuchMethodError e) {
			try {
				Method method = PluginParameters.Builder.class
						.getMethod("setFormatting", String.class, boolean.class, boolean.class, boolean.class);
				builder = (PluginParameters.Builder) method
						.invoke(builder, cfg.lineSeparator, cfg.expandEmptyElements, cfg.spaceBeforeCloseEmptyElement,
								cfg.keepBlankLines);
			} catch (ReflectiveOperationException | RuntimeException ignore) {
				throw e;
			}
		}
		try {
			builder = builder
					.setIndent(cfg.nrOfIndentSpace, cfg.indentBlankLines, cfg.indentSchemaLocation,
							cfg.indentAttribute);
		} catch (NoSuchMethodError e) {
			try {
				Method method = PluginParameters.Builder.class
						.getMethod("setIndent", int.class, boolean.class, boolean.class);
				builder = (PluginParameters.Builder) method
						.invoke(builder, cfg.nrOfIndentSpace, cfg.indentBlankLines, cfg.indentSchemaLocation);
			} catch (ReflectiveOperationException | RuntimeException ignore) {
				throw e;
			}
		}
		builder = builder
				.setSortOrder(cfg.sortOrderFile, cfg.predefinedSortOrder)
				.setSortEntities(cfg.sortDependencies, cfg.sortDependencyExclusions, cfg.sortDependencyManagement,
						cfg.sortPlugins, cfg.sortProperties, cfg.sortModules, cfg.sortExecutions)
				.setIgnoreLineSeparators(false);
		sortPom.setup(new MySortPomLogger(cfg.quiet), builder.build());
		sortPom.sortPom();
		return Files.readString(pom.toPath(), Charset.forName(cfg.encoding));
	}

	private static class MySortPomLogger implements SortPomLogger {
		private final boolean quiet;

		public MySortPomLogger(boolean quiet) {
			this.quiet = quiet;
		}

		@Override
		public void warn(String content) {
			LOGGER.warn(content);
		}

		@Override
		public void info(String content) {
			if (!quiet) {
				LOGGER.info(content);
			}
		}

		@Override
		public void error(String content) {
			LOGGER.error(content);
		}
	}
}
