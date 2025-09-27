/*
 * Copyright 2023-2025 DiffPlug
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
package com.diffplug.spotless.json;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

public final class JsonPatchStep implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;
	private static final String MAVEN_COORDINATE = "com.flipkart.zjsonpatch:zjsonpatch";
	private static final String DEFAULT_VERSION = "0.4.16";
	public static final String NAME = "apply-json-patch";

	private final JarState.Promised jarState;
	@Nullable private final List<Map<String, Object>> patch;
	@Nullable private final String patchString;

	private JsonPatchStep(JarState.Promised jarState,
			@Nullable String patchString,
			@Nullable List<Map<String, Object>> patch) {
		this.jarState = jarState;
		this.patchString = patchString;
		this.patch = patch;
	}

	public static FormatterStep create(String patchString, Provisioner provisioner) {
		return create(DEFAULT_VERSION, patchString, provisioner);
	}

	public static FormatterStep create(String zjsonPatchVersion, String patchString, Provisioner provisioner) {
		Objects.requireNonNull(zjsonPatchVersion, "zjsonPatchVersion cannot be null");
		Objects.requireNonNull(patchString, "patchString cannot be null");
		Objects.requireNonNull(provisioner, "provisioner cannot be null");
		return FormatterStep.create(NAME,
				new JsonPatchStep(JarState.promise(() -> JarState.from(MAVEN_COORDINATE + ":" + zjsonPatchVersion, provisioner)), patchString, null),
				JsonPatchStep::equalityState,
				State::toFormatter);
	}

	public static FormatterStep create(List<Map<String, Object>> patch, Provisioner provisioner) {
		return create(DEFAULT_VERSION, patch, provisioner);
	}

	public static FormatterStep create(String zjsonPatchVersion, List<Map<String, Object>> patch, Provisioner provisioner) {
		Objects.requireNonNull(zjsonPatchVersion, "zjsonPatchVersion cannot be null");
		Objects.requireNonNull(patch, "patch cannot be null");
		Objects.requireNonNull(provisioner, "provisioner cannot be null");
		return FormatterStep.create(NAME,
				new JsonPatchStep(JarState.promise(() -> JarState.from(MAVEN_COORDINATE + ":" + zjsonPatchVersion, provisioner)), null, patch),
				JsonPatchStep::equalityState,
				State::toFormatter);
	}

	private State equalityState() {
		return new State(jarState.get(), patchString, patch);
	}

	static final class State implements Serializable {
		@Serial
		private static final long serialVersionUID = 1L;

		private final JarState jarState;
		@Nullable private final List<Map<String, Object>> patch;
		@Nullable private final String patchString;

		State(JarState jarState,
				@Nullable String patchString,
				@Nullable List<Map<String, Object>> patch) {
			this.jarState = jarState;
			this.patchString = patchString;
			this.patch = patch;
		}

		FormatterFunc toFormatter() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
			Class<?> formatterFunc = jarState.getClassLoader().loadClass("com.diffplug.spotless.glue.json.JsonPatchFormatterFunc");
			if (this.patch != null) {
				Constructor<?> constructor = formatterFunc.getConstructor(List.class);
				return (FormatterFunc) constructor.newInstance(patch);
			} else {
				Constructor<?> constructor = formatterFunc.getConstructor(String.class);
				return (FormatterFunc) constructor.newInstance(patchString);
			}
		}
	}
}
