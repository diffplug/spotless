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

import org.gradle.api.GradleException;
import org.gradle.api.internal.file.UnionFileCollection;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

import com.diffplug.spotless.kotlin.KtLintFormatStep;

public class KotlinExtension extends FormatExtension {
	static final String NAME = "kotin";

	public KotlinExtension(SpotlessExtension rootExtension) {
		super(rootExtension);
	}

	private static final String LICENSE_HEADER_DELIMITER = "package ";

	public void licenseHeader(String licenseHeader) {
		licenseHeader(licenseHeader, LICENSE_HEADER_DELIMITER);
	}

	public void licenseHeaderFile(Object licenseHeaderFile) {
		licenseHeaderFile(licenseHeaderFile, LICENSE_HEADER_DELIMITER);
	}

	/** Adds the specified version of [ktlint](https://github.com/shyiko/ktlint). */
	public void ktlint(String version) {
		addStep(KtLintFormatStep.create(version, GradleProvisioner.fromProject(getProject())));
	}

	public void ktlint() {
		ktlint(KtLintFormatStep.defaultVersion());
	}

	/** If the user hasn't specified the files yet, we'll assume he/she means all of the kotlin files. */
	@Override
	protected void setupTask(SpotlessTask task) {
		if (target == null) {
			JavaPluginConvention javaPlugin = getProject().getConvention().findPlugin(JavaPluginConvention.class);
			if (javaPlugin == null) {
				throw new GradleException("You must apply the java plugin before the spotless plugin if you are using the java extension.");
			}
			UnionFileCollection union = new UnionFileCollection();
			for (SourceSet sourceSet : javaPlugin.getSourceSets()) {
				if (sourceSet.getClass().getSimpleName().startsWith("Kotlin")) {
					union.add(sourceSet.getAllSource());
				}
			}
			target = union;
		}
		super.setupTask(task);
	}
}
