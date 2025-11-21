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
package com.diffplug.spotless.biome;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.ForeignExe;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ProcessRunner;

/**
 * formatter step that formats JavaScript and TypeScript code with Biome:
 * <a href= "https://github.com/biomejs/biome">https://github.com/biomejs/biome</a>.
 * It delegates to the Biome executable. The Biome executable is downloaded from
 * the network when no executable path is provided explicitly.
 */
public final class BiomeStep {
	private static final Logger LOGGER = LoggerFactory.getLogger(BiomeStep.class);

	/**
	 * Path to the directory with the {@code biome.json} config file, can be
	 * <code>null</code>, in which case the defaults are used.
	 */
	private String configPath;

	/**
	 * The language (syntax) of the input files to format. When <code>null</code> or
	 * the empty string, the language is detected automatically from the file name.
	 * Currently, the following languages are supported by Biome:
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
	 */
	private String language;

	/**
	 * Path to the Biome executable. Can be <code>null</code>, but either a path to
	 * the executable of a download directory and version must be given. The path
	 * must be either an absolute path, or a file name without path separators. If
	 * the latter, it is interpreted as a command in the user's path.
	 */
	private final String pathToExe;

	/**
	 * Absolute path to the download directory for storing the download Biome
	 * executable. Can be <code>null</code>, but either a path to the executable of
	 * a download directory and version must be given.
	 */
	private final String downloadDir;

	/**
	 * Version of Biome to download. Can be <code>null</code>, but either a path to
	 * the executable of a download directory and version must be given.
	 */
	private final String version;

	/**
	 * @return The name of this format step, i.e. <code>biome</code> or <code>rome</code>.
	 */
	public String name() {
		return BiomeSettings.shortName();
	}

	/**
	 * Creates a Biome step that format code by downloading to the given Biome
	 * version. The executable is downloaded from the network.
	 *
	 * @param version     Version of the Biome executable to download.
	 * @param downloadDir Directory where to place the downloaded executable.
	 * @return A new Biome step that download the executable from the network.
	 */
	public static BiomeStep withExeDownload(String version, String downloadDir) {
		return new BiomeStep(version, null, downloadDir);
	}

	/**
	 * Creates a Biome step that formats code by delegating to the Biome executable
	 * located at the given path.
	 *
	 * @param pathToExe Path to the Biome executable to use.
	 * @return A new Biome step that format with the given executable.
	 */
	public static BiomeStep withExePath(String pathToExe) {
		return new BiomeStep(null, pathToExe, null);
	}

	/**
	 * Attempts to add a POSIX permission to the given file, ignoring any errors.
	 * All existing permissions on the file are preserved and the new permission is
	 * added, if possible.
	 *
	 * @param file       File or directory to which to add a permission.
	 * @param permission The POSIX permission to add.
	 */
	private static void attemptToAddPosixPermission(Path file, PosixFilePermission permission) {
		try {
			var newPermissions = new HashSet<>(Files.getPosixFilePermissions(file));
			newPermissions.add(permission);
			Files.setPosixFilePermissions(file, newPermissions);
		} catch (final Exception ignore) {
			LOGGER.debug("Unable to add POSIX permission '{}' to file '{}'", permission, file);
		}
	}

	/**
	 * Finds the default version for Biome when no version is specified explicitly.
	 * Over time this will become outdated -- people should always specify the
	 * version explicitly!
	 *
	 * @return The default version for Biome.
	 */
	private static String defaultVersion() {
		return BiomeSettings.defaultVersion();
	}

	/**
	 * Attempts to make the given file executable. This is a best-effort attempt,
	 * any errors are swallowed. Depending on the OS, the file might still be
	 * executable even if this method fails. The user will get a descriptive error
	 * later when we attempt to execute the Biome executable.
	 *
	 * @param filePath Path to the file to make executable.
	 */
	private static void makeExecutable(String filePath) {
		var exePath = Path.of(filePath);
		attemptToAddPosixPermission(exePath, PosixFilePermission.GROUP_EXECUTE);
		attemptToAddPosixPermission(exePath, PosixFilePermission.OTHERS_EXECUTE);
		attemptToAddPosixPermission(exePath, PosixFilePermission.OWNER_EXECUTE);
	}

