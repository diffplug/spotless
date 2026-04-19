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
package com.diffplug.spotless.maven.java;

import org.apache.maven.plugins.annotations.Parameter;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.java.TableTestFormatterStep;
import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.FormatterStepFactory;

/**
 * Formats {@code @TableTest} annotation tables. Configuration is read from {@code .editorconfig} files,
 * falling back to the configured {@code indentStyle} and {@code indentSize} when no editorconfig is found.
 */
public class TableTestFormatter implements FormatterStepFactory {

	@Parameter
	private String version;

	/**
	 * Fallback indent style when no {@code .editorconfig} is found: {@code space} or {@code tab}.
	 * Defaults to {@code space}.
	 */
	@Parameter
	private String indentStyle;

	/**
	 * Fallback indent size when no {@code .editorconfig} is found. Must be &gt;= 0.
	 * Defaults to {@code 4}.
	 */
	@Parameter
	private Integer indentSize;

	@Override
	public FormatterStep newFormatterStep(FormatterStepConfig config) {
		String resolvedVersion = this.version != null ? this.version : TableTestFormatterStep.defaultVersion();
		String resolvedStyle = this.indentStyle != null
				? TableTestFormatterStep.validateIndentStyle(this.indentStyle)
				: TableTestFormatterStep.DEFAULT_INDENT_STYLE;
		int resolvedSize = this.indentSize != null
				? TableTestFormatterStep.validateIndentSize(this.indentSize)
				: TableTestFormatterStep.DEFAULT_INDENT_SIZE;
		return TableTestFormatterStep.create(resolvedVersion, config.getProvisioner(), resolvedStyle, resolvedSize);
	}
}
