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

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.Provisioner;

public abstract class AbstractSpotlessMojo extends AbstractMojo {

	private static final String DEFAULT_ENCODING = "UTF-8";
	private static final String DEFAULT_LINE_ENDINGS = "GIT_ATTRIBUTES";

	@Component
	private RepositorySystem repositorySystem;

	@Parameter(defaultValue = "${repositorySystemSession}", required = true, readonly = true)
	private RepositorySystemSession repositorySystemSession;

	@Parameter(defaultValue = "${project.remotePluginRepositories}", required = true, readonly = true)
	private List<RemoteRepository> repositories;

	@Parameter(defaultValue = "${project.basedir}", required = true, readonly = true)
	private File baseDir;

	@Parameter(defaultValue = "${project.compileSourceRoots}", required = true, readonly = true)
	private List<String> compileSourceRoots;

	@Parameter(defaultValue = "${project.testCompileSourceRoots}", required = true, readonly = true)
	private List<String> testCompileSourceRoots;

	@Parameter(defaultValue = DEFAULT_ENCODING)
	private String encoding;

	@Parameter(defaultValue = DEFAULT_LINE_ENDINGS)
	private LineEnding lineEndings;

	@Parameter
	private Java java;

	protected List<Path> getAllSourceRoots() {
		return Stream.concat(compileSourceRoots.stream(), testCompileSourceRoots.stream())
				.map(Paths::get)
				.filter(Files::isDirectory)
				.collect(toList());
	}

	protected MojoConfig getMojoConfig() {
		ArtifactResolver resolver = new ArtifactResolver(repositorySystem, repositorySystemSession, repositories);
		Provisioner provisioner = MavenProvisioner.create(resolver);
		return new MojoConfig(baseDir, encoding, lineEndings, provisioner);
	}

	protected Java getJava() {
		return java;
	}
}
