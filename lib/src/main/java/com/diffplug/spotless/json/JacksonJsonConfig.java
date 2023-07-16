/*
 * Copyright 2023 DiffPlug
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
package com.diffplug.spotless.json;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Specialization of {@link JacksonConfig} for JSON documents
 */
public class JacksonJsonConfig extends JacksonConfig {
	private static final long serialVersionUID = 1L;

	protected Map<String, Boolean> jsonFeatureToToggle = new LinkedHashMap<>();

	// https://github.com/revelc/formatter-maven-plugin/pull/280
	// By default, Jackson adds a ' ' before separator, which is not standard with most IDE/JSON libraries
	protected boolean spaceBeforeSeparator = false;

	public Map<String, Boolean> getJsonFeatureToToggle() {
		return Collections.unmodifiableMap(jsonFeatureToToggle);
	}

	/**
	 * Refers to com.fasterxml.jackson.core.JsonGenerator.Feature
	 */
	public void setJsonFeatureToToggle(Map<String, Boolean> jsonFeatureToToggle) {
		this.jsonFeatureToToggle = jsonFeatureToToggle;
	}

	/**
	 * Refers to com.fasterxml.jackson.core.JsonGenerator.Feature
	 */
	public void appendJsonFeatureToToggle(Map<String, Boolean> features) {
		this.jsonFeatureToToggle.putAll(features);
	}

	public boolean isSpaceBeforeSeparator() {
		return spaceBeforeSeparator;
	}

	public void setSpaceBeforeSeparator(boolean spaceBeforeSeparator) {
		this.spaceBeforeSeparator = spaceBeforeSeparator;
	}
}
