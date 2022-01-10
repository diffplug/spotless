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
package com.diffplug.spotless.java;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Objects;

import com.diffplug.spotless.*;
import com.diffplug.spotless.ThrowingEx.Function;

/** Wraps up <a href="https://github.com/palantir/palantir-java-format">palantir-java-format</a> fork of
 * <a href="https://github.com/google/google-java-format">google-java-format</a> as a FormatterStep. */
public class PalantirJavaFormatStep {
	// prevent direct instantiation
	private PalantirJavaFormatStep() {}

	private static final String DEFAULT_STYLE = "PALANTIR";
	static final String NAME = "palantir-java-format";
	static final String MAVEN_COORDINATE = "com.palantir.javaformat:palantir-java-format";
	static final String FORMATTER_CLASS = "com.palantir.javaformat.java.Formatter";
	static final String FORMATTER_METHOD = "formatSource";

	private static final String OPTIONS_CLASS = "com.palantir.javaformat.java.JavaFormatterOptions";
	private static final String OPTIONS_BUILDER_METHOD = "builder";
	private static final String OPTIONS_BUILDER_CLASS = "com.palantir.javaformat.java.JavaFormatterOptions$Builder";
	private static final String OPTIONS_BUILDER_STYLE_METHOD = "style";
	private static final String OPTIONS_BUILDER_BUILD_METHOD = "build";
	private static final String OPTIONS_Style = "com.palantir.javaformat.java.JavaFormatterOptions$Style";
	private static final String OPTIONS_MAX_LINE_LENGTH_METHOD = "maxLineLength";

	private static final String REMOVE_UNUSED_CLASS = "com.palantir.javaformat.java.RemoveUnusedImports";
	private static final String REMOVE_UNUSED_METHOD = "removeUnusedImports";

	private static final String IMPORT_ORDERER_CLASS = "com.palantir.javaformat.java.ImportOrderer";
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
		return create(MAVEN_COORDINATE, version, style, provisioner);
	}

	/** Creates a step which formats everything - groupArtifact, code, import order, and unused imports. */
	public static FormatterStep create(String groupArtifact, String version, String style, Provisioner provisioner) {
		Objects.requireNonNull(groupArtifact, "groupArtifact");
		if (groupArtifact.chars().filter(ch -> ch == ':').count() != 1) {
			throw new IllegalArgumentException("groupArtifact must be in the form 'groupId:artifactId'");
		}
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(style, "style");
		Objects.requireNonNull(provisioner, "provisioner");
		return FormatterStep.createLazy(NAME,
				() -> new State(NAME, groupArtifact, version, style, provisioner),
				State::createFormat);
	}

	static final Jvm.Support<String> JVM_SUPPORT = Jvm.<String> support(NAME).add(8, "1.1.0").add(11, "2.10.0");

	public static String defaultGroupArtifact() {
		return MAVEN_COORDINATE;
	}

	/** Get default formatter version */
	public static String defaultVersion() {
		return JVM_SUPPORT.getRecommendedFormatterVersion();
	}

	public static String defaultStyle() {
		return DEFAULT_STYLE;
	}

	static final class State implements Serializable {
		private static final long serialVersionUID = 1L;

		/** The jar that contains the formatter. */
		final JarState jarState;
		final String stepName;
		final String version;
		final String style;

		State(String stepName, String version, Provisioner provisioner) throws Exception {
			this(stepName, version, DEFAULT_STYLE, provisioner);
		}

		State(String stepName, String version, String style, Provisioner provisioner) throws Exception {
			this(stepName, MAVEN_COORDINATE, version, style, provisioner);
		}

		State(String stepName, String groupArtifact, String version, String style, Provisioner provisioner) throws Exception {
			JVM_SUPPORT.assertFormatterSupported(version);
			this.jarState = JarState.from(groupArtifact + ":" + version, provisioner);
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
			Object formatter = formatterClazz.getMethod("createFormatter", optionsClass).invoke(null, options);
			Method formatterMethod = formatterClazz.getMethod(FORMATTER_METHOD, String.class);

			Function<String, String> removeUnused = constructRemoveUnusedFunction(classLoader);

			Class<?> importOrdererClass = classLoader.loadClass(IMPORT_ORDERER_CLASS);
			Method importOrdererMethod = importOrdererClass.getMethod(IMPORT_ORDERER_METHOD, String.class);

			return JVM_SUPPORT.suggestLaterVersionOnError(version, (input -> {
				String formatted = (String) formatterMethod.invoke(formatter, input);
				String removedUnused = removeUnused.apply(formatted);
				String sortedImports = (String) importOrdererMethod.invoke(null, removedUnused);
				return sortedImports;
			}));
		}

		FormatterFunc createRemoveUnusedImportsOnly() throws Exception {
			ClassLoader classLoader = jarState.getClassLoader();
			Function<String, String> removeUnused = constructRemoveUnusedFunction(classLoader);
			return JVM_SUPPORT.suggestLaterVersionOnError(version, removeUnused::apply);
		}

		private static Function<String, String> constructRemoveUnusedFunction(ClassLoader classLoader)
				throws NoSuchMethodException, ClassNotFoundException {
			Class<?> removeUnusedClass = classLoader.loadClass(REMOVE_UNUSED_CLASS);
			Method removeUnusedMethod = removeUnusedClass.getMethod(REMOVE_UNUSED_METHOD, String.class);
			return (x) -> (String) removeUnusedMethod.invoke(null, x);
		}
	}
}
