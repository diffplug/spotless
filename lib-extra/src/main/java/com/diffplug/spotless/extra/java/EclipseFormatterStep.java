/*
 * Copyright 2016 DiffPlug
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
package com.diffplug.spotless.extra.java;

import java.io.File;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.extra.ExtFormatterState;

/** Formatter step which calls out to the Eclipse formatter. */
public final class EclipseFormatterStep {
	// prevent direct instantiation
	private EclipseFormatterStep() {}

	private static final String DEFAULT_VERSION = "4.6.1";
	private static final String NAME = "eclipse formatter";
	private static final String MAVEN_COORDINATE = "com.diffplug.spotless:spotless-ext-eclipse-jdt:";
	private static final String FORMATTER_CLASS = "com.diffplug.gradle.spotless.java.eclipse.EclipseFormatterStepImpl";

	/** Creates a formatter step for the given version and settings file. */
	public static FormatterStep create(Iterable<File> settingsFiles, Provisioner provisioner) {
		return create(defaultVersion(), settingsFiles, provisioner);
	}

	/** Creates a formatter step for the given version and property files (supporting pref, profiles, properties, ...). */
	public static FormatterStep create(String version, Iterable<File> configFiles, Provisioner provisioner) {
		return FormatterStep.createLazy(NAME,
				() -> ExtFormatterState.from(
						JarState.from(MAVEN_COORDINATE + version, provisioner),
						FORMATTER_CLASS, configFiles),
				ExtFormatterState::basedOnProperties);
	}

	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

}
