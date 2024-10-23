/*
 * Copyright 2021-2024 DiffPlug
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

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.Lint;
import com.diffplug.spotless.glue.ktlint.compat.*;

public class KtlintFormatterFunc implements FormatterFunc.NeedsFile {
	private final KtLintCompatAdapter adapter;
	private final FileSignature editorConfigPath;
	private final Map<String, Object> editorConfigOverrideMap;

	public KtlintFormatterFunc(
			String version,
			FileSignature editorConfigPath,
			Map<String, Object> editorConfigOverrideMap) {
		String[] versions = version.split("\\.");
		int majorVersion = Integer.parseInt(versions[0]);
		int minorVersion = Integer.parseInt(versions[1]);
		if (majorVersion == 1) {
			this.adapter = new KtLintCompat1Dot0Dot0Adapter();
		} else {
			if (minorVersion >= 50) {
				// Fixed `RuleId` and `RuleSetId` issues
				// New argument to `EditorConfigDefaults.Companion.load(...)` for custom property type parsing
				// New argument to `new KtLintRuleEngine(...)` to fail on usage of `treeCopyHandler` extension point
				this.adapter = new KtLintCompat0Dot50Dot0Adapter();
			} else if (minorVersion == 49) {
				// Packages and modules moved around (`ktlint-core` -> `ktlint-rule-engine`)
				// Experimental ruleset was replaced by implementing `Rule.Experimental` and checking the `ktlint_experimental` `.editorconfig` property
				// `RuleId` and `RuleSetId` became inline classes (mangled to be unrepresentable in Java source code, so reflection is needed), tracked here: https://github.com/pinterest/ktlint/issues/2041
				this.adapter = new KtLintCompat0Dot49Dot0Adapter();
			} else if (minorVersion == 48) {
				// ExperimentalParams lost two constructor arguments, EditorConfigProperty moved to its own class
				this.adapter = new KtLintCompat0Dot48Dot0Adapter();
			} else {
				throw new IllegalStateException("Ktlint versions < 0.48.0 not supported!");
			}
		}
		this.editorConfigPath = editorConfigPath;
		this.editorConfigOverrideMap = editorConfigOverrideMap;
	}

	@Override
	public String applyWithFile(String unix, File file) throws NoSuchFieldException, IllegalAccessException {
		Path absoluteEditorConfigPath = null;
		if (editorConfigPath != null) {
			absoluteEditorConfigPath = editorConfigPath.getOnlyFile().toPath();
		}
		try {
			return adapter.format(
					unix,
					file.toPath(),
					absoluteEditorConfigPath,
					editorConfigOverrideMap);
		} catch (KtLintCompatReporting.KtlintSpotlessException e) {
			throw Lint.atLine(e.line, e.ruleId, e.detail).shortcut();
		}
	}
}
