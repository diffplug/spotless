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
import com.diffplug.spotless.java.PrinceOfSpaceStep;
import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.FormatterStepFactory;

public class PrinceOfSpace implements FormatterStepFactory {

	@Parameter
	private String version;

	@Parameter
	private String indentStyle;

	@Parameter
	private Integer indentSize;

	@Parameter
	private Integer lineLength;

	@Parameter
	private String wrapStyle;

	@Parameter
	private Boolean closingParenOnNewLine;

	@Parameter
	private Boolean trailingCommas;

	@Parameter
	private Integer javaLanguageLevel;

	@Override
	public FormatterStep newFormatterStep(FormatterStepConfig config) {
		String version = this.version != null ? this.version : PrinceOfSpaceStep.defaultVersion();
		PrinceOfSpaceStep.Options options = new PrinceOfSpaceStep.Options();
		if (indentStyle != null) {
			options.setIndentStyle(indentStyle);
		}
		if (indentSize != null) {
			options.setIndentSize(indentSize);
		}
		if (lineLength != null) {
			options.setLineLength(lineLength);
		}
		if (wrapStyle != null) {
			options.setWrapStyle(wrapStyle);
		}
		if (closingParenOnNewLine != null) {
			options.setClosingParenOnNewLine(closingParenOnNewLine);
		}
		if (trailingCommas != null) {
			options.setTrailingCommas(trailingCommas);
		}
		if (javaLanguageLevel != null) {
			options.setJavaLanguageLevel(javaLanguageLevel);
		}
		return PrinceOfSpaceStep.create(version, config.getProvisioner(), options);
	}
}
