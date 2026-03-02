/*
 * Copyright 2025-2026 DiffPlug
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

import static com.diffplug.spotless.JarState.from;
import static com.diffplug.spotless.JarState.promise;
import static java.util.List.copyOf;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

public final class ExpandWildcardImportsStep implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	private static final String INCOMPATIBLE_ERROR_MESSAGE = "There was a problem interacting with Java-Parser; maybe you set an incompatible version?";
	private static final String MAVEN_COORDINATES = "com.github.javaparser:javaparser-symbol-solver-core:3.27.1";

	private final Collection<File> typeSolverClasspath;
	private final JarState.Promised jarState;

	private ExpandWildcardImportsStep(Collection<File> typeSolverClasspath, JarState.Promised jarState) {
		this.typeSolverClasspath = new ArrayList<>(typeSolverClasspath); // Create defensive copy of the classpath.
		this.jarState = jarState;
	}

	public static FormatterStep create(Set<File> typeSolverClasspath, Provisioner provisioner) {
		requireNonNull(provisioner);
		requireNonNull(typeSolverClasspath);
		return FormatterStep.create("expandwildcardimports",
				new ExpandWildcardImportsStep(
						new ArrayList<>(typeSolverClasspath), // Create a defensive copy of the classpath.
						promise(() -> from(MAVEN_COORDINATES, provisioner))),
				ExpandWildcardImportsStep::equalityState,
				State::toFormatter);
	}

	private State equalityState() {
		return new State(typeSolverClasspath, jarState.get());
	}

	private record State(Collection<File> typeSolverClasspath, JarState jarState) implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private State(Collection<File> typeSolverClasspath, JarState jarState) {
				this.typeSolverClasspath = copyOf(typeSolverClasspath); // Create immutable copy.
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
}}