	/**
	 * Finds the absolute path of a command on the user's path. Uses {@code which}
	 * for Linux and {@code where} for Windows.
	 *
	 * @param name Name of the command to resolve.
	 * @return The absolute path of the command's executable.
	 * @throws IOException          When the command could not be resolved.
	 * @throws InterruptedException When this thread was interrupted while waiting
	 *                              to the which command to finish.
	 */
	private static String resolveNameAgainstPath(String name) throws IOException, InterruptedException {
		try (var runner = new ProcessRunner()) {
			var cmdWhich = runner.shellWinUnix("where " + name, "which " + name);
			if (cmdWhich.exitNotZero()) {
				throw new IOException("Unable to find " + name + " on path via command " + cmdWhich);
			} else {
				return cmdWhich.assertExitZero(Charset.defaultCharset()).trim();
			}
		}
	}

	/**
	 * Checks the Biome config path. When the config path does not exist or when it
	 * does not contain a file named {@code biome.json}, an error is thrown.
	 * @param configPath The path to validate.
	 * @param version The version of Biome.
	 */
	private static void validateBiomeConfigPath(String configPath, String version) {
		if (configPath == null) {
			return;
		}
		var atLeastV2 = BiomeSettings.versionHigherThanOrEqualTo(version, 2, 0, 0);
		var path = Path.of(configPath);
		var configFile = Files.isRegularFile(path) && atLeastV2 ? path : path.resolve(BiomeSettings.configName());
		if (!Files.exists(path)) {
			throw new IllegalArgumentException("Biome config directory does not exist: " + path);
		}
		if (!Files.exists(configFile)) {
			throw new IllegalArgumentException("Biome config does not exist: " + configFile);
		}
	}

	/**
	 * Checks the Biome executable file. When the file does not exist, an error is
	 * thrown.
	 */
	private static void validateBiomeExecutable(String resolvedPathToExe) {
		if (!new File(resolvedPathToExe).isFile()) {
			throw new IllegalArgumentException("Biome executable does not exist: " + resolvedPathToExe);
		}
	}

	/**
	 * Creates a new Biome step with the configuration from the given builder.
	 *
	 * @param version     Version of the Biome executable to download.
	 * @param pathToExe Path to the Biome executable to use.
	 * @param downloadDir Directory where to place the downloaded executable.
	 */
	private BiomeStep(String version, String pathToExe, String downloadDir) {
		this.version = version != null && !version.isBlank() ? version : defaultVersion();
		this.pathToExe = pathToExe;
		this.downloadDir = downloadDir;
	}

	/**
	 * Creates a formatter step with the current configuration, which formats code
	 * by passing it to the Biome executable.
	 *
	 * @return A new formatter step for formatting with Biome.
	 */
	public FormatterStep create() {
		return FormatterStep.createLazy(name(), this::createState, State::toFunc);
	}

	/**
	 * Sets the path to the Biome configuration. Must be either a directory with a file named {@code biome.json}, or
	 * a file with the Biome config as JSON. When no config path is set, the default configuration is used.
	 *
	 * @param configPath Config path to use.
	 * @return This builder instance for chaining method calls.
	 */
	public BiomeStep withConfigPath(String configPath) {
		this.configPath = configPath;
		return this;
	}

	/**
	 * Sets the language of the files to format When no language is set, it is
	 * determined automatically from the file name. The following languages are
	 * currently supported by Biome.
	 *
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
	 * @param language The language of the files to format.
	 * @return This builder instance for chaining method calls.
	 */
	public BiomeStep withLanguage(String language) {
		this.language = language;
		return this;
	}

