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
package com.diffplug.spotless.maven.generic;

import java.nio.file.Path;

import org.apache.maven.plugins.annotations.Parameter;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.biome.BiomeSettings;
import com.diffplug.spotless.biome.BiomeStep;
import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.FormatterStepFactory;

/**
 * Factory for creating the Biome formatter step that can format code in
 * various types of language with Biome. Currently, Biome supports JavaScript,
 * TypeScript, JSX, TSX, and JSON. See also <a href=
 * "https://github.com/biomejs/biome">https://github.com/biomejs/biome</a>. It
 * delegates to the Biome CLI executable.
 */
public abstract class AbstractBiome implements FormatterStepFactory {

	protected AbstractBiome() {}

	/**
	 * Optional path to the configuration file for Biome. Must be either a directory that contains a file named
	 * {@code biome.json}, or a file that contains the Biome config as JSON. When none is given, the default
	 * configuration is used. If this is a relative path, it is resolved against the project's base directory.
	 */
	@Parameter
	private String configPath;

	/**
	 * Optional directory where the downloaded Biome executable is placed. If this
	 * is a relative path, it is resolved against the project's base directory.
	 * Defaults to
	 * <code>~/.m2/repository/com/diffplug/spotless/spotless-data/biome</code>.
	 * <p>
	 * You can use an expression like <code>${user.home}/biome</code> if you want to
	 * use the home directory, or <code>${project.build.directory</code> if you want
	 * to use the target directory of the current project.
	 */
	@Parameter
	private String downloadDir;

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
	@Parameter
	private String pathToExe;

	/**
	 * Biome version to download, applies only when no <code>pathToExe</code> is
	 * specified explicitly. Either a <code>version</code> or a
	 * <code>pathToExe</code> should be specified. When not given, a default known
	 * version is used. For stable builds, it is recommended that you always set the
	 * version explicitly. This parameter is ignored when you specify a
	 * <code>pathToExe</code> explicitly.
	 */
	@Parameter
	private String version;

	@Override
	public FormatterStep newFormatterStep(FormatterStepConfig config) {
		var builder = newBuilder(config);
		if (configPath != null) {
			var resolvedConfigFile = resolveConfigFile(config);
			builder.withConfigPath(resolvedConfigFile);
		}
		if (getLanguage() != null) {
			builder.withLanguage(getLanguage());
		}
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
	 * <li>css (CSS, requires biome &gt;= 1.9.0)</li>
	 * <li>json (JSON)</li>
	 * <li>jsonc (JSON + comments)</li>
	 * </ul>
	 *
	 * @return The language of the input files.
	 */
	protected abstract String getLanguage();

	/**
	 * A new builder for configuring a Biome step that either downloads the Biome
	 * executable with the given version from the network, or uses the executable
	 * from the given path.
	 *
	 * @param config Configuration from the Maven Mojo execution with details about
	 *               the currently executed project.
	 * @return A builder for a Biome step.
	 */
	private BiomeStep newBuilder(FormatterStepConfig config) {
		if (pathToExe != null) {
			var resolvedExePath = resolveExePath(config);
			return BiomeStep.withExePath(resolvedExePath);
		} else {
			var downloadDir = resolveDownloadDir(config);
			return BiomeStep.withExeDownload(version, downloadDir);
		}
	}

	/**
	 * Resolves the path to the configuration file for Biome. Relative paths are
	 * resolved against the project's base directory.
	 *
	 * @param config Configuration from the Maven Mojo execution with details about
	 *               the currently executed project.
	 * @return The resolved path to the configuration file.
	 */
	private String resolveConfigFile(FormatterStepConfig config) {
		return config.getFileLocator().getBaseDir().toPath().resolve(configPath).toAbsolutePath().toString();
	}

	/**
	 * Resolves the path to the Biome executable. When the path is only a file name,
	 * do not perform any resolution and interpret it as a command that must be on
	 * the user's path. Otherwise resolve the executable path against the project's
	 * base directory.
	 *
	 * @param config Configuration from the Maven Mojo execution with details about
	 *               the currently executed project.
	 * @return The resolved path to the Biome executable.
	 */
	private String resolveExePath(FormatterStepConfig config) {
		var path = Path.of(pathToExe);
		if (path.getNameCount() == 1) {
			return path.toString();
		} else {
			return config.getFileLocator().getBaseDir().toPath().resolve(path).toAbsolutePath().toString();
		}
	}

	/**
	 * Resolves the directory to use for storing downloaded Biome executable. When a
	 * {@link #downloadDir} is given, use that directory, resolved against the
	 * current project's directory. Otherwise, use the <code>biome</code> sub folder
	 * in the shared data directory.
	 *
	 * @param config Configuration for this step.
	 * @return The download directory for the Biome executable.
	 */
	private String resolveDownloadDir(FormatterStepConfig config) {
		final var fileLocator = config.getFileLocator();
		if (downloadDir != null && !downloadDir.isBlank()) {
			return fileLocator.getBaseDir().toPath().resolve(downloadDir).toAbsolutePath().toString();
		} else {
			return fileLocator.getDataDir().toPath().resolve(BiomeSettings.shortName()).toString();
		}
	}
}
