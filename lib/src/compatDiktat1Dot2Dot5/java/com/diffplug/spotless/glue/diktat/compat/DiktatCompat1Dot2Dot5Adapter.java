/*
 * Copyright 2023 DiffPlug
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
package com.diffplug.spotless.glue.diktat.compat;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.cqfn.diktat.ruleset.rules.DiktatRuleSetProvider;

import com.pinterest.ktlint.core.KtLint;
import com.pinterest.ktlint.core.LintError;
import com.pinterest.ktlint.core.RuleSet;
import com.pinterest.ktlint.core.api.EditorConfigOverride;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;

public class DiktatCompat1Dot2Dot5Adapter implements DiktatCompatAdapter {
	private final List<RuleSet> ruleSets;
	private final Function2<? super LintError, ? super Boolean, Unit> formatterCallback;
	private final ArrayList<LintError> errors = new ArrayList<>();

	public DiktatCompat1Dot2Dot5Adapter(@Nullable File configFile) {
		if (configFile != null) {
			System.setProperty("diktat.config.path", configFile.getAbsolutePath());
		}
		this.ruleSets = Collections.singletonList(new DiktatRuleSetProvider().get());
		this.formatterCallback = new FormatterCallback(errors);
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
	public String format(final File file, final String content, final boolean isScript) {
		errors.clear();
		String result = KtLint.INSTANCE.format(new KtLint.ExperimentalParams(
				// Unlike Ktlint, Diktat requires full path to the file.
				// See https://github.com/diffplug/spotless/issues/1189, https://github.com/analysis-dev/diktat/issues/1202
				file.getAbsolutePath(),
				content,
				ruleSets,
				Collections.emptyMap(),
				formatterCallback,
				isScript,
				null,
				false,
				new EditorConfigOverride(),
				false));

		DiktatReporting.reportIfRequired(errors, LintError::getLine, LintError::getCol, LintError::getDetail);

		return result;
	}
}
