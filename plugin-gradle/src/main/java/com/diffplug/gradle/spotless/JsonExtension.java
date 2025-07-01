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
package com.diffplug.gradle.spotless;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.json.JacksonJsonConfig;
import com.diffplug.spotless.json.JacksonJsonStep;
import com.diffplug.spotless.json.JsonPatchStep;
import com.diffplug.spotless.json.JsonSimpleStep;
import com.diffplug.spotless.json.gson.GsonStep;

public class JsonExtension extends FormatExtension {
	private static final int DEFAULT_INDENTATION = 4;
	private static final String DEFAULT_ZJSONPATCH_VERSION = "0.4.14";
	static final String NAME = "json";

	@Inject
	public JsonExtension(SpotlessExtension spotless) {
		super(spotless);
	}

	@Override
	protected void setupTask(SpotlessTask task) {
		if (target == null) {
			throw noDefaultTargetException();
		}
		super.setupTask(task);
	}

	public SimpleConfig simple() {
		return new SimpleConfig(DEFAULT_INDENTATION);
	}

	public GsonConfig gson() {
		return new GsonConfig();
	}

	public JacksonJsonGradleConfig jackson() {
		return new JacksonJsonGradleConfig(this);
	}

	/**
	 * Defaults to downloading the default Biome version from the network. To work
	 * offline, you can specify the path to the Biome executable via
	 * {@code biome().pathToExe(...)}.
	 */
	public BiomeJson biome() {
		return biome(null);
	}

	/** Downloads the given Biome version from the network. */
	public BiomeJson biome(String version) {
		var biomeConfig = new BiomeJson(version);
		addStep(biomeConfig.createStep());
		return biomeConfig;
	}

	public JsonPatchConfig jsonPatch(List<Map<String, Object>> patch) {
		return new JsonPatchConfig(patch);
	}

	public JsonPatchConfig jsonPatch(String zjsonPatchVersion, List<Map<String, Object>> patch) {
		return new JsonPatchConfig(zjsonPatchVersion, patch);
	}

	public class SimpleConfig {
		private int indent;

		public SimpleConfig(int indent) {
			this.indent = indent;
			addStep(createStep());
		}

		public void indentWithSpaces(int indent) {
			this.indent = indent;
			replaceStep(createStep());
		}

		private FormatterStep createStep() {
			return JsonSimpleStep.create(indent, provisioner());
		}
	}

	public class GsonConfig {
		private int indentSpaces;
		private boolean sortByKeys;
		private boolean escapeHtml;
		private String version;

		public GsonConfig() {
			this.indentSpaces = DEFAULT_INDENTATION;
			this.sortByKeys = false;
			this.escapeHtml = false;
			this.version = GsonStep.DEFAULT_VERSION;
			addStep(createStep());
		}

		public GsonConfig indentWithSpaces(int indentSpaces) {
			this.indentSpaces = indentSpaces;
			replaceStep(createStep());
			return this;
		}

		public GsonConfig sortByKeys() {
			this.sortByKeys = true;
			replaceStep(createStep());
			return this;
		}

		public GsonConfig escapeHtml() {
			this.escapeHtml = true;
			replaceStep(createStep());
			return this;
		}

		public GsonConfig version(String version) {
			this.version = version;
			replaceStep(createStep());
			return this;
		}

		private FormatterStep createStep() {
			return GsonStep.create(
					new com.diffplug.spotless.json.gson.GsonConfig(sortByKeys, escapeHtml, indentSpaces, version),
					provisioner());
		}
	}

	public static class JacksonJsonGradleConfig extends AJacksonGradleConfig<JacksonJsonGradleConfig> {
		protected JacksonJsonConfig jacksonConfig;

		public JacksonJsonGradleConfig(JacksonJsonConfig jacksonConfig, FormatExtension formatExtension) {
			super(jacksonConfig, formatExtension);
			this.jacksonConfig = jacksonConfig;

			formatExtension.addStep(createStep());
		}

		public JacksonJsonGradleConfig(FormatExtension formatExtension) {
			this(new JacksonJsonConfig(), formatExtension);
		}

		/**
		 * Refers to com.fasterxml.jackson.core.JsonGenerator.Feature
		 */
		public JacksonJsonGradleConfig jsonFeature(String feature, boolean toggle) {
			this.jacksonConfig.appendJsonFeatureToToggle(Collections.singletonMap(feature, toggle));
			formatExtension.replaceStep(createStep());
			return this;
		}

		@Override
		public JacksonJsonGradleConfig self() {
			return this;
		}

		// 'final' as it is called in the constructor
		@Override
		protected final FormatterStep createStep() {
			return JacksonJsonStep.create(jacksonConfig, version, formatExtension.provisioner());
		}
	}

	/**
	 * Biome formatter step for JSON.
	 */
	public class BiomeJson extends BiomeStepConfig<BiomeJson> {
		/**
		 * Creates a new Biome formatter step config for formatting JSON files. Unless
		 * overwritten, the given Biome version is downloaded from the network.
		 *
		 * @param version Biome version to use.
		 */
		public BiomeJson(String version) {
			super(getProject(), JsonExtension.this::replaceStep, version);
		}

		@Override
		protected String getLanguage() {
			return "json";
		}

		@Override
		protected BiomeJson getThis() {
			return this;
		}
	}

	public class JsonPatchConfig {
		private String zjsonPatchVersion;
		private List<Map<String, Object>> patch;

		public JsonPatchConfig(List<Map<String, Object>> patch) {
			this(DEFAULT_ZJSONPATCH_VERSION, patch);
		}

		public JsonPatchConfig(String zjsonPatchVersion, List<Map<String, Object>> patch) {
			this.zjsonPatchVersion = zjsonPatchVersion;
			this.patch = patch;
			addStep(createStep());
		}

		public JsonPatchConfig version(String zjsonPatchVersion) {
			this.zjsonPatchVersion = zjsonPatchVersion;
			replaceStep(createStep());
			return this;
		}

		private FormatterStep createStep() {
			return JsonPatchStep.create(zjsonPatchVersion, patch, provisioner());
		}
	}
}
