/*
 * Copyright 2016 DiffPlug
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

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.kotlin.KtLintStep;

public class KotlinExtension extends FormatExtension implements HasBuiltinDelimiterForLicense {
	static final String NAME = "kotlin";

	public KotlinExtension(SpotlessExtension rootExtension) {
		super(rootExtension);
	}

	@Override
	public LicenseHeaderConfig licenseHeader(String licenseHeader) {
		return licenseHeader(licenseHeader, LICENSE_HEADER_DELIMITER);
	}

	@Override
	public LicenseHeaderConfig licenseHeaderFile(Object licenseHeaderFile) {
		return licenseHeaderFile(licenseHeaderFile, LICENSE_HEADER_DELIMITER);
	}

	/** Adds the specified version of [ktlint](https://github.com/shyiko/ktlint). */
	public KotlinFormatExtension ktlint(String version) {
		Objects.requireNonNull(version);
		return new KotlinFormatExtension(version, Collections.emptyMap());
	}

	public KotlinFormatExtension ktlint() {
		return ktlint(KtLintStep.defaultVersion());
	}

	public class KotlinFormatExtension {

		private final String version;
		private Map<String, String> userData;

		KotlinFormatExtension(String version, Map<String, String> config) {
			this.version = version;
			this.userData = config;
			addStep(createStep());
		}

		public void userData(Map<String, String> userData) {
			// Copy the map to a sorted map because up-to-date checking is based on binary-equals of the serialized
			// representation.
			this.userData = userData;
			replaceStep(createStep());
		}

		private FormatterStep createStep() {
			return KtLintStep.create(version, GradleProvisioner.fromProject(getProject()), userData);
		}
	}

	/** If the user hasn't specified the files yet, we'll assume he/she means all of the kotlin files. */
	@Override
	protected void setupTask(SpotlessTask task) {
		if (target == null) {
			JavaPluginConvention javaPlugin = getProject().getConvention().findPlugin(JavaPluginConvention.class);
			if (javaPlugin == null) {
				throw new GradleException("You must either specify 'target' manually or apply a kotlin plugin.");
			}
			FileCollection union = getProject().files();
			for (SourceSet sourceSet : javaPlugin.getSourceSets()) {
				union = union.plus(sourceSet.getAllSource().filter(file -> {
					String name = file.getName();
					return name.endsWith(".kt") || name.endsWith(".kts");
				}));
			}
			target = union;
		}
		super.setupTask(task);
	}
}
