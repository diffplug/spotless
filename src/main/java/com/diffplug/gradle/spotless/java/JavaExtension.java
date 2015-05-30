/*
 * Copyright 2015 DiffPlug
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

import com.diffplug.gradle.spotless.FormatExtension;
import com.diffplug.gradle.spotless.FormatTask;
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

	/** If the user hasn't specified the files yet, we'll assume he/she means all of the java files. */
	@Override
	protected void setupTask(FormatTask task) throws Exception {
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
		super.setupTask(task);
	}
}
