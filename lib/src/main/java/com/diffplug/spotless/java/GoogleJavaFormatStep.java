/*
 * Copyright 2016-2021 DiffPlug
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
package com.diffplug.spotless.java;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.ThrowingEx.Function;

/** Wraps up [google-java-format](https://github.com/google/google-java-format) as a FormatterStep. */
public class GoogleJavaFormatStep {
	// prevent direct instantiation
	private GoogleJavaFormatStep() {}

	private static final String DEFAULT_STYLE = "GOOGLE";
	static final String NAME = "google-java-format";
	static final String MAVEN_COORDINATE = "com.google.googlejavaformat:google-java-format:";
	static final String FORMATTER_CLASS = "com.google.googlejavaformat.java.Formatter";
	static final String FORMATTER_METHOD = "formatSource";

	private static final String OPTIONS_CLASS = "com.google.googlejavaformat.java.JavaFormatterOptions";
	private static final String OPTIONS_BUILDER_METHOD = "builder";
	private static final String OPTIONS_BUILDER_CLASS = "com.google.googlejavaformat.java.JavaFormatterOptions$Builder";
	private static final String OPTIONS_BUILDER_STYLE_METHOD = "style";
	private static final String OPTIONS_BUILDER_BUILD_METHOD = "build";
	private static final String OPTIONS_Style = "com.google.googlejavaformat.java.JavaFormatterOptions$Style";

	private static final String REMOVE_UNUSED_CLASS = "com.google.googlejavaformat.java.RemoveUnusedImports";
	private static final String REMOVE_UNUSED_METHOD = "removeUnusedImports";

	private static final String REMOVE_UNUSED_IMPORT_JavadocOnlyImports = "com.google.googlejavaformat.java.RemoveUnusedImports$JavadocOnlyImports";
	private static final String REMOVE_UNUSED_IMPORT_JavadocOnlyImports_Keep = "KEEP";

	private static final String IMPORT_ORDERER_CLASS = "com.google.googlejavaformat.java.ImportOrderer";
	private static final String IMPORT_ORDERER_METHOD = "reorderImports";

	/** Creates a step which formats everything - code, import order, and unused imports. */
	public static FormatterStep create(Provisioner provisioner) {
		return create(defaultVersion(), provisioner);
	}

	/** Creates a step which formats everything - code, import order, and unused imports. */
	public static FormatterStep create(String version, Provisioner provisioner) {
		return create(version, DEFAULT_STYLE, provisioner);
	}

	/** Creates a step which formats everything - code, import order, and unused imports. */
	public static FormatterStep create(String version, String style, Provisioner provisioner) {
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(style, "style");
		Objects.requireNonNull(provisioner, "provisioner");
		return FormatterStep.createLazy(NAME,
				() -> new State(NAME, version, style, provisioner),
				State::createFormat);
	}

	private static final int JRE_VERSION;

	static {
		String jre = System.getProperty("java.version");
		if (jre.startsWith("1.8")) {
			JRE_VERSION = 8;
		} else {
			Matcher matcher = Pattern.compile("(\\d+)").matcher(jre);
			if (!matcher.find()) {
				throw new IllegalArgumentException("Expected " + jre + " to start with an integer");
			}
			JRE_VERSION = Integer.parseInt(matcher.group(1));
			if (JRE_VERSION <= 8) {
				throw new IllegalArgumentException("Expected " + jre + " to start with an integer greater than 8");
			}
		}
	}

	/** On JRE 11+, returns `1.9`. On earlier JREs, returns `1.7`. */
	public static String defaultVersion() {
		return JRE_VERSION >= 11 ? LATEST_VERSION_JRE_11 : LATEST_VERSION_JRE_8;
	}

	private static final String LATEST_VERSION_JRE_8 = "1.7";
	private static final String LATEST_VERSION_JRE_11 = "1.10.0";

	public static String defaultStyle() {
		return DEFAULT_STYLE;
	}

	static final class State implements Serializable {
		private static final long serialVersionUID = 1L;

		/** The jar that contains the eclipse formatter. */
		final JarState jarState;
		final String stepName;
		final String version;
		final String style;

		State(String stepName, String version, Provisioner provisioner) throws IOException {
			this(stepName, version, DEFAULT_STYLE, provisioner);
		}

		State(String stepName, String version, String style, Provisioner provisioner) throws IOException {
			this.jarState = JarState.from(MAVEN_COORDINATE + version, provisioner);
			this.stepName = stepName;
			this.version = version;
			this.style = style;
		}