	/**
	 * Resolves the Biome executable, possibly downloading it from the network, and
	 * creates a new state instance with the resolved executable that can format
	 * code via Biome.
	 *
	 * @return The state instance for formatting code via Biome.
	 * @throws IOException          When any file system or network operations
	 *                              failed, such as when the Biome executable could
	 *                              not be downloaded, or when the given executable
	 *                              does not exist.
	 * @throws InterruptedException When the Biome executable needs to be downloaded
	 *                              and this thread was interrupted while waiting
	 *                              for the download to complete.
	 */
	private State createState() throws IOException, InterruptedException {
		var resolvedPathToExe = resolveExe();
		validateBiomeExecutable(resolvedPathToExe);
		validateBiomeConfigPath(configPath, version);
		LOGGER.debug("Using Biome executable located at  '{}'", resolvedPathToExe);
		var exeSignature = FileSignature.signAsList(new File(resolvedPathToExe));
		makeExecutable(resolvedPathToExe);
		return new State(resolvedPathToExe, exeSignature, configPath, language);
	}

	/**
	 * Resolves the path to the Biome executable, given the configuration of this
	 * step. When the path to the Biome executable is given explicitly, that path is
	 * used as-is. Otherwise, at attempt is made to download the Biome executable for
	 * the configured version from the network, unless it was already downloaded and
	 * is available in the cache.
	 *
	 * @return The path to the resolved Biome executable.
	 * @throws IOException          When any file system or network operations
	 *                              failed, such as when the Biome executable could
	 *                              not be downloaded.
	 * @throws InterruptedException When the Biome executable needs to be downloaded
	 *                              and this thread was interrupted while waiting
	 *                              for the download to complete.
	 */
	private String resolveExe() throws IOException, InterruptedException {
		new ForeignExe();
		if (pathToExe != null) {
			if (Path.of(pathToExe).getNameCount() == 1) {
				return resolveNameAgainstPath(pathToExe);
			} else {
				return pathToExe;
			}
		} else {
			var downloader = new BiomeExecutableDownloader(Path.of(downloadDir));
			var downloaded = downloader.ensureDownloaded(version).toString();
			makeExecutable(downloaded);
			return downloaded;
		}
	}

	/**
	 * The internal state used by the Biome formatter. A state instance is created
	 * when the spotless plugin for Maven or Gradle is executed, and reused for all
	 * formatting requests for different files. The lifetime of the instance ends
	 * when the Maven or Gradle plugin was successfully executed.
	 * <p>
	 * The state encapsulated a particular executable. It is serializable for
	 * caching purposes. Spotless keeps a cache of which files need to be formatted.
	 * The cache is busted when the serialized form of a state instance changes.
	 */
	private static final class State implements Serializable {
		private static final long serialVersionUID = 6846790911054484379L;

		/** Path to the exe file */
		private final String pathToExe;

		/** The signature of the exe file, if any, used for caching. */
		@SuppressWarnings("unused")
		private final FileSignature exeSignature;

		/**
		 * The optional path to the directory with the {@code biome.json} config file.
		 */
		private final String configPath;

		/**
		 * The language of the files to format. When <code>null</code> or the empty
		 * string, the language is detected from the file name.
		 */
		private final String language;

		/**
		 * Creates a new state for instance which can format code with the given Biome
		 * executable.
		 *
		 * @param exe          Path to the Biome executable.
		 * @param exeSignature Signature (e.g. SHA-256 checksum) of the Biome executable.
		 * @param configPath   Path to the optional directory with the {@code biome.json}
		 *                     config file, can be <code>null</code>, in which case the
		 *                     defaults are used.
		 */
		private State(String exe, FileSignature exeSignature, String configPath, String language) {
			this.pathToExe = exe;
			this.exeSignature = exeSignature;
			this.configPath = configPath;
			this.language = language;
		}

