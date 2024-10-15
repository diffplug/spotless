/*
 * Copyright 2024 DiffPlug
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
package com.diffplug.spotless.rdf;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

public class RdfFormatterStep implements Serializable {
	public static final String LATEST_TURTLE_FORMATTER_VERSION = "1.2.13";
	private static final long serialVersionUID = 1L;

	private static final String TURTLE_FORMATTER_COORDINATES = "de.atextor:turtle-formatter";

	private final JarState.Promised jarState;
	private final Map<String, String> turtleFormatterStyle;
	private final RdfFormatterConfig config;

	public static FormatterStep create(RdfFormatterConfig config, Map<String, String> turtleOptions, Provisioner provisioner)
			throws ClassNotFoundException {
		JarState.Promised jarState;
		jarState = JarState.promise(() -> JarState.from(TURTLE_FORMATTER_COORDINATES + ":" + config.getTurtleFormatterVersion(), provisioner));
		RdfFormatterStep step = new RdfFormatterStep(jarState, config, turtleOptions);
		return FormatterStep.create("RdfFormatter", step, RdfFormatterStep::state, RdfFormatterStep::formatterFunc);
	}

	public static State state(RdfFormatterStep step) {
		return new State(step.config, step.turtleFormatterStyle, step.jarState.get());
	}

	public static RdfFormatterFunc formatterFunc(State state)
			throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		return new RdfFormatterFunc(state);
	}

	public RdfFormatterStep(JarState.Promised jarState, RdfFormatterConfig config,
			Map<String, String> turtleFormatterStyle) {
		this.jarState = jarState;
		this.turtleFormatterStyle = turtleFormatterStyle;
		this.config = config;
	}

	public static class State implements Serializable {
		private static final long serialVersionUID = 1L;

		private final RdfFormatterConfig config;

		private final Map<String, String> turtleFormatterStyle;

		private final JarState jarState;

		public State(RdfFormatterConfig config, Map<String, String> turtleFormatterStyle,
				JarState jarState) {
			this.config = config;
			this.turtleFormatterStyle = new TreeMap<>(turtleFormatterStyle == null ? Map.of() : turtleFormatterStyle);
			this.jarState = jarState;
		}

		public RdfFormatterConfig getConfig() {
			return config;
		}

		public Map<String, String> getTurtleFormatterStyle() {
			return turtleFormatterStyle;
		}

		public JarState getJarState() {
			return jarState;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof State))
				return false;
			State state = (State) o;
			return Objects.equals(getConfig(), state.getConfig()) && Objects.equals(
					getTurtleFormatterStyle(), state.getTurtleFormatterStyle())
					&& Objects.equals(
							getJarState(), state.getJarState());
		}

		@Override
		public int hashCode() {
			return Objects.hash(getConfig(), getTurtleFormatterStyle(), getJarState());
		}
	}

}
