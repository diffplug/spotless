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
import java.util.Locale;

import org.tabletest.formatter.config.Config;
import org.tabletest.formatter.config.EditorConfigProvider;
import org.tabletest.formatter.config.IndentStyle;
import org.tabletest.formatter.core.SourceFileFormatter;
import org.tabletest.formatter.core.TableTestFormatter;

import com.diffplug.spotless.FormatterFunc;

/**
 * Formats {@code @TableTest} annotation tables in Java, Kotlin, and standalone {@code .table} files.
 * <p>
 * For Java/Kotlin files, the indent config priority is:
 * <ol>
 *   <li>Settings from {@code .editorconfig}</li>
 *   <li>Configured fallback ({@code indentStyle}/{@code indentSize} constructor parameters)</li>
 *   <li>Built-in default: {@code Config.SPACES_4}</li>
 * </ol>
 * For {@code .table} files, {@code Config.NO_INDENT} is always used.
 */
public class TableTestFormatterFunc implements FormatterFunc.NeedsFile {

	private final EditorConfigProvider CONFIG_PROVIDER = new EditorConfigProvider();
	private final Config sourceFallbackConfig;

	private final SourceFileFormatter sourceFormatter = new SourceFileFormatter();
	private final TableTestFormatter tableFormatter = new TableTestFormatter();

	/** Creates a formatter using the built-in default fallback ({@code Config.SPACES_4}). */
	public TableTestFormatterFunc() {
		this.sourceFallbackConfig = Config.SPACES_4;
	}

	/**
	 * Creates a formatter with a configured fallback indent style and size for Java/Kotlin files.
	 * Used when {@code .editorconfig} is absent or the lookup fails.
	 *
	 * @param indentStyle {@code "space"} or {@code "tab"} (case-insensitive)
	 * @param indentSize  indent size (&gt;= 0)
	 */
	public TableTestFormatterFunc(String indentStyle, int indentSize) {
		this.sourceFallbackConfig = new Config(IndentStyle.valueOf(indentStyle.toUpperCase(Locale.ROOT)), indentSize);
	}

	@Override
	public String applyWithFile(String unix, File file) throws Exception {
		String fileName = file.getName();

		if (fileName.endsWith(".java") || fileName.endsWith(".kt")) {
			Config config = CONFIG_PROVIDER.lookupConfig(file.toPath(), sourceFallbackConfig);
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
