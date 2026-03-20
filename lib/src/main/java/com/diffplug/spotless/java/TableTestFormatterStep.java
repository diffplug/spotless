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
import java.util.Locale;
import java.util.Objects;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

/**
 * Formats {@code @TableTest} annotation tables in Java and Kotlin source files.
 * <p>
 * Configuration is read from {@code .editorconfig} files. When no editorconfig is found
 * or the lookup fails, the configured fallback indent style and size are used.
 * If no fallback is configured, defaults matching {@code Config.SPACES_4} and
 * {@code Config.NO_INDENT} are applied.
 */
public final class TableTestFormatterStep implements Serializable {
	@Serial
	private static final long serialVersionUID = 2L;
	private static final String NAME = "tableTestFormatter";
	private static final String MAVEN_COORDINATE = "org.tabletest:tabletest-formatter-core:";
	private static final String DEFAULT_VERSION = "1.1.1";

	/** Default fallback indent style ({@code "space"}) for Java/Kotlin files. */
	public static final String DEFAULT_INDENT_STYLE = "space";
	/** Default fallback indent size ({@code 4}) for Java/Kotlin files, matching {@code Config.SPACES_4}. */
	public static final int DEFAULT_INDENT_SIZE = 4;

	private final JarState.Promised jarState;
	private final String version;
	private final String indentStyle;
	private final int indentSize;

	private TableTestFormatterStep(JarState.Promised jarState, String version, String indentStyle, int indentSize) {
		this.jarState = jarState;
		this.version = version;
		this.indentStyle = validateIndentStyle(indentStyle);
		this.indentSize = validateIndentSize(indentSize);
	}

	/** Creates a step which formats {@code @TableTest} tables using the default version and indent config. */
	public static FormatterStep create(Provisioner provisioner) {
		return create(defaultVersion(), provisioner);
	}

	/** Creates a step which formats {@code @TableTest} tables using the given version and default indent config. */
	public static FormatterStep create(String version, Provisioner provisioner) {
		return create(version, provisioner, DEFAULT_INDENT_STYLE, DEFAULT_INDENT_SIZE);
	}

	/**
	 * Creates a step which formats {@code @TableTest} tables using the given version and fallback indent config.
	 * <p>
	 * The fallback config is used when no {@code .editorconfig} is found or the lookup fails.
	 *
	 * @param version     the tabletest-formatter-core version
	 * @param provisioner the jar provisioner
	 * @param indentStyle fallback indent style: {@code "space"} or {@code "tab"} (case-insensitive)
	 * @param indentSize  fallback indent size (must be &gt;= 0)
	 */
	public static FormatterStep create(String version, Provisioner provisioner, String indentStyle, int indentSize) {
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(provisioner, "provisioner");
		return FormatterStep.create(NAME,
				new TableTestFormatterStep(JarState.promise(() -> JarState.from(MAVEN_COORDINATE + version, provisioner)), version, indentStyle, indentSize),
				TableTestFormatterStep::equalityState,
				State::createFormat);
	}

	/** Get default formatter version. */
	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	private State equalityState() {
		return new State(jarState.get(), version, indentStyle, indentSize);
	}

	public static String validateIndentStyle(String indentStyle) {
		Objects.requireNonNull(indentStyle, "indentStyle");
		String lower = indentStyle.toLowerCase(Locale.ROOT);
		if (!lower.equals("space") && !lower.equals("tab")) {
			throw new IllegalArgumentException("indentStyle must be 'space' or 'tab', got: " + indentStyle);
		}
		return lower;
	}

	public static int validateIndentSize(int indentSize) {
		if (indentSize < 0) {
			throw new IllegalArgumentException("indentSize must be >= 0, got: " + indentSize);
		}
		return indentSize;
	}

	private static final class State implements Serializable {
		@Serial
		private static final long serialVersionUID = 2L;

		private final JarState jarState;
		private final String version;
		private final String indentStyle;
		private final int indentSize;

		State(JarState jarState, String version, String indentStyle, int indentSize) {
			this.jarState = jarState;
			this.version = version;
			this.indentStyle = indentStyle;
			this.indentSize = indentSize;
		}

		FormatterFunc createFormat() throws Exception {
			ClassLoader classLoader = jarState.getClassLoader();
			Class<?> formatterClazz = classLoader.loadClass("com.diffplug.spotless.glue.java.TableTestFormatterFunc");
			Constructor<?> constructor = formatterClazz.getConstructor(String.class, int.class);
			return (FormatterFunc.NeedsFile) constructor.newInstance(indentStyle, indentSize);
		}
	}
}
