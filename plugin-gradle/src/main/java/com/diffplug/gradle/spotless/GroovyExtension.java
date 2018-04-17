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
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.plugins.GroovyBasePlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.GroovySourceSet;
import org.gradle.api.tasks.SourceSet;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.SerializableFileFilter;
import com.diffplug.spotless.extra.groovy.GrEclipseFormatterStep;
import com.diffplug.spotless.generic.LicenseHeaderStep;
import com.diffplug.spotless.java.ImportOrderStep;

public class GroovyExtension extends FormatExtension implements SimpleLicenseHeaderExtension {
	static final String NAME = "groovy";

	public GroovyExtension(SpotlessExtension rootExtension) {
		super(rootExtension);
	}

	boolean excludeJava = false;

	/** Excludes .java files, to focus on only .groovy files. */
	public void excludeJava() {
		excludeJava(true);
	}

	/** Determines whether to exclude .java files, to focus on only .groovy files. */
	public void excludeJava(boolean excludeJava) {
		this.excludeJava = excludeJava;
	}

	@Override
	public LicenseHeaderConfig licenseHeader(String licenseHeader) {
		return licenseHeader(licenseHeader, JavaExtension.LICENSE_HEADER_DELIMITER);
	}

	@Override
	public LicenseHeaderConfig licenseHeaderFile(Object licenseHeaderFile) {
		return licenseHeaderFile(licenseHeaderFile, JavaExtension.LICENSE_HEADER_DELIMITER);
	}

	/** Method interface has been changed to
	 * {@link GroovyExtension#importOrder(String...)}.*/
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

	public GrEclipseConfig greclipse() {
		return greclipse(GrEclipseFormatterStep.defaultVersion());
	}

	public GrEclipseConfig greclipse(String version) {
		return new GrEclipseConfig(version, this);
	}

	public static class GrEclipseConfig {
		final String version;
		Object[] configFiles;
		final FormatExtension extension;

		GrEclipseConfig(String version, FormatExtension extension) {
			this.extension = extension;
			configFiles = new Object[0];
			this.version = Objects.requireNonNull(version);

			extension.addStep(createStep());
		}

		public void configFile(Object... configFiles) {
			this.configFiles = requireElementsNonNull(configFiles);
			extension.replaceStep(createStep());
		}

		private FormatterStep createStep() {
			Project project = extension.getProject();
			return GrEclipseFormatterStep.create(version,
					project.files(configFiles).getFiles(),
					GradleProvisioner.fromProject(project));
		}
	}

	/** If the user hasn't specified the files yet, we'll assume he/she means all of the groovy files. */
	@Override
	protected void setupTask(SpotlessTask task) {
		if (target == null) {
			JavaPluginConvention convention = getProject().getConvention().getPlugin(JavaPluginConvention.class);
			if (convention == null || !getProject().getPlugins().hasPlugin(GroovyBasePlugin.class)) {
				throw new GradleException("You must apply the groovy plugin before the spotless plugin if you are using the groovy extension.");
			}
			//Add all Groovy files (may contain Java files as well)

			FileCollection union = getProject().files();
			for (SourceSet sourceSet : convention.getSourceSets()) {
				GroovySourceSet groovySourceSet = new DslObject(sourceSet).getConvention().getPlugin(GroovySourceSet.class);
				if (excludeJava) {
					union = union.plus(groovySourceSet.getAllGroovy());
				} else {
					union = union.plus(groovySourceSet.getGroovy());
				}
			}
			target = union;
		} else if (excludeJava) {
			throw new IllegalArgumentException("'excludeJava' is not supported in combination with a custom 'target'.");
		}
		// LicenseHeaderStep completely blows apart package-info.java/groovy - this common-sense check
		// ensures that it skips both. See https://github.com/diffplug/spotless/issues/1
		steps.replaceAll(step -> {
			if (LicenseHeaderStep.name().equals(step.getName())) {
				return step.filterByFile(SerializableFileFilter.skipFilesNamed("package-info.java", "package-info.groovy"));
			} else {
				return step;
			}
		});
		super.setupTask(task);
	}
}
