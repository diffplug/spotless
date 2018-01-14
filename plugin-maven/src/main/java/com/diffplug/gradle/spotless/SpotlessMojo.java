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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singleton;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.extra.java.EclipseFormatterStep;

@Mojo(name = "spotless")
public class SpotlessMojo extends AbstractMojo {

	@Component
	private RepositorySystem repositorySystem;

	@Parameter(defaultValue = "${repositorySystemSession}", required = true, readonly = true)
	private RepositorySystemSession repositorySystemSession;

	@Parameter(defaultValue = "${project.remotePluginRepositories}", required = true, readonly = true)
	private List<RemoteRepository> repositories;

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	private MavenProject project;

	@Parameter(property = "eclipseFormatFile", required = true)
	private String eclipseFormatFile;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		ArtifactResolver resolver = new ArtifactResolver(repositorySystem, repositorySystemSession, repositories);

		Provisioner provisioner = MavenProvisioner.create(resolver);
		Set<File> settingFiles = singleton(new File(eclipseFormatFile));
		FormatterStep step = EclipseFormatterStep.create(settingFiles, provisioner);

		for (String compileSourceRoot : project.getCompileSourceRoots()) {
			Path root = Paths.get(compileSourceRoot);
			try (Stream<Path> entries = Files.walk(root)) {
				entries.filter(Files::isRegularFile)
						.filter(file -> file.getFileName().toString().endsWith(".java"))
						.forEach(file -> format(file, step));
			} catch (Exception e) {
				throw new MojoExecutionException("Unable to walk the file tree", e);
			}
		}
	}

	private void format(Path file, FormatterStep step) {
		try {
			getLog().info("Formatting " + file);
			String contents = new String(Files.readAllBytes(file), UTF_8);
			String formatted = step.format(contents, file.toFile());
			Files.write(file, formatted.getBytes(UTF_8));
		} catch (Exception e) {
			throw new RuntimeException("Unable to format " + file, e);
		}
	}
}
