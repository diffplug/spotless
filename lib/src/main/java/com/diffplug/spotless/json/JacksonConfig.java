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
package com.diffplug.spotless.json;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * A DTO holding the basic for Jackson-based formatters
 */
public class JacksonConfig implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	private static final Map<String, Boolean> DEFAULT_FEATURE_TOGGLES;

	static {
		Map<String, Boolean> defaultFeatureToggles = new LinkedHashMap<>();
		// We activate by default the PrettyPrinter from Jackson
		// @see com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT
		defaultFeatureToggles.put("INDENT_OUTPUT", true);
		DEFAULT_FEATURE_TOGGLES = defaultFeatureToggles;
	}

	protected Map<String, Boolean> featureToToggle = new TreeMap<>(DEFAULT_FEATURE_TOGGLES);

	public Map<String, Boolean> getFeatureToToggle() {
		return Collections.unmodifiableMap(featureToToggle);
	}

	public void setFeatureToToggle(Map<String, Boolean> featureToToggle) {
		this.featureToToggle = featureToToggle;
	}

	public void appendFeatureToToggle(Map<String, Boolean> features) {
		this.featureToToggle.putAll(features);
	}
}
