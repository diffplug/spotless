/*
 * Copyright 2022-2025 DiffPlug
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

import static java.util.Objects.requireNonNull;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

public final class GsonStep implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;
	private static final String MAVEN_COORDINATES = "com.google.code.gson:gson";
	private static final String INCOMPATIBLE_ERROR_MESSAGE = "There was a problem interacting with Gson; maybe you set an incompatible version?";
	public static final String NAME = "gson";
	public static final String DEFAULT_VERSION = "2.13.2";

	private final JarState.Promised jarState;
	private final GsonConfig gsonConfig;

	private GsonStep(JarState.Promised jarState, GsonConfig gsonConfig) {
		this.gsonConfig = gsonConfig;
		this.jarState = jarState;
	}

	public static FormatterStep create(GsonConfig gsonConfig, Provisioner provisioner) {
		requireNonNull(provisioner, "provisioner cannot be null");
		return FormatterStep.create(NAME,
				new GsonStep(JarState.promise(() -> JarState.from(MAVEN_COORDINATES + ":" + gsonConfig.getVersion(), provisioner)), gsonConfig),
				GsonStep::equalityState,
				State::toFormatter);
	}

	private State equalityState() {
		return new State(jarState.get(), gsonConfig);
	}

	private static final class State implements Serializable {
		@Serial
		private static final long serialVersionUID = -3240568265160440420L;

		private final JarState jarState;
		private final GsonConfig gsonConfig;

		private State(JarState jarState, GsonConfig gsonConfig) {
			this.jarState = jarState;
			this.gsonConfig = gsonConfig;
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
