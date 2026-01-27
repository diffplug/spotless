/*
 * Copyright 2016-2026 DiffPlug
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
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

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
import com.diffplug.spotless.extra.P2ModelWrapper;
import com.diffplug.spotless.extra.P2Provisioner;

/** Should be package-private. */
final class GradleProvisioner {
	private GradleProvisioner() {}

	enum Policy {
		INDEPENDENT, ROOT_PROJECT, ROOT_BUILDSCRIPT;

		public DedupingProvisioner dedupingProvisioner(Project project) {
			return switch (this) {
				case ROOT_PROJECT -> new DedupingProvisioner(forProject(project));
				case ROOT_BUILDSCRIPT -> new DedupingProvisioner(forRootProjectBuildscript(project));
				default -> throw Unhandled.enumException(this);
			};
		}

		public DedupingP2Provisioner dedupingP2Provisioner(Project project) {
			return switch (this) {
				case ROOT_PROJECT, ROOT_BUILDSCRIPT -> new DedupingP2Provisioner(P2Provisioner.createDefault());
				default -> throw Unhandled.enumException(this);
			};
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
						"You need to add a repository containing the '%s' artifact in '%sbuild.gradle'.%n"
								+ "E.g.: 'repositories { mavenCentral() }'",
						mavenCoords, projName), e);
			}
		};
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(GradleProvisioner.class);

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

	static class DedupingP2Provisioner implements P2Provisioner {
		private final Map<P2Request, List<File>> cache = new HashMap<>();
		private final P2Provisioner p2Provisioner;

		public DedupingP2Provisioner(P2Provisioner p2Provisioner) {
			this.p2Provisioner = p2Provisioner;
		}

		@Override
		public synchronized List<File> provisionP2Dependencies(
				P2ModelWrapper modelWrapper,
				Provisioner mavenProvisioner,
				@Nullable File cacheDirectory) throws IOException {

			P2Request req = new P2Request(
					List.copyOf(modelWrapper.getP2Repos()),
					List.copyOf(modelWrapper.getInstallList()),
					Set.copyOf(modelWrapper.getFilterNames()),
					List.copyOf(modelWrapper.getPureMaven()),
					modelWrapper.isUseMavenCentral(),
					cacheDirectory);

			List<File> result = cache.get(req);
			if (result != null) {
				return result;
			}

			result = p2Provisioner.provisionP2Dependencies(modelWrapper, mavenProvisioner, cacheDirectory);
			cache.put(req, List.copyOf(result));
			return result;
		}

		/** A child P2Provisioner which retrieves cached elements only. */
		final P2Provisioner cachedOnly = (modelWrapper, mavenProvisioner, cacheDirectory) -> {
			P2Request req = new P2Request(
					List.copyOf(modelWrapper.getP2Repos()),
					List.copyOf(modelWrapper.getInstallList()),
					Set.copyOf(modelWrapper.getFilterNames()),
					List.copyOf(modelWrapper.getPureMaven()),
					modelWrapper.isUseMavenCentral(),
					cacheDirectory);
			List<File> result;
			synchronized (cache) {
				result = cache.get(req);
			}
			if (result != null) {
				return result;
			}
			throw new GradleException("P2 dependencies not predeclared. Add Eclipse formatter configuration to the `spotlessPredeclare` block in the root project.");
		};

		/**
		 * Cache key capturing all P2Model state that affects query results.
		 * Based on P2Model fields from equo-ide:
		 * - p2repo (TreeSet<String>): P2 repository URLs
		 * - install (TreeSet<String>): Installation targets
		 * - filters (TreeMap<String, Filter>): Named filter configurations
		 * - pureMaven (TreeSet<String>): Pure Maven dependencies
		 * - useMavenCentral (boolean): Controls whether Maven Central is used
		 */
		private record P2Request(
				List<String> p2Repos,
				List<String> installList,
				java.util.Set<String> filterNames, // Filter names (Filter objects aren't easily comparable)
				List<String> pureMaven,
				boolean useMavenCentral,
				@Nullable File cacheDirectory) {}
	}
}
