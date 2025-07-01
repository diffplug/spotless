/*
 * Copyright 2024-2025 DiffPlug
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
package com.diffplug.gradle.spotless;

import javax.inject.Inject;

/** Gradle step for formatting CSS files. */
public class CssExtension extends FormatExtension {
	private static final String CSS_FILE_EXTENSION = "**/*.css";

	static final String NAME = "css";

	@Inject
	public CssExtension(SpotlessExtension spotless) {
		super(spotless);
	}

	/** If the user hasn't specified files, assume all CSS files should be checked. */
	@Override
	protected void setupTask(SpotlessTask task) {
		if (target == null) {
			target = parseTarget(CSS_FILE_EXTENSION);
		}
		super.setupTask(task);
	}

	/**
	 * Adds the default version of the biome formatter.
	 * Defaults to downloading the default Biome version from the network. To work
	 * offline, you can specify the path to the Biome executable via
	 * {@code biome().pathToExe(...)}.
	 */
	public BiomeCss biome() {
		return biome(null);
	}

	/**
	 * Adds the given version of the biome formatter.
	 * Defaults to downloading the default Biome version from the network. To work
	 * offline, you can specify the path to the Biome executable via
	 * {@code biome().pathToExe(...)}.
	 * @param version Biome version to use.
	 */
	public BiomeCss biome(String version) {
		var biomeConfig = new BiomeCss(version);
		addStep(biomeConfig.createStep());
		return biomeConfig;
	}

	/**
	 * Biome formatter step for CSS.
	 */
	public class BiomeCss extends BiomeStepConfig<BiomeCss> {
		/**
		 * Creates a new Biome formatter step config for formatting CSS files. Unless
		 * overwritten, the given Biome version is downloaded from the network.
		 *
		 * @param version Biome version to use.
		 */
		public BiomeCss(String version) {
			super(getProject(), CssExtension.this::replaceStep, version);
		}

		@Override
		protected String getLanguage() {
			return "css";
		}

		@Override
		protected BiomeCss getThis() {
			return this;
		}
	}
}
