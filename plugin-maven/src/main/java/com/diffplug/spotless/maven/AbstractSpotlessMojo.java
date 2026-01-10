/*
 * Copyright 2016-2025 DiffPlug
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

import static com.diffplug.common.base.Strings.isNullOrEmpty;
import static com.diffplug.spotless.generic.LicenseHeaderStep.SPOTLESS_SET_LICENSE_HEADER_YEARS_FROM_GIT_HISTORY;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.resource.ResourceManager;
import org.codehaus.plexus.resource.loader.FileResourceLoader;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.MatchPatterns;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.Jvm;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.LintState;
import com.diffplug.spotless.LintSuppression;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.maven.antlr4.Antlr4;
import com.diffplug.spotless.maven.cpp.Cpp;
import com.diffplug.spotless.maven.css.Css;
import com.diffplug.spotless.maven.generic.Format;
import com.diffplug.spotless.maven.generic.LicenseHeader;
import com.diffplug.spotless.maven.gherkin.Gherkin;
import com.diffplug.spotless.maven.go.Go;
import com.diffplug.spotless.maven.groovy.Groovy;
import com.diffplug.spotless.maven.incremental.UpToDateChecker;
import com.diffplug.spotless.maven.incremental.UpToDateChecking;
import com.diffplug.spotless.maven.java.Java;
import com.diffplug.spotless.maven.javascript.Javascript;
import com.diffplug.spotless.maven.json.Json;
import com.diffplug.spotless.maven.kotlin.Kotlin;
import com.diffplug.spotless.maven.markdown.Markdown;
import com.diffplug.spotless.maven.pom.Pom;
import com.diffplug.spotless.maven.protobuf.Protobuf;
import com.diffplug.spotless.maven.python.Python;
import com.diffplug.spotless.maven.rdf.Rdf;
import com.diffplug.spotless.maven.scala.Scala;
import com.diffplug.spotless.maven.shell.Shell;
import com.diffplug.spotless.maven.sql.Sql;
import com.diffplug.spotless.maven.typescript.Typescript;
import com.diffplug.spotless.maven.yaml.Yaml;

public abstract class AbstractSpotlessMojo extends AbstractMojo {
	private static final String DEFAULT_INDEX_FILE_NAME = "spotless-index";
	private static final String DEFAULT_ENCODING = "UTF-8";
	private static final String DEFAULT_LINE_ENDINGS = "GIT_ATTRIBUTES_FAST_ALLSAME";

	/** Value to allow unsetting the ratchet inherited from parent pom configuration. */
	static final String RATCHETFROM_NONE = "NONE";

	static final String GOAL_CHECK = "check";
	static final String GOAL_APPLY = "apply";
	static final String GOAL_PRE_PUSH_HOOK = "install-git-pre-push-hook";

	@Component
	private RepositorySystem repositorySystem;

	@Component
	private ResourceManager resourceManager;

	@Component
	protected BuildContext buildContext;

	@Parameter(defaultValue = "${mojoExecution.goal}", required = true, readonly = true)
	private String goal;

	@Parameter(property = "spotless.skip", defaultValue = "false")
	private boolean skip;

	@Parameter(property = "spotless.apply.skip", defaultValue = "false")
	private boolean applySkip;

	@Parameter(property = "spotless.check.skip", defaultValue = "false")
	private boolean checkSkip;

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	private MavenProject project;

	@Parameter(defaultValue = "${repositorySystemSession}", required = true, readonly = true)
	private RepositorySystemSession repositorySystemSession;

	@Parameter(defaultValue = "${project.remotePluginRepositories}", required = true, readonly = true)
	private List<RemoteRepository> repositories;

	@Parameter(defaultValue = "${project.basedir}", required = true, readonly = true)
	protected File baseDir;

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
	private Css css;

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
	private Javascript javascript;

	@Parameter
	private Antlr4 antlr4;

	@Parameter
	private Pom pom;

	@Parameter
	private Sql sql;

	@Parameter
	private Python python;

	@Parameter
	private Markdown markdown;

	@Parameter
	private Json json;

	@Parameter
	private Shell shell;

	@Parameter
	private Yaml yaml;

	@Parameter
	private Gherkin gherkin;

	@Parameter
	private Go go;

	@Parameter
	private Rdf rdf;

	@Parameter
	private Protobuf protobuf;

	@Parameter(property = "spotlessFiles")
	private String filePatterns;

	@Parameter(property = SPOTLESS_SET_LICENSE_HEADER_YEARS_FROM_GIT_HISTORY)
	private String setLicenseHeaderYearsFromGitHistory;

	@Parameter
	private UpToDateChecking upToDateChecking = UpToDateChecking.enabled();

	@Parameter
	private List<LintSuppression> lintSuppressions = new ArrayList<>();

	/**
	 * If set to {@code true} will also run on incremental builds (i.e. within Eclipse with m2e).
	 * Otherwise this goal is skipped in incremental builds and only runs on full builds.
	 */
	@Parameter(defaultValue = "false")
	protected boolean m2eEnableForIncrementalBuild;

	protected List<LintSuppression> getLintSuppressions() {
		return lintSuppressions;
	}

	protected abstract void process(String name, Iterable<File> files, Formatter formatter, UpToDateChecker upToDateChecker) throws MojoExecutionException;

	protected LintState calculateLintState(Formatter formatter, File file) throws IOException {
		String relativePath = LintSuppression.relativizeAsUnix(baseDir, file);
		if (relativePath == null) {
			// File is not within baseDir, use absolute path as fallback
			relativePath = file.getAbsolutePath();
		}
		return LintState.of(formatter, file).withRemovedSuppressions(formatter, relativePath, lintSuppressions);
	}

	private static final int MINIMUM_JRE = 11;

	protected AbstractSpotlessMojo() {
		if (Jvm.version() < MINIMUM_JRE) {
			throw new RuntimeException("Spotless requires JRE " + MINIMUM_JRE + " or newer, this was " + Jvm.version() + ".\n"
					+ "You can upgrade your build JRE and still compile for older targets, see below\n"
					+ "https://docs.gradle.org/current/userguide/building_java_projects.html#sec:java_cross_compilation");
		}
	}

	@Override
	public final void execute() throws MojoExecutionException {
		if (shouldSkip()) {
			getLog().info("Spotless %s skipped".formatted(goal));
			return;
		}

		List<FormatterFactory> formatterFactories = getFormatterFactories();
		FormatterConfig config = getFormatterConfig();

		Map<FormatterFactory, Supplier<Iterable<File>>> formatterFactoryToFiles = new LinkedHashMap<>();
		for (FormatterFactory formatterFactory : formatterFactories) {
			Supplier<Iterable<File>> filesToFormat = () -> collectFiles(formatterFactory, config);
			formatterFactoryToFiles.put(formatterFactory, filesToFormat);
		}

		try (FormattersHolder formattersHolder = FormattersHolder.create(formatterFactoryToFiles, config);
				UpToDateChecker upToDateChecker = createUpToDateChecker(formattersHolder.openFormatters.values())) {
			for (FormatterFactory factory : formattersHolder.openFormatters.keySet()) {
				Formatter formatter = formattersHolder.openFormatters.get(factory);
				Iterable<File> files = formattersHolder.factoryToFiles.get(factory).get();
				process(formattersHolder.nameFor(factory), files, formatter, upToDateChecker);
			}
		} catch (PluginException e) {
			throw e.asMojoExecutionException();
		}
	}

	private boolean shouldSkip() {
		if (skip) {
			return true;
		}
		if (buildContext.isIncremental() && !m2eEnableForIncrementalBuild) {
			getLog().debug("Skipping for incremental builds as parameter 'enableForIncrementalBuilds' is set to 'false'");
			return true;
		}

		switch (goal) {
		case GOAL_CHECK:
			return checkSkip;
		case GOAL_APPLY:
			return applySkip;
		default:
			break;
		}

		return false;
	}

	private List<File> collectFiles(FormatterFactory formatterFactory, FormatterConfig config) {
		Optional<String> ratchetFrom = formatterFactory.ratchetFrom(config);
		try {
			final List<File> files;
			if (ratchetFrom.isPresent()) {
				files = collectFilesFromGit(formatterFactory, ratchetFrom.orElseThrow());
			} else {
				files = collectFilesFromFormatterFactory(formatterFactory);
			}
			if (isNullOrEmpty(filePatterns)) {
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
			throw new PluginException("Unable to scan file tree rooted at " + baseDir, e);
		}
	}

	private List<File> collectFilesFromGit(FormatterFactory formatterFactory, String ratchetFrom) {
		MatchPatterns includePatterns = MatchPatterns.from(
				withNormalizedFileSeparators(getIncludes(formatterFactory)));
		MatchPatterns excludePatterns = MatchPatterns.from(
				withNormalizedFileSeparators(getExcludes(formatterFactory)));

		Iterable<String> dirtyFiles = DirtyFilesCache.instance().getDirtyFiles(baseDir, ratchetFrom);

		List<File> result = new ArrayList<>();
		for (String file : withNormalizedFileSeparators(dirtyFiles)) {
			if (includePatterns.matches(file, true)) {
				if (!excludePatterns.matches(file, true)) {
					result.add(new File(baseDir.getPath(), file));
				}
			}
		}
		return result;
	}

	private List<File> collectFilesFromFormatterFactory(FormatterFactory formatterFactory) throws IOException {
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

	private Set<String> getIncludes(FormatterFactory formatterFactory) {
		Set<String> configuredIncludes = formatterFactory.includes();
		Set<String> includes = configuredIncludes.isEmpty() ? formatterFactory.defaultIncludes(project) : configuredIncludes;
		if (includes.isEmpty()) {
			throw new PluginException("You must specify some files to include, such as '<includes><include>src/**/*.blah</include></includes>'");
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
		final Optional<String> optionalRatchetFrom = Optional.ofNullable(this.ratchetFrom)
				.filter(ratchet -> !RATCHETFROM_NONE.equals(ratchet));
		return new FormatterConfig(baseDir, encoding, lineEndings, optionalRatchetFrom, provisioner, fileLocator, formatterStepFactories, Optional.ofNullable(setLicenseHeaderYearsFromGitHistory), lintSuppressions);
	}

	private FileLocator getFileLocator() {
		resourceManager.addSearchPath(FileResourceLoader.ID, baseDir.getAbsolutePath());
		resourceManager.addSearchPath("url", "");
		resourceManager.setOutputDirectory(buildDir);
		return new FileLocator(resourceManager, baseDir, buildDir);
	}

	private List<FormatterFactory> getFormatterFactories() {
		return Stream.concat(formats.stream(), Stream.of(groovy, java, scala, kotlin, cpp, css, typescript, javascript, antlr4, pom, sql, python, markdown, json, shell, yaml, gherkin, go, rdf, protobuf))
				.filter(Objects::nonNull)
				.map(factory -> factory.init(repositorySystemSession))
				.collect(toList());
	}

	private List<FormatterStepFactory> getFormatterStepFactories() {
		return Stream.ofNullable(licenseHeader)
				.collect(toList());
	}

	private UpToDateChecker createUpToDateChecker(Iterable<Formatter> formatters) {
		Path indexFile = upToDateChecking == null ? null : upToDateChecking.getIndexFile();
		if (indexFile == null) {
			Path targetDir = project.getBasedir().toPath().resolve(project.getBuild().getDirectory());
			indexFile = targetDir.resolve(DEFAULT_INDEX_FILE_NAME);
		}
		final UpToDateChecker checker;
		if (upToDateChecking != null && upToDateChecking.isEnabled()) {
			checker = UpToDateChecker.forProject(project, indexFile, formatters, getLog());
		} else {
			getLog().info("Up-to-date checking disabled");
			checker = UpToDateChecker.noop(project, indexFile, getLog());
		}
		return UpToDateChecker.wrapWithBuildContext(checker, buildContext);
	}
}
