/*
 * Copyright 2021-2025 DiffPlug
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
package com.diffplug.spotless.maven.markdown;

import java.util.List;

import org.apache.maven.plugins.annotations.Parameter;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.markdown.FlexmarkConfig;
import com.diffplug.spotless.markdown.FlexmarkStep;
import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.FormatterStepFactory;

public class Flexmark implements FormatterStepFactory {

	@Parameter
	private String version;
	@Parameter
	private String emulationProfile;

	@Parameter
	private String pegdownExtensions;
	@Parameter
	private String extensions;

	@Override
	public FormatterStep newFormatterStep(FormatterStepConfig config) {
		String version = this.version != null ? this.version : FlexmarkStep.defaultVersion();
		FlexmarkConfig flexmarkConfig = new FlexmarkConfig();
		if (this.emulationProfile != null) {
			flexmarkConfig.setEmulationProfile(this.emulationProfile);
		}
		if (this.pegdownExtensions != null) {
			flexmarkConfig.setPegdownExtensions(List.of(this.pegdownExtensions.split(",")));
		}
		if (this.extensions != null) {
			flexmarkConfig.setExtensions(List.of(this.extensions.split(",")));
		}
		return FlexmarkStep.create(version, config.getProvisioner(), flexmarkConfig);
	}
}
