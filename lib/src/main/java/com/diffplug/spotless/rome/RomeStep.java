package com.diffplug.spotless.rome;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.ForeignExe;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ProcessRunner;

/**
 * formatter step that formats JavaScript and TypeScript code with Rome:
 * <a href= "https://github.com/rome/tools">https://github.com/rome/tools</a>.
 * It delegates to the Rome executable. The Rome executable is downloaded from
 * the network when no executable path is provided explicitly.
 */
public class RomeStep {
	private static final Logger logger = LoggerFactory.getLogger(RomeStep.class);

	/**
	 * Path to the directory with the {@code rome.json} config file, can be
	 * <code>null</code>, in which case the defaults are used.
	 */
	private final String configPath;

	/**
	 * The language (syntax) of the input files to format. When <code>null</code> or
	 * the empty string, the language is detected automatically from the file name.
	 * Currently the following languages are supported by Rome:
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
	 */
	private final String language;

	/**
	 * Path to the Rome executable. Can be <code>null</code>, but either a path to
	 * the executable of a download directory and version must be given. The path
	 * must be either an absolute path, or a file name without path separators. If
	 * the latter, it is interpreted as a command in the user's path.
	 */
	private final String pathToExe;

	/**
	 * Absolute path to the download directory for storing the download Rome
	 * executable. Can be <code>null</code>, but either a path to the executable of
	 * a download directory and version must be given.
	 */
	private final String downloadDir;

	/**
	 * Version of Rome to download. Can be <code>null</code>, but either a path to
	 * the executable of a download directory and version must be given.
	 */
	private final String version;

	/**
	 * @return The name of this format step, i.e. {@code rome}.
	 */
	public static String name() {
		return "rome";
	}

	/**
	 * Creates a Rome step that format code by downloading to the given Rome
	 * version. The executable is downloaded from the network.
	 * 
	 * @param version     Version of the Rome executable to download.
	 * @param downloadDir Directory where to place the downloaded executable.
	 * @return A new Rome step that download the executable from the network.
	 */
	public static RomeStep.Builder withExeDownload(String version, String downloadDir) {
		return new RomeStep.Builder(version, null, downloadDir);
	}