		/**
		 * Builds the list of arguments for the command that executes Biome to format a
		 * piece of code passed via stdin.
		 *
		 * @param file File to format.
		 * @return The Biome command to use for formatting code.
		 */
		private String[] buildBiomeCommand(File file) {
			var fileName = resolveFileName(file);
			var argList = new ArrayList<String>();
			argList.add(pathToExe);
			argList.add("format");
			argList.add("--stdin-file-path");
			argList.add(fileName);
			if (configPath != null) {
				argList.add("--config-path");
				argList.add(configPath);
			}
			return argList.toArray(String[]::new);
		}

		/**
		 * Formats the given piece of code by delegating to the Biome executable. The
		 * code is passed to Biome via stdin, the file name is used by Biome only to
		 * determine the code syntax (e.g. JavaScript or TypeScript).
		 *
		 * @param runner Process runner for invoking the Biome executable.
		 * @param input  Code to format.
		 * @param file   File to format.
		 * @return The formatted code.
		 * @throws IOException          When a file system error occurred while
		 *                              executing Biome.
		 * @throws InterruptedException When this thread was interrupted while waiting
		 *                              for Biome to finish formatting.
		 */
		private String format(ProcessRunner runner, String input, File file) throws IOException, InterruptedException {
			var stdin = input.getBytes(StandardCharsets.UTF_8);
			var args = buildBiomeCommand(file);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Running Biome command to format code: '{}'", String.join(", ", args));
			}
			var runnerResult = runner.exec(stdin, args);
			var stdErr = runnerResult.stdErrUtf8();
			if (!stdErr.isEmpty()) {
				LOGGER.warn("Biome stderr ouptut for file '{}'\n{}", file, stdErr.trim());
			}
			var formatted = runnerResult.assertExitZero(StandardCharsets.UTF_8);
			// When biome encounters an ignored file, it does not output any formatted code
			// Ignored files come from (a) the biome.json configuration file and (b) from
			// a list of hard-coded file names, such as package.json or tsconfig.json.
			if (formatted.isEmpty()) {
				return input;
			} else {
				return formatted;
			}
		}

		/**
		 * The Biome executable currently does not have a parameter to specify the
		 * expected language / syntax. Biome always determined the language from the file
		 * extension. This method returns the file name for the desired language when a
		 * language was requested explicitly, or the file name of the input file for
		 * auto-detection.
		 *
		 * @param file File to be formatted.
		 * @return The file name to pass to the Biome executable.
		 */
		private String resolveFileName(File file) {
			var name = file.getName();
			if (language == null || language.isBlank()) {
				return name;
			}
			var dot = name.lastIndexOf(".");
			var ext = dot >= 0 ? name.substring(dot + 1) : name;
			return switch (language) {
			case "js?" -> "jsx".equals(ext) || "js".equals(ext) || "mjs".equals(ext) || "cjs".equals(ext) ? name
						: "file.js";
			case "ts?" -> "tsx".equals(ext) || "ts".equals(ext) || "mts".equals(ext) || "cts".equals(ext) ? name
						: "file.js";
			case "js" -> "js".equals(ext) || "mjs".equals(ext) || "cjs".equals(ext) ? name : "file.js";
			case "jsx" -> "jsx".equals(ext) ? name : "file.jsx";
			case "ts" -> "ts".equals(ext) || "mts".equals(ext) || "cts".equals(ext) ? name : "file.ts";
			case "tsx" -> "tsx".equals(ext) ? name : "file.tsx";
			case "json" -> "json".equals(ext) ? name : "file.json";
			case "jsonc" -> "jsonc".equals(ext) ? name : "file.jsonc";
			case "css" -> "css".equals(ext) ? name : "file.css";
			// so that we can support new languages such as css or yaml when Biome adds
			// support for them without having to change the code
			default -> "file." + language;
			};
		}

		/**
		 * Creates a new formatter function for formatting a piece of code by delegating
		 * to the Biome executable.
		 *
		 * @return A formatter function for formatting code.
		 */
		private FormatterFunc.Closeable toFunc() {
			var runner = new ProcessRunner();
			return FormatterFunc.Closeable.of(runner, this::format);
		}
	}
}
