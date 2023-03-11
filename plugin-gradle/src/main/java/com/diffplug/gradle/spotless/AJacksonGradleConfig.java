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
package com.diffplug.gradle.spotless;

import java.util.Collections;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.json.JacksonConfig;
import com.diffplug.spotless.json.JacksonJsonStep;

public abstract class AJacksonGradleConfig {
	protected final FormatExtension formatExtension;

	protected final JacksonConfig jacksonConfig;

	protected String version = JacksonJsonStep.defaultVersion();

	// Make sure to call 'formatExtension.addStep(createStep());' in the extented constructors
	public AJacksonGradleConfig(JacksonConfig jacksonConfig, FormatExtension formatExtension) {
		this.formatExtension = formatExtension;

		this.jacksonConfig = jacksonConfig;
	}

	public AJacksonGradleConfig feature(String feature, boolean toggle) {
		this.jacksonConfig.appendFeatureToToggle(Collections.singletonMap(feature, toggle));
		formatExtension.replaceStep(createStep());
		return this;
	}

	public AJacksonGradleConfig version(String version) {
		this.version = version;
		formatExtension.replaceStep(createStep());
		return this;
	}

	protected abstract FormatterStep createStep();
}
