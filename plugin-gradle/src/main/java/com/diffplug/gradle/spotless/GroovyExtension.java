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

import java.util.List;
import java.util.Objects;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.internal.file.UnionFileCollection;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.GroovySourceSet;
import org.gradle.api.tasks.SourceSet;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.SerializableFileFilter;
import com.diffplug.spotless.extra.groovy.GrEclipseFormatterStep;
import com.diffplug.spotless.generic.LicenseHeaderStep;
import com.diffplug.spotless.java.ImportOrderStep;

public class GroovyExtension extends FormatExtension {
	static final String NAME = "groovy";
	private static final boolean EXCLUDE_JAVA_DEFAULT = false;
	private boolean excludeJava;

	public GroovyExtension(SpotlessExtension rootExtension) {
		super(rootExtension);
		excludeJava = EXCLUDE_JAVA_DEFAULT;
	}

	void excludeJava() {
		excludeJava = !EXCLUDE_JAVA_DEFAULT;
	}

	public boolean getExcludeJava() {
		//getExcludeJava only used for DSL convenience so that braces can be omitted.
		excludeJava();
		return excludeJava;
	}

	public static final String LICENSE_HEADER_DELIMITER = "package ";

	public void licenseHeader(String licenseHeader) {
		licenseHeader(licenseHeader, LICENSE_HEADER_DELIMITER);
	}

	public void licenseHeaderFile(Object licenseHeaderFile) {
		licenseHeaderFile(licenseHeaderFile, LICENSE_HEADER_DELIMITER);
	}

	public void importOrder(List<String> importOrder) {
		addStep(ImportOrderStep.createFromOrder(importOrder));
	}

	public void importOrderFile(Object importOrderFile) {
		addStep(ImportOrderStep.createFromFile(getProject().file(importOrderFile)));
	}

	public GrEclipseFormatConfig greclipseFormat() {
		return greclipseFormat(GrEclipseFormatterStep.defaultVersion());
	}

	public GrEclipseFormatConfig greclipseFormat(String version) {
		return new GrEclipseFormatConfig(version);
	}

	public class GrEclipseFormatConfig {
		final String version;
		Object[] configFiles;

		GrEclipseFormatConfig(String version) {
			configFiles = new Object[0];
			this.version = Objects.requireNonNull(version);
			addStep(createStep());
		}

		public void configFile(Object... configFiles) {
			this.configFiles = configFiles;
			replaceStep(createStep());
		}

		private FormatterStep createStep() {
			Project project = getProject();
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
			if (convention == null) {
				throw new GradleException("You must apply the goovy plugin before the spotless plugin if you are using the groovy extension.");
			}
			//Add all Groovy files (may contain Java files as well)

			UnionFileCollection union = new UnionFileCollection();
			for (SourceSet sourceSet : convention.getSourceSets()) {
				GroovySourceSet groovySourceSet = new DslObject(sourceSet).getConvention().getPlugin(GroovySourceSet.class);
				if (excludeJava) {
					union.add(groovySourceSet.getAllGroovy());
				} else {
					union.add(groovySourceSet.getGroovy());
				}
			}
			target = union;
		} else if (excludeJava != EXCLUDE_JAVA_DEFAULT) {
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
