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
package com.diffplug.maven.spotless;

import java.io.File;
import java.util.List;
import java.util.Objects;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

public class ArtifactResolver {

	private final RepositorySystem repositorySystem;
	private final RepositorySystemSession repositorySystemSession;
	private final List<RemoteRepository> remoteRepositories;

	public ArtifactResolver(RepositorySystem repositorySystem, RepositorySystemSession repositorySystemSession,
			List<RemoteRepository> remoteRepositories) {
		this.repositorySystem = Objects.requireNonNull(repositorySystem);
		this.repositorySystemSession = Objects.requireNonNull(repositorySystemSession);
		this.remoteRepositories = Objects.requireNonNull(remoteRepositories);
	}

	public File resolve(String mavenCoordinate) throws ArtifactResolutionException {
		Artifact artifact = new DefaultArtifact(mavenCoordinate);
		ArtifactRequest request = new ArtifactRequest();
		request.setArtifact(artifact);
		request.setRepositories(remoteRepositories);

		ArtifactResult result = repositorySystem.resolveArtifact(repositorySystemSession, request);
		return result.getArtifact().getFile();
	}
}
