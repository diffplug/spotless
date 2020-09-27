/*
 * Copyright 2016-2020 DiffPlug
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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.resource.ResourceManager;
import org.codehaus.plexus.resource.loader.FileResourceLoader;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.MatchPatterns;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.generic.LicenseHeaderStep;
import com.diffplug.spotless.maven.antlr4.Antlr4;
import com.diffplug.spotless.maven.cpp.Cpp;
import com.diffplug.spotless.maven.generic.Format;
import com.diffplug.spotless.maven.generic.LicenseHeader;
import com.diffplug.spotless.maven.groovy.Groovy;
import com.diffplug.spotless.maven.java.Java;
import com.diffplug.spotless.maven.kotlin.Kotlin;
import com.diffplug.spotless.maven.scala.Scala;
import com.diffplug.spotless.maven.sql.Sql;
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
	private String ratchetFrom;

	@Parameter
	private LicenseHeader licenseHeader;

	@Parameter
	private List<Format> formats = Collections.emptyList();

	@Parameter
	private Groovy groovy;

	@Parameter
	private Java java;

	@Parameter
	private Scala scala;

	@Parameter
	private Kotlin kotlin;

	@Parameter
	private Cpp cpp;

	@Parameter
	private Typescript typescript;

	@Parameter
	private Antlr4 antlr4;

	@Parameter
	private Sql sql;

	@Parameter(property = "spotlessFiles")
	private String filePatterns;

	@Parameter(property = LicenseHeaderStep.spotlessSetLicenseHeaderYearsFromGitHistory)
	private String setLicenseHeaderYearsFromGitHistory;

	protected abstract void process(Iterable<File> files, Formatter formatter) throws MojoExecutionException;

	@Override
	public final void execute() throws MojoExecutionException {
		List<FormatterFactory> formatterFactories = getFormatterFactories();
		for (FormatterFactory formatterFactory : formatterFactories) {
			execute(formatterFactory);
		}
	}

	private void execute(FormatterFactory formatterFactory) throws MojoExecutionException {
		FormatterConfig config = getFormatterConfig();
		List<File> files = collectFiles(formatterFactory, config);

		try (Formatter formatter = formatterFactory.newFormatter(files, config)) {
			process(files, formatter);
		}
	}

	private List<File> collectFiles(FormatterFactory formatterFactory, FormatterConfig config) throws MojoExecutionException {
		Optional<String> ratchetFrom = formatterFactory.ratchetFrom(config);
		try {
			final List<File> files;
			if (ratchetFrom.isPresent()) {
				files = collectFilesFromGit(formatterFactory, ratchetFrom.get());
			} else {
				files = collectFilesFromFormatterFactory(formatterFactory);
			}
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

	private List<File> collectFilesFromGit(FormatterFactory formatterFactory, String ratchetFrom) throws MojoExecutionException {
		MatchPatterns includePatterns = MatchPatterns.from(
				withNormalizedFileSeparators(getIncludes(formatterFactory)));
		MatchPatterns excludePatterns = MatchPatterns.from(
				withNormalizedFileSeparators(getExcludes(formatterFactory)));

		Iterable<String> dirtyFiles;
		try {
			dirtyFiles = GitRatchetMaven
					.instance().getDirtyFiles(baseDir, ratchetFrom);
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to scan file tree rooted at " + baseDir, e);
		}

		List<File> result = new ArrayList<>();
		for (String file : withNormalizedFileSeparators(dirtyFiles)) {
			if (includePatterns.matches(file, true)) {
				if (!excludePatterns.matches(file, true)) {
					result.add(Paths.get(baseDir.getPath(), file).toFile());
				}
			}
		}
		return result;
	}

	private List<File> collectFilesFromFormatterFactory(FormatterFactory formatterFactory)
			throws MojoExecutionException, IOException {
		String includesString = String.join(",", getIncludes(formatterFactory));
		String excludesString = String.join(",", getExcludes(formatterFactory));

		return FileUtils.getFiles(baseDir, includesString, excludesString);
	}

	private Iterable<String> withNormalizedFileSeparators(Iterable<String> patterns) {
		return StreamSupport.stream(patterns.spliterator(), true)
				.map(pattern -> pattern.replace('/', File.separatorChar))
				.map(pattern -> pattern.replace('\\', File.separatorChar))
				.collect(Collectors.toSet());
	}

	private static String withTrailingSeparator(String path) {
		return path.endsWith(File.separator) ? path : path + File.separator;
	}

	private Set<String> getIncludes(FormatterFactory formatterFactory) throws MojoExecutionException {
		Set<String> configuredIncludes = formatterFactory.includes();
		Set<String> includes = configuredIncludes.isEmpty() ? formatterFactory.defaultIncludes() : configuredIncludes;
		if (includes.isEmpty()) {
			throw new MojoExecutionException("You must specify some files to include, such as '<includes><include>src/**</include></includes>'");
		}
		return includes;
	}

	private Set<String> getExcludes(FormatterFactory formatterFactory) {
		Set<String> configuredExcludes = formatterFactory.excludes();

		Set<String> excludes = new HashSet<>(FileUtils.getDefaultExcludesAsList());
		excludes.add(withTrailingSeparator(buildDir.toString()));
		excludes.addAll(configuredExcludes);
		return excludes;
	}

	private FormatterConfig getFormatterConfig() {
		ArtifactResolver resolver = new ArtifactResolver(repositorySystem, repositorySystemSession, repositories, getLog());
		Provisioner provisioner = MavenProvisioner.create(resolver);
		List<FormatterStepFactory> formatterStepFactories = getFormatterStepFactories();
		FileLocator fileLocator = getFileLocator();
		return new FormatterConfig(baseDir, encoding, lineEndings, Optional.ofNullable(ratchetFrom), provisioner, fileLocator, formatterStepFactories, Optional.ofNullable(setLicenseHeaderYearsFromGitHistory));
	}

	private FileLocator getFileLocator() {
		resourceManager.addSearchPath(FileResourceLoader.ID, baseDir.getAbsolutePath());
		resourceManager.addSearchPath("url", "");
		resourceManager.setOutputDirectory(buildDir);
		return new FileLocator(resourceManager, baseDir, buildDir);
	}

	private List<FormatterFactory> getFormatterFactories() {
		return Stream.concat(formats.stream(), Stream.of(groovy, java, scala, kotlin, cpp, typescript, antlr4, sql))
				.filter(Objects::nonNull)
				.collect(toList());
	}

	private List<FormatterStepFactory> getFormatterStepFactories() {
		return Stream.of(licenseHeader)
				.filter(Objects::nonNull)
				.collect(toList());
	}
}
