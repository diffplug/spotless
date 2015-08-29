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

import org.gradle.api.GradleException;
import org.gradle.api.internal.file.UnionFileCollection;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

import com.diffplug.gradle.spotless.FormatExtension;
import com.diffplug.gradle.spotless.FormatTask;
import com.diffplug.gradle.spotless.FormattingOperation;
import com.diffplug.gradle.spotless.FormattingOperationSupplier;
import com.diffplug.gradle.spotless.SpotlessExtension;

import java.io.IOException;
import java.util.List;

public class JavaExtension extends FormatExtension {
	public static final String LICENSE_HEADER_DELIMITER = "package ";
	private static final String NAME = "java";

	public JavaExtension(SpotlessExtension rootExtension) {
		super(NAME, rootExtension);
	}

	public void licenseHeader(String licenseHeader) {
		licenseHeader(licenseHeader, LICENSE_HEADER_DELIMITER);
	}

	public void licenseHeaderFile(Object licenseHeaderFile) {
		licenseHeaderFile(licenseHeaderFile, LICENSE_HEADER_DELIMITER);
	}

	public void importOrder(final List<String> importOrder) {
		customLazy(ImportSorterStep.NAME, new FormattingOperationSupplier(new FormattingOperation() {
			ImportSorterStep step;

			@Override
			public String apply(String raw) {
				return step.format(raw);
			}

			@Override
			public void init() {
				step = new ImportSorterStep(importOrder);
			}
		}));
	}

	public void importOrderFile(final Object importOrderFile) {
		customLazy(ImportSorterStep.NAME, new FormattingOperationSupplier(new FormattingOperation() {
			ImportSorterStep step;

			@Override
			public String apply(String raw) {
				return step.format(raw);
			}

			@Override
			public void init() throws IOException {
				step = new ImportSorterStep(getProject().file(importOrderFile));
			}
		}));
	}

	public void eclipseFormatFile(final Object eclipseFormatFile) {
		customLazy(EclipseFormatterStep.NAME, new FormattingOperationSupplier(new FormattingOperation() {
			EclipseFormatterStep step;

			@Override
			public String apply(String raw) throws Exception {
				return step.format(raw);
			}

			@Override
			public void init() throws Exception {
				step = EclipseFormatterStep.load(getProject().file(eclipseFormatFile));
			}
		}));
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
