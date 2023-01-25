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
package com.diffplug.spotless.extra.eclipse;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.ThrowingEx;

import dev.equo.solstice.p2.P2Client;
import dev.equo.solstice.p2.P2Model;
import dev.equo.solstice.p2.P2Unit;

/**
 * Generic Eclipse based formatter step {@link State} builder.
 */
public class EquoBasedStepBuilder {
	private final String formatterName;
	private final Provisioner mavenProvisioner;
	private final P2Model p2;
	private final ThrowingEx.Function<State, FormatterFunc> stateToFormatter;
	private String formatterVersion;

	/** Initialize valid default configuration, taking latest version */
	public EquoBasedStepBuilder(String formatterName, Provisioner mavenProvisioner, P2Model p2, ThrowingEx.Function<State, FormatterFunc> stateToFormatter) {
		this.formatterName = formatterName;
		this.mavenProvisioner = mavenProvisioner;
		this.p2 = p2;
		this.stateToFormatter = stateToFormatter;
	}

	/** Returns the FormatterStep (whose state will be calculated lazily). */
	public FormatterStep build() throws Exception {
		return FormatterStep.createLazy(formatterName, this::get, stateToFormatter);
	}

	/** Creates the state of the configuration. */
	EquoBasedStepBuilder.State get() throws Exception {
		var caching = P2Client.Caching.PREFER_OFFLINE;
		var p2query = p2.query(caching);

		List<P2Unit> p2units = p2query.getJarsNotOnMavenCentral();
		List<String> mavenCoords = p2query.getJarsOnMavenCentral();

		List<File> p2Jars = new ArrayList<>();
		if (!p2units.isEmpty()) {
			try (P2Client client = new P2Client(caching)) {
				for (var p2unit : p2units) {
					p2Jars.add(client.download(p2unit));
				}
			}
		}
		boolean withTransitives = false;
		Set<File> mavenJars = mavenProvisioner.provisionWithTransitives(withTransitives, mavenCoords);

		throw new UnsupportedOperationException("TODO");
	}

	/**
	 * State of Eclipse configuration items, providing functionality to derived information
	 * based on the state.
	 */
	public static class State implements Serializable {}
}
