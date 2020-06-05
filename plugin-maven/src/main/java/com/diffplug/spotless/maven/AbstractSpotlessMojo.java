/*
 * Copyright 2018-2020 DiffPlug
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
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.resource.ResourceManager;
import org.codehaus.plexus.resource.loader.FileResourceLoader;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.maven.cpp.Cpp;
import com.diffplug.spotless.maven.generic.Format;
import com.diffplug.spotless.maven.generic.LicenseHeader;
import com.diffplug.spotless.maven.java.Java;
import com.diffplug.spotless.maven.kotlin.Kotlin;
import com.diffplug.spotless.maven.scala.Scala;
import com.diffplug.spotless.maven.typescript.Typescript;

public abstract class AbstractSpotlessMojo extends AbstractMojo {

	private static final String DEFAULT_ENCODING = "UTF-8";
	private static final String DEFAULT_LINE_ENDINGS = "GIT_ATTRIBUTES";

	@Component
	private RepositorySystem repositorySystem;

	@Component
	private ResourceManager resourceManager;

	@Parameter(defaultValue = "${repositorySystemSession}", required = true, readonly = true)
	private RepositorySystemSession repositorySystemSession;

	@Parameter(defaultValue = "${project.remotePluginRepositories}", required = true, readonly = true)
	private List<RemoteRepository> repositories;

	@Parameter(defaultValue = "${project.basedir}", required = true, readonly = true)
	private File baseDir;

	@Parameter(defaultValue = "${project.build.directory}", required = true, readonly = true)
	private File buildDir;

	@Parameter(defaultValue = DEFAULT_ENCODING)
	private String encoding;

	@Parameter(defaultValue = DEFAULT_LINE_ENDINGS)
	private LineEnding lineEndings;

	@Parameter
	private LicenseHeader licenseHeader;

	@Parameter
	private List<Format> formats = Collections.emptyList();

	@Parameter
	private Java java;

	@Parameter
	private Scala scala;

	@Parameter
	private Kotlin kotlin;

	/** The XML extension is discontinued. */
	@Parameter
	@Deprecated
	private com.diffplug.spotless.maven.xml.Xml xml;

	@Parameter
	private Cpp cpp;

	@Parameter
	private Typescript typescript;

	/** The CSS extension is discontinued. */
	@Parameter
	@Deprecated
	private com.diffplug.spotless.maven.css.Css css;

	@Parameter(property = "spotlessFiles")
	private String filePatterns;

	protected abstract void process(List<File> files, Formatter formatter) throws MojoExecutionException;

	@Override
	public final void execute() throws MojoExecutionException {
		List<FormatterFactory> formatterFactories = getFormatterFactories();

		for (FormatterFactory formatterFactory : formatterFactories) {
			execute(formatterFactory);
		}
	}

	private void execute(FormatterFactory formatterFactory) throws MojoExecutionException {
		List<File> files = collectFiles(formatterFactory);
		try (Formatter formatter = formatterFactory.newFormatter(files, getFormatterConfig())) {
			process(files, formatter);
		}
	}

	private List<File> collectFiles(FormatterFactory formatterFactory) throws MojoExecutionException {
		Set<String> configuredIncludes = formatterFactory.includes();
		Set<String> configuredExcludes = formatterFactory.excludes();

		Set<String> includes = configuredIncludes.isEmpty() ? formatterFactory.defaultIncludes() : configuredIncludes;

		Set<String> excludes = new HashSet<>(FileUtils.getDefaultExcludesAsList());
		excludes.add(withTrailingSeparator(buildDir.toString()));
		excludes.addAll(configuredExcludes);

		String includesString = String.join(",", includes);
		String excludesString = String.join(",", excludes);

		try {
			final List<File> files = FileUtils.getFiles(baseDir, includesString, excludesString);
			if (filePatterns == null || filePatterns.isEmpty()) {
				return files;
			}
			final String[] includePatterns = this.filePatterns.split(",");
			final List<Pattern> compiledIncludePatterns = Arrays.stream(includePatterns)
					.map(Pattern::compile)
					.collect(Collectors.toList());
			final Predicate<File> shouldInclude = file -> compiledIncludePatterns
					.stream()
					.anyMatch(filePattern -> filePattern.matcher(file.getAbsolutePath())
							.matches());
			return files
					.stream()
					.filter(shouldInclude)
					.collect(toList());
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to scan file tree rooted at " + baseDir, e);
		}
	}

	private static String withTrailingSeparator(String path) {
		return path.endsWith(File.separator) ? path : path + File.separator;
	}

	private FormatterConfig getFormatterConfig() {
		ArtifactResolver resolver = new ArtifactResolver(repositorySystem, repositorySystemSession, repositories, getLog());
		Provisioner provisioner = MavenProvisioner.create(resolver);
		List<FormatterStepFactory> formatterStepFactories = getFormatterStepFactories();
		FileLocator fileLocator = getFileLocator();
		return new FormatterConfig(baseDir, encoding, lineEndings, provisioner, fileLocator, formatterStepFactories);
	}

	private FileLocator getFileLocator() {
		resourceManager.addSearchPath(FileResourceLoader.ID, baseDir.getAbsolutePath());
		resourceManager.addSearchPath("url", "");
		resourceManager.setOutputDirectory(buildDir);
		return new FileLocator(resourceManager, baseDir, buildDir);
	}

	private List<FormatterFactory> getFormatterFactories() {
		return Stream.concat(formats.stream(), Stream.of(java, scala, kotlin, cpp, typescript, css, xml))
				.filter(Objects::nonNull)
				.collect(toList());
	}

	private List<FormatterStepFactory> getFormatterStepFactories() {
		return Stream.of(licenseHeader)
				.filter(Objects::nonNull)
				.collect(toList());
	}
}
