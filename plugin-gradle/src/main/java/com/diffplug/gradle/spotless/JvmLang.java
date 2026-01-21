/*
 * Copyright 2023-2026 DiffPlug
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
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

interface JvmLang {

	default SourceSetContainer getSourceSets(Project project, String message) {
		final JavaPluginExtension javaPluginExtension = project.getExtensions().findByType(JavaPluginExtension.class);
		if (javaPluginExtension == null) {
			throw new GradleException(message);
		}
		return javaPluginExtension.getSourceSets();
	}

	default FileCollection getSources(Project project, String message, Function<SourceSet, SourceDirectorySet> sourceSetSourceDirectory, Spec<? super File> filterSpec) {
		FileCollection union = project.files();
		final SourceSetContainer sourceSets = getSourceSets(project, message);
		for (SourceSet sourceSet : sourceSets) {
			union = union.plus(sourceSetSourceDirectory.apply(sourceSet).filter(filterSpec));
		}
		return union;
	}
}
