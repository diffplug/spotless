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

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

/**
 * Formats {@code @TableTest} annotation tables in Java and Kotlin source files.
 * Configuration is read from {@code .editorconfig} files.
 */
public final class TableTestFormatterStep implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;
	private static final String NAME = "tableTestFormatter";
	private static final String MAVEN_COORDINATE = "org.tabletest:tabletest-formatter-core:";
	private static final String DEFAULT_VERSION = "1.1.1";

	private final JarState.Promised jarState;
	private final String version;

	private TableTestFormatterStep(JarState.Promised jarState, String version) {
		this.jarState = jarState;
		this.version = version;
	}

	/** Creates a step which formats {@code @TableTest} tables using the default version. */
	public static FormatterStep create(Provisioner provisioner) {
		return create(defaultVersion(), provisioner);
	}

	/** Creates a step which formats {@code @TableTest} tables using the given version. */
	public static FormatterStep create(String version, Provisioner provisioner) {
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(provisioner, "provisioner");
		return FormatterStep.create(NAME,
				new TableTestFormatterStep(JarState.promise(() -> JarState.from(MAVEN_COORDINATE + version, provisioner)), version),
				TableTestFormatterStep::equalityState,
				State::createFormat);
	}

	/** Get default formatter version. */
	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	private State equalityState() {
		return new State(jarState.get(), version);
	}

	private static final class State implements Serializable {
		@Serial
		private static final long serialVersionUID = 1L;

		private final JarState jarState;
		private final String version;

		State(JarState jarState, String version) {
			this.jarState = jarState;
			this.version = version;
		}

		FormatterFunc createFormat() throws Exception {
			ClassLoader classLoader = jarState.getClassLoader();
			Class<?> formatterClazz = classLoader.loadClass("com.diffplug.spotless.glue.java.TableTestFormatterFunc");
			Constructor<?> constructor = formatterClazz.getConstructor();
			return (FormatterFunc.NeedsFile) constructor.newInstance();
		}
	}
}
