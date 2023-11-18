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

import static com.diffplug.spotless.kotlin.KotlinConstants.LICENSE_HEADER_DELIMITER;

import javax.inject.Inject;

import org.gradle.api.tasks.SourceSet;

public class KotlinExtension extends BaseKotlinExtension implements HasBuiltinDelimiterForLicense, JvmLang {
	static final String NAME = "kotlin";

	@Inject
	public KotlinExtension(SpotlessExtension spotless) {
		super(spotless);
	}

	@Override
	public LicenseHeaderConfig licenseHeader(String licenseHeader) {
		return licenseHeader(licenseHeader, LICENSE_HEADER_DELIMITER);
	}

	@Override
	public LicenseHeaderConfig licenseHeaderFile(Object licenseHeaderFile) {
		return licenseHeaderFile(licenseHeaderFile, LICENSE_HEADER_DELIMITER);
	}

	@Override
	protected boolean isScript() {
		return false;
	}

	/** If the user hasn't specified the files yet, we'll assume he/she means all of the kotlin files. */
	@Override
	protected void setupTask(SpotlessTask task) {
		if (target == null) {
			target = getSources(getProject(),
					"You must either specify 'target' manually or apply a kotlin plugin.",
					SourceSet::getAllSource,
					file -> {
						final String name = file.getName();
						return name.endsWith(".kt") || name.endsWith(".kts");
					});
		}
		super.setupTask(task);
	}
}
