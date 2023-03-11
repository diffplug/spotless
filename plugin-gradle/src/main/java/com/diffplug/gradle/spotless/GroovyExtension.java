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

import static com.diffplug.gradle.spotless.PluginGradlePreconditions.requireElementsNonNull;

import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.plugins.GroovyBasePlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.GroovySourceDirectorySet;
import org.gradle.api.tasks.GroovySourceSet;
import org.gradle.api.tasks.SourceSet;
import org.gradle.util.GradleVersion;

import com.diffplug.spotless.extra.EquoBasedStepBuilder;
import com.diffplug.spotless.extra.groovy.GrEclipseFormatterStep;
import com.diffplug.spotless.generic.LicenseHeaderStep;
import com.diffplug.spotless.java.ImportOrderStep;

public class GroovyExtension extends FormatExtension implements HasBuiltinDelimiterForLicense {
	static final String NAME = "groovy";

	@Inject
	public GroovyExtension(SpotlessExtension spotless) {
		super(spotless);
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

	public void importOrder(String... importOrder) {
		addStep(ImportOrderStep.forGroovy().createFrom(importOrder));
	}

	public void importOrderFile(Object importOrderFile) {
		Objects.requireNonNull(importOrderFile);
		addStep(ImportOrderStep.forGroovy().createFrom(getProject().file(importOrderFile)));
	}

	public GrEclipseConfig greclipse() {
		return greclipse(GrEclipseFormatterStep.defaultVersion());
	}

	public GrEclipseConfig greclipse(String version) {
		return new GrEclipseConfig(version, this);
	}

	public static class GrEclipseConfig {
		private final EquoBasedStepBuilder builder;
		private final FormatExtension extension;

		GrEclipseConfig(String version, FormatExtension extension) {
			this.extension = extension;
			builder = GrEclipseFormatterStep.createBuilder(extension.provisioner());
			builder.setVersion(version);
			extension.addStep(builder.build());
		}

		public void configFile(Object... configFiles) {
			requireElementsNonNull(configFiles);
			Project project = extension.getProject();
			builder.setPreferences(project.files(configFiles).getFiles());
			extension.replaceStep(builder.build());
		}

		public GrEclipseConfig withP2Mirrors(Map<String, String> mirrors) {
			builder.setP2Mirrors(mirrors);
			extension.replaceStep(builder.build());
			return this;
		}
	}

	/** If the user hasn't specified the files yet, we'll assume he/she means all of the groovy files. */
	@Override
	protected void setupTask(SpotlessTask task) {
		if (target == null) {
			if (GradleVersion.current().compareTo(GradleVersion.version("7.1")) >= 0) {
				JavaPluginExtension javaPluginExtension = getProject().getExtensions().findByType(JavaPluginExtension.class);
				if (javaPluginExtension == null || !getProject().getPlugins().hasPlugin(GroovyBasePlugin.class)) {
					throw new GradleException("You must apply the groovy plugin before the spotless plugin if you are using the groovy extension.");
				}
				//Add all Groovy files (may contain Java files as well)

				FileCollection union = getProject().files();
				for (SourceSet sourceSet : javaPluginExtension.getSourceSets()) {
					union = union.plus(sourceSet.getExtensions().getByType(GroovySourceDirectorySet.class).filter(file -> {
						String name = file.getName();
						if (excludeJava) {
							return name.endsWith(".groovy");
						} else {
							return name.endsWith(".groovy") || name.endsWith(".java");
						}
					}));
				}
				target = union;
			} else {
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
			}
		} else if (excludeJava) {
			throw new IllegalArgumentException("'excludeJava' is not supported in combination with a custom 'target'.");
		}
		// LicenseHeaderStep completely blows apart package-info.java/groovy - this common-sense check
		// ensures that it skips both. See https://github.com/diffplug/spotless/issues/1
		steps.replaceAll(step -> {
			if (isLicenseHeaderStep(step)) {
				return step.filterByFile(LicenseHeaderStep.unsupportedJvmFilesFilter());
			} else {
				return step;
			}
		});
		super.setupTask(task);
	}
}
