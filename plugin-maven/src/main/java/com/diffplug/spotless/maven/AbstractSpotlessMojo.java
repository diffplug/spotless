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

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
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

	protected abstract void process(List<File> files, Formatter formatter) throws MojoExecutionException;

	@Override
	public final void execute() throws MojoExecutionException, MojoFailureException {
		List<FormatterFactory> formatterFactories = singletonList(java);

		for (FormatterFactory formatterFactory : formatterFactories) {
			execute(formatterFactory);
		}
	}

	private void execute(FormatterFactory formatterFactory) throws MojoExecutionException {
		List<File> files = collectFiles(formatterFactory.fileExtension());
		Formatter formatter = formatterFactory.newFormatter(files, getFormatterConfig());
		process(files, formatter);
	}

	private List<File> collectFiles(String extension) throws MojoExecutionException {
		try {
			return getAllSourceRoots().stream()
					.flatMap(root -> collectFiles(root, extension).stream())
					.map(Path::toFile)
					.collect(toList());
		} catch (Exception e) {
			throw new MojoExecutionException("Unable to collect files to format", e);
		}
	}

	private static List<Path> collectFiles(Path root, String extension) {
		try (Stream<Path> entries = Files.walk(root)) {
			return entries.filter(Files::isRegularFile)
					.filter(file -> hasExtension(file, extension))
					.collect(toList());
		} catch (IOException e) {
			throw new UncheckedIOException("Unable to walk the file tree rooted at " + root, e);
		}
	}

	private static boolean hasExtension(Path file, String extension) {
		Path fileName = file.getFileName();
		if (fileName == null) {
			return false;
		} else {
			String fileNameString = fileName.toString();
			return fileNameString.endsWith(FILE_EXTENSION_SEPARATOR + extension);
		}
	}

	private List<Path> getAllSourceRoots() {
		return Stream.concat(compileSourceRoots.stream(), testCompileSourceRoots.stream())
				.map(Paths::get)
				.filter(Files::isDirectory)
				.collect(toList());
	}

	private FormatterConfig getFormatterConfig() {
		ArtifactResolver resolver = new ArtifactResolver(repositorySystem, repositorySystemSession, repositories, getLog());
		Provisioner provisioner = MavenProvisioner.create(resolver);
		return new FormatterConfig(baseDir, encoding, lineEndings, provisioner);
	}
}
