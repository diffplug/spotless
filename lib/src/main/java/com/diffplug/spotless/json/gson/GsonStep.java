/*
 * Copyright 2022-2023 DiffPlug
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
package com.diffplug.spotless.json.gson;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

public class GsonStep {
	private static final String MAVEN_COORDINATES = "com.google.code.gson:gson";
	private static final String INCOMPATIBLE_ERROR_MESSAGE = "There was a problem interacting with Gson; maybe you set an incompatible version?";

	@Deprecated
	public static FormatterStep create(int indentSpaces, boolean sortByKeys, boolean escapeHtml, String version, Provisioner provisioner) {
		return create(new GsonConfig(sortByKeys, escapeHtml, indentSpaces, version), provisioner);
	}

	public static FormatterStep create(GsonConfig gsonConfig, Provisioner provisioner) {
		Objects.requireNonNull(provisioner, "provisioner cannot be null");
		return FormatterStep.createLazy("gson", () -> new State(gsonConfig, provisioner), State::toFormatter);
	}

	private static final class State implements Serializable {
		private static final long serialVersionUID = -3240568265160440420L;

		private final JarState jarState;
		private final GsonConfig gsonConfig;

		private State(GsonConfig gsonConfig, Provisioner provisioner) throws IOException {
			this.gsonConfig = gsonConfig;
			this.jarState = JarState.from(MAVEN_COORDINATES + ":" + gsonConfig.getVersion(), provisioner);
		}

		FormatterFunc toFormatter() {
			try {
				Class<?> formatterFunc = jarState.getClassLoader().loadClass("com.diffplug.spotless.glue.gson.GsonFormatterFunc");
				Constructor<?> constructor = formatterFunc.getConstructor(GsonConfig.class);
				return (FormatterFunc) constructor.newInstance(gsonConfig);
			} catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException
					| InstantiationException | IllegalAccessException | NoClassDefFoundError cause) {
				throw new IllegalStateException(INCOMPATIBLE_ERROR_MESSAGE, cause);
			}
		}
	}

}
