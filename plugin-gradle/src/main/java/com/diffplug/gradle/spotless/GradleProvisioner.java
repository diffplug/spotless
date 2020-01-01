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

import com.diffplug.common.base.StringPrinter;
import com.diffplug.spotless.Provisioner;

/** Gradle integration for Provisioner. */
public class GradleProvisioner {
	private GradleProvisioner() {}

	public static Provisioner fromProject(Project project) {
		return project.getPlugins().apply(SpotlessPlugin.class).getExtension().registerDependenciesTask.rootProvisioner;
	}

	static Provisioner fromRootBuildscript(Project project) {
		Objects.requireNonNull(project);
		return (withTransitives, mavenCoords) -> {
			try {
				Dependency[] deps = mavenCoords.stream()
						.map(project.getBuildscript().getDependencies()::create)
						.toArray(Dependency[]::new);

				// #372 workaround: Accessing rootProject.configurations from multiple projects is not thread-safe
				ConfigurationContainer configContainer;
				synchronized (project.getRootProject()) {
					configContainer = project.getRootProject().getBuildscript().getConfigurations();
				}
				Configuration config = configContainer.detachedConfiguration(deps);
				config.setDescription(mavenCoords.toString());
				config.setTransitive(withTransitives);
				return config.resolve();
			} catch (Exception e) {
				logger.log(Level.SEVERE,
						StringPrinter.buildStringFromLines("You probably need to add a repository containing the '" + mavenCoords + "' artifact in the 'build.gradle' of your root project.",
								"E.g.: 'buildscript { repositories { mavenCentral() }}'",
								"Note that included buildscripts (using 'apply from') do not share their buildscript repositories with the underlying project.",
								"You have to specify the missing repository explicitly in the buildscript of the root project."),
						e);
				throw e;
			}
		};
	}

	static final Logger logger = Logger.getLogger(GradleProvisioner.class.getName());
}
