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

import static com.diffplug.gradle.spotless.PluginGradlePreconditions.requireElementsNonNull;

import javax.inject.Inject;

import org.gradle.api.Project;

import com.diffplug.spotless.cpp.CppDefaults;
import com.diffplug.spotless.extra.EquoBasedStepBuilder;
import com.diffplug.spotless.extra.cpp.EclipseCdtFormatterStep;

public class CppExtension extends FormatExtension implements HasBuiltinDelimiterForLicense {
	static final String NAME = "cpp";

	@Inject
	public CppExtension(SpotlessExtension spotless) {
		super(spotless);
	}

	public EclipseConfig eclipseCdt() {
		return eclipseCdt(EclipseCdtFormatterStep.defaultVersion());
	}

	public EclipseConfig eclipseCdt(String version) {
		return new EclipseConfig(version);
	}

	public class EclipseConfig {
		private final EquoBasedStepBuilder builder;

		EclipseConfig(String version) {
			builder = EclipseCdtFormatterStep.createBuilder(provisioner());
			builder.setVersion(version);
			addStep(builder.build());
		}

		public void configFile(Object... configFiles) {
			requireElementsNonNull(configFiles);
			Project project = getProject();
			builder.setPreferences(project.files(configFiles).getFiles());
			replaceStep(builder.build());
		}
	}

	@Override
	protected void setupTask(SpotlessTask task) {
		if (target == null) {
			throw noDefaultTargetException();
		}
		super.setupTask(task);
	}

	@Override
	public LicenseHeaderConfig licenseHeader(String licenseHeader) {
		return licenseHeader(licenseHeader, CppDefaults.DELIMITER_EXPR);
	}

	@Override
	public LicenseHeaderConfig licenseHeaderFile(Object licenseHeaderFile) {
		return licenseHeaderFile(licenseHeaderFile, CppDefaults.DELIMITER_EXPR);
	}
}
