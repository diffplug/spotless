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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalDependency;
import org.gradle.api.artifacts.component.ModuleComponentSelector;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.attributes.Bundling;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.java.TargetJvmEnvironment;
import org.gradle.api.file.FileCollection;
import org.gradle.api.initialization.dsl.ScriptHandler;
import org.gradle.api.provider.Provider;
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
			case ROOT_PROJECT, ROOT_BUILDSCRIPT -> new DedupingP2Provisioner(P2Provisioner.createDefault(), defaultP2CacheDirectory(project));
			default -> throw Unhandled.enumException(this);
			};
		}
	}

	static class DedupingProvisioner implements Provisioner {
		private final Provisioner provisioner;
		private final Map<Request, Set<File>> cache = new HashMap<>();
		private final Map<DependencyClasspathRequest, DependencyClasspath> dependencyClasspathCache = new HashMap<>();

		DedupingProvisioner(Provisioner provisioner) {
			this.provisioner = provisioner;
		}

		DependencyClasspath dependencyClasspath(
				Collection<String> mavenCoordinates,
				Collection<String> projectPaths,
				String strictlyEnforcedCoordinate) {
			ConfigurationProvisioner configurationProvisioner = configurationProvisioner();
			DependencyClasspathRequest request = new DependencyClasspathRequest(mavenCoordinates, projectPaths, strictlyEnforcedCoordinate);
			synchronized (dependencyClasspathCache) {
				// Do not use a concurrent map here: different keys could create detached configurations concurrently,
				// while calls into Gradle's mutable project model must remain serialized.
				return dependencyClasspathCache.computeIfAbsent(
						request,
						unused -> configurationProvisioner.dependencyClasspath(
								request.mavenCoordinates, request.projectPaths, request.strictlyEnforcedCoordinate));
			}
		}

		DependencyClasspath cachedOnlyDependencyClasspath(
				Collection<String> mavenCoordinates,
				Collection<String> projectPaths,
				String strictlyEnforcedCoordinate) {
			ConfigurationProvisioner configurationProvisioner = configurationProvisioner();
			DependencyClasspathRequest request = new DependencyClasspathRequest(mavenCoordinates, projectPaths, strictlyEnforcedCoordinate);
			Provider<DependencyClasspath> cached = configurationProvisioner.project.getProviders().provider(() -> {
				synchronized (dependencyClasspathCache) {
					DependencyClasspath result = dependencyClasspathCache.get(request);
					if (result != null) {
						return result;
					}
				}
				throw new GradleException("Add a step with " + request.mavenCoordinates + " and projects " + request.projectPaths
						+ " into the `spotlessPredeclare` block in the root project.");
			});
			Provisioner externalProvisioner = (withTransitives, requestedCoordinates) -> cached.get().externalProvisioner
					.provisionWithTransitives(withTransitives, requestedCoordinates);
			FileCollection projectArtifacts = configurationProvisioner.project.getObjects().fileCollection()
					.from(cached.map(classpath -> classpath.projectArtifacts));
			return new DependencyClasspath(externalProvisioner, projectArtifacts);
		}

		private ConfigurationProvisioner configurationProvisioner() {
			if (provisioner instanceof ConfigurationProvisioner configurationProvisioner) {
				return configurationProvisioner;
			}
			throw new IllegalStateException("Project dependencies require a Gradle configuration-backed provisioner.");
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
		return new ConfigurationProvisioner(project, project.getConfigurations(), project.getDependencies());
	}

	static Provisioner forRootProjectBuildscript(Project project) {
		Project rootProject = project.getRootProject();
		ScriptHandler buildscript = rootProject.getBuildscript();
		return new ConfigurationProvisioner(rootProject, buildscript.getConfigurations(), buildscript.getDependencies());
	}

	/**
	 * A provisioner bound to a specific Gradle dependency-resolution scope.
	 * <p>
	 * Retaining the {@link ConfigurationContainer} and {@link DependencyHandler} is necessary when a formatter uses
	 * both Maven and project dependencies. It lets both dependency types participate in one detached configuration,
	 * so Gradle resolves version conflicts before the selected artifacts are separated into external and project
	 * classpaths. The binding also preserves whether dependencies must resolve from the consuming project's or the
	 * root buildscript's repositories, when using {@code predeclareDepsFromBuildscript()}.
	 */
	private static final class ConfigurationProvisioner implements Provisioner {
		private final Project project;
		private final ConfigurationContainer configurations;
		private final DependencyHandler dependencies;

		private ConfigurationProvisioner(Project project, ConfigurationContainer configurations, DependencyHandler dependencies) {
			this.project = project;
			this.configurations = configurations;
			this.dependencies = dependencies;
		}

		@Override
		public Set<File> provisionWithTransitives(boolean withTransitives, Collection<String> mavenCoords) {
			try {
				Request request = new Request(withTransitives, mavenCoords);
				Configuration config = configuration(
						mavenCoords, List.of(), withTransitives, "Spotless internal dependency resolution for " + request, null);
				return config.resolve();
			} catch (Exception e) {
				throw repositoryException(mavenCoords, e);
			}
		}

		DependencyClasspath dependencyClasspath(
				Collection<String> mavenCoordinates,
				Collection<String> projectPaths,
				String strictlyEnforcedCoordinate) {
			StrictVersion strictVersion = strictVersion(strictlyEnforcedCoordinate);
			// Create every dependency before creating the configuration.
			// This produces one conflict-resolution graph.
			Configuration config = configuration(
					mavenCoordinates,
					projectPaths,
					true,
					"Spotless internal dependency resolution for " + mavenCoordinates + " and projects " + projectPaths,
					strictVersion);
			// This view contains ktlint and external libraries.
			FileCollection externalArtifacts = config.getIncoming()
					.artifactView(view -> view.componentFilter(identifier -> !(identifier instanceof ProjectComponentIdentifier)))
					.getFiles();
			// This view contains local project artifacts.
			FileCollection projectArtifacts = config.getIncoming()
					.artifactView(view -> view.componentFilter(ProjectComponentIdentifier.class::isInstance))
					.getFiles();
			Set<String> expectedCoordinates = Set.copyOf(mavenCoordinates);
			Provisioner externalProvisioner = new DedupingProvisioner((withTransitives, requestedCoordinates) -> {
				if (!withTransitives || !expectedCoordinates.equals(new HashSet<>(requestedCoordinates))) {
					throw new IllegalArgumentException("Unexpected dependency request for unified ktlint classpath: " + requestedCoordinates);
				}
				strictVersion.rejectConflicts(config.getIncoming().getResolutionResult().getAllDependencies());
				return externalArtifacts.getFiles();
			});
			return new DependencyClasspath(externalProvisioner, projectArtifacts);
		}

		private Configuration configuration(
				Collection<String> mavenCoordinates,
				Collection<String> projectPaths,
				boolean withTransitives,
				String description,
				@Nullable StrictVersion strictVersion) {
			List<Dependency> requestedDependencies = new ArrayList<>(mavenCoordinates.size() + projectPaths.size());
			boolean strictDependencyFound = strictVersion == null;
			for (String coordinate : mavenCoordinates) {
				Dependency dependency = dependencies.create(coordinate);
				if (strictVersion != null && strictVersion.coordinate.equals(coordinate)) {
					strictVersion.enforce(dependency);
					strictDependencyFound = true;
				}
				requestedDependencies.add(dependency);
			}
			if (!strictDependencyFound) {
				throw new IllegalArgumentException("Strictly enforced dependency is not part of the request: " + strictVersion.coordinate);
			}
			projectPaths.stream()
					.map(projectPath -> project.getDependencies().project(Map.of("path", projectPath)))
					.forEach(requestedDependencies::add);
			Configuration config = configurations.detachedConfiguration(requestedDependencies.toArray(Dependency[]::new));
			configure(config, withTransitives, description);
			return config;
		}

		private StrictVersion strictVersion(String coordinate) {
			Dependency dependency = dependencies.create(coordinate);
			if (!(dependency instanceof ExternalDependency externalDependency)) {
				throw new IllegalArgumentException("Cannot strictly enforce non-module dependency " + coordinate);
			}
			return new StrictVersion(
					Objects.requireNonNull(externalDependency.getGroup(), "group"),
					Objects.requireNonNull(externalDependency.getName(), "name"),
					Objects.requireNonNull(externalDependency.getVersion(), "version"),
					coordinate);
		}

		private void configure(Configuration config, boolean withTransitives, String description) {
			// Detached configurations avoid mutating configuration containers during task execution, which Gradle 9
			// forbids for buildscript configurations. See https://github.com/diffplug/spotless/issues/2599.
			config.setDescription(description);
			config.setTransitive(withTransitives);
			config.setCanBeConsumed(false);
			config.setVisible(false);
			config.attributes(attr -> {
				attr.attribute(Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, Category.LIBRARY));
				attr.attribute(Bundling.BUNDLING_ATTRIBUTE, project.getObjects().named(Bundling.class, Bundling.EXTERNAL));
				// Add this attribute for resolving Guava dependency, see https://github.com/google/guava/issues/6801.
				attr.attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, project.getObjects().named(TargetJvmEnvironment.class, TargetJvmEnvironment.STANDARD_JVM));
			});
		}

		private GradleException repositoryException(Collection<String> mavenCoordinates, Exception cause) {
			String projName = project.getPath().substring(1).replace(':', '/');
			if (!projName.isEmpty()) {
				projName = projName + "/";
			}
			return new GradleException(String.format(
					"You need to add a repository containing the '%s' artifact in '%sbuild.gradle'.%n"
							+ "E.g.: 'repositories { mavenCentral() }'",
					mavenCoordinates, projName), cause);
		}
	}

	static final class DependencyClasspath {
		final Provisioner externalProvisioner;
		final FileCollection projectArtifacts;

		private DependencyClasspath(Provisioner externalProvisioner, FileCollection projectArtifacts) {
			this.externalProvisioner = externalProvisioner;
			this.projectArtifacts = projectArtifacts;
		}
	}

	private record DependencyClasspathRequest(
			ImmutableList<String> mavenCoordinates,
			ImmutableList<String> projectPaths,
			String strictlyEnforcedCoordinate) {
		private DependencyClasspathRequest(
				Collection<String> mavenCoordinates,
				Collection<String> projectPaths,
				String strictlyEnforcedCoordinate) {
			this(ImmutableList.copyOf(mavenCoordinates), ImmutableList.copyOf(projectPaths), strictlyEnforcedCoordinate);
		}
	}

	private record StrictVersion(String group, String name, String version, String coordinate) {
		private void enforce(Dependency dependency) {
			if (!(dependency instanceof ExternalDependency externalDependency)
					|| !group.equals(externalDependency.getGroup())
					|| !name.equals(externalDependency.getName())
					|| !version.equals(externalDependency.getVersion())) {
				throw new IllegalArgumentException("Cannot strictly enforce " + coordinate + " on " + dependency);
			}
			externalDependency.version(constraint -> constraint.strictly(version));
		}

		/**
		 * Rejects any dependencies that conflict with the strictly enforced version.
		 *
		 * <p>Current limitation: ktlint 0.x and 1.x use different modules. Which is not currently handled,
		 * i.e. it won't be detected / treated as a version conflict.</p>
		 *
		 * @param dependencies
		 */
		private void rejectConflicts(Collection<? extends DependencyResult> dependencies) {
			for (DependencyResult dependency : dependencies) {
				if (dependency.getRequested() instanceof ModuleComponentSelector requested
						&& group.equals(requested.getGroup())
						&& name.equals(requested.getModule())
						&& !version.equals(requested.getVersion())) {
					throw new GradleException("The dependency graph requests '" + requested + "', but Spotless is configured to use '"
							+ coordinate + "'. Remove the conflicting ktlint dependency or align it with the version requested by the Spotless DSL.");
				}
			}
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(GradleProvisioner.class);

	static File defaultP2CacheDirectory(Project project) {
		return new File(project.getGradle().getGradleUserHomeDir(), "caches/p2-data");
	}

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
		@Nullable private final File defaultCacheDirectory;

		public DedupingP2Provisioner(P2Provisioner p2Provisioner) {
			this(p2Provisioner, null);
		}

		public DedupingP2Provisioner(P2Provisioner p2Provisioner, @Nullable File defaultCacheDirectory) {
			this.p2Provisioner = p2Provisioner;
			this.defaultCacheDirectory = defaultCacheDirectory;
		}

		@Override
		public synchronized List<File> provisionP2Dependencies(
				P2ModelWrapper modelWrapper,
				Provisioner mavenProvisioner,
				@Nullable File cacheDirectory) throws IOException {

			File effectiveCacheDirectory = effectiveCacheDirectory(cacheDirectory);
			P2Request req = new P2Request(
					List.copyOf(modelWrapper.getP2Repos()),
					List.copyOf(modelWrapper.getInstallList()),
					Set.copyOf(modelWrapper.getFilterNames()),
					List.copyOf(modelWrapper.getPureMaven()),
					modelWrapper.isUseMavenCentral(),
					effectiveCacheDirectory);

			List<File> result = cache.get(req);
			if (result != null) {
				return result;
			}

			result = p2Provisioner.provisionP2Dependencies(modelWrapper, mavenProvisioner, effectiveCacheDirectory);
			cache.put(req, List.copyOf(result));
			return result;
		}

		/** A child P2Provisioner which retrieves cached elements only. */
		final P2Provisioner cachedOnly = (modelWrapper, mavenProvisioner, cacheDirectory) -> {
			File effectiveCacheDirectory = effectiveCacheDirectory(cacheDirectory);
			P2Request req = new P2Request(
					List.copyOf(modelWrapper.getP2Repos()),
					List.copyOf(modelWrapper.getInstallList()),
					Set.copyOf(modelWrapper.getFilterNames()),
					List.copyOf(modelWrapper.getPureMaven()),
					modelWrapper.isUseMavenCentral(),
					effectiveCacheDirectory);
			List<File> result;
			synchronized (cache) {
				result = cache.get(req);
			}
			if (result != null) {
				return result;
			}
			throw new GradleException("P2 dependencies not predeclared. Add Eclipse formatter configuration to the `spotlessPredeclare` block in the root project.");
		};

		@Nullable private File effectiveCacheDirectory(@Nullable File cacheDirectory) {
			return cacheDirectory != null ? cacheDirectory : defaultCacheDirectory;
		}

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
				Set<String> filterNames, // Filter names (Filter objects aren't easily comparable)
				List<String> pureMaven,
				boolean useMavenCentral,
				@Nullable File cacheDirectory) {}
	}
}
