/*
 * Copyright 2016-2023 DiffPlug
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
package com.diffplug.spotless.maven.rome;

import java.nio.file.Paths;

import org.apache.maven.plugins.annotations.Parameter;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.FormatterStepFactory;
import com.diffplug.spotless.rome.RomeStep;
import com.diffplug.spotless.rome.RomeStep.Builder;

/**
 * Factory for creating the Rome formatter step that can format format code in
 * various types of language with Rome. Currently Rome support JavaScript,
 * TypeScript, JSX, TSX, and JSON. See also
 * <a href= "https://github.com/rome/tools">https://github.com/rome/tools</a>.
 * It delegates to the Rome CLI executable.
 */
public abstract class AbstractRome implements FormatterStepFactory {
	/**
	 * Optional path to the directory with configuration file for Rome. The file
	 * must be named {@code rome.json}. When none is given, the default
	 * configuration is used. If this is a relative path, it is resolved against the
	 * project's base directory.
	 */
	@Parameter
	private String configPath;

	/**
	 * Optional directory where the downloaded Rome executable is placed. If this is
	 * a relative path, it is resolved against the project's base directory.
	 * Defaults to
	 * <code>~/.m2/repository/com/diffplug/spotless/spotless-data/rome</code>.
	 * <p>
	 * You can use an expression like <code>${user.home}/rome</code> if you want to
	 * use the home directory, or <code>${project.build.directory</code> if you want
	 * to use the target directory of the current project.
	 */
	@Parameter
	private String downloadDir;

	/**
	 * Optional path to the Rome executable. Either a <code>version</code> or a
	 * <code>pathToExe</code> should be specified. When not given, an attempt is
	 * made to download the executable for the given version from the network. When
	 * given, the executable is used and the <code>version</code> parameter is
	 * ignored.
	 * <p>
	 * When an absolute path is given, that path is used as-is. When a relative path
	 * is given, it is resolved against the project's base directory. When only a
	 * file name (i.e. without any slashes or back slash path separators such as
	 * {@code rome}) is given, this is interpreted as the name of a command with
	 * executable that is in your {@code path} environment variable. Use
	 * {@code ./executable-name} if you want to use an executable in the project's
	 * base directory.
	 */
	@Parameter
	private String pathToExe;

	/**
	 * Rome version to download, applies only when no <code>pathToExe</code> is
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
		var step = builder.build();
		return step.create();
	}

	/**
	 * Gets the language (syntax) of the input files to format. When
	 * <code>null</code> or the empty string, the language is detected automatically
	 * from the file name. Currently the following languages are supported by Rome:
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
	 * A new builder for configuring a Rome step that either downloads the Rome
	 * executable with the given version from the network, or uses the executable
	 * from the given path.
	 *
	 * @param config Configuration from the Maven Mojo execution with details about
	 *               the currently executed project.
	 * @return A builder for a Rome step.
	 */
	private Builder newBuilder(FormatterStepConfig config) {
		if (pathToExe != null) {
			var resolvedExePath = resolveExePath(config);
			return RomeStep.withExePath(resolvedExePath);
		} else {
			var downloadDir = resolveDownloadDir(config);
			return RomeStep.withExeDownload(version, downloadDir);
		}
	}

	/**
	 * Resolves the path to the configuration file for Rome. Relative paths are
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
	 * Resolves the path to the Rome executable. When the path is only a file name,
	 * do not perform any resolution and interpret it as a command that must be on
	 * the user's path. Otherwise resolve the executable path against the project's
	 * base directory.
	 *
	 * @param config Configuration from the Maven Mojo execution with details about
	 *               the currently executed project.
	 * @return The resolved path to the Rome executable.
	 */
	private String resolveExePath(FormatterStepConfig config) {
		var path = Paths.get(pathToExe);
		if (path.getNameCount() == 1) {
			return path.toString();
		} else {
			return config.getFileLocator().getBaseDir().toPath().resolve(path).toAbsolutePath().toString();
		}
	}

	/**
	 * Resolves the directory to use for storing downloaded Rome executable. When a
	 * {@link #downloadDir} is given, use that directory, resolved against the
	 * current project's directory. Otherwise, use the {@code Rome} sub folder in
	 * the shared data directory.
	 *
	 * @param config Configuration for this step.
	 * @return The download directory for the Rome executable.
	 */
	private String resolveDownloadDir(FormatterStepConfig config) {
		final var fileLocator = config.getFileLocator();
		if (downloadDir != null && !downloadDir.isBlank()) {
			return fileLocator.getBaseDir().toPath().resolve(downloadDir).toAbsolutePath().toString();
		} else {
			return fileLocator.getDataDir().toPath().resolve("rome").toString();
		}
	}
}
