/*
 * Copyright 2021-2025 DiffPlug
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
package com.diffplug.spotless.kotlin;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.Objects;

import javax.annotation.Nullable;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

/** Wraps up <a href="https://github.com/cqfn/diKTat">diktat</a> as a FormatterStep. */
public class DiktatStep implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;
	private final JarState.Promised jarState;
	private final String versionDiktat;
	private final boolean isScript;
	private final @Nullable FileSignature.Promised config;

	private DiktatStep(JarState.Promised jarState, String versionDiktat, boolean isScript, @Nullable FileSignature config) {
		this.jarState = jarState;
		this.versionDiktat = versionDiktat;
		this.isScript = isScript;
		this.config = config != null ? config.asPromise() : null;
	}

	private static final String MIN_SUPPORTED_VERSION = "1.2.1";

	private static final String PACKAGE_RELOCATED_VERSION = "2.0.0";

	private static final String DEFAULT_VERSION = "2.0.0";
	private static final String NAME = "diktat";
	private static final String MAVEN_COORDINATE_PRE_2_0_0 = "org.cqfn.diktat:diktat-rules:";
	private static final String MAVEN_COORDINATE = "com.saveourtool.diktat:diktat-runner:";

	public static String defaultVersionDiktat() {
		return DEFAULT_VERSION;
	}

	public static FormatterStep create(Provisioner provisioner) {
		return create(defaultVersionDiktat(), provisioner);
	}

	public static FormatterStep create(String versionDiktat, Provisioner provisioner) {
		return create(versionDiktat, provisioner, null);
	}

	public static FormatterStep create(String versionDiktat, Provisioner provisioner, @Nullable FileSignature config) {
		return create(versionDiktat, provisioner, false, config);
	}

	public static FormatterStep create(String versionDiktat, Provisioner provisioner, boolean isScript, @Nullable FileSignature config) {
		if (BadSemver.version(versionDiktat) < BadSemver.version(MIN_SUPPORTED_VERSION)) {
			throw new IllegalStateException("Minimum required Diktat version is " + MIN_SUPPORTED_VERSION + ", you tried " + versionDiktat + " which is too old");
		}
		Objects.requireNonNull(versionDiktat, "versionDiktat");
		Objects.requireNonNull(provisioner, "provisioner");
		final String diktatCoordinate;
		if (BadSemver.version(versionDiktat) >= BadSemver.version(PACKAGE_RELOCATED_VERSION)) {
			diktatCoordinate = MAVEN_COORDINATE + versionDiktat;
		} else {
			diktatCoordinate = MAVEN_COORDINATE_PRE_2_0_0 + versionDiktat;
		}
		return FormatterStep.create(NAME,
				new DiktatStep(JarState.promise(() -> JarState.from(diktatCoordinate, provisioner)), versionDiktat, isScript, config),
				DiktatStep::equalityState, State::createFormat);
	}

	private State equalityState() throws Exception {
		return new State(jarState.get(), versionDiktat, isScript, config != null ? config.get() : null);
	}

	static final class State implements Serializable {
		@Serial
		private static final long serialVersionUID = 1L;

		final JarState jar;
		final String versionDiktat;
		/** Are the files being linted Kotlin script files. */
		private final boolean isScript;
		private final @Nullable FileSignature config;

		State(JarState jar, String versionDiktat, boolean isScript, @Nullable FileSignature config) {
			this.jar = jar;
			this.versionDiktat = versionDiktat;
			this.isScript = isScript;
			this.config = config;
		}

		FormatterFunc createFormat() throws Exception {
			final File configFile = (config != null) ? config.getOnlyFile() : null;
			Class<?> formatterFunc = jar.getClassLoader().loadClass("com.diffplug.spotless.glue.diktat.DiktatFormatterFunc");
			Constructor<?> constructor = formatterFunc.getConstructor(
					String.class,
					File.class,
					boolean.class);
			return (FormatterFunc.NeedsFile) constructor.newInstance(versionDiktat, configFile, isScript);
		}
	}
}
