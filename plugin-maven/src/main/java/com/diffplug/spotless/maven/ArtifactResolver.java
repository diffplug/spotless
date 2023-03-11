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
package com.diffplug.spotless.maven;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;

public class ArtifactResolver {

	private final static Exclusion EXCLUDE_ALL_TRANSITIVES = new Exclusion("*", "*", "*", "*");

	private final RepositorySystem repositorySystem;
	private final RepositorySystemSession session;
	private final List<RemoteRepository> repositories;
	private final Log log;

	public ArtifactResolver(RepositorySystem repositorySystem, RepositorySystemSession session,
			List<RemoteRepository> repositories, Log log) {
		this.repositorySystem = Objects.requireNonNull(repositorySystem);
		this.session = Objects.requireNonNull(session);
		this.repositories = Objects.requireNonNull(repositories);
		this.log = Objects.requireNonNull(log);
	}

	/**
	 * Given a set of maven coordinates, returns a set of jars which include all
	 * of the specified coordinates and optionally their transitive dependencies.
	 */
	public Set<File> resolve(boolean withTransitives, Collection<String> mavenCoordinates) {
		Collection<Exclusion> excludeTransitive = new ArrayList<>(1);
		if (!withTransitives) {
			excludeTransitive.add(EXCLUDE_ALL_TRANSITIVES);
		}
		List<Dependency> dependencies = mavenCoordinates.stream()
				.map(DefaultArtifact::new)
				.map(artifact -> new Dependency(artifact, null, null, excludeTransitive))
				.collect(toList());
		CollectRequest collectRequest = new CollectRequest(dependencies, null, repositories);
		DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, null);
		DependencyResult dependencyResult = resolveDependencies(dependencyRequest);

		return dependencyResult.getArtifactResults()
				.stream()
				.peek(this::logResolved)
				.map(ArtifactResult::getArtifact)
				.map(Artifact::getFile)
				.collect(toSet());
	}

	private DependencyResult resolveDependencies(DependencyRequest dependencyRequest) {
		try {
			return repositorySystem.resolveDependencies(session, dependencyRequest);
		} catch (DependencyResolutionException e) {
			throw new ArtifactResolutionException("Unable to resolve dependencies", e);
		}
	}

	private void logResolved(ArtifactResult artifactResult) {
		if (log.isDebugEnabled()) {
			log.debug("Resolved artifact: " + artifactResult);
		}
	}
}
