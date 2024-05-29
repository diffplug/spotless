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

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugins.annotations.Parameter;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ThrowingEx;
import com.diffplug.spotless.kotlin.KtLintStep;
import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.FormatterStepFactory;

public class Ktlint implements FormatterStepFactory {
	@Parameter
	private String version;
	@Parameter
	private String editorConfigPath;
	@Parameter
	private Map<String, Object> editorConfigOverride;
	@Parameter
	private List<String> customRuleSets;

	@Override
	public FormatterStep newFormatterStep(final FormatterStepConfig stepConfig) {
		String ktlintVersion = version != null ? version : KtLintStep.defaultVersion();
		FileSignature configPath = null;
		if (editorConfigPath == null) {
			File defaultEditorConfig = new File(".editorconfig");
			if (defaultEditorConfig.exists()) {
				editorConfigPath = defaultEditorConfig.getPath();
			}
		}
		if (editorConfigPath != null) {
			configPath = ThrowingEx.get(() -> FileSignature.signAsList(stepConfig.getFileLocator().locateFile(editorConfigPath)));
		}
		if (editorConfigOverride == null) {
			editorConfigOverride = new HashMap<>();
		}
		if (customRuleSets == null) {
			customRuleSets = Collections.emptyList();
		}
		return KtLintStep.create(
				ktlintVersion,
				stepConfig.getProvisioner(),
				configPath,
				editorConfigOverride,
				customRuleSets);
	}
}
