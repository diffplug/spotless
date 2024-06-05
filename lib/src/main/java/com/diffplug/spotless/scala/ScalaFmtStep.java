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
package com.diffplug.spotless.scala;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.Collections;

import javax.annotation.Nullable;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

/** Wraps up <a href="https://github.com/scalameta/scalafmt">scalafmt</a> as a FormatterStep. */
public class ScalaFmtStep implements Serializable {
	private static final long serialVersionUID = 1L;

	static final String DEFAULT_VERSION = "3.8.1";

	private static final String DEFAULT_SCALA_MAJOR_VERSION = "2.13";
	private static final String NAME = "scalafmt";
	private static final String MAVEN_COORDINATE = "org.scalameta:scalafmt-core_";

	private final JarState.Promised jarState;
	@Nullable
	private final File configFile;

	private ScalaFmtStep(JarState.Promised jarState, @Nullable File configFile) {
		this.jarState = jarState;
		this.configFile = configFile;
	}

	public static FormatterStep create(Provisioner provisioner) {
		return create(defaultVersion(), defaultScalaMajorVersion(), provisioner, null);
	}

	public static FormatterStep create(String version, Provisioner provisioner, @Nullable File configFile) {
		return create(version, defaultScalaMajorVersion(), provisioner, configFile);
	}

	public static FormatterStep create(String version, @Nullable String scalaMajorVersion, Provisioner provisioner, @Nullable File configFile) {
		String finalScalaMajorVersion = scalaMajorVersion == null ? DEFAULT_SCALA_MAJOR_VERSION : scalaMajorVersion;

		return FormatterStep.create(NAME,
				new ScalaFmtStep(JarState.promise(() -> JarState.from(MAVEN_COORDINATE + finalScalaMajorVersion + ":" + version, provisioner)), configFile),
				ScalaFmtStep::equalityState,
				State::createFormat);
	}

	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	public static String defaultScalaMajorVersion() {
		return DEFAULT_SCALA_MAJOR_VERSION;
	}

	private State equalityState() throws IOException {
		return new State(jarState.get(), configFile);
	}

	private static final class State implements Serializable {
		private static final long serialVersionUID = 1L;

		private final JarState jarState;
		private final FileSignature configSignature;

		State(JarState jarState, @Nullable File configFile) throws IOException {
			this.jarState = jarState;
			this.configSignature = FileSignature.signAsList(configFile == null ? Collections.emptySet() : Collections.singleton(configFile));
		}

		FormatterFunc createFormat() throws Exception {
			final ClassLoader classLoader = jarState.getClassLoader();
			final Class<?> formatterFunc = classLoader.loadClass("com.diffplug.spotless.glue.scalafmt.ScalafmtFormatterFunc");
			final Constructor<?> constructor = formatterFunc.getConstructor(FileSignature.class);
			return (FormatterFunc) constructor.newInstance(this.configSignature);
		}
	}
}