		@SuppressWarnings({"unchecked", "rawtypes"})
		FormatterFunc createFormat() throws Exception {
			ClassLoader classLoader = jarState.getClassLoader();

			// instantiate the formatter and get its format method
			Class<?> optionsClass = classLoader.loadClass(OPTIONS_CLASS);
			Class<?> optionsBuilderClass = classLoader.loadClass(OPTIONS_BUILDER_CLASS);
			Method optionsBuilderMethod = optionsClass.getMethod(OPTIONS_BUILDER_METHOD);
			Object optionsBuilder = optionsBuilderMethod.invoke(null);

			Class<?> optionsStyleClass = classLoader.loadClass(OPTIONS_Style);
			Object styleConstant = Enum.valueOf((Class<Enum>) optionsStyleClass, style);
			Method optionsBuilderStyleMethod = optionsBuilderClass.getMethod(OPTIONS_BUILDER_STYLE_METHOD, optionsStyleClass);
			optionsBuilderStyleMethod.invoke(optionsBuilder, styleConstant);

			Method optionsBuilderBuildMethod = optionsBuilderClass.getMethod(OPTIONS_BUILDER_BUILD_METHOD);
			Object options = optionsBuilderBuildMethod.invoke(optionsBuilder);

			Class<?> formatterClazz = classLoader.loadClass(FORMATTER_CLASS);
			Object formatter = formatterClazz.getConstructor(optionsClass).newInstance(options);
			Method formatterMethod = formatterClazz.getMethod(FORMATTER_METHOD, String.class);

			Function<String, String> removeUnused = constructRemoveUnusedFunction(classLoader);

			Class<?> importOrdererClass = classLoader.loadClass(IMPORT_ORDERER_CLASS);
			Method importOrdererMethod = importOrdererClass.getMethod(IMPORT_ORDERER_METHOD, String.class);

			return suggestJre11(input -> {
				String formatted = (String) formatterMethod.invoke(formatter, input);
				String removedUnused = removeUnused.apply(formatted);
				String sortedImports = (String) importOrdererMethod.invoke(null, removedUnused);
				return fixWindowsBug(sortedImports, version);
			});
		}

		FormatterFunc createRemoveUnusedImportsOnly() throws Exception {
			ClassLoader classLoader = jarState.getClassLoader();
			Function<String, String> removeUnused = constructRemoveUnusedFunction(classLoader);
			return suggestJre11(input -> fixWindowsBug(removeUnused.apply(input), version));
		}

		private static Function<String, String> constructRemoveUnusedFunction(ClassLoader classLoader)
				throws NoSuchMethodException, ClassNotFoundException {
			Class<?> removeUnusedClass = classLoader.loadClass(REMOVE_UNUSED_CLASS);
			Class<?> removeJavadocOnlyClass;
			try {
				// google-java-format 1.7 or lower
				removeJavadocOnlyClass = classLoader.loadClass(REMOVE_UNUSED_IMPORT_JavadocOnlyImports);
			} catch (ClassNotFoundException e) {
				// google-java-format 1.8+
				removeJavadocOnlyClass = null;
			}

			Function<String, String> removeUnused;
			if (removeJavadocOnlyClass != null) {
				@SuppressWarnings({"unchecked", "rawtypes"})
				Object removeJavadocConstant = Enum.valueOf((Class<Enum>) removeJavadocOnlyClass, REMOVE_UNUSED_IMPORT_JavadocOnlyImports_Keep);
				Method removeUnusedMethod = removeUnusedClass.getMethod(REMOVE_UNUSED_METHOD, String.class, removeJavadocOnlyClass);
				removeUnused = (x) -> (String) removeUnusedMethod.invoke(null, x, removeJavadocConstant);
			} else {
				Method removeUnusedMethod = removeUnusedClass.getMethod(REMOVE_UNUSED_METHOD, String.class);
				removeUnused = (x) -> (String) removeUnusedMethod.invoke(null, x);
			}
			return removeUnused;
		}
	}

	private static final boolean IS_WINDOWS = LineEnding.PLATFORM_NATIVE.str().equals("\r\n");

	/**
	 * google-java-format-1.1's removeUnusedImports does *wacky* stuff on Windows.
	 * The beauty of normalizing all line endings to unix!
	 */
	static String fixWindowsBug(String input, String version) {
		if (IS_WINDOWS && version.equals("1.1")) {
			int firstImport = input.indexOf("\nimport ");
			if (firstImport == 0) {
				return input;
			} else if (firstImport > 0) {
				int numToTrim = 0;
				char prevChar;
				do {
					++numToTrim;
					prevChar = input.charAt(firstImport - numToTrim);
				} while (Character.isWhitespace(prevChar) && (firstImport - numToTrim) > 0);
				if (firstImport - numToTrim == 0) {
					// import was the very first line, and we'd like to maintain a one-line gap
					++numToTrim;
				} else if (prevChar == ';' || prevChar == '/') {
					// import came after either license or a package declaration
					--numToTrim;
				}
				if (numToTrim > 0) {
					return input.substring(0, firstImport - numToTrim + 2) + input.substring(firstImport + 1);
				}
			}
		}
		return input;
	}

	private static FormatterFunc suggestJre11(FormatterFunc in) {
		if (JRE_VERSION >= 11) {
			return in;
		} else {
			return unixIn -> {
				try {
					return in.apply(unixIn);
				} catch (Exception e) {
					throw new Exception("You are running Spotless on JRE " + JRE_VERSION + ", which limits you to google-java-format " + LATEST_VERSION_JRE_8 + "\n"
							+ "If you upgrade your build JVM to 11+, then you can use google-java-format " + LATEST_VERSION_JRE_11 + ", which may have fixed this problem.", e);
				}
			};
		}
	}
}
