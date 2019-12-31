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

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.util.GradleVersion;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.common.base.Unhandled;
import com.diffplug.spotless.Provisioner;

/** Gradle integration for Provisioner. */
public class GradleProvisioner {
	/** Determines where Spotless will resolve its dependencies. */
	public enum ResolveDependenciesIn {
		/** Spotless dependencies will be resolved from the root buildscript (deprecated for multi-project builds in 6.0). */
		ROOT_BUILDSCRIPT,
		/** Spotless dependencies will be resolved from the project buildscript (by default subprojects don't have even a single repository). */
		PROJECT_BUILDSCRIPT,
		/** Spotless dependencies will be resolved from the normal repositories of this project. */
		PROJECT
	}

	static final GradleVersion STRICT_CONFIG_ACCESS = GradleVersion.version("6.0");

	private GradleProvisioner() {}

	public static Provisioner fromProject(Project project) {
		ResolveDependenciesIn mode = project.getPlugins().getPlugin(SpotlessPlugin.class).getExtension().getResolveDependenciesIn();
		Objects.requireNonNull(project);
		return (withTransitives, mavenCoords) -> {
			try {
				Dependency[] deps = mavenCoords.stream()
						.map(project.getBuildscript().getDependencies()::create)
						.toArray(Dependency[]::new);

				ConfigurationContainer configContainer;
				if (mode == null || mode == ResolveDependenciesIn.ROOT_BUILDSCRIPT) {
					// #372 workaround: Accessing rootProject.configurations from multiple projects is not thread-safe
					synchronized (project.getRootProject()) {
						configContainer = project.getRootProject().getBuildscript().getConfigurations();
					}
				} else if (mode == ResolveDependenciesIn.PROJECT_BUILDSCRIPT) {
					configContainer = project.getBuildscript().getConfigurations();
				} else if (mode == ResolveDependenciesIn.PROJECT) {
					configContainer = project.getConfigurations();
				} else {
					throw Unhandled.enumException(mode);
				}
				Configuration config = configContainer.detachedConfiguration(deps);
				config.setDescription(mavenCoords.toString());
				config.setTransitive(withTransitives);
				return config.resolve();
			} catch (Exception e) {
				String errorMsg;
				if (mode == null || mode == ResolveDependenciesIn.ROOT_BUILDSCRIPT) {
					errorMsg = StringPrinter.buildStringFromLines(
							"You need to add a repository containing the '" + mavenCoords + "' artifact to the `buildscript{}` block of the build.gradle of your root project.",
							"E.g.: 'buildscript { repositories { mavenCentral() }}'",
							"Note that included buildscripts (using 'apply from') do not share their buildscript repositories with the underlying project.",
							"You have to specify the missing repository explicitly in the buildscript of the root project.");
				} else if (mode == ResolveDependenciesIn.PROJECT_BUILDSCRIPT) {
					errorMsg = StringPrinter.buildStringFromLines(
							"You need to add a repository containing the '" + mavenCoords + "' artifact to the `buildscript{}` block of the build.gradle of this project.",
							"E.g.: 'buildscript { repositories { mavenCentral() }}'",
							"By default, subprojects dont have any repositories in their buildscript whatsoever.");
				} else if (mode == ResolveDependenciesIn.PROJECT) {
					errorMsg = StringPrinter.buildStringFromLines(
							"You need to add a repository containing the '" + mavenCoords + "' artifact to the `repositories{}` block of the build.gradle of this project.",
							"E.g.: 'repositories { mavenCentral() }'");
				} else {
					throw Unhandled.enumException(mode);
				}
				logger.log(Level.SEVERE, errorMsg);
				throw e;
			}
		};
	}

	static final Logger logger = Logger.getLogger(GradleProvisioner.class.getName());
}
