/*
 * Copyright 2016-2025 DiffPlug
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
import java.lang.reflect.Constructor;
import java.util.Objects;

import com.diffplug.spotless.*;

/** Wraps up <a href="https://github.com/palantir/palantir-java-format">palantir-java-format</a> fork of
 * <a href="https://github.com/google/google-java-format">google-java-format</a> as a FormatterStep. */
public class PalantirJavaFormatStep implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final boolean DEFAULT_FORMAT_JAVADOC = false;
	private static final String DEFAULT_STYLE = "PALANTIR";
	private static final String NAME = "palantir-java-format";
	public static final String MAVEN_COORDINATE = "com.palantir.javaformat:palantir-java-format:";
	private static final Jvm.Support<String> JVM_SUPPORT = Jvm.<String> support(NAME).add(8, "1.1.0").add(11, "2.28.0").add(21, "2.71.0");

	/** The jar that contains the formatter. */
	private final JarState.Promised jarState;
	/** Version of the formatter jar. */
	private final String formatterVersion;
	private final String style;
	/** Whether to format Java docs. */
	private final boolean formatJavadoc;

	private PalantirJavaFormatStep(JarState.Promised jarState,
			String formatterVersion,
			String style,
			boolean formatJavadoc) {
		this.jarState = jarState;
		this.formatterVersion = formatterVersion;
		this.style = style;
		this.formatJavadoc = formatJavadoc;
	}

	/** Creates a step which formats everything - code, import order, and unused imports. */
	public static FormatterStep create(Provisioner provisioner) {
		return create(defaultVersion(), provisioner);
	}

	/** Creates a step which formats everything - code, import order, and unused imports. */
	public static FormatterStep create(String version, Provisioner provisioner) {
		return create(version, defaultStyle(), provisioner);
	}

	/**
	 * Creates a step which formats code, import order, and unused imports, but not Java docs. And with the given format
	 * style.
	 */
	public static FormatterStep create(String version, String style, Provisioner provisioner) {
		return create(version, style, DEFAULT_FORMAT_JAVADOC, provisioner);
	}

	/**
	 * Creates a step which formats everything - code, import order, unused imports, and Java docs. And with the given
	 * format style.
	 */
	public static FormatterStep create(String version, String style, boolean formatJavadoc, Provisioner provisioner) {
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(style, "style");
		Objects.requireNonNull(provisioner, "provisioner");

		return FormatterStep.create(NAME,
				new PalantirJavaFormatStep(JarState.promise(() -> JarState.from(MAVEN_COORDINATE + version, provisioner)), version, style, formatJavadoc),
				PalantirJavaFormatStep::equalityState,
				State::createFormat);
	}

	/** Get default formatter version */
	public static String defaultVersion() {
		return JVM_SUPPORT.getRecommendedFormatterVersion();
	}

	/** Get default style */
	public static String defaultStyle() {
		return DEFAULT_STYLE;
	}

	/** Get default for whether Java docs should be formatted */
	public static boolean defaultFormatJavadoc() {
		return DEFAULT_FORMAT_JAVADOC;
	}

	private State equalityState() {
		return new State(jarState.get(), formatterVersion, style, formatJavadoc);
	}

	private static final class State implements Serializable {
		private static final long serialVersionUID = 1L;

		private final JarState jarState;
		private final String formatterVersion;
		private final String style;
		private final boolean formatJavadoc;

		State(JarState jarState, String formatterVersion, String style, boolean formatJavadoc) {
			ModuleHelper.doOpenInternalPackagesIfRequired();
			this.jarState = jarState;
			this.formatterVersion = formatterVersion;
			this.style = style;
			this.formatJavadoc = formatJavadoc;
		}

		FormatterFunc createFormat() throws Exception {
			final ClassLoader classLoader = jarState.getClassLoader();
			final Class<?> formatterFunc = classLoader.loadClass("com.diffplug.spotless.glue.pjf.PalantirJavaFormatFormatterFunc");
			// 1st arg is "style", 2nd arg is "formatJavadoc"
			final Constructor<?> constructor = formatterFunc.getConstructor(String.class, boolean.class);
			return JVM_SUPPORT.suggestLaterVersionOnError(formatterVersion, (FormatterFunc) constructor.newInstance(style, formatJavadoc));
		}
	}
}
