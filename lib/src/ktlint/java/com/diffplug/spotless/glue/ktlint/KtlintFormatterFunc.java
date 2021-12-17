/*
 * Copyright 2021 DiffPlug
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.pinterest.ktlint.core.KtLint;
import com.pinterest.ktlint.core.KtLint.Params;
import com.pinterest.ktlint.core.LintError;
import com.pinterest.ktlint.core.RuleSet;
import com.pinterest.ktlint.ruleset.standard.StandardRuleSetProvider;

import com.diffplug.spotless.FormatterFunc;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;

public class KtlintFormatterFunc implements FormatterFunc.NeedsFile {
	private static final Logger logger = Logger.getLogger(KtlintFormatterFunc.class.getName());

	private final List<RuleSet> rulesets;
	private final Function2<? super LintError, ? super Boolean, Unit> formatterCallback;
	private final boolean isScript;
	private final Map<String, String> userData;
	private final String editorConfig;

	public KtlintFormatterFunc(boolean isScript, Map<String, String> userData, String editorConfig) {
		rulesets = Collections.singletonList(new StandardRuleSetProvider().get());
		formatterCallback = new FormatterCallback();
		this.isScript = isScript;
		this.userData = userData;
		this.editorConfig = editorConfig;
	}

	static class FormatterCallback implements Function2<LintError, Boolean, Unit> {
		@Override
		public Unit invoke(LintError lint, Boolean corrected) {
			if (!corrected) {
				throw new AssertionError("Error on line: " + lint.getLine() + ", column: " + lint.getCol() + "\n" + lint.getDetail());
			}
			return null;
		}
	}

	String searchEditorConfig(File file) {
		if (this.editorConfig == null) {
			return null;
		}
		if (!this.editorConfig.equals("?")) {
			return editorConfig;
		}
		Path path = file.toPath().toAbsolutePath();
		while (true) {
			Path dir = path.getParent();
			if (dir == null) {
				break;
			}
			Path editorConfig = dir.resolve(".editorconfig");
			if (Files.isRegularFile(editorConfig)) {
				return editorConfig.toString();
			}
			path = dir;
		}
		return null;
	}

	@Override
	public String applyWithFile(String unix, File file) throws Exception {
		return KtLint.INSTANCE.format(new Params(
				file.getName(),
				unix,
				rulesets,
				userData,
				formatterCallback,
				isScript,
				searchEditorConfig(file),
				false));
	}
}
