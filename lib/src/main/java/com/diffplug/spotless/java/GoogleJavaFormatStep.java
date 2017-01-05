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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.Objects;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

/** Wraps up [google-java-format](https://github.com/google/google-java-format) as a FormatterStep. */
public class GoogleJavaFormatStep {
	// prevent direct instantiation
	private GoogleJavaFormatStep() {}

	private static final String DEFAULT_VERSION = "1.1";
	private static final String NAME = "google-java-format";
	private static final String MAVEN_COORDINATE = "com.google.googlejavaformat:google-java-format:";
	private static final String FORMATTER_CLASS = "com.google.googlejavaformat.java.Formatter";
	private static final String FORMATTER_METHOD = "formatSource";

	private static final String REMOVE_UNUSED_CLASS = "com.google.googlejavaformat.java.RemoveUnusedImports";
	private static final String REMOVE_UNUSED_METHOD = "removeUnusedImports";

	private static final String REMOVE_UNUSED_IMPORT_JavadocOnlyImports = "com.google.googlejavaformat.java.RemoveUnusedImports$JavadocOnlyImports";
	private static final String REMOVE_UNUSED_IMPORT_JavadocOnlyImports_Keep = "KEEP";

	private static final String IMPORT_ORDERER_CLASS = "com.google.googlejavaformat.java.ImportOrderer";
	private static final String IMPORT_ORDERER_METHOD = "reorderImports";

	/** Creates a formatter step for the given version and settings file. */
	public static FormatterStep create(Provisioner provisioner) {
		return create(defaultVersion(), provisioner);
	}

	/** Creates a formatter step for the given version and settings file. */
	public static FormatterStep create(String version, Provisioner provisioner) {
		return FormatterStep.createCloseableLazy(NAME,
				() -> new State(JarState.from(MAVEN_COORDINATE + version, provisioner)),
				State::createFormat);
	}

	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	private static class State implements Serializable {
		private static final long serialVersionUID = 1L;

		/** The jar that contains the eclipse formatter. */
		final JarState jarState;

		State(JarState jarState) {
			this.jarState = Objects.requireNonNull(jarState);
		}

		@SuppressWarnings({"unchecked", "rawtypes"})
		FormatterFunc.Closeable createFormat() throws Exception {
			URLClassLoader classLoader = jarState.openIsolatedClassLoader();

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

			return FormatterFunc.Closeable.of(classLoader, input -> {
				String formatted = (String) formatterMethod.invoke(formatter, input);
				String removedUnused = (String) removeUnusedMethod.invoke(null, formatted, removeJavadocConstant);
				String sortedImports = (String) importOrdererMethod.invoke(null, removedUnused);
				return sortedImports;
			});
		}
	}
}
