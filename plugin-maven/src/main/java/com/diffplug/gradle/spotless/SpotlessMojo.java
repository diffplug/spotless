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

import static java.util.Collections.singleton;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
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

import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.ThrowingEx;
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

		// create the eclipse step
		Set<File> settingFiles = singleton(new File(eclipseFormatFile));
		FormatterStep step = EclipseFormatterStep.create(EclipseFormatterStep.defaultVersion(),
				settingFiles, provisioner);

		// collect all the files that are going to be formatted
		File rootDir = project.getFile();
		List<File> toFormat = new ArrayList<>();
		for (String compileSourceRoot : project.getCompileSourceRoots()) {
			Path root = Paths.get(compileSourceRoot);
			try (Stream<Path> entries = Files.walk(root)) {
				entries.filter(Files::isRegularFile)
						.filter(file -> file.getFileName().toString().endsWith(".java"))
						.map(Path::toFile)
						.forEach(toFormat::add);
			} catch (Exception e) {
				throw new MojoExecutionException("Unable to walk the file tree", e);
			}
		}

		// create a formatter
		Formatter formatter = Formatter.builder()
				.lineEndingsPolicy(LineEnding.GIT_ATTRIBUTES.createPolicy(rootDir, () -> toFormat))
				.encoding(StandardCharsets.UTF_8)
				.rootDir(rootDir.toPath())
				.steps(Collections.singletonList(step))
				.build();

		// use the formatter to format all the files
		try {
			for (File file : toFormat) {
				formatter.applyTo(file);
			}
		} catch (IOException e) {
			throw ThrowingEx.asRuntime(e);
		}
	}
}
