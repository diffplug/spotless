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
package com.diffplug.gradle.spotless;

import java.util.Collections;

import javax.inject.Inject;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.json.JacksonJsonStep;
import com.diffplug.spotless.yaml.JacksonYamlConfig;
import com.diffplug.spotless.yaml.JacksonYamlStep;

public class YamlExtension extends FormatExtension {
	private static final String DEFAULT_GSON_VERSION = JacksonJsonStep.defaultVersion();
	static final String NAME = "yaml";

	@Inject
	public YamlExtension(SpotlessExtension spotless) {
		super(spotless);
	}

	@Override
	protected void setupTask(SpotlessTask task) {
		if (target == null) {
			throw noDefaultTargetException();
		}
		super.setupTask(task);
	}

	public JacksonGradleConfig jacksonYaml() {
		return new JacksonYamlGradleConfig(this);
	}

	public class JacksonYamlGradleConfig extends JacksonGradleConfig {
		protected JacksonYamlConfig jacksonConfig;

		public JacksonYamlGradleConfig(JacksonYamlConfig jacksonConfig, FormatExtension formatExtension) {
			super(jacksonConfig, formatExtension);

			this.jacksonConfig = jacksonConfig;

			if (jacksonConfig == null) {
				throw new IllegalArgumentException("ARG");
			}
		}

		public JacksonYamlGradleConfig(FormatExtension formatExtension) {
			this(new JacksonYamlConfig(), formatExtension);
		}

		/**
		 * @see com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature
		 */
		public JacksonGradleConfig yamlFeature(String feature, boolean toggle) {
			this.jacksonConfig.appendYamlFeatureToToggle(Collections.singletonMap(feature, toggle));
			formatExtension.replaceStep(createStep());
			return this;
		}

		@Override
		protected FormatterStep createStep() {
			return JacksonYamlStep.create(jacksonConfig, version, provisioner());
		}
	}
}
