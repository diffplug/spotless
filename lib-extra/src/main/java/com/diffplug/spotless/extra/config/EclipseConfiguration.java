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
package com.diffplug.spotless.extra.config;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterProperties;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.ThrowingEx;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Eclipse formatter generic configuration validates the user arguments and creates on request
 * a {@link State} of the current values.
 */
public class EclipseConfiguration implements ThrowingEx.Supplier<EclipseConfiguration.State> {
	/**
	 * Resource location of Spotless Eclipse Formatter Maven coordinate lists.
	 * <p>
	 * Spotless Eclipse Formatter dependencies have fixed transitive versions, since Spotless Eclipse Formatter
	 * implementations access internal methods of the Eclipse plugins, which may change with every
	 * version change, including minor and patch version changes.
	 * At the resource location for each supported Spotless Eclipse Formatter, a text file is provided, containing
	 * the fixed versions for the formatter and its transitive dependencies.
	 * Each line in the text file corresponds to the {@link MavenCoordinates.Coordinate#FORMAT}.
	 * </p>
	 */
	private static final String ECLIPSE_FORMATTER_RESOURCES = EclipseConfiguration.class.getPackage().getName().replace('.', '/');

	private final TreeSet<SemanticVersion> supportedEclipseVersions;
	private final URL eclipseConfigContext;
	private final Provisioner jarProvisioner;
	private URL defaultCoordinatesUrl;
	private MavenCoordinates coordinateAdaptations;
	private SemanticVersion version;
	private Iterable<File> settingsFiles;

	/** Initialize valid default configuration, taking latest version */
	public EclipseConfiguration(String formatterName, Provisioner jarProvisioner, String... supportedEclipseVersions) {
		Objects.requireNonNull(formatterName, "formatterName");
		Objects.requireNonNull(jarProvisioner, "jarProvisioner");
		Objects.requireNonNull(supportedEclipseVersions, "supportedEclipseVersions");
		if (supportedEclipseVersions.length < 1) {
			throw new IllegalArgumentException("At lease one version must be allowed.");
		}

		Path relativeFormatterResourcePath = Paths.get(ECLIPSE_FORMATTER_RESOURCES, formatterName.replace(' ', '_'));
		eclipseConfigContext = getClass().getClassLoader().getResource(relativeFormatterResourcePath.toString());
		if (null == eclipseConfigContext) {
			throw new IllegalArgumentException(String.format("Eclipse configuration context resource path '%s' not found.", relativeFormatterResourcePath));
		}

		this.supportedEclipseVersions = new TreeSet<SemanticVersion>();
		for (String allowedVersion : supportedEclipseVersions) {
			this.supportedEclipseVersions.add(new SemanticVersion(allowedVersion));
		}
		version = this.supportedEclipseVersions.last(); //Use latest version per default
		defaultCoordinatesUrl = getDefaultCoordinatesUrl(eclipseConfigContext, version);
		this.jarProvisioner = jarProvisioner;

		coordinateAdaptations = new MavenCoordinates(); //Use default coordinates configured for version
		settingsFiles = new ArrayList<File>(); //Use default preferences
	}

	/** Set Eclipse version */
	public void setVersion(String versionOrUrl) {
		try {
			// For development purpose and patch delivery it is allowed
			// to specify directly the URL of a M2 coordinates file.
			defaultCoordinatesUrl = new URL(versionOrUrl);
			// The default version is not changed.
		} catch (MalformedURLException ignored) {
			SemanticVersion newVersion = new SemanticVersion(versionOrUrl);
			if (supportedEclipseVersions.contains(newVersion)) {
				defaultCoordinatesUrl = getDefaultCoordinatesUrl(eclipseConfigContext, newVersion);
				version = newVersion;
			} else {
				throw new IllegalArgumentException(
						String.format("Version '%s' is not part of the supported versions '%s'.",
								versionOrUrl,
								supportedEclipseVersions.stream().map(v -> v.toString()).collect(Collectors.joining(", "))));
			}
		}
	}

