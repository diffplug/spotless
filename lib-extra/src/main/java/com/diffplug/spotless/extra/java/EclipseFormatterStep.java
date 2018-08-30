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
import java.util.Collections;
import java.util.Objects;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.extra.EclipseBasedStepBuilder;

/**
 * Formatter step which calls out to the Eclipse formatter.
 * This class is deprecated. Use {@link EclipseJdtFormatterStep} instead.
 */
@Deprecated
public final class EclipseFormatterStep {
	// prevent direct instantiation
	private EclipseFormatterStep() {}

	private static final String DEFAULT_VERSION = "4.7.2";

	/** Creates a formatter step for the given version and settings file.
	 * Formatter steps based on property configuration should support zero (default configuration)
	 * to many files. Use {@link #create(Iterable, Provisioner)} instead.*/
	@Deprecated
	public static FormatterStep create(File settingsFile, Provisioner provisioner) {
		return create(Collections.singletonList(settingsFile), provisioner);
	}

	/** Creates a formatter step for the given version and settings file. */
	public static FormatterStep create(Iterable<File> settingsFiles, Provisioner provisioner) {
		return create(defaultVersion(), settingsFiles, provisioner);
	}

	/** Creates a formatter step for the given version and settings file.
	 * Formatter steps based on property configuration should support zero (default configuration)
	 * to many files. Use {@link #create(String, Iterable, Provisioner)} instead.*/
	@Deprecated
	public static FormatterStep create(String version, File settingsFile, Provisioner provisioner) {
		return create(version, Collections.singletonList(settingsFile), provisioner);
	}

	/** Creates a formatter step for the given version and settings files. */
	public static FormatterStep create(String version, Iterable<File> settingsFiles, Provisioner provisioner) {
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(settingsFiles, "settingsFiles");
		Objects.requireNonNull(provisioner, "provisioner");
		EclipseBasedStepBuilder builder = EclipseJdtFormatterStep.createBuilder(provisioner);
		builder.setPreferences(settingsFiles);
		builder.setVersion(DEFAULT_VERSION);
		return builder.build();
	}

	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

}
