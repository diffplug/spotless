/*
 * Copyright 2021-2022 DiffPlug
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
package com.diffplug.spotless.glue.diktat;

import java.io.File;
import java.util.*;

import org.cqfn.diktat.ruleset.rules.DiktatRuleSetProvider;

import com.pinterest.ktlint.core.KtLint;
import com.pinterest.ktlint.core.LintError;
import com.pinterest.ktlint.core.RuleSet;
import com.pinterest.ktlint.core.api.EditorConfigOverride;

import com.diffplug.spotless.FormatterFunc;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;

import org.jetbrains.annotations.NotNull;

public class DiktatFormatterFunc implements FormatterFunc.NeedsFile {

	private final List<RuleSet> rulesets;
	private final Function2<? super LintError, ? super Boolean, Unit> formatterCallback;
	private final boolean isScript;

	private final ArrayList<LintError> errors = new ArrayList<>();

	public DiktatFormatterFunc(boolean isScript) {
		rulesets = Collections.singletonList(new DiktatRuleSetProvider().get());
		this.formatterCallback = new FormatterCallback(errors);
		this.isScript = isScript;
	}

	static class FormatterCallback implements Function2<LintError, Boolean, Unit> {
		private final ArrayList<LintError> errors;

		FormatterCallback(ArrayList<LintError> errors) {
			this.errors = errors;
		}

		@Override
		public Unit invoke(LintError lintError, Boolean corrected) {
			if (!corrected) {
				errors.add(lintError);
			}
			return null;
		}
	}

	@Override
	public String applyWithFile(String unix, File file) throws Exception {
		errors.clear();
		String result = KtLint.INSTANCE.format(new KtLint.ExperimentalParams(
				// Unlike Ktlint, Diktat requires full path to the file.
				// See https://github.com/diffplug/spotless/issues/1189, https://github.com/analysis-dev/diktat/issues/1202
				file.getAbsolutePath(),
				unix,
				rulesets,
				Collections.emptyMap(),
				formatterCallback,
				isScript,
				null,
				false,
				new EditorConfigOverride(),
				false));

		if (!errors.isEmpty()) {
			StringBuilder error = new StringBuilder();
			error.append("There are ").append(errors.size()).append(" unfixed errors:");
			for (LintError er : errors) {
				error.append(System.lineSeparator())
						.append("Error on line: ").append(er.getLine())
						.append(", column: ").append(er.getCol())
						.append(" cannot be fixed automatically")
						.append(System.lineSeparator())
						.append(er.getDetail());
			}
			throw new AssertionError(error);
		}

		return result;
	}
}