	private static URL getDefaultCoordinatesUrl(URL eclipseConfigContext, SemanticVersion version) {
		try {
			Path filePath = Paths.get(eclipseConfigContext.getPath(), String.format("v%s%s",
					version.toString(), MavenCoordinates.GRADLE_LOCKFILE_EXTENSION));
			return new URL(eclipseConfigContext.getProtocol(), eclipseConfigContext.getHost(), filePath.toString());
		} catch (MalformedURLException e) {
			/*
			 *  This exception must be prevented by the strict syntax checking of
			 *  SemanticVersion and unit-tests of all allowed versions.
			 */
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Modify default Maven dependencies configured for the formatter.
	 * The default dependencies are located in {@link #ECLIPSE_FORMATTER_RESOURCES}.
	 */
	public void setDependencies(String... coordinates) {
		coordinateAdaptations = new MavenCoordinates();
		coordinateAdaptations.update(coordinates);
	}

	/** Set settings files containing Eclipse preferences */
	public void setPreferences(Iterable<File> settingsFiles) {
		this.settingsFiles = settingsFiles;
	}

	/** Creates the state of the configuration */
	public EclipseConfiguration.State get() {
		/*
		 * The current use case is tailored for Gradle.
		 * Gradle calls this method only once per execution
		 * and compares the State with the one of a previous run
		 * for incremental building.
		 * Hence a lazy construction is not required.
		 */
		return new State(
				jarProvisioner,
				createMavenCoordinates(),
				version,
				settingsFiles);
	}

	private MavenCoordinates createMavenCoordinates() {
		MavenCoordinates coordinates = new MavenCoordinates();
		coordinates.add(defaultCoordinatesUrl);
		coordinates.update(coordinateAdaptations);
		return coordinates;
	}

	/**
	 * State of Eclipse configuration items, providing functionality to derived information
	 * based on the state.
	 */
	public static class State implements Serializable {
		// Not used, only the serialization output is required to determine whether the object has changed
		private static final long serialVersionUID = 1L;

		private final FileSignature settingsFiles;
		private final MavenCoordinates coordinates;
		private final SemanticVersion version;

		/*
		 * Fields are transient because not needed to uniquely identify a Eclipse configuration state, and also because
		 * Gradle only needs this class to be serializable so it can compare its members for incremental builds.
		 */
		private transient ClassLoader lazyClassLoader;
		@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
		private final transient Provisioner jarProvisioner;

		/** State constructor expects that all passed items are not modified afterwards */
		protected State(Provisioner jarProvisioner, MavenCoordinates coordinates, SemanticVersion version, Iterable<File> settingsFiles) {
			this.jarProvisioner = jarProvisioner;
			this.coordinates = coordinates;
			this.version = version;
			lazyClassLoader = null;

			try {
				this.settingsFiles = FileSignature.signAsList(settingsFiles);
			} catch (NullPointerException e) {
				throw new IllegalArgumentException("Some configuration files are 'null'.", e);
			} catch (IOException e) {
				throw new IllegalStateException(e); //Canonical path problems are not user argument problems
			}
		}

		/**
		 * Comparison of configured Eclipse version with another version.
		 * Return one of -1, 0, or 1 according to whether the other version is higher, equal or lower.
		 */
		public int compareVersionTo(String otherVersion) {
			return version.compareTo(new SemanticVersion(otherVersion));
		}

		/** Get formatter preferences */
		public Properties getPreferences() {
			//Keep the IllegalArgumentException since it contains detailed information
			FormatterProperties preferences = FormatterProperties.from(settingsFiles.files());
			return preferences.getProperties();
		}

		/** Load class based on the given configuration of JAR provider and Maven coordinates. */
		public Class<?> loadClass(String name) {
			try {
				return getClassLoader().loadClass(name);
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException(
						String.format("Could not find class '%s' in Maven coordinates:%n%s%n",
								name, coordinates),
						e);
			}
		}

		private ClassLoader getClassLoader() {
			if (null == lazyClassLoader) {
				String[] mavenCoordinates = coordinates.get();
				try {
					JarState jarState = JarState.from(jarProvisioner, mavenCoordinates);
					lazyClassLoader = jarState.getClassLoader();
				} catch (IOException e) {
					throw new IllegalArgumentException(
							String.format(
									"Not all dependencies have been resolved successfully by the Maven coordinates:%n%s%n", coordinates),
							e);
				}
			}
			return lazyClassLoader;
		}
	}
}
