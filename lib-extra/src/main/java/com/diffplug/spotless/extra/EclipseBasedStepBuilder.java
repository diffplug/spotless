/*
 * Copyright 2016-2023 DiffPlug
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
package com.diffplug.spotless.extra;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import com.diffplug.common.base.Errors;
import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterProperties;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.SerializedFunction;

/**
 * Generic Eclipse based formatter step {@link State} builder.
 */
public class EclipseBasedStepBuilder {
	private final String formatterName;
	private final String formatterStepExt;
	private final SerializedFunction<State, FormatterFunc> stateToFormatter;
	private final Provisioner jarProvisioner;
	private String formatterVersion;

	/**
	 * Resource location of Spotless Eclipse Formatter Maven coordinate lists.
	 * <p>
	 * Spotless Eclipse Formatter dependencies have fixed transitive versions, since Spotless Eclipse Formatter
	 * implementations access internal methods of the Eclipse plugins, which may change with every
	 * version change, including minor and patch version changes.
	 * At the resource location for each supported Spotless Eclipse Formatter, a text file is provided, containing
	 * the fixed versions for the formatter and its transitive dependencies.
	 * Each line is either a comment starting with {@code #} or corresponds to the format
	 * {@code <groupId>:<artifactId>[:packaging][:classifier]:<versionRestriction>}
	 * </p>
	 */
	private static final String ECLIPSE_FORMATTER_RESOURCES = EclipseBasedStepBuilder.class.getPackage().getName().replace('.', '/');

	private List<String> dependencies = new ArrayList<>();
	private Iterable<File> settingsFiles = new ArrayList<>();

	/** Initialize valid default configuration, taking latest version */
	public EclipseBasedStepBuilder(String formatterName, Provisioner jarProvisioner, SerializedFunction<State, FormatterFunc> stateToFormatter) {
		this(formatterName, "", jarProvisioner, stateToFormatter);
	}

	/** Initialize valid default configuration, taking latest version */
	public EclipseBasedStepBuilder(String formatterName, String formatterStepExt, Provisioner jarProvisioner, SerializedFunction<State, FormatterFunc> stateToFormatter) {
		this.formatterName = Objects.requireNonNull(formatterName, "formatterName");
		this.formatterStepExt = Objects.requireNonNull(formatterStepExt, "formatterStepExt");
		this.jarProvisioner = Objects.requireNonNull(jarProvisioner, "jarProvisioner");
		this.stateToFormatter = Objects.requireNonNull(stateToFormatter, "stateToFormatter");
		formatterVersion = "No version set"; //Will fail creation
	}

	/** Returns the FormatterStep (whose state will be calculated lazily). */
	public FormatterStep build() {
		var roundtrippableState = new EclipseStep(formatterVersion, formatterStepExt, FileSignature.promise(settingsFiles), JarState.promise(() -> {
			return JarState.withoutTransitives(dependencies, jarProvisioner);
		}));
		return FormatterStep.create(formatterName + formatterStepExt, roundtrippableState,
				EclipseStep::state, stateToFormatter);
	}

	/** Set dependencies for the corresponding Eclipse version */
	public void setVersion(String version) {
		String url = "/" + ECLIPSE_FORMATTER_RESOURCES + "/" + formatterName.replace(' ', '_') + "/v" + version + ".lockfile";
		InputStream depsFile = EclipseBasedStepBuilder.class.getResourceAsStream(url);
		if (depsFile == null) {
			throw new IllegalArgumentException("No such version " + version + ", expected at " + url);
		}
		byte[] content = toByteArray(depsFile);
		String allLines = new String(content, StandardCharsets.UTF_8);
		String[] lines = allLines.split("\n");
		dependencies.clear();
		for (String line : lines) {
			if (!line.startsWith("#")) {
				dependencies.add(line);
			}
		}
		formatterVersion = version;
	}

	private static byte[] toByteArray(InputStream in) {
		ByteArrayOutputStream to = new ByteArrayOutputStream();
		byte[] buf = new byte[8192];
		try {
			while (true) {
				int r = in.read(buf);
				if (r == -1) {
					break;
				}
				to.write(buf, 0, r);
			}
			return to.toByteArray();
		} catch (IOException e) {
			throw Errors.asRuntime(e);
		}
	}

	/** Set settings files containing Eclipse preferences */
	public void setPreferences(Iterable<File> settingsFiles) {
		this.settingsFiles = settingsFiles;
	}

	static class EclipseStep implements Serializable {
		private static final long serialVersionUID = 1;
		private final String semanticVersion;
		private final String formatterStepExt;
		private final FileSignature.Promised settingsPromise;
		private final JarState.Promised jarPromise;

		EclipseStep(String semanticVersion, String formatterStepExt, FileSignature.Promised settingsPromise, JarState.Promised jarPromise) {
			this.semanticVersion = semanticVersion;
			this.formatterStepExt = formatterStepExt;
			this.settingsPromise = settingsPromise;
			this.jarPromise = jarPromise;
		}

		private State state() {
			return new State(semanticVersion, formatterStepExt, jarPromise.get(), settingsPromise.get());
		}
	}

	/**
	 * State of Eclipse configuration items, providing functionality to derived information
	 * based on the state.
	 */
	public static class State implements Serializable {
		// Not used, only the serialization output is required to determine whether the object has changed
		private static final long serialVersionUID = 1L;

		private final JarState jarState;
		private final String semanticVersion;
		//The formatterStepExt assures that different class loaders are used for different step types
		@SuppressWarnings("unused")
		private final String formatterStepExt;
		private final FileSignature settingsFiles;

		/** State constructor expects that all passed items are not modified afterwards */
		protected State(String formatterVersion, String formatterStepExt, JarState jarState, FileSignature settingsFiles) {
			this.jarState = jarState;
			this.settingsFiles = settingsFiles;
			this.formatterStepExt = formatterStepExt;
			semanticVersion = convertEclipseVersion(formatterVersion);
		}

		private static String convertEclipseVersion(String version) {
			String semanticVersion = version;
			//Old Eclipse versions used a character at the end. For example '4.7.3a'.
			if (1 < version.length()) {
				char lastChar = version.charAt(version.length() - 1);
				if ('.' != lastChar && 'a' <= lastChar) {
					semanticVersion = version.substring(0, version.length() - 1);
					semanticVersion += String.format(".%d", (int) lastChar);
				}
			}
			return semanticVersion;
		}

		public String getSemanticVersion() {
			return semanticVersion;
		}

		/** Get formatter preferences */
		public Properties getPreferences() {
			//Keep the IllegalArgumentException since it contains detailed information
			FormatterProperties preferences = FormatterProperties.from(settingsFiles.files());
			return preferences.getProperties();
		}

		/**
		 * Load class based on the given configuration of JAR provider and Maven coordinates.
		 * Different class loader instances are provided in the following scenarios:
		 * <ol>
		 * <li>The JARs ({@link #jarState}) have changes (this should only occur during development)</li>
		 * <li>Different configurations ({@link #settingsFiles}) are used for different sub-projects</li>
		 * <li>The same Eclipse step implementation provides different formatter types ({@link #formatterStepExt})</li>
		 * </ol>
		 */
		public Class<?> loadClass(String name) {
			try {
				return jarState.getClassLoader(this).loadClass(name);
			} catch (ClassNotFoundException e) {
				throw Errors.asRuntime(e);
			}
		}
	}
}
