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

import static com.diffplug.spotless.JarState.Promised;
import static com.diffplug.spotless.JarState.from;
import static com.diffplug.spotless.JarState.promise;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.Objects;

import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;

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

	private final Promised jarState;
	private final transient MutableDataSet options;

	private FlexmarkStep(
		final Promised jarState,
		final MutableDataSet options
	) {
		this.jarState = jarState;
		this.options = options;
	}

	/** Creates a formatter step for the custom version. */
	public static FormatterStep create(
		final String version,
		final Provisioner provisioner
	) {
		return FlexmarkStep.create(
			version,
			provisioner,
			new MutableDataSet()
		);
	}

	/** Creates a formatter step for the default version. */
	public static FormatterStep create(final Provisioner provisioner) {
		return FlexmarkStep.create(
			FlexmarkStep.defaultVersion(),
			provisioner,
			new MutableDataSet()
		);
	}

	/** Creates a formatter step for the default version with custom options. */
	public static FormatterStep create(
		final Provisioner provisioner,
		final MutableDataSet options
	) {
		return FlexmarkStep.create(
			FlexmarkStep.defaultVersion(),
			provisioner,
			options
		);
	}

	/** Creates a formatter step for the given version. */
	public static FormatterStep create(
		final String version,
		final Provisioner provisioner,
		final MutableDataSet options
	) {
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(provisioner, "provisioner");
		return FormatterStep.create(
			FlexmarkStep.NAME,
			new FlexmarkStep(
				promise(() -> from(FlexmarkStep.MAVEN_COORDINATE + version, provisioner)),
				options
			),
			FlexmarkStep::equalityState,
			State::createFormat
		);
	}

	public static String defaultVersion() {
		return FlexmarkStep.DEFAULT_VERSION;
	}

	private State equalityState() {
		return new State(this.jarState.get(), this.options);
	}

	private static class State implements Serializable {
		private static final long serialVersionUID = 1L;

		private final JarState jarState;

		private final MutableDataSet options;

		State(final JarState jarState, final MutableDataSet options) {
			this.jarState = jarState;
			this.options = options;
		}

		FormatterFunc createFormat() throws Exception {
			final ClassLoader classLoader = this.jarState.getClassLoader();
			final Class<?> formatterFunc = classLoader.loadClass("com.diffplug.spotless.glue.markdown.FlexmarkFormatterFunc");
			final Constructor<?> constructor = formatterFunc.getConstructor(MutableDataHolder.class);
			return (FormatterFunc) constructor.newInstance(this.options);
		}
	}
}
