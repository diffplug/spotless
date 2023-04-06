/*
 * Copyright 2023 DiffPlug
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

import java.io.File;
import java.util.function.Function;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.util.GradleVersion;

interface JvmLang {

	default FileCollection getSources(Project project, String message, Function<SourceSet, SourceDirectorySet> sourceSetSourceDirectory, Spec<? super File> filterSpec) {
		final SourceSetContainer sourceSets;
		FileCollection union = project.files();
		if (GradleVersion.current().compareTo(GradleVersion.version(SpotlessPlugin.VER_GRADLE_javaPluginExtension)) >= 0) {
			final JavaPluginExtension javaPluginExtension = project.getExtensions().findByType(JavaPluginExtension.class);
			if (javaPluginExtension == null) {
				throw new GradleException(message);
			}
			sourceSets = javaPluginExtension.getSourceSets();
		} else {
			final JavaPluginConvention javaPluginConvention = project.getConvention().findPlugin(JavaPluginConvention.class);
			if (javaPluginConvention == null) {
				throw new GradleException(message);
			}
			sourceSets = javaPluginConvention.getSourceSets();
		}
		for (SourceSet sourceSet : sourceSets) {
			union = union.plus(sourceSetSourceDirectory.apply(sourceSet).filter(filterSpec));
		}
		return union;
	}
}
