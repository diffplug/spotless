/*
 * Copyright 2026 DiffPlug
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
package com.diffplug.spotless.glue.java;

import java.io.File;

import org.tabletest.formatter.config.Config;
import org.tabletest.formatter.config.EditorConfigProvider;
import org.tabletest.formatter.core.SourceFileFormatter;
import org.tabletest.formatter.core.TableTestFormatter;

import com.diffplug.spotless.FormatterFunc;

/**
 * Formats {@code @TableTest} annotation tables in Java, Kotlin, and standalone {@code .table} files.
 */
public class TableTestFormatterFunc implements FormatterFunc.NeedsFile {

	private static final EditorConfigProvider CONFIG_PROVIDER = new EditorConfigProvider();

	private final SourceFileFormatter sourceFormatter = new SourceFileFormatter();
	private final TableTestFormatter tableFormatter = new TableTestFormatter();

	@Override
	public String applyWithFile(String unix, File file) throws Exception {
		String fileName = file.getName();

		if (fileName.endsWith(".java") || fileName.endsWith(".kt")) {
			Config config = CONFIG_PROVIDER.lookupConfig(file.toPath(), Config.SPACES_4);
			String formatted = sourceFormatter.format(unix, config);
			return formatted.equals(unix) ? unix : formatted;
		}

		if (fileName.endsWith(".table")) {
			String formatted = tableFormatter.format(unix, "", Config.NO_INDENT);
			return formatted.equals(unix) ? unix : formatted;
		}

		return unix;
	}
}
