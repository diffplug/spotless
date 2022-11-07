/*
 * Copyright 2022 DiffPlug
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
package com.diffplug.spotless.glue.ktlint.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.github.shyiko.ktlint.core.KtLint;
import com.github.shyiko.ktlint.core.LintError;
import com.github.shyiko.ktlint.core.RuleSet;
import com.github.shyiko.ktlint.ruleset.experimental.ExperimentalRuleSetProvider;
import com.github.shyiko.ktlint.ruleset.standard.StandardRuleSetProvider;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;

public class KtLintCompat0Dot31Dot0Adapter implements KtLintCompatAdapter {
	private final ArrayList<String> errors = new ArrayList<>();

	static class FormatterCallback implements Function2<LintError, Boolean, Unit> {
		private final ArrayList<String> errors;

		FormatterCallback(final ArrayList<String> errors) {
			this.errors = errors;
		}

		@Override
		public Unit invoke(LintError lint, Boolean corrected) {
			if (!corrected) {
				KtLintCompatReporting.addReport(errors, lint.getLine(), lint.getCol(), lint.getRuleId(), lint.getDetail());
			}
			return null;
		}
	}

	@Override
	public String format(final String text, final String name, final boolean isScript,
			final boolean useExperimental,
			final Map<String, String> userData,
			final Map<String, Object> editorConfigOverrideMap) {
		final FormatterCallback formatterCallback = new FormatterCallback(errors);

		final List<RuleSet> rulesets = new ArrayList<>();
		rulesets.add(new StandardRuleSetProvider().get());

		if (useExperimental) {
			rulesets.add(new ExperimentalRuleSetProvider().get());
		}

		final String result = KtLint.INSTANCE.format(
				text,
				rulesets,
				userData,
				formatterCallback);

		if (!errors.isEmpty()) {
			KtLintCompatReporting.report(errors);
		}

		return result;
	}
}
