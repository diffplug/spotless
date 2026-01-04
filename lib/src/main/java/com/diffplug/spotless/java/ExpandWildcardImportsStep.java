/*
 * Copyright 2025 DiffPlug
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

import java.io.File;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

public final class ExpandWildcardImportsStep implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	private static final String INCOMPATIBLE_ERROR_MESSAGE = "There was a problem interacting with Java-Parser; maybe you set an incompatible version?";
	private static final String MAVEN_COORDINATES = "com.github.javaparser:javaparser-symbol-solver-core";
	public static final String DEFAULT_VERSION = "3.27.1";

	private final Collection<File> typeSolverClasspath;
	private final JarState.Promised jarState;

	private ExpandWildcardImportsStep(Collection<File> typeSolverClasspath, JarState.Promised jarState) {
		this.typeSolverClasspath = typeSolverClasspath;
		this.jarState = jarState;
	}

	public static FormatterStep create(Set<File> typeSolverClasspath, Provisioner provisioner) {
		Objects.requireNonNull(provisioner, "provisioner cannot be null");
		return FormatterStep.create("expandwildcardimports",
				new ExpandWildcardImportsStep(typeSolverClasspath,
						JarState.promise(() -> JarState.from(MAVEN_COORDINATES + ":" + DEFAULT_VERSION, provisioner))),
				ExpandWildcardImportsStep::equalityState,
				State::toFormatter);
	}

	private State equalityState() {
		return new State(typeSolverClasspath, jarState.get());
	}

	private static class State implements Serializable {
		@Serial
		private static final long serialVersionUID = 1L;

		private final Collection<File> typeSolverClasspath;
		private final JarState jarState;

		public State(Collection<File> typeSolverClasspath, JarState jarState) {
			this.typeSolverClasspath = typeSolverClasspath;
			this.jarState = jarState;
		}

		FormatterFunc toFormatter() {
			try {
				return (FormatterFunc) jarState
						.getClassLoader()
						.loadClass("com.diffplug.spotless.glue.javaparser.ExpandWildcardsFormatterFunc")
						.getConstructor(Collection.class)
						.newInstance(typeSolverClasspath);
			} catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException
					| InstantiationException | IllegalAccessException | NoClassDefFoundError cause) {
				throw new IllegalStateException(INCOMPATIBLE_ERROR_MESSAGE, cause);
			}
		}

	}

}
