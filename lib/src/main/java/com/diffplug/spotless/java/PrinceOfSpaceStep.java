/*
 * Copyright 2026 DiffPlug
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

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.Objects;

import javax.annotation.Nullable;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Jvm;
import com.diffplug.spotless.Provisioner;

/** Wraps up <a href="https://github.com/agustafson/prince-of-space">prince-of-space</a> as a FormatterStep. */
public final class PrinceOfSpaceStep implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;
	private static final String NAME = "prince-of-space";
	public static final String MAVEN_COORDINATE = "io.github.agustafson.princeofspace:prince-of-space-core:";
	public static final String DEFAULT_VERSION = "2.2.0";

	/** The jar that contains the formatter. */
	private final JarState.Promised jarState;
	/** Version of the formatter jar. */
	private final String formatterVersion;
	@Nullable private final Options options;

	private PrinceOfSpaceStep(JarState.Promised jarState, String formatterVersion, @Nullable Options options) {
		this.jarState = jarState;
		this.formatterVersion = formatterVersion;
		this.options = options;
	}

	/** Creates a step which formats Java source with prince-of-space's default options. */
	public static FormatterStep create(Provisioner provisioner) {
		return create(defaultVersion(), provisioner);
	}

	/** Creates a step which formats Java source with prince-of-space's default options. */
	public static FormatterStep create(String version, Provisioner provisioner) {
		return create(version, provisioner, null);
	}

	/** Creates a step which formats Java source with the given options. */
	public static FormatterStep create(String version, Provisioner provisioner, @Nullable Options options) {
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(provisioner, "provisioner");
		return FormatterStep.create(NAME,
				new PrinceOfSpaceStep(JarState.promise(() -> JarState.from(MAVEN_COORDINATE + version, provisioner)), version, options),
				PrinceOfSpaceStep::equalityState,
				State::createFormat);
	}

	/** Get default formatter version */
	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	private State equalityState() {
		return new State(jarState.get(), formatterVersion, options);
	}

	/** Mutable bag of the optional prince-of-space {@code FormatterConfig} knobs; unset (null) fields keep prince-of-space's own defaults. */
	public static class Options implements Serializable {
		@Serial
		private static final long serialVersionUID = 1L;

		@Nullable private String indentStyle;
		@Nullable private Integer indentSize;
		@Nullable private Integer lineLength;
		@Nullable private String wrapStyle;
		@Nullable private Boolean closingParenOnNewLine;
		@Nullable private Boolean trailingCommas;
		@Nullable private Integer javaLanguageLevel;

		public Options() {}

		public void setIndentStyle(String indentStyle) {
			this.indentStyle = indentStyle;
		}

		public void setIndentSize(int indentSize) {
			this.indentSize = indentSize;
		}

		public void setLineLength(int lineLength) {
			this.lineLength = lineLength;
		}

		public void setWrapStyle(String wrapStyle) {
			this.wrapStyle = wrapStyle;
		}

		public void setClosingParenOnNewLine(boolean closingParenOnNewLine) {
			this.closingParenOnNewLine = closingParenOnNewLine;
		}

		public void setTrailingCommas(boolean trailingCommas) {
			this.trailingCommas = trailingCommas;
		}

		public void setJavaLanguageLevel(int javaLanguageLevel) {
			this.javaLanguageLevel = javaLanguageLevel;
		}
	}

	private static final class State implements Serializable {
		@Serial
		private static final long serialVersionUID = 1L;

		private final JarState jarState;
		private final String formatterVersion;
		@Nullable private final Options options;

		State(JarState jarState, String formatterVersion, @Nullable Options options) {
			if (Jvm.version() < 17) {
				throw new IllegalStateException("prince-of-space requires a JDK 17+ host runtime, this JVM is " + Jvm.version());
			}
			this.jarState = jarState;
			this.formatterVersion = formatterVersion;
			this.options = options;
		}

		FormatterFunc createFormat() throws Exception {
			final ClassLoader classLoader = jarState.getClassLoader();
			final Class<?> formatterFunc = classLoader.loadClass("com.diffplug.spotless.glue.pos.PrinceOfSpaceFormatterFunc");
			final Constructor<?> constructor = formatterFunc.getConstructor(
					String.class, Integer.class, Integer.class, String.class, Boolean.class, Boolean.class, Integer.class);
			if (options == null) {
				return (FormatterFunc) constructor.newInstance(null, null, null, null, null, null, null);
			}
			return (FormatterFunc) constructor.newInstance(
					options.indentStyle,
					options.indentSize,
					options.lineLength,
					options.wrapStyle,
					options.closingParenOnNewLine,
					options.trailingCommas,
					options.javaLanguageLevel);
		}
	}
}
