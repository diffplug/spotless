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
 * A DTO holding the options for Jackson-based formatters
 */
public class JacksonConfig {

	private static final Map<String, Boolean> DEFAULT_FEATURE_TOGGLES;

	static {
		Map<String, Boolean> defaultFeatureToggles = new LinkedHashMap<>();
		// We activate by default the PrettyPrinter from Jackson
		defaultFeatureToggles.put("INDENT_OUTPUT", true);
		DEFAULT_FEATURE_TOGGLES = defaultFeatureToggles;
	}

	protected Map<String, Boolean> featureToToggle;

	// https://github.com/revelc/formatter-maven-plugin/pull/405
	protected boolean endWithEol = false;

	// https://github.com/revelc/formatter-maven-plugin/pull/280
	protected boolean spaceBeforeSeparator = false;

	public Map<String, Boolean> getFeatureToToggle() {
		return Collections.unmodifiableMap(featureToToggle);
	}

	public void setFeatureToToggle(Map<String, Boolean> featureToToggle) {
		this.featureToToggle = featureToToggle;
	}

	public void appendFeatureToToggle(Map<String, Boolean> features) {
		this.featureToToggle.putAll(features);
	}

	public boolean isEndWithEol() {
		return endWithEol;
	}

	public void setEndWithEol(boolean endWithEol) {
		this.endWithEol = endWithEol;
	}

	public boolean isSpaceBeforeSeparator() {
		return spaceBeforeSeparator;
	}

	public void setSpaceBeforeSeparator(boolean spaceBeforeSeparator) {
		this.spaceBeforeSeparator = spaceBeforeSeparator;
	}
}
