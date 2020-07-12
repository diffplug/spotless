/*
 * Copyright 2016-2020 DiffPlug
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

import java.util.Objects;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.extra.EclipseBasedStepBuilder;
import com.diffplug.spotless.extra.java.EclipseJdtFormatterStep;
import com.diffplug.spotless.generic.LicenseHeaderStep;
import com.diffplug.spotless.java.GoogleJavaFormatStep;
import com.diffplug.spotless.java.ImportOrderStep;
import com.diffplug.spotless.java.RemoveUnusedImportsStep;

public class JavaExtension extends FormatExtension implements HasBuiltinDelimiterForLicense {
	static final String NAME = "java";

	public JavaExtension(SpotlessExtension spotless) {
		super(spotless);
	}

	// If this constant changes, don't forget to change the similarly-named one in
	// testlib/src/test/java/com/diffplug/spotless/generic/LicenseHeaderStepTest.java as well
	static final String LICENSE_HEADER_DELIMITER = "package ";

	@Override
	public LicenseHeaderConfig licenseHeader(String licenseHeader) {
		return licenseHeader(licenseHeader, LICENSE_HEADER_DELIMITER);
	}

	@Override
	public LicenseHeaderConfig licenseHeaderFile(Object licenseHeaderFile) {
		return licenseHeaderFile(licenseHeaderFile, LICENSE_HEADER_DELIMITER);
	}

	public void importOrder(String... importOrder) {
		addStep(ImportOrderStep.forJava().createFrom(importOrder));
	}

	public void importOrderFile(Object importOrderFile) {
		Objects.requireNonNull(importOrderFile);
		addStep(ImportOrderStep.forJava().createFrom(getProject().file(importOrderFile)));
	}

	/** Removes any unused imports. */
	public void removeUnusedImports() {
		addStep(RemoveUnusedImportsStep.create(provisioner()));
	}

	/** Uses the [google-java-format](https://github.com/google/google-java-format) jar to format source code. */
	public GoogleJavaFormatConfig googleJavaFormat() {
		return googleJavaFormat(GoogleJavaFormatStep.defaultVersion());
	}

	/**
	 * Uses the given version of [google-java-format](https://github.com/google/google-java-format) to format source code.
	 *
	 * Limited to published versions.  See [issue #33](https://github.com/diffplug/spotless/issues/33#issuecomment-252315095)
	 * for an workaround for using snapshot versions.
	 */
	public GoogleJavaFormatConfig googleJavaFormat(String version) {
		Objects.requireNonNull(version);
		return new GoogleJavaFormatConfig(version);
	}

	public class GoogleJavaFormatConfig {
		final String version;
		String style;

		GoogleJavaFormatConfig(String version) {
			this.version = Objects.requireNonNull(version);
			this.style = GoogleJavaFormatStep.defaultStyle();
			addStep(createStep());
		}

		public void style(String style) {
			this.style = Objects.requireNonNull(style);
			replaceStep(createStep());
		}

		public void aosp() {
			style("AOSP");
		}

		private FormatterStep createStep() {
			return GoogleJavaFormatStep.create(version,
					style,
					provisioner());
		}
	}

	public EclipseConfig eclipse() {
		return new EclipseConfig(EclipseJdtFormatterStep.defaultVersion());
	}

	public EclipseConfig eclipse(String version) {
		return new EclipseConfig(version);
	}

	public class EclipseConfig {
		private final EclipseBasedStepBuilder builder;

		EclipseConfig(String version) {
			builder = EclipseJdtFormatterStep.createBuilder(provisioner());
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

	/** If the user hasn't specified the files yet, we'll assume he/she means all of the java files. */
	@Override
	protected void setupTask(SpotlessTask task) {
		if (target == null) {
			JavaPluginConvention javaPlugin = getProject().getConvention().findPlugin(JavaPluginConvention.class);
			if (javaPlugin == null) {
				throw new GradleException("You must either specify 'target' manually or apply the 'java' plugin.");
			}
			FileCollection union = getProject().files();
			for (SourceSet sourceSet : javaPlugin.getSourceSets()) {
				union = union.plus(sourceSet.getAllJava());
			}
			target = union;
		}

		steps.replaceAll(step -> {
			if (LicenseHeaderStep.name().equals(step.getName())) {
				return step.filterByFile(LicenseHeaderStep.unsupportedJvmFilesFilter());
			} else {
				return step;
			}
		});
		super.setupTask(task);
	}
}
