/*
 * Copyright 2022-2023 DiffPlug
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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.pinterest.ktlint.core.KtLint;
import com.pinterest.ktlint.core.LintError;
import com.pinterest.ktlint.core.RuleSet;
import com.pinterest.ktlint.ruleset.experimental.ExperimentalRuleSetProvider;
import com.pinterest.ktlint.ruleset.standard.StandardRuleSetProvider;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;

public class KtLintCompat0Dot34Dot2Adapter implements KtLintCompatAdapter {

	static class FormatterCallback implements Function2<LintError, Boolean, Unit> {
		@Override
		public Unit invoke(LintError lint, Boolean corrected) {
			if (!corrected) {
				KtLintCompatReporting.report(lint.getLine(), lint.getCol(), lint.getRuleId(), lint.getDetail());
			}
			return null;
		}
	}

	@Override
	public String format(final String text, Path path, final boolean isScript,
			final boolean useExperimental,
			Path editorConfigPath, final Map<String, String> userData,
			final Map<String, Object> editorConfigOverrideMap) {
		final FormatterCallback formatterCallback = new FormatterCallback();

		final List<RuleSet> rulesets = new ArrayList<>();
		rulesets.add(new StandardRuleSetProvider().get());

		if (useExperimental) {
			rulesets.add(new ExperimentalRuleSetProvider().get());
		}

		return KtLint.INSTANCE.format(new KtLint.Params(
				path.toFile().getAbsolutePath(),
				text,
				rulesets,
				userData,
				formatterCallback,
				isScript,
				editorConfigPath == null ? null : editorConfigPath.toFile().getAbsolutePath(),
				false));
	}
}
