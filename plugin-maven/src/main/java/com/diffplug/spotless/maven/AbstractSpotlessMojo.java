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
package com.diffplug.spotless.maven;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.maven.java.Java;
import com.diffplug.spotless.maven.scala.Scala;

public abstract class AbstractSpotlessMojo extends AbstractMojo {

	private static final String DEFAULT_ENCODING = "UTF-8";
	private static final String DEFAULT_LINE_ENDINGS = "GIT_ATTRIBUTES";

	private static final String FILE_EXTENSION_SEPARATOR = ".";

	@Component
	private RepositorySystem repositorySystem;

	@Parameter(defaultValue = "${repositorySystemSession}", required = true, readonly = true)
	private RepositorySystemSession repositorySystemSession;

	@Parameter(defaultValue = "${project.remotePluginRepositories}", required = true, readonly = true)
	private List<RemoteRepository> repositories;

	@Parameter(defaultValue = "${project.basedir}", required = true, readonly = true)
	private File baseDir;

	@Parameter(defaultValue = "${project.build.directory}", required = true, readonly = true)
	private File targetDir;

	@Parameter(defaultValue = DEFAULT_ENCODING)
	private String encoding;

	@Parameter(defaultValue = DEFAULT_LINE_ENDINGS)
	private LineEnding lineEndings;

	@Parameter
	private Java java;

	@Parameter
	private Scala scala;

	protected abstract void process(List<File> files, Formatter formatter) throws MojoExecutionException;

	@Override
	public final void execute() throws MojoExecutionException, MojoFailureException {
		List<FormatterFactory> formatterFactories = Arrays.asList(java, scala);

		for (FormatterFactory formatterFactory : formatterFactories) {
			if (formatterFactory != null) {
				execute(formatterFactory);
			}
		}
	}

	private void execute(FormatterFactory formatterFactory) throws MojoExecutionException {
		List<File> files = collectFiles(formatterFactory.fileExtensions());
		Formatter formatter = formatterFactory.newFormatter(files, getFormatterConfig());
		process(files, formatter);
	}

	private List<File> collectFiles(Set<String> extensions) throws MojoExecutionException {
		Path projectDir = baseDir.toPath();
		Path outputDir = targetDir.toPath();

		try (Stream<Path> entries = Files.walk(projectDir)) {
			return entries.filter(entry -> !entry.startsWith(outputDir))
					.filter(Files::isRegularFile)
					.filter(file -> hasExtension(file, extensions))
					.map(Path::toFile)
					.collect(toList());
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to walk the file tree rooted at " + projectDir, e);
		}
	}

	private static boolean hasExtension(Path file, Set<String> extensions) {
		Path fileName = file.getFileName();
		if (fileName == null) {
			return false;
		} else {
			String fileNameString = fileName.toString();
			for (String extension : extensions) {
				if (fileNameString.endsWith(FILE_EXTENSION_SEPARATOR + extension)) {
					return true;
				}
			}
			return false;
		}
	}

	private FormatterConfig getFormatterConfig() {
		ArtifactResolver resolver = new ArtifactResolver(repositorySystem, repositorySystemSession, repositories, getLog());
		Provisioner provisioner = MavenProvisioner.create(resolver);
		return new FormatterConfig(baseDir, encoding, lineEndings, provisioner);
	}
}
