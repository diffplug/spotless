/*
 * Copyright 2021-2024 DiffPlug
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
package com.diffplug.spotless.generic;

import java.io.Serializable;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

public final class Jsr223Step implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	@Nullable
	private final JarState.Promised jarState;
	private final String engine;
	private final String script;

	private Jsr223Step(@Nullable JarState.Promised jarState, String engine, String script) {
		this.jarState = jarState;
		this.engine = engine;
		this.script = script;
	}

	public static FormatterStep create(String name, @Nullable String dependency, CharSequence engine, CharSequence script, Provisioner provisioner) {
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(engine, "engine");
		Objects.requireNonNull(script, "script");
		return FormatterStep.create(name,
				new Jsr223Step(dependency == null ? null : JarState.promise(() -> JarState.from(dependency, provisioner)), engine.toString(), script.toString()),
				Jsr223Step::equalityState,
				State::toFormatter);
	}

	private State equalityState() {
		return new State(jarState == null ? null : jarState.get(), engine, script);
	}

	private static final class State implements Serializable {
		private static final long serialVersionUID = 1L;

		@Nullable
		private final JarState jarState;
		private final String engine;
		private final String script;

		State(@Nullable JarState jarState, CharSequence engine, CharSequence script) {
			this.jarState = jarState;
			this.engine = engine.toString();
			this.script = script.toString();
		}

		FormatterFunc toFormatter() {
			ScriptEngineManager scriptEngineManager;
			if (jarState == null) {
				scriptEngineManager = new ScriptEngineManager(ClassLoader.getSystemClassLoader());
			} else {
				scriptEngineManager = new ScriptEngineManager(jarState.getClassLoader());
			}
			ScriptEngine scriptEngine = scriptEngineManager.getEngineByName(engine);

			if (scriptEngine == null) {
				throw new IllegalArgumentException("Unknown script engine '" + engine + "'. Available engines: " +
						scriptEngineManager.getEngineFactories().stream().flatMap(f -> f.getNames().stream()).collect(Collectors.joining(", ")));
			}

			// evaluate script code
			return raw -> {
				scriptEngine.put("source", raw);
				return (String) scriptEngine.eval(script);
			};
		}
	}
}
