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
package com.diffplug.spotless.extra.antlr4;

import com.khubla.antlr4formatter.Antlr4Formatter;

import com.diffplug.spotless.FormatterStep;

public class Antlr4FormatterStep {

	public static final String NAME = "antlr4";

	private Antlr4FormatterStep() {}

	private static final String DEFAULT_VERSION = "4.7.1";
	static final String MAVEN_COORDINATE = "org.antlr:antlr4:";

	public static FormatterStep create() {
		return FormatterStep.createNeverUpToDate(NAME, Antlr4Formatter::format);
		//			(name,
		//				new State(name),
		//				step -> step::format);
	}

	//	private static final class State implements Serializable {
	//		private static final long serialVersionUID = 1L;
	//
	//		private final String name;
	//
	//		State(String name) {
	//			this.name = name;
	//		}
	//
	//		public static String format(String raw) {
	//			System.out.println("formatting source ...");
	//			return "asd";
	////			return Antlr4Formatter.format(raw);
	//		}
	//	}

	//	static final class State implements Serializable {
	//		private static final long serialVersionUID = 1L;
	//
	//		/** The jar that contains the formatter. */
	//		final JarState jarState;
	//
	//		State(String version, Provisioner provisioner) throws IOException {
	//			this.jarState = JarState.from(MAVEN_COORDINATE + version, provisioner);
	//		}
	//
	//		FormatterFunc createFormat() throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException {
	//			ClassLoader classLoader = jarState.getClassLoader();
	//
	//			// String KtLint::format(String input, Iterable<RuleSet> rules, Function2 errorCallback)
	//
	//			// first, we get the standard rules
	//			Class<?> standardRuleSetProviderClass = classLoader.loadClass("com.github.shyiko.ktlint.ruleset.standard.StandardRuleSetProvider");
	//			Object standardRuleSet = standardRuleSetProviderClass.getMethod("get").invoke(standardRuleSetProviderClass.newInstance());
	//			Iterable<?> ruleSets = Collections.singletonList(standardRuleSet);
	//
	//			// next, we create an error callback which throws an assertion error when the format is bad
	//			Class<?> function2Interface = classLoader.loadClass("kotlin.jvm.functions.Function2");
	//			Class<?> lintErrorClass = classLoader.loadClass("com.github.shyiko.ktlint.core.LintError");
	//			Method detailGetter = lintErrorClass.getMethod("getDetail");
	//			Method lineGetter = lintErrorClass.getMethod("getLine");
	//			Method colGetter = lintErrorClass.getMethod("getCol");
	//			Object formatterCallback = Proxy.newProxyInstance(classLoader, new Class[]{function2Interface},
	//				(proxy, method, args) -> {
	//					Object lintError = args[0]; // com.github.shyiko.ktlint.core.LintError
	//					boolean corrected = (Boolean) args[1];
	//					if (!corrected) {
	//						String detail = (String) detailGetter.invoke(lintError);
	//						int line = (Integer) lineGetter.invoke(lintError);
	//						int col = (Integer) colGetter.invoke(lintError);
	//						throw new AssertionError("Error on line: " + line + ", column: " + col + "\n" + detail);
	//					}
	//					return null;
	//				});
	//
	//			// grab the KtLint singleton
	//			Class<?> ktlintClass = classLoader.loadClass("com.github.shyiko.ktlint.core.KtLint");
	//			Object ktlint = ktlintClass.getDeclaredField("INSTANCE").get(null);
	//			// and its format method
	//			Method formatterMethod = ktlintClass.getMethod("", String.class, Iterable.class, Map.class, function2Interface);
	//
	//			return input -> {
	//				try {
	//					String formatted = (String) formatterMethod.invoke(ktlint, input, ruleSets, userData, formatterCallback);
	//					return formatted;
	//				} catch (InvocationTargetException e) {
	//					throw ThrowingEx.unwrapCause(e);
	//				}
	//			};
	//		}
	//	}
}
