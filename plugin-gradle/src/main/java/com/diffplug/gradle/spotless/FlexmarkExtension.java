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

import java.util.Objects;

import javax.inject.Inject;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.markdown.FlexmarkStep;

public class FlexmarkExtension extends FormatExtension implements HasBuiltinDelimiterForLicense {
	static final String NAME = "flexmark";

	@Inject
	public FlexmarkExtension(SpotlessExtension spotless) {
		super(spotless);
	}

	@Override
	public LicenseHeaderConfig licenseHeader(String licenseHeader) {
		return licenseHeader(licenseHeader, null);
	}

	@Override
	public LicenseHeaderConfig licenseHeaderFile(Object licenseHeaderFile) {
		return licenseHeaderFile(licenseHeaderFile, null);
	}

	public FlexmarkFormatterConfig flexmarkFormatter() {
		return flexmarkFormatter(FlexmarkStep.defaultVersion());
	}

	public FlexmarkFormatterConfig flexmarkFormatter(String version) {
		return new FlexmarkFormatterConfig(version);
	}

	@Override
	protected void setupTask(SpotlessTask task) {
		// defaults to all markdown files
		if (target == null) {
			throw noDefaultTargetException();
		}
		super.setupTask(task);
	}

	public class FlexmarkFormatterConfig {

		private final String version;

		FlexmarkFormatterConfig(String version) {
			this.version = Objects.requireNonNull(version);
			addStep(createStep());
		}

		private FormatterStep createStep() {
			return FlexmarkStep.create(this.version, provisioner());
		}
	}

}
