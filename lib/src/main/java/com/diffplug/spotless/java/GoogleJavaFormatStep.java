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
package com.diffplug.spotless.java;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Objects;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.Provisioner;

/** Wraps up [google-java-format](https://github.com/google/google-java-format) as a FormatterStep. */
public class GoogleJavaFormatStep {
	// prevent direct instantiation
	private GoogleJavaFormatStep() {}

	private static final String DEFAULT_VERSION = "1.3";
	static final String NAME = "google-java-format";
	static final String MAVEN_COORDINATE = "com.google.googlejavaformat:google-java-format:";
	static final String FORMATTER_CLASS = "com.google.googlejavaformat.java.Formatter";
	static final String FORMATTER_METHOD = "formatSource";

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
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(provisioner, "provisioner");
		return FormatterStep.createLazy(NAME,
				() -> new State(NAME, version, provisioner),
				State::createFormat);
	}

	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	static final class State implements Serializable {
		private static final long serialVersionUID = 1L;

		/** The jar that contains the eclipse formatter. */
		final JarState jarState;
		final String stepName;
		final String version;

		State(String stepName, String version, Provisioner provisioner) throws IOException {
			this.jarState = JarState.from(MAVEN_COORDINATE + version, provisioner);
			this.stepName = stepName;
			this.version = version;
		}

		@SuppressWarnings({"unchecked", "rawtypes"})
		FormatterFunc createFormat() throws Exception {
			ClassLoader classLoader = jarState.getClassLoader();

			// instantiate the formatter and get its format method
			Class<?> formatterClazz = classLoader.loadClass(FORMATTER_CLASS);
			Object formatter = formatterClazz.getConstructor().newInstance();
			Method formatterMethod = formatterClazz.getMethod(FORMATTER_METHOD, String.class);

			Class<?> removeUnusedClass = classLoader.loadClass(REMOVE_UNUSED_CLASS);
			Class<?> removeJavadocOnlyClass = classLoader.loadClass(REMOVE_UNUSED_IMPORT_JavadocOnlyImports);
			Object removeJavadocConstant = Enum.valueOf((Class<Enum>) removeJavadocOnlyClass, REMOVE_UNUSED_IMPORT_JavadocOnlyImports_Keep);
			Method removeUnusedMethod = removeUnusedClass.getMethod(REMOVE_UNUSED_METHOD, String.class, removeJavadocOnlyClass);

			Class<?> importOrdererClass = classLoader.loadClass(IMPORT_ORDERER_CLASS);
			Method importOrdererMethod = importOrdererClass.getMethod(IMPORT_ORDERER_METHOD, String.class);

			return input -> {
				String formatted = (String) formatterMethod.invoke(formatter, input);
				String removedUnused = (String) removeUnusedMethod.invoke(null, formatted, removeJavadocConstant);
				String sortedImports = (String) importOrdererMethod.invoke(null, removedUnused);
				return fixWindowsBug(sortedImports, version);
			};
		}

		@SuppressWarnings({"unchecked", "rawtypes"})
		FormatterFunc createRemoveUnusedImportsOnly() throws Exception {
			ClassLoader classLoader = jarState.getClassLoader();

			Class<?> removeUnusedClass = classLoader.loadClass(REMOVE_UNUSED_CLASS);
			Class<?> removeJavadocOnlyClass = classLoader.loadClass(REMOVE_UNUSED_IMPORT_JavadocOnlyImports);
			Object removeJavadocConstant = Enum.valueOf((Class<Enum>) removeJavadocOnlyClass, REMOVE_UNUSED_IMPORT_JavadocOnlyImports_Keep);
			Method removeUnusedMethod = removeUnusedClass.getMethod(REMOVE_UNUSED_METHOD, String.class, removeJavadocOnlyClass);

			return input -> {
				String removeUnused = (String) removeUnusedMethod.invoke(null, input, removeJavadocConstant);
				return fixWindowsBug(removeUnused, version);
			};
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
}
