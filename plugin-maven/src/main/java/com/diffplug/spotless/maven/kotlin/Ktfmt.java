/*
 * Copyright 2016-2024 DiffPlug
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
package com.diffplug.spotless.maven.kotlin;

import org.apache.maven.plugins.annotations.Parameter;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.kotlin.KtfmtStep;
import com.diffplug.spotless.kotlin.KtfmtStep.KtfmtFormattingOptions;
import com.diffplug.spotless.kotlin.KtfmtStep.Style;
import com.diffplug.spotless.kotlin.KtfmtStep.TrailingCommaManagementStrategy;
import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.FormatterStepFactory;

public class Ktfmt implements FormatterStepFactory {

	@Parameter
	private String version;

	@Parameter
	private String style;

	@Parameter
	private Integer maxWidth;

	@Parameter
	private Integer blockIndent;

	@Parameter
	private Integer continuationIndent;

	@Parameter
	private Boolean removeUnusedImports;

	@Parameter
	private TrailingCommaManagementStrategy trailingCommaManagementStrategy;

	@Override
	public FormatterStep newFormatterStep(FormatterStepConfig config) {
		String version = this.version != null ? this.version : KtfmtStep.defaultVersion();
		Style style = this.style != null ? Style.valueOf(this.style) : null;
		KtfmtFormattingOptions options = new KtfmtFormattingOptions(maxWidth, blockIndent, continuationIndent, removeUnusedImports, trailingCommaManagementStrategy);
		return KtfmtStep.create(version, config.getProvisioner(), style, options);
	}
}
