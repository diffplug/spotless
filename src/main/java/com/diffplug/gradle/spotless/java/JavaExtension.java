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
package com.diffplug.gradle.spotless.java;

import java.util.List;

import org.gradle.api.GradleException;
import org.gradle.api.internal.file.UnionFileCollection;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

import com.diffplug.common.base.Errors;
import com.diffplug.gradle.spotless.BaseFormatTask;
import com.diffplug.gradle.spotless.FormatExtension;
import com.diffplug.gradle.spotless.FormatterStep;
import com.diffplug.gradle.spotless.LicenseHeaderStep;
import com.diffplug.gradle.spotless.SpotlessExtension;

public class JavaExtension extends FormatExtension {
	public static final String NAME = "java";

	public JavaExtension(SpotlessExtension rootExtension) {
		super(NAME, rootExtension);
	}

	public static final String LICENSE_HEADER_DELIMITER = "package ";

	public void licenseHeader(String licenseHeader) {
		licenseHeader(licenseHeader, LICENSE_HEADER_DELIMITER);
	}

	public void licenseHeaderFile(Object licenseHeaderFile) {
		licenseHeaderFile(licenseHeaderFile, LICENSE_HEADER_DELIMITER);
	}

	public void importOrder(List<String> importOrder) {
		customLazy(ImportSorterStep.NAME, () -> new ImportSorterStep(importOrder)::format);
	}

	public void importOrderFile(Object importOrderFile) {
		customLazy(ImportSorterStep.NAME, () -> new ImportSorterStep(getProject().file(importOrderFile))::format);
	}

	public void eclipseFormatFile(Object eclipseFormatFile) {
		customLazy(EclipseFormatterStep.NAME, () -> EclipseFormatterStep.load(getProject().file(eclipseFormatFile))::format);
	}

	/** Uses the [google-java-format](https://github.com/google/google-java-format) jar to format source code. */
	public void googleJavaFormat() {
		googleJavaFormat(GoogleJavaFormat.DEFAULT_VERSION);
	}

	/**
	 * Uses the given version of [google-java-format](https://github.com/google/google-java-format) to format source code.
	 *
	 * Limited to published versions.  See [issue #33](https://github.com/diffplug/spotless/issues/33#issuecomment-252315095)
	 * for an workaround for using snapshot versions.
	 */
	public void googleJavaFormat(String version) {
		addStep(FormatterStep.create(GoogleJavaFormat.NAME,
				version,
				(key, input) -> {
					try {
						return GoogleJavaFormat.createRule(key, getProject()).apply(input);
					} catch (Exception e) {
						throw Errors.asRuntime(e);
					}
				}));
	}

	/** If the user hasn't specified the files yet, we'll assume he/she means all of the java files. */
	@Override
	protected void setupTask(BaseFormatTask task) {
		if (target == null) {
			JavaPluginConvention javaPlugin = getProject().getConvention().getPlugin(JavaPluginConvention.class);
			if (javaPlugin == null) {
				throw new GradleException("You must apply the java plugin before the spotless plugin if you are using the java extension.");
			}
			UnionFileCollection union = new UnionFileCollection();
			for (SourceSet sourceSet : javaPlugin.getSourceSets()) {
				union.add(sourceSet.getJava());
			}
			target = union;
		}
		// LicenseHeaderStep completely blows apart package-info.java - this common-sense check ensures that
		// it skips package-info.java. See https://github.com/diffplug/spotless/issues/1
		steps.replaceAll(step -> {
			if (LicenseHeaderStep.NAME.equals(step.getName())) {
				return step.filterByFile(file -> !file.getName().equals("package-info.java"));
			} else {
				return step;
			}
		});
		super.setupTask(task);
	}
}
