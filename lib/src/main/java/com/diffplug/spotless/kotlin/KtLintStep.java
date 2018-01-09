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

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Objects;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.ThrowingEx;

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
			ClassLoader classLoader = jarState.getClassLoader();

			// String KtLint::format(String input, Iterable<RuleSet> rules, Function2 errorCallback)

			// first, we get the standard rules
			Class<?> standardRuleSetProviderClass = classLoader.loadClass("com.github.shyiko.ktlint.ruleset.standard.StandardRuleSetProvider");
			Object standardRuleSet = standardRuleSetProviderClass.getMethod("get").invoke(standardRuleSetProviderClass.newInstance());
			Iterable<?> ruleSets = Collections.singletonList(standardRuleSet);

			// next, we create an error callback which throws an assertion error when the format is bad
			Class<?> function2Interface = classLoader.loadClass("kotlin.jvm.functions.Function2");
			Class<?> lintErrorClass = classLoader.loadClass("com.github.shyiko.ktlint.core.LintError");
			Method detailGetter = lintErrorClass.getMethod("getDetail");
			Object formatterCallback = Proxy.newProxyInstance(classLoader, new Class[]{function2Interface},
					(proxy, method, args) -> {
						Object lintError = args[0]; // com.github.shyiko.ktlint.core.LintError
						boolean corrected = (Boolean) args[1];
						if (!corrected) {
							String detail = (String) detailGetter.invoke(lintError);
							throw new AssertionError(detail);
						}
						return null;
					});

			// grab the KtLint singleton
			Class<?> ktlintClass = classLoader.loadClass("com.github.shyiko.ktlint.core.KtLint");
			Object ktlint = ktlintClass.getDeclaredField("INSTANCE").get(null);
			// and its format method
			String formatterMethodName = isScript ? "formatScript" : "format";
			Method formatterMethod = ktlintClass.getMethod(formatterMethodName, String.class, Iterable.class, function2Interface);

			return input -> {
				try {
					String formatted = (String) formatterMethod.invoke(ktlint, input, ruleSets, formatterCallback);
					return formatted;
				} catch (InvocationTargetException e) {
					throw ThrowingEx.unwrapCause(e);
				}
			};
		}
	}
}
