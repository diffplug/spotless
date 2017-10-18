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

import static com.diffplug.gradle.spotless.PluginGradlePreconditions.requireElementsNonNull;

import java.util.List;
import java.util.Objects;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.SerializableFileFilter;
import com.diffplug.spotless.extra.java.EclipseFormatterStep;
import com.diffplug.spotless.generic.LicenseHeaderStep;
import com.diffplug.spotless.java.GoogleJavaFormatStep;
import com.diffplug.spotless.java.ImportOrderStep;
import com.diffplug.spotless.java.RemoveUnusedImportsStep;

public class JavaExtension extends FormatExtension {
	static final String NAME = "java";

	public JavaExtension(SpotlessExtension rootExtension) {
		super(rootExtension);
	}

	// If this constant changes, don't forget to change the similarly-named one in
	// testlib/src/test/java/com/diffplug/spotless/generic/LicenseHeaderStepTest.java as well
	static final String LICENSE_HEADER_DELIMITER = "package ";

	public void licenseHeader(String licenseHeader) {
		licenseHeader(licenseHeader, LICENSE_HEADER_DELIMITER);
	}

	public void licenseHeaderFile(Object licenseHeaderFile) {
		licenseHeaderFile(licenseHeaderFile, LICENSE_HEADER_DELIMITER);
	}

	/** Method interface has been changed to
	 * {@link JavaExtension#importOrder(String...)}.*/
	@Deprecated
	public void importOrder(List<String> importOrder) {
		getProject().getLogger().warn(
				StringPrinter.buildStringFromLines(
						"'importOrder([x, y, z])' is deprecated.",
						"Use 'importOrder x, y, z' instead.",
						"For details see https://github.com/diffplug/spotless/tree/master/plugin-gradle#applying-to-java-source"));
		addStep(ImportOrderStep.createFromOrder(importOrder));
	}

	public void importOrder(String... importOrder) {
		addStep(ImportOrderStep.createFromOrder(importOrder));
	}

	public void importOrderFile(Object importOrderFile) {
		Objects.requireNonNull(importOrderFile);
		addStep(ImportOrderStep.createFromFile(getProject().file(importOrderFile)));
	}

	/** Use {@link #eclipse()} instead */
	@Deprecated
	public void eclipseFormatFile(Object eclipseFormatFile) {
		eclipseFormatFile(EclipseFormatterStep.defaultVersion(), eclipseFormatFile);
	}

	/** Use {@link #eclipse(String)} instead */
	@Deprecated
	public void eclipseFormatFile(String eclipseVersion, Object eclipseFormatFile) {
		Objects.requireNonNull(eclipseVersion, "eclipseVersion");
		Objects.requireNonNull(eclipseFormatFile, "eclipseFormatFile");
		getProject().getLogger().warn(
				StringPrinter.buildStringFromLines(
						"'eclipseFormatFile [version] <file>' is deprecated.",
						"Use 'eclipse([version]).configFile(<file>)' instead.",
						"For details see https://github.com/diffplug/spotless/tree/master/plugin-gradle#applying-to-java-source"));
		eclipse(eclipseVersion).configFile(eclipseFormatFile);
	}

	/** Removes any unused imports. */
	public void removeUnusedImports() {
		addStep(RemoveUnusedImportsStep.create(GradleProvisioner.fromProject(getProject())));
	}

	/** Uses the [google-java-format](https://github.com/google/google-java-format) jar to format source code. */
	public void googleJavaFormat() {
		googleJavaFormat(GoogleJavaFormatStep.defaultVersion());
	}

	/**
	 * Uses the given version of [google-java-format](https://github.com/google/google-java-format) to format source code.
	 *
	 * Limited to published versions.  See [issue #33](https://github.com/diffplug/spotless/issues/33#issuecomment-252315095)
	 * for an workaround for using snapshot versions.
	 */
	public void googleJavaFormat(String version) {
		Objects.requireNonNull(version);
		addStep(GoogleJavaFormatStep.create(version, GradleProvisioner.fromProject(getProject())));
	}

	public EclipseConfig eclipse() {
		return eclipse(EclipseFormatterStep.defaultVersion());
	}

	public EclipseConfig eclipse(String version) {
		return new EclipseConfig(version);
	}

	public class EclipseConfig {
		final String version;
		Object[] configFiles;

		EclipseConfig(String version) {
			configFiles = new Object[0];
			this.version = Objects.requireNonNull(version);
			addStep(createStep());
		}

		public void configFile(Object... configFiles) {
			this.configFiles = requireElementsNonNull(configFiles);
			replaceStep(createStep());
		}

		private FormatterStep createStep() {
			Project project = getProject();
			return EclipseFormatterStep.create(version,
					project.files(configFiles).getFiles(),
					GradleProvisioner.fromProject(project));
		}
	}

	/** If the user hasn't specified the files yet, we'll assume he/she means all of the java files. */
	@Override
	protected void setupTask(SpotlessTask task) {
		if (target == null) {
			JavaPluginConvention javaPlugin = getProject().getConvention().findPlugin(JavaPluginConvention.class);
			if (javaPlugin == null) {
				throw new GradleException("You must apply the java plugin before the spotless plugin if you are using the java extension.");
			}
			FileCollection union = getProject().files();
			for (SourceSet sourceSet : javaPlugin.getSourceSets()) {
				union = union.plus(sourceSet.getAllJava());
			}
			target = union;
		}
		// LicenseHeaderStep completely blows apart package-info.java - this common-sense check ensures that
		// it skips package-info.java. See https://github.com/diffplug/spotless/issues/1
		steps.replaceAll(step -> {
			if (LicenseHeaderStep.name().equals(step.getName())) {
				return step.filterByFile(SerializableFileFilter.skipFilesNamed("package-info.java"));
			} else {
				return step;
			}
		});
		super.setupTask(task);
	}
}
