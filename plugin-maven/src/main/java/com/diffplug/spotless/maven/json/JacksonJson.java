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
package com.diffplug.spotless.maven.json;

import static java.util.Collections.emptyMap;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.json.JacksonJsonConfig;
import com.diffplug.spotless.json.JacksonJsonStep;
import com.diffplug.spotless.maven.FormatterFactory;
import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.FormatterStepFactory;
import java.util.Collections;
import java.util.Map;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * A {@link FormatterFactory} implementation that corresponds to {@code <jackson>...</jackson>} configuration element.
 */
public class JacksonJson implements FormatterStepFactory {

	@Parameter
	private String version = JacksonJsonStep.defaultVersion();

	@Parameter
	private boolean spaceBeforeSeparator = new JacksonJsonConfig().isSpaceBeforeSeparator();

	@Parameter
	private Map<String, Boolean> features = emptyMap();

	@Parameter
	private Map<String, Boolean> jsonFeatures = emptyMap();

	@Override
	public FormatterStep newFormatterStep(FormatterStepConfig stepConfig) {
		JacksonJsonConfig jacksonConfig = new JacksonJsonConfig();

		jacksonConfig.appendFeatureToToggle(features);
		jacksonConfig.appendJsonFeatureToToggle(jsonFeatures);
		jacksonConfig.setSpaceBeforeSeparator(spaceBeforeSeparator);

		return JacksonJsonStep
				.create(jacksonConfig, version, stepConfig.getProvisioner());
	}
}
