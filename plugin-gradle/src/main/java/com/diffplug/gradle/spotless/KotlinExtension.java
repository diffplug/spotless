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

import java.util.Objects;

import org.gradle.api.GradleException;
import org.gradle.api.internal.file.UnionFileCollection;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

import com.diffplug.spotless.kotlin.KtLintStep;

public class KotlinExtension extends FormatExtension {
	// '^' is prepended to the regex in LICENSE_HEADER_DELIMITER later in FormatExtension.licenseHeader(String, String)
	private static final String LICENSE_HEADER_DELIMITER = "(package |@file)";
	static final String NAME = "kotlin";

	public KotlinExtension(SpotlessExtension rootExtension) {
		super(rootExtension);
	}

	public void licenseHeader(String licenseHeader) {
		licenseHeader(licenseHeader, LICENSE_HEADER_DELIMITER);
	}

	public void licenseHeaderFile(Object licenseHeaderFile) {
		licenseHeaderFile(licenseHeaderFile, LICENSE_HEADER_DELIMITER);
	}

	/** Adds the specified version of [ktlint](https://github.com/shyiko/ktlint). */
	public void ktlint(String version) {
		Objects.requireNonNull(version);
		addStep(KtLintStep.create(version, GradleProvisioner.fromProject(getProject())));
	}

	public void ktlint() {
		ktlint(KtLintStep.defaultVersion());
	}

	/** If the user hasn't specified the files yet, we'll assume he/she means all of the kotlin files. */
	@Override
	protected void setupTask(SpotlessTask task) {
		if (target == null) {
			JavaPluginConvention javaPlugin = getProject().getConvention().findPlugin(JavaPluginConvention.class);
			if (javaPlugin == null) {
				throw new GradleException("You must either specify 'target' manually or apply a kotlin plugin.");
			}
			UnionFileCollection union = new UnionFileCollection();
			for (SourceSet sourceSet : javaPlugin.getSourceSets()) {
				union.add(sourceSet.getAllSource().filter(file -> {
					String name = file.getName();
					return name.endsWith(".kt") || name.endsWith(".kts");
				}));
			}
			target = union;
		}
		super.setupTask(task);
	}
}