	/**
	 * Creates a Rome step that formats code by delegating to the Rome executable
	 * located at the given path.
	 * 
	 * @param pathToExe Path to the Rome executable to use.
	 * @return A new Rome step that format with the given executable.
	 */
	public static RomeStep.Builder withExePath(String pathToExe) {
		return new RomeStep.Builder(null, pathToExe, null);
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
		}
	}

	/**
	 * Finds the default version for Rome when no version is specified explicitly.
	 * Over time this will become outdated -- people should always specify the
	 * version explicitly!
	 * 
	 * @return The default version for Rome.
	 */
	private static String defaultVersion() {
		return "12.0.0";
	}

	/**
	 * Attempts to make the given file executable. This is a best-effort attempt,
	 * any errors are swallowed. Depending on the OS, the file might still be
	 * executable even if this method fails. The user will get a descriptive error
	 * later when we attempt to execute the Rome executable.
	 * 
	 * @param filePath Path to the file to make executable.
	 */
	private static void makeExecutable(String filePath) {
		var exePath = Paths.get(filePath);
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
	 * Checks the Rome config path. When the config path does not exist or when it
	 * does not contain a file named {@code rome.json}, an error is thrown.
	 */
	private static void validateRomeConfigPath(String configPath) {
		if (configPath == null) {
			return;
		}
		var path = Paths.get(configPath);
		var config = path.resolve("rome.json");
		if (!Files.exists(path)) {
			throw new IllegalArgumentException("Rome config directory does not exist: " + path);
		}
		if (!Files.exists(config)) {
			throw new IllegalArgumentException("Rome config does not exist: " + config);
		}
	}

	/**
	 * Checks the Rome executable file. When the file does not exist, an error is
	 * thrown.
	 */
	private static void validateRomeExecutable(String resolvedPathToExe) {
		if (!new File(resolvedPathToExe).isFile()) {
			throw new IllegalArgumentException("Rome executable does not exist: " + resolvedPathToExe);
		}
	}

	/**
	 * Creates a new Rome step with the configuration from the given builder.
	 * 
	 * @param builder Builder with the configuration to use.
	 */
	private RomeStep(RomeStep.Builder builder) {
		this.version = builder.version != null && !builder.version.isBlank() ? builder.version : defaultVersion();
		this.pathToExe = builder.pathToExe;
		this.downloadDir = builder.downloadDir;
		this.configPath = builder.configPath;
		this.language = builder.language;
	}

	/**
	 * Creates a formatter step with the current configuration, which formats code
	 * by passing it to the Rome executable.
	 * 
	 * @return A new formatter step for formatting with Rome.
	 */
	public FormatterStep create() {
		return FormatterStep.createLazy(name(), this::createState, State::toFunc);
	}

	/**
	 * Resolves the Rome executable, possibly downloading it from the network, and
	 * creates a new state instance with the resolved executable that can format
	 * code via Rome.
	 * 
	 * @return The state instance for formatting code via Rome.
	 * @throws IOException          When any file system or network operations
	 *                              failed, such as when the Rome executable could
	 *                              not be downloaded, or when the given executable
	 *                              does not exist.
	 * @throws InterruptedException When the Rome executable needs to be downloaded
	 *                              and this thread was interrupted while waiting
	 *                              for the download to complete.
	 */
	private State createState() throws IOException, InterruptedException {
		var resolvedPathToExe = resolveExe();
		validateRomeExecutable(resolvedPathToExe);
		validateRomeConfigPath(configPath);
		logger.debug("Using Rome executable located at  '{}'", resolvedPathToExe);
		var exeSignature = FileSignature.signAsList(Collections.singleton(new File(resolvedPathToExe)));
		makeExecutable(resolvedPathToExe);
		return new State(resolvedPathToExe, exeSignature, configPath, language);
	}

	/**
	 * Resolves the path to the Rome executable, given the configuration of this
	 * step. When the path to the Rome executable is given explicitly, that path is
	 * used as-is. Otherwise, at attempt is made to download the Rome executable for
	 * the configured version from the network, unless it was already downloaded and
	 * is available in the cache.
	 * 
	 * @return The path to the resolved Rome executable.
	 * @throws IOException          When any file system or network operations
	 *                              failed, such as when the Rome executable could
	 *                              not be downloaded.
	 * @throws InterruptedException When the Rome executable needs to be downloaded
	 *                              and this thread was interrupted while waiting
	 *                              for the download to complete.
	 */
	private String resolveExe() throws IOException, InterruptedException {
		new ForeignExe();
		if (pathToExe != null) {
			if (Paths.get(pathToExe).getNameCount() == 1) {
				return resolveNameAgainstPath(pathToExe);
			} else {
				return pathToExe;
			}
		} else {
			var downloader = new RomeExecutableDownloader(Paths.get(downloadDir));
			var downloaded = downloader.ensureDownloaded(version).toString();
			makeExecutable(downloaded);
			return downloaded;
		}
	}

	public final static class Builder {
		/** See {@link RomeStep#configPath} */
		private String configPath;

		/** See {@link RomeStep#downloadDir} */
		private final String downloadDir;

		/** See {@link RomeStep#language} */
		private String language;

		/** See {@link RomeStep#pathToExe} */
		private final String pathToExe;

		/** See {@link RomeStep#version} */
		private final String version;

		/**
		 * Creates a new builder for configuring a Rome step that can format code via
		 * Rome. Either a version and and downloadDir, or a pathToExe must be given.
		 * 
		 * @param version     The version of Rome to download, see
		 *                    {@link RomeStep#version}.
		 * @param pathToExe   The path to the Rome executable to use, see
		 *                    {@link RomeStep#pathToExe}.
		 * @param downloadDir The path to the download directory when downloading Rome
		 *                    from the network, {@link RomeStep#downloadDir}.
		 */
		private Builder(String version, String pathToExe, String downloadDir) {
			this.version = version;
			this.pathToExe = pathToExe;
			this.downloadDir = downloadDir;
		}

		/**
		 * Creates a new Rome step for formatting code with Rome from the current
		 * configuration of this builder.
		 * 
		 * @return A new Rome step with the current configuration.
		 */
		public RomeStep build() {
			return new RomeStep(this);
		}

		/**
		 * Sets the path to the directory with the {@code rome.json} config file. When
		 * no config path is set, the default configuration is used.
		 * 
		 * @param configPath Config path to use. Must point to a directory which contain
		 *                   a file named {@code rome.json}.
		 * @return This builder instance for chaining method calls.
		 */
		public Builder withConfigPath(String configPath) {
			this.configPath = configPath;
			return this;
		}

		/**
		 * Sets the language of the files to format When no language is set, it is
		 * determined automatically from the file name. The following languages are
		 * currently supported by Rome.
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
		 * <li>json (JSON)</li>
		 * </ul>
		 * 
		 * @param language The language of the files to format.
		 * @return This builder instance for chaining method calls.
		 */
		public Builder withLanguage(String language) {
			this.language = language;
			return this;
		}
	}

	/**
	 * The internal state used by the Rome formatter. A state instance is created
	 * when the spotless plugin for Maven or Gradle is executed, and reused for all
	 * formatting requests for different files. The lifetime of the instance ends
	 * when the Maven or Gradle plugin was successfully executed.
	 * <p>
	 * The state encapsulated a particular executable. It is serializable for
	 * caching purposes. Spotless keeps a cache of which files need to be formatted.
	 * The cache is busted when the serialized form of a state instance changes.
	 */
	private static class State implements Serializable {
		private static final long serialVersionUID = 6846790911054484379L;

		/** Path to the exe file */
		private final String pathToExe;

		/** The signature of the exe file, if any, used for caching. */
		@SuppressWarnings("unused")
		private final FileSignature exeSignature;

		/**
		 * The optional path to the directory with the {@code rome.json} config file.
		 */
		private final String configPath;

		/**
		 * The language of the files to format. When <code>null</code> or the empty
		 * string, the language is detected from the file name.
		 */
		private final String language;

		/**
		 * Creates a new state for instance which can format code with the given Rome
		 * executable.
		 * 
		 * @param exe          Path to the Rome executable.
		 * @param exeSignature Signature (e.g. SHA-256 checksum) of the Rome executable.
		 * @param configPath   Path to the optional directory with the {@code rome.json}
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
		 * Builds the list of arguments for the command that executes Rome to format a
		 * piece of code passed via stdin.
		 * 
		 * @param file File to format.
		 * @return The Rome command to use for formatting code.
		 */
		private String[] buildRomeCommand(File file) {
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
		 * Formats the given piece of code by delegating to the Rome executable. The
		 * code is passed to Rome via stdin, the file name is used by Rome only to
		 * determine the code syntax (e.g. JavaScript or TypeScript).
		 * 
		 * @param runner Process runner for invoking the Rome executable.
		 * @param input  Code to format.
		 * @param file   File to format.
		 * @return The formatted code.
		 * @throws IOException          When a file system error occurred while
		 *                              executing Rome.
		 * @throws InterruptedException When this thread was interrupted while waiting
		 *                              for Rome to finish formatting.
		 */
		private String format(ProcessRunner runner, String input, File file) throws IOException, InterruptedException {
			var stdin = input.getBytes(StandardCharsets.UTF_8);
			var args = buildRomeCommand(file);
			if (logger.isDebugEnabled()) {
				logger.debug("Running Rome comand to format code: '{}'", String.join(", ", args));
			}
			return runner.exec(stdin, args).assertExitZero(StandardCharsets.UTF_8);
		}

		/**
		 * The Rome executable currently does not have a parameter to specify the
		 * expected language / syntax. Rome always determined the language from the file
		 * extension. This method returns the file name for the desired language when a
		 * language was requested explicitly, or the file name of the input file for
		 * auto detection.
		 * 
		 * @param file File to be formatted.
		 * @return The file name to pass to the Rome executable.
		 */
		private String resolveFileName(File file) {
			var name = file.getName();
			if (language == null || language.isBlank()) {
				return name;
			}
			var dot = name.lastIndexOf(".");
			var ext = dot >= 0 ? name.substring(dot + 1) : name;
			switch (language) {
			case "js?":
				return "jsx".equals(ext) || "js".equals(ext) || "mjs".equals(ext) || "cjs".equals(ext) ? name
						: "file.js";
			case "ts?":
				return "tsx".equals(ext) || "ts".equals(ext) || "tjs".equals(ext) || "tjs".equals(ext) ? name
						: "file.js";
			case "js":
				return "js".equals(ext) || "mjs".equals(ext) || "cjs".equals(ext) ? name : "file.js";
			case "jsx":
				return "jsx".equals(ext) ? name : "file.jsx";
			case "ts":
				return "ts".equals(ext) || "mts".equals(ext) || "cts".equals(ext) ? name : "file.ts";
			case "tsx":
				return "tsx".equals(ext) ? name : "file.tsx";
			case "json":
				return "json".equals(ext) ? name : "file.json";
			// so that we can support new languages such as css or yaml when Rome adds
			// support for them without having to change the code
			default:
				return "file." + language;
			}
		}

		/**
		 * Creates a new formatter function for formatting a piece of code by delegating
		 * to the Rome executable.
		 * 
		 * @return A formatter function for formatting code.
		 */
		private FormatterFunc.Closeable toFunc() {
			var runner = new ProcessRunner();
			return FormatterFunc.Closeable.of(runner, this::format);
		}
	}
}
