/*
 * Copyright 2023 DiffPlug
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

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

public class JsonPatchStep {
	// https://mvnrepository.com/artifact/com.flipkart.zjsonpatch/zjsonpatch
	static final String MAVEN_COORDINATE = "com.flipkart.zjsonpatch:zjsonpatch";
	static final String DEFAULT_VERSION = "0.4.14";

	private JsonPatchStep() {}

	public static FormatterStep create(String patchString, Provisioner provisioner) {
		return create(DEFAULT_VERSION, patchString, provisioner);
	}

	public static FormatterStep create(String zjsonPatchVersion, String patchString, Provisioner provisioner) {
		Objects.requireNonNull(zjsonPatchVersion, "zjsonPatchVersion cannot be null");
		Objects.requireNonNull(patchString, "patchString cannot be null");
		Objects.requireNonNull(provisioner, "provisioner cannot be null");
		return FormatterStep.createLazy("apply-json-patch", () -> new State(zjsonPatchVersion, patchString, provisioner), State::toFormatter);
	}

	public static FormatterStep create(List<Map<String, Object>> patch, Provisioner provisioner) {
		return create(DEFAULT_VERSION, patch, provisioner);
	}

	public static FormatterStep create(String zjsonPatchVersion, List<Map<String, Object>> patch, Provisioner provisioner) {
		Objects.requireNonNull(zjsonPatchVersion, "zjsonPatchVersion cannot be null");
		Objects.requireNonNull(patch, "patch cannot be null");
		Objects.requireNonNull(provisioner, "provisioner cannot be null");
		return FormatterStep.createLazy("apply-json-patch", () -> new State(zjsonPatchVersion, patch, provisioner), State::toFormatter);
	}

	static final class State implements Serializable {
		private static final long serialVersionUID = 1L;

		private final JarState jarState;
		private final List<Map<String, Object>> patch;
		private final String patchString;

		private State(String zjsonPatchVersion, List<Map<String, Object>> patch, Provisioner provisioner) throws IOException {
			this.jarState = JarState.from(MAVEN_COORDINATE + ":" + zjsonPatchVersion, provisioner);
			this.patch = patch;
			this.patchString = null;
		}

		private State(String zjsonPatchVersion, String patchString, Provisioner provisioner) throws IOException {
			this.jarState = JarState.from(MAVEN_COORDINATE + ":" + zjsonPatchVersion, provisioner);
			this.patch = null;
			this.patchString = patchString;
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
