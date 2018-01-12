/*
 * Copyright 2016 DiffPlug
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
package com.diffplug.spotless.kotlin;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;
import com.github.shyiko.ktlint.core.KtLint;
import com.github.shyiko.ktlint.core.LintError;
import com.github.shyiko.ktlint.core.RuleSet;
import com.github.shyiko.ktlint.ruleset.standard.StandardRuleSetProvider;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Wraps up [ktlint](https://github.com/shyiko/ktlint) as a FormatterStep. */
public class KtLintStep {
	// prevent direct instantiation
	private KtLintStep() {}

	private static final String DEFAULT_VERSION = "0.14.0";
	static final String NAME = "ktlint";
	static final String MAVEN_COORDINATE = "com.github.shyiko:ktlint:";

	public static FormatterStep create(Provisioner provisioner) {
		return create(defaultVersion(), provisioner);
	}

	public static FormatterStep create(String version, Provisioner provisioner) {
		return create(version, provisioner, false);
	}

	public static FormatterStep createForScript(String version, Provisioner provisioner) {
		return create(version, provisioner, true);
	}

	private static FormatterStep create(String version, Provisioner provisioner, boolean isScript) {
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(provisioner, "provisioner");
		return FormatterStep.createLazy(NAME,
				() -> new State(version, provisioner, isScript),
				State::createFormat);
	}

	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	static final class State implements Serializable {
		private static final long serialVersionUID = 1L;

		/** Are the files being linted Kotlin script files. */
		private final boolean isScript;
		/** The jar that contains the eclipse formatter. */
		final JarState jarState;

		State(String version, Provisioner provisioner, boolean isScript) throws IOException {
			this.jarState = JarState.from(MAVEN_COORDINATE + version, provisioner);
			this.isScript = isScript;
		}

		FormatterFunc createFormat() throws Exception {

			RuleSet ruleSet = new StandardRuleSetProvider().get();
			List<RuleSet> rules = Collections.singletonList(ruleSet);

			Function2<? super LintError,? super Boolean, Unit> formatterCallback = (LintError error, Boolean corrected) -> {
				if (!corrected) {
					throw new AssertionError(error.getDetail());
				}
				return null;
			};

			// grab the KtLint singleton
			KtLint ktLint = KtLint.INSTANCE;
			return input -> {
				if (isScript) {
					return ktLint.format(input, rules, formatterCallback);
				} else {
					return ktLint.formatScript(input, rules, formatterCallback);
				}
			};
		}
	}
}
