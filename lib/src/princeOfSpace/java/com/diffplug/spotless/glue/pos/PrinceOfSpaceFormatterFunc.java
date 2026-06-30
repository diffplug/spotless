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
package com.diffplug.spotless.glue.pos;

import java.io.File;

import io.princeofspace.Formatter;
import io.princeofspace.model.FormatterConfig;
import io.princeofspace.model.IndentStyle;
import io.princeofspace.model.JavaLanguageLevel;
import io.princeofspace.model.WrapStyle;

import com.diffplug.spotless.FormatterFunc;

public class PrinceOfSpaceFormatterFunc implements FormatterFunc.NeedsFile {

	private final Formatter formatter;

	public PrinceOfSpaceFormatterFunc(String indentStyle, Integer indentSize, Integer lineLength, String wrapStyle,
			Boolean closingParenOnNewLine, Boolean trailingCommas, Integer javaLanguageLevel) {
		FormatterConfig.Builder builder = FormatterConfig.builder();
		if (indentStyle != null) {
			builder.indentStyle(IndentStyle.valueOf(indentStyle));
		}
		if (indentSize != null) {
			builder.indentSize(indentSize);
		}
		if (lineLength != null) {
			builder.lineLength(lineLength);
		}
		if (wrapStyle != null) {
			builder.wrapStyle(WrapStyle.valueOf(wrapStyle));
		}
		if (closingParenOnNewLine != null) {
			builder.closingParenOnNewLine(closingParenOnNewLine);
		}
		if (trailingCommas != null) {
			builder.trailingCommas(trailingCommas);
		}
		if (javaLanguageLevel != null) {
			builder.javaLanguageLevel(JavaLanguageLevel.of(javaLanguageLevel));
		}
		this.formatter = new Formatter(builder.build());
	}

	@Override
	public String applyWithFile(String unix, File file) throws Exception {
		return formatter.format(unix, file.toPath());
	}

	@Override
	public String toString() {
		return "PrinceOfSpaceFormatterFunc{formatter=" + formatter + '}';
	}
}
