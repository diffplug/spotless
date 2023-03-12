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

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Properties;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterProperties;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.ThrowingEx;

import dev.equo.solstice.NestedJars;
import dev.equo.solstice.p2.P2ClientCache;
import dev.equo.solstice.p2.P2Model;
import dev.equo.solstice.p2.P2QueryCache;

/**
 * Generic Eclipse based formatter step {@link State} builder.
 */
public abstract class EquoBasedStepBuilder {
	private final String formatterName;
	private final Provisioner mavenProvisioner;
	private final ThrowingEx.Function<State, FormatterFunc> stateToFormatter;
	private String formatterVersion;
	private Iterable<File> settingsFiles = new ArrayList<>();

	/** Initialize valid default configuration, taking latest version */
	public EquoBasedStepBuilder(String formatterName, Provisioner mavenProvisioner, ThrowingEx.Function<State, FormatterFunc> stateToFormatter) {
		this.formatterName = formatterName;
		this.mavenProvisioner = mavenProvisioner;
		this.stateToFormatter = stateToFormatter;
	}

	public void setVersion(String version) {
		formatterVersion = version;
	}

	public void setPreferences(Iterable<File> settingsFiles) {
		this.settingsFiles = settingsFiles;
	}

	/** Returns the FormatterStep (whose state will be calculated lazily). */
	public FormatterStep build() {
		return FormatterStep.createLazy(formatterName, this::get, stateToFormatter);
	}

	protected abstract P2Model model(String version);

	/** Creates the state of the configuration. */
	EquoBasedStepBuilder.State get() throws Exception {
		var query = model(formatterVersion).query(P2ClientCache.PREFER_OFFLINE, P2QueryCache.ALLOW);
		var classpath = new ArrayList<File>();
		var mavenDeps = new ArrayList<String>();
		mavenDeps.add("dev.equo.ide:solstice:0.19.1");
		mavenDeps.addAll(query.getJarsOnMavenCentral());
		classpath.addAll(mavenProvisioner.provisionWithTransitives(false, mavenDeps));
		classpath.addAll(query.getJarsNotOnMavenCentral());
		for (var nested : NestedJars.inFiles(query.getJarsNotOnMavenCentral()).extractAllNestedJars()) {
			classpath.add(nested.getValue());
		}
		var jarState = JarState.preserveOrder(classpath);
		return new State(formatterVersion, jarState, FileSignature.signAsList(settingsFiles));
	}

	/**
	 * State of Eclipse configuration items, providing functionality to derived information
	 * based on the state.
	 */
	public static class State implements Serializable {
		private static final long serialVersionUID = 584400372246020995L;
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
