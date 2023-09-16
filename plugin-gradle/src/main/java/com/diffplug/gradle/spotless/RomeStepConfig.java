/*
 * Copyright 2023 DiffPlug
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

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.gradle.api.Project;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.rome.BiomeFlavor;
import com.diffplug.spotless.rome.RomeStep;

public abstract class RomeStepConfig<Self extends RomeStepConfig<Self>> {
	/**
	 * Optional path to the directory with configuration file for Biome. The file
	 * must be named {@code biome.json}. When none is given, the default
	 * configuration is used. If this is a relative path, it is resolved against the
	 * project's base directory.
	 */
	@Nullable
	private Object configPath;

	/**
	 * Optional directory where the downloaded Biome executable is placed. If this
	 * is a relative path, it is resolved against the project's base directory.
	 * Defaults to
	 * <code>~/.m2/repository/com/diffplug/spotless/spotless-data/biome</code>.
	 */
	@Nullable
	private Object downloadDir;

	/**
	 * The flavor of Biome to use. Will be removed when we stop support the
	 * deprecated Rome project.
	 */
	private final BiomeFlavor flavor;

	/**
	 * Optional path to the Biome executable. Either a <code>version</code> or a
	 * <code>pathToExe</code> should be specified. When not given, an attempt is
	 * made to download the executable for the given version from the network. When
	 * given, the executable is used and the <code>version</code> parameter is
	 * ignored.
	 * <p>
	 * When an absolute path is given, that path is used as-is. When a relative path
	 * is given, it is resolved against the project's base directory. When only a
	 * file name (i.e. without any slashes or back slash path separators such as
	 * <code>biome</code>) is given, this is interpreted as the name of a command
	 * with executable that is in your <code>path</code> environment variable. Use
	 * <code>./executable-name</code> if you want to use an executable in the
	 * project's base directory.
	 */
	@Nullable
	private Object pathToExe;

	/**
	 * A reference to the Gradle project for which spotless is executed.
	 */
	private final Project project;

	/**
	 * Replaces the current Biome formatter step with the given step.
	 */
	private final Consumer<FormatterStep> replaceStep;

	/**
	 * Biome version to download, applies only when no <code>pathToExe</code> is
	 * specified explicitly. Either a <code>version</code> or a
	 * <code>pathToExe</code> should be specified. When not given, a default known
	 * version is used. For stable builds, it is recommended that you always set the
	 * version explicitly. This parameter is ignored when you specify a
	 * <code>pathToExe</code> explicitly.
	 */
	@Nullable
	private String version;

	protected RomeStepConfig(Project project, Consumer<FormatterStep> replaceStep, BiomeFlavor flavor,
			String version) {
		this.project = requireNonNull(project);
		this.replaceStep = requireNonNull(replaceStep);
		this.flavor = flavor;
		this.version = version;
	}

	/**
	 * Optional path to the directory with configuration file for Biome. The file
	 * must be named {@code biome.json}. When none is given, the default
	 * configuration is used. If this is a relative path, it is resolved against the
	 * project's base directory.
	 *
	 * @return This step for further configuration.
	 */
	public Self configPath(Object configPath) {
		this.configPath = configPath;
		replaceStep();
		return getThis();
	}

	/**
	 * Optional directory where the downloaded Biome executable is placed. If this
	 * is a relative path, it is resolved against the project's base directory.
	 * Defaults to
	 * <code>~/.m2/repository/com/diffplug/spotless/spotless-data/biome</code>.
	 *
	 * @return This step for further configuration.
	 */
	public Self downloadDir(Object downloadDir) {
		this.downloadDir = downloadDir;
		replaceStep();
		return getThis();
	}

	/**
	 * Optional path to the Biome executable. Overwrites the configured version. No
	 * attempt is made to download the Biome executable from the network.
	 * <p>
	 * When an absolute path is given, that path is used as-is. When a relative path
	 * is given, it is resolved against the project's base directory. When only a
	 * file name (i.e. without any slashes or back slash path separators such as
	 * <code>biome</code>) is given, this is interpreted as the name of a command
	 * with executable that is in your <code>path</code> environment variable. Use
	 * <code>./executable-name</code> if you want to use an executable in the
	 * project's base directory.
	 *
	 * @return This step for further configuration.
	 */
	public Self pathToExe(Object pathToExe) {
		this.pathToExe = pathToExe;
		replaceStep();
		return getThis();
	}

	/**
	 * Creates a new formatter step that formats code by calling the Biome
	 * executable, using the current configuration.
	 *
	 * @return A new formatter step for the Biome formatter.
	 */
	protected FormatterStep createStep() {
		var builder = newBuilder();
		if (configPath != null) {
			var resolvedConfigPath = project.file(configPath);
			builder.withConfigPath(resolvedConfigPath.toString());
		}
		builder.withLanguage(getLanguage());
		return builder.create();
	}

	/**
	 * Gets the language (syntax) of the input files to format. When
	 * <code>null</code> or the empty string, the language is detected automatically
	 * from the file name. Currently the following languages are supported by Biome:
	 * <ul>
	 * <li>js (JavaScript)</li>
	 * <li>jsx (JavaScript + JSX)</li>
	 * <li>js? (JavaScript or JavaScript + JSX, depending on the file
	 * extension)</li>
	 * <li>ts (TypeScript)</li>
	 * <li>tsx (TypeScript + JSX)</li>
	 * <li>ts? (TypeScript or TypeScript + JSX, depending on the file
	 * extension)</li>
	 * <li>json (JSON)</li>
	 * </ul>
	 *
	 * @return The language of the input files.
	 */
	protected abstract String getLanguage();

	/**
	 * @return This Biome config instance.
	 */
	protected abstract Self getThis();

	/**
	 * Creates a new Biome step and replaces the existing Biome step in the list of
	 * format steps.
	 */
	protected void replaceStep() {
		replaceStep.accept(createStep());
	}

	/**
	 * Finds the data directory that can be used for storing shared data such as
	 * Biome executable globally. This is a directory in the local repository, e.g.
	 * <code>~/.m2/repository/com/diffplus/spotless/spotless-data<code>.
	 *
	 * @return The directory for storing shared data.
	 */
	private File findDataDir() {
		var currentRepo = project.getRepositories().stream().filter(r -> r instanceof MavenArtifactRepository)
				.map(r -> (MavenArtifactRepository) r).filter(r -> "file".equals(r.getUrl().getScheme())).findAny()
				.orElse(null);
		// Temporarily add mavenLocal() repository to get its file URL
		var localRepo = currentRepo != null ? (MavenArtifactRepository) currentRepo
				: project.getRepositories().mavenLocal();
		try {
			// e.g. ~/.m2/repository/
			var repoPath = Path.of(localRepo.getUrl());
			var dataPath = repoPath.resolve("com").resolve("diffplug").resolve("spotless").resolve("spotless-data");
			return dataPath.toAbsolutePath().toFile();
		} finally {
			// Remove mavenLocal() repository again if it was not part of the project
			if (currentRepo == null) {
				project.getRepositories().remove(localRepo);
			}
		}
	}

	/**
	 * A new builder for configuring a Biome step that either downloads the Biome
	 * executable with the given version from the network, or uses the executable
	 * from the given path.
	 *
	 * @return A builder for a Biome step.
	 */
	private RomeStep newBuilder() {
		if (pathToExe != null) {
			var resolvedPathToExe = resolvePathToExe();
			return RomeStep.withExePath(flavor, resolvedPathToExe);
		} else {
			var downloadDir = resolveDownloadDir();
			return RomeStep.withExeDownload(flavor, version, downloadDir);
		}
	}

	/**
	 * Resolves the path to the Biome executable. When the path is only a file name,
	 * do not perform any resolution and interpret it as a command that must be on
	 * the user's path. Otherwise resolve the executable path against the project's
	 * base directory.
	 *
	 * @return The resolved path to the Biome executable.
	 */
	private String resolvePathToExe() {
		var fileNameOnly = pathToExe instanceof String && Paths.get(pathToExe.toString()).getNameCount() == 1;
		if (fileNameOnly) {
			return pathToExe.toString();
		} else {
			return project.file(pathToExe).toString();
		}
	}

	/**
	 * Resolves the directory to use for storing downloaded Biome executable. When a
	 * {@link #downloadDir} is given, use that directory, resolved against the
	 * current project's directory. Otherwise, use the {@code biome} sub folder in
	 * the shared data directory.
	 *
	 * @return The download directory for the Biome executable.
	 */
	private String resolveDownloadDir() {
		if (downloadDir != null) {
			return project.file(downloadDir).toString();
		} else {
			return findDataDir().toPath().resolve(flavor.shortName()).toString();
		}
	}
}
