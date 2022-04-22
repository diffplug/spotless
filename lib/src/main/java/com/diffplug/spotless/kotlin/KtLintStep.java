/*
 * Copyright 2016-2022 DiffPlug
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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.ThrowingEx;

/** Wraps up <a href="https://github.com/pinterest/ktlint">ktlint</a> as a FormatterStep. */
public class KtLintStep {
	// prevent direct instantiation
	private KtLintStep() {}

	private static final String DEFAULT_VERSION = "0.43.2";
	static final String NAME = "ktlint";
	static final String PACKAGE_PRE_0_32 = "com.github.shyiko";
	static final String PACKAGE = "com.pinterest";
	static final String MAVEN_COORDINATE_PRE_0_32 = PACKAGE_PRE_0_32 + ":ktlint:";
	static final String MAVEN_COORDINATE = PACKAGE + ":ktlint:";

	public static FormatterStep create(Provisioner provisioner) {
		return create(defaultVersion(), provisioner);
	}

	public static FormatterStep create(String version, Provisioner provisioner) {
		return create(version, provisioner, false, Collections.emptyMap());
	}

	public static FormatterStep create(String version, Provisioner provisioner, boolean useExperimental, Map<String, String> userData) {
		return create(version, provisioner, false, useExperimental, userData);
	}

	public static FormatterStep createForScript(String version, Provisioner provisioner) {
		return create(version, provisioner, true, false, Collections.emptyMap());
	}

	public static FormatterStep createForScript(String version, Provisioner provisioner, boolean useExperimental, Map<String, String> userData) {
		return create(version, provisioner, true, useExperimental, userData);
	}

	private static FormatterStep create(String version, Provisioner provisioner, boolean isScript, boolean useExperimental, Map<String, String> userData) {
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(provisioner, "provisioner");
		return FormatterStep.createLazy(NAME,
				() -> new State(version, provisioner, isScript, useExperimental, userData),
				State::createFormat);
	}

	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	static final class State implements Serializable {
		private static final long serialVersionUID = 1L;

		/** Are the files being linted Kotlin script files. */
		private final boolean isScript;
		private final String pkg;
		/** The jar that contains the formatter. */
		final JarState jarState;
		private final boolean useExperimental;
		private final TreeMap<String, String> userData;
		private final boolean useParams;

		State(String version, Provisioner provisioner, boolean isScript, boolean useExperimental, Map<String, String> userData) throws IOException {
			this.useExperimental = useExperimental;
			this.userData = new TreeMap<>(userData);
			String coordinate;
			if (BadSemver.version(version) < BadSemver.version(0, 32)) {
				coordinate = MAVEN_COORDINATE_PRE_0_32;
				this.pkg = PACKAGE_PRE_0_32;
			} else {
				coordinate = MAVEN_COORDINATE;
				this.pkg = PACKAGE;
			}
			this.useParams = BadSemver.version(version) >= BadSemver.version(0, 34);
			this.jarState = JarState.from(coordinate + version, provisioner);
			this.isScript = isScript;
		}

		FormatterFunc createFormat() throws Exception {
			if (useParams) {
				Class<?> formatterFunc = jarState.getClassLoader().loadClass("com.diffplug.spotless.glue.ktlint.KtlintFormatterFunc");
				Constructor<?> constructor = formatterFunc.getConstructor(boolean.class, boolean.class, Map.class);
				return (FormatterFunc.NeedsFile) constructor.newInstance(isScript, useExperimental, userData);
			}

			ClassLoader classLoader = jarState.getClassLoader();
			// String KtLint::format(String input, Iterable<RuleSet> rules, Function2 errorCallback)

			ArrayList<Object> ruleSets = new ArrayList<>();

			// first, we get the standard rules
			Class<?> standardRuleSetProviderClass = classLoader.loadClass(pkg + ".ktlint.ruleset.standard.StandardRuleSetProvider");
			Object standardRuleSet = standardRuleSetProviderClass.getMethod("get").invoke(standardRuleSetProviderClass.newInstance());
			ruleSets.add(standardRuleSet);

			// second, we get the experimental rules if desired
			if (useExperimental) {
				Class<?> experimentalRuleSetProviderClass = classLoader.loadClass(pkg + ".ktlint.ruleset.experimental.ExperimentalRuleSetProvider");
				Object experimentalRuleSet = experimentalRuleSetProviderClass.getMethod("get").invoke(experimentalRuleSetProviderClass.newInstance());
				ruleSets.add(experimentalRuleSet);
			}

			// next, we create an error callback which throws an assertion error when the format is bad
			Class<?> function2Interface = classLoader.loadClass("kotlin.jvm.functions.Function2");
			Class<?> lintErrorClass = classLoader.loadClass(pkg + ".ktlint.core.LintError");
			Method detailGetter = lintErrorClass.getMethod("getDetail");
			Method lineGetter = lintErrorClass.getMethod("getLine");
			Method colGetter = lintErrorClass.getMethod("getCol");
			Object formatterCallback = Proxy.newProxyInstance(classLoader, new Class[]{function2Interface},
					(proxy, method, args) -> {
						Object lintError = args[0]; //ktlint.core.LintError
						boolean corrected = (Boolean) args[1];
						if (!corrected) {
							String detail = (String) detailGetter.invoke(lintError);
							int line = (Integer) lineGetter.invoke(lintError);
							int col = (Integer) colGetter.invoke(lintError);
							throw new AssertionError("Error on line: " + line + ", column: " + col + "\n" + detail);
						}
						return null;
					});

			// grab the KtLint singleton
			Class<?> ktlintClass = classLoader.loadClass(pkg + ".ktlint.core.KtLint");
			Object ktlint = ktlintClass.getDeclaredField("INSTANCE").get(null);

			// and its format method
			String formatterMethodName = isScript ? "formatScript" : "format";
			Method formatterMethod = ktlintClass.getMethod(formatterMethodName, String.class, Iterable.class, Map.class, function2Interface);
			return input -> {
				try {
					return (String) formatterMethod.invoke(ktlint, input, ruleSets, userData, formatterCallback);
				} catch (InvocationTargetException e) {
					throw ThrowingEx.unwrapCause(e);
				}
			};
		}
	}
}
