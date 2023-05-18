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
package com.diffplug.spotless.glue.ktlint;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.glue.ktlint.compat.*;

public class KtlintFormatterFunc implements FormatterFunc.NeedsFile {

	private final Map<String, String> userData;
	private final boolean isScript;
	@NotNull
	private final KtLintCompatAdapter adapter;
	private final boolean useExperimental;
	private final FileSignature editorConfigPath;
	private final Map<String, Object> editorConfigOverrideMap;

	public KtlintFormatterFunc(String version, boolean isScript, boolean useExperimental, FileSignature editorConfigPath, Map<String, String> userData,
			Map<String, Object> editorConfigOverrideMap) {
		int minorVersion = Integer.parseInt(version.split("\\.")[1]);
		if (minorVersion >= 49) {
			// Packages and modules moved around (`ktlint-core` -> `ktlint-rule-engine`)
			// Experimental ruleset was replaced by implementing `Rule.Experimental` and checking the `ktlint_experimental` `.editorconfig` property
			// `RuleId` and `RuleSetId` became inline classes (mangled to be unrepresentable in Java source code, so reflection is needed), tracked here: https://github.com/pinterest/ktlint/issues/2041
			this.adapter = new KtLintCompat0Dot49Dot0Adapter();
		} else if (minorVersion == 48) {
			// ExperimentalParams lost two constructor arguments, EditorConfigProperty moved to its own class
			this.adapter = new KtLintCompat0Dot48Dot0Adapter();
		} else {
			// rename RuleSet to RuleProvider
			this.adapter = new KtLintCompat0Dot47Dot0Adapter();
		}
		this.editorConfigPath = editorConfigPath;
		this.useExperimental = useExperimental;
		this.editorConfigOverrideMap = editorConfigOverrideMap;
		this.userData = userData;
		this.isScript = isScript;
	}

	@Override
	public String applyWithFile(String unix, File file) {

		Path absoluteEditorConfigPath = null;
		if (editorConfigPath != null) {
			absoluteEditorConfigPath = editorConfigPath.getOnlyFile().toPath();
		}
		return adapter.format(unix, file.toPath(), isScript, useExperimental, absoluteEditorConfigPath, userData, editorConfigOverrideMap);
	}
}
