/*
 * Copyright 2016-2024 DiffPlug
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
package com.diffplug.spotless.markdown;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.Objects;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

/** A step for <a href="https://github.com/vsch/flexmark-java">flexmark-java</a>. */
public class FlexmarkStep implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final String DEFAULT_VERSION = "0.64.8";
	private static final String MAVEN_COORDINATE = "com.vladsch.flexmark:flexmark-all:";
	public static final String NAME = "flexmark-java";

	private final JarState.Promised jarState;

	private FlexmarkStep(JarState.Promised jarState) {
		this.jarState = jarState;
	}

	/** Creates a formatter step for the default version. */
	public static FormatterStep create(Provisioner provisioner) {
		return create(defaultVersion(), provisioner);
	}

	/** Creates a formatter step for the given version. */
	public static FormatterStep create(String version, Provisioner provisioner) {
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(provisioner, "provisioner");
		return FormatterStep.create(NAME,
				new FlexmarkStep(JarState.promise(() -> JarState.from(MAVEN_COORDINATE + version, provisioner))),
				FlexmarkStep::equalityState,
				State::createFormat);
	}

	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	private State equalityState() {
		return new State(jarState.get());
	}

	private static class State implements Serializable {
		private static final long serialVersionUID = 1L;

		private final JarState jarState;

		State(JarState jarState) {
			this.jarState = jarState;
		}

		FormatterFunc createFormat() throws Exception {
			final ClassLoader classLoader = jarState.getClassLoader();
			final Class<?> formatterFunc = classLoader.loadClass("com.diffplug.spotless.glue.markdown.FlexmarkFormatterFunc");
			final Constructor<?> constructor = formatterFunc.getConstructor();
			return (FormatterFunc) constructor.newInstance();
		}
	}
}
