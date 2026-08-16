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
import static java.util.Objects.requireNonNull;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

/**
 * Replaces fully qualified type names with simple names and adds the necessary imports.
 * Uses JavaParser to identify type references in the AST, avoiding false positives
 * in strings, comments, annotations, and other non-type contexts.
 *
 * <p>Designed to run before {@code importOrder()} and {@code removeUnusedImports()}.
 */
public final class ShortenFullyQualifiedTypesStep implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	private static final String NAME = "shortenFullyQualifiedTypes";
	private static final String INCOMPATIBLE_ERROR_MESSAGE = "There was a problem interacting with JavaParser; maybe you set an incompatible version?";
	private static final String MAVEN_COORDINATES = "com.github.javaparser:javaparser-core:3.27.1";

	private final JarState.Promised jarState;

	private ShortenFullyQualifiedTypesStep(JarState.Promised jarState) {
		this.jarState = jarState;
	}

	public static FormatterStep create(Provisioner provisioner) {
		requireNonNull(provisioner);
		return FormatterStep.create(NAME,
				new ShortenFullyQualifiedTypesStep(promise(() -> from(MAVEN_COORDINATES, provisioner))),
				ShortenFullyQualifiedTypesStep::equalityState,
				State::toFormatter);
	}

	private State equalityState() {
		return new State(jarState.get());
	}

	private record State(JarState jarState) implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	FormatterFunc toFormatter() {
		try {
			return (FormatterFunc) jarState
					.getClassLoader()
					.loadClass("com.diffplug.spotless.glue.javaparser.ShortenQualifiedTypesFormatterFunc")
					.getConstructor()
					.newInstance();
		} catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException
				| InstantiationException | IllegalAccessException | NoClassDefFoundError cause) {
			throw new IllegalStateException(INCOMPATIBLE_ERROR_MESSAGE, cause);
		}
	}
}}
