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
package com.diffplug.spotless.extra;

import static java.util.stream.Collectors.toMap;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nullable;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterProperties;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.SerializedFunction;

import dev.equo.solstice.NestedJars;
import dev.equo.solstice.p2.CacheLocations;
import dev.equo.solstice.p2.P2ClientCache;
import dev.equo.solstice.p2.P2Model;
import dev.equo.solstice.p2.P2QueryCache;
import dev.equo.solstice.p2.P2QueryResult;

/**
 * Generic Eclipse based formatter step {@link State} builder.
 */
public abstract class EquoBasedStepBuilder {
	private final String formatterName;
	private final Provisioner mavenProvisioner;
	private final SerializedFunction<State, FormatterFunc> stateToFormatter;
	private String formatterVersion;
	private Iterable<File> settingsFiles = new ArrayList<>();
	private Map<String, String> p2Mirrors = Map.of();
	private File cacheDirectory;

	/** Initialize valid default configuration, taking latest version */
	public EquoBasedStepBuilder(String formatterName, Provisioner mavenProvisioner, @Nullable String defaultVersion, SerializedFunction<State, FormatterFunc> stateToFormatter) {
		this.formatterName = formatterName;
		this.mavenProvisioner = mavenProvisioner;
		this.formatterVersion = defaultVersion;
		this.stateToFormatter = stateToFormatter;
	}

	public void setVersion(String version) {
		formatterVersion = version;
	}

	public void setPreferences(Iterable<File> settingsFiles) {
		this.settingsFiles = settingsFiles;
	}

	public void setP2Mirrors(Map<String, String> p2Mirrors) {
		this.p2Mirrors = Map.copyOf(p2Mirrors);
	}

	public void setP2Mirrors(Collection<P2Mirror> p2Mirrors) {
		this.p2Mirrors = p2Mirrors.stream().collect(toMap(P2Mirror::getPrefix, P2Mirror::getUrl));
	}

	public void setCacheDirectory(File cacheDirectory) {
		this.cacheDirectory = cacheDirectory;
	}

	protected abstract P2Model model(String version);

	protected void addPlatformRepo(P2Model model, String version) {
		if (!version.startsWith("4.")) {
			throw new IllegalArgumentException("Expected 4.x");
		}
		int minorVersion = Integer.parseInt(version.substring("4.".length()));

		model.addP2Repo("https://download.eclipse.org/eclipse/updates/" + version + "/");
		model.getInstall().addAll(List.of(
				"org.apache.felix.scr",
				"org.eclipse.equinox.event"));
		if (minorVersion >= 25) {
			model.getInstall().addAll(List.of(
					"org.osgi.service.cm",
					"org.osgi.service.metatype"));
		}
	}

	/** Returns the FormatterStep (whose state will be calculated lazily). */
	public FormatterStep build() {
		var roundtrippableState = new EquoStep(formatterVersion, FileSignature.promise(settingsFiles), JarState.promise(() -> {
			P2QueryResult query;
			try {
				if (null != cacheDirectory) {
					CacheLocations.override_p2data = cacheDirectory.toPath().resolve("dev/equo/p2-data").toFile();
				}
				query = createModelWithMirrors().query(P2ClientCache.PREFER_OFFLINE, P2QueryCache.ALLOW);
			} catch (Exception x) {
				throw new IOException("Failed to load " + formatterName + ": " + x, x);
			}
			var classpath = new ArrayList<File>();
			var mavenDeps = new ArrayList<String>();
			mavenDeps.add("dev.equo.ide:solstice:1.8.0");
			mavenDeps.add("com.diffplug.durian:durian-swt.os:4.2.0");
			mavenDeps.addAll(query.getJarsOnMavenCentral());
			classpath.addAll(mavenProvisioner.provisionWithTransitives(false, mavenDeps));
			classpath.addAll(query.getJarsNotOnMavenCentral());
			for (var nested : NestedJars.inFiles(query.getJarsNotOnMavenCentral()).extractAllNestedJars()) {
				classpath.add(nested.getValue());
			}
			return JarState.preserveOrder(classpath);
		}));
		return FormatterStep.create(formatterName, roundtrippableState, EquoStep::state, stateToFormatter);
	}

	private P2Model createModelWithMirrors() {
		P2Model model = model(formatterVersion);
		if (p2Mirrors.isEmpty()) {
			return model;
		}

		ArrayList<String> p2Repos = new ArrayList<>(model.getP2repo());
		p2Repos.replaceAll(url -> {
			for (Map.Entry<String, String> mirror : p2Mirrors.entrySet()) {
				String prefix = mirror.getKey();
				if (url.startsWith(prefix)) {
					return mirror.getValue() + url.substring(prefix.length());
				}
			}

			throw new IllegalStateException("no mirror configured for P2 repository: " + url);
		});

		model.getP2repo().clear();
		model.getP2repo().addAll(p2Repos);
		return model;
	}

	static class EquoStep implements Serializable {
		private static final long serialVersionUID = 1;
		private final String semanticVersion;
		private final FileSignature.Promised settingsPromise;
		private final JarState.Promised jarPromise;

		EquoStep(String semanticVersion, FileSignature.Promised settingsPromise, JarState.Promised jarPromise) {
			this.semanticVersion = semanticVersion;
			this.settingsPromise = settingsPromise;
			this.jarPromise = jarPromise;
		}

		private State state() {
			return new State(semanticVersion, jarPromise.get(), settingsPromise.get());
		}
	}

	/**
	 * State of Eclipse configuration items, providing functionality to derived information
	 * based on the state.
	 */
	public static class State implements Serializable {
		private static final long serialVersionUID = 1;
		final String semanticVersion;
		final JarState jarState;
		final FileSignature settingsFiles;

		public State(String semanticVersion, JarState jarState, FileSignature settingsFiles) {
			this.semanticVersion = semanticVersion;
			this.jarState = jarState;
			this.settingsFiles = settingsFiles;
		}

		public JarState getJarState() {
			return jarState;
		}

		public String getSemanticVersion() {
			return semanticVersion;
		}

		public Properties getPreferences() {
			return FormatterProperties.from(settingsFiles.files()).getProperties();
		}
	}
}
