/*
 * Copyright 2016-2025 DiffPlug
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.attributes.Bundling;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.java.TargetJvmEnvironment;
import org.gradle.api.initialization.dsl.ScriptHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diffplug.common.base.Unhandled;
import com.diffplug.common.collect.ImmutableList;
import com.diffplug.spotless.Provisioner;

/** Should be package-private. */
class GradleProvisioner {
	private GradleProvisioner() {}

	enum Policy {
		INDEPENDENT, ROOT_PROJECT, ROOT_BUILDSCRIPT;

		public DedupingProvisioner dedupingProvisioner(Project project) {
			switch (this) {
			case ROOT_PROJECT:
				return new DedupingProvisioner(forProject(project));
			case ROOT_BUILDSCRIPT:
				return new DedupingProvisioner(forRootProjectBuildscript(project));
			case INDEPENDENT:
			default:
				throw Unhandled.enumException(this);
			}
		}
	}

	static class DedupingProvisioner implements Provisioner {
		private final Provisioner provisioner;
		private final Map<Request, Set<File>> cache = new HashMap<>();

		DedupingProvisioner(Provisioner provisioner) {
			this.provisioner = provisioner;
		}

		@Override
		public Set<File> provisionWithTransitives(boolean withTransitives, Collection<String> mavenCoordinates) {
			Request req = new Request(withTransitives, mavenCoordinates);
			Set<File> result;
			synchronized (cache) {
				result = cache.get(req);
			}
			if (result != null) {
				return result;
			} else {
				synchronized (cache) {
					result = cache.get(req);
					if (result == null) {
						result = provisioner.provisionWithTransitives(req.withTransitives, req.mavenCoords);
						cache.put(req, result);
					}
					return result;
				}
			}
		}

		/** A child Provisioner which retries cached elements only. */
		final Provisioner cachedOnly = (withTransitives, mavenCoordinates) -> {
			Request req = new Request(withTransitives, mavenCoordinates);
			Set<File> result;
			synchronized (cache) {
				result = cache.get(req);
			}
			if (result != null) {
				return result;
			}
			throw new GradleException("Add a step with " + req.mavenCoords + " into the `spotlessPredeclare` block in the root project.");
		};
	}

	static Provisioner forProject(Project project) {
		return forConfigurationContainer(project, project.getConfigurations(), project.getDependencies());
	}

	static Provisioner forRootProjectBuildscript(Project project) {
		Project rootProject = project.getRootProject();
		ScriptHandler buildscript = rootProject.getBuildscript();
		return forConfigurationContainer(rootProject, buildscript.getConfigurations(), buildscript.getDependencies());
	}

	private static Provisioner forConfigurationContainer(Project project, ConfigurationContainer configurations, DependencyHandler dependencies) {
		return (withTransitives, mavenCoords) -> {
			try {
				Configuration config = configurations.create("spotless"
						+ new Request(withTransitives, mavenCoords).hashCode());
				mavenCoords.stream()
						.map(dependencies::create)
						.forEach(config.getDependencies()::add);
				config.setDescription(mavenCoords.toString());
				config.setTransitive(withTransitives);
				config.setCanBeConsumed(false);
				config.setVisible(false);
				config.attributes(attr -> {
					attr.attribute(Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, Category.LIBRARY));
					attr.attribute(Bundling.BUNDLING_ATTRIBUTE, project.getObjects().named(Bundling.class, Bundling.EXTERNAL));
					// Add this attribute for resolving Guava dependency, see https://github.com/google/guava/issues/6801.
					attr.attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, project.getObjects().named(TargetJvmEnvironment.class, TargetJvmEnvironment.STANDARD_JVM));
				});
				return config.resolve();
			} catch (Exception e) {
				String projName = project.getPath().substring(1).replace(':', '/');
				if (!projName.isEmpty()) {
					projName = projName + "/";
				}
				throw new GradleException(String.format(
						"You need to add a repository containing the '%s' artifact in '%sbuild.gradle'.%n" +
								"E.g.: 'repositories { mavenCentral() }'",
						mavenCoords, projName), e);
			}
		};
	}

	private static final Logger logger = LoggerFactory.getLogger(GradleProvisioner.class);

	/** Models a request to the provisioner. */
	private static class Request {
		final boolean withTransitives;
		final ImmutableList<String> mavenCoords;

		public Request(boolean withTransitives, Collection<String> mavenCoords) {
			this.withTransitives = withTransitives;
			this.mavenCoords = ImmutableList.copyOf(mavenCoords);
		}

		@Override
		public int hashCode() {
			return withTransitives ? mavenCoords.hashCode() : ~mavenCoords.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			} else if (obj instanceof Request o) {
				return o.withTransitives == withTransitives && o.mavenCoords.equals(mavenCoords);
			} else {
				return false;
			}
		}

		@Override
		public String toString() {
			String coords = mavenCoords.toString();
			StringBuilder builder = new StringBuilder();
			builder.append(coords, 1, coords.length() - 1); // strip off []
			if (withTransitives) {
				builder.append(" with transitives");
			} else {
				builder.append(" no transitives");
			}
			return builder.toString();
		}
	}
}
