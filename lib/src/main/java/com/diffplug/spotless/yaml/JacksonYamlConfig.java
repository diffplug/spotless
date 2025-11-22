/*
 * Copyright 2023-2025 DiffPlug
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
package com.diffplug.spotless.yaml;

import static java.util.Collections.unmodifiableMap;

import java.io.Serial;
import java.util.LinkedHashMap;
import java.util.Map;

import com.diffplug.spotless.json.JacksonConfig;

/**
 * Specialization of {@link JacksonConfig} for YAML documents
 */
public class JacksonYamlConfig extends JacksonConfig {
	@Serial
	private static final long serialVersionUID = 1L;

	protected Map<String, Boolean> yamlFeatureToToggle = new LinkedHashMap<>();

	public Map<String, Boolean> getYamlFeatureToToggle() {
		return unmodifiableMap(yamlFeatureToToggle);
	}

	/**
	 * Refers to com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature
	 */
	public void setYamlFeatureToToggle(Map<String, Boolean> yamlFeatureToToggle) {
		this.yamlFeatureToToggle = yamlFeatureToToggle;
	}

	/**
	 * Refers to com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature
	 */
	public void appendYamlFeatureToToggle(Map<String, Boolean> features) {
		this.yamlFeatureToToggle.putAll(features);
	}

}
