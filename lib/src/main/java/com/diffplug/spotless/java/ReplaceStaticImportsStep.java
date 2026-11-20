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

import static com.diffplug.spotless.JarState.from;
import static com.diffplug.spotless.JarState.promise;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Replaces static imports with regular imports and qualified references, so that
 * {@code assertTrue(x)} becomes {@code Assertions.assertTrue(x)}.
 *
 * <p>Uses JavaParser and the project classpath to work out what each name refers to. A static
 * import is only replaced when every use of it can be resolved; otherwise it is left as it is.
 *
 * <p>Designed to run before {@code importOrder()} and {@code removeUnusedImports()}.
 */
public final class ReplaceStaticImportsStep implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	private static final String NAME = "replaceStaticImports";
	private static final String INCOMPATIBLE_ERROR_MESSAGE = "There was a problem interacting with JavaParser; maybe you set an incompatible version?";
	private static final String MAVEN_COORDINATES = "com.github.javaparser:javaparser-symbol-solver-core:" + BuildConfig.VERSION_JAVAPARSER;

	private final List<File> typeSolverClasspath;
	private final List<String> excludes;
	private final JarState.Promised jarState;

	private ReplaceStaticImportsStep(List<File> typeSolverClasspath, List<String> excludes, JarState.Promised jarState) {
		this.typeSolverClasspath = typeSolverClasspath;
		this.excludes = excludes;
		this.jarState = jarState;
	}

	/**
	 * @param typeSolverClasspath source directories and jars used to resolve names, normally the
	 *        source directories and compile classpath of the project
	 */
	public static FormatterStep create(Set<File> typeSolverClasspath, Provisioner provisioner) {
		return create(typeSolverClasspath, List.of(), provisioner);
	}

	/**
	 * @param typeSolverClasspath source directories and jars used to resolve names, normally the
	 *        source directories and compile classpath of the project
	 * @param excludes static imports to keep: {@code org.assertj.core.api.Assertions.*}, or just the type name, keeps
	 *        every static import from that class; {@code java.util.concurrent.TimeUnit.SECONDS} keeps the static import of
	 *        that member, and an on-demand static import of its class if the file uses the member through it
	 */
	public static FormatterStep create(Set<File> typeSolverClasspath, Collection<String> excludes, Provisioner provisioner) {
		requireNonNull(typeSolverClasspath);
		requireNonNull(excludes);
		requireNonNull(provisioner);
		Set<String> sortedExcludes = new TreeSet<>();
		for (String exclude : excludes) {
			sortedExcludes.add(requireNonNull(exclude).trim());
		}
		return FormatterStep.create(NAME,
				new ReplaceStaticImportsStep(List.copyOf(typeSolverClasspath), List.copyOf(sortedExcludes), promise(() -> from(MAVEN_COORDINATES, provisioner))),
				ReplaceStaticImportsStep::equalityState,
				State::toFormatter);
	}

	private State equalityState() throws IOException {
		List<File> jars = typeSolverClasspath.stream().filter(file -> file.isFile() && file.getName().endsWith(".jar")).toList();
		return new State(typeSolverClasspath, FileSignature.signAsList(jars), excludes, jarState.get());
	}

	/**
	 * Equality depends on the content of the jars, the excludes, the JavaParser version and the JDK version. The
	 * paths are transient, so that build cache keys do not depend on where the project is.
	 */
	private static final class State implements Serializable {
		@Serial
		private static final long serialVersionUID = 1L;

		/** Only used to create the formatter: the equality state is never deserialized, and the paths must not affect equality. */
		@SuppressFBWarnings(value = "SE_TRANSIENT_FIELD_NOT_RESTORED", justification = "paths are kept out of the equality check, like FileSignature does")
		private final transient List<File> typeSolverClasspath;
		@SuppressWarnings("unused")
		private final FileSignature jars;
		private final List<String> excludes;
		private final JarState jarState;
		/** The JDK classes are those of the JDK running the build, which can make a difference. */
		@SuppressWarnings("unused")
		private final int jdkVersion = Runtime.version().feature();

		State(List<File> typeSolverClasspath, FileSignature jars, List<String> excludes, JarState jarState) {
			this.typeSolverClasspath = typeSolverClasspath;
			this.jars = jars;
			this.excludes = excludes;
			this.jarState = jarState;
		}

		FormatterFunc toFormatter() {
			try {
				return (FormatterFunc) jarState
						.getClassLoader()
						.loadClass("com.diffplug.spotless.glue.javaparser.ReplaceStaticImportsFormatterFunc")
						.getConstructor(Collection.class, Collection.class)
						.newInstance(typeSolverClasspath, excludes);
			} catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException
					| InstantiationException | IllegalAccessException | NoClassDefFoundError cause) {
				throw new IllegalStateException(INCOMPATIBLE_ERROR_MESSAGE, cause);
			}
		}
	}
}
